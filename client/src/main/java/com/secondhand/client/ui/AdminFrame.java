package com.secondhand.client.ui;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.secondhand.client.api.ApiClient;
import com.secondhand.client.model.Result;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * 管理员界面
 * 功能：搜索用户 → 查看用户商品 → 管理用户/商品
 */
public class AdminFrame extends JFrame {

    // ===== 用户管理组件 =====
    private JTable userTable;
    private DefaultTableModel userTableModel;
    private JTextField searchField;
    private JButton searchBtn;
    private JButton deleteUserBtn;
    private JButton changePwdBtn;
    private JButton muteBtn;
    private JButton refreshUserBtn;
    private JSONArray currentUsers;

    // ===== 用户商品管理组件 =====
    private JTable prodTable;
    private DefaultTableModel prodTableModel;
    private JButton deleteProdBtn;
    private JButton refreshProdBtn;
    private JLabel selectedUserLabel;
    private JSONArray currentUserProducts;
    private Long selectedUserId;

    public AdminFrame() {
        setTitle("二手物品交易系统 - 管理员");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(950, 700);
        setLocationRelativeTo(null);

        initUI();
        loadAllUsers();
    }

    private void initUI() {
        // 整体垂直布局
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setResizeWeight(0.5);
        splitPane.setDividerLocation(0.5);

        // ========== 上半部分：用户管理 ==========
        JPanel userPanel = new JPanel(new BorderLayout());

        // 搜索栏
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchPanel.add(new JLabel("搜索用户（ID/用户名）:"));
        searchField = new JTextField(20);
        searchPanel.add(searchField);
        searchBtn = new JButton("搜索");
        searchPanel.add(searchBtn);
        refreshUserBtn = new JButton("全部用户");
        searchPanel.add(refreshUserBtn);
        userPanel.add(searchPanel, BorderLayout.NORTH);

        // 用户表格
        String[] userCols = {"用户ID", "用户名", "角色", "昵称", "手机", "状态"};
        userTableModel = new DefaultTableModel(userCols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        userTable = new JTable(userTableModel);
        userTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane userScroll = new JScrollPane(userTable);
        userPanel.add(userScroll, BorderLayout.CENTER);

        // 用户操作按钮
        JPanel userBtnPanel = new JPanel(new FlowLayout());
        deleteUserBtn = new JButton("删除用户");
        changePwdBtn = new JButton("修改密码");
        muteBtn = new JButton("禁言/解禁");
        userBtnPanel.add(deleteUserBtn);
        userBtnPanel.add(changePwdBtn);
        userBtnPanel.add(muteBtn);
        userPanel.add(userBtnPanel, BorderLayout.SOUTH);

        splitPane.setTopComponent(userPanel);

        // ========== 下半部分：用户商品 ==========
        JPanel prodPanel = new JPanel(new BorderLayout());

        // 标题栏：显示当前选中的用户
        selectedUserLabel = new JLabel("请在上方选择一个用户查看其商品");
        selectedUserLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        selectedUserLabel.setFont(new Font("微软雅黑", Font.BOLD, 14));
        prodPanel.add(selectedUserLabel, BorderLayout.NORTH);

        // 商品表格
        String[] prodCols = {"商品ID", "名称", "价格", "上架时间", "状态"};
        prodTableModel = new DefaultTableModel(prodCols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        prodTable = new JTable(prodTableModel);
        prodTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane prodScroll = new JScrollPane(prodTable);
        prodPanel.add(prodScroll, BorderLayout.CENTER);

        // 商品操作按钮
        JPanel prodBtnPanel = new JPanel(new FlowLayout());
        deleteProdBtn = new JButton("删除选中商品");
        refreshProdBtn = new JButton("刷新");
        prodBtnPanel.add(deleteProdBtn);
        prodBtnPanel.add(refreshProdBtn);
        prodPanel.add(prodBtnPanel, BorderLayout.SOUTH);

        splitPane.setBottomComponent(prodPanel);

        add(splitPane);

        // ========== 事件绑定 ==========

        // 搜索
        searchBtn.addActionListener(e -> searchUsers());
        searchField.addActionListener(e -> searchUsers());
        refreshUserBtn.addActionListener(e -> loadAllUsers());

        // 用户管理操作
        deleteUserBtn.addActionListener(e -> deleteUser());
        changePwdBtn.addActionListener(e -> changePassword());
        muteBtn.addActionListener(e -> toggleMute());

        // 点击用户行 → 加载该用户的商品
        userTable.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            int row = userTable.getSelectedRow();
            if (row < 0 || currentUsers == null || row >= currentUsers.size()) return;
            JSONObject u = currentUsers.getJSONObject(row);
            selectedUserId = u.getLong("userId");
            loadUserProducts(selectedUserId, u.getStr("username"));
        });

        // 商品管理操作
        deleteProdBtn.addActionListener(e -> deleteUserProduct());
        refreshProdBtn.addActionListener(e -> {
            if (selectedUserId != null) {
                // 从 userTable 获取当前选中行的用户名
                int row = userTable.getSelectedRow();
                String name = (row >= 0 && currentUsers != null && row < currentUsers.size())
                        ? currentUsers.getJSONObject(row).getStr("username") : "";
                loadUserProducts(selectedUserId, name);
            }
        });
    }

