package com.dipak.server.service;

import com.dipak.server.domain.Conversation;
import com.dipak.server.domain.ConversationMember;
import com.dipak.server.domain.Message;
import com.dipak.server.domain.User;
import com.dipak.server.dto.ConversationDto;
import com.dipak.server.dto.MessageDto;
import com.dipak.server.dto.SendMessageRequest;
import com.dipak.server.exception.AppException;
import com.dipak.server.repository.ConversationMemberRepository;
import com.dipak.server.repository.ConversationRepository;
import com.dipak.server.repository.MessageRepository;
import com.dipak.server.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ConversationService {

    private final ConversationRepository convRepo;
    private final ConversationMemberRepository memberRepo;
    private final MessageRepository messageRepo;
    private final UserRepository userRepo;

    public ConversationService(ConversationRepository convRepo,
                               ConversationMemberRepository memberRepo,
                               MessageRepository messageRepo,
                               UserRepository userRepo) {
        this.convRepo = convRepo;
        this.memberRepo = memberRepo;
        this.messageRepo = messageRepo;
        this.userRepo = userRepo;
    }

    @Transactional
    public ConversationDto getOrCreateDirectConversation(Long currentUserId, Long targetUserId) {
        if (currentUserId.equals(targetUserId)) {
            throw AppException.badRequest("Cannot start a direct conversation with yourself");
        }

        User targetUser = userRepo.findById(targetUserId)
                .orElseThrow(() -> AppException.notFound("User not found: " + targetUserId));

        long min = Math.min(currentUserId, targetUserId);
        long max = Math.max(currentUserId, targetUserId);
        String directKey = min + ":" + max;

        Conversation conv = convRepo.findByDirectKey(directKey).orElseGet(() -> {
            Conversation c = new Conversation();
            c.setType("DIRECT");
            c.setDirectKey(directKey);
            c.setCreatedBy(currentUserId);
            c.setTitle(targetUser.getName());
            c = convRepo.save(c);

            ConversationMember m1 = new ConversationMember();
            m1.setConversationId(c.getId());
            m1.setUserId(currentUserId);
            m1.setRole("ADMIN");
            memberRepo.save(m1);

            ConversationMember m2 = new ConversationMember();
            m2.setConversationId(c.getId());
            m2.setUserId(targetUserId);
            m2.setRole("MEMBER");
            memberRepo.save(m2);

            return c;
        });

        ConversationDto dto = new ConversationDto();
        dto.setId(conv.getId());
        dto.setType(conv.getType());
        dto.setTitle(targetUser.getName());
        dto.setOtherUserId(targetUser.getId());
        dto.setOtherUserName(targetUser.getName());
        dto.setCreatedAt(conv.getCreatedAt());
        return dto;
    }

    public List<ConversationDto> getUserConversations(Long currentUserId) {
        List<ConversationMember> memberships = memberRepo.findByUserId(currentUserId);
        List<ConversationDto> list = new ArrayList<>();

        for (ConversationMember mem : memberships) {
            convRepo.findById(mem.getConversationId()).ifPresent(conv -> {
                ConversationDto dto = new ConversationDto();
                dto.setId(conv.getId());
                dto.setType(conv.getType());
                dto.setCreatedAt(conv.getCreatedAt());

                if ("DIRECT".equals(conv.getType())) {
                    List<ConversationMember> allMembers = memberRepo.findByConversationId(conv.getId());
                    for (ConversationMember other : allMembers) {
                        if (!other.getUserId().equals(currentUserId)) {
                            userRepo.findById(other.getUserId()).ifPresent(u -> {
                                dto.setOtherUserId(u.getId());
                                dto.setOtherUserName(u.getName());
                                dto.setTitle(u.getName());
                            });
                            break;
                        }
                    }
                } else {
                    dto.setTitle(conv.getTitle());
                }
                list.add(dto);
            });
        }

        return list;
    }

    @Transactional
    public MessageDto sendMessage(Long currentUserId, Long conversationId, SendMessageRequest req) {
        memberRepo.findByConversationIdAndUserId(conversationId, currentUserId)
                .orElseThrow(() -> AppException.forbidden("Not a member of this conversation"));

        Message msg = new Message();
        msg.setConversationId(conversationId);
        msg.setSenderId(currentUserId);
        msg.setClientMessageId(req.getClientMessageId() != null ? req.getClientMessageId() : UUID.randomUUID().toString());
        msg.setType(req.getType() != null ? req.getType() : "TEXT");
        msg.setCiphertext(req.getCiphertext());
        msg.setIv(req.getIv() != null ? req.getIv() : "none");
        msg = messageRepo.save(msg);

        return new MessageDto(msg);
    }

    public List<MessageDto> getMessages(Long currentUserId, Long conversationId) {
        memberRepo.findByConversationIdAndUserId(conversationId, currentUserId)
                .orElseThrow(() -> AppException.forbidden("Not a member of this conversation"));

        return messageRepo.findByConversationIdOrderByIdAsc(conversationId)
                .stream()
                .map(MessageDto::new)
                .collect(Collectors.toList());
    }
}
