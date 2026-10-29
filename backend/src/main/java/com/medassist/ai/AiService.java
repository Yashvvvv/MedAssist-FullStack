package com.medassist.ai;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.medassist.ai.AiDtos.IdentifyResult;
import com.medassist.ai.AiDtos.InteractionResult;
import com.medassist.ai.AiDtos.MedicineIdentification;
import com.medassist.ai.AiDtos.MedicineInfo;
import com.medassist.ai.AiDtos.PrescriptionExtraction;
import com.medassist.ai.AiDtos.PrescriptionReadResult;
import com.medassist.ai.AiDtos.ProductMatch;
import com.medassist.ai.AiDtos.RxItem;
import com.medassist.ai.AiDtos.SuggestedLine;
import com.medassist.billing.EntitlementService;
import com.medassist.billing.Plan;
import com.medassist.catalog.CatalogDtos.MedicineView;
import com.medassist.catalog.MedicineRepository;
import com.medassist.catalog.Product;
import com.medassist.catalog.ProductRepository;
import com.medassist.common.error.ApiException;
import com.medassist.common.web.Pages;
import com.medassist.customers.CustomerService;
import com.medassist.customers.Prescription;
import com.medassist.inventory.InventoryService;
import com.medassist.security.CurrentUser;
import com.medassist.storage.FileStorage;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * AI features, with the cost controls a SaaS needs around a paid API:
 * <ul>
 *   <li>staff calls count against the org's monthly plan quota ({@link EntitlementService});</li>
 *   <li>consumer calls are rate limited per user (20 a day);</li>
 *   <li>text answers are cached, so the 100th "Dolo 650 + Combiflam?" costs nothing.</li>
 * </ul>
 * Answers are always matched back to real catalog/stock data, so the AI suggests and the
 * database decides.
 */
@Service
public class AiService {

    private final AiClient ai;
    private final ObjectMapper mapper;
    private final EntitlementService entitlements;
    private final MedicineRepository medicines;
    private final ProductRepository products;
    private final InventoryService inventory;
    private final CustomerService customers;
    private final FileStorage storage;
    private final Cache<String, Object> answers = Caffeine.newBuilder()
            .maximumSize(5_000).expireAfterWrite(Duration.ofDays(7)).build();
    private final Cache<UUID, Bucket> consumerBuckets = Caffeine.newBuilder()
            .maximumSize(100_000).expireAfterAccess(Duration.ofDays(1)).build();

    public AiService(AiClient ai, ObjectMapper mapper, EntitlementService entitlements, MedicineRepository medicines,
                     ProductRepository products, InventoryService inventory, CustomerService customers,
                     FileStorage storage) {
        this.ai = ai;
        this.mapper = mapper;
        this.entitlements = entitlements;
        this.medicines = medicines;
        this.products = products;
        this.inventory = inventory;
        this.customers = customers;
        this.storage = storage;
    }

    // ------------------------------------------------------------ medicine photo

    public IdentifyResult identify(CurrentUser user, AiClient.Image image, UUID branchId) {
        charge(user);
        String json = ai.generateJson(Prompts.IDENTIFY, List.of(image));
        MedicineIdentification id = parse(json, MedicineIdentification.class);
        List<MedicineView> matches = id.name() == null ? List.of() : matchMaster(id.name(), id.saltComposition());
        List<ProductMatch> stock = user.isStaff() && branchId != null && id.name() != null
                ? matchStock(id.name(), branchId) : List.of();
        return new IdentifyResult(id, matches, stock, AiDtos.DISCLAIMER);
    }

    // ------------------------------------------------------------ prescription reader

    /**
     * Reads a stored prescription image and turns it into a draft cart: every line the
     * model finds is matched to a product in this pharmacy, with current stock. The
     * pharmacist reviews and bills; the AI never bills on its own. Deliberately not one
     * transaction: the model call can take seconds and must not hold a pooled DB
     * connection while it runs.
     */
    public PrescriptionReadResult readPrescription(CurrentUser user, UUID prescriptionId, UUID branchId) {
        UUID orgId = user.requireOrg();
        entitlements.requireFeature(orgId, Plan.Feature.AI_PRESCRIPTION_READER);
        Prescription rx = customers.findPrescription(prescriptionId);
        if (rx.getFileKey() == null) {
            throw ApiException.badRequest("NO_FILE", "This prescription has no image to read");
        }
        if ("application/pdf".equals(rx.getContentType())) {
            throw ApiException.badRequest("UNSUPPORTED_FILE", "The reader needs a photo (JPEG/PNG/WebP), not a PDF");
        }
        charge(user);
        String json = ai.generateJson(Prompts.PRESCRIPTION,
                List.of(new AiClient.Image(storage.get(rx.getFileKey()), rx.getContentType())));
        PrescriptionExtraction ex = parse(json, PrescriptionExtraction.class);
        customers.recordAiExtraction(rx.getId(), json.length() > 20_000 ? json.substring(0, 20_000) : json,
                ex.doctorName() == null ? null : truncate(ex.doctorName(), 120),
                ex.doctorRegNo() == null ? null : truncate(ex.doctorRegNo(), 60));
        List<SuggestedLine> lines = new ArrayList<>();
        for (RxItem item : Objects.requireNonNullElse(ex.items(), List.<RxItem>of())) {
            if (item.medicine() == null || item.medicine().isBlank()) {
                continue;
            }
            List<ProductMatch> m = branchId == null ? List.of() : matchStock(item.medicine(), branchId);
            ProductMatch best = m.isEmpty() ? null : m.getFirst();
            lines.add(new SuggestedLine(item, best == null ? null : best.productId(),
                    best == null ? null : best.name(), best == null ? 0 : best.inStock()));
        }
        return new PrescriptionReadResult(rx.getId(), ex, lines, AiDtos.DISCLAIMER);
    }

