package com.bfsi.customer;

import com.bfsi.customer.Dtos.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/** Service-to-service API (guarded by InternalKeyFilter, not routed through the gateway). */
@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class InternalController {
    private final CustomerService service;

    @PostMapping("/customers")
    public void create(@RequestBody CreateCustomerRequest r) { service.create(r); }

    @PostMapping("/accounts/transfer")
    public TransferResult transfer(@RequestBody TransferRequest r) { return service.transfer(r); }
}
