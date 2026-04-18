package com.example.watch_together.friend.repository;

import com.example.watch_together.friend.entity.Friendship;
import com.example.watch_together.friend.entity.FriendshipStatus;
import com.example.watch_together.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

    Optional<Friendship> findByRequesterAndAddressee(User requester, User addressee);

    Optional<Friendship> findByIdAndAddressee(Long id, User addressee);

    List<Friendship> findAllByAddresseeAndStatusOrderByCreatedAtDesc(User addressee, FriendshipStatus status);

    List<Friendship> findAllByRequesterAndStatusOrderByCreatedAtDesc(User requester, FriendshipStatus status);

    List<Friendship> findAllByRequesterOrAddresseeAndStatus(User requester, User addressee, FriendshipStatus status);
}