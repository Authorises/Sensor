package dev.authorises;

import com.google.gson.Gson;
import dev.authorises.access.Role;
import dev.authorises.messages.Message;
import dev.authorises.mongodb.MongoDBConnector;
import dev.authorises.services.MessageService;
import dev.authorises.services.UserService;
import io.javalin.Javalin;
import io.javalin.http.UnauthorizedResponse;
import io.javalin.websocket.WsContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class Main {
    
    public static Logger LOGGER = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {

        LOGGER.info("Checking if admin account exists...");
        CompletableFuture.supplyAsync(() -> UserService.getInstance().getUser("admin")).thenAccept((user -> {
            if(user==null){
                LOGGER.info("Could not find an admin account, creating one now");
                UserService.getInstance().createUser("admin", "sensor", Role.ROOT);
            }
        }));


        Javalin.create((cfg) -> {
            cfg.accessManager(((handler, context, set) -> {
                if(set.contains(Role.ANYONE)){
                    handler.handle(context);
                }else{
                    if(context.sessionAttributeMap().containsKey("userid")){
                        UserService.getInstance().hasAccess((Set<Role>) set, UserService.getInstance().getUser(UUID.fromString(context.sessionAttribute("userid"))).getUserRole());
                    }else{
                        throw new UnauthorizedResponse();
                    }
                }
            }));
                })
                .post("/message", ctx -> {
                    try {
                        Message message = new Message(UUID.fromString(ctx.queryParam("sender")), ctx.queryParam("message"), Long.valueOf(ctx.queryParam("time")));
                        MessageService.getInstance().processMessage(message);
                    }catch (NullPointerException e){
                        throw new io.javalin.http.BadRequestResponse();
                    }
                }, Role.ANYONE)
                .get("/login", ctx -> {
                    String username = ctx.queryParamAsClass("username", String.class).get();
                    String password = ctx.queryParamAsClass("username", String.class).get();

                    if(username == null || password == null){

                    }

                    ctx.result(new Gson().toJson(MessageService.getInstance().getLastMessages()));
                }, Role.ROOT)
                .start(1234);

    }
}