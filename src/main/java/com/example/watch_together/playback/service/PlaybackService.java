package com.example.watch_together.playback.service;

import com.example.watch_together.playback.dto.*;
import com.example.watch_together.playback.entity.*;
import com.example.watch_together.playback.repository.PlaybackEventRepository;
import com.example.watch_together.playback.repository.PlaybackStateRepository;
import com.example.watch_together.room.entity.JoinStatus;
import com.example.watch_together.room.entity.RoomParticipant;
import com.example.watch_together.room.entity.WatchRoom;
import com.example.watch_together.room.repository.RoomParticipantRepository;
import com.example.watch_together.room.repository.WatchRoomRepository;
import com.example.watch_together.user.entity.User;
import com.example.watch_together.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PlaybackService {

    private final WatchRoomRepository watchRoomRepository;
    private final RoomParticipantRepository roomParticipantRepository;
    private final UserRepository userRepository;
    private final PlaybackStateRepository playbackStateRepository;
    private final PlaybackEventRepository playbackEventRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public PlaybackStateResponse handleCommand(PlaybackCommandRequest request, Principal principal) {
        WatchRoom room = getActiveRoomByCode(request.getRoomCode());
        User user = getUserByPrincipal(principal);

        validateRoomMembership(room, user);
        validatePlaybackPermission(room, user);

        PlaybackState state = playbackStateRepository.findByRoom(room)
                .orElseGet(() -> PlaybackState.builder()
                        .room(room)
                        .mediaId(request.getMediaId() != null ? request.getMediaId() : 1L)
                        .playbackStatus(PlaybackStatus.PAUSED)
                        .currentPositionSeconds(0)
                        .playbackSpeed(BigDecimal.valueOf(1.00))
                        .lastSyncedAt(LocalDateTime.now())
                        .build());

        Integer oldPosition = state.getCurrentPositionSeconds();

        String action = request.getAction();

        if (action == null) {
            throw new RuntimeException("Action is required");
        }

        switch (action.toUpperCase()) {
            case "PLAY" -> {
                if (request.getPositionSeconds() != null) {
                    state.setCurrentPositionSeconds(request.getPositionSeconds());
                }
                if (request.getMediaId() != null) {
                    state.setMediaId(request.getMediaId());
                }
                state.setPlaybackStatus(PlaybackStatus.PLAYING);
                saveEvent(room, user, state.getMediaId(), PlaybackEventType.PLAY, oldPosition, state.getCurrentPositionSeconds(), null);
            }
            case "PAUSE" -> {
                if (request.getPositionSeconds() != null) {
                    state.setCurrentPositionSeconds(request.getPositionSeconds());
                }
                state.setPlaybackStatus(PlaybackStatus.PAUSED);
                saveEvent(room, user, state.getMediaId(), PlaybackEventType.PAUSE, oldPosition, state.getCurrentPositionSeconds(), null);
            }
            case "SEEK" -> {
                if (request.getPositionSeconds() == null) {
                    throw new RuntimeException("Position is required for SEEK");
                }
                state.setCurrentPositionSeconds(request.getPositionSeconds());
                saveEvent(room, user, state.getMediaId(), PlaybackEventType.SEEK, oldPosition, state.getCurrentPositionSeconds(), null);
            }
            case "STOP" -> {
                state.setPlaybackStatus(PlaybackStatus.STOPPED);
                state.setCurrentPositionSeconds(0);
                saveEvent(room, user, state.getMediaId(), PlaybackEventType.STOP, oldPosition, 0, null);
            }
            case "SPEED_CHANGE" -> {
                if (request.getPlaybackSpeed() == null) {
                    throw new RuntimeException("Playback speed is required");
                }
                state.setPlaybackSpeed(request.getPlaybackSpeed());
                saveEvent(room, user, state.getMediaId(), PlaybackEventType.SPEED_CHANGE, oldPosition, oldPosition,
                        "{\"speed\":\"" + request.getPlaybackSpeed() + "\"}");
            }
            case "SYNC" -> {
                if (request.getPositionSeconds() != null) {
                    state.setCurrentPositionSeconds(request.getPositionSeconds());
                }
                saveEvent(room, user, state.getMediaId(), PlaybackEventType.SYNC, oldPosition, state.getCurrentPositionSeconds(), null);
            }
            default -> throw new RuntimeException("Unsupported action: " + action);
        }

        state.setLastActionBy(user);
        state.setLastSyncedAt(LocalDateTime.now());

        state = playbackStateRepository.save(state);

        PlaybackStateResponse response = mapState(state);
        broadcastState(room.getRoomCode(), response);

        return response;
    }

    public PlaybackStateResponse getCurrentState(String roomCode, Principal principal) {
        WatchRoom room = getActiveRoomByCode(roomCode);
        User user = getUserByPrincipal(principal);

        validateRoomMembership(room, user);

        PlaybackState state = playbackStateRepository.findByRoom(room)
                .orElseThrow(() -> new RuntimeException("Playback state not found"));

        return mapState(state);
    }

    public List<PlaybackEventResponse> getPlaybackEvents(String roomCode, Principal principal) {
        WatchRoom room = getActiveRoomByCode(roomCode);
        User user = getUserByPrincipal(principal);

        validateRoomMembership(room, user);

        return playbackEventRepository.findAllByRoomOrderByCreatedAtDesc(room)
                .stream()
                .map(this::mapEvent)
                .toList();
    }

    private void saveEvent(WatchRoom room,
                           User user,
                           Long mediaId,
                           PlaybackEventType type,
                           Integer fromPosition,
                           Integer toPosition,
                           String metadata) {
        PlaybackEvent event = PlaybackEvent.builder()
                .room(room)
                .user(user)
                .mediaId(mediaId)
                .eventType(type)
                .fromPositionSeconds(fromPosition)
                .toPositionSeconds(toPosition)
                .metadata(metadata)
                .build();

        playbackEventRepository.save(event);
    }

    private void broadcastState(String roomCode, PlaybackStateResponse response) {
        messagingTemplate.convertAndSend("/topic/rooms/" + roomCode + "/playback", response);
    }

    private WatchRoom getActiveRoomByCode(String roomCode) {
        WatchRoom room = watchRoomRepository.findByRoomCode(roomCode)
                .orElseThrow(() -> new RuntimeException("Room not found"));

        if (!Boolean.TRUE.equals(room.getIsActive())) {
            throw new RuntimeException("Room is closed");
        }

        return room;
    }

    private User getUserByPrincipal(Principal principal) {
        if (principal == null) {
            throw new RuntimeException("Unauthorized");
        }

        return userRepository.findByEmail(principal.getName())
                .or(() -> userRepository.findByUsername(principal.getName()))
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private void validateRoomMembership(WatchRoom room, User user) {
        RoomParticipant participant = roomParticipantRepository.findByRoomAndUser(room, user)
                .orElseThrow(() -> new RuntimeException("You are not in this room"));

        if (participant.getJoinStatus() != JoinStatus.JOINED) {
            throw new RuntimeException("You are not an active participant of this room");
        }
    }

    private void validatePlaybackPermission(WatchRoom room, User user) {
        RoomParticipant participant = roomParticipantRepository.findByRoomAndUser(room, user)
                .orElseThrow(() -> new RuntimeException("You are not in this room"));

        if (!Boolean.TRUE.equals(participant.getCanControlPlayback())
                && participant.getParticipantRole().name().equals("VIEWER")) {
            throw new RuntimeException("You cannot control playback in this room");
        }
    }

    private PlaybackStateResponse mapState(PlaybackState state) {
        return PlaybackStateResponse.builder()
                .roomCode(state.getRoom().getRoomCode())
                .mediaId(state.getMediaId())
                .playbackStatus(state.getPlaybackStatus())
                .currentPositionSeconds(state.getCurrentPositionSeconds())
                .playbackSpeed(state.getPlaybackSpeed())
                .lastActionByUserId(state.getLastActionBy() != null ? state.getLastActionBy().getId() : null)
                .lastActionByUsername(state.getLastActionBy() != null ? state.getLastActionBy().getUsername() : null)
                .lastSyncedAt(state.getLastSyncedAt())
                .build();
    }

    private PlaybackEventResponse mapEvent(PlaybackEvent event) {
        return PlaybackEventResponse.builder()
                .id(event.getId())
                .roomCode(event.getRoom().getRoomCode())
                .userId(event.getUser() != null ? event.getUser().getId() : null)
                .username(event.getUser() != null ? event.getUser().getUsername() : null)
                .mediaId(event.getMediaId())
                .eventType(event.getEventType())
                .fromPositionSeconds(event.getFromPositionSeconds())
                .toPositionSeconds(event.getToPositionSeconds())
                .metadata(event.getMetadata())
                .createdAt(event.getCreatedAt())
                .build();
    }
}