package com.example.watch_together.room.dto;

import com.example.watch_together.room.entity.AccessMode;
import com.example.watch_together.room.entity.RoomType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class UpdateRoomRequest {

    private String name;
    private String description;
    private RoomType roomType;
    private AccessMode accessMode;

    @Min(2)
    @Max(100)
    private Integer maxParticipants;
}