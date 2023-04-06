package dev.authorises.services;

import dev.authorises.messages.Message;
import dev.authorises.mongodb.MongoDBConnector;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class MessageService {

    private static MessageService instance;
    private List<Document> insertQueue;

    private MessageService(){
        insertQueue = new ArrayList<>();
    }

    public static MessageService getInstance(){
        if(instance==null){
            instance = new MessageService();
        }
        return instance;
    }

    /**
     * Processes a message, adding it to the queue to be inserted and broadcasting it to websocket clients
     * @param message The message to process
     */
    public void processMessage(Message message){
        insertQueue.add(message.generateDocument());

        if(insertQueue.size()>10){
            insertMessages();
        }

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
