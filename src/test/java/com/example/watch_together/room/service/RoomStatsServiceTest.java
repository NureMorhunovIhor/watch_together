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
import com.example.watch_together.room.entity.RoomParticipant;
import com.example.watch_together.room.entity.WatchRoom;
import com.example.watch_together.room.repository.RoomParticipantRepository;
import com.example.watch_together.room.repository.WatchRoomRepository;
import com.example.watch_together.user.entity.User;
import com.example.watch_together.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoomStatsServiceTest {

    @Mock
    private WatchRoomRepository watchRoomRepository;

    @Mock
    private RoomParticipantRepository roomParticipantRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private MessageReactionRepository messageReactionRepository;

    @Mock
    private WatchQueueRepository watchQueueRepository;

    @Mock
    private MediaItemRepository mediaItemRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BillingService billingService;

    @InjectMocks
    private RoomStatsService roomStatsService;

    private final Principal principal = () -> "igor@example.com";

    @Test
    void getRoomStats_shouldReturnStatisticsForProUser() {
        User user = User.builder()
                .id(1L)
                .username("igor")
                .email("igor@example.com")
                .build();

        WatchRoom room = WatchRoom.builder()
                .id(2L)
                .roomCode("ABC123")
                .name("Movie night")
                .currentMediaId(10L)
                .createdAt(LocalDateTime.now().minusHours(2))
                .startedAt(LocalDateTime.now().minusHours(1))
                .build();

        RoomParticipant participant = RoomParticipant.builder()
                .room(room)
                .user(user)
                .joinStatus(JoinStatus.JOINED)
                .build();

        MediaItem mediaItem = MediaItem.builder()
                .id(10L)
                .title("Interstellar")
                .build();

        BillingPlanResponse proPlan = BillingPlanResponse.builder()
                .id("PRO")
                .allowRoomStatistics(true)
                .build();

        when(watchRoomRepository.findByRoomCode("ABC123")).thenReturn(Optional.of(room));
        when(userRepository.findByEmail("igor@example.com")).thenReturn(Optional.of(user));
        when(roomParticipantRepository.findByRoomAndUser(room, user)).thenReturn(Optional.of(participant));
        when(billingService.getCurrentPlan(user)).thenReturn(proPlan);
        when(mediaItemRepository.findById(10L)).thenReturn(Optional.of(mediaItem));
        when(chatMessageRepository.countByRoom(room)).thenReturn(8L);
        when(messageReactionRepository.countByRoom(room)).thenReturn(3L);
        when(roomParticipantRepository.countByRoomAndJoinStatus(room, JoinStatus.JOINED)).thenReturn(2L);
        when(watchQueueRepository.countByRoom(room)).thenReturn(4L);
        when(watchQueueRepository.countByRoomAndStatus(room, QueueStatus.PLAYED)).thenReturn(1L);
        when(watchQueueRepository.countByRoomAndStatus(room, QueueStatus.REMOVED)).thenReturn(1L);

        RoomStatsResponse response = roomStatsService.getRoomStats("ABC123", principal);

        assertThat(response.getRoomCode()).isEqualTo("ABC123");
        assertThat(response.getRoomName()).isEqualTo("Movie night");
        assertThat(response.getMessagesCount()).isEqualTo(8L);
        assertThat(response.getReactionsCount()).isEqualTo(3L);
        assertThat(response.getCurrentParticipantsCount()).isEqualTo(2L);
        assertThat(response.getQueueItemsCount()).isEqualTo(4L);
        assertThat(response.getPlayedItemsCount()).isEqualTo(1L);
        assertThat(response.getRemovedItemsCount()).isEqualTo(1L);
        assertThat(response.getCurrentMediaId()).isEqualTo(10L);
        assertThat(response.getCurrentMediaTitle()).isEqualTo("Interstellar");
    }

    @Test
    void getRoomStats_shouldRejectUserWithoutProPlan() {
        User user = User.builder()
                .id(1L)
                .username("igor")
                .email("igor@example.com")
                .build();

        WatchRoom room = WatchRoom.builder()
                .id(2L)
                .roomCode("ABC123")
                .name("Movie night")
                .build();

        RoomParticipant participant = RoomParticipant.builder()
                .room(room)
                .user(user)
                .joinStatus(JoinStatus.JOINED)
                .build();

        BillingPlanResponse freePlan = BillingPlanResponse.builder()
                .id("FREE")
                .allowRoomStatistics(false)
                .build();

        when(watchRoomRepository.findByRoomCode("ABC123")).thenReturn(Optional.of(room));
        when(userRepository.findByEmail("igor@example.com")).thenReturn(Optional.of(user));
        when(roomParticipantRepository.findByRoomAndUser(room, user)).thenReturn(Optional.of(participant));
        when(billingService.getCurrentPlan(user)).thenReturn(freePlan);

        assertThatThrownBy(() -> roomStatsService.getRoomStats("ABC123", principal))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode())
                        .isEqualTo(HttpStatus.PAYMENT_REQUIRED));
    }

    @Test
    void getRoomStats_shouldRejectUserWhoIsNotParticipant() {
        User user = User.builder()
                .id(1L)
                .username("igor")
                .email("igor@example.com")
                .build();

        WatchRoom room = WatchRoom.builder()
                .id(2L)
                .roomCode("ABC123")
                .name("Movie night")
                .build();

        when(watchRoomRepository.findByRoomCode("ABC123")).thenReturn(Optional.of(room));
        when(userRepository.findByEmail("igor@example.com")).thenReturn(Optional.of(user));
        when(roomParticipantRepository.findByRoomAndUser(room, user)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roomStatsService.getRoomStats("ABC123", principal))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode())
                        .isEqualTo(HttpStatus.FORBIDDEN));
    }
}
