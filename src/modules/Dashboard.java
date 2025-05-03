package modules;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import java.awt.FlowLayout;
import javax.swing.JButton;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JTextField;


public class Dashboard extends javax.swing.JPanel {

    private JLabel totalItemsValueLabel;
    private JLabel lowStockValueLabel;
    private JLabel pendingOrdersValueLabel;
    private JLabel totalUsersValueLabel;
    private DefaultTableModel recentActivitiesModel;
    private DefaultPieDataset categoryDataset;
    private DefaultCategoryDataset movementDataset;

    private int currentPage = 1;
    private final int itemsPerPage = 10;
    private int totalActivities = 0;
    private int totalPages = 1;
    private JButton prevButton;
    private JButton nextButton;
    private JLabel pageInfoLabel;
    private JTable recentActivitiesTable;

    private JTextField searchActivityField;
    private JButton searchActivityButton;
    private String currentActivitySearchText = "";


    private static final String DB_URL = "jdbc:mysql://127.0.0.1:3307/assetwise_academia";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "root";

    public Dashboard() {
        initComponents();
        setupDashboard();
        fetchStatsData();
        fetchInventoryCategoryData();
        fetchInventoryMovementData();
        fetchRecentActivities();
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    private void setupDashboard() {
        this.removeAll();
        this.setLayout(new BorderLayout(10, 10));
        this.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        this.setBackground(new Color(30, 30, 30));

        JPanel statsPanel = createStatsPanel();
        this.add(statsPanel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new GridLayout(2, 1, 0, 10));
        centerPanel.setOpaque(false);

        JPanel chartsPanel = createChartsPanel();
        centerPanel.add(chartsPanel);

        JPanel recentActivitiesPanel = createRecentActivitiesPanel();
        centerPanel.add(recentActivitiesPanel);

        this.add(centerPanel, BorderLayout.CENTER);

        JPanel quickAccessPanel = createQuickAccessPanel();
        this.add(quickAccessPanel, BorderLayout.SOUTH);

        this.revalidate();
        this.repaint();
    }

    private JPanel createStatsPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 4, 10, 0));
        panel.setOpaque(false);

        JPanel totalItemsPanel = createStatBox("Total Items", "0", new Color(41, 128, 185));
        totalItemsValueLabel = (JLabel) totalItemsPanel.getComponent(1);
        panel.add(totalItemsPanel);

        JPanel lowStockPanel = createStatBox("Low Stock", "0", new Color(231, 76, 60));
        lowStockValueLabel = (JLabel) lowStockPanel.getComponent(1);
        panel.add(lowStockPanel);

        JPanel pendingOrdersPanel = createStatBox("Pending Orders", "0", new Color(243, 156, 18));
        pendingOrdersValueLabel = (JLabel) pendingOrdersPanel.getComponent(1);
        panel.add(pendingOrdersPanel);

        JPanel usersPanel = createStatBox("Total Users", "0", new Color(46, 204, 113));
        totalUsersValueLabel = (JLabel) usersPanel.getComponent(1);
        panel.add(usersPanel);

        return panel;
    }

    private JPanel createStatBox(String title, String initialValue, Color color) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(color);
        panel.setPreferredSize(new Dimension(150, 100));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("Verdana", Font.BOLD, 14));
        panel.add(titleLabel, BorderLayout.NORTH);

        JLabel valueLabel = new JLabel(initialValue);
        valueLabel.setForeground(Color.WHITE);
        valueLabel.setFont(new Font("Verdana", Font.BOLD, 24));
        valueLabel.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(valueLabel, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createChartsPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 10, 0));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.GRAY),
                "Inventory Analytics", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
                javax.swing.border.TitledBorder.DEFAULT_POSITION,
                new Font("Verdana", Font.BOLD, 14), Color.WHITE));

        categoryDataset = new DefaultPieDataset();
        ChartPanel categoryChart = createPieChart("Inventory by Category", categoryDataset);
        panel.add(categoryChart);

        movementDataset = new DefaultCategoryDataset();
        ChartPanel movementChart = createBarChart("Inventory Movement (Last 4 Months)", "Month", "Quantity", movementDataset);
        panel.add(movementChart);

        return panel;
    }

    private ChartPanel createPieChart(String title, DefaultPieDataset dataset) {
        JFreeChart chart = ChartFactory.createPieChart(
                title,
                dataset,
                true,
                true,
                false
        );
        customizeChart(chart);
        PiePlot plot = (PiePlot) chart.getPlot();
        customizePlot(plot);
        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setOpaque(false);
        chartPanel.setPreferredSize(new Dimension(400, 300));
        return chartPanel;
    }

    private ChartPanel createBarChart(String title, String categoryAxisLabel, String valueAxisLabel, DefaultCategoryDataset dataset) {
        JFreeChart chart = ChartFactory.createBarChart(
                title,
                categoryAxisLabel,
                valueAxisLabel,
                dataset,
                PlotOrientation.VERTICAL,
                true,
                true,
                false
        );
        customizeChart(chart);
        CategoryPlot plot = (CategoryPlot) chart.getPlot();
        customizePlot(plot);
        BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setSeriesPaint(0, new Color(46, 204, 113));
        renderer.setSeriesPaint(1, new Color(231, 76, 60));
        plot.setRangeGridlinePaint(new Color(50, 50, 50));
        plot.getDomainAxis().setLabelPaint(Color.WHITE);
        plot.getDomainAxis().setTickLabelPaint(Color.WHITE);
        plot.getRangeAxis().setLabelPaint(Color.WHITE);
        plot.getRangeAxis().setTickLabelPaint(Color.WHITE);
        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setOpaque(false);
        chartPanel.setPreferredSize(new Dimension(400, 300));
        return chartPanel;
    }

    private void customizeChart(JFreeChart chart) {
        chart.setBackgroundPaint(new Color(30, 30, 30));
        chart.getTitle().setPaint(Color.WHITE);
        chart.getLegend().setBackgroundPaint(new Color(30, 30, 30));
        chart.getLegend().setItemPaint(Color.WHITE);
    }

    private void customizePlot(org.jfree.chart.plot.Plot plot) {
        plot.setBackgroundPaint(new Color(30, 30, 30));
        plot.setOutlinePaint(new Color(30, 30, 30));
    }

    private JPanel createRecentActivitiesPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 5));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.GRAY),
                "Recent Activities", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
                javax.swing.border.TitledBorder.DEFAULT_POSITION,
                new Font("Verdana", Font.BOLD, 14), Color.WHITE));

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        searchPanel.setOpaque(false);

        JLabel searchLabel = new JLabel("Search Activities:");
        searchLabel.setFont(new Font("Verdana", Font.BOLD, 12));
        searchLabel.setForeground(Color.WHITE);
        searchPanel.add(searchLabel);

        searchActivityField = new JTextField(20);
        searchActivityField.setFont(new Font("Verdana", Font.PLAIN, 12));
        searchPanel.add(searchActivityField);

        searchActivityButton = new JButton("Search");
        stylePaginationButton(searchActivityButton);
        searchActivityButton.addActionListener(e -> {
            currentActivitySearchText = searchActivityField.getText().trim();
            currentPage = 1;
            fetchRecentActivities();
        });
        searchPanel.add(searchActivityButton);

        panel.add(searchPanel, BorderLayout.NORTH);

        String[] columns = {"Date", "Activity", "User", "Details"};
        recentActivitiesModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        recentActivitiesTable = new JTable(recentActivitiesModel);
        recentActivitiesTable.setForeground(Color.WHITE);
        recentActivitiesTable.setBackground(new Color(30, 30, 30));
        recentActivitiesTable.setGridColor(new Color(50, 50, 50));
        recentActivitiesTable.getTableHeader().setFont(new Font("Verdana", Font.BOLD, 12));
        recentActivitiesTable.getTableHeader().setBackground(new Color(40, 40, 40));
        recentActivitiesTable.getTableHeader().setForeground(Color.WHITE);
        recentActivitiesTable.getColumnModel().getColumn(0).setPreferredWidth(140);
        recentActivitiesTable.getColumnModel().getColumn(1).setPreferredWidth(100);
        recentActivitiesTable.getColumnModel().getColumn(2).setPreferredWidth(120);
        recentActivitiesTable.getColumnModel().getColumn(3).setPreferredWidth(300);

        JScrollPane scrollPane = new JScrollPane(recentActivitiesTable);
        scrollPane.getViewport().setBackground(new Color(30, 30, 30));
        panel.add(scrollPane, BorderLayout.CENTER);

        JPanel paginationPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        paginationPanel.setOpaque(false);

        prevButton = new JButton("<< Previous");
        stylePaginationButton(prevButton);
        prevButton.addActionListener(e -> changeActivityPage(-1));

        pageInfoLabel = new JLabel("Page 1 of 1");
        pageInfoLabel.setForeground(Color.WHITE);
        pageInfoLabel.setFont(new Font("Verdana", Font.PLAIN, 12));

        nextButton = new JButton("Next >>");
        stylePaginationButton(nextButton);
        nextButton.addActionListener(e -> changeActivityPage(1));

        paginationPanel.add(prevButton);
        paginationPanel.add(pageInfoLabel);
        paginationPanel.add(nextButton);

        panel.add(paginationPanel, BorderLayout.SOUTH);

        return panel;
    }

    private void stylePaginationButton(JButton button) {
        button.setFont(new Font("Verdana", Font.PLAIN, 12));
        button.setBackground(new Color(70, 70, 70));
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.GRAY, 1),
            BorderFactory.createEmptyBorder(3, 8, 3, 8)
        ));
    }

    private void fetchStatsData() {
        try (Connection dbConn = getConnection()) {

            try (PreparedStatement totalItemsStmt = dbConn.prepareStatement("SELECT COUNT(*) FROM Items");
                 ResultSet totalItemsRs = totalItemsStmt.executeQuery()) {
                if (totalItemsRs.next()) {
                    final String totalItems = String.valueOf(totalItemsRs.getInt(1));
                    SwingUtilities.invokeLater(() -> totalItemsValueLabel.setText(totalItems));
                }
            } catch (SQLException e) {
                System.err.println("Error fetching total items: " + e.getMessage());
                e.printStackTrace();
                logAndSetNA("total items", e);
            }

            try (PreparedStatement lowStockStmt = dbConn.prepareStatement("SELECT COUNT(*) FROM Items WHERE Quantity <= ReorderLevel");
                 ResultSet lowStockRs = lowStockStmt.executeQuery()) {
                if (lowStockRs.next()) {
                    final String lowStockItems = String.valueOf(lowStockRs.getInt(1));
                    SwingUtilities.invokeLater(() -> lowStockValueLabel.setText(lowStockItems));
                }
            } catch (SQLException e) {
                System.err.println("Error fetching low stock items: " + e.getMessage());
                e.printStackTrace();
                logAndSetNA("low stock", e);
            }

            try (PreparedStatement pendingOrdersStmt = dbConn.prepareStatement("SELECT COUNT(*) FROM PurchaseOrders WHERE Status = 'Pending'");
                 ResultSet pendingOrdersRs = pendingOrdersStmt.executeQuery()) {
                if (pendingOrdersRs.next()) {
                    final String pendingOrders = String.valueOf(pendingOrdersRs.getInt(1));
                    SwingUtilities.invokeLater(() -> pendingOrdersValueLabel.setText(pendingOrders));
                }
            } catch (SQLException e) {
                System.err.println("Error fetching pending orders: " + e.getMessage());
                e.printStackTrace();
                logAndSetNA("pending orders", e);
            }

            try (PreparedStatement totalUsersStmt = dbConn.prepareStatement("SELECT COUNT(*) FROM Users");
                 ResultSet totalUsersRs = totalUsersStmt.executeQuery()) {
                if (totalUsersRs.next()) {
                    final String totalUsers = String.valueOf(totalUsersRs.getInt(1));
                    SwingUtilities.invokeLater(() -> totalUsersValueLabel.setText(totalUsers));
                }
            } catch (SQLException e) {
                System.err.println("Error fetching total users: " + e.getMessage());
                e.printStackTrace();
                logAndSetNA("total users", e);
            }

        } catch (SQLException e) {
            System.err.println("Error connecting to database or general SQL error: " + e.getMessage());
            e.printStackTrace();
            SwingUtilities.invokeLater(() -> {
                if(totalItemsValueLabel != null) totalItemsValueLabel.setText("N/A");
                if(lowStockValueLabel != null) lowStockValueLabel.setText("N/A");
                if(pendingOrdersValueLabel != null) pendingOrdersValueLabel.setText("N/A");
                if(totalUsersValueLabel != null) totalUsersValueLabel.setText("N/A");
            });
        }
    }

    private void logAndSetNA(String statType, SQLException e) {
        System.err.println("Database error fetching " + statType + ": " + e.getMessage());
        SwingUtilities.invokeLater(() -> {
            if(totalItemsValueLabel != null) totalItemsValueLabel.setText("N/A");
            if(lowStockValueLabel != null) lowStockValueLabel.setText("N/A");
            if(pendingOrdersValueLabel != null) pendingOrdersValueLabel.setText("N/A");
            if(totalUsersValueLabel != null) totalUsersValueLabel.setText("N/A");
        });
    }

    private String fetchUsername(int userId) {
        String fullName = null;

        String sql = "SELECT FullName FROM Users WHERE UserID = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    fullName = rs.getString("FullName");
                } else {
                    System.err.println("Warning: User with ID " + userId + " not found.");
                    fullName = "Unknown User";
                }
            }

        } catch (SQLException e) {
            System.err.println("Error fetching full name for user ID " + userId + ": " + e.getMessage());
            e.printStackTrace();
            fullName = "Error";
        }

        return fullName;
    }

    private JPanel createQuickAccessPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 3, 20, 0));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));

        javax.swing.JButton addItemButton = new javax.swing.JButton("Add New Item");
        addItemButton.setFont(new Font("Verdana", Font.BOLD, 14));
        addItemButton.setBackground(new Color(41, 128, 185));
        addItemButton.setForeground(Color.WHITE);
        addItemButton.setFocusPainted(false);
        panel.add(addItemButton);

        javax.swing.JButton purchaseOrderButton = new javax.swing.JButton("Create Purchase Order");
        purchaseOrderButton.setFont(new Font("Verdana", Font.BOLD, 14));
        purchaseOrderButton.setBackground(new Color(46, 204, 113));
        purchaseOrderButton.setForeground(Color.WHITE);
        purchaseOrderButton.setFocusPainted(false);
        panel.add(purchaseOrderButton);

        javax.swing.JButton reportButton = new javax.swing.JButton("Generate Report");
        reportButton.setFont(new Font("Verdana", Font.BOLD, 14));
        reportButton.setBackground(new Color(243, 156, 18));
        reportButton.setForeground(Color.WHITE);
        reportButton.setFocusPainted(false);
        panel.add(reportButton);

        return panel;
    }

    private void fetchRecentActivities() {
        System.out.println("Fetching recent activities for page " + currentPage + " with search: '" + currentActivitySearchText + "'");

        StringBuilder activityFilterClause = new StringBuilder("WHERE (ra.ActivityType LIKE 'Item Added%' OR ra.ActivityType LIKE 'Item Updated%' OR ra.ActivityType LIKE 'Item Deleted%' OR ra.ActivityType = 'New User' OR ra.ActivityType = 'User Login')");

        if (currentActivitySearchText != null && !currentActivitySearchText.isEmpty()) {
            activityFilterClause.append(" AND (ra.ActivityType LIKE ? OR ra.Details LIKE ? OR u.FullName LIKE ?)");
        }

        String countSql = "SELECT COUNT(*) FROM RecentActivities ra " +
                          "LEFT JOIN Users u ON ra.UserID = u.UserID " +
                          activityFilterClause.toString();

        totalActivities = 0;
        totalPages = 1;

        try (Connection dbConn = getConnection();
             PreparedStatement countPstmt = dbConn.prepareStatement(countSql)) {

            if (currentActivitySearchText != null && !currentActivitySearchText.isEmpty()) {
                String searchTerm = "%" + currentActivitySearchText.toLowerCase() + "%";
                countPstmt.setString(1, searchTerm);
                countPstmt.setString(2, searchTerm);
                countPstmt.setString(3, searchTerm);
            }

            try (ResultSet countRs = countPstmt.executeQuery()) {
                 if (countRs.next()) {
                    totalActivities = countRs.getInt(1);
                }
            }

            totalPages = (int) Math.ceil((double) totalActivities / itemsPerPage);
            if (totalPages == 0) {
                totalPages = 1;
            }
            if (currentPage > totalPages) {
                currentPage = totalPages;
            }
            if (currentPage < 1 && totalPages >= 1) {
                currentPage = 1;
            }

            System.out.println("Total activities (filtered): " + totalActivities + ", Total pages: " + totalPages);

        } catch (SQLException e) {
            System.err.println("Error counting recent activities with filter: " + e.getMessage());
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> recentActivitiesModel.setRowCount(0));

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        int offset = (currentPage - 1) * itemsPerPage;

        String dataSql = "SELECT ra.ActivityDate, ra.ActivityType, u.FullName AS UserName, ra.Details " +
                         "FROM RecentActivities ra " +
                         "LEFT JOIN Users u ON ra.UserID = u.UserID " +
                         activityFilterClause.toString() +
                         " ORDER BY ra.ActivityDate DESC " +
                         "LIMIT ? OFFSET ?";

        try (Connection dbConn = getConnection();
             PreparedStatement pstmt = dbConn.prepareStatement(dataSql)) {

            int paramIndex = 1;
            if (currentActivitySearchText != null && !currentActivitySearchText.isEmpty()) {
                String searchTerm = "%" + currentActivitySearchText.toLowerCase() + "%";
                pstmt.setString(paramIndex++, searchTerm);
                pstmt.setString(paramIndex++, searchTerm);
                pstmt.setString(paramIndex++, searchTerm);
            }

            pstmt.setInt(paramIndex++, itemsPerPage);
            pstmt.setInt(paramIndex++, offset);

            try (ResultSet rs = pstmt.executeQuery()) {
                boolean dataFoundOnPage = false;
                while (rs.next()) {
                    dataFoundOnPage = true;
                    LocalDateTime dateTime = null;
                    if (rs.getTimestamp("ActivityDate") != null) {
                        dateTime = rs.getTimestamp("ActivityDate").toLocalDateTime();
                    }
                    String formattedDate = (dateTime != null) ? dtf.format(dateTime) : "Invalid Date";
                    String activityType = rs.getString("ActivityType");
                    String userName = rs.getString("UserName");
                    String details = rs.getString("Details");
                    String displayUser = (userName != null && !userName.trim().isEmpty()) ? userName : "System/Unknown";

                    final Object[] rowData = {formattedDate, activityType, displayUser, details};
                    SwingUtilities.invokeLater(() -> recentActivitiesModel.addRow(rowData));
                }
                 if (!dataFoundOnPage && totalActivities == 0) {
                     System.out.println("No activities found in total (with filter).");
                     SwingUtilities.invokeLater(() -> {
                         recentActivitiesModel.setRowCount(0);
                         recentActivitiesModel.addRow(new Object[]{"", "No matching activities found", "", ""});
                     });
                 } else if (!dataFoundOnPage) {
                     System.out.println("No activities found on page " + currentPage + " (with filter).");
                      SwingUtilities.invokeLater(() -> {
                          recentActivitiesModel.setRowCount(0);
                          recentActivitiesModel.addRow(new Object[]{"", "No activities on this page", "", ""});
                      });
                 }
            }

        } catch (SQLException e) {
            System.err.println("Error fetching recent activities data for page " + currentPage + " with filter: " + e.getMessage());
            e.printStackTrace();
            SwingUtilities.invokeLater(() -> {
                recentActivitiesModel.setRowCount(0);
                recentActivitiesModel.addRow(new Object[]{"N/A", "Error", "System", "Could not fetch activities"});
            });
        } finally {
            SwingUtilities.invokeLater(this::updatePaginationControls);
        }
    }

    private void changeActivityPage(int direction) {
        int newPage = currentPage + direction;
        if (newPage >= 1 && newPage <= totalPages) {
            currentPage = newPage;
            new Thread(this::fetchRecentActivities).start();
        } else {
             System.out.println("Change page ignored: newPage (" + newPage + ") out of bounds (1-" + totalPages + ")");
        }
    }

    private void updatePaginationControls() {
        if (pageInfoLabel != null) {
            pageInfoLabel.setText("Page " + currentPage + " of " + totalPages);
        }
        if (prevButton != null) {
            prevButton.setEnabled(currentPage > 1);
        }
        if (nextButton != null) {
            nextButton.setEnabled(currentPage < totalPages);
        }
         System.out.println("Pagination controls updated: Page " + currentPage + "/" + totalPages);
    }

    private void fetchInventoryCategoryData() {
        categoryDataset.clear();
        String sql = "SELECT c.CategoryName, COUNT(i.ItemID) AS ItemCount FROM Categories c LEFT JOIN Items i ON c.CategoryID = i.CategoryID GROUP BY c.CategoryName";
        try (Connection dbConn = getConnection();
             PreparedStatement pstmt = dbConn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                final String categoryName = rs.getString("CategoryName");
                final double itemCount = rs.getDouble("ItemCount");
                SwingUtilities.invokeLater(() -> categoryDataset.setValue(categoryName, itemCount));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching inventory category data: " + e.getMessage());
            e.printStackTrace();
            SwingUtilities.invokeLater(() -> categoryDataset.setValue("Error", 1));
        }
    }

    private void fetchInventoryMovementData() {
        movementDataset.clear();
        String sql = "SELECT " +
                     "    DATE_FORMAT(TransactionDate, '%Y-%m') AS Month, " +
                     "    TransactionType, " +
                     "    SUM(Quantity) AS TotalQuantity " +
                     "FROM Transactions " +
                     "WHERE TransactionDate >= DATE_SUB(CURDATE(), INTERVAL 4 MONTH) " +
                     "GROUP BY DATE_FORMAT(TransactionDate, '%Y-%m'), TransactionType " +
                     "ORDER BY Month";
        try (Connection dbConn = getConnection();
             PreparedStatement pstmt = dbConn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                final String month = rs.getString("Month");
                final String transactionType = rs.getString("TransactionType");
                final double totalQuantity = rs.getDouble("TotalQuantity");
                SwingUtilities.invokeLater(() -> movementDataset.addValue(totalQuantity, transactionType, month));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching inventory movement data: " + e.getMessage());
            e.printStackTrace();
            SwingUtilities.invokeLater(() -> {
                movementDataset.addValue(0, "Received", "Error");
                movementDataset.addValue(0, "Issued", "Error");
            });
        }
    }
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();

        jPanel1.setBackground(new java.awt.Color(0, 0, 0));
        jPanel1.setForeground(new java.awt.Color(255, 255, 255));
        jPanel1.setPreferredSize(new java.awt.Dimension(834, 644));
        jPanel1.setLayout(new java.awt.BorderLayout());

        jPanel2.setLayout(new java.awt.GridLayout(1, 0));

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 834, Short.MAX_VALUE)
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 644, Short.MAX_VALUE)
        );

        jPanel2.add(jPanel3);

        jPanel1.add(jPanel2, java.awt.BorderLayout.CENTER);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 834, Short.MAX_VALUE)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addGap(0, 0, Short.MAX_VALUE)
                    .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGap(0, 0, Short.MAX_VALUE)))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 644, Short.MAX_VALUE)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addGap(0, 0, Short.MAX_VALUE)
                    .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGap(0, 0, Short.MAX_VALUE)))
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    // End of variables declaration//GEN-END:variables
}
