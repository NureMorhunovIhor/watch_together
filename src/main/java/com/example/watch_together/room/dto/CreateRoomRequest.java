package com.example.watch_together.room.dto;

import com.example.watch_together.room.entity.AccessMode;
import com.example.watch_together.room.entity.RoomType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateRoomRequest {

    @NotBlank
    private String name;

    private String description;

    private RoomType roomType = RoomType.PRIVATE;

    private AccessMode accessMode = AccessMode.LINK_ONLY;

    @Min(2)
    @Max(100)
    private Integer maxParticipants = 10;
}