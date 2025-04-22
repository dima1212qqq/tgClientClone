package ru.dovakun.controller;

import ru.dovakun.service.StompChatClient;
import ru.dovakun.service.WebSocketService;
import ru.dovakun.model.User;
import ru.dovakun.view.ChatView;

import javax.swing.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.Timer;

public class ChatController {
    private final long currentUserId;
    private final ChatView view;
    private WebSocketService contactsWebSocketClient;
    private final Map<Long, StompChatClient> chatClients = new HashMap<>();

    public ChatController(long userId, ChatView view) {
        this.currentUserId = userId;
        this.view = view;
    }

    public void connectWebSocket() {
        if (contactsWebSocketClient != null) {
            System.out.println("WebSocket уже подключен, пропускаем повторное подключение");
            return;
        }
        try {
            URI serverUri = new URI("ws://localhost:8080/ws");
            contactsWebSocketClient = new WebSocketService(serverUri, this::updateContacts);
            contactsWebSocketClient.setUserId(currentUserId);
            contactsWebSocketClient.requestContacts();
        } catch (URISyntaxException e) {
            e.printStackTrace();
            view.showError("Ошибка подключения к серверу", "Ошибка");
        }
    }

    private void updateContacts(List<User> contacts) {
        SwingUtilities.invokeLater(() -> view.updateContacts(contacts));
    }

    public void openChat(User contact) {
        if (!chatClients.containsKey(contact.getId())) {
            createChatConnection(contact);
        } else {
            System.out.println("Чат для contactId=" + contact.getId() + " уже открыт");
        }
        view.openChat(contact);
    }

    private void createChatConnection(User contact) {
        if (chatClients.containsKey(contact.getId())) {
            System.out.println("StompChatClient для contactId=" + contact.getId() + " уже существует, пропускаем");
            return;
        }
        try {
            StompChatClient chatClient = new StompChatClient(
                    contactsWebSocketClient,
                    currentUserId,
                    contact.getId(),
                    (message, isOwnMessage, messageId) -> {
                        SwingUtilities.invokeLater(() -> {
                            view.addMessage(contact.getId(), message, isOwnMessage, messageId);
                        });
                    }
            );
            chatClients.put(contact.getId(), chatClient);
            contactsWebSocketClient.addChatClient(chatClient);
            System.out.println("Создан StompChatClient для contactId=" + contact.getId());
            contactsWebSocketClient.requestHistory(contact.getId());
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    System.out.println("Повторный запрос истории для contactId=" + contact.getId());
                    contactsWebSocketClient.requestHistory(contact.getId());
                }
            }, 1000);
            List<Map<String, Object>> pendingMessages = contactsWebSocketClient.getPendingMessages();
            Iterator<Map<String, Object>> iterator = pendingMessages.iterator();
            while (iterator.hasNext()) {
                Map<String, Object> message = iterator.next();
                if (message.containsKey("id")) {
                    long pendingMessageId = ((Number) message.get("id")).longValue();
                    if (contactsWebSocketClient.getProcessedMessageIds().contains(pendingMessageId)) {
                        System.out.println("Отложенное сообщение с id=" + pendingMessageId + " уже обработано, пропускаем");
                        iterator.remove();
                        continue;
                    }
                }
                long senderId = message.containsKey("senderId") ? ((Number) message.get("senderId")).longValue() : 0;
                long receiverId = message.containsKey("receiverId") ? ((Number) message.get("receiverId")).longValue() : 0;
                long pendingContactId = (senderId == currentUserId) ? receiverId : senderId;
                if (pendingContactId == contact.getId()) {
                    chatClient.handleMessage(message);
                    iterator.remove();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            view.showError("Ошибка подключения к чату", "Ошибка");
        }
    }

    public void sendMessage(long contactId, String message) {
        StompChatClient chatClient = chatClients.get(contactId);
        if (chatClient == null) {
            System.out.println("StompChatClient для contactId=" + contactId + " не найден, создаем новый");
            User contact = new User(contactId, "User" + contactId, "");
            createChatConnection(contact);
            chatClient = chatClients.get(contactId);
        }
        if (chatClient != null) {
            chatClient.sendMessage(message);
        } else {
            view.showError("Ошибка отправки сообщения: нет соединения", "Ошибка");
        }
    }

    public long getCurrentUserId() {
        return currentUserId;
    }

    public Set<Long> getChatClientsKeys() {
        return chatClients.keySet();
    }

    public int getPendingMessagesSize() {
        return contactsWebSocketClient.getPendingMessages().size();
    }
}