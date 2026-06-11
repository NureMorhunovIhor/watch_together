package com.example.watch_together.billing.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class BillingMeResponse {
    private String planId;
    private String planName;
    private String status;
    private String billingCycle;

    private Boolean autoRenew;
    private LocalDateTime startedAt;
    private LocalDateTime currentPeriodEnd;

    private Integer maxRooms;
    private Integer maxParticipants;
    private Integer maxQueueItems;

    private Boolean allowInviteOnly;
    private Boolean allowRoomCustomization;
    private Boolean allowAdvancedModeration;
    private Boolean allowRoomStatistics;
}