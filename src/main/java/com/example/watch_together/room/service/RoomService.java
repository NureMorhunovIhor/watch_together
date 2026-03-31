package com.example.watch_together.room.service;

import com.example.watch_together.room.dto.CreateRoomRequest;
import com.example.watch_together.room.dto.ParticipantResponse;
import com.example.watch_together.room.dto.RoomResponse;
import com.example.watch_together.room.dto.UpdateRoomRequest;
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
import java.util.Comparator;
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

    @Transactional
    public RoomResponse updateRoom(String code, UpdateRoomRequest request) {
        WatchRoom room = getRoomByCode(code);
        User user = getCurrentUser();

        getHostParticipant(room, user);

        if (request.getName() != null) room.setName(request.getName());
        if (request.getDescription() != null) room.setDescription(request.getDescription());
        if (request.getRoomType() != null) room.setRoomType(request.getRoomType());
        if (request.getAccessMode() != null) room.setAccessMode(request.getAccessMode());

        if (request.getMaxParticipants() != null) {
            long current = roomParticipantRepository.countByRoomAndJoinStatus(room, JoinStatus.JOINED);

            if (request.getMaxParticipants() < current) {
                throw new RuntimeException("Cannot reduce maxParticipants below current participants");
            }

            room.setMaxParticipants(request.getMaxParticipants());
        }

        return mapRoom(room);
    }
    public RoomResponse getByCode(String code) {
        WatchRoom room = watchRoomRepository.findByRoomCode(code)
                .orElseThrow(() -> new RuntimeException("Room not found"));

        return mapRoom(room);
    }

    @Transactional
    public RoomResponse joinRoom(String code) {
        WatchRoom room = getRoomByCode(code);
        User user = getCurrentUser();

        if (!Boolean.TRUE.equals(room.getIsActive())) {
            throw new RuntimeException("Room is closed");
        }

        RoomParticipant participant = roomParticipantRepository.findByRoomAndUser(room, user)
                .orElse(null);

        if (participant != null) {
            if (participant.getJoinStatus() == JoinStatus.KICKED) {
                throw new RuntimeException("You were kicked from this room");
            }

            if (participant.getJoinStatus() == JoinStatus.JOINED) {
                return mapRoom(room);
            }

            if (participant.getJoinStatus() == JoinStatus.INVITED ||
                    participant.getJoinStatus() == JoinStatus.LEFT) {
                long activeParticipants = roomParticipantRepository.countByRoomAndJoinStatus(room, JoinStatus.JOINED);
                if (activeParticipants >= room.getMaxParticipants()) {
                    throw new RuntimeException("Room is full");
                }

                participant.setJoinStatus(JoinStatus.JOINED);
                participant.setJoinedAt(LocalDateTime.now());
                participant.setLeftAt(null);

                roomParticipantRepository.save(participant);
                return mapRoom(room);
            }

            throw new RuntimeException("Cannot join room");
        }

        long activeParticipants = roomParticipantRepository.countByRoomAndJoinStatus(room, JoinStatus.JOINED);
        if (activeParticipants >= room.getMaxParticipants()) {
            throw new RuntimeException("Room is full");
        }

        RoomParticipant newParticipant = RoomParticipant.builder()
                .room(room)
                .user(user)
                .participantRole(ParticipantRole.VIEWER)
                .joinStatus(JoinStatus.JOINED)
                .joinedAt(LocalDateTime.now())
                .isMuted(false)
                .canControlPlayback(false)
                .build();

        roomParticipantRepository.save(newParticipant);

        return mapRoom(room);
    }

    @Transactional
    public void leaveRoom(String code) {
        WatchRoom room = getRoomByCode(code);
        User user = getCurrentUser();

        RoomParticipant participant = roomParticipantRepository.findByRoomAndUser(room, user)
                .orElseThrow(() -> new RuntimeException("Not in room"));

        boolean isHost = participant.getParticipantRole() == ParticipantRole.HOST;

        participant.setJoinStatus(JoinStatus.LEFT);
        participant.setLeftAt(LocalDateTime.now());

        if (isHost) {
            handleHostLeaving(room);
        }
    }
    private void handleHostLeaving(WatchRoom room) {

        List<RoomParticipant> participants = roomParticipantRepository
                .findAllByRoomAndJoinStatus(room, JoinStatus.JOINED);

        if (participants.isEmpty()) {
            room.setIsActive(false);
            room.setEndedAt(LocalDateTime.now());
            return;
        }

        RoomParticipant newHost = participants.stream()
                .sorted(Comparator.comparing(RoomParticipant::getJoinedAt))
                .findFirst()
                .orElseThrow();

        newHost.setParticipantRole(ParticipantRole.HOST);
        newHost.setCanControlPlayback(true);
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

    private RoomParticipant getHostParticipant(WatchRoom room, User user) {
        RoomParticipant participant = roomParticipantRepository.findByRoomAndUser(room, user)
                .orElseThrow(() -> new RuntimeException("Not in room"));

        if (participant.getParticipantRole() != ParticipantRole.HOST) {
            throw new RuntimeException("Only host can perform this action");
        }

        return participant;
    }

    @Transactional
    public void kickUser(String code, Long userId) {
        WatchRoom room = getRoomByCode(code);
        User host = getCurrentUser();

        getHostParticipant(room, host);

        User target = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        RoomParticipant participant = roomParticipantRepository.findByRoomAndUser(room, target)
                .orElseThrow(() -> new RuntimeException("User not in room"));

        if (participant.getParticipantRole() == ParticipantRole.HOST) {
            throw new RuntimeException("Cannot kick host");
        }

        participant.setJoinStatus(JoinStatus.KICKED);
        participant.setLeftAt(LocalDateTime.now());
    }

    @Transactional
    public void transferHost(String code, Long userId) {
        WatchRoom room = getRoomByCode(code);
        User currentUser = getCurrentUser();

        RoomParticipant currentHost = getHostParticipant(room, currentUser);

        User newHostUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        RoomParticipant newHost = roomParticipantRepository.findByRoomAndUser(room, newHostUser)
                .orElseThrow(() -> new RuntimeException("User not in room"));

        currentHost.setParticipantRole(ParticipantRole.VIEWER);
        currentHost.setCanControlPlayback(false);

        newHost.setParticipantRole(ParticipantRole.HOST);
        newHost.setCanControlPlayback(true);
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

    @Transactional
    public String regenerateCode(String code) {
        WatchRoom room = getRoomByCode(code);
        User user = getCurrentUser();

        getHostParticipant(room, user);

        String newCode = generateRoomCode();
        room.setRoomCode(newCode);

        return newCode;
    }

    private WatchRoom getRoomByCode(String code) {
        return watchRoomRepository.findByRoomCode(code)
                .orElseThrow(() -> new RuntimeException("Room not found"));
    }

    public List<RoomResponse> getPublicRooms() {
        return watchRoomRepository
                .findByRoomTypeAndIsActive(RoomType.PUBLIC, true)
                .stream()
                .map(this::mapRoom)
                .toList();
    }

    public List<RoomResponse> searchRooms(String query) {
        return watchRoomRepository.searchRooms(query)
                .stream()
                .map(this::mapRoom)
                .toList();
    }

    @Transactional
    public void inviteUser(String code, Long userId) {
        WatchRoom room = getRoomByCode(code);
        User host = getCurrentUser();

        getHostParticipant(room, host);

        User target = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        RoomParticipant existing = roomParticipantRepository.findByRoomAndUser(room, target)
                .orElse(null);

        if (existing != null) {
            throw new RuntimeException("User already invited or in room");
        }

        RoomParticipant invite = RoomParticipant.builder()
                .room(room)
                .user(target)
                .participantRole(ParticipantRole.VIEWER)
                .joinStatus(JoinStatus.INVITED)
                .joinedAt(LocalDateTime.now())
                .isMuted(false)
                .canControlPlayback(false)
                .build();

        roomParticipantRepository.save(invite);
    }

    @Transactional
    public void acceptInvite(String code) {
        WatchRoom room = getRoomByCode(code);
        User user = getCurrentUser();

        RoomParticipant participant = roomParticipantRepository.findByRoomAndUser(room, user)
                .orElseThrow(() -> new RuntimeException("No invite found"));

        if (participant.getJoinStatus() == JoinStatus.JOINED) {
            return;
        }

        if (participant.getJoinStatus() != JoinStatus.INVITED) {
            throw new RuntimeException("Invalid invite state");
        }

        long activeParticipants = roomParticipantRepository.countByRoomAndJoinStatus(room, JoinStatus.JOINED);
        if (activeParticipants >= room.getMaxParticipants()) {
            throw new RuntimeException("Room is full");
        }

        participant.setJoinStatus(JoinStatus.JOINED);
        participant.setJoinedAt(LocalDateTime.now());
        participant.setLeftAt(null);
    }

    @Transactional
    public void closeRoom(String code) {
        WatchRoom room = getRoomByCode(code);
        User user = getCurrentUser();

        getHostParticipant(room, user);

        room.setIsActive(false);
        room.setEndedAt(LocalDateTime.now());
    }
}