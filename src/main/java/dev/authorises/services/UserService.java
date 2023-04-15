package dev.authorises.services;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.mongodb.client.FindIterable;
import com.mongodb.client.model.UpdateOptions;
import dev.authorises.access.Role;
import dev.authorises.access.User;
import dev.authorises.mongodb.MongoDBConnector;
import org.bson.Document;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import static com.mongodb.client.model.Updates.combine;
import static com.mongodb.client.model.Updates.set;

public class UserService {

    private static UserService instance;

    private final Map<UUID, User> cachedUsers;

    public UserService() {
        this.cachedUsers = new HashMap<>();
    }


    public static UserService getInstance(){
        if(instance==null){
            instance = new UserService();
        }
        return instance;
    }

    public boolean hasAccess(Set<Role> requiredRoles, Role has) {
        for (Role role : requiredRoles){
            if (has.weight >= role.weight) {
                return true;
            }
        }
        return false;
    }

    /**
     * Creates a new account and inserts it to Mongo
     * @param username The username of the new account
     * @param password The password of the new account
     * @param role The role of the new account
     */
    public void createUser(@NotNull String username, @NotNull String password, @NotNull Role role){
        upsertUser(new User(username, role, BCrypt.withDefaults().hashToString(12, password.toCharArray())));
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
        if(cachedUsers.containsKey(userId)){
            cachedUsers.remove(userId);
        }
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

        cachedUsers.put(user.getUserId(), user);

    }

    /**
     * @param uuid UUID of the user
     * @return User found. null if no user was found.
     */
    public User getUser(@NotNull UUID uuid){

        if(cachedUsers.containsKey(uuid)){
            return cachedUsers.get(uuid);
        }

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

    /**
     * @param username Username of the account
     * @return User found. null if no user was found.
     */
    public User getUser(@NotNull String username){
        FindIterable<Document> find = MongoDBConnector
                .getInstance()
                .getClient()
                .getDatabase("sensor")
                .getCollection("users")
                .find(new Document("username", Pattern.compile("/^"+Pattern.quote(username)+"/i")));
        if(find.first()!=null){
            return new User(find.first());
        }

        return null;
    }



}
