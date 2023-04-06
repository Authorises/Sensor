package dev.authorises;

import com.google.gson.Gson;
import dev.authorises.messages.Message;
import dev.authorises.mongodb.MongoDBConnector;
import dev.authorises.services.MessageService;
import io.javalin.Javalin;
import io.javalin.websocket.WsContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class Main {
    
    public static Logger LOGGER = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {

        Javalin.create()
                .post("/message", ctx -> {
                    Message message = new Message(UUID.fromString(ctx.queryParam("sender")), ctx.queryParam("message"), Long.valueOf(ctx.queryParam("time"))) ;
                    MessageService.getInstance().processMessage(message);
                })
                .get("/messages", ctx -> {
                    ctx.result(new Gson().toJson(MessageService.getInstance().getLastMessages()));
                })
                .start(1234);

    }
}