    // ------------------------------------------------------------ text tools

    public InteractionResult interactions(CurrentUser user, List<String> names) {
        List<String> normalized = names.stream().map(n -> n.trim().toLowerCase(Locale.ROOT)).distinct().sorted().toList();
        if (normalized.size() < 2) {
            throw ApiException.badRequest("NEED_TWO", "Give at least two different medicines");
        }
        String key = "ix:" + String.join("|", normalized);
        InteractionResult cached = (InteractionResult) answers.getIfPresent(key);
        if (cached != null) {
            return cached;
        }
        charge(user);
        InteractionResult raw = parse(ai.generateJson(Prompts.interactions(normalized), List.of()),
                InteractionResult.class);
        InteractionResult result = new InteractionResult(Objects.requireNonNullElse(raw.interactions(), List.of()),
                raw.summary(), AiDtos.DISCLAIMER);
        answers.put(key, result);
        return result;
    }

    public MedicineInfo info(CurrentUser user, String medicine) {
        String key = "info:" + medicine.trim().toLowerCase(Locale.ROOT);
        MedicineInfo cached = (MedicineInfo) answers.getIfPresent(key);
        if (cached != null) {
            return cached;
        }
        charge(user);
        MedicineInfo raw = parse(ai.generateJson(Prompts.info(medicine.trim()), List.of()), MedicineInfo.class);
        MedicineInfo result = new MedicineInfo(raw.name(), raw.saltComposition(), raw.usedFor(), raw.howToTake(),
                raw.commonSideEffects(), raw.seriousSideEffects(), raw.precautions(), raw.storage(),
                AiDtos.DISCLAIMER);
        answers.put(key, result);
        return result;
    }

    // ------------------------------------------------------------ helpers

    /** Staff: one unit of the org's monthly AI quota. Consumers: 20 calls a day per user. */
    private void charge(CurrentUser user) {
        if (user.isStaff()) {
            entitlements.consume(user.orgId(), EntitlementService.Metric.AI_CALLS);
            return;
        }
        Bucket bucket = consumerBuckets.get(user.userId(), id -> Bucket.builder()
                .addLimit(Bandwidth.builder().capacity(20).refillIntervally(20, Duration.ofDays(1)).build()).build());
        if (!bucket.tryConsume(1)) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "AI_DAILY_LIMIT",
                    "You have used today's 20 AI requests. Try again tomorrow.");
        }
    }

    private List<MedicineView> matchMaster(String name, String salt) {
        List<MedicineView> out = new ArrayList<>(medicines.search(name, Pages.of(0, 3)).map(MedicineView::of).getContent());
        if (out.isEmpty() && salt != null && !salt.isBlank()) {
            out.addAll(medicines.search(salt, Pages.of(0, 3)).map(MedicineView::of).getContent());
        }
        return out;
    }

    private List<ProductMatch> matchStock(String name, UUID branchId) {
        List<Product> found = products.search(name.trim(), true, Pages.of(0, 3)).getContent();
        if (found.isEmpty()) {
            String firstWord = name.trim().split("\\s+")[0];
            if (firstWord.length() >= 3) {
                found = products.search(firstWord, true, Pages.of(0, 3)).getContent();
            }
        }
        Map<UUID, Long> qty = inventory.sellableByProduct(branchId, found.stream().map(Product::getId).toList());
        return found.stream().map(p -> new ProductMatch(p.getId(), p.getName(), qty.getOrDefault(p.getId(), 0L)))
                .toList();
    }

    private <T> T parse(String json, Class<T> type) {
        try {
            return mapper.readValue(stripFences(json), type);
        } catch (RuntimeException e) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "AI_BAD_ANSWER", "The AI answer could not be understood");
        }
    }

    /** Some models wrap JSON in ```json fences even in JSON mode. */
    static String stripFences(String s) {
        String t = s.trim();
        if (t.startsWith("```")) {
            t = t.replaceFirst("^```[a-zA-Z]*\\s*", "");
            int end = t.lastIndexOf("```");
            if (end >= 0) {
                t = t.substring(0, end);
            }
        }
        return t.trim();
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max);
    }
}
