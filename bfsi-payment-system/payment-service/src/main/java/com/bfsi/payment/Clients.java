package com.bfsi.payment;

import com.bfsi.payment.Dtos.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

public final class Clients {
    private Clients() {}

    @FeignClient(name = "customer-service", configuration = InternalFeignConfig.class)
    public interface CustomerClient {
        @PostMapping("/internal/accounts/transfer")
        TransferResult transfer(@RequestBody TransferRequest r);
    }

    @FeignClient(name = "ledger-service", configuration = InternalFeignConfig.class)
    public interface LedgerClient {
        @PostMapping("/internal/ledger")
        void record(@RequestBody LedgerRequest r);
        @PostMapping("/internal/audit")
        void audit(@RequestBody AuditRequest r);
    }

    @FeignClient(name = "notification-service", configuration = InternalFeignConfig.class)
    public interface NotificationClient {
        @PostMapping("/internal/notify")
        void send(@RequestBody NotifyRequest r);
    }
}
