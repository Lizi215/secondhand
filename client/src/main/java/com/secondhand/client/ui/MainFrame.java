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
 * 商品展示主界面（分页加载）
 */
public class MainFrame extends JFrame {

    private JTable productTable;
    private DefaultTableModel tableModel;
    private JTextField searchField;
    private JButton searchBtn;
    private JButton viewDetailBtn;
    private JButton chatBtn;
    private JButton manageBtn;
    private JButton refreshBtn;

    // 缓存当前商品列表数据
    private JSONArray currentProducts = new JSONArray();

    // 分页状态
    private int currentPage = 1;
    private static final int PAGE_SIZE = 50;
    private boolean isLoading = false;
    private boolean hasMore = true;
    private JScrollPane scrollPane;
    private JLabel statusLabel;

    public MainFrame() {
        setTitle("二手物品交易系统 - 商品列表");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);

        initUI();
        loadProducts();
    }

    private void initUI() {
        // 顶部搜索栏
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.add(new JLabel("搜索:"));
        searchField = new JTextField(20);
        topPanel.add(searchField);
        searchBtn = new JButton("搜索");
        topPanel.add(searchBtn);
        refreshBtn = new JButton("刷新");
        topPanel.add(refreshBtn);

        // 底部状态栏
        statusLabel = new JLabel("共 0 条商品");

        // 表格
        String[] columns = {"商品ID", "名称", "价格", "上架时间", "商家", "简介", "自提点"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        productTable = new JTable(tableModel);
        productTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        productTable.getColumnModel().getColumn(0).setPreferredWidth(120);
        productTable.getColumnModel().getColumn(1).setPreferredWidth(150);
        productTable.getColumnModel().getColumn(2).setPreferredWidth(60);
        productTable.getColumnModel().getColumn(3).setPreferredWidth(130);
        productTable.getColumnModel().getColumn(4).setPreferredWidth(80);
        productTable.getColumnModel().getColumn(5).setPreferredWidth(180);
        productTable.getColumnModel().getColumn(6).setPreferredWidth(100);

        scrollPane = new JScrollPane(productTable);

        // 底部按钮
        JPanel bottomPanel = new JPanel(new BorderLayout());
        JPanel btnPanel = new JPanel(new FlowLayout());
        viewDetailBtn = new JButton("查看详情");
        chatBtn = new JButton("联系卖家");
        manageBtn = new JButton("我的商品");
        btnPanel.add(viewDetailBtn);
        btnPanel.add(chatBtn);
        btnPanel.add(manageBtn);
        bottomPanel.add(btnPanel, BorderLayout.CENTER);
        bottomPanel.add(statusLabel, BorderLayout.SOUTH);

        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        // 事件
        searchBtn.addActionListener(e -> searchProducts());
        refreshBtn.addActionListener(e -> {
            currentPage = 1;
            hasMore = true;
            currentProducts = new JSONArray();
            loadProducts();
        });
        viewDetailBtn.addActionListener(e -> viewDetail());
        chatBtn.addActionListener(e -> openChat());
        manageBtn.addActionListener(e -> {
            new MyProductFrame().setVisible(true);
        });
        productTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    viewDetail();
                }
            }
        });

        // 滚动加载更多（监听滚动条变化）
        scrollPane.getVerticalScrollBar().addAdjustmentListener(e -> {
            if (!e.getValueIsAdjusting()) {
                checkScrollBottom();
            }
        });
    }

    /**
     * 检查是否滚动到底部，如果是则加载更多
     */
    private void checkScrollBottom() {
        if (!hasMore || isLoading) return;
        JScrollBar verticalBar = scrollPane.getVerticalScrollBar();
        int value = verticalBar.getValue();
        int extent = verticalBar.getModel().getExtent();
        int maximum = verticalBar.getMaximum();
        // 距离底部 100px 内触发加载
        if (value + extent >= maximum - 100) {
            loadMore();
        }
    }

    /**
     * 首次加载 / 刷新
     */
    private void loadProducts() {
        if (isLoading) return;
        isLoading = true;
        statusLabel.setText("加载中...");

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                Map<String, Object> params = new HashMap<>();
                params.put("page", 1);
                params.put("size", PAGE_SIZE);
                Result result = ApiClient.get("/product/list", params);
                if (result.isSuccess()) {
                    JSONObject data = (JSONObject) result.getData();
                    JSONArray records = data.getJSONArray("records");
                    long total = data.getLong("total");

                    currentProducts = records != null ? records : new JSONArray();
                    currentPage = 1;
                    hasMore = currentProducts.size() >= PAGE_SIZE;

                    SwingUtilities.invokeLater(() -> {
                        refreshTable(currentProducts);
                        statusLabel.setText("共 " + total + " 条商品");
                    });
                }
                return null;
            }

            @Override
            protected void done() {
                isLoading = false;
            }
        }.execute();
    }

    /**
     * 加载下一页
     */
    private void loadMore() {
        if (isLoading) return;
        isLoading = true;

        int pageToLoad = currentPage + 1;
        statusLabel.setText("加载更多...");

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                Map<String, Object> params = new HashMap<>();
                params.put("page", pageToLoad);
                params.put("size", PAGE_SIZE);
                Result result = ApiClient.get("/product/list", params);
                if (result.isSuccess()) {
                    JSONObject data = (JSONObject) result.getData();
                    JSONArray records = data.getJSONArray("records");
                    long total = data.getLong("total");

                    if (records != null && !records.isEmpty()) {
                        for (int i = 0; i < records.size(); i++) {
                            currentProducts.add(records.getJSONObject(i));
                        }
                        currentPage = pageToLoad;
                        hasMore = records.size() >= PAGE_SIZE;

                        SwingUtilities.invokeLater(() -> {
                            refreshTable(currentProducts);
                            statusLabel.setText("共 " + total + " 条商品");
                        });
                    } else {
                        hasMore = false;
                    }
                }
                return null;
            }

            @Override
            protected void done() {
                isLoading = false;
            }
        }.execute();
    }

    private void searchProducts() {
        String keyword = searchField.getText().trim();
        if (keyword.isEmpty()) {
            currentPage = 1;
            hasMore = true;
            currentProducts = new JSONArray();
            loadProducts();
            return;
        }

        statusLabel.setText("搜索中...");
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                Map<String, Object> params = new HashMap<>();
                params.put("keyword", keyword);
                Result result = ApiClient.get("/product/search", params);
                if (result.isSuccess()) {
                    JSONArray records = (JSONArray) result.getData();
                    currentProducts = records != null ? records : new JSONArray();
                    hasMore = false;

                    SwingUtilities.invokeLater(() -> {
                        refreshTable(currentProducts);
                        statusLabel.setText("搜索 \"" + keyword + "\" 找到 " + currentProducts.size() + " 条");
                    });
                }
                return null;
            }

            @Override
            protected void done() {
                isLoading = false;
            }
        }.execute();
    }

    private void refreshTable(JSONArray products) {
        tableModel.setRowCount(0);
        if (products == null) return;

        for (int i = 0; i < products.size(); i++) {
            JSONObject p = products.getJSONObject(i);
            String desc = p.getStr("description");
            if (desc != null && desc.length() > 30) {
                desc = desc.substring(0, 30) + "...";
            }
            // 格式化日期：取前 16 位 (yyyy-MM-dd HH:mm)
            String createdAt = p.getStr("createdAt");
            if (createdAt != null && createdAt.length() > 16) {
                createdAt = createdAt.substring(0, 16).replace("T", " ");
            }
            tableModel.addRow(new Object[]{
                    p.getLong("productId"),
                    p.getStr("name"),
                    p.getBigDecimal("price"),
                    createdAt,
                    p.getStr("sellerName"),
                    desc,
                    p.getStr("pickupPoint")
            });
        }
    }

    private void viewDetail() {
        int row = productTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "请先选择一个商品");
            return;
        }
        if (currentProducts == null || row >= currentProducts.size()) return;

        JSONObject p = currentProducts.getJSONObject(row);
        StringBuilder sb = new StringBuilder();
        sb.append("商品ID: ").append(p.getLong("productId")).append("\n\n");
        sb.append("名称: ").append(p.getStr("name")).append("\n\n");
        sb.append("价格: ").append(p.getBigDecimal("price")).append("\n\n");
        sb.append("商家: ").append(p.getStr("sellerName")).append("\n\n");
        String createdAt = p.getStr("createdAt");
        if (createdAt != null && createdAt.length() > 16) {
            createdAt = createdAt.substring(0, 16).replace("T", " ");
        }
        sb.append("上架时间: ").append(createdAt).append("\n\n");
        sb.append("自提点: ").append(p.getStr("pickupPoint")).append("\n\n");
        sb.append("详细介绍:\n").append(p.getStr("description"));

        JTextArea textArea = new JTextArea(sb.toString());
        textArea.setEditable(false);
        textArea.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        JScrollPane sp = new JScrollPane(textArea);
        sp.setPreferredSize(new Dimension(450, 400));

        JOptionPane.showMessageDialog(this, sp, "商品详情", JOptionPane.INFORMATION_MESSAGE);
    }

    private void openChat() {
        int row = productTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "请先选择一个商品");
            return;
        }
        if (currentProducts == null || row >= currentProducts.size()) return;

        JSONObject p = currentProducts.getJSONObject(row);
        Long sellerId = p.getLong("userId");
        Long productId = p.getLong("productId");
        String sellerName = p.getStr("sellerName");

        if (sellerId.equals(ApiClient.getCurrentUserId())) {
            JOptionPane.showMessageDialog(this, "不能和自己聊天");
            return;
        }

        new ChatFrame(sellerId, sellerName, productId).setVisible(true);
    }
}
