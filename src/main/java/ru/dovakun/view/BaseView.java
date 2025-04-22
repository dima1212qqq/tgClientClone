package ru.dovakun.view;

import ru.dovakun.controller.UserController;

import javax.swing.*;

public interface BaseView {

    void initComponents();

    default void showError(String message, String title) {
        JOptionPane.showMessageDialog(
                (JComponent) this,
                message,
                title,
                JOptionPane.ERROR_MESSAGE
        );
    }


    default void showInfo(String message) {
        JOptionPane.showMessageDialog(
                (JComponent) this,
                message,
                "Информация",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    UserController getUserController();
}