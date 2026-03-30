package com.example.watch_together.room.service;

import com.example.watch_together.room.dto.CreateRoomRequest;
import com.example.watch_together.room.dto.ParticipantResponse;
import com.example.watch_together.room.dto.RoomResponse;
import com.example.watch_together.room.entity.*;
import com.example.watch_together.room.repository.RoomParticipantRepository;
import com.example.watch_together.room.repository.RoomSettingsRepository;
import com.example.watch_together.room.repository.WatchRoomRepository;
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
import java.util.Random;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final WatchRoomRepository watchRoomRepository;
    private final RoomSettingsRepository roomSettingsRepository;
    private final RoomParticipantRepository roomParticipantRepository;
    private final UserRepository userRepository;

    @Transactional
    public RoomResponse createRoom(CreateRoomRequest request) {
        User owner = getCurrentUser();

        WatchRoom room = WatchRoom.builder()
                .roomCode(generateRoomCode())
                .name(request.getName())
                .description(request.getDescription())
                .owner(owner)
                .roomType(request.getRoomType())
                .accessMode(request.getAccessMode())
                .maxParticipants(request.getMaxParticipants())
                .isActive(true)
                .build();

        room = watchRoomRepository.save(room);

        RoomSettings settings = RoomSettings.builder()
                .room(room)
                .allowParticipantControl(false)
                .allowChat(true)
                .allowReactions(true)
                .allowVoiceChat(false)
                .allowVideoChat(false)
                .autoPauseOnBuffer(true)
                .showSubtitles(true)
                .subtitlesLanguage("en")
                .playbackSpeed(BigDecimal.valueOf(1.00))
                .build();

        roomSettingsRepository.save(settings);

        RoomParticipant ownerParticipant = RoomParticipant.builder()
                .room(room)
                .user(owner)
                .participantRole(ParticipantRole.HOST)
                .joinStatus(JoinStatus.JOINED)
                .joinedAt(LocalDateTime.now())
                .isMuted(false)
                .canControlPlayback(true)
                .build();

        roomParticipantRepository.save(ownerParticipant);

        return mapRoom(room);
    }

    public RoomResponse getByCode(String code) {
        WatchRoom room = watchRoomRepository.findByRoomCode(code)
                .orElseThrow(() -> new RuntimeException("Room not found"));

        return mapRoom(room);
    }

    @Transactional
    public RoomResponse joinRoom(String code) {
        WatchRoom room = watchRoomRepository.findByRoomCode(code)
                .orElseThrow(() -> new RuntimeException("Room not found"));

        User user = getCurrentUser();

        long activeParticipants = roomParticipantRepository.countByRoomAndJoinStatus(room, JoinStatus.JOINED);
        if (activeParticipants >= room.getMaxParticipants()) {
            throw new RuntimeException("Room is full");
        }

        RoomParticipant participant = roomParticipantRepository.findByRoomAndUser(room, user)
                .orElse(null);

        if (participant == null) {
            participant = RoomParticipant.builder()
                    .room(room)
                    .user(user)
                    .participantRole(ParticipantRole.VIEWER)
                    .joinStatus(JoinStatus.JOINED)
                    .joinedAt(LocalDateTime.now())
                    .isMuted(false)
                    .canControlPlayback(false)
                    .build();
        } else {
            participant.setJoinStatus(JoinStatus.JOINED);
            participant.setLeftAt(null);
        }

        roomParticipantRepository.save(participant);

        return mapRoom(room);
    }

    public List<ParticipantResponse> getParticipants(String code) {
        WatchRoom room = watchRoomRepository.findByRoomCode(code)
                .orElseThrow(() -> new RuntimeException("Room not found"));

        return roomParticipantRepository.findAllByRoomAndJoinStatus(room, JoinStatus.JOINED)
                .stream()
                .map(p -> ParticipantResponse.builder()
                        .userId(p.getUser().getId())
                        .username(p.getUser().getUsername())
                        .displayName(p.getUser().getDisplayName())
                        .role(p.getParticipantRole())
                        .muted(p.getIsMuted())
                        .canControlPlayback(p.getCanControlPlayback())
                        .build())
                .toList();
    }

    private RoomResponse mapRoom(WatchRoom room) {
        return RoomResponse.builder()
                .roomCode(room.getRoomCode())
                .name(room.getName())
                .description(room.getDescription())
                .ownerId(room.getOwner().getId())
                .ownerUsername(room.getOwner().getUsername())
                .roomType(room.getRoomType())
                .accessMode(room.getAccessMode())
                .maxParticipants(room.getMaxParticipants())
                .active(room.getIsActive())
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

    private String generateRoomCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();

        String code;
        do {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 8; i++) {
                sb.append(chars.charAt(random.nextInt(chars.length())));
            }
            code = sb.toString();
        } while (watchRoomRepository.existsByRoomCode(code));

        return code;
    }
}