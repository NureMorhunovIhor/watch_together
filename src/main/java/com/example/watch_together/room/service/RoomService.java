package com.example.watch_together.room.service;

import com.example.watch_together.billing.service.BillingService;
import com.example.watch_together.notification.entity.NotificationType;
import com.example.watch_together.notification.service.NotificationService;
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
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.watch_together.billing.dto.BillingPlanResponse;
import com.example.watch_together.billing.service.BillingService;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final WatchRoomRepository watchRoomRepository;
    private final RoomSettingsRepository roomSettingsRepository;
    private final RoomParticipantRepository roomParticipantRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final SimpMessagingTemplate messagingTemplate;
    private final BillingService billingService;
    @Transactional
    public RoomResponse createRoom(CreateRoomRequest request) {
        User owner = getCurrentUser();
        BillingPlanResponse plan = billingService.getCurrentPlan(owner);

        long activeRooms = watchRoomRepository.countByOwnerAndIsActive(owner, true);

        if (activeRooms >= plan.getMaxRooms()) {
            throw new ResponseStatusException(
                    HttpStatus.PAYMENT_REQUIRED,
                    "Your current plan allows only " + plan.getMaxRooms() + " active rooms"
            );
        }

        if (request.getMaxParticipants() > plan.getMaxParticipants()) {
            throw new ResponseStatusException(
                    HttpStatus.PAYMENT_REQUIRED,
                    "Your current plan allows up to " + plan.getMaxParticipants() + " participants"
            );
        }

        if (request.getAccessMode() == AccessMode.INVITE_ONLY && !Boolean.TRUE.equals(plan.getAllowInviteOnly())) {
            throw new ResponseStatusException(
                    HttpStatus.PAYMENT_REQUIRED,
                    "Invite-only rooms are available on Premium and Pro plans"
            );
        }

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

        BillingPlanResponse plan = billingService.getCurrentPlan(user);

        boolean canCustomize = Boolean.TRUE.equals(plan.getAllowRoomCustomization());
        boolean canUseInviteOnly = Boolean.TRUE.equals(plan.getAllowInviteOnly());

        if (request.getName() != null) {
            room.setName(cleanOrNull(request.getName()));
        }

        if (request.getDescription() != null) {
            room.setDescription(cleanOrNull(request.getDescription()));
        }

        if (request.getRoomType() != null) {
            room.setRoomType(request.getRoomType());
        }

        if (request.getAccessMode() != null) {
            if (request.getAccessMode() == AccessMode.INVITE_ONLY && !canUseInviteOnly) {
                throw new ResponseStatusException(
                        HttpStatus.PAYMENT_REQUIRED,
                        "Invite-only rooms are available on Premium and Pro plans"
                );
            }

            room.setAccessMode(request.getAccessMode());
        }

        if (request.getMaxParticipants() != null) {
            long current = roomParticipantRepository.countByRoomAndJoinStatus(room, JoinStatus.JOINED);

            if (request.getMaxParticipants() < current) {
                throw new RuntimeException("Cannot reduce maxParticipants below current participants");
            }

            if (request.getMaxParticipants() > plan.getMaxParticipants()) {
                throw new ResponseStatusException(
                        HttpStatus.PAYMENT_REQUIRED,
                        "Your current plan allows up to " + plan.getMaxParticipants() + " participants"
                );
            }

            room.setMaxParticipants(request.getMaxParticipants());
        }

        boolean wantsCustomization =
                request.getThemeColor() != null ||
                        request.getCoverImageUrl() != null ||
                        request.getBackgroundUrl() != null;

        if (wantsCustomization && !canCustomize) {
            throw new ResponseStatusException(
                    HttpStatus.PAYMENT_REQUIRED,
                    "Room customization is available on Premium and Pro plans"
            );
        }

        if (canCustomize) {
            if (request.getThemeColor() != null) {
                room.setThemeColor(cleanOrNull(request.getThemeColor()));
            }

            if (request.getCoverImageUrl() != null) {
                room.setCoverImageUrl(cleanOrNull(request.getCoverImageUrl()));
            }

            if (request.getBackgroundUrl() != null) {
                room.setBackgroundUrl(cleanOrNull(request.getBackgroundUrl()));
            }
        }

        return mapRoom(room);
    }

    private String cleanOrNull(String value) {
        if (value == null || value.trim().isBlank()) {
            return null;
        }

        return value.trim();
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
                sendParticipantsChanged(room.getRoomCode());
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
                sendParticipantsChanged(room.getRoomCode());
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
        sendParticipantsChanged(room.getRoomCode());
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
        sendParticipantsChanged(room.getRoomCode());
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
                        .avatarUrl(p.getUser().getAvatarUrl())
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
        sendRoomEvent(room.getRoomCode(), "room.kick", Map.of(
                "userId", target.getId()
        ));

        sendParticipantsChanged(room.getRoomCode());
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

        if (newHost.getJoinStatus() != JoinStatus.JOINED) {
            throw new RuntimeException("New host must be an active participant");
        }

        currentHost.setParticipantRole(ParticipantRole.VIEWER);
        currentHost.setCanControlPlayback(false);
        notificationService.createNotification(
                currentUser,
                NotificationType.HOST_TRANSFERRED,
                "Host transferred",
                "You transferred host rights to " + newHostUser.getUsername() + " in room: " + room.getName(),
                "ROOM",
                room.getId()
        );

        newHost.setParticipantRole(ParticipantRole.HOST);
        newHost.setCanControlPlayback(true);
        notificationService.createNotification(
                newHostUser,
                NotificationType.HOST_TRANSFERRED,
                "You are the new host",
                "Host rights were transferred to you in room: " + room.getName(),
                "ROOM",
                room.getId()
        );
        sendRoomEvent(room.getRoomCode(), "room.host-transferred", Map.of(
                "oldHostId", currentUser.getId(),
                "newHostId", newHostUser.getId()
        ));

        sendParticipantsChanged(room.getRoomCode());
    }

    private RoomResponse mapRoom(WatchRoom room) {
        long participantsCount = roomParticipantRepository.countByRoomAndJoinStatus(
                room,
                JoinStatus.JOINED
        );

        return RoomResponse.builder()
                .roomCode(room.getRoomCode())
                .name(room.getName())
                .description(room.getDescription())
                .ownerId(room.getOwner().getId())
                .ownerUsername(room.getOwner().getUsername())
                .roomType(room.getRoomType())
                .accessMode(room.getAccessMode())
                .maxParticipants(room.getMaxParticipants())
                .participantsCount(participantsCount)
                .active(room.getIsActive())
                .themeColor(room.getThemeColor())
                .coverImageUrl(room.getCoverImageUrl())
                .backgroundUrl(room.getBackgroundUrl())
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

        String oldCode = room.getRoomCode();
        String newCode = generateRoomCode();

        room.setRoomCode(newCode);

        sendRoomEvent(oldCode, "room.code-regenerated", Map.of(
                "oldCode", oldCode,
                "newCode", newCode
        ));

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

        if (host.getId().equals(userId)) {
            throw new RuntimeException("You cannot invite yourself");
        }

        User target = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        RoomParticipant existing = roomParticipantRepository.findByRoomAndUser(room, target)
                .orElse(null);

        if (existing != null) {
            if (existing.getJoinStatus() == JoinStatus.JOINED) {
                throw new RuntimeException("User is already in room");
            }

            if (existing.getJoinStatus() == JoinStatus.KICKED) {
                throw new RuntimeException("User was kicked from this room");
            }

            if (existing.getJoinStatus() == JoinStatus.INVITED) {
                return;
            }

            if (existing.getJoinStatus() == JoinStatus.LEFT) {
                existing.setJoinStatus(JoinStatus.INVITED);
                existing.setJoinedAt(LocalDateTime.now());
                existing.setLeftAt(null);
                existing.setParticipantRole(ParticipantRole.VIEWER);
                existing.setIsMuted(false);
                existing.setCanControlPlayback(false);

                roomParticipantRepository.save(existing);

                notificationService.createNotification(
                        target,
                        NotificationType.ROOM_INVITE,
                        "Room invitation",
                        "You were invited to room: " + room.getName(),
                        "ROOM",
                        room.getId()
                );

                return;
            }
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

        notificationService.createNotification(
                target,
                NotificationType.ROOM_INVITE,
                "Room invitation",
                "You were invited to room: " + room.getName(),
                "ROOM",
                room.getId()
        );
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
        notificationService.createNotification(
                room.getOwner(),
                NotificationType.INVITE_ACCEPTED,
                "Invitation accepted",
                user.getUsername() + " joined room: " + room.getName(),
                "ROOM",
                room.getId()
        );
    }

    @Transactional
    public void closeRoom(String code) {
        WatchRoom room = getRoomByCode(code);
        User user = getCurrentUser();

        getHostParticipant(room, user);

        room.setIsActive(false);
        room.setEndedAt(LocalDateTime.now());
    }
    @Transactional
    public void grantPlaybackControl(String code, Long userId) {
        WatchRoom room = getRoomByCode(code);
        User host = getCurrentUser();

        getHostParticipant(room, host);

        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        RoomParticipant target = roomParticipantRepository.findByRoomAndUser(room, targetUser)
                .orElseThrow(() -> new RuntimeException("User not in room"));

        target.setCanControlPlayback(true);
    }
    @Transactional
    public void revokePlaybackControl(String code, Long userId) {
        WatchRoom room = getRoomByCode(code);
        User host = getCurrentUser();

        getHostParticipant(room, host);

        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        RoomParticipant target = roomParticipantRepository.findByRoomAndUser(room, targetUser)
                .orElseThrow(() -> new RuntimeException("User not in room"));

        target.setCanControlPlayback(false);
    }

    private void sendRoomEvent(String roomCode, String type, Object data) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", type);
        payload.put("data", data);

        messagingTemplate.convertAndSend(
                "/topic/rooms/" + roomCode + "/events",
                (Object) payload
        );
    }

    private void sendParticipantsChanged(String roomCode) {
        Map<String, Object> data = new HashMap<>();
        data.put("roomCode", roomCode);

        System.out.println("SEND PARTICIPANTS CHANGED: " + roomCode);

        sendRoomEvent(roomCode, "room.participants-changed", data);
    }

    public List<RoomResponse> getMyRooms() {
        User user = getCurrentUser();

        return roomParticipantRepository
                .findAllByUserAndJoinStatus(user, JoinStatus.JOINED)
                .stream()
                .map(RoomParticipant::getRoom)
                .filter(room -> Boolean.TRUE.equals(room.getIsActive()))
                .distinct()
                .map(this::mapRoom)
                .toList();
    }
}