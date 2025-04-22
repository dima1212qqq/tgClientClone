package ru.dovakun.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;
import ru.dovakun.model.ChatMessage;
import ru.dovakun.model.User;

import java.lang.reflect.Type;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class WebSocketService {
    private WebSocketStompClient stompClient;
    private StompSession session;
    private Map<Long, StompChatClient> chatClients = new HashMap<>();
    private Consumer<List<User>> onContactsReceived;
    private ObjectMapper objectMapper = new ObjectMapper();
    private long userId;
    private List<Map<String, Object>> pendingMessages = new ArrayList<>();
    private Set<Long> processedMessageIds = new HashSet<>();

    public WebSocketService(URI serverUri, Consumer<List<User>> onContactsReceived) {
        this.onContactsReceived = onContactsReceived;
        List<Transport> transports = new ArrayList<>();
        transports.add(new WebSocketTransport(new StandardWebSocketClient()));
        SockJsClient sockJsClient = new SockJsClient(transports);
        stompClient = new WebSocketStompClient(sockJsClient);
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());
        try {
            session = stompClient.connect(serverUri.toString(), new StompSessionHandlerAdapter() {
                @Override
                public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                    System.out.println("WebSocket-сессия установлена: " + session.getSessionId());
                }
            }).get(10, TimeUnit.SECONDS);
            System.out.println("Успешно подключено к WebSocket: " + serverUri);
            subscribeToContacts();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Не удалось подключиться к WebSocket-серверу: " + serverUri, e);
        }
    }

    private void subscribeToContacts() {
        session.subscribe("/topic/contacts", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                System.out.println("Заголовки сообщения (/topic/contacts): " + headers);
                return List.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                System.out.println("Тип payload: " + (payload != null ? payload.getClass().getName() : "null"));
                if (payload instanceof List) {
                    List<?> receivedList = (List<?>) payload;
                    System.out.println("Получены данные контактов (/topic/contacts): " + receivedList);
                    try {
                        List<User> users = new ArrayList<>();
                        for (Object item : receivedList) {
                            if (item instanceof Map) {
                                User user = objectMapper.convertValue(item, User.class);
                                users.add(user);
                            }
                        }
                        System.out.println("Преобразованный список контактов: " + users);
                        onContactsReceived.accept(users);
                    } catch (Exception e) {
                        System.err.println("Ошибка преобразования списка: " + e.getMessage());
                        e.printStackTrace();
                    }
                } else if (payload instanceof String) {
                    String jsonStr = (String) payload;
                    System.out.println("Получены данные контактов (/topic/contacts) как строка: " + jsonStr);
                    try {
                        List<User> users = objectMapper.readValue(jsonStr, new TypeReference<List<User>>() {});
                        System.out.println("Десериализованный список контактов: " + users);
                        onContactsReceived.accept(users);
                    } catch (Exception e) {
                        System.err.println("Ошибка десериализации: " + e.getMessage());
                        e.printStackTrace();
                    }
                } else {
                    System.err.println("Ошибка: неизвестный тип payload: " +
                            (payload != null ? payload.getClass().getName() : "null"));
                }
            }
        });
        System.out.println("Подписка на топик /topic/contacts");
    }

    public void setUserId(long userId) {
        this.userId = userId;
        String destination = "/user/" + userId + "/topic/messages";
        System.out.println("Подписка на топик " + destination);
        session.subscribe(destination, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return byte[].class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                System.out.println("Получено сообщение для userId " + userId + ", заголовки: " + headers);
                try {
                    Map<String, Object> message;
                    if (payload instanceof byte[]) {
                        String messageStr = new String((byte[]) payload, StandardCharsets.UTF_8);
                        System.out.println("Сырое сообщение: " + messageStr);
                        message = objectMapper.readValue(messageStr, Map.class);
                    } else {
                        System.err.println("Неизвестный тип payload: " + payload.getClass().getName());
                        return;
                    }
                    if (!message.containsKey("type") || !"history".equals(message.get("type"))) {
                        if (message.containsKey("id")) {
                            long messageId = ((Number) message.get("id")).longValue();
                            if (processedMessageIds.contains(messageId)) {
                                System.out.println("Сообщение с id=" + messageId + " уже обработано, пропускаем");
                                return;
                            }
                            processedMessageIds.add(messageId);
                        }
                    }
                    pendingMessages.add(message);
                    if (message.containsKey("type") && "history".equals(message.get("type"))) {
                        for (StompChatClient client : chatClients.values()) {
                            client.handleMessage(message);
                        }
                    } else {
                        long senderId = message.containsKey("senderId") ? ((Number) message.get("senderId")).longValue() : 0;
                        long receiverId = message.containsKey("receiverId") ? ((Number) message.get("receiverId")).longValue() : 0;
                        long contactId = (senderId == userId) ? receiverId : senderId;
                        StompChatClient client = chatClients.get(contactId);
                        if (client != null) {
                            client.handleMessage(message);
                            Iterator<Map<String, Object>> iterator = pendingMessages.iterator();
                            while (iterator.hasNext()) {
                                Map<String, Object> pendingMessage = iterator.next();
                                long pendingSenderId = pendingMessage.containsKey("senderId") ? ((Number) pendingMessage.get("senderId")).longValue() : 0;
                                long pendingReceiverId = pendingMessage.containsKey("receiverId") ? ((Number) pendingMessage.get("receiverId")).longValue() : 0;
                                long pendingContactId = (pendingSenderId == userId) ? pendingReceiverId : pendingSenderId;
                                if (pendingContactId == contactId) {
                                    client.handleMessage(pendingMessage);
                                    iterator.remove();
                                }
                            }
                        } else {
                            System.out.println("Клиент для contactId=" + contactId + " не найден");
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Ошибка обработки сообщения: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        });
    }

    public void requestContacts() {
        Map<String, Object> payload = Map.of("userId", userId);
        System.out.println("Отправка запроса на /app/contacts: " + payload);
        session.send("/app/contacts", payload);
    }

    public void requestHistory(long contactId) {
        ChatMessage request = new ChatMessage();
        request.setType("history");
        request.setUserId(userId);
        request.setContactId(contactId);
        System.out.println("Запрос истории для contactId: " + contactId);
        session.send("/app/message", request);
    }

    public void addChatClient(StompChatClient client) {
        chatClients.put(client.getContactId(), client);
    }

    public StompSession getSession() {
        return session;
    }

    public List<Map<String, Object>> getPendingMessages() {
        return pendingMessages;
    }

    public Set<Long> getProcessedMessageIds() {
        return processedMessageIds;
    }
}