    // ==================== 用户管理 ====================

    /**
     * 加载全部用户
     */
    private void loadAllUsers() {
        Result result = ApiClient.get("/user/admin/list");
        if (result.isSuccess()) {
            currentUsers = (JSONArray) result.getData();
            refreshUserTable();
        } else {
            JOptionPane.showMessageDialog(this, "获取用户列表失败: " + result.getMsg());
        }
    }

    /**
     * 搜索用户
     */
    private void searchUsers() {
        String keyword = searchField.getText().trim();
        if (keyword.isEmpty()) {
            loadAllUsers();
            return;
        }

        Map<String, Object> params = new HashMap<>();
        params.put("keyword", keyword);
        Result result = ApiClient.get("/user/admin/search", params);
        if (result.isSuccess()) {
            currentUsers = (JSONArray) result.getData();
            refreshUserTable();
        } else {
            JOptionPane.showMessageDialog(this, "搜索失败: " + result.getMsg());
        }
    }

    /**
     * 刷新用户表格
     */
    private void refreshUserTable() {
        userTableModel.setRowCount(0);
        if (currentUsers == null) return;

        for (int i = 0; i < currentUsers.size(); i++) {
            JSONObject u = currentUsers.getJSONObject(i);
            String role = u.getInt("role") == 1 ? "管理员" : "普通用户";
            String status = u.getInt("isMuted") == 1 ? "禁言" : "正常";
            userTableModel.addRow(new Object[]{
                    u.getLong("userId"),
                    u.getStr("username"),
                    role,
                    u.getStr("nickname"),
                    u.getStr("phone"),
                    status
            });
        }
    }

