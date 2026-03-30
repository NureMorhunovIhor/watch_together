package com.example.watch_together.room.entity;

import com.example.watch_together.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "room_participants",
        uniqueConstraints = @UniqueConstraint(name = "uk_room_participant", columnNames = {"room_id", "user_id"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private WatchRoom room;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "participant_role", nullable = false, length = 20)
    private ParticipantRole participantRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "join_status", nullable = false, length = 20)
    private JoinStatus joinStatus;

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    @Column(name = "left_at")
    private LocalDateTime leftAt;

    @Column(name = "is_muted", nullable = false)
    private Boolean isMuted;

    @Column(name = "can_control_playback", nullable = false)
    private Boolean canControlPlayback;
}