package dev.authorises.services;

import com.google.common.collect.BiMap;
import com.google.common.collect.EvictingQueue;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.authorises.Main;
import dev.authorises.access.Role;
import dev.authorises.access.WebsocketClient;
import dev.authorises.messages.Message;
import dev.authorises.mongodb.MongoDBConnector;
import io.javalin.websocket.WsContext;
import org.bson.Document;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class MessageService {

    private static MessageService instance;
    private List<Document> insertQueue;
    private EvictingQueue<Message> messagesHistory;
    private Message lastMessage;

    private MessageService(){
        this.insertQueue = new ArrayList<>();
        this.messagesHistory = EvictingQueue.create(500);
    }

    public static MessageService getInstance(){
        if(instance==null){
            instance = new MessageService();
        }
        return instance;
    }


    /**
     * @return The last 500 messages, or if there are not 500 messages stored, load all messages stored.
     */
    public List<Message> getLastMessages(){
        if(messagesHistory.size()>=1){
            return messagesHistory.stream().toList();
        }else{
            return List.of();
        }
    }

    /**
     * Processes a message, adding it to the queue to be inserted and broadcasting it to websocket clients
     * @param message The message to process
     */
    public void processMessage(Message message){
        if(insertQueue.size()>10){
            insertMessages();
        }

        insertQueue.add(message.generateDocument());

        messagesHistory.add(message);

        setLastMessage(message);

        UserService userService = UserService.getInstance();

        JsonObject messageMsg = new JsonObject();
        messageMsg.addProperty("msg", "MESSAGE");
        messageMsg.add("data", Main.gson.toJsonTree(message));

        BiMap<WsContext, WebsocketClient> websocketUsers = userService.getWebsocketUsers();
        for (Map.Entry<WsContext, WebsocketClient> entry : websocketUsers.entrySet()) {
            WebsocketClient client = entry.getValue();
            if (!userService.hasAccess(Set.of(Role.READ), client.user.getUserRole())) {
                continue;
            }
            if (client.receiveFilters.getAsJsonObject().has("accounts") &&
                    !client.receiveFilters.getAsJsonObject().get("accounts").getAsJsonArray().contains(
                            JsonParser.parseString(message.getSender().toString()))) {
                continue;
            }
            if (client.receiveFilters.getAsJsonObject().has("servers") &&
                    !client.receiveFilters.getAsJsonObject().get("servers").getAsJsonArray().contains(
                            JsonParser.parseString(message.getServer()))) {
                continue;
            }

            entry.getKey().send(message);
        }

        Main.LOGGER.info(Main.gson.toJson(message));

    }

    /**
     * Resets the insert queue and inserts all the items of the queue into the database
     */
    private void insertMessages(){
        CompletableFuture.runAsync(() -> {
            MongoDBConnector
                    .getInstance()
                    .getClient()
                    .getDatabase("sensor")
                    .getCollection("messages")
                    .insertMany(insertQueue);
        }).thenRun(() -> {
            insertQueue.clear();
        });
    }

    public Message getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(Message lastMessage) {
        this.lastMessage = lastMessage;
    }

}
