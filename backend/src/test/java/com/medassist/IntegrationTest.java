package com.medassist;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full-stack tests: real HTTP layer (MockMvc), real security, real Postgres. Each test
 * creates its own pharmacies with unique emails, so tests never share state and need
 * no cleanup.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({TestcontainersConfig.class, TestAiConfig.class})
public abstract class IntegrationTest {

    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired
    protected MockMvc mvc;

    protected String uniq;

    @BeforeEach
    void uniqueSuffix() {
        uniq = UUID.randomUUID().toString().substring(0, 8) + SEQ.incrementAndGet();
    }

    /** Registers a pharmacy and returns the owner's session. */
    protected Session registerPharmacy(String name) throws Exception {
        String email = "owner-" + name.toLowerCase().replace(' ', '-') + "-" + uniq + "@test.in";
        String body = """
                {"owner":{"email":"%s","password":"secret123","fullName":"Owner %s","phone":"9876543210"},
                 "organization":{"name":"%s","phone":"9876543210",
                   "firstBranch":{"name":"Main Store","addressLine":"Sector 18","city":"Noida","state":"Uttar Pradesh",
                                  "pincode":"201301","latitude":28.5708,"longitude":77.3261}}}
                """.formatted(email, name, name);
        MvcResult res = mvc.perform(post("/api/v1/auth/register-pharmacy").contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andDo(r -> {
                    if (r.getResponse().getStatus() != 201) {
                        System.out.println("REGISTER FAILED: " + r.getResponse().getContentAsString());
                    }
                })
                .andExpect(status().isCreated()).andReturn();
        return Session.from(res, email);
    }

    protected Session registerConsumer(String name) throws Exception {
        String email = "patient-" + name.toLowerCase() + "-" + uniq + "@test.in";
        MvcResult res = mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"secret123","fullName":"%s","phone":"9812345678"}
                                """.formatted(email, name)))
                .andExpect(status().isCreated()).andReturn();
        return Session.from(res, email);
    }

    protected String firstBranchId(Session s) throws Exception {
        String json = mvc.perform(get("/api/v1/branches").with(s)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$[0].id");
    }

    protected String createProduct(Session s, String name, String schedule, String mrp, String gst) throws Exception {
        String json = mvc.perform(post("/api/v1/products").with(s).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s","schedule":"%s","gstRate":%s,"mrp":%s,"saltComposition":"Test salt","reorderLevel":5}
                                """.formatted(name, schedule, gst, mrp)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.id");
    }

    protected static String json(MvcResult r) throws Exception {
        return r.getResponse().getContentAsString();
    }

    /** A logged-in caller; usable as a MockMvc request post-processor via {@code .with(session)}. */
    public record Session(String accessToken, String refreshToken, String email, String orgId)
            implements org.springframework.test.web.servlet.request.RequestPostProcessor {

        static Session from(MvcResult res, String email) throws Exception {
            String json = res.getResponse().getContentAsString();
            String org = null;
            try {
                org = JsonPath.read(json, "$.activeOrg.orgId");
            } catch (Exception ignored) {
                // consumer: no org
            }
            return new Session(JsonPath.read(json, "$.accessToken"), JsonPath.read(json, "$.refreshToken"), email, org);
        }

        @Override
        public org.springframework.mock.web.MockHttpServletRequest postProcessRequest(
                org.springframework.mock.web.MockHttpServletRequest request) {
            request.addHeader("Authorization", "Bearer " + accessToken);
            return request;
        }
    }

    protected static MockHttpServletRequestBuilder jsonPost(String url, String body) {
        return post(url).contentType(MediaType.APPLICATION_JSON).content(body);
    }
}
