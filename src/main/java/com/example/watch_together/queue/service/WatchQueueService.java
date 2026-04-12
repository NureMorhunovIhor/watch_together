package com.example.watch_together.queue.service;

import com.example.watch_together.media.entity.MediaItem;
import com.example.watch_together.media.repository.MediaItemRepository;
import com.example.watch_together.playback.entity.PlaybackState;
import com.example.watch_together.playback.entity.PlaybackStatus;
import com.example.watch_together.playback.repository.PlaybackStateRepository;
import com.example.watch_together.queue.dto.AddToQueueRequest;
import com.example.watch_together.queue.dto.MoveQueueItemRequest;
import com.example.watch_together.queue.dto.QueueItemResponse;
import com.example.watch_together.queue.entity.QueueStatus;
import com.example.watch_together.queue.entity.WatchQueueItem;
import com.example.watch_together.queue.repository.WatchQueueRepository;
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
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WatchQueueService {

    private final WatchRoomRepository watchRoomRepository;
    private final RoomParticipantRepository roomParticipantRepository;
    private final UserRepository userRepository;
    private final MediaItemRepository mediaItemRepository;
    private final WatchQueueRepository watchQueueRepository;
    private final PlaybackStateRepository playbackStateRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public QueueItemResponse addToQueue(String roomCode, AddToQueueRequest request, Principal principal) {
        WatchRoom room = getActiveRoomByCode(roomCode);
        User user = getUserByPrincipal(principal);

        validateRoomMembership(room, user);

        if (request.getMediaId() == null) {
            throw new RuntimeException("Media ID is required");
        }

        MediaItem media = mediaItemRepository.findById(request.getMediaId())
                .orElseThrow(() -> new RuntimeException("Media not found"));

        int nextOrder = getNextQueueOrder(room);

        WatchQueueItem item = WatchQueueItem.builder()
                .room(room)
                .mediaId(media.getId())
                .addedBy(user)
                .queueOrder(nextOrder)
                .status(QueueStatus.QUEUED)
                .build();

        item = watchQueueRepository.save(item);

        broadcastQueue(room);

        return mapItem(item, media);
    }

    public List<QueueItemResponse> getQueue(String roomCode, Principal principal) {
        WatchRoom room = getActiveRoomByCode(roomCode);
        User user = getUserByPrincipal(principal);

        validateRoomMembership(room, user);

        return watchQueueRepository.findAllByRoomOrderByQueueOrderAsc(room)
                .stream()
                .map(this::mapItem)
                .toList();
    }

    @Transactional
    public void removeFromQueue(String roomCode, Long queueItemId, Principal principal) {
        WatchRoom room = getActiveRoomByCode(roomCode);
        User user = getUserByPrincipal(principal);

        validateRoomMembership(room, user);
        validatePlaybackPermission(room, user);

        WatchQueueItem item = watchQueueRepository.findByIdAndRoom(queueItemId, room)
                .orElseThrow(() -> new RuntimeException("Queue item not found"));

        QueueStatus oldStatus = item.getStatus();
        item.setStatus(QueueStatus.REMOVED);

        if (oldStatus == QueueStatus.PLAYING
                && room.getCurrentMediaId() != null
                && room.getCurrentMediaId().equals(item.getMediaId())) {
            room.setCurrentMediaId(null);
        }

        // Не перенумеровываем все подряд, чтобы не ловить конфликтов на уникальном индексе.
        broadcastQueue(room);
    }

    @Transactional
    public QueueItemResponse playQueueItem(String roomCode, Long queueItemId, Principal principal) {
        WatchRoom room = getActiveRoomByCode(roomCode);
        User user = getUserByPrincipal(principal);

        validateRoomMembership(room, user);
        validatePlaybackPermission(room, user);

        WatchQueueItem item = watchQueueRepository.findByIdAndRoom(queueItemId, room)
                .orElseThrow(() -> new RuntimeException("Queue item not found"));

        MediaItem media = mediaItemRepository.findById(item.getMediaId())
                .orElseThrow(() -> new RuntimeException("Media not found"));

        markCurrentlyPlayingAsPlayed(room);

        item.setStatus(QueueStatus.PLAYING);
        item.setPlayedAt(LocalDateTime.now());

        room.setCurrentMediaId(media.getId());

        PlaybackState state = playbackStateRepository.findByRoom(room)
                .orElseGet(() -> PlaybackState.builder()
                        .room(room)
                        .mediaId(media.getId())
                        .playbackStatus(PlaybackStatus.PAUSED)
                        .currentPositionSeconds(0)
                        .playbackSpeed(BigDecimal.valueOf(1.00))
                        .lastSyncedAt(LocalDateTime.now())
                        .build());

        state.setMediaId(media.getId());
        state.setPlaybackStatus(PlaybackStatus.PAUSED);
        state.setCurrentPositionSeconds(0);
        state.setLastActionBy(user);
        state.setLastSyncedAt(LocalDateTime.now());

        playbackStateRepository.save(state);

        broadcastQueue(room);

        return mapItem(item, media);
    }

    @Transactional
    public QueueItemResponse playNext(String roomCode, Principal principal) {
        WatchRoom room = getActiveRoomByCode(roomCode);
        User user = getUserByPrincipal(principal);

        validateRoomMembership(room, user);
        validatePlaybackPermission(room, user);

        markCurrentlyPlayingAsPlayed(room);

        WatchQueueItem next = watchQueueRepository.findFirstByRoomAndStatusOrderByQueueOrderAsc(room, QueueStatus.QUEUED)
                .orElseThrow(() -> new RuntimeException("No next media in queue"));

        MediaItem media = mediaItemRepository.findById(next.getMediaId())
                .orElseThrow(() -> new RuntimeException("Media not found"));

        next.setStatus(QueueStatus.PLAYING);
        next.setPlayedAt(LocalDateTime.now());

        room.setCurrentMediaId(media.getId());

        PlaybackState state = playbackStateRepository.findByRoom(room)
                .orElseGet(() -> PlaybackState.builder()
                        .room(room)
                        .mediaId(media.getId())
                        .playbackStatus(PlaybackStatus.PAUSED)
                        .currentPositionSeconds(0)
                        .playbackSpeed(BigDecimal.valueOf(1.00))
                        .lastSyncedAt(LocalDateTime.now())
                        .build());

        state.setMediaId(media.getId());
        state.setPlaybackStatus(PlaybackStatus.PAUSED);
        state.setCurrentPositionSeconds(0);
        state.setLastActionBy(user);
        state.setLastSyncedAt(LocalDateTime.now());

        playbackStateRepository.save(state);

        broadcastQueue(room);

        return mapItem(next, media);
    }

    @Transactional
    public List<QueueItemResponse> moveQueueItem(String roomCode, Long queueItemId, MoveQueueItemRequest request, Principal principal) {
        WatchRoom room = getActiveRoomByCode(roomCode);
        User user = getUserByPrincipal(principal);

        validateRoomMembership(room, user);
        validatePlaybackPermission(room, user);

        if (request.getNewOrder() == null) {
            throw new RuntimeException("New order is required");
        }

        List<WatchQueueItem> queuedItems = new ArrayList<>(
                watchQueueRepository.findAllByRoomOrderByQueueOrderAsc(room)
                        .stream()
                        .filter(i -> i.getStatus() == QueueStatus.QUEUED)
                        .toList()
        );

        WatchQueueItem target = watchQueueRepository.findByIdAndRoom(queueItemId, room)
                .orElseThrow(() -> new RuntimeException("Queue item not found"));

        if (target.getStatus() != QueueStatus.QUEUED) {
            throw new RuntimeException("Only QUEUED items can be moved");
        }

        int newOrder = request.getNewOrder();
        if (newOrder < 1 || newOrder > queuedItems.size()) {
            throw new RuntimeException("Invalid new order");
        }

        queuedItems.removeIf(i -> i.getId().equals(target.getId()));
        queuedItems.add(newOrder - 1, target);

        /*
         * Важно:
         * чтобы не ловить duplicate key на уникальном (room_id, queue_order),
         * сначала временно уводим queue_order у QUEUED-элементов в безопасную зону.
         */
        int tempBase = getNextQueueOrder(room) + 1000;

        for (int i = 0; i < queuedItems.size(); i++) {
            queuedItems.get(i).setQueueOrder(tempBase + i);
        }

        watchQueueRepository.flush();

        for (int i = 0; i < queuedItems.size(); i++) {
            queuedItems.get(i).setQueueOrder(i + 1);
        }

        watchQueueRepository.flush();

        broadcastQueue(room);

        return queuedItems.stream().map(this::mapItem).toList();
    }

    private void markCurrentlyPlayingAsPlayed(WatchRoom room) {
        List<WatchQueueItem> all = watchQueueRepository.findAllByRoomOrderByQueueOrderAsc(room);
        for (WatchQueueItem item : all) {
            if (item.getStatus() == QueueStatus.PLAYING) {
                item.setStatus(QueueStatus.PLAYED);
            }
        }
    }

    private int getNextQueueOrder(WatchRoom room) {
        Integer maxOrder = watchQueueRepository.findMaxQueueOrderByRoom(room);
        return (maxOrder == null ? 0 : maxOrder) + 1;
    }

    private void broadcastQueue(WatchRoom room) {
        List<QueueItemResponse> queue = watchQueueRepository.findAllByRoomOrderByQueueOrderAsc(room)
                .stream()
                .map(this::mapItem)
                .toList();

        messagingTemplate.convertAndSend("/topic/rooms/" + room.getRoomCode() + "/queue", queue);
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
            throw new RuntimeException("You cannot manage queue in this room");
        }
    }

    private QueueItemResponse mapItem(WatchQueueItem item) {
        MediaItem media = mediaItemRepository.findById(item.getMediaId())
                .orElseThrow(() -> new RuntimeException("Media not found"));
        return mapItem(item, media);
    }

    private QueueItemResponse mapItem(WatchQueueItem item, MediaItem media) {
        return QueueItemResponse.builder()
                .queueItemId(item.getId())
                .roomCode(item.getRoom().getRoomCode())
                .mediaId(media.getId())
                .mediaTitle(media.getTitle())
                .sourceUrl(media.getSourceUrl())
                .queueOrder(item.getQueueOrder())
                .status(item.getStatus())
                .addedByUserId(item.getAddedBy() != null ? item.getAddedBy().getId() : null)
                .addedByUsername(item.getAddedBy() != null ? item.getAddedBy().getUsername() : null)
                .addedAt(item.getAddedAt())
                .playedAt(item.getPlayedAt())
                .build();
    }
}