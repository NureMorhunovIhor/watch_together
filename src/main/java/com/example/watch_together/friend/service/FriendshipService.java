package com.example.watch_together.friend.service;

import com.example.watch_together.friend.dto.FriendRequestResponse;
import com.example.watch_together.friend.dto.FriendResponse;
import com.example.watch_together.friend.entity.Friendship;
import com.example.watch_together.friend.entity.FriendshipStatus;
import com.example.watch_together.friend.repository.FriendshipRepository;
import com.example.watch_together.notification.entity.NotificationType;
import com.example.watch_together.notification.service.NotificationService;
import com.example.watch_together.user.entity.User;
import com.example.watch_together.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FriendshipService {

    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Transactional
    public FriendRequestResponse sendRequest(Long targetUserId, Principal principal) {
        User currentUser = getUserByPrincipal(principal);
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new RuntimeException("Target user not found"));

        if (currentUser.getId().equals(targetUser.getId())) {
            throw new RuntimeException("You cannot send a friend request to yourself");
        }

        friendshipRepository.findByRequesterAndAddressee(currentUser, targetUser)
                .ifPresent(f -> {
                    throw new RuntimeException("Friend request already exists");
                });

        friendshipRepository.findByRequesterAndAddressee(targetUser, currentUser)
                .ifPresent(f -> {
                    if (f.getStatus() == FriendshipStatus.PENDING) {
                        throw new RuntimeException("This user has already sent you a friend request");
                    }
                    if (f.getStatus() == FriendshipStatus.ACCEPTED) {
                        throw new RuntimeException("You are already friends");
                    }
                });

        Friendship friendship = Friendship.builder()
                .requester(currentUser)
                .addressee(targetUser)
                .status(FriendshipStatus.PENDING)
                .build();

        friendship = friendshipRepository.save(friendship);

        notificationService.createNotification(
                targetUser,
                NotificationType.FRIEND_REQUEST,
                "Friend request",
                currentUser.getUsername() + " sent you a friend request",
                "FRIENDSHIP",
                friendship.getId()
        );

        return map(friendship);
    }

    @Transactional
    public FriendRequestResponse acceptRequest(Long friendshipId, Principal principal) {
        User currentUser = getUserByPrincipal(principal);

        Friendship friendship = friendshipRepository.findByIdAndAddressee(friendshipId, currentUser)
                .orElseThrow(() -> new RuntimeException("Friend request not found"));

        if (friendship.getStatus() != FriendshipStatus.PENDING) {
            throw new RuntimeException("Friend request is not pending");
        }

        friendship.setStatus(FriendshipStatus.ACCEPTED);
        friendship.setRespondedAt(LocalDateTime.now());

        notificationService.createNotification(
                friendship.getRequester(),
                NotificationType.SYSTEM,
                "Friend request accepted",
                currentUser.getUsername() + " accepted your friend request",
                "FRIENDSHIP",
                friendship.getId()
        );

        return map(friendship);
    }

    @Transactional
    public FriendRequestResponse declineRequest(Long friendshipId, Principal principal) {
        User currentUser = getUserByPrincipal(principal);

        Friendship friendship = friendshipRepository.findByIdAndAddressee(friendshipId, currentUser)
                .orElseThrow(() -> new RuntimeException("Friend request not found"));

        if (friendship.getStatus() != FriendshipStatus.PENDING) {
            throw new RuntimeException("Friend request is not pending");
        }

        friendship.setStatus(FriendshipStatus.DECLINED);
        friendship.setRespondedAt(LocalDateTime.now());

        return map(friendship);
    }

    public List<FriendRequestResponse> getIncomingRequests(Principal principal) {
        User currentUser = getUserByPrincipal(principal);

        return friendshipRepository.findAllByAddresseeAndStatusOrderByCreatedAtDesc(currentUser, FriendshipStatus.PENDING)
                .stream()
                .map(this::map)
                .toList();
    }

    public List<FriendRequestResponse> getOutgoingRequests(Principal principal) {
        User currentUser = getUserByPrincipal(principal);

        return friendshipRepository.findAllByRequesterAndStatusOrderByCreatedAtDesc(currentUser, FriendshipStatus.PENDING)
                .stream()
                .map(this::map)
                .toList();
    }

    public List<FriendResponse> getFriends(Principal principal) {
        User currentUser = getUserByPrincipal(principal);

        List<Friendship> friendships = friendshipRepository.findAllByRequesterOrAddresseeAndStatus(
                currentUser, currentUser, FriendshipStatus.ACCEPTED
        );

        return friendships.stream()
                .map(friendship -> {
                    User friend = friendship.getRequester().getId().equals(currentUser.getId())
                            ? friendship.getAddressee()
                            : friendship.getRequester();

                    return FriendResponse.builder()
                            .userId(friend.getId())
                            .username(friend.getUsername())
                            .displayName(friend.getDisplayName())
                            .avatarUrl(friend.getAvatarUrl())
                            .build();
                })
                .toList();
    }

    @Transactional
    public void removeFriend(Long friendUserId, Principal principal) {
        User currentUser = getUserByPrincipal(principal);
        User friendUser = userRepository.findById(friendUserId)
                .orElseThrow(() -> new RuntimeException("Friend user not found"));

        Friendship direct = friendshipRepository.findByRequesterAndAddressee(currentUser, friendUser).orElse(null);
        Friendship reverse = friendshipRepository.findByRequesterAndAddressee(friendUser, currentUser).orElse(null);

        Friendship friendship = direct != null ? direct : reverse;

        if (friendship == null || friendship.getStatus() != FriendshipStatus.ACCEPTED) {
            throw new RuntimeException("Friendship not found");
        }

        friendshipRepository.delete(friendship);
    }

    private User getUserByPrincipal(Principal principal) {
        if (principal == null) {
            throw new RuntimeException("Unauthorized");
        }

        return userRepository.findByEmail(principal.getName())
                .or(() -> userRepository.findByUsername(principal.getName()))
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private FriendRequestResponse map(Friendship friendship) {
        return FriendRequestResponse.builder()
                .friendshipId(friendship.getId())
                .requesterId(friendship.getRequester().getId())
                .requesterUsername(friendship.getRequester().getUsername())
                .requesterDisplayName(friendship.getRequester().getDisplayName())
                .addresseeId(friendship.getAddressee().getId())
                .addresseeUsername(friendship.getAddressee().getUsername())
                .addresseeDisplayName(friendship.getAddressee().getDisplayName())
                .status(friendship.getStatus())
                .createdAt(friendship.getCreatedAt())
                .respondedAt(friendship.getRespondedAt())
                .build();
    }
}