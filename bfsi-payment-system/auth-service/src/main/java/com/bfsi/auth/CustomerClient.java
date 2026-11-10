package com.bfsi.auth;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "customer-service", configuration = InternalFeignConfig.class)
public interface CustomerClient {
    @PostMapping("/internal/customers")
    void create(@RequestBody Dtos.CreateCustomerRequest request);
}
