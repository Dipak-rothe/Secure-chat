package com.dipak.server.controller;

import com.dipak.server.dto.ConversationDto;
import com.dipak.server.dto.MessageDto;
import com.dipak.server.dto.SendMessageRequest;
import com.dipak.server.security.SecurityUtils;
import com.dipak.server.service.ConversationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/conversations")
@Tag(name = "Conversations", description = "Direct chats and messaging")
public class ConversationController {

    private final ConversationService convService;

    public ConversationController(ConversationService convService) {
        this.convService = convService;
    }

    @PostMapping("/direct/{targetUserId}")
    @Operation(summary = "Get or create a direct conversation with a target user")
    public ResponseEntity<ConversationDto> getOrCreateDirect(@PathVariable Long targetUserId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(convService.getOrCreateDirectConversation(currentUserId, targetUserId));
    }

    @GetMapping
    @Operation(summary = "Get list of all conversations for current user")
    public ResponseEntity<List<ConversationDto>> getConversations() {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(convService.getUserConversations(currentUserId));
    }

    @PostMapping("/{conversationId}/messages")
    @Operation(summary = "Send a message in a conversation")
    public ResponseEntity<MessageDto> sendMessage(@PathVariable Long conversationId,
                                                  @Valid @RequestBody SendMessageRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        MessageDto dto = convService.sendMessage(currentUserId, conversationId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @GetMapping("/{conversationId}/messages")
    @Operation(summary = "Get all messages in a conversation")
    public ResponseEntity<List<MessageDto>> getMessages(@PathVariable Long conversationId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(convService.getMessages(currentUserId, conversationId));
    }
}
