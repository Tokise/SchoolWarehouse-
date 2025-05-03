package modules;

import Package1.DBConnection;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;


public class Inventory extends javax.swing.JPanel {

    private JTable inventoryTable;
    private JTextField searchField;
    private JComboBox<String> categoryFilter;
    private DefaultTableModel tableModel;

    private JPanel detailPanel;
    private JTextField itemIdField;
    private JTextField nameField;
    private JComboBox<String> categoryField;
    private JTextField quantityField;
    private JTextField reorderLevelField;
    private JComboBox<String> unitField;
    private JLabel imageLabel;
    private File selectedImageFile;
    private String selectedImageType;
    private boolean isNewItem = true;

    private JButton saveButton;
    private JButton deleteButton;
    private JButton cancelButton;

    private Connection conn = null;
    private int currentUserId;

    private int currentPage = 1;
    private final int itemsPerPage = 10;
    private int totalItems = 0;
    private JButton jButtonPreviousPage;
    private JButton jButtonNextPage;
    private JLabel jLabelPageInfo;

    private JButton jButtonAddCategory;


    public Inventory() {
        if (!connectToDatabase()) {
            JOptionPane.showMessageDialog(this, "Database connection failed. Inventory features disabled.", "Connection Error", JOptionPane.ERROR_MESSAGE);
        }
        initComponents();
        setupInventoryPanel();
        loadCategories();
    }


    public void setCurrentUserId(int userId) {
        this.currentUserId = userId;
        System.out.println("Inventory: UserID set to " + this.currentUserId);
        currentPage = 1;
        fetchTotalItemCount();
    }


    private boolean connectToDatabase() {
        try {
            conn = DBConnection.getConnection();
            if (conn != null && !conn.isClosed()) {
                System.out.println("Database connected successfully");
                return true;
            } else {
                System.err.println("Failed to establish database connection.");
                return false;
            }
        } catch (SQLException ex) {
            Logger.getLogger(Inventory.class.getName()).log(Level.SEVERE, "Database connection error", ex);
            return false;
        }
    }


