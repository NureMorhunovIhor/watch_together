package com.example.watch_together.billing.controller;

import com.example.watch_together.billing.dto.*;
import com.example.watch_together.billing.service.BillingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/billing")
@RequiredArgsConstructor
public class BillingController {

    private final BillingService billingService;

    @GetMapping("/plans")
    public List<BillingPlanResponse> plans() {
        return billingService.getPlans();
    }

    @GetMapping("/me")
    public BillingMeResponse me() {
        return billingService.getMe();
    }

    @PostMapping("/checkout")
    public CheckoutResponse checkout(@RequestBody CheckoutRequest request) {
        return billingService.checkout(request);
    }

    @PostMapping("/capture")
    public BillingMeResponse capture(@RequestBody CaptureRequest request) {
        return billingService.capture(request);
    }

    @PostMapping("/cancel-renewal")
    public BillingMeResponse cancelRenewal() {
        return billingService.cancelRenewal();
    }
}