    /**
     * 删除用户
     */
    private void deleteUser() {
        int row = userTable.getSelectedRow();
        if (row < 0 || currentUsers == null || row >= currentUsers.size()) {
            JOptionPane.showMessageDialog(this, "请先选择一个用户");
            return;
        }
        JSONObject u = currentUsers.getJSONObject(row);
        if (u.getInt("role") == 1) {
            JOptionPane.showMessageDialog(this, "不能删除管理员账号");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "确定删除用户 \"" + u.getStr("username") + "\" 吗？\n该用户的所有商品也将被删除！",
                "确认删除", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        Result result = ApiClient.delete("/user/admin/delete/" + u.getLong("userId"));
        if (result.isSuccess()) {
            JOptionPane.showMessageDialog(this, "删除成功");
            // 如果删除的是当前正在查看商品的用户，清空商品区域
            if (u.getLong("userId").equals(selectedUserId)) {
                selectedUserId = null;
                prodTableModel.setRowCount(0);
                selectedUserLabel.setText("请在上方选择一个用户查看其商品");
            }
            loadAllUsers();
        } else {
            JOptionPane.showMessageDialog(this, "删除失败: " + result.getMsg());
        }
    }

    /**
     * 修改密码
     */
    private void changePassword() {
        int row = userTable.getSelectedRow();
        if (row < 0 || currentUsers == null || row >= currentUsers.size()) {
            JOptionPane.showMessageDialog(this, "请先选择一个用户");
            return;
        }
        JSONObject u = currentUsers.getJSONObject(row);

        String newPwd = JOptionPane.showInputDialog(this,
                "输入新密码（用户: " + u.getStr("username") + "）:");
        if (newPwd == null || newPwd.trim().isEmpty()) return;

        JSONObject body = new JSONObject();
        body.putOpt("userId", u.getLong("userId"));
        body.putOpt("newPassword", newPwd.trim());

        Result result = ApiClient.put("/user/admin/password", body);
        if (result.isSuccess()) {
            JOptionPane.showMessageDialog(this, "密码修改成功");
        } else {
            JOptionPane.showMessageDialog(this, "修改失败: " + result.getMsg());
        }
    }

    /**
     * 禁言/解禁
     */
    private void toggleMute() {
        int row = userTable.getSelectedRow();
        if (row < 0 || currentUsers == null || row >= currentUsers.size()) {
            JOptionPane.showMessageDialog(this, "请先选择一个用户");
            return;
        }
        JSONObject u = currentUsers.getJSONObject(row);

        Result result = ApiClient.put("/user/admin/mute/" + u.getLong("userId"), null);
        if (result.isSuccess()) {
            String status = u.getInt("isMuted") == 1 ? "解禁" : "禁言";
            JOptionPane.showMessageDialog(this, "用户 " + u.getStr("username") + " 已" + status);
            loadAllUsers();
        } else {
            JOptionPane.showMessageDialog(this, "操作失败: " + result.getMsg());
        }
    }

    // ==================== 用户商品管理 ====================

    /**
     * 加载指定用户的商品
     */
    private void loadUserProducts(Long userId, String username) {
        selectedUserLabel.setText("正在加载 " + username + " 的商品...");

        Map<String, Object> params = new HashMap<>();
        params.put("userId", userId);
        Result result = ApiClient.get("/product/admin/user-products/" + userId);
        if (result.isSuccess()) {
            currentUserProducts = (JSONArray) result.getData();
            prodTableModel.setRowCount(0);
            selectedUserLabel.setText("用户 \"" + username + "\" 的商品（共 "
                    + (currentUserProducts != null ? currentUserProducts.size() : 0) + " 件）");

            if (currentUserProducts != null) {
                for (int i = 0; i < currentUserProducts.size(); i++) {
                    JSONObject p = currentUserProducts.getJSONObject(i);
                    String status = p.getInt("status") == 1 ? "上架" : "下架";
                    String createdAt = p.getStr("createdAt");
                    if (createdAt != null && createdAt.length() > 16) {
                        createdAt = createdAt.substring(0, 16).replace("T", " ");
                    }
                    prodTableModel.addRow(new Object[]{
                            p.getLong("productId"),
                            p.getStr("name"),
                            p.getBigDecimal("price"),
                            createdAt,
                            status
                    });
                }
            }
        } else {
            JOptionPane.showMessageDialog(this, "获取商品列表失败: " + result.getMsg());
            selectedUserLabel.setText("获取 " + username + " 的商品失败");
        }
    }

    /**
     * 管理员删除用户的商品
     */
    private void deleteUserProduct() {
        int row = prodTable.getSelectedRow();
        if (row < 0 || currentUserProducts == null || row >= currentUserProducts.size()) {
            JOptionPane.showMessageDialog(this, "请先选择一个商品");
            return;
        }
        JSONObject p = currentUserProducts.getJSONObject(row);
        int confirm = JOptionPane.showConfirmDialog(this,
                "确定删除商品 \"" + p.getStr("name") + "\"（ID: " + p.getLong("productId") + "）吗？",
                "确认删除", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        Result result = ApiClient.delete("/product/admin/delete/" + p.getLong("productId"));
        if (result.isSuccess()) {
            JOptionPane.showMessageDialog(this, "删除成功");
            // 重新加载当前用户的商品
            if (selectedUserId != null) {
                int userRow = userTable.getSelectedRow();
                String userName = (userRow >= 0 && currentUsers != null && userRow < currentUsers.size())
                        ? currentUsers.getJSONObject(userRow).getStr("username") : "";
                loadUserProducts(selectedUserId, userName);
            }
        } else {
            JOptionPane.showMessageDialog(this, "删除失败: " + result.getMsg());
        }
    }
}
