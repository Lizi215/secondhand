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
 * 商品管理界面
 */
public class MyProductFrame extends JFrame {

    private JTable productTable;
    private DefaultTableModel tableModel;
    private JButton addBtn;
    private JButton editBtn;
    private JButton deleteBtn;
    private JButton refreshBtn;
    private JButton viewBuyersBtn;
    private JSONArray myProducts;

    public MyProductFrame() {
        setTitle("我的商品管理");
        setSize(850, 550);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        initUI();
        loadMyProducts();
    }

    private void initUI() {
        // 按钮栏
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        addBtn = new JButton("上架商品");
        editBtn = new JButton("修改");
        deleteBtn = new JButton("删除");
        viewBuyersBtn = new JButton("查看买家");
        refreshBtn = new JButton("刷新");
        topPanel.add(addBtn);
        topPanel.add(editBtn);
        topPanel.add(deleteBtn);
        topPanel.add(viewBuyersBtn);
        topPanel.add(refreshBtn);

        // 表格
        String[] columns = {"商品ID", "名称", "价格", "介绍", "自提点", "状态"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        productTable = new JTable(tableModel);
        productTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scrollPane = new JScrollPane(productTable);

        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        // 事件
        addBtn.addActionListener(e -> showAddDialog());
        editBtn.addActionListener(e -> showEditDialog());
        deleteBtn.addActionListener(e -> deleteProduct());
        viewBuyersBtn.addActionListener(e -> showBuyersDialog());
        refreshBtn.addActionListener(e -> loadMyProducts());
    }

    private void loadMyProducts() {
        Result result = ApiClient.get("/product/my");
        if (result.isSuccess()) {
            myProducts = (JSONArray) result.getData();
            refreshTable();
        }
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        if (myProducts == null) return;

        for (int i = 0; i < myProducts.size(); i++) {
            JSONObject p = myProducts.getJSONObject(i);
            String desc = p.getStr("description");
            if (desc != null && desc.length() > 20) {
                desc = desc.substring(0, 20) + "...";
            }
            String status = p.getInt("status") == 1 ? "上架" : "下架";
            tableModel.addRow(new Object[]{
                    p.getLong("productId"),
                    p.getStr("name"),
                    p.getBigDecimal("price"),
                    desc,
                    p.getStr("pickupPoint"),
                    status
            });
        }
    }

    // ==================== 查看买家 ====================

    /**
     * 弹出买家列表对话框
     */
    private void showBuyersDialog() {
        int row = productTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "请先选择一个商品");
            return;
        }
        if (myProducts == null || row >= myProducts.size()) return;

        JSONObject p = myProducts.getJSONObject(row);
        Long productId = p.getLong("productId");
        String productName = p.getStr("name");

        // 获取询问该商品的买家 ID 列表
        Result result = ApiClient.get("/chat/product-inquirers/" + productId);
        if (!result.isSuccess()) {
            JOptionPane.showMessageDialog(this, "获取买家列表失败: " + result.getMsg());
            return;
        }

        JSONArray buyerIds = (JSONArray) result.getData();
        if (buyerIds == null || buyerIds.isEmpty()) {
            JOptionPane.showMessageDialog(this, "该商品暂无买家询问");
            return;
        }

