package org.example;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class SignalingHandler extends TextWebSocketHandler {

    // --- VARIÁVEIS NOVAS (AQUI ESTAVA FALTANDO O 'rooms') ---

    // 1. Mapa para guardar as SALAS (Nome da Sala -> Conjunto de Sessões)
    // Ex: "sala-01" -> [SessaoDoJoao, SessaoDaMaria]
    private final Map<String, Set<WebSocketSession>> rooms = new ConcurrentHashMap<>();

    // 2. Mapa auxiliar para saber em qual sala a pessoa está (para quando ela desconectar)
    // Ex: "ID-Sessao-Joao" -> "sala-01"
    private final Map<String, String> sessionRoomMap = new ConcurrentHashMap<>();

    private final Gson gson = new Gson();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        System.out.println("Conexão estabelecida: " + session.getId());
        // Aqui não fazemos nada, esperamos o usuário mandar a mensagem de "join"
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        JsonObject jsonMessage = gson.fromJson(payload, JsonObject.class);

        // --- AQUI SE RESOLVE O MISTÉRIO DO 'type' ---
        // Lemos qual é o tipo da mensagem que o React mandou
        String type = jsonMessage.get("type").getAsString();

        // LÓGICA 1: USUÁRIO QUER ENTRAR NA SALA
        if ("join".equals(type)) {
            String roomName = jsonMessage.get("room").getAsString();

            // Cria a sala se não existir
            Set<WebSocketSession> clients = rooms.computeIfAbsent(roomName, k -> ConcurrentHashMap.newKeySet());

            // --- REGRA DE OURO: Limite de 2 pessoas (1:1) ---
            if (clients.size() >= 2) {
                System.out.println("Sala cheia: " + roomName + ". Rejeitando " + session.getId());

                // Manda erro pro Front
                JsonObject errorMsg = new JsonObject();
                errorMsg.addProperty("type", "full");
                errorMsg.addProperty("message", "Sala cheia!");
                session.sendMessage(new TextMessage(gson.toJson(errorMsg)));
                return;
            }

            // Adiciona usuário na sala
            clients.add(session);
            sessionRoomMap.put(session.getId(), roomName);
            System.out.println("Sessão " + session.getId() + " entrou na sala: " + roomName);

        }
        // LÓGICA 2: TROCA DE MENSAGENS (Offer, Answer, Candidate)
        // Se a mensagem tiver o campo 'room', nós reencaminhamos para quem está lá
        else if (jsonMessage.has("room")) {
            String roomName = jsonMessage.get("room").getAsString();
            Set<WebSocketSession> clientsInRoom = rooms.get(roomName);

            if (clientsInRoom != null) {
                // Loop: Manda para todo mundo na sala, MENOS para quem enviou
                for (WebSocketSession client : clientsInRoom) {
                    if (!client.getId().equals(session.getId()) && client.isOpen()) {
                        client.sendMessage(message);
                    }
                }
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        // Quando desconectar, precisamos limpar a bagunça
        String roomName = sessionRoomMap.remove(session.getId());

        if (roomName != null) {
            Set<WebSocketSession> clients = rooms.get(roomName);
            if (clients != null) {
                clients.remove(session); // Remove da lista da sala
                System.out.println("Saiu da sala: " + roomName);

                // Se a sala ficar vazia, podemos apagar ela da memória pra economizar
                if (clients.isEmpty()) {
                    rooms.remove(roomName);
                }
            }
        }
        System.out.println("Desconectado: " + session.getId());
    }
}