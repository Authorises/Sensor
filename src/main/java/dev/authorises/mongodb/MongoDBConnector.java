package dev.authorises.mongodb;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;

public class MongoDBConnector {

    private final MongoClient client;
    private static MongoDBConnector instance;

    private MongoDBConnector(String uri){
        MongoClientURI mongoClientURI = new MongoClientURI(uri);
        client = new MongoClient(mongoClientURI);
    }

    public static void connect(String uri){
        instance = new MongoDBConnector(uri);
    }

    public static MongoDBConnector getInstance(){
        return instance;
    }

    public MongoClient getClient(){
        return this.client;
    }

}
