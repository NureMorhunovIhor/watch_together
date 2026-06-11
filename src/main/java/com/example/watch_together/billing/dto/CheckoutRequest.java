package com.example.watch_together.billing.dto;

import lombok.Data;

@Data
public class CheckoutRequest {
    private String planId;
    private BillingCycle cycle;
}