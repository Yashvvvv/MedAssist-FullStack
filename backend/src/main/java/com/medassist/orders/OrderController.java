package com.medassist.orders;

import com.medassist.common.web.PageResponse;
import com.medassist.orders.OrderDtos.CompleteRequest;
import com.medassist.orders.OrderDtos.OrderView;
import com.medassist.orders.OrderDtos.PlaceOrderRequest;
import com.medassist.orders.OrderDtos.RejectRequest;
import com.medassist.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Tag(name = "Orders", description = "Patients order from a pharmacy; staff accept, prepare and hand over")
@RestController
public class OrderController {

    private final OrderService orders;

    public OrderController(OrderService orders) {
        this.orders = orders;
    }

    // ------------------------------------------------------------------ patient

    @Operation(summary = "Place an order (multipart: 'order' JSON + optional 'prescription' image)")
    @PostMapping(value = "/api/v1/me/orders", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public OrderView place(CurrentUser user, @Valid @RequestPart("order") PlaceOrderRequest req,
                           @RequestPart(value = "prescription", required = false) MultipartFile prescription) {
        return orders.place(user, req, prescription);
    }

    @Operation(summary = "Place an order without a prescription (plain JSON)")
    @PostMapping(value = "/api/v1/me/orders", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public OrderView placeJson(CurrentUser user, @Valid @RequestBody PlaceOrderRequest req) {
        return orders.place(user, req, null);
    }

    @GetMapping("/api/v1/me/orders")
    public PageResponse<OrderView> mine(CurrentUser user, @RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "20") int size) {
        return orders.myOrders(user, page, size);
    }

    @GetMapping("/api/v1/me/orders/{id}")
    public OrderView myOrder(CurrentUser user, @PathVariable UUID id) {
        return orders.myOrder(user, id);
    }

    @PostMapping("/api/v1/me/orders/{id}/cancel")
    public OrderView cancel(CurrentUser user, @PathVariable UUID id) {
        return orders.cancelMine(user, id);
    }

    // ------------------------------------------------------------------ staff

    @Operation(summary = "Incoming orders, newest first")
    @GetMapping("/api/v1/orders")
    @PreAuthorize("hasRole('CASHIER')")
    public PageResponse<OrderView> queue(@RequestParam(required = false) CustomerOrder.Status status,
                                         @RequestParam(required = false) UUID branchId,
                                         @RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "20") int size) {
        return orders.queue(status, branchId, page, size);
    }

    @GetMapping("/api/v1/orders/{id}")
    @PreAuthorize("hasRole('CASHIER')")
    public OrderView get(@PathVariable UUID id) {
        return orders.get(id);
    }

    @PostMapping("/api/v1/orders/{id}/accept")
    @PreAuthorize("hasRole('CASHIER')")
    public OrderView accept(CurrentUser user, @PathVariable UUID id) {
        return orders.transition(user, id, CustomerOrder.Status.ACCEPTED, null);
    }

    @PostMapping("/api/v1/orders/{id}/ready")
    @PreAuthorize("hasRole('CASHIER')")
    public OrderView ready(CurrentUser user, @PathVariable UUID id) {
        return orders.transition(user, id, CustomerOrder.Status.READY, null);
    }

    @PostMapping("/api/v1/orders/{id}/reject")
    @PreAuthorize("hasRole('CASHIER')")
    public OrderView reject(CurrentUser user, @PathVariable UUID id, @Valid @RequestBody RejectRequest req) {
        return orders.transition(user, id, CustomerOrder.Status.REJECTED, req.reason());
    }

    @Operation(summary = "Hand over: bills the order as a sale (FEFO, GST, Rx checks) and links the invoice")
    @PostMapping("/api/v1/orders/{id}/complete")
    @PreAuthorize("hasRole('CASHIER')")
    public OrderView complete(CurrentUser user, @PathVariable UUID id, @Valid @RequestBody CompleteRequest req) {
        return orders.complete(user, id, req.paymentMode());
    }
}
