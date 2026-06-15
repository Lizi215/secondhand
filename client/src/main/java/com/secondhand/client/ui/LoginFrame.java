package com.secondhand.client.ui;

import cn.hutool.json.JSONObject;
import com.secondhand.client.api.ApiClient;
import com.secondhand.client.model.Result;

import javax.swing.*;
import java.awt.*;

/**
 * 登录界面
 */
public class LoginFrame extends JFrame {

    private JTextField usernameField;
    private JPasswordField passwordField;

    public LoginFrame() {
        setTitle("二手物品交易系统 - 登录");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(400, 300);
        setLocationRelativeTo(null);
        setResizable(false);

        initUI();
    }

    private void initUI() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        // 标题
        JLabel titleLabel = new JLabel("二手物品交易系统", SwingConstants.CENTER);
        titleLabel.setFont(new Font("微软雅黑", Font.BOLD, 20));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        panel.add(titleLabel, gbc);

        // 用户名
        gbc.gridwidth = 1;
        gbc.gridy = 1;
        gbc.gridx = 0;
        panel.add(new JLabel("用户名:"), gbc);
        gbc.gridx = 1;
        usernameField = new JTextField(15);
        panel.add(usernameField, gbc);

        // 密码
        gbc.gridy = 2;
        gbc.gridx = 0;
        panel.add(new JLabel("密  码:"), gbc);
        gbc.gridx = 1;
        passwordField = new JPasswordField(15);
        panel.add(passwordField, gbc);

        // 按钮
        JPanel btnPanel = new JPanel(new FlowLayout());
        JButton loginBtn = new JButton("登 录");
        JButton registerBtn = new JButton("注 册");
        btnPanel.add(loginBtn);
        btnPanel.add(registerBtn);

        gbc.gridy = 3;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        panel.add(btnPanel, gbc);

        add(panel);

        // 事件
        loginBtn.addActionListener(e -> doLogin());
        registerBtn.addActionListener(e -> doRegister());
        passwordField.addActionListener(e -> doLogin());
    }

    private void doLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请输入用户名和密码");
            return;
        }

        JSONObject body = new JSONObject();
        body.putOpt("username", username);
        body.putOpt("password", password);

        Result result = ApiClient.post("/auth/login", body);
        if (result.isSuccess()) {
            JSONObject data = (JSONObject) result.getData();
            ApiClient.setToken(data.getStr("token"));
            ApiClient.setCurrentUserId(data.getLong("userId"));
            ApiClient.setCurrentRole(data.getInt("role"));
            ApiClient.setCurrentNickname(data.getStr("nickname"));

            JOptionPane.showMessageDialog(this, "登录成功，欢迎 " + ApiClient.getCurrentNickname());
            dispose();

            if (ApiClient.getCurrentRole() == 1) {
                new AdminFrame().setVisible(true);
            } else {
                new MainFrame().setVisible(true);
            }
        } else {
            JOptionPane.showMessageDialog(this, "登录失败: " + result.getMsg(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void doRegister() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请输入用户名和密码");
            return;
        }

        JSONObject body = new JSONObject();
        body.putOpt("username", username);
        body.putOpt("password", password);
        body.putOpt("nickname", username);

        Result result = ApiClient.post("/auth/register", body);
        if (result.isSuccess()) {
            JSONObject data = (JSONObject) result.getData();
            ApiClient.setToken(data.getStr("token"));
            ApiClient.setCurrentUserId(data.getLong("userId"));
            ApiClient.setCurrentRole(data.getInt("role"));
            ApiClient.setCurrentNickname(data.getStr("nickname"));

            JOptionPane.showMessageDialog(this, "注册成功，欢迎 " + ApiClient.getCurrentNickname());
            dispose();

            if (ApiClient.getCurrentRole() == 1) {
                new AdminFrame().setVisible(true);
            } else {
                new MainFrame().setVisible(true);
            }
        } else {
            JOptionPane.showMessageDialog(this, "注册失败: " + result.getMsg(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}
