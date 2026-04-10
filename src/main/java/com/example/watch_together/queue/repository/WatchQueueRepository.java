package com.example.watch_together.queue.repository;

import com.example.watch_together.queue.entity.QueueStatus;
import com.example.watch_together.queue.entity.WatchQueueItem;
import com.example.watch_together.room.entity.WatchRoom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WatchQueueRepository extends JpaRepository<WatchQueueItem, Long> {

    List<WatchQueueItem> findAllByRoomOrderByQueueOrderAsc(WatchRoom room);

    List<WatchQueueItem> findAllByRoomAndStatusOrderByQueueOrderAsc(WatchRoom room, QueueStatus status);

    Optional<WatchQueueItem> findByIdAndRoom(Long id, WatchRoom room);

    Optional<WatchQueueItem> findFirstByRoomAndStatusOrderByQueueOrderAsc(WatchRoom room, QueueStatus status);

    int countByRoom(WatchRoom room);

    boolean existsByRoomAndMediaIdAndStatus(WatchRoom room, Long mediaId, QueueStatus status);
}