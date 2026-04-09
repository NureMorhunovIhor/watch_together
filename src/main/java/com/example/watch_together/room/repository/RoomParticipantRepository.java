package com.example.watch_together.room.repository;

import com.example.watch_together.room.entity.RoomParticipant;
import com.example.watch_together.room.entity.WatchRoom;
import com.example.watch_together.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoomParticipantRepository extends JpaRepository<RoomParticipant, Long> {
    Optional<RoomParticipant> findByRoomAndUser(WatchRoom room, User user);
    List<RoomParticipant> findAllByRoomAndJoinStatus(WatchRoom room, com.example.watch_together.room.entity.JoinStatus joinStatus);
    long countByRoomAndJoinStatus(WatchRoom room, com.example.watch_together.room.entity.JoinStatus joinStatus);

}