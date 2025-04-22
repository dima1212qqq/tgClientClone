package ru.dovakun.service;

import org.springframework.messaging.simp.stomp.StompSession;
import ru.dovakun.model.ChatMessage;
import ru.dovakun.util.TriConsumer;

import java.util.List;
import java.util.Map;

public class StompChatClient {
    private final WebSocketService webSocketService;
    private final long userId;
    private final long contactId;
    private final TriConsumer<String, Boolean, Long> onMessageReceived;

    public StompChatClient(WebSocketService webSocketService, long userId, long contactId, TriConsumer<String, Boolean, Long> onMessageReceived) {
        this.webSocketService = webSocketService;
        this.userId = userId;
        this.contactId = contactId;
        this.onMessageReceived = onMessageReceived;
    }

    public void sendMessage(String message) {
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setType("message");
        chatMessage.setSenderId(userId);
        chatMessage.setReceiverId(contactId);
        chatMessage.setContent(message);

        try {
            StompSession session = webSocketService.getSession();
            session.send("/app/message", chatMessage);
            System.out.println("Сообщение успешно отправлено: " + message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void handleMessage(Map<String, Object> message) {
        System.out.println("Обработка сообщения для userId=" + userId + ", contactId=" + contactId + ": " + message);
        if (message.containsKey("type") && "history".equals(message.get("type"))) {
            if (!message.containsKey("messages")) {
                System.out.println("Ошибка: поле 'messages' отсутствует в сообщении истории");
                return;
            }
            try {
                List<Map<String, Object>> messages = (List<Map<String, Object>>) message.get("messages");
                System.out.println("Получено " + messages.size() + " сообщений в истории для contactId=" + contactId);
                for (Map<String, Object> msg : messages) {
                    if (!msg.containsKey("content") || !msg.containsKey("senderId") || !msg.containsKey("id") || !msg.containsKey("receiverId")) {
                        System.out.println("Ошибка: некорректный формат сообщения в истории: " + msg);
                        continue;
                    }
                    long senderId = ((Number) msg.get("senderId")).longValue();
                    long receiverId = ((Number) msg.get("receiverId")).longValue();
                    if ((senderId == userId && receiverId == contactId) || (senderId == contactId && receiverId == userId)) {
                        String content = (String) msg.get("content");
                        long messageId = ((Number) msg.get("id")).longValue();
                        boolean isSentByUser = senderId == userId;
                        System.out.println("История: content=" + content + ", senderId=" + senderId + ", receiverId=" + receiverId + ", isOwn=" + isSentByUser + ", messageId=" + messageId);
                        onMessageReceived.accept(content, isSentByUser, messageId);
                    } else {
                        System.out.println("Сообщение из истории игнорируется: senderId=" + senderId + ", receiverId=" + receiverId + ", contactId=" + contactId + ", userId=" + userId);
                    }
                }
            } catch (ClassCastException e) {
                System.out.println("Ошибка приведения типов для поля 'messages': " + e.getMessage());
                e.printStackTrace();
            }
        } else if (message.containsKey("content") && message.containsKey("senderId") && message.containsKey("receiverId")) {
            long senderId = ((Number) message.get("senderId")).longValue();
            long receiverId = ((Number) message.get("receiverId")).longValue();
            String content = (String) message.get("content");
            long messageId = ((Number) message.get("id")).longValue();
            if (senderId == userId && receiverId == contactId) {
                System.out.println("Собственное сообщение: content=" + content + ", senderId=" + senderId + ", receiverId=" + receiverId + ", messageId=" + messageId);
                onMessageReceived.accept(content, true, messageId);
            } else if (senderId == contactId && receiverId == userId) {
                System.out.println("Сообщение от контакта: content=" + content + ", senderId=" + senderId + ", receiverId=" + receiverId + ", messageId=" + messageId);
                onMessageReceived.accept(content, false, messageId);
            } else {
                System.out.println("Сообщение игнорируется: senderId=" + senderId + ", receiverId=" + receiverId + ", contactId=" + contactId + ", userId=" + userId + ", messageId=" + messageId);
            }
        }
    }

    public long getContactId() {
        return contactId;
    }
}