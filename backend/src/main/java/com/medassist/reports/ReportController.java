package com.medassist.reports;

import com.medassist.common.time.BusinessTime;
import com.medassist.reports.ReportDtos.Dashboard;
import com.medassist.reports.ReportDtos.GstSummary;
import com.medassist.reports.ReportDtos.H1Entry;
import com.medassist.reports.ReportDtos.SalesSummary;
import com.medassist.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Tag(name = "Reports", description = "Dashboard, sales and margin, GST (rate + HSN), Schedule H1 register")
@RestController
public class ReportController {

    private final ReportService reports;
    private final BusinessTime time;

    public ReportController(ReportService reports, BusinessTime time) {
        this.reports = reports;
        this.time = time;
    }

    @Operation(summary = "Home screen numbers: today, month to date, stock alerts, open orders, refills due")
    @GetMapping("/api/v1/reports/dashboard")
    @PreAuthorize("hasRole('CASHIER')")
    public Dashboard dashboard(CurrentUser user, @RequestParam(required = false) UUID branchId) {
        return reports.dashboard(user.requireOrg(), branchId);
    }

    @Operation(summary = "Sales, returns, gross profit, payment split, top products (PRO)")
    @GetMapping("/api/v1/reports/sales")
    @PreAuthorize("hasRole('ADMIN')")
    public SalesSummary sales(CurrentUser user, @RequestParam(required = false) UUID branchId,
                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        LocalDate end = to == null ? time.today() : to;
        return reports.sales(user.requireOrg(), branchId, from == null ? end.withDayOfMonth(1) : from, end);
    }

    @Operation(summary = "GST outward supplies by rate and HSN, net of credit notes (PRO)")
    @GetMapping("/api/v1/reports/gst")
    @PreAuthorize("hasRole('ADMIN')")
    public GstSummary gst(CurrentUser user,
                          @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                          @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return reports.gst(user.requireOrg(), from, to);
    }

    @Operation(summary = "HSN summary as CSV, ready for the GST portal's offline tool (PRO)")
    @GetMapping(value = "/api/v1/reports/gst/hsn.csv", produces = "text/csv")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> gstHsnCsv(CurrentUser user,
                                            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        GstSummary g = reports.gst(user.requireOrg(), from, to);
        String csv = Stream.concat(Stream.of("HSN,Rate,Quantity,Taxable Value,CGST,SGST"),
                        g.byHsn().stream().map(r -> String.join(",", r.hsnCode(), r.rate().toPlainString(),
                                String.valueOf(r.quantity()), r.taxable().toPlainString(), r.cgst().toPlainString(),
                                r.sgst().toPlainString())))
                .collect(Collectors.joining("\n", "", "\n"));
        return csv("gst-hsn-" + from + "-to-" + to + ".csv", csv);
    }

    @Operation(summary = "Schedule H1/X register for a period (every plan: it is a legal requirement)")
    @GetMapping("/api/v1/reports/h1-register")
    @PreAuthorize("hasRole('PHARMACIST')")
    public List<H1Entry> h1(CurrentUser user,
                            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return reports.h1Register(user.requireOrg(), from, to);
    }

    @GetMapping(value = "/api/v1/reports/h1-register.csv", produces = "text/csv")
    @PreAuthorize("hasRole('PHARMACIST')")
    public ResponseEntity<byte[]> h1Csv(CurrentUser user,
                                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        String csv = Stream.concat(
                        Stream.of("Date,Invoice,Patient,Phone,Doctor,Doctor Reg No,Medicine,Schedule,Batch,Quantity"),
                        reports.h1Register(user.requireOrg(), from, to).stream().map(e -> Stream.of(
                                        e.date().toString(), e.invoiceNo(), e.patientName(), e.patientPhone(),
                                        e.doctorName(), e.doctorRegNo(), e.productName(), e.schedule(), e.batchNo(),
                                        String.valueOf(e.quantity()))
                                .map(ReportController::cell).collect(Collectors.joining(","))))
                .collect(Collectors.joining("\n", "", "\n"));
        return csv("h1-register-" + from + "-to-" + to + ".csv", csv);
    }

    /** RFC 4180 quoting, plus a leading quote on =+-@ so spreadsheets never run a cell as a formula. */
    public static String cell(String v) {
        if (v == null) {
            return "";
        }
        String s = v;
        if (!s.isEmpty() && "=+-@".indexOf(s.charAt(0)) >= 0) {
            s = "'" + s;
        }
        return s.contains(",") || s.contains("\"") || s.contains("\n") ? "\"" + s.replace("\"", "\"\"") + "\"" : s;
    }

    private static ResponseEntity<byte[]> csv(String filename, String body) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(body.getBytes(StandardCharsets.UTF_8));
    }
}
