package com.example.watch_together.direct.service;

import com.example.watch_together.billing.dto.BillingPlanResponse;
import com.example.watch_together.billing.service.BillingService;
import com.example.watch_together.direct.dto.DirectMessageRequest;
import com.example.watch_together.direct.dto.DirectMessageResponse;
import com.example.watch_together.direct.entity.DirectMessage;
import com.example.watch_together.direct.entity.DirectMessageType;
import com.example.watch_together.direct.repository.DirectMessageRepository;
import com.example.watch_together.user.entity.User;
import com.example.watch_together.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DirectMessageService {

    private final DirectMessageRepository directMessageRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final BillingService billingService;

    public List<DirectMessageResponse> getMessages(Long friendId, Principal principal) {
        User me = getUser(principal);
        User friend = userRepository.findById(friendId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        validateFriendship(me, friend);

        return directMessageRepository
                .findAllBySenderAndReceiverOrReceiverAndSenderOrderBySentAtAsc(
                        me,
                        friend,
                        me,
                        friend
                )
                .stream()
                .map(this::map)
                .toList();
    }

    @Transactional
    public DirectMessageResponse sendMessage(Long friendId,
                                             DirectMessageRequest request,
                                             Principal principal) {
        User me = getUser(principal);
        User friend = userRepository.findById(friendId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        validateFriendship(me, friend);

        DirectMessageType messageType = request.getMessageType() != null
                ? request.getMessageType()
                : DirectMessageType.TEXT;

        if (request.getContent() == null || request.getContent().trim().isBlank()) {
            throw new RuntimeException("Message content is required");
        }

        String content = request.getContent().trim();

        if (messageType == DirectMessageType.STICKER) {
            validateSticker(content, me);
        }

        DirectMessage message = DirectMessage.builder()
                .sender(me)
                .receiver(friend)
                .content(content)
                .messageType(messageType)
                .read(false)
                .build();

        message = directMessageRepository.save(message);

        DirectMessageResponse response = map(message);

        sendDirectEvent(friend.getUsername(), response);
        sendDirectEvent(me.getUsername(), response);

        return response;
    }

    private void validateSticker(String stickerId, User user) {
        if (!DirectStickerRegistry.exists(stickerId)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Unknown sticker"
            );
        }

        if (DirectStickerRegistry.isPremium(stickerId)) {
            BillingPlanResponse plan = billingService.getCurrentPlan(user);

            String planId = plan.getId() != null
                    ? plan.getId().toUpperCase()
                    : "FREE";

            if ("FREE".equals(planId)) {
                throw new ResponseStatusException(
                        HttpStatus.PAYMENT_REQUIRED,
                        "Premium stickers are available only for Premium and Pro users"
                );
            }
        }
    }
    private void sendDirectEvent(String username, DirectMessageResponse message) {
        Map<String, Object> payload = Map.of(
                "type", "direct.message",
                "data", message
        );

        messagingTemplate.convertAndSend(
                "/topic/users/" + username + "/direct-messages",
                (Object) payload
        );
    }

    private User getUser(Principal principal) {
        if (principal == null) {
            throw new RuntimeException("Unauthorized");
        }

        return userRepository.findByEmail(principal.getName())
                .or(() -> userRepository.findByUsername(principal.getName()))
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private void validateFriendship(User me, User friend) {
        if (me.getId().equals(friend.getId())) {
            throw new RuntimeException("You cannot message yourself");
        }
    }

    private DirectMessageResponse map(DirectMessage message) {
        User sender = message.getSender();
        User receiver = message.getReceiver();

        return DirectMessageResponse.builder()
                .id(message.getId())
                .senderId(sender.getId())
                .senderUsername(sender.getUsername())
                .senderDisplayName(sender.getDisplayName())
                .senderAvatarUrl(sender.getAvatarUrl())
                .receiverId(receiver.getId())
                .receiverUsername(receiver.getUsername())
                .receiverDisplayName(receiver.getDisplayName())
                .receiverAvatarUrl(receiver.getAvatarUrl())
                .content(message.getContent())
                .messageType(message.getMessageType() != null ? message.getMessageType().name() : "TEXT")
                .read(message.getRead())
                .sentAt(message.getSentAt())
                .build();
    }
}