package dev.authorises.services;

import com.google.common.collect.EvictingQueue;
import com.google.gson.Gson;
import dev.authorises.Main;
import dev.authorises.messages.Message;
import dev.authorises.mongodb.MongoDBConnector;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class MessageService {

    private static MessageService instance;
    private List<Document> insertQueue;
    private EvictingQueue<Message> messagesHistory;

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

        Main.LOGGER.info(new Gson().toJson(message));

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

}
