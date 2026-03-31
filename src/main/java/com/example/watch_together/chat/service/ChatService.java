package com.example.watch_together.chat.service;

import com.example.watch_together.chat.dto.*;
import com.example.watch_together.chat.entity.*;
import com.example.watch_together.chat.repository.ChatMessageRepository;
import com.example.watch_together.chat.repository.MessageReactionRepository;
import com.example.watch_together.room.entity.JoinStatus;
import com.example.watch_together.room.entity.RoomParticipant;
import com.example.watch_together.room.entity.RoomSettings;
import com.example.watch_together.room.entity.WatchRoom;
import com.example.watch_together.room.repository.RoomParticipantRepository;
import com.example.watch_together.room.repository.RoomSettingsRepository;
import com.example.watch_together.room.repository.WatchRoomRepository;
import com.example.watch_together.user.entity.User;
import com.example.watch_together.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final WatchRoomRepository watchRoomRepository;
    private final RoomParticipantRepository roomParticipantRepository;
    private final RoomSettingsRepository roomSettingsRepository;
    private final UserRepository userRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final MessageReactionRepository messageReactionRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public ChatMessageResponse sendMessage(ChatMessageRequest request, Principal principal) {
        WatchRoom room = getActiveRoomByCode(request.getRoomCode());
        User user = getUserByPrincipal(principal);

        validateRoomMembership(room, user);
        validateChatAllowed(room);

        ChatMessage replyTo = null;
        if (request.getReplyToMessageId() != null) {
            replyTo = chatMessageRepository.findByIdAndRoom(request.getReplyToMessageId(), room)
                    .orElseThrow(() -> new RuntimeException("Reply message not found in this room"));
        }

        ChatMessage message = ChatMessage.builder()
                .room(room)
                .sender(user)
                .messageType(MessageType.TEXT)
                .content(request.getContent())
                .replyToMessage(replyTo)
                .isEdited(false)
                .isDeleted(false)
                .build();

        message = chatMessageRepository.save(message);

        ChatMessageResponse response = mapMessage(message);
        broadcast(room.getRoomCode(), response);

        return response;
    }

    public List<ChatMessageResponse> getRoomMessages(String roomCode, Principal principal) {
        WatchRoom room = getActiveRoomByCode(roomCode);
        User user = getUserByPrincipal(principal);

        validateRoomMembership(room, user);

        return chatMessageRepository.findAllByRoomOrderBySentAtAsc(room)
                .stream()
                .map(this::mapMessage)
                .toList();
    }

    @Transactional
    public ChatMessageResponse editMessage(Long messageId, EditMessageRequest request, Principal principal) {
        User user = getUserByPrincipal(principal);
        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));

        validateRoomMembership(message.getRoom(), user);

        if (!message.getSender().getId().equals(user.getId())) {
            throw new RuntimeException("You can edit only your own messages");
        }

        if (Boolean.TRUE.equals(message.getIsDeleted())) {
            throw new RuntimeException("Cannot edit deleted message");
        }

        message.setContent(request.getContent());
        message.setIsEdited(true);
        message.setEditedAt(LocalDateTime.now());

        ChatMessageResponse response = mapMessage(message);
        broadcast(message.getRoom().getRoomCode(), response);

        return response;
    }

    @Transactional
    public void deleteMessage(Long messageId, Principal principal) {
        User user = getUserByPrincipal(principal);
        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));

        validateRoomMembership(message.getRoom(), user);

        if (!message.getSender().getId().equals(user.getId())) {
            throw new RuntimeException("You can delete only your own messages");
        }

        message.setIsDeleted(true);
        message.setContent("[deleted]");
        message.setEditedAt(LocalDateTime.now());

        ChatMessageResponse response = mapMessage(message);
        broadcast(message.getRoom().getRoomCode(), response);
    }

    @Transactional
    public ChatMessageResponse addReaction(Long messageId, ReactionRequest request, Principal principal) {
        User user = getUserByPrincipal(principal);
        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));

        validateRoomMembership(message.getRoom(), user);

        boolean exists = messageReactionRepository.findByMessageAndUserAndReactionType(
                message, user, request.getReactionType()
        ).isPresent();

        if (!exists) {
            MessageReaction reaction = MessageReaction.builder()
                    .message(message)
                    .user(user)
                    .reactionType(request.getReactionType())
                    .build();

            messageReactionRepository.save(reaction);
        }

        ChatMessageResponse response = mapMessage(message);
        broadcast(message.getRoom().getRoomCode(), response);

        return response;
    }

    @Transactional
    public ChatMessageResponse removeReaction(Long messageId, ReactionRequest request, Principal principal) {
        User user = getUserByPrincipal(principal);
        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));

        validateRoomMembership(message.getRoom(), user);

        messageReactionRepository.deleteByMessageAndUserAndReactionType(message, user, request.getReactionType());

        ChatMessageResponse response = mapMessage(message);
        broadcast(message.getRoom().getRoomCode(), response);

        return response;
    }

    private void broadcast(String roomCode, ChatMessageResponse response) {
        messagingTemplate.convertAndSend("/topic/rooms/" + roomCode + "/chat", response);
    }

    private WatchRoom getActiveRoomByCode(String roomCode) {
        WatchRoom room = watchRoomRepository.findByRoomCode(roomCode)
                .orElseThrow(() -> new RuntimeException("Room not found"));

        if (!Boolean.TRUE.equals(room.getIsActive())) {
            throw new RuntimeException("Room is closed");
        }

        return room;
    }

    private User getUserByPrincipal(Principal principal) {
        if (principal == null) {
            throw new RuntimeException("Unauthorized");
        }

        return userRepository.findByEmail(principal.getName())
                .or(() -> userRepository.findByUsername(principal.getName()))
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private void validateRoomMembership(WatchRoom room, User user) {
        RoomParticipant participant = roomParticipantRepository.findByRoomAndUser(room, user)
                .orElseThrow(() -> new RuntimeException("You are not in this room"));

        if (participant.getJoinStatus() != JoinStatus.JOINED) {
            throw new RuntimeException("You are not an active participant of this room");
        }
    }

    private void validateChatAllowed(WatchRoom room) {
        RoomSettings settings = roomSettingsRepository.findByRoom(room)
                .orElseThrow(() -> new RuntimeException("Room settings not found"));

        if (!Boolean.TRUE.equals(settings.getAllowChat())) {
            throw new RuntimeException("Chat is disabled in this room");
        }
    }

    private ChatMessageResponse mapMessage(ChatMessage message) {
        List<MessageReactionResponse> reactions = messageReactionRepository.findAllByMessage(message)
                .stream()
                .map(r -> MessageReactionResponse.builder()
                        .userId(r.getUser().getId())
                        .username(r.getUser().getUsername())
                        .reactionType(r.getReactionType())
                        .build())
                .toList();

        return ChatMessageResponse.builder()
                .id(message.getId())
                .roomCode(message.getRoom().getRoomCode())
                .senderId(message.getSender().getId())
                .senderUsername(message.getSender().getUsername())
                .senderDisplayName(message.getSender().getDisplayName())
                .messageType(message.getMessageType())
                .content(message.getContent())
                .replyToMessageId(message.getReplyToMessage() != null ? message.getReplyToMessage().getId() : null)
                .edited(message.getIsEdited())
                .deleted(message.getIsDeleted())
                .sentAt(message.getSentAt())
                .editedAt(message.getEditedAt())
                .reactions(reactions)
                .build();
    }
}