    private void setupInventoryPanel() {
        this.setLayout(new BorderLayout(10, 10));
        this.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        this.setBackground(new Color(30, 30, 30));

        JPanel searchFilterPanel = createSearchFilterPanel();
        this.add(searchFilterPanel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new BorderLayout(10, 0));
        centerPanel.setOpaque(false);

        JPanel tablePanel = createInventoryTablePanel();
        centerPanel.add(tablePanel, BorderLayout.CENTER);

        detailPanel = createItemDetailsPanel();
        detailPanel.setVisible(false);
        centerPanel.add(detailPanel, BorderLayout.EAST);

        this.add(centerPanel, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setOpaque(false);

        JPanel addCategoryPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        addCategoryPanel.setOpaque(false);
        jButtonAddCategory = new JButton("Add Category");
        styleAddCategoryButton(jButtonAddCategory);
        jButtonAddCategory.addActionListener(e -> showAddCategoryDialog());
        addCategoryPanel.add(jButtonAddCategory);
        bottomPanel.add(addCategoryPanel, BorderLayout.WEST);

        JPanel actionPanel = createActionPanel();
        bottomPanel.add(actionPanel, BorderLayout.EAST);

        this.add(bottomPanel, BorderLayout.SOUTH);

        inventoryTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent evt) {
                int selectedRow = inventoryTable.getSelectedRow();
                if (selectedRow >= 0) {
                    int modelRow = inventoryTable.convertRowIndexToModel(selectedRow);
                    Object itemIdObj = tableModel.getValueAt(modelRow, 0);
                    if (itemIdObj != null) {
                        try {
                            int itemId = Integer.parseInt(itemIdObj.toString());
                            loadItemDetails(itemId);
                        } catch (NumberFormatException e) {
                            System.err.println("Error parsing Item ID from table: " + itemIdObj);
                        }
                    }
                }
            }
        });
    }


    private JPanel createSearchFilterPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        panel.setOpaque(false);

        JLabel searchLabel = new JLabel("Search:");
        searchLabel.setFont(new Font("Verdana", Font.BOLD, 14));
        searchLabel.setForeground(Color.WHITE);
        panel.add(searchLabel);

        searchField = new JTextField(20);
        searchField.setFont(new Font("Verdana", Font.PLAIN, 14));
        panel.add(searchField);

        JLabel categoryLabel = new JLabel("Category:");
        categoryLabel.setFont(new Font("Verdana", Font.BOLD, 14));
        categoryLabel.setForeground(Color.WHITE);
        panel.add(categoryLabel);

        categoryFilter = new JComboBox<>();
        categoryFilter.addItem("All Categories");
        categoryFilter.setFont(new Font("Verdana", Font.PLAIN, 14));
        panel.add(categoryFilter);

        JButton searchBtn = new JButton("Search");
        searchBtn.setFont(new Font("Verdana", Font.BOLD, 14));
        searchBtn.setBackground(new Color(41, 128, 185));
        searchBtn.setForeground(Color.WHITE);
        searchBtn.addActionListener((ActionEvent e) -> searchInventory());
        panel.add(searchBtn);

        return panel;
    }


    private JPanel createInventoryTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.GRAY),
                "Inventory Items", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
                javax.swing.border.TitledBorder.DEFAULT_POSITION,
                new Font("Verdana", Font.BOLD, 14), Color.WHITE));

        // Removed "Unit" column from the table definition
        String[] columns = {"Item ID", "Name", "Category", "Quantity", "Status", "Image"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                switch (columnIndex) {
                    case 0:
                        return Integer.class;
                    case 3:
                        return String.class;
                    case 5: // Image is now at index 5
                        return ImageIcon.class;
                    default:
                        return String.class;
                }
            }
        };

        inventoryTable = new JTable(tableModel);
        inventoryTable.setForeground(Color.WHITE);
        inventoryTable.setBackground(new Color(30, 30, 30));
        inventoryTable.setGridColor(new Color(50, 50, 50));
        inventoryTable.setSelectionBackground(new Color(41, 128, 185));
        inventoryTable.setSelectionForeground(Color.WHITE);
        inventoryTable.setFont(new Font("Verdana", Font.PLAIN, 12));
        inventoryTable.setRowHeight(60);
        inventoryTable.setAutoCreateRowSorter(true);

        inventoryTable.getTableHeader().setFont(new Font("Verdana", Font.BOLD, 12));
        inventoryTable.getTableHeader().setBackground(new Color(40, 40, 40));
        inventoryTable.getTableHeader().setForeground(Color.WHITE);
        inventoryTable.getTableHeader().setReorderingAllowed(false);

        // Custom Cell Renderer for centering text in columns
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);

        // Apply center renderer to relevant columns (indices updated)
        inventoryTable.getColumnModel().getColumn(0).setCellRenderer(centerRenderer); // Item ID
        inventoryTable.getColumnModel().getColumn(1).setCellRenderer(centerRenderer); // Name
        inventoryTable.getColumnModel().getColumn(2).setCellRenderer(centerRenderer); // Category
        inventoryTable.getColumnModel().getColumn(3).setCellRenderer(centerRenderer); // Quantity

        // Custom Cell Renderer for Status Column (Index 4) - Center Align and Color (Index updated)
        inventoryTable.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                String status = value != null ? value.toString() : "";
                if ("Low Stock".equalsIgnoreCase(status)) {
                    c.setForeground(new Color(231, 76, 60));
                } else if ("Out of Stock".equalsIgnoreCase(status)) {
                    c.setForeground(new Color(255, 165, 0));
                } else if ("In Stock".equalsIgnoreCase(status)) {
                    c.setForeground(new Color(46, 204, 113));
                } else {
                    c.setForeground(isSelected ? table.getSelectionForeground() : Color.WHITE);
                }
                c.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
                setHorizontalAlignment(JLabel.CENTER); // Ensure centering
                return c;
            }
        });

        // Custom Cell Renderer for Image Column (Index 5) - Center Align and display Icon (Index updated)
        inventoryTable.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                label.setText(""); // Clear any text
                if (value instanceof ImageIcon) {
                    label.setIcon((ImageIcon) value); // Set the icon
                } else {
                    label.setIcon(null); // No icon if value is not ImageIcon
                    label.setText("No Image"); // Or some placeholder text
                }
                label.setHorizontalAlignment(JLabel.CENTER);
                label.setVerticalAlignment(JLabel.CENTER);
                label.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
                return label;
            }
        });


        // Set preferred column widths (indices updated)
        inventoryTable.getColumnModel().getColumn(0).setPreferredWidth(60);
        inventoryTable.getColumnModel().getColumn(1).setPreferredWidth(160);
        inventoryTable.getColumnModel().getColumn(2).setPreferredWidth(100);
        inventoryTable.getColumnModel().getColumn(3).setPreferredWidth(100);
        // Removed width settings for the hidden Unit column
        inventoryTable.getColumnModel().getColumn(4).setPreferredWidth(80); // Status is now at index 4
        inventoryTable.getColumnModel().getColumn(5).setPreferredWidth(70); // Image is now at index 5

        JScrollPane scrollPane = new JScrollPane(inventoryTable);
        scrollPane.getViewport().setBackground(new Color(30, 30, 30));
        panel.add(scrollPane, BorderLayout.CENTER);

        JPanel paginationPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        paginationPanel.setOpaque(false);
        jButtonPreviousPage = new JButton("Previous");
        stylePaginationButton(jButtonPreviousPage);
        jButtonPreviousPage.addActionListener(e -> gotoPreviousPage());
        paginationPanel.add(jButtonPreviousPage);
        jLabelPageInfo = new JLabel("Page 1 of 1");
        jLabelPageInfo.setFont(new Font("Verdana", Font.PLAIN, 12));
        jLabelPageInfo.setForeground(Color.WHITE);
        paginationPanel.add(jLabelPageInfo);
        jButtonNextPage = new JButton("Next");
        stylePaginationButton(jButtonNextPage);
        jButtonNextPage.addActionListener(e -> gotoNextPage());
        paginationPanel.add(jButtonNextPage);
        panel.add(paginationPanel, BorderLayout.SOUTH);

        return panel;
    }


    private JPanel createItemDetailsPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.GRAY),
                "Item Details", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
                javax.swing.border.TitledBorder.DEFAULT_POSITION,
                new Font("Verdana", Font.BOLD, 14), Color.WHITE));
        panel.setPreferredSize(new Dimension(350, 0));

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        String[] units = {"pcs", "boxes", "packs", "reams", "liters", "kg", "meters", "rolls"};
        unitField = new JComboBox<>(units);
        unitField.setEditable(true);

        addField(formPanel, gbc, 0, "Item ID:", itemIdField = createNonEditableTextField(), 0.0);
        addField(formPanel, gbc, 1, "Name:", nameField = createEditableTextField(), 0.0);
        addField(formPanel, gbc, 2, "Category:", categoryField = new JComboBox<>(), 0.0);
        addField(formPanel, gbc, 3, "Quantity:", quantityField = createEditableTextField(), 0.0);
        addField(formPanel, gbc, 4, "Unit:", unitField, 0.0);
        addField(formPanel, gbc, 5, "Reorder Lvl:", reorderLevelField = createEditableTextField(), 0.0);

        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.weighty = 0.0;
        JPanel imagePanel = new JPanel(new BorderLayout(0, 5));
        imagePanel.setOpaque(false);
        imagePanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.GRAY),
                "Item Image", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
                javax.swing.border.TitledBorder.DEFAULT_POSITION, new Font("Verdana", Font.BOLD, 12), Color.WHITE));
        imageLabel = new JLabel("No Image Selected", SwingConstants.CENTER);
        imageLabel.setForeground(Color.LIGHT_GRAY);
        imageLabel.setPreferredSize(new Dimension(200, 150));
        imageLabel.setMinimumSize(new Dimension(150, 100));
        imageLabel.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
        imagePanel.add(imageLabel, BorderLayout.CENTER);
        JButton browseButton = new JButton("Browse...");
        browseButton.setFont(new Font("Verdana", Font.PLAIN, 12));
        browseButton.addActionListener((ActionEvent e) -> browseImage());
        imagePanel.add(browseButton, BorderLayout.SOUTH);
        formPanel.add(imagePanel, gbc);

        gbc.gridx = 0;
        gbc.gridy = 7;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.VERTICAL;
        formPanel.add(new JLabel(), gbc);

        panel.add(formPanel, BorderLayout.CENTER);
        return panel;
    }


    private JTextField createEditableTextField() {
        JTextField textField = new JTextField(15);
        textField.setFont(new Font("Verdana", Font.PLAIN, 12));
        return textField;
    }


    private JTextField createNonEditableTextField() {
        JTextField textField = new JTextField(15);
        textField.setFont(new Font("Verdana", Font.PLAIN, 12));
        textField.setEditable(false);
        textField.setBackground(Color.DARK_GRAY);
        textField.setForeground(Color.LIGHT_GRAY);
        return textField;
    }


    private void addField(JPanel panel, GridBagConstraints gbc, int y, String labelText, Component component, double weighty) {
        gbc.gridx = 0;
        gbc.gridy = y;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0.0;
        gbc.weighty = weighty;
        JLabel label = new JLabel(labelText);
        label.setForeground(Color.WHITE);
        label.setFont(new Font("Verdana", Font.PLAIN, 12));
        panel.add(label, gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        if (component instanceof JComboBox) {
            ((JComboBox<?>) component).setFont(new Font("Verdana", Font.PLAIN, 12));
        } else if (component instanceof JTextField) {
            ((JTextField) component).setFont(new Font("Verdana", Font.PLAIN, 12));
        }
        panel.add(component, gbc);
    }


    private JPanel createActionPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        panel.setOpaque(false);
        JButton addButton = new JButton("Add New");
        styleActionButton(addButton, new Color(46, 204, 113));
        addButton.addActionListener((ActionEvent e) -> prepareNewItem());
        panel.add(addButton);
        saveButton = new JButton("Save");
        styleActionButton(saveButton, new Color(41, 128, 185));
        saveButton.addActionListener((ActionEvent e) -> saveItem());
        saveButton.setEnabled(false);
        panel.add(saveButton);
        deleteButton = new JButton("Delete");
        styleActionButton(deleteButton, new Color(231, 76, 60));
        deleteButton.addActionListener((ActionEvent e) -> deleteItem());
        deleteButton.setEnabled(false);
        panel.add(deleteButton);
        cancelButton = new JButton("Cancel");
        styleActionButton(cancelButton, new Color(149, 165, 166));
        cancelButton.addActionListener((ActionEvent e) -> hideDetailsPanel());
        cancelButton.setEnabled(false);
        panel.add(cancelButton);
        return panel;
    }


    private void styleActionButton(JButton button, Color bgColor) {
        button.setFont(new Font("Verdana", Font.BOLD, 14));
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(bgColor.darker(), 1), BorderFactory.createEmptyBorder(5, 15, 5, 15)));
    }


    private void stylePaginationButton(JButton button) {
        button.setFont(new Font("Verdana", Font.PLAIN, 12));
        button.setBackground(new Color(70, 70, 70));
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.GRAY, 1), BorderFactory.createEmptyBorder(3, 8, 3, 8)));
    }


    private void styleAddCategoryButton(JButton button) {
        button.setFont(new Font("Verdana", Font.BOLD, 14));
        button.setBackground(new Color(155, 89, 182));
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(128, 57, 153), 1), BorderFactory.createEmptyBorder(5, 15, 5, 15)));
    }


    private void loadCategories() {
        categoryFilter.removeAllItems();
        categoryField.removeAllItems();
        categoryFilter.addItem("All Categories");
        String sql = "SELECT CategoryName FROM Categories ORDER BY CategoryName";
        if (conn == null) {
            System.err.println("Cannot load categories: DB connection null.");
            return;
        }
        try (PreparedStatement pstmt = conn.prepareStatement(sql); ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                String categoryName = rs.getString("CategoryName");
                categoryFilter.addItem(categoryName);
                categoryField.addItem(categoryName);
            }
        } catch (SQLException e) {
            System.err.println("Error loading categories: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Failed to load categories: " + e.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
        }
    }


    private boolean validateInput() {
        String name = nameField.getText().trim();
        String quantityStr = quantityField.getText().trim();
        String reorderLevelStr = reorderLevelField.getText().trim();
        Object unitObj = unitField.getSelectedItem();

        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Item Name cannot be empty.", "Input Error", JOptionPane.WARNING_MESSAGE);
            nameField.requestFocus();
            return false;
        }
        if (categoryField.getSelectedIndex() < 0) {
            JOptionPane.showMessageDialog(this, "Please select a category.", "Input Error", JOptionPane.WARNING_MESSAGE);
            categoryField.requestFocus();
            return false;
        }
        if (quantityStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Quantity cannot be empty.", "Input Error", JOptionPane.WARNING_MESSAGE);
            quantityField.requestFocus();
            return false;
        }
        if (unitObj == null || unitObj.toString().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Unit cannot be empty.", "Input Error", JOptionPane.WARNING_MESSAGE);
            unitField.requestFocus();
            return false;
        }
        if (reorderLevelStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Reorder Level cannot be empty.", "Input Error", JOptionPane.WARNING_MESSAGE);
            reorderLevelField.requestFocus();
            return false;
        }

        try {
            int quantity = Integer.parseInt(quantityStr);
            if (quantity < 0) {
                JOptionPane.showMessageDialog(this, "Quantity cannot be negative.", "Input Error", JOptionPane.WARNING_MESSAGE);
                quantityField.requestFocus();
                return false;
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid Quantity format.", "Input Error", JOptionPane.WARNING_MESSAGE);
            quantityField.requestFocus();
            return false;
        }

        try {
            int reorderLevel = Integer.parseInt(reorderLevelStr);
            if (reorderLevel < 0) {
                JOptionPane.showMessageDialog(this, "Reorder Level cannot be negative.", "Input Error", JOptionPane.WARNING_MESSAGE);
                reorderLevelField.requestFocus();
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid Reorder Level format.", "Input Error", JOptionPane.WARNING_MESSAGE);
            reorderLevelField.requestFocus();
            return false;
        }

        return true;
    }


    private void logActivity(String activityType, String details) {
        String sql = "INSERT INTO RecentActivities (ActivityType, UserID, Details, ActivityDate) VALUES (?, ?, ?, NOW())";
        if (conn == null || currentUserId <= 0) {
            System.err.println("Cannot log activity: DB null or invalid UserID.");
            return;
        }
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, activityType);
            pstmt.setInt(2, currentUserId);
            pstmt.setString(3, details);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error logging activity: " + e.getMessage());
        }
    }


    private void fetchTotalItemCount() {
        String searchText = searchField.getText().trim().toLowerCase();
        String selectedCategory = (String) categoryFilter.getSelectedItem();
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) AS total FROM Items i");
        boolean hasWhere = false;
        boolean categorySelected = selectedCategory != null && !selectedCategory.equals("All Categories");
        if (categorySelected) {
            sql.append(" JOIN Categories c ON i.CategoryID = c.CategoryID");
        }
        if (!searchText.isEmpty()) {
            sql.append(" WHERE LOWER(i.ItemName) LIKE ?");
            hasWhere = true;
        }
        if (categorySelected) {
            sql.append(hasWhere ? " AND" : " WHERE");
            sql.append(" c.CategoryName = ?");
        }
        final String finalSql = sql.toString();

        new Thread(() -> {
            int count = 0;
            try (Connection tempConn = DBConnection.getConnection(); PreparedStatement pstmt = tempConn.prepareStatement(finalSql)) {
                int paramIndex = 1;
                if (!searchText.isEmpty()) {
                    pstmt.setString(paramIndex++, "%" + searchText + "%");
                }
                if (categorySelected) {
                    pstmt.setString(paramIndex++, selectedCategory);
                }
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        count = rs.getInt("total");
                    }
                }
            } catch (SQLException e) {
                System.err.println("Error fetching total item count: " + e.getMessage());
            }
            final int finalCount = count;
            SwingUtilities.invokeLater(() -> {
                totalItems = finalCount;
                int totalPages = (int) Math.ceil((double) totalItems / itemsPerPage);
                totalPages = Math.max(totalPages, 1);
                if (currentPage > totalPages) {
                    currentPage = totalPages;
                }
                updatePaginationControls();
                loadInventoryData();
            });
        }).start();
    }


    private void loadInventoryData() {
        if (conn == null) {
            return;
        }
        String searchText = searchField.getText().trim().toLowerCase();
        String selectedCategory = (String) categoryFilter.getSelectedItem();
        int offset = (currentPage - 1) * itemsPerPage;
        StringBuilder sql = new StringBuilder("SELECT i.ItemID, i.ItemName, c.CategoryName, i.Quantity, i.Unit, i.Status, i.ItemImage FROM Items i JOIN Categories c ON i.CategoryID = c.CategoryID");
        StringBuilder whereClause = new StringBuilder();
        boolean hasWhere = false;
        if (!searchText.isEmpty()) {
            whereClause.append(" WHERE LOWER(i.ItemName) LIKE ?");
            hasWhere = true;
        }
        boolean categorySelected = selectedCategory != null && !selectedCategory.equals("All Categories");
        if (categorySelected) {
            whereClause.append(hasWhere ? " AND" : " WHERE");
            whereClause.append(" c.CategoryName = ?");
        }
        sql.append(whereClause);
        // Changed ordering to show newest items first
        sql.append(" ORDER BY i.CreatedAt DESC LIMIT ? OFFSET ?");
        final String finalSql = sql.toString();
        new InventoryLoader(finalSql, searchText, selectedCategory, categorySelected, offset, itemsPerPage).execute();
    }


    private class InventoryLoader extends SwingWorker<Void, Object[]> {

        private final String sql;
        private final String searchText;
        private final String selectedCategory;
        private final boolean categorySelected;
        private final int offset;
        private final int limit;

        public InventoryLoader(String s, String st, String sc, boolean cs, int o, int l) {
            sql = s;
            searchText = st;
            selectedCategory = sc;
            categorySelected = cs;
            offset = o;
            limit = l;
            SwingUtilities.invokeLater(() -> tableModel.setRowCount(0));
        }

        @Override
        protected Void doInBackground() throws Exception {
            try (Connection tConn = DBConnection.getConnection(); PreparedStatement ps = tConn.prepareStatement(sql)) {
                int pIdx = 1;
                if (!searchText.isEmpty()) {
                    ps.setString(pIdx++, "%" + searchText + "%");
                }
                if (categorySelected) {
                    ps.setString(pIdx++, selectedCategory);
                }
                ps.setInt(pIdx++, limit);
                ps.setInt(pIdx++, offset);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        ImageIcon thumb = null;
                        byte[] imgData = rs.getBytes("ItemImage");
                        if (imgData != null && imgData.length > 0) {
                            try {
                                ImageIcon orig = new ImageIcon(imgData);
                                // Scale the image to fit the row height
                                Image scaled = orig.getImage().getScaledInstance(-1, 50, Image.SCALE_SMOOTH); // Scale height to 50, maintain aspect ratio
                                thumb = new ImageIcon(scaled);
                            } catch (Exception ix) {
                                System.err.println("Error scaling image: " + ix.getMessage());
                                // On error, set a null icon and potentially text
                                thumb = null;
                            }
                        }
                        String quantityUnit = rs.getInt("Quantity") + " " + rs.getString("Unit");

                        // Publish data in the correct order for the table model
                        publish(new Object[]{
                            rs.getInt("ItemID"),         // Column 0: Item ID
                            rs.getString("ItemName"),     // Column 1: Name
                            rs.getString("CategoryName"), // Column 2: Category
                            quantityUnit,                 // Column 3: Quantity (combined)
                            rs.getString("Unit"),         // Column 4: Unit (hidden) - Still publish for loadItemDetails
                            rs.getString("Status"),       // Column 5: Status
                            thumb                         // Column 6: Image
                        });
                    }
                }
            } catch (SQLException e) {
                System.err.println("Error in InventoryLoader: " + e.getMessage());
            }
            return null;
        }

        @Override
        protected void process(java.util.List<Object[]> chunks) {
            for (Object[] row : chunks) {
                // Add data to table model columns in the correct order after removing Unit column
                // Columns in tableModel: Item ID, Name, Category, Quantity, Status, Image
                // Data in row[] from publish: Item ID, Name, Category, Quantity (combined), Unit, Status, Image
                tableModel.addRow(new Object[]{
                    row[0], // Item ID
                    row[1], // Name
                    row[2], // Category
                    row[3], // Quantity (combined)
                    row[5], // Status (original index 5 in publish, now index 4 in tableModel)
                    row[6]  // Image (original index 6 in publish, now index 5 in tableModel)
                });
            }
        }

        @Override
        protected void done() {
            try {
                get();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    private void searchInventory() {
        currentPage = 1;
        fetchTotalItemCount();
    }


    private void updatePaginationControls() {
        int totalPages = (int) Math.ceil((double) totalItems / itemsPerPage);
        totalPages = Math.max(totalPages, 1);
        jLabelPageInfo.setText("Page " + currentPage + " of " + totalPages);
        jButtonPreviousPage.setEnabled(currentPage > 1);
        jButtonNextPage.setEnabled(currentPage < totalPages);
        if (totalItems <= 0) {
            jLabelPageInfo.setText("No items found");
        }
    }


    private void gotoPreviousPage() {
        if (currentPage > 1) {
            currentPage--;
            loadInventoryData();
            updatePaginationControls();
        }
    }


    private void gotoNextPage() {
        int totalPages = (int) Math.ceil((double) totalItems / itemsPerPage);
        totalPages = Math.max(totalPages, 1);
        if (currentPage < totalPages) {
            currentPage++;
            loadInventoryData();
            updatePaginationControls();
        }
    }


    private void loadItemDetails(int itemId) {
        String sql = "SELECT i.ItemName, i.CategoryID, c.CategoryName, i.Quantity, i.ReorderLevel, i.Unit, i.ItemImage, i.ItemImageType FROM Items i JOIN Categories c ON i.CategoryID = c.CategoryID WHERE i.ItemID = ?";
        if (conn == null) {
            return;
        }
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, itemId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String itemName = rs.getString("ItemName");
                    String categoryName = rs.getString("CategoryName");
                    int quantity = rs.getInt("Quantity");
                    int reorderLevel = rs.getInt("ReorderLevel");
                    String unit = rs.getString("Unit");
                    byte[] imageData = rs.getBytes("ItemImage");
                    selectedImageType = rs.getString("ItemImageType");
                    SwingUtilities.invokeLater(() -> {
                        itemIdField.setText(String.valueOf(itemId));
                        nameField.setText(itemName);
                        categoryField.setSelectedItem(categoryName);
                        quantityField.setText(String.valueOf(quantity));
                        reorderLevelField.setText(String.valueOf(reorderLevel));
                        unitField.setSelectedItem(unit);
                        selectedImageFile = null;
                        if (imageData != null && imageData.length > 0) {
                            try {
                                ImageIcon iIcon = new ImageIcon(imageData);
                                int tW = imageLabel.getPreferredSize().width - 10;
                                int tH = imageLabel.getPreferredSize().height - 10;
                                Image scImg = iIcon.getImage().getScaledInstance(tW, tH, Image.SCALE_SMOOTH);
                                imageLabel.setIcon(new ImageIcon(scImg));
                                imageLabel.setText("");
                            } catch (Exception imgEx) {
                                imageLabel.setIcon(null);
                                imageLabel.setText("Preview Error");
                                imageLabel.setForeground(Color.RED);
                                System.err.println("Error displaying image preview: " + imgEx.getMessage());
                            }
                        } else {
                            imageLabel.setIcon(null);
                            imageLabel.setText("No Image Available");
                            imageLabel.setForeground(Color.LIGHT_GRAY);
                        }
                        isNewItem = false;
                        detailPanel.setVisible(true);
                        enableActionButtons(true);
                    });
                } else {
                    hideDetailsPanel();
                }
            }
        } catch (SQLException e) {
            hideDetailsPanel();
            System.err.println("Error loading item details: " + e.getMessage());
        }
    }


    private void prepareNewItem() {
        isNewItem = true;
        clearDetailsPanel();
        itemIdField.setText("AUTO");
        detailPanel.setVisible(true);
        enableActionButtons(true);
        deleteButton.setEnabled(false);
        nameField.requestFocus();
    }


    private void saveItem() {
        if (!validateInput()) {
            return;
        }
        String name = nameField.getText().trim();
        String categoryName = (String) categoryField.getSelectedItem();
        int quantity = Integer.parseInt(quantityField.getText().trim());
        int reorderLevel = Integer.parseInt(reorderLevelField.getText().trim());
        String unit = unitField.getSelectedItem().toString().trim();
        int categoryId = getCategoryId(categoryName);
        if (categoryId == -1) {
            JOptionPane.showMessageDialog(this, "Invalid category selected.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (conn == null) {
            JOptionPane.showMessageDialog(this, "Database connection is not available.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        FileInputStream fis = null;
        String sql;
        String successMessage;
        String logActivityType;
        String logDetails;
        String newStatus = "In Stock";
        if (quantity <= 0) {
            newStatus = "Out of Stock";
        } else if (quantity <= reorderLevel) {
            newStatus = "Low Stock";
        }

        try {
            if (isNewItem) {
                sql = "INSERT INTO Items (ItemName, CategoryID, Quantity, ReorderLevel, Unit, ItemImage, ItemImageType, AddedBy, Status, CreatedAt) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())";
                successMessage = "Item added successfully.";
                logActivityType = "Item Added";
                logDetails = "Item '" + name + "' added";
                try (PreparedStatement pstmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
                    pstmt.setString(1, name);
                    pstmt.setInt(2, categoryId);
                    pstmt.setInt(3, quantity);
                    pstmt.setInt(4, reorderLevel);
                    pstmt.setString(5, unit);
                    if (selectedImageFile != null && selectedImageFile.exists()) {
                        fis = new FileInputStream(selectedImageFile);
                        pstmt.setBinaryStream(6, fis, (int) selectedImageFile.length());
                        pstmt.setString(7, selectedImageType);
                    } else {
                        pstmt.setNull(6, java.sql.Types.BLOB);
                        pstmt.setNull(7, java.sql.Types.VARCHAR);
                    }
                    pstmt.setInt(8, currentUserId);
                    pstmt.setString(9, newStatus);
                    int rowsAffected = pstmt.executeUpdate();
                    if (rowsAffected > 0) {
                        try (ResultSet gk = pstmt.getGeneratedKeys()) {
                            if (gk.next()) {
                                logDetails += " (ID: " + gk.getInt(1) + ")";
                            }
                        }
                    } else {
                        throw new SQLException("Creating item failed, no rows affected.");
                    }
                }
            } else {
                int itemId = Integer.parseInt(itemIdField.getText());
                successMessage = "Item updated successfully.";
                logActivityType = "Item Updated";
                logDetails = "Item '" + name + "' (ID: " + itemId + ") updated";
                if (selectedImageFile != null && selectedImageFile.exists()) {
                    sql = "UPDATE Items SET ItemName=?, CategoryID=?, Quantity=?, ReorderLevel=?, Unit=?, ItemImage=?, ItemImageType=?, Status=?, UpdatedAt=NOW() WHERE ItemID=?";
                    try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                        pstmt.setString(1, name);
                        pstmt.setInt(2, categoryId);
                        pstmt.setInt(3, quantity);
                        pstmt.setInt(4, reorderLevel);
                        pstmt.setString(5, unit);
                        fis = new FileInputStream(selectedImageFile);
                        pstmt.setBinaryStream(6, fis, (int) selectedImageFile.length());
                        pstmt.setString(7, selectedImageType);
                        pstmt.setString(8, newStatus);
                        pstmt.setInt(9, itemId);
                        pstmt.executeUpdate();
                    }
                } else {
                    sql = "UPDATE Items SET ItemName=?, CategoryID=?, Quantity=?, ReorderLevel=?, Unit=?, Status=?, UpdatedAt=NOW() WHERE ItemID=?";
                    try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                        pstmt.setString(1, name);
                        pstmt.setInt(2, categoryId);
                        pstmt.setInt(3, quantity);
                        pstmt.setInt(4, reorderLevel);
                        pstmt.setString(5, unit);
                        pstmt.setString(6, newStatus);
                        pstmt.setInt(7, itemId);
                        pstmt.executeUpdate();
                    }
                }
            }
            JOptionPane.showMessageDialog(this, successMessage, "Success", JOptionPane.INFORMATION_MESSAGE);
            logActivity(logActivityType, logDetails);
            fetchTotalItemCount();
            hideDetailsPanel();
        } catch (SQLException | IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Failed to save item: " + e.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException ex) {
                }
            }
        }
    }


    private int getCategoryId(String categoryName) {
        String sql = "SELECT CategoryID FROM Categories WHERE CategoryName = ?";
        if (conn == null || categoryName == null) {
            return -1;
        }
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, categoryName);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("CategoryID");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting CategoryID: " + e.getMessage());
        }
        return -1;
    }


    private void deleteItem() {
        if (isNewItem || itemIdField.getText().isEmpty() || itemIdField.getText().equals("AUTO")) {
            JOptionPane.showMessageDialog(this, "No item selected.", "Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int itemId;
        try {
            itemId = Integer.parseInt(itemIdField.getText());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid Item ID.", "Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String itemName = nameField.getText();
        int result = JOptionPane.showConfirmDialog(this, "Delete item '" + itemName + "' (ID: " + itemId + ")?\nCannot be undone.", "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (result == JOptionPane.YES_OPTION) {
            if (hasDependencies(itemId)) {
                JOptionPane.showMessageDialog(this, "Cannot delete item '" + itemName + "'. Referenced elsewhere.", "Deletion Blocked", JOptionPane.ERROR_MESSAGE);
                return;
            }
            String sql = "DELETE FROM Items WHERE ItemID = ?";
            if (conn == null) {
                JOptionPane.showMessageDialog(this, "Database connection is not available.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, itemId);
                int rowsAffected = pstmt.executeUpdate();
                if (rowsAffected > 0) {
                    JOptionPane.showMessageDialog(this, "Item deleted.", "Success", JOptionPane.INFORMATION_MESSAGE);
                    logActivity("Item Deleted", "Item '" + itemName + "' (ID: " + itemId + ") deleted");
                    fetchTotalItemCount();
                    hideDetailsPanel();
                } else {
                    JOptionPane.showMessageDialog(this, "Item not found for deletion.", "Error", JOptionPane.WARNING_MESSAGE);
                }
            } catch (SQLException e) {
                System.err.println("Error deleting item: " + e.getMessage());
                JOptionPane.showMessageDialog(this, "DB error deleting item.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }


    private boolean hasDependencies(int itemId) {
        String[] checkSqls = {"SELECT 1 FROM Transactions WHERE ItemID=? LIMIT 1", "SELECT 1 FROM PurchaseOrderItems WHERE ItemID=? LIMIT 1"};
        if (conn == null) {
            return true;
        }
        for (String sql : checkSqls) {
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, itemId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return true;
                    }
                }
            } catch (SQLException e) {
                System.err.println("Dependency check error: " + e.getMessage());
            }
        }
        return false;
    }


    private void browseImage() {
        JFileChooser fc = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Images", "jpg", "jpeg", "png", "gif");
        fc.setFileFilter(filter);
        fc.setAcceptAllFileFilterUsed(false);
        int ret = fc.showOpenDialog(this);
        if (ret == JFileChooser.APPROVE_OPTION) {
            selectedImageFile = fc.getSelectedFile();
            String fName = selectedImageFile.getName();
            int dot = fName.lastIndexOf('.');
            selectedImageType = (dot > 0 && dot < fName.length() - 1) ? fName.substring(dot + 1).toLowerCase() : null;
            displaySelectedImage(selectedImageFile.getAbsolutePath());
        }
    }


    private void displaySelectedImage(String imagePath) {
        try {
            ImageIcon iIcn = new ImageIcon(imagePath);
            Image img = iIcn.getImage();
            int tW = imageLabel.getPreferredSize().width - 10;
            int tH = imageLabel.getPreferredSize().height - 10;
            int oW = img.getWidth(null);
            int oH = img.getHeight(null);
            if (oW <= 0 || oH <= 0) {
                throw new Exception("Bad dims");
            }
            int sW = tW;
            int sH = (sW * oH) / oW;
            if (sH > tH) {
                sH = tH;
                sW = (sH * oW) / oH;
            }
            sW = Math.max(1, sW);
            sH = Math.max(1, sH);
            Image scImg = img.getScaledInstance(sW, sH, Image.SCALE_SMOOTH);
            imageLabel.setIcon(new ImageIcon(scImg));
            imageLabel.setText(null);
        } catch (Exception e) {
            imageLabel.setIcon(null);
            imageLabel.setText("Preview Error");
            imageLabel.setForeground(Color.RED);
            selectedImageFile = null;
            selectedImageType = null;
            System.err.println("Error displaying image: " + e.getMessage());
        }
    }


    private void clearDetailsPanel() {
        itemIdField.setText("");
        nameField.setText("");
        if (categoryField.getItemCount() > 0) {
            categoryField.setSelectedIndex(0);
        }
        quantityField.setText("");
        reorderLevelField.setText("");
        if (unitField.getItemCount() > 0) {
            unitField.setSelectedIndex(0);
        } else {
            unitField.setSelectedItem("");
        }
        imageLabel.setIcon(null);
        imageLabel.setText("No Image Selected");
        imageLabel.setForeground(Color.LIGHT_GRAY);
        selectedImageFile = null;
        selectedImageType = null;
        isNewItem = true;

    }


    private void enableActionButtons(boolean enable) {
        if (saveButton != null) {
            saveButton.setEnabled(enable);
        }
        if (deleteButton != null) {
            deleteButton.setEnabled(enable && !isNewItem);
        }
        if (cancelButton != null) {
            cancelButton.setEnabled(enable);
        }
    }


    private void hideDetailsPanel() {
        detailPanel.setVisible(false);
        clearDetailsPanel();
        enableActionButtons(false);
        inventoryTable.clearSelection();
    }

    private void showAddCategoryDialog() {
        String ncn = JOptionPane.showInputDialog(this, "New category:", "Add Category", JOptionPane.PLAIN_MESSAGE);
        if (ncn != null && !ncn.trim().isEmpty()) {
            addNewCategory(ncn.trim());
        } else if (ncn != null) {
            JOptionPane.showMessageDialog(this, "Category name empty.", "Input Error", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void addNewCategory(String catName) {
        if (conn == null) {
            JOptionPane.showMessageDialog(this, "Database connection is not available.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (categoryExists(catName)) {
            JOptionPane.showMessageDialog(this, "Category '" + catName + "' already exists.", "Duplicate", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String sql = "INSERT INTO Categories (CategoryName) VALUES (?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, catName);
            int ra = ps.executeUpdate();
            if (ra > 0) {
                JOptionPane.showMessageDialog(this, "Category added successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
                logActivity("Category Added", "Category '" + catName + "' added");
                loadCategories();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to add category.", "Error", JOptionPane.ERROR_MESSAGE);
            }

        } catch (SQLException e) {
            System.err.println("Error adding category: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Database error adding category: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private boolean categoryExists(String catName) {
        String sql = "SELECT 1 FROM Categories WHERE CategoryName=? LIMIT 1";
        if (conn == null) {
            return false;
        }
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, catName);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("Error checking category existence: " + e.getMessage());
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();

        setPreferredSize(new java.awt.Dimension(834, 644));

        jPanel1.setBackground(new java.awt.Color(0, 0, 0));
        jPanel1.setForeground(new java.awt.Color(255, 255, 255));
        jPanel1.setPreferredSize(new java.awt.Dimension(834, 644));

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 834, Short.MAX_VALUE)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 644, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    // End of variables declaration//GEN-END:variables
}
