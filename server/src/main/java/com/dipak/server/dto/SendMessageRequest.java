package com.dipak.server.dto;

import jakarta.validation.constraints.NotBlank;

public class SendMessageRequest {
    @NotBlank
    private String ciphertext;
    private String iv = "default-iv";
    private String clientMessageId;
    private String type = "TEXT";

    public SendMessageRequest() {}

    public String getCiphertext() { return ciphertext; }
    public void setCiphertext(String ciphertext) { this.ciphertext = ciphertext; }

    public String getIv() { return iv; }
    public void setIv(String iv) { this.iv = iv; }

    public String getClientMessageId() { return clientMessageId; }
    public void setClientMessageId(String clientMessageId) { this.clientMessageId = clientMessageId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
}
