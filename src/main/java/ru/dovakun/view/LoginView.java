package ru.dovakun.view;

import com.formdev.flatlaf.FlatClientProperties;
import net.miginfocom.swing.MigLayout;
import ru.dovakun.manager.FormsManager;
import ru.dovakun.model.User;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

import static ru.dovakun.constant.TelegramTheme.TG_BACKGROUND;

public class LoginView extends AbstractBaseView {

    private JTextField txtUsername;
    private JPasswordField txtPassword;
    private JButton cmdLogin;

    public LoginView(boolean initializeComponents) {
        super(initializeComponents);
    }

    @Override
    public void initComponents() {
        setBackground(TG_BACKGROUND);
        setLayout(new MigLayout("fill,insets 20", "[center]", "[center]"));

        txtUsername = new JTextField();
        txtUsername.setBackground(TG_BACKGROUND);
        txtPassword = new JPasswordField();
        txtPassword.setBackground(TG_BACKGROUND);
        cmdLogin = new JButton("Продолжить");
        Action loginAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cmdLogin.doClick();
            }
        };

        String actionKey = "loginAction";
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ENTER"), actionKey);
        getActionMap().put(actionKey, loginAction);
        JPanel panel = new JPanel(new MigLayout("wrap,fillx,insets 35 45 30 45", "fill,250:280"));
        panel.setBackground(TG_BACKGROUND);
        panel.putClientProperty(FlatClientProperties.STYLE, "arc:20;");

        txtPassword.putClientProperty(FlatClientProperties.STYLE, "showRevealButton:true");
        cmdLogin.putClientProperty(FlatClientProperties.STYLE,
                "background:#2f6ea5;" +
                        "foreground:#ffffff;" +
                        "borderWidth:0;" +
                        "focusWidth:0;" +
                        "innerFocusWidth:0");

        txtUsername.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Введите ваш логин");
        txtPassword.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Введите ваш пароль");

        JLabel lbTitle = new JLabel("Ваш логин и пароль");
        JLabel description = new JLabel("Проверьте что вы ввели правильные данные.");
        lbTitle.putClientProperty(FlatClientProperties.STYLE, "font:bold +10");
        description.putClientProperty(FlatClientProperties.STYLE,
                "[light]foreground:lighten(@foreground,30%);" +
                        "[dark]foreground:darken(@foreground,30%)");

        panel.add(lbTitle);
        panel.add(description);
        panel.add(new JLabel("Логин"), "gapy 8");
        panel.add(txtUsername);
        panel.add(new JLabel("Пароль"), "gapy 8");
        panel.add(txtPassword);
        panel.add(cmdLogin, "gapy 10, h 40");
        panel.add(createSignupLabel(), "gapy 10");
        add(panel);

        cmdLogin.addActionListener((ActionEvent e) -> handleLogin());
    }

    private void handleLogin() {
        String username = txtUsername.getText();
        String password = new String(txtPassword.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            showError("Заполните все поля", "Ошибка");
            return;
        }

        try {
            User user = new User(username, password);
            Long userId = getUserController().authenticateUser(user);

            if (userId != null) {
                showInfo("Успешная авторизация!");
                ChatView chat = new ChatView(userId);
                FormsManager.getInstance().showForm(chat);
            } else {
                showError("Неверный логин или пароль", "Ошибка");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            showError("Ошибка при авторизации: " + ex.getMessage(), "Ошибка");
        }
    }

    private Component createSignupLabel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        panel.putClientProperty(FlatClientProperties.STYLE, "background:null");

        JButton cmdRegister = new JButton("<html><a href=\"#\">Регистрация</a></html>");
        cmdRegister.putClientProperty(FlatClientProperties.STYLE, "border:3,3,3,3");
        cmdRegister.setContentAreaFilled(false);
        cmdRegister.setCursor(new Cursor(Cursor.HAND_CURSOR));
        cmdRegister.addActionListener(e -> {
            FormsManager.getInstance().showForm(new RegisterView(true));
        });

        JLabel label = new JLabel("У вас нет аккаунта?");
        label.putClientProperty(FlatClientProperties.STYLE,
                "[light]foreground:lighten(@foreground,30%);" +
                        "[dark]foreground:darken(@foreground,30%)");

        panel.add(label);
        panel.add(cmdRegister);
        return panel;
    }
}