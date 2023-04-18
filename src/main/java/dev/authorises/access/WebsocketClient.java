package dev.authorises.access;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.authorises.Main;
import io.javalin.websocket.WsContext;

public class WebsocketClient {

    public Boolean acceptMessages;

    public transient User user;
    public JsonElement receiveFilters;

    public WebsocketClient(){

    }

    public void sendSettings(WsContext websocket){
        JsonObject message = new JsonObject();
        message.addProperty("msg", "settings");
        message.add("data", Main.gson.toJsonTree(this));
        websocket.send(message.toString());
    }

}
