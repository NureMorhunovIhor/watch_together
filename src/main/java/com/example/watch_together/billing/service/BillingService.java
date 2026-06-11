package com.example.watch_together.billing.service;

import com.example.watch_together.billing.dto.*;
import com.example.watch_together.billing.entity.Payment;
import com.example.watch_together.billing.entity.UserSubscription;
import com.example.watch_together.billing.repository.PaymentRepository;
import com.example.watch_together.billing.repository.UserSubscriptionRepository;
import com.example.watch_together.security.CustomUserDetails;
import com.example.watch_together.user.entity.User;
import com.example.watch_together.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BillingService {

    private final UserRepository userRepository;
    private final UserSubscriptionRepository subscriptionRepository;
    private final PaymentRepository paymentRepository;
    private final PayPalService payPalService;
    public List<BillingPlanResponse> getPlans() {
        return List.of(
                freePlan(),
                premiumPlan(),
                proPlan()
        );
    }

    public BillingMeResponse getMe() {
        User user = getCurrentUser();
        return buildBillingResponse(getCurrentPlanId(user), getSubscription(user).orElse(null));
    }

    @Transactional
    public CheckoutResponse checkout(CheckoutRequest request) {
        User user = getCurrentUser();

        String planId = normalizePlan(request.getPlanId());

        if ("FREE".equals(planId)) {
            throw new RuntimeException("Free plan does not require checkout");
        }

        BillingCycle cycle = request.getCycle() == null
                ? BillingCycle.MONTHLY
                : request.getCycle();

        BigDecimal amount = getPrice(planId, cycle);

        PayPalService.PayPalOrder order =
                payPalService.createOrder(planId, cycle, amount);

        Payment payment = Payment.builder()
                .user(user)
                .orderId(order.orderId())
                .planId(planId)
                .billingCycle(cycle.name())
                .amount(amount)
                .currency("USD")
                .status("PENDING")
                .createdAt(LocalDateTime.now())
                .build();

        paymentRepository.save(payment);

        return new CheckoutResponse(order.orderId(), order.approveUrl());
    }

    @Transactional
    public BillingMeResponse capture(CaptureRequest request) {
        User user = getCurrentUser();

        Payment payment = paymentRepository.findByOrderId(request.getOrderId())
                .orElseThrow(() -> new RuntimeException("Payment order not found"));

        if (!payment.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Payment does not belong to current user");
        }

        if (!"PAID".equals(payment.getStatus())) {
            payPalService.captureOrder(payment.getOrderId());

            payment.setStatus("PAID");
            payment.setPaidAt(LocalDateTime.now());
            paymentRepository.save(payment);
        }

        UserSubscription subscription = getSubscription(user)
                .orElse(UserSubscription.builder()
                        .user(user)
                        .startedAt(LocalDateTime.now())
                        .build());

        subscription.setPlanId(payment.getPlanId());
        subscription.setStatus("ACTIVE");
        subscription.setBillingCycle(payment.getBillingCycle());
        subscription.setAutoRenew(true);

        LocalDateTime now = LocalDateTime.now();
        subscription.setStartedAt(subscription.getStartedAt() == null ? now : subscription.getStartedAt());

        if ("YEARLY".equals(payment.getBillingCycle())) {
            subscription.setCurrentPeriodEnd(now.plusYears(1));
        } else {
            subscription.setCurrentPeriodEnd(now.plusMonths(1));
        }

        subscriptionRepository.save(subscription);

        return buildBillingResponse(subscription.getPlanId(), subscription);
    }

    @Transactional
    public BillingMeResponse cancelRenewal() {
        User user = getCurrentUser();

        UserSubscription subscription = getSubscription(user)
                .orElseThrow(() -> new RuntimeException("No active subscription"));

        subscription.setAutoRenew(false);
        subscriptionRepository.save(subscription);

        return buildBillingResponse(subscription.getPlanId(), subscription);
    }

    public String getCurrentPlanId(User user) {
        return getSubscription(user)
                .filter(s -> "ACTIVE".equals(s.getStatus()))
                .map(UserSubscription::getPlanId)
                .orElse("FREE");
    }

    public BillingPlanResponse getCurrentPlan(User user) {
        String planId = getCurrentPlanId(user);

        return getPlans().stream()
                .filter(p -> p.getId().equals(planId))
                .findFirst()
                .orElse(freePlan());
    }

    private BillingMeResponse buildBillingResponse(String planId, UserSubscription subscription) {
        BillingPlanResponse plan = getPlans().stream()
                .filter(p -> p.getId().equals(planId))
                .findFirst()
                .orElse(freePlan());

        return BillingMeResponse.builder()
                .planId(plan.getId())
                .planName(plan.getName())
                .status(subscription == null ? "ACTIVE" : subscription.getStatus())
                .billingCycle(subscription == null ? "MONTHLY" : subscription.getBillingCycle())
                .autoRenew(subscription == null ? false : subscription.getAutoRenew())
                .startedAt(subscription == null ? null : subscription.getStartedAt())
                .currentPeriodEnd(subscription == null ? null : subscription.getCurrentPeriodEnd())
                .maxRooms(plan.getMaxRooms())
                .maxParticipants(plan.getMaxParticipants())
                .maxQueueItems(plan.getMaxQueueItems())
                .allowInviteOnly(plan.getAllowInviteOnly())
                .allowRoomCustomization(plan.getAllowRoomCustomization())
                .allowAdvancedModeration(plan.getAllowAdvancedModeration())
                .allowRoomStatistics(plan.getAllowRoomStatistics())
                .build();
    }

    private Optional<UserSubscription> getSubscription(User user) {
        return subscriptionRepository.findTopByUserOrderByStartedAtDesc(user);
    }

    private String normalizePlan(String planId) {
        if (planId == null) {
            throw new RuntimeException("Plan is required");
        }

        String normalized = planId.trim().toUpperCase();

        if (!List.of("FREE", "PREMIUM", "PRO").contains(normalized)) {
            throw new RuntimeException("Unknown plan");
        }

        return normalized;
    }

    private BigDecimal getPrice(String planId, BillingCycle cycle) {
        BillingPlanResponse plan = getPlans().stream()
                .filter(p -> p.getId().equals(planId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Unknown plan"));

        return cycle == BillingCycle.YEARLY
                ? plan.getPriceYearly()
                : plan.getPriceMonthly();
    }

    private BillingPlanResponse freePlan() {
        return BillingPlanResponse.builder()
                .id("FREE")
                .name("Free")
                .description("Basic watch parties for small groups.")
                .priceMonthly(BigDecimal.ZERO)
                .priceYearly(BigDecimal.ZERO)
                .maxRooms(3)
                .maxParticipants(5)
                .maxQueueItems(10)
                .allowInviteOnly(false)
                .allowRoomCustomization(false)
                .allowAdvancedModeration(false)
                .allowRoomStatistics(false)
                .perks(List.of(
                        "Create up to 3 active rooms",
                        "Up to 5 participants",
                        "Basic chat and reactions",
                        "Queue up to 10 media items"
                ))
                .build();
    }

    private BillingPlanResponse premiumPlan() {
        return BillingPlanResponse.builder()
                .id("PREMIUM")
                .name("Premium")
                .description("For bigger movie nights and custom rooms.")
                .priceMonthly(BigDecimal.valueOf(4.99))
                .priceYearly(BigDecimal.valueOf(47.90))
                .maxRooms(15)
                .maxParticipants(20)
                .maxQueueItems(100)
                .allowInviteOnly(true)
                .allowRoomCustomization(true)
                .allowAdvancedModeration(true)
                .allowRoomStatistics(false)
                .perks(List.of(
                        "Create up to 15 active rooms",
                        "Up to 20 participants",
                        "Room themes and cover images",
                        "Invite-only rooms",
                        "Queue up to 100 media items"
                ))
                .build();
    }

    private BillingPlanResponse proPlan() {
        return BillingPlanResponse.builder()
                .id("PRO")
                .name("Pro Host")
                .description("For hosts, communities and large watch parties.")
                .priceMonthly(BigDecimal.valueOf(9.99))
                .priceYearly(BigDecimal.valueOf(95.90))
                .maxRooms(9999)
                .maxParticipants(50)
                .maxQueueItems(9999)
                .allowInviteOnly(true)
                .allowRoomCustomization(true)
                .allowAdvancedModeration(true)
                .allowRoomStatistics(true)
                .perks(List.of(
                        "Unlimited active rooms",
                        "Up to 50 participants",
                        "Unlimited queue",
                        "Advanced moderation",
                        "Room statistics"
                ))
                .build();
    }

    private User getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (!(principal instanceof CustomUserDetails userDetails)) {
            throw new RuntimeException("Unauthorized");
        }

        return userRepository.findById(userDetails.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}