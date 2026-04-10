package com.example.watch_together.queue.entity;

import com.example.watch_together.room.entity.WatchRoom;
import com.example.watch_together.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "watch_queue")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WatchQueueItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private WatchRoom room;

    @Column(name = "media_id", nullable = false)
    private Long mediaId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "added_by")
    private User addedBy;

    @Column(name = "queue_order", nullable = false)
    private Integer queueOrder;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private QueueStatus status;

    @Column(name = "added_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime addedAt;

    @Column(name = "played_at")
    private LocalDateTime playedAt;
}