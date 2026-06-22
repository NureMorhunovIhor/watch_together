package com.example.watch_together.room.service;

import com.example.watch_together.billing.dto.BillingPlanResponse;
import com.example.watch_together.billing.service.BillingService;
import com.example.watch_together.chat.repository.ChatMessageRepository;
import com.example.watch_together.chat.repository.MessageReactionRepository;
import com.example.watch_together.media.entity.MediaItem;
import com.example.watch_together.media.repository.MediaItemRepository;
import com.example.watch_together.queue.entity.QueueStatus;
import com.example.watch_together.queue.repository.WatchQueueRepository;
import com.example.watch_together.room.dto.RoomStatsResponse;
import com.example.watch_together.room.entity.JoinStatus;
import com.example.watch_together.room.entity.WatchRoom;
import com.example.watch_together.room.repository.RoomParticipantRepository;
import com.example.watch_together.room.repository.WatchRoomRepository;
import com.example.watch_together.user.entity.User;
import com.example.watch_together.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;

@Service
@RequiredArgsConstructor
public class RoomStatsService {

    private final WatchRoomRepository watchRoomRepository;
    private final RoomParticipantRepository roomParticipantRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final MessageReactionRepository messageReactionRepository;
    private final WatchQueueRepository watchQueueRepository;
    private final MediaItemRepository mediaItemRepository;
    private final UserRepository userRepository;
    private final BillingService billingService;

    public RoomStatsResponse getRoomStats(String roomCode, Principal principal) {
        WatchRoom room = watchRoomRepository.findByRoomCode(roomCode)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Room not found"
                ));

        User user = getUserByPrincipal(principal);

        validateRoomMembership(room, user);
        validateStatsAllowed(user);

        String currentMediaTitle = null;

        if (room.getCurrentMediaId() != null) {
            currentMediaTitle = mediaItemRepository.findById(room.getCurrentMediaId())
                    .map(MediaItem::getTitle)
                    .orElse(null);
        }

        return RoomStatsResponse.builder()
                .roomCode(room.getRoomCode())
                .roomName(room.getName())
                .messagesCount(chatMessageRepository.countByRoom(room))
                .reactionsCount(messageReactionRepository.countByRoom(room))
                .currentParticipantsCount(
                        roomParticipantRepository.countByRoomAndJoinStatus(room, JoinStatus.JOINED)
                )
                .queueItemsCount(watchQueueRepository.countByRoom(room))
                .playedItemsCount(watchQueueRepository.countByRoomAndStatus(room, QueueStatus.PLAYED))
                .removedItemsCount(watchQueueRepository.countByRoomAndStatus(room, QueueStatus.REMOVED))
                .currentMediaId(room.getCurrentMediaId())
                .currentMediaTitle(currentMediaTitle)
                .createdAt(room.getCreatedAt())
                .startedAt(room.getStartedAt())
                .build();
    }

    private void validateStatsAllowed(User user) {
        BillingPlanResponse plan = billingService.getCurrentPlan(user);

        if (!Boolean.TRUE.equals(plan.getAllowRoomStatistics())) {
            throw new ResponseStatusException(
                    HttpStatus.PAYMENT_REQUIRED,
                    "Room statistics are available only on Pro plan"
            );
        }
    }

    private User getUserByPrincipal(Principal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }

        return userRepository.findByEmail(principal.getName())
                .or(() -> userRepository.findByUsername(principal.getName()))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"
                ));
    }

    private void validateRoomMembership(WatchRoom room, User user) {
        boolean joined = roomParticipantRepository.findByRoomAndUser(room, user)
                .filter(p -> p.getJoinStatus() == JoinStatus.JOINED)
                .isPresent();

        if (!joined) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You are not a participant of this room"
            );
        }
    }
}