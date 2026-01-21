package org.example;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SignalingHandler extends TextWebSocketHandler {
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final Gson gson = new Gson();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = session.getId();
        sessions.put(sessionId, session);
        System.out.println("Usuário conectado: " + sessionId);

        JsonObject welcome = new JsonObject();
        welcome.addProperty("type", "id");
        welcome.addProperty("id", sessionId);
        session.sendMessage(new TextMessage(gson.toJson(welcome)));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        JsonObject jsonMessage = gson.fromJson(payload, JsonObject.class);

        if(jsonMessage.has("target")) {
            String targetId = jsonMessage.get("target").getAsString();
            WebSocketSession targetSession = sessions.get(targetId);

            if (targetSession != null && targetSession.isOpen()) {
                targetSession.sendMessage(new TextMessage(payload));
            }
        } else {
            System.out.println("Mensagem sem target recebida: " + payload);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session.getId());
        System.out.println("Usuário desconectado: " + session.getId());
    }
}
