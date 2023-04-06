package dev.authorises.messages;

import org.bson.Document;

import java.util.UUID;

public class Message {

    private final UUID sender;
    private final UUID messageId;
    private final String message;
    private final Long timeSent;

    public Message(UUID sender, String message, Long timeSent){
        this.sender = sender;
        this.messageId = UUID.randomUUID();
        this.message = message;
        this.timeSent = timeSent;
    }

    public Document generateDocument(){
        return new Document("_id", this.messageId.toString())
                .append("sender", this.sender.toString())
                .append("message", this.message)
                .append("time", this.timeSent);
    }

    public UUID getSender() {
        return sender;
    }

    public UUID getMessageId() {
        return messageId;
    }

    public String getMessage() {
        return message;
    }

    public Long getTimeSent() {
        return timeSent;
    }

}
