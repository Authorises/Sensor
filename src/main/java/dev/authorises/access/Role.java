package dev.authorises.access;

import io.javalin.security.RouteRole;

public enum Role implements RouteRole {
    ANYONE(1),
    READ(2),
    READ_WRITE(3),
    ADMIN(4),
    ROOT(5);

    public final int weight;

    Role(int weight){
        this.weight = weight;
    }

}
