package com.example.watch_together.user.service;

import com.example.watch_together.friend.entity.Friendship;
import com.example.watch_together.friend.entity.FriendshipStatus;
import com.example.watch_together.friend.repository.FriendshipRepository;
import com.example.watch_together.user.dto.UpdateProfileRequest;
import com.example.watch_together.user.dto.UserProfileResponse;
import com.example.watch_together.user.dto.UserSearchResponse;
import com.example.watch_together.user.entity.User;
import com.example.watch_together.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserSearchService {

    private final UserRepository userRepository;
    private final FriendshipRepository friendshipRepository;

    public List<UserSearchResponse> searchUsers(String query, Principal principal) {
        User currentUser = getUserByPrincipal(principal);

        String normalizedQuery = query == null ? "" : query.trim();
        if (normalizedQuery.isBlank()) {
            return List.of();
        }

        List<User> users = userRepository
                .findTop20ByUsernameContainingIgnoreCaseOrDisplayNameContainingIgnoreCase(normalizedQuery, normalizedQuery);

        return users.stream()
                .filter(user -> !user.getId().equals(currentUser.getId()))
                .map(user -> mapUserWithRelation(currentUser, user))
                .toList();
    }

    private UserSearchResponse mapUserWithRelation(User currentUser, User otherUser) {
        String relationStatus = "NONE";

        Friendship direct = friendshipRepository.findByRequesterAndAddressee(currentUser, otherUser).orElse(null);
        Friendship reverse = friendshipRepository.findByRequesterAndAddressee(otherUser, currentUser).orElse(null);

        Friendship friendship = direct != null ? direct : reverse;

        if (friendship != null) {
            if (friendship.getStatus() == FriendshipStatus.ACCEPTED) {
                relationStatus = "FRIEND";
            } else if (friendship.getStatus() == FriendshipStatus.PENDING) {
                if (friendship.getRequester().getId().equals(currentUser.getId())) {
                    relationStatus = "OUTGOING_REQUEST";
                } else {
                    relationStatus = "INCOMING_REQUEST";
                }
            }
        }

        return UserSearchResponse.builder()
                .id(otherUser.getId())
                .username(otherUser.getUsername())
                .displayName(otherUser.getDisplayName())
                .avatarUrl(otherUser.getAvatarUrl())
                .relationStatus(relationStatus)
                .build();
    }

    private User getUserByPrincipal(Principal principal) {
        if (principal == null) {
            throw new RuntimeException("Unauthorized");
        }

        return userRepository.findByEmail(principal.getName())
                .or(() -> userRepository.findByUsername(principal.getName()))
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
    @Transactional
    public UserProfileResponse updateCurrentUser(UpdateProfileRequest request, Principal principal) {
        User user = getUserByPrincipal(principal);

        if (request.getDisplayName() != null && !request.getDisplayName().isBlank()) {
            user.setDisplayName(request.getDisplayName().trim());
        }

        if (request.getUsername() != null && !request.getUsername().isBlank()) {
            String newUsername = request.getUsername().trim();

            if (!newUsername.equalsIgnoreCase(user.getUsername())
                    && userRepository.existsByUsername(newUsername)) {
                throw new RuntimeException("Username is already taken");
            }

            user.setUsername(newUsername);
        }

        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            String newEmail = request.getEmail().trim().toLowerCase();

            if (!newEmail.equalsIgnoreCase(user.getEmail())
                    && userRepository.existsByEmail(newEmail)) {
                throw new RuntimeException("Email is already taken");
            }

            user.setEmail(newEmail);
        }

        User savedUser = userRepository.save(user);

        return mapToProfileResponse(savedUser);
    }
    private UserProfileResponse mapToProfileResponse(User user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .avatarUrl(user.getAvatarUrl())
                .build();
    }

}