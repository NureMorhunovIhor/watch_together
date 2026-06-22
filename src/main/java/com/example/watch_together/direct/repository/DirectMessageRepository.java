package com.example.watch_together.direct.repository;

import com.example.watch_together.direct.entity.DirectMessage;
import com.example.watch_together.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DirectMessageRepository extends JpaRepository<DirectMessage, Long> {

    List<DirectMessage> findAllBySenderAndReceiverOrReceiverAndSenderOrderBySentAtAsc(
            User sender,
            User receiver,
            User receiver2,
            User sender2
    );
}