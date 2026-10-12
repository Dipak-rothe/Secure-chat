package com.dipak.server.dto;

import com.dipak.server.domain.Message;
import java.time.Instant;

public class MessageDto {
    private Long id;
    private Long conversationId;
    private Long senderId;
    private String clientMessageId;
    private String type;
    private String ciphertext;
    private String iv;
    private Instant createdAt;

    public MessageDto() {}

    public MessageDto(Message msg) {
        this.id = msg.getId();
        this.conversationId = msg.getConversationId();
        this.senderId = msg.getSenderId();
        this.clientMessageId = msg.getClientMessageId();
        this.type = msg.getType();
        this.ciphertext = msg.getCiphertext();
        this.iv = msg.getIv();
        this.createdAt = msg.getCreatedAt();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getConversationId() { return conversationId; }
    public void setConversationId(Long conversationId) { this.conversationId = conversationId; }

    public Long getSenderId() { return senderId; }
    public void setSenderId(Long senderId) { this.senderId = senderId; }

    public String getClientMessageId() { return clientMessageId; }
    public void setClientMessageId(String clientMessageId) { this.clientMessageId = clientMessageId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getCiphertext() { return ciphertext; }
    public void setCiphertext(String ciphertext) { this.ciphertext = ciphertext; }

    public String getIv() { return iv; }
    public void setIv(String iv) { this.iv = iv; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
