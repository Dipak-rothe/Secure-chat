package com.dipak.chatcore.model;

public class SendMessageRequest {
    private String ciphertext;
    private String iv;
    private String clientMessageId;
    private String type;

    public SendMessageRequest() {}

    public SendMessageRequest(String ciphertext) {
        this.ciphertext = ciphertext;
        this.iv = "none";
        this.type = "TEXT";
    }

    public String getCiphertext() { return ciphertext; }
    public void setCiphertext(String ciphertext) { this.ciphertext = ciphertext; }

    public String getIv() { return iv; }
    public void setIv(String iv) { this.iv = iv; }

    public String getClientMessageId() { return clientMessageId; }
    public void setClientMessageId(String clientMessageId) { this.clientMessageId = clientMessageId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
}
