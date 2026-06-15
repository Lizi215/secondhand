package com.secondhand.client.ui;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.secondhand.client.api.ApiClient;
import com.secondhand.client.model.Result;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * 聊天界面
 */
public class ChatFrame extends JFrame {

    private final Long otherUserId;
    private final String otherUserName;
    private final Long productId;

    private JTextArea chatArea;
    private JTextField inputField;
    private JButton sendBtn;
    private JButton closeBtn;
    private Timer pollTimer;
    private Long lastMsgId = 0L;

    public ChatFrame(Long otherUserId, String otherUserName, Long productId) {
        this.otherUserId = otherUserId;
        this.otherUserName = otherUserName;
        this.productId = productId;

        setTitle("与 " + otherUserName + " 聊天");
        setSize(500, 500);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        initUI();
        loadHistory();
        startPolling();
    }

    private void initUI() {
        // 聊天记录区
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(chatArea);

        // 输入区
        JPanel inputPanel = new JPanel(new BorderLayout());
        inputField = new JTextField();
        sendBtn = new JButton("发送");
        closeBtn = new JButton("关闭");
        inputPanel.add(inputField, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout());
        btnPanel.add(sendBtn);
        btnPanel.add(closeBtn);
        inputPanel.add(btnPanel, BorderLayout.EAST);

        add(scrollPane, BorderLayout.CENTER);
        add(inputPanel, BorderLayout.SOUTH);

        // 事件
        sendBtn.addActionListener(e -> sendMessage());
        inputField.addActionListener(e -> sendMessage());
        closeBtn.addActionListener(e -> {
            stopPolling();
            dispose();
        });
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                stopPolling();
            }
        });
    }

    private void loadHistory() {
        Map<String, Object> params = new HashMap<>();
        params.put("userId", otherUserId);
        params.put("productId", productId);

        Result result = ApiClient.get("/chat/history", params);
        if (result.isSuccess()) {
            JSONArray messages = (JSONArray) result.getData();
            chatArea.setText("");
            if (messages != null) {
                for (int i = 0; i < messages.size(); i++) {
                    JSONObject msg = messages.getJSONObject(i);
                    appendMessage(msg);
                    Long msgId = msg.getLong("msgId");
                    if (msgId > lastMsgId) {
                        lastMsgId = msgId;
                    }
                }
            }
        }
    }

    private void sendMessage() {
        String content = inputField.getText().trim();
        if (content.isEmpty()) return;

        JSONObject body = new JSONObject();
        body.putOpt("toUserId", otherUserId);
        body.putOpt("productId", productId);
        body.putOpt("content", content);

        Result result = ApiClient.post("/chat/send", body);
        if (result.isSuccess()) {
            chatArea.append("我 (" + ApiClient.getCurrentNickname() + "): " + content + "\n\n");
            inputField.setText("");

            JSONObject data = (JSONObject) result.getData();
            Long msgId = data.getLong("msgId");
            if (msgId > lastMsgId) {
                lastMsgId = msgId;
            }
        } else {
            JOptionPane.showMessageDialog(this, "发送失败: " + result.getMsg());
        }
    }

    private void appendMessage(JSONObject msg) {
        Long fromId = msg.getLong("fromUserId");
        String content = msg.getStr("content");
        String sender = fromId.equals(ApiClient.getCurrentUserId())
                ? "我 (" + ApiClient.getCurrentNickname() + ")"
                : otherUserName;
        chatArea.append(sender + ": " + content + "\n\n");
    }

    private void startPolling() {
        pollTimer = new Timer(3000, e -> pollNewMessages());
        pollTimer.start();
    }

    private void stopPolling() {
        if (pollTimer != null) {
            pollTimer.stop();
        }
    }

    private void pollNewMessages() {
        Map<String, Object> params = new HashMap<>();
        params.put("lastMsgId", lastMsgId);
        params.put("otherUserId", otherUserId);

        Result result = ApiClient.get("/chat/poll", params);
        if (result.isSuccess()) {
            JSONArray messages = (JSONArray) result.getData();
            if (messages != null && !messages.isEmpty()) {
                for (int i = 0; i < messages.size(); i++) {
                    JSONObject msg = messages.getJSONObject(i);
                    appendMessage(msg);
                    Long msgId = msg.getLong("msgId");
                    if (msgId > lastMsgId) {
                        lastMsgId = msgId;
                    }
                }
            }
        }
    }
}
