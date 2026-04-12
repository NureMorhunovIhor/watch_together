package com.example.watch_together.queue.repository;

import com.example.watch_together.queue.entity.QueueStatus;
import com.example.watch_together.queue.entity.WatchQueueItem;
import com.example.watch_together.room.entity.WatchRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WatchQueueRepository extends JpaRepository<WatchQueueItem, Long> {

    List<WatchQueueItem> findAllByRoomOrderByQueueOrderAsc(WatchRoom room);

    List<WatchQueueItem> findAllByRoomAndStatusOrderByQueueOrderAsc(WatchRoom room, QueueStatus status);

    Optional<WatchQueueItem> findByIdAndRoom(Long id, WatchRoom room);

    Optional<WatchQueueItem> findFirstByRoomAndStatusOrderByQueueOrderAsc(WatchRoom room, QueueStatus status);

    @Query("select coalesce(max(w.queueOrder), 0) from WatchQueueItem w where w.room = :room")
    Integer findMaxQueueOrderByRoom(@Param("room") WatchRoom room);
}