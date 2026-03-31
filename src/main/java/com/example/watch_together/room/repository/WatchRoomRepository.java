package com.example.watch_together.room.repository;

import com.example.watch_together.room.entity.RoomType;
import com.example.watch_together.room.entity.WatchRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WatchRoomRepository extends JpaRepository<WatchRoom, Long> {
    Optional<WatchRoom> findByRoomCode(String roomCode);
    boolean existsByRoomCode(String roomCode);

    List<WatchRoom> findByRoomTypeAndIsActive(RoomType roomType, Boolean isActive);

    @Query("""
    SELECT r FROM WatchRoom r
    WHERE r.isActive = true
    AND LOWER(r.name) LIKE LOWER(CONCAT('%', :query, '%'))
""")
    List<WatchRoom> searchRooms(@Param("query") String query);
}