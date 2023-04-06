package dev.authorises.access;

import org.bson.Document;

import java.util.UUID;

public class User {

    private final UUID userId;
    private final String userName;
    private final Role userRole;
    private final String passwordHash;

    /**
     * Creating a new user
     * @param userName The name of the new user
     * @param userRole The role of the new user
     * @param passwordHash BCrypt hash of the password
     */
    public User(String userName, Role userRole, String passwordHash){
        this.userId = UUID.randomUUID();
        this.userName = userName;
        this.userRole = userRole;
        this.passwordHash = passwordHash;
    }

    /**
     * Loading an existing user from a Document
     * @param document Document to create the user from
     */
    public User(Document document){
        this.userId = UUID.fromString(document.getString("_id"));
        this.userName = document.getString("username");
        this.userRole = Role.valueOf(document.getString("role"));
        this.passwordHash = document.getString("password");
    }

    /**
     * @return Document form of the user
     */
    public Document generateDocument(){
        return new Document("_id", this.userId.toString())
                .append("username", this.userName.toString())
                .append("role", this.userRole.toString())
                .append("password", this.passwordHash);
    }

    /**
     * @return id of the user
     */
    public UUID getUserId() {
        return userId;
    }

    /**
     * @return name of the user
     */
    public String getUserName() {
        return userName;
    }

    /**
     * @return role of the user
     */
    public Role getUserRole() {
        return userRole;
    }

    /**
     * @return password hash of the user
     */
    public String getPasswordHash() {
        return passwordHash;
    }

}
