package ru.dovakun.view;

import com.formdev.flatlaf.FlatClientProperties;
import net.miginfocom.swing.MigLayout;
import ru.dovakun.controller.ChatController;
import ru.dovakun.model.User;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

import static ru.dovakun.constant.TelegramTheme.*;

public class ChatView extends AbstractBaseView {

    private JPanel contactsPanel;
    private JPanel chatPanel;
    private JPanel sidebarPanel;
    private JTextField searchField;
    private final ChatController chatController;
    private final Map<Long, JPanel> messagesContainers = new HashMap<>();
    private final Map<Long, Set<Long>> displayedMessageIds = new HashMap<>();

    public ChatView(long userId) {
        super(false);
        this.chatController = new ChatController(userId, this);
        initComponents();
    }

    @Override
    public void initComponents() {
        setLayout(new MigLayout("fill", "[280!][grow]", "fill"));
        setBackground(TG_BACKGROUND);

        sidebarPanel = createSidebarPanel();
        add(sidebarPanel, "grow");

        chatPanel = new JPanel(new CardLayout());
        chatPanel.setBackground(TG_BACKGROUND);

        JPanel welcomePanel = createWelcomePanel();
        chatPanel.add(welcomePanel, "welcome");
        add(chatPanel, "grow");

        chatController.connectWebSocket();
    }

