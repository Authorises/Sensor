package dev.authorises.services;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.mongodb.client.FindIterable;
import com.mongodb.client.model.UpdateOptions;
import dev.authorises.access.Role;
import dev.authorises.access.User;
import dev.authorises.access.WebsocketClient;
import dev.authorises.mongodb.MongoDBConnector;
import io.javalin.websocket.WsContext;
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
    private HashMap<UUID, User> cachedUsers;
    private BiMap<WsContext, WebsocketClient> websocketUsers;

    public UserService() {
        cachedUsers = new HashMap<>();
        websocketUsers = HashBiMap.create();
    }


    public static UserService getInstance(){
        if(instance==null){
            instance = new UserService();
        }
        return instance;
    }

    public String genHash(String input){
        return BCrypt.withDefaults().hashToString(12, input.toCharArray());
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
        insertUser(new User(username, role, genHash(password)));
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

        cachedUsers.remove(userId);
    }


    /**
     * Updates a user
     * @param user User to update.
     */
    public void updateUser(@NotNull User user){
        MongoDBConnector
                .getInstance()
                .getClient()
                .getDatabase("sensor")
                .getCollection("users")
                .replaceOne(new Document("_id", user.getUserId().toString()), user.generateDocument());
    }

    /**
     * Inserts a user
     * @param user User to update.
     */
    public void insertUser(@NotNull User user){
        MongoDBConnector
                .getInstance()
                .getClient()
                .getDatabase("sensor")
                .getCollection("users")
                .insertOne(user.generateDocument());

        cachedUsers.put(user.getUserId(), user);
    }

    /**
     * @param uuid UUID of the user
     * @return User found. null if no user was found.
     */
    public User getUser(@NotNull UUID uuid){

        if (cachedUsers.containsKey(uuid)) {
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
                .find(new Document("username", new Document("$regex",username).append("$options","i")));
        if(find.first()!=null){
            return new User(find.first());
        }

        return null;
    }

    public WebsocketClient getWebsocketClient(WsContext context){
        return websocketUsers.get(context);
    }

    public WsContext getWebsocket(User searchUser){
        for(Map.Entry<WsContext, WebsocketClient> entry : websocketUsers.entrySet()){
            if(entry.getValue().user==searchUser){
                return entry.getKey();
            }
        }
        return null;
    }

    public BiMap<WsContext, WebsocketClient> getWebsocketUsers(){
        return websocketUsers;
    }



}
