package com.soarclient.management.websocket;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import com.soarclient.management.websocket.client.SoarWebSocketClient;
import com.soarclient.management.websocket.packet.SoarPacket;
import com.soarclient.utils.HttpUtils;

public class WebSocketManager {

    private static final int MAX_RETRY = 3;
    
    private Minecraft client = Minecraft.getInstance();
    private GameProfile gameProfile;
    private SoarWebSocketClient webSocket;
    private int retryCount = 0;
    
    public WebSocketManager() {
        new Thread("Websocket Thread") {
            @Override
            public void run() {
                while (true) {
                	
                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException e) {
                    }
                    
                    GameProfile currentProfile = client.getGameProfile();
                    
                    if (gameProfile != null && currentProfile != null && !gameProfile.equals(currentProfile)) {
                        retryCount = 0;
                    }
                    
                    if ((webSocket == null || !gameProfile.equals(currentProfile) || webSocket.isClosed()) 
                            && retryCount < MAX_RETRY) {
                        
                        gameProfile = currentProfile;
                        
                        if (webSocket != null) {
                            webSocket.close();
                        }
                        
                        JsonObject postObject = new JsonObject();
                        User session = client.getUser();

                        postObject.addProperty("accessToken", session.getAccessToken());
                        postObject.addProperty("selectedProfile", session.getProfileId().toString().replace("-", ""));
                        postObject.addProperty("serverId", "cbd2c3f65d7ba5cceba0cc9647ff9a85c371f4");
                        HttpUtils.postJson("https://sessionserver.mojang.com/session/minecraft/join", postObject);
                        
                        Map<String, String> headers = new HashMap<>();
                        headers.put("name", gameProfile.name());
                        headers.put("uuid", gameProfile.id().toString().replace("-", ""));
                        
                        try {
                            webSocket = new SoarWebSocketClient(headers, () -> {
                            	retryCount++;
                            });
                            webSocket.connect();
                        } catch (URISyntaxException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }.start();
    }
    
    public void send(SoarPacket packet) {
        if (webSocket != null && webSocket.isOpen()) {
            webSocket.send(packet.toJson().toString());
        }
    }
}
