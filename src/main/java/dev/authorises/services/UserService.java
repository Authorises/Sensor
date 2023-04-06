package dev.authorises.services;

import com.mongodb.client.FindIterable;
import com.mongodb.client.model.UpdateOptions;
import dev.authorises.access.Role;
import dev.authorises.access.User;
import dev.authorises.mongodb.MongoDBConnector;
import org.bson.Document;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

import static com.mongodb.client.model.Updates.combine;
import static com.mongodb.client.model.Updates.set;

public class UserService {

    private static UserService instance;

    private UserService(){

    }

    public static UserService getInstance(){
        if(instance==null){
            instance = new UserService();
        }
        return instance;
    }

    /**
     * @param userId The UUID of the user to delete
     */
    public void deleteUser(@NotNull UUID userId){

        MongoDBConnector
                .getInstance()
                .getClient()
                .getDatabase("sensor")
                .getCollection("users")
                .deleteOne(new Document("_id", userId.toString()));
    }


    /**
     * Inserts/Updates a user
     * @param user User to update.
     */
    public void upsertUser(@NotNull User user){
        MongoDBConnector
                .getInstance()
                .getClient()
                .getDatabase("sensor")
                .getCollection("users")
                .updateOne(new Document("_id", user.getUserId().toString()), user.generateDocument(), new UpdateOptions().upsert(true));
    }

    /**
     * @param uuid UUID of the user
     * @return User found. null if no user was found.
     */
    public User getUser(@NotNull UUID uuid){
        FindIterable<Document> find = MongoDBConnector
                .getInstance()
                .getClient()
                .getDatabase("sensor")
                .getCollection("users")
                .find(new Document("_id", uuid.toString()));
        if(find.first()!=null){
            return new User(find.first());
        }

        return null;
    }



}