    private JPanel createSidebarPanel() {
        JPanel sidebar = new JPanel(new MigLayout("fillx, insets 0", "[fill]", "[45:45:45][50:50:50][grow]"));
        sidebar.setBackground(TG_SIDEBAR);
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(42, 47, 50)));

        JPanel menuPanel = new JPanel(new MigLayout("fill", "10[40!]10[grow]10[]10", "center"));
        menuPanel.setBackground(TG_SIDEBAR);
        sidebar.add(menuPanel, "growx, wrap");

        JPanel searchPanel = new JPanel(new MigLayout("fillx, insets 8 12 8 12", "[grow]", "center"));
        searchPanel.setBackground(TG_SIDEBAR);
        searchField = createSearchField();
        searchPanel.add(searchField, "growx");
        sidebar.add(searchPanel, "growx, wrap");

        contactsPanel = new JPanel(new MigLayout("fillx, wrap", "fill", "[]"));
        contactsPanel.setBackground(TG_SIDEBAR);

        JScrollPane contactsScrollPane = new JScrollPane(contactsPanel);
        contactsScrollPane.setBorder(null);
        contactsScrollPane.setBackground(TG_SIDEBAR);
        contactsScrollPane.getViewport().setBackground(TG_SIDEBAR);
        contactsScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        contactsScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        contactsScrollPane.putClientProperty(FlatClientProperties.STYLE, "background:" + colorToHex(TG_SIDEBAR));

        sidebar.add(contactsScrollPane, "grow");
        return sidebar;
    }

    private JTextField createSearchField() {
        JTextField field = new JTextField();
        field.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Поиск");
        field.putClientProperty(FlatClientProperties.STYLE,
                "background:" + colorToHex(TG_HEADER) + ";" +
                        "foreground:" + colorToHex(TG_TEXT) + ";" +
                        "arc:8;" +
                        "borderWidth:0");
        field.setFont(new Font(field.getFont().getName(), Font.PLAIN, 14));
        return field;
    }

    private JPanel createWelcomePanel() {
        JPanel panel = new JPanel(new MigLayout("fill, center", "center", "center"));
        panel.setBackground(TG_BACKGROUND);
        JLabel welcomeLabel = new JLabel("Выберите контакт для начала общения");
        welcomeLabel.setForeground(TG_TEXT);
        welcomeLabel.setFont(new Font(welcomeLabel.getFont().getName(), Font.PLAIN, 16));
        panel.add(welcomeLabel);
        return panel;
    }

    public void updateContacts(List<User> contacts) {
        SwingUtilities.invokeLater(() -> {
            contactsPanel.removeAll();

            JPanel chatsHeaderPanel = new JPanel(new MigLayout("fillx", "[]push[]", "[]"));
            chatsHeaderPanel.setBackground(TG_SIDEBAR);
            JLabel chatsLabel = new JLabel("Чаты");
            chatsLabel.setForeground(TG_SECONDARY_TEXT);
            chatsLabel.setFont(new Font(chatsLabel.getFont().getName(), Font.BOLD, 14));
            chatsHeaderPanel.add(chatsLabel);
            contactsPanel.add(chatsHeaderPanel, "growx, gaptop 5, gapbottom 5");

            for (User contact : contacts) {
                if (contact.getId() == chatController.getCurrentUserId()) {
                    continue;
                }
                JPanel contactItem = createContactItem(contact);
                contactsPanel.add(contactItem, "growx");
                JSeparator separator = new JSeparator();
                separator.setForeground(new Color(42, 47, 50, 100));
                contactsPanel.add(separator, "growx, gapleft 60");
            }

            contactsPanel.revalidate();
            contactsPanel.repaint();
        });
    }

    private JPanel createContactItem(User contact) {
        JPanel panel = new JPanel(new MigLayout("fillx", "10[40!]10[grow]10", "5[25!]3[17!]5"));
        panel.setBackground(TG_SIDEBAR);
        panel.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JLabel avatar = createAvatarLabel(contact);
        JPanel infoPanel = createInfoPanel(contact);

        panel.add(avatar, "cell 0 0, spany 2, width 40!, height 40!");
        panel.add(infoPanel, "cell 1 0, span 1 2, growx");

        panel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                for (Component comp : contactsPanel.getComponents()) {
                    if (comp instanceof JPanel && !comp.equals(panel)) {
                        comp.setBackground(TG_SIDEBAR);
                    }
                }
                panel.setBackground(TG_SELECTED);
                chatController.openChat(contact);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                if (panel.getBackground() != TG_SELECTED) {
                    panel.setBackground(TG_HOVER);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (panel.getBackground() != TG_SELECTED) {
                    panel.setBackground(TG_SIDEBAR);
                }
            }
        });

        return panel;
    }

    private JLabel createAvatarLabel(User contact) {
        JLabel avatar = new JLabel(contact.getUsername().substring(0, 1).toUpperCase()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                super.paintComponent(g);
                g2.dispose();
            }
        };
        avatar.setOpaque(false);
        avatar.setBackground(getRandomAvatarColor(contact.getId()));
        avatar.setForeground(Color.WHITE);
        avatar.setFont(new Font(avatar.getFont().getName(), Font.BOLD, 16));
        avatar.setHorizontalAlignment(SwingConstants.CENTER);
        avatar.setPreferredSize(new Dimension(40, 40));
        avatar.setMinimumSize(new Dimension(40, 40));
        return avatar;
    }

    private JPanel createInfoPanel(User contact) {
        JPanel infoPanel = new JPanel(new MigLayout("fillx, insets 0", "[grow]push[]", "[]0[]"));
        infoPanel.setOpaque(false);

        JLabel username = new JLabel(contact.getUsername());
        username.setForeground(TG_TEXT);
        username.setFont(new Font(username.getFont().getName(), Font.BOLD, 14));

        JLabel lastMessage = new JLabel("Нажмите чтобы начать чат");
        lastMessage.setForeground(TG_SECONDARY_TEXT);
        lastMessage.setFont(new Font(lastMessage.getFont().getName(), Font.PLAIN, 13));

        infoPanel.add(username, "cell 0 0");
        infoPanel.add(lastMessage, "cell 0 1, span 2, growx");
        return infoPanel;
    }

    private Color getRandomAvatarColor(long userId) {
        Color[] avatarColors = {
                new Color(255, 87, 34),
                new Color(233, 30, 99),
                new Color(156, 39, 176),
                new Color(103, 58, 183),
                new Color(33, 150, 243),
                new Color(0, 188, 212),
                new Color(0, 150, 136),
                new Color(76, 175, 80),
                new Color(205, 220, 57),
                new Color(255, 193, 7)
        };
        return avatarColors[(int)(userId % avatarColors.length)];
    }

    private String colorToHex(Color color) {
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }

    public void openChat(User contact) {
        String chatId = "chat_" + contact.getId();
        CardLayout cl = (CardLayout) chatPanel.getLayout();

        displayedMessageIds.remove(contact.getId());
        System.out.println("Очищены displayedMessageIds для contactId=" + contact.getId());

        boolean chatExists = false;
        for (Component comp : chatPanel.getComponents()) {
            if (chatId.equals(comp.getName())) {
                chatExists = true;
                break;
            }
        }

        if (!chatExists) {
            System.out.println("Создание новой панели чата для contactId=" + contact.getId());
            JPanel newChatPanel = createChatPanel(contact);
            newChatPanel.setName(chatId);
            chatPanel.add(newChatPanel, chatId);
        } else {
            System.out.println("Панель чата для contactId=" + contact.getId() + " уже существует");
        }

        cl.show(chatPanel, chatId);
    }

    private JPanel createChatPanel(User contact) {
        JPanel panel = new JPanel(new MigLayout("fill, insets 0", "[grow]", "[50!][grow][60!]"));
        panel.setBackground(TG_BACKGROUND);

        JPanel headerPanel = createHeaderPanel(contact);
        JScrollPane messagesScrollPane = createMessagesScrollPane(contact);
        JPanel inputPanel = createInputPanel(contact);

        panel.add(headerPanel, "growx, wrap");
        panel.add(messagesScrollPane, "grow, wrap");
        panel.add(inputPanel, "growx");

        return panel;
    }

    private JPanel createHeaderPanel(User contact) {
        JPanel headerPanel = new JPanel(new MigLayout("fill, insets 0 15 0 15", "[]10[grow]push[]", "center"));
        headerPanel.setBackground(TG_HEADER);
        headerPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(42, 47, 50)));

        JLabel avatar = createAvatarLabel(contact);
        avatar.setPreferredSize(new Dimension(40, 40));

        JPanel contactInfoPanel = new JPanel(new MigLayout("fillx, insets 0", "[grow]", "[]0[]"));
        contactInfoPanel.setOpaque(false);

        JLabel usernameLabel = new JLabel(contact.getUsername());
        usernameLabel.setForeground(TG_TEXT);
        usernameLabel.setFont(new Font(usernameLabel.getFont().getName(), Font.BOLD, 15));

        contactInfoPanel.add(usernameLabel, "wrap");

        JPanel actionsPanel = new JPanel(new MigLayout("insets 0", "[]5[]5[]", "center"));
        actionsPanel.setOpaque(false);

        headerPanel.add(avatar, "width 40!, height 40!");
        headerPanel.add(contactInfoPanel);
        headerPanel.add(actionsPanel);

        return headerPanel;
    }

    private JScrollPane createMessagesScrollPane(User contact) {
        if (messagesContainers.containsKey(contact.getId())) {
            System.out.println("messagesPanel для contactId=" + contact.getId() + " уже существует");
        }
        JPanel messagesPanel = new JPanel(new MigLayout("fillx, wrap, insets 10", "[grow]", "[]"));
        messagesPanel.setBackground(TG_BACKGROUND);
        messagesContainers.put(contact.getId(), messagesPanel);
        System.out.println("Создан messagesPanel для contactId=" + contact.getId());

        JScrollPane scrollPane = new JScrollPane(messagesPanel);
        scrollPane.setBorder(null);
        scrollPane.setBackground(TG_BACKGROUND);
        scrollPane.getViewport().setBackground(TG_BACKGROUND);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        return scrollPane;
    }

    private JPanel createInputPanel(User contact) {
        JPanel inputPanel = new JPanel(new MigLayout("fill, insets 10", "[grow]5[40]5[40]", "center"));
        inputPanel.setBackground(TG_INPUT_AREA);
        inputPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(42, 47, 50)));

        JTextArea messageInput = new JTextArea();
        messageInput.setRows(1);
        messageInput.setLineWrap(true);
        messageInput.setWrapStyleWord(true);
        messageInput.setBackground(TG_HEADER);
        messageInput.setForeground(TG_TEXT);
        messageInput.setBorder(new EmptyBorder(8, 12, 8, 12));
        messageInput.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Написать сообщение...");
        messageInput.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1, true));

        JScrollPane inputScrollPane = new JScrollPane(messageInput);
        inputScrollPane.setBorder(null);
        inputScrollPane.setBackground(TG_HEADER);
        inputScrollPane.getViewport().setBackground(TG_HEADER);
        inputScrollPane.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1, true));

        JButton sendButton = new JButton("▶");
        Action sendAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendButton.doClick();
            }
        };

        String sendKey = "sendAction";
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ENTER"), sendAction);
        getActionMap().put(sendKey, sendAction);
        sendButton.setForeground(TG_ACCENT);
        sendButton.setFont(new Font(sendButton.getFont().getName(), Font.BOLD, 18));
        sendButton.setFocusPainted(false);
        sendButton.setContentAreaFilled(false);
        sendButton.setBorderPainted(false);
        sendButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

        sendButton.addActionListener(e -> {
            String message = messageInput.getText().trim();
            if (!message.isEmpty()) {
                chatController.sendMessage(contact.getId(), message);
                messageInput.setText("");
            }
        });


        inputPanel.add(inputScrollPane, "grow");
        inputPanel.add(sendButton);
        return inputPanel;
    }

    public void addMessage(long contactId, String messageText, boolean isOwnMessage, long messageId) {
        System.out.println("Попытка добавить сообщение: contactId=" + contactId + ", messageText=" + messageText + ", isOwnMessage=" + isOwnMessage + ", messageId=" + messageId);
        JPanel messagesPanel = messagesContainers.get(contactId);
        if (messagesPanel == null) {
            System.out.println("messagesPanel для contactId=" + contactId + " не найден");
            return;
        }

        displayedMessageIds.computeIfAbsent(contactId, k -> new HashSet<>());
        if (displayedMessageIds.get(contactId).contains(messageId)) {
            System.out.println("Сообщение с messageId=" + messageId + " уже отображено, пропускаем");
            return;
        }
        displayedMessageIds.get(contactId).add(messageId);
        System.out.println("Добавлено сообщение в UI: messageId=" + messageId);

        JPanel messagePanel = new JPanel(new MigLayout("fillx, insets 3",
                isOwnMessage ? "push[]" : "[]push", "[]"));
        messagePanel.setOpaque(false);

        JPanel bubblePanel = new JPanel(new MigLayout("fillx, insets 0", "[grow]push[]", "[]0[]"));
        bubblePanel.setOpaque(true);
        bubblePanel.setBackground(isOwnMessage ? TG_MESSAGE_OUT : TG_MESSAGE_IN);
        bubblePanel.setBorder(new EmptyBorder(8, 12, 8, 12));
        bubblePanel.putClientProperty(FlatClientProperties.STYLE, "arc:12;");

        JTextArea message = new JTextArea(messageText);
        message.setEditable(false);
        message.setLineWrap(true);
        message.setWrapStyleWord(true);
        message.setOpaque(false);
        message.setForeground(TG_TEXT);
        message.setFont(new Font(message.getFont().getName(), Font.PLAIN, 14));
        message.setBorder(null);

        bubblePanel.add(message, "wrap, growx, width 100:300:");

        JPanel metaPanel = new JPanel(new MigLayout("insets 0", "push[][]", "center"));
        metaPanel.setOpaque(false);

        bubblePanel.add(metaPanel, "growx");
        messagePanel.add(bubblePanel, "width 100:400:, growx");

        messagesPanel.add(messagePanel, "growx");
        messagesPanel.revalidate();
        messagesPanel.repaint();

        JScrollPane scrollPane = (JScrollPane) messagesPanel.getParent().getParent();
        JScrollBar vertical = scrollPane.getVerticalScrollBar();
        vertical.setValue(vertical.getMaximum());
    }

}