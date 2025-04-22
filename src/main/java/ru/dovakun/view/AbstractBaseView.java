package ru.dovakun.view;

import ru.dovakun.controller.UserController;

import javax.swing.*;

public abstract class AbstractBaseView extends JPanel implements BaseView {

    protected final UserController userController;

    public AbstractBaseView(boolean initializeComponents) {
        this.userController = new UserController();
        if (initializeComponents) {
            initComponents();

        }
    }

    @Override
    public UserController getUserController() {
        return userController;
    }
}