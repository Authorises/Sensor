package dev.authorises;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.google.gson.*;
import dev.authorises.access.Role;
import dev.authorises.access.User;
import dev.authorises.access.WebsocketClient;
import dev.authorises.messages.Message;
import dev.authorises.mongodb.MongoDBConnector;
import dev.authorises.services.MessageService;
import dev.authorises.services.UserService;
import io.javalin.Javalin;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.UnauthorizedResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class Main {
    
    public static Logger LOGGER = LoggerFactory.getLogger(Main.class);
    public static Gson gson = new Gson();

    public static void main(String[] args) {

        MongoDBConnector.connect("");

        UserService userService = UserService.getInstance();


        LOGGER.info("Checking if admin account exists...");
        CompletableFuture.supplyAsync(() -> userService.getUser("admin")).thenAccept((user -> {
            if(user==null){
                LOGGER.info("Could not find an admin account, creating one now");
                userService.createUser("admin", "sensor", Role.ROOT);
            }
        }));


        Javalin app = Javalin.create((cfg) -> {
                    cfg.accessManager(((handler, context, set) -> {
                        if(set.contains(Role.ANYONE)){
                            handler.handle(context);
                        }else{
                            if(context.sessionAttribute("userid")!=null){
                                if(userService.hasAccess((Set<Role>) set, userService.getUser(UUID.fromString(context.sessionAttribute("userid"))).getUserRole())){
                                    handler.handle(context);
                                }else{
                                    throw new UnauthorizedResponse();
                                }
                            }else{
                                throw new UnauthorizedResponse();
                            }
                        }

                    }));
                })
                .post("/message", ctx -> {
                    try {
                        Message message = new Message(UUID.fromString(ctx.queryParam("sender")), ctx.queryParam("server"), ctx.queryParam("message"), Long.valueOf(ctx.queryParam("time")));
                        MessageService.getInstance().processMessage(message);
                    }catch (NullPointerException e){
                        throw new BadRequestResponse();
                    }
                }, Role.READ_WRITE)
                .get("/last-message", ctx -> {
                    try {
                        MessageService messageService = MessageService.getInstance();
                        if(messageService.getLastMessage()!=null){
                            ctx.result(Main.gson.toJson(messageService.getLastMessage()));
                        }else{
                            ctx.result("{\"error\": \"not enough messages\"}");
                        }
                    }catch (NullPointerException e){
                        throw new BadRequestResponse();
                    }
                }, Role.READ)
                .get("/login", ctx -> {
                    String username = ctx.queryParam("username");
                    String password = ctx.queryParam("password");

                    if(username == null || password == null){
                        ctx.result("{\"error\": \"Missing Fields\"}");
                        return;
                    }
                    User user = userService.getUser(username);

                    if(user == null){
                        ctx.result("{\"error\": \"Invalid Credentials\"}");
                        return;
                    }

                    if(!BCrypt.verifyer().verify(password.toCharArray(), user.getPasswordHash()).verified){
                        ctx.result("{\"error\": \"Invalid Credentials\"}");
                        return;
                    }

                    ctx.sessionAttribute("userid", user.getUserId().toString());

                    ctx.result("{\"success\": \"Logged in successfully\"}");

                }, Role.ANYONE)
                .get("/logout", ctx -> {

                    String uuid = ctx.consumeSessionAttribute("userid");

                    if(uuid==null) {
                        ctx.result("{\"error\": \"You are not logged in\"}");
                        return;
                    };

                    userService.getWebsocketUsers().remove(userService.getWebsocket(userService.getUser(UUID.fromString(uuid))));

                    ctx.result("{\"success\": \"Logged out successfully\"}");
                }, Role.READ)
                .get("/new-password", ctx -> {

                    UserService service = userService;

                    User user = service.getUser(UUID.fromString(ctx.sessionAttribute("userid")));

                    String newPassword = ctx.queryParam("password");
                    if(newPassword==null){
                        ctx.result("{\"error\": \"Missing parameters\"}");
                        return;
                    }

                    user.setPasswordHash(service.genHash(newPassword));
                    service.updateUser(user);

                    ctx.result("{\"success\": \"Password updated successfully\"}");
                }, Role.READ)
                .get("/userinfo", ctx -> {
                    JsonObject response = new JsonObject();
                    response.add("userData", Main.gson.toJsonTree(userService.getUser(UUID.fromString(ctx.sessionAttribute("userid")))));

                    ctx.result(response.toString());

                }, Role.READ)
                .start(7000);

        app.ws("/chat", wsConfig -> {

            wsConfig.onConnect(ctx -> {
                String username = ctx.queryParam("username");
                String password = ctx.queryParam("password");

                if(username == null || password == null){
                    ctx.send("{\"error\": \"Missing Fields\"}");
                    ctx.closeSession();
                    return;
                }
                User user = userService.getUser(username);

                if(user == null){
                    ctx.send("{\"error\": \"Invalid Credentials\"}");
                    ctx.closeSession();
                    return;
                }

                if(!BCrypt.verifyer().verify(password.toCharArray(), user.getPasswordHash()).verified){
                    ctx.send("{\"error\": \"Invalid Credentials\"}");
                    ctx.closeSession();
                    return;
                }

                WebsocketClient client = new WebsocketClient();
                client.user = user;
                client.acceptMessages = true;
                client.receiveFilters = new JsonObject();
                ctx.enableAutomaticPings();

                userService.getWebsocketUsers().put(ctx, client);

            });
            wsConfig.onClose(ctx -> {
                System.out.println("B");
            });
            wsConfig.onMessage(ctx -> {
                
                if(!userService.getWebsocketUsers().containsKey(ctx)){
                    ctx.send("{\"error\": \"Unauthenticated\"}");
                    ctx.closeSession();
                }

                try {

                    JsonObject object = JsonParser.parseString(ctx.message()).getAsJsonObject();

                    if (!object.has("msg")){
                        ctx.send("{\"error\": \"Bad JSON format\"}");
                        return;
                    }

                    WebsocketClient client = userService.getWebsocketClient(ctx);

                    switch (object.get("msg").getAsString()){
                        case "ENABLE_LIVE":{
                            client.acceptMessages=true;
                            client.sendSettings(ctx);
                            break;
                        }
                        case "DISABLE_LIVE":{
                            client.acceptMessages=false;
                            client.sendSettings(ctx);
                            break;
                        }
                        case "UPDATE_FILTER":{

                            if(!object.has("data")){
                                ctx.send("{\"error\": \"Missing filter data\"}");
                                return;
                            }

                            // Example filter
                            /**
                             {
                                "msg": "UPDATE_FILTER",
                                "data": {
                                    "accounts":["grace"],
                                    "servers":["skyblock", "hub"]
                                }
                             }

                             Receive messages sent by a player with username "grace" on the either "skyblock" or "hub" servers.

                             {
                                 "msg": "UPDATE_FILTER",
                                 "data": {
                                     "accounts":[
                                         "grace",
                                         "emily",
                                         "bob",
                                         "david"
                                     ],
                                     "servers":[

                                     ]
                                 }
                             }

                             Receive messages sent by a player called grace, emily, bob or david on any server


                             {
                                 "msg": "UPDATE_FILTER",
                                 "data": {
                                     "accounts":[

                                     ],
                                     "servers":[
                                         "bedwars"
                                     ]
                                 }
                             }

                             Receive messages sent by anyone on a server called bedwars

                             {
                                 "msg": "UPDATE_FILTER",
                                 "data": {

                                 }
                             }

                             Receive all messages
                             */

                            client.receiveFilters = JsonParser.parseString(object.get("data").getAsString());
                            client.sendSettings(ctx);
                            break;
                        }
                        case "SETTINGS":{
                            client.sendSettings(ctx);
                            break;
                        }
                        case "MESSAGE":{

                            if(!userService.hasAccess(Set.of(Role.READ_WRITE), client.user.getUserRole())) {
                                ctx.send("{\"error\": \"You do not have permission to add messages\"}");
                                return;
                            }

                            Message message = Main.gson.fromJson(object.get("data"), Message.class);
                            MessageService.getInstance().processMessage(message);

                            break;
                        }
                    }


                }catch (JsonSyntaxException exception){
                    exception.printStackTrace();
                    ctx.send("{\"error\": \"Bad JSON format\"}");
                }

            });

        }, Role.ANYONE);

    }
}