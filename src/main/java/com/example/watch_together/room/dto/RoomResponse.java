package com.example.watch_together.room.dto;

import com.example.watch_together.room.entity.AccessMode;
import com.example.watch_together.room.entity.RoomType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RoomResponse {
    private String roomCode;
    private String name;
    private String description;
    private Long ownerId;
    private String ownerUsername;
    private RoomType roomType;
    private AccessMode accessMode;
    private Integer maxParticipants;
    private Boolean active;
}