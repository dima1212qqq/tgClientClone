package ru.dovakun.view;

import com.formdev.flatlaf.FlatClientProperties;
import net.miginfocom.swing.MigLayout;
import ru.dovakun.manager.FormsManager;
import ru.dovakun.model.User;

import javax.swing.*;
import java.awt.*;

import static ru.dovakun.constant.TelegramTheme.TG_BACKGROUND;

public class RegisterView extends AbstractBaseView {

    private JTextField txtUsername;
    private JPasswordField txtPassword;
    private JPasswordField txtConfirmPassword;
    private JButton cmdRegister;

    public RegisterView(boolean initializeComponents) {
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
        txtConfirmPassword = new JPasswordField();
        txtConfirmPassword.setBackground(TG_BACKGROUND);
        cmdRegister = new JButton("Регистрация");
        cmdRegister.putClientProperty(FlatClientProperties.STYLE,
                "background:#2f6ea5;" +
                        "foreground:#ffffff;" +
                        "borderWidth:0;" +
                        "focusWidth:0;" +
                        "innerFocusWidth:0");

        JPanel panel = new JPanel(new MigLayout("wrap,fillx,insets 35 45 30 45", "[fill,360]"));
        panel.putClientProperty(FlatClientProperties.STYLE, "arc:20;");
        panel.setBackground(TG_BACKGROUND);
        txtUsername.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Введите свой логин");
        txtPassword.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Введите свой пароль");
        txtConfirmPassword.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Повторите свой пароль");

        txtPassword.putClientProperty(FlatClientProperties.STYLE, "showRevealButton:true");
        txtConfirmPassword.putClientProperty(FlatClientProperties.STYLE, "showRevealButton:true");

        JLabel lbTitle = new JLabel("Добро пожаловать в клонТелеграм!");
        lbTitle.putClientProperty(FlatClientProperties.STYLE, "font:bold +10");

        panel.add(lbTitle);
        panel.add(new JSeparator(), "gapy 5 5");
        panel.add(new JLabel("Ваш логин"));
        panel.add(txtUsername);
        panel.add(new JLabel("Ваш пароль"), "gapy 8");
        panel.add(txtPassword);
        panel.add(new JLabel("Подтвердите пароль"), "gapy 0");
        panel.add(txtConfirmPassword);
        panel.add(cmdRegister, "gapy 20");
        panel.add(createLoginLabel(), "gapy 10");
        add(panel);

        cmdRegister.addActionListener(e -> handleRegistration());
    }

    private void handleRegistration() {
        String username = txtUsername.getText();
        String password = new String(txtPassword.getPassword());
        String confirmPassword = new String(txtConfirmPassword.getPassword());

        User user = new User(username, password, confirmPassword);

        if (username.isEmpty() || password.isEmpty()) {
            showError("Заполните все поля!", "Ошибка");
            return;
        }

        if (!user.isPasswordMatching()) {
            showError("Пароли не совпадают!", "Ошибка");
            return;
        }

        try {
            boolean success = getUserController().registerUser(user);

            if (success) {
                showInfo("Регистрация прошла успешно!");
                FormsManager.getInstance().showForm(new LoginView(true));
            } else {
                showError("Ошибка регистрации. Возможно, пользователь уже существует.", "Ошибка");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            showError("Ошибка при регистрации: " + ex.getMessage(), "Ошибка");
        }
    }

    private Component createLoginLabel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        panel.putClientProperty(FlatClientProperties.STYLE, "background:null");

        JButton cmdLogin = new JButton("<html><a href=\"#\">Войти</a></html>");
        cmdLogin.putClientProperty(FlatClientProperties.STYLE, "border:3,3,3,3");
        cmdLogin.setContentAreaFilled(false);
        cmdLogin.setCursor(new Cursor(Cursor.HAND_CURSOR));
        cmdLogin.addActionListener(e -> {
            FormsManager.getInstance().showForm(new LoginView(true));
        });

        JLabel label = new JLabel("У вас уже есть аккаунт?");
        label.putClientProperty(FlatClientProperties.STYLE,
                "[light]foreground:lighten(@foreground,30%);" +
                        "[dark]foreground:darken(@foreground,30%)");

        panel.add(label);
        panel.add(cmdLogin);
        return panel;
    }
}