        // 为每个买家获取昵称，构建展示数据
        DefaultTableModel buyerModel = new DefaultTableModel(
                new String[]{"买家ID", "买家昵称"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        // 存储每个买家的 userId（用于打开聊天）
        Long[] userIds = new Long[buyerIds.size()];
        String[] userNames = new String[buyerIds.size()];

        for (int i = 0; i < buyerIds.size(); i++) {
            Long buyerId = buyerIds.getLong(i);
            userIds[i] = buyerId;

            // 调用用户服务获取昵称
            String nickname = "用户" + buyerId;
            try {
                Result userResult = ApiClient.get("/user/" + buyerId);
                if (userResult.isSuccess()) {
                    JSONObject userData = (JSONObject) userResult.getData();
                    nickname = userData.getStr("nickname");
                }
            } catch (Exception ignored) {
            }
            userNames[i] = nickname;
            buyerModel.addRow(new Object[]{buyerId, nickname});
        }

        // 构建对话框
        JDialog dialog = new JDialog(this, "询问 \"" + productName + "\" 的买家", true);
        dialog.setSize(400, 350);
        dialog.setLocationRelativeTo(this);

        JTable buyerTable = new JTable(buyerModel);
        buyerTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scrollPane = new JScrollPane(buyerTable);

        JButton chatBtn = new JButton("与选中买家聊天");
        JButton closeBtn = new JButton("关闭");

        JPanel btnPanel = new JPanel(new FlowLayout());
        btnPanel.add(chatBtn);
        btnPanel.add(closeBtn);

        dialog.add(scrollPane, BorderLayout.CENTER);
        dialog.add(btnPanel, BorderLayout.SOUTH);

        chatBtn.addActionListener(e -> {
            int br = buyerTable.getSelectedRow();
            if (br < 0) {
                JOptionPane.showMessageDialog(dialog, "请先选择一个买家");
                return;
            }
            dialog.dispose();
            new ChatFrame(userIds[br], userNames[br], productId).setVisible(true);
        });

        closeBtn.addActionListener(e -> dialog.dispose());

        dialog.setVisible(true);
    }

    // ==================== 上架 / 修改 / 删除 ====================

    private void showAddDialog() {
        JDialog dialog = new JDialog(this, "上架商品", true);
        dialog.setSize(400, 350);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 10, 5, 10);

        JTextField nameField = new JTextField(20);
        JTextField priceField = new JTextField(20);
        JTextArea descArea = new JTextArea(3, 20);
        JTextField pickupField = new JTextField(20);

        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("名称*:"), gbc);
        gbc.gridx = 1;
        panel.add(nameField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("价格*:"), gbc);
        gbc.gridx = 1;
        panel.add(priceField, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("介绍:"), gbc);
        gbc.gridx = 1;
        panel.add(new JScrollPane(descArea), gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        panel.add(new JLabel("自提点:"), gbc);
        gbc.gridx = 1;
        panel.add(pickupField, gbc);

        JButton submitBtn = new JButton("提交");
        gbc.gridx = 0; gbc.gridy = 4;
        gbc.gridwidth = 2;
        panel.add(submitBtn, gbc);

        submitBtn.addActionListener(e -> {
            if (nameField.getText().trim().isEmpty() || priceField.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "名称和价格为必填项");
                return;
            }
            try {
                Double.parseDouble(priceField.getText().trim());
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "价格请输入数字");
                return;
            }

            JSONObject body = new JSONObject();
            body.putOpt("name", nameField.getText().trim());
            body.putOpt("price", Double.parseDouble(priceField.getText().trim()));
            body.putOpt("description", descArea.getText().trim());
            body.putOpt("pickupPoint", pickupField.getText().trim());

            Result result = ApiClient.post("/product/add", body);
            if (result.isSuccess()) {
                JOptionPane.showMessageDialog(dialog, "上架成功");
                dialog.dispose();
                loadMyProducts();
            } else {
                JOptionPane.showMessageDialog(dialog, "上架失败: " + result.getMsg());
            }
        });

        dialog.add(panel);
        dialog.setVisible(true);
    }

    private void showEditDialog() {
        int row = productTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "请先选择一个商品");
            return;
        }
        if (myProducts == null || row >= myProducts.size()) return;

        JSONObject p = myProducts.getJSONObject(row);

        JDialog dialog = new JDialog(this, "修改商品", true);
        dialog.setSize(400, 350);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 10, 5, 10);

        JTextField nameField = new JTextField(p.getStr("name"), 20);
        JTextField priceField = new JTextField(p.getBigDecimal("price").toString(), 20);
        JTextArea descArea = new JTextArea(p.getStr("description"), 3, 20);
        JTextField pickupField = new JTextField(p.getStr("pickupPoint"), 20);

        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("名称*:"), gbc);
        gbc.gridx = 1;
        panel.add(nameField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("价格*:"), gbc);
        gbc.gridx = 1;
        panel.add(priceField, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("介绍:"), gbc);
        gbc.gridx = 1;
        panel.add(new JScrollPane(descArea), gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        panel.add(new JLabel("自提点:"), gbc);
        gbc.gridx = 1;
        panel.add(pickupField, gbc);

        JButton submitBtn = new JButton("保存");
        gbc.gridx = 0; gbc.gridy = 4;
        gbc.gridwidth = 2;
        panel.add(submitBtn, gbc);

        Long productId = p.getLong("productId");
        submitBtn.addActionListener(e -> {
            JSONObject body = new JSONObject();
            body.putOpt("productId", productId);
            if (!nameField.getText().trim().isEmpty()) {
                body.putOpt("name", nameField.getText().trim());
            }
            if (!priceField.getText().trim().isEmpty()) {
                try {
                    body.putOpt("price", Double.parseDouble(priceField.getText().trim()));
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(dialog, "价格请输入数字");
                    return;
                }
            }
            body.putOpt("description", descArea.getText().trim());
            body.putOpt("pickupPoint", pickupField.getText().trim());

            Result result = ApiClient.put("/product/update", body);
            if (result.isSuccess()) {
                JOptionPane.showMessageDialog(dialog, "修改成功");
                dialog.dispose();
                loadMyProducts();
            } else {
                JOptionPane.showMessageDialog(dialog, "修改失败: " + result.getMsg());
            }
        });

        dialog.add(panel);
        dialog.setVisible(true);
    }

    private void deleteProduct() {
        int row = productTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "请先选择一个商品");
            return;
        }
        if (myProducts == null || row >= myProducts.size()) return;

        JSONObject p = myProducts.getJSONObject(row);
        int confirm = JOptionPane.showConfirmDialog(this,
                "确定删除商品 \"" + p.getStr("name") + "\" 吗？",
                "确认删除", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        Long productId = p.getLong("productId");
        Result result = ApiClient.delete("/product/delete/" + productId);
        if (result.isSuccess()) {
            JOptionPane.showMessageDialog(this, "删除成功");
            loadMyProducts();
        } else {
            JOptionPane.showMessageDialog(this, "删除失败: " + result.getMsg());
        }
    }
}
