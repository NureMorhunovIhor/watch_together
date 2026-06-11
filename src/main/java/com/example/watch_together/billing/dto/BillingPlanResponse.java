package com.example.watch_together.billing.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class BillingPlanResponse {
    private String id;
    private String name;
    private String description;

    private BigDecimal priceMonthly;
    private BigDecimal priceYearly;

    private Integer maxRooms;
    private Integer maxParticipants;
    private Integer maxQueueItems;

    private Boolean allowInviteOnly;
    private Boolean allowRoomCustomization;
    private Boolean allowAdvancedModeration;
    private Boolean allowRoomStatistics;

    private List<String> perks;
}