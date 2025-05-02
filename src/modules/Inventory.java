package modules;

import Package1.DBConnection;
import com.mysql.cj.jdbc.Blob; // Keep this specific import if you are sure you need the CJ driver's Blob implementation
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component; // Import Component
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
import javax.swing.table.DefaultTableCellRenderer; // Import DefaultTableCellRenderer

public class Inventory extends javax.swing.JPanel {

    private JTable inventoryTable;
    private JTextField searchField;
    private JComboBox<String> categoryFilter;
    private DefaultTableModel tableModel;

    // Item detail components
    private JPanel detailPanel;
    private JTextField itemIdField;
    private JTextField nameField;
    private JComboBox<String> categoryField;
    private JTextField quantityField;
    private JTextField priceField;
    private JLabel imageLabel;
    private File selectedImageFile;
    private String selectedImageType;
    private boolean isNewItem = true; // Flag to track if adding new or updating existing

    // Action buttons (declared as fields for easier access)
    private JButton saveButton;
    private JButton deleteButton;
    private JButton cancelButton; // Added cancel button reference

    // Database connection
    private Connection conn = null; // Initialize conn to null
    private int currentUserId; // Assume this is set correctly elsewhere

    // >>> Pagination Variables <<<
    private int currentPage = 1;
    private final int itemsPerPage = 10; // You can adjust this value for inventory
    private int totalItems = 0;
    private JButton jButtonPreviousPage;
    private JButton jButtonNextPage;
    private JLabel jLabelPageInfo;
    // >>> End Pagination Variables <<<

    // >>> Add Category Components <<<
    private JButton jButtonAddCategory;
    // >>> End Add Category Components <<<


    public Inventory() {
        // Establish connection first
        if (!connectToDatabase()) {
            // Handle connection failure (e.g., show error message and disable functionality)
            JOptionPane.showMessageDialog(this, "Database connection failed. Inventory features disabled.", "Connection Error", JOptionPane.ERROR_MESSAGE);
            // Optionally disable components or return
        }
        initComponents(); // Initialize NetBeans generated components (if any)
        setupInventoryPanel(); // Setup custom UI components
        loadCategories(); // Load categories into combo boxes
        // Initial data load and pagination update will happen after setCurrentUserId is called
    }

    // Setter for currentUserId (ensure this is called after login)
    public void setCurrentUserId(int userId) {
        this.currentUserId = userId;
        System.out.println("Inventory: setCurrentUserId called with: " + userId + ", currentUserId is now: " + this.currentUserId);
        // Load initial data and total count after user is set
        currentPage = 1; // Start from the first page
        fetchTotalItemCount(); // Fetch total count first
        // loadInventoryData() is called after total count is fetched and pagination updated
    }

    // Establishes database connection
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

    // Sets up the main layout and components of the Inventory panel
    // Modified to include pagination and add category button
    private void setupInventoryPanel() {
        this.setLayout(new BorderLayout(10, 10));
        this.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        this.setBackground(new Color(30, 30, 30)); // Dark background

        // Top: Search and Filter Panel
        JPanel searchFilterPanel = createSearchFilterPanel(); // Method to create search/filter part
        this.add(searchFilterPanel, BorderLayout.NORTH);


        // Center: Table and Details Panel Wrapper
        JPanel centerPanel = new JPanel(new BorderLayout(10, 0));
        centerPanel.setOpaque(false); // Make transparent to show main background

        // Center-Left: Inventory Table Panel (Modified to include pagination controls)
        JPanel tablePanel = createInventoryTablePanel();
        centerPanel.add(tablePanel, BorderLayout.CENTER);

        // Center-Right: Item Details Panel (initially hidden)
        detailPanel = createItemDetailsPanel();
        detailPanel.setVisible(false);
        centerPanel.add(detailPanel, BorderLayout.EAST);

        this.add(centerPanel, BorderLayout.CENTER);

        // Bottom: Action Buttons and Add Category Panel
        JPanel bottomPanel = new JPanel(new BorderLayout()); // Use BorderLayout for bottom
        bottomPanel.setOpaque(false);

        // Add Category Button Panel (Bottom Left)
        JPanel addCategoryPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        addCategoryPanel.setOpaque(false);

        jButtonAddCategory = new JButton("Add Category");
        styleAddCategoryButton(jButtonAddCategory); // Apply separate styling
        jButtonAddCategory.addActionListener(e -> showAddCategoryDialog()); // Action to show dialog
        addCategoryPanel.add(jButtonAddCategory);

        bottomPanel.add(addCategoryPanel, BorderLayout.WEST); // Add add category button to the west of bottom panel

        // Action Buttons Panel (Bottom Right)
        JPanel actionPanel = createActionPanel(); // This panel already uses FlowLayout.RIGHT
        bottomPanel.add(actionPanel, BorderLayout.EAST); // Add action buttons to the east of bottom panel


        this.add(bottomPanel, BorderLayout.SOUTH); // Add the combined bottom panel to the main panel's SOUTH


        // Add Mouse Listener to the table for row selection
        inventoryTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent evt) {
                // Use single click to load details
                int selectedRow = inventoryTable.getSelectedRow(); // Get view row index
                if (selectedRow >= 0) {
                    int modelRow = inventoryTable.convertRowIndexToModel(selectedRow); // Convert to model index
                    Object itemIdObj = tableModel.getValueAt(modelRow, 0); // Get ID from model
                    if (itemIdObj != null) {
                        try {
                            int itemId = Integer.parseInt(itemIdObj.toString());
                            loadItemDetails(itemId); // Load details from DB based on ID
                        } catch (NumberFormatException e) {
                            System.err.println("Error parsing Item ID from table: " + itemIdObj);
                        }
                    }
                }
            }
        });
    }

    // Creates the panel with search field and category filter
    private JPanel createSearchFilterPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        panel.setOpaque(false);

        // Search Label and Field
        JLabel searchLabel = new JLabel("Search:");
        searchLabel.setFont(new Font("Verdana", Font.BOLD, 14));
        searchLabel.setForeground(Color.WHITE);
        panel.add(searchLabel);

        searchField = new JTextField(20);
        searchField.setFont(new Font("Verdana", Font.PLAIN, 14));
        panel.add(searchField);

        // Category Filter Label and ComboBox
        JLabel categoryLabel = new JLabel("Category:");
        categoryLabel.setFont(new Font("Verdana", Font.BOLD, 14));
        categoryLabel.setForeground(Color.WHITE);
        panel.add(categoryLabel);

        categoryFilter = new JComboBox<>();
        categoryFilter.addItem("All Categories"); // Default item
        categoryFilter.setFont(new Font("Verdana", Font.PLAIN, 14));
        panel.add(categoryFilter);

        // Search Button
        JButton searchBtn = new JButton("Search");
        searchBtn.setFont(new Font("Verdana", Font.BOLD, 14));
        searchBtn.setBackground(new Color(41, 128, 185));
        searchBtn.setForeground(Color.WHITE);
        searchBtn.addActionListener((ActionEvent e) -> {
             currentPage = 1; // Reset to first page on search
             fetchTotalItemCount(); // Fetch total count with filter and then display data
        });
        panel.add(searchBtn);

        return panel;
    }


    // Creates the panel containing the inventory JTable and pagination controls
    private JPanel createInventoryTablePanel() {
        JPanel panel = new JPanel(new BorderLayout()); // Use BorderLayout for table and pagination
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.GRAY),
                "Inventory Items", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
                javax.swing.border.TitledBorder.DEFAULT_POSITION,
                new Font("Verdana", Font.BOLD, 14), Color.WHITE));

        // Define table columns
        String[] columns = {"Item ID", "Name", "Category", "Quantity", "Unit Price", "Total Value", "Status", "Image"};
        tableModel = new DefaultTableModel(columns, 0) {
            // Make table cells non-editable
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            // Define column data types for correct sorting and rendering
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                switch (columnIndex) {
                    case 0: return Integer.class; // Item ID
                    case 3: return Integer.class; // Quantity
                    case 4: return Double.class;  // Unit Price
                    case 5: return Double.class;  // Total Value
                    case 7: return ImageIcon.class; // Image
                    default: return String.class; // Name, Category, Status
                }
            }
        };

        inventoryTable = new JTable(tableModel);
        // --- Table Styling ---
        inventoryTable.setForeground(Color.WHITE);
        inventoryTable.setBackground(new Color(30, 30, 30)); // Dark cell background
        inventoryTable.setGridColor(new Color(50, 50, 50)); // Dark grid lines
        inventoryTable.setSelectionBackground(new Color(41, 128, 185)); // Selection color
        inventoryTable.setSelectionForeground(Color.WHITE);
        inventoryTable.setFont(new Font("Verdana", Font.PLAIN, 12));
        inventoryTable.setRowHeight(60); // Increased row height for image thumbnails
        inventoryTable.setAutoCreateRowSorter(true); // Enable sorting by clicking headers

        // --- Table Header Styling ---
        inventoryTable.getTableHeader().setFont(new Font("Verdana", Font.BOLD, 12));
        inventoryTable.getTableHeader().setBackground(new Color(40, 40, 40)); // Dark header background
        inventoryTable.getTableHeader().setForeground(Color.WHITE); // Header text color
        inventoryTable.getTableHeader().setReorderingAllowed(false); // Prevent column reordering

        // --- Custom Cell Renderer for Status Column ---
        inventoryTable.getColumnModel().getColumn(6).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                String status = value != null ? value.toString() : "";
                if ("Low Stock".equals(status)) {
                    c.setForeground(new Color(231, 76, 60)); // Red for low stock
                } else if ("Out of Stock".equals(status)) {
                    c.setForeground(new Color(255, 165, 0)); // Orange for out of stock
                } else if ("In Stock".equals(status)) {
                    c.setForeground(new Color(46, 204, 113)); // Green for in stock
                } else {
                    c.setForeground(isSelected ? table.getSelectionForeground() : Color.WHITE); // Default or selection color
                }
                c.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground()); // Handle selection background
                setHorizontalAlignment(JLabel.CENTER); // Center status text
                return c;
            }
        });

        // --- Custom Cell Renderer for Image Column ---
        inventoryTable.getColumnModel().getColumn(7).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                label.setText(""); // Remove text from image cell
                label.setIcon((ImageIcon) value); // Set the icon
                label.setHorizontalAlignment(JLabel.CENTER); // Center the image
                label.setVerticalAlignment(JLabel.CENTER);
                label.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground()); // Handle selection background
                return label;
            }
        });

        // Set preferred column widths (adjust as needed)
        inventoryTable.getColumnModel().getColumn(0).setPreferredWidth(60);  // Item ID
        inventoryTable.getColumnModel().getColumn(1).setPreferredWidth(150); // Name
        inventoryTable.getColumnModel().getColumn(2).setPreferredWidth(100); // Category
        inventoryTable.getColumnModel().getColumn(3).setPreferredWidth(70);  // Quantity
        inventoryTable.getColumnModel().getColumn(4).setPreferredWidth(80);  // Unit Price
        inventoryTable.getColumnModel().getColumn(5).setPreferredWidth(90);  // Total Value
        inventoryTable.getColumnModel().getColumn(6).setPreferredWidth(80);  // Status
        inventoryTable.getColumnModel().getColumn(7).setPreferredWidth(70);  // Image

        // Add table to scroll pane
        JScrollPane scrollPane = new JScrollPane(inventoryTable);
        scrollPane.getViewport().setBackground(new Color(30, 30, 30)); // Match background
        panel.add(scrollPane, BorderLayout.CENTER);

        // >>> Pagination Controls Panel <<<
        JPanel paginationPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        paginationPanel.setOpaque(false);

        jButtonPreviousPage = new JButton("Previous");
        stylePaginationButton(jButtonPreviousPage); // Apply styling
        jButtonPreviousPage.addActionListener(e -> gotoPreviousPage()); // Add action listener
        paginationPanel.add(jButtonPreviousPage);

        jLabelPageInfo = new JLabel("Page 1 of 1"); // Initial text
        jLabelPageInfo.setFont(new Font("Verdana", Font.PLAIN, 12));
        jLabelPageInfo.setForeground(Color.WHITE);
        paginationPanel.add(jLabelPageInfo);

        jButtonNextPage = new JButton("Next");
        stylePaginationButton(jButtonNextPage); // Apply styling
        jButtonNextPage.addActionListener(e -> gotoNextPage()); // Add action listener
        paginationPanel.add(jButtonNextPage);

        panel.add(paginationPanel, BorderLayout.SOUTH); // Add pagination controls to the bottom
        // >>> End Pagination Controls Panel <<<

        return panel;
    }

    // Creates the right-side panel for displaying and editing item details
    private JPanel createItemDetailsPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 10)); // Vertical gap of 10
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.GRAY),
                "Item Details", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
                javax.swing.border.TitledBorder.DEFAULT_POSITION,
                new Font("Verdana", Font.BOLD, 14), Color.WHITE));
        panel.setPreferredSize(new Dimension(350, 0)); // Preferred width, height adjusts

        // --- Form Panel using GridBagLayout ---
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5); // Padding around components
        gbc.anchor = GridBagConstraints.WEST; // Align components to the left

        // Helper method to add label and field pairs
        // Set weighty to 0 for text fields and combo box to prevent vertical stretching
        addField(formPanel, gbc, 0, "Item ID:", itemIdField = createNonEditableTextField(), 0.0);
        addField(formPanel, gbc, 1, "Name:", nameField = createEditableTextField(), 0.0);
        addField(formPanel, gbc, 2, "Category:", categoryField = new JComboBox<>(), 0.0);
        addField(formPanel, gbc, 3, "Quantity:", quantityField = createEditableTextField(), 0.0);
        addField(formPanel, gbc, 4, "Unit Price:", priceField = createEditableTextField(), 0.0);

        // --- Image Panel ---
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 2; // Span across both columns
        gbc.fill = GridBagConstraints.HORIZONTAL; // Fill horizontally, but don't expand vertically
        gbc.weightx = 1.0; // Allow horizontal expansion
        gbc.weighty = 0.0; // Prevent vertical expansion

        JPanel imagePanel = new JPanel(new BorderLayout(0, 5)); // Small gap
        imagePanel.setOpaque(false);
        imagePanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.GRAY),
                "Item Image", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
                javax.swing.border.TitledBorder.DEFAULT_POSITION,
                new Font("Verdana", Font.BOLD, 12), Color.WHITE));

        imageLabel = new JLabel("No Image Selected", SwingConstants.CENTER);
        imageLabel.setForeground(Color.LIGHT_GRAY);
        // Set a fixed preferred size for the image label itself
        imageLabel.setPreferredSize(new Dimension(200, 150));
        imageLabel.setMinimumSize(new Dimension(150, 100)); // Optional: set minimum
        imageLabel.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY)); // Add a border
        imagePanel.add(imageLabel, BorderLayout.CENTER);

        JButton browseButton = new JButton("Browse...");
        browseButton.setFont(new Font("Verdana", Font.PLAIN, 12));
        browseButton.addActionListener((ActionEvent e) -> browseImage());
        imagePanel.add(browseButton, BorderLayout.SOUTH);

        formPanel.add(imagePanel, gbc);

        // Add a vertical glue component at the bottom to push everything up
        gbc.gridx = 0;
        gbc.gridy = 6; // Next row
        gbc.weighty = 1.0; // Give this component all the extra vertical space
        gbc.fill = GridBagConstraints.VERTICAL;
        formPanel.add(new JLabel(), gbc); // Add an empty label or JPanel as glue

        panel.add(formPanel, BorderLayout.CENTER);
        return panel;
    }

    // Helper to create a standard editable text field
    private JTextField createEditableTextField() {
        JTextField textField = new JTextField(15); // Approx 15 columns wide
        textField.setFont(new Font("Verdana", Font.PLAIN, 12));
        return textField;
    }

    // Helper to create a non-editable text field (for Item ID)
    private JTextField createNonEditableTextField() {
        JTextField textField = new JTextField(15);
        textField.setFont(new Font("Verdana", Font.PLAIN, 12));
        textField.setEditable(false);
        textField.setBackground(Color.DARK_GRAY); // Indicate non-editable
        textField.setForeground(Color.LIGHT_GRAY);
        return textField;
    }

    // Helper method to add a label and component pair to the GridBagLayout
    // Added weighty parameter
    private void addField(JPanel panel, GridBagConstraints gbc, int y, String labelText, Component component, double weighty) {
        gbc.gridx = 0;
        gbc.gridy = y;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE; // Don't stretch label
        gbc.weightx = 0.0; // Don't give label extra horizontal space
        gbc.weighty = weighty; // Control vertical stretch for the row
        JLabel label = new JLabel(labelText);
        label.setForeground(Color.WHITE);
        label.setFont(new Font("Verdana", Font.PLAIN, 12));
        panel.add(label, gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL; // Stretch component horizontally
        gbc.weightx = 1.0; // Give component extra horizontal space
        // weighty is already set for the row
        if (component instanceof JComboBox) {
            ((JComboBox<?>) component).setFont(new Font("Verdana", Font.PLAIN, 12));
        }
        panel.add(component, gbc);
    }

    // Creates the panel with action buttons (Add, Save, Delete, Cancel)
    private JPanel createActionPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        panel.setOpaque(false);

        // Add New Button
        JButton addButton = new JButton("Add New");
        styleActionButton(addButton, new Color(46, 204, 113)); // Green
        addButton.addActionListener((ActionEvent e) -> prepareNewItem());
        panel.add(addButton);

        // Save Button (Store reference)
        saveButton = new JButton("Save");
        styleActionButton(saveButton, new Color(41, 128, 185)); // Blue
        saveButton.addActionListener((ActionEvent e) -> saveItem());
        saveButton.setEnabled(false); // Initially disabled
        panel.add(saveButton);

        // Delete Button (Store reference)
        deleteButton = new JButton("Delete");
        styleActionButton(deleteButton, new Color(231, 76, 60)); // Red
        deleteButton.addActionListener((ActionEvent e) -> deleteItem());
        deleteButton.setEnabled(false); // Initially disabled
        panel.add(deleteButton);

        // Cancel Button (Store reference)
        cancelButton = new JButton("Cancel");
        styleActionButton(cancelButton, new Color(149, 165, 166)); // Gray
        cancelButton.addActionListener((ActionEvent e) -> hideDetailsPanel());
        cancelButton.setEnabled(false); // Initially disabled
        panel.add(cancelButton);

        return panel;
    }

    // Helper method to style action buttons consistently
    private void styleActionButton(JButton button, Color bgColor) {
        button.setFont(new Font("Verdana", Font.BOLD, 14));
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false); // Optional: remove focus border
        // Add more styling like padding or borders if desired
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(bgColor.darker(), 1), // Outer darker border
            BorderFactory.createEmptyBorder(5, 15, 5, 15) // Inner padding
        ));
    }

     /**
     * Helper method to style pagination/search buttons for dark theme.
     */
    private void stylePaginationButton(JButton button) {
        button.setFont(new Font("Verdana", Font.PLAIN, 12));
        button.setBackground(new Color(70, 70, 70)); // Slightly lighter gray
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.GRAY, 1),
            BorderFactory.createEmptyBorder(3, 8, 3, 8) // Padding
        ));
    }

    /**
     * Helper method to style the Add Category button.
     */
    private void styleAddCategoryButton(JButton button) {
        button.setFont(new Font("Verdana", Font.BOLD, 14)); // Slightly larger font
        button.setBackground(new Color(155, 89, 182)); // Purple color
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
         button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(128, 57, 153), 1), // Darker purple border
            BorderFactory.createEmptyBorder(5, 15, 5, 15) // Inner padding
        ));
    }


    // Loads category names from the database into the filter and detail combo boxes
    private void loadCategories() {
        // Clear existing items first
        categoryFilter.removeAllItems();
        categoryField.removeAllItems();

        // Add the default "All" option to the filter
        categoryFilter.addItem("All Categories");

        String sql = "SELECT CategoryName FROM Categories ORDER BY CategoryName"; // Added ORDER BY
        if (conn == null) {
            System.err.println("Cannot load categories: Database connection is null.");
            return;
        }
        try (PreparedStatement pstmt = conn.prepareStatement(sql); ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                String categoryName = rs.getString("CategoryName");
                categoryFilter.addItem(categoryName); // Add to filter dropdown
                categoryField.addItem(categoryName); // Add to details dropdown
            }
        } catch (SQLException e) {
            System.err.println("Error loading categories: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Failed to load categories: " + e.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Validates user input in the details panel before saving
    private boolean validateInput() {
        String name = nameField.getText().trim();
        String quantityStr = quantityField.getText().trim();
        String priceStr = priceField.getText().trim();

        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Item Name cannot be empty.", "Input Error", JOptionPane.WARNING_MESSAGE);
            nameField.requestFocus(); // Focus the field with error
            return false;
        }
        if (categoryField.getSelectedIndex() < 0) { // Check if a category is selected
             JOptionPane.showMessageDialog(this, "Please select a category.", "Input Error", JOptionPane.WARNING_MESSAGE);
             categoryField.requestFocus();
             return false;
        }
        if (quantityStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Quantity cannot be empty.", "Input Error", JOptionPane.WARNING_MESSAGE);
            quantityField.requestFocus();
            return false;
        }
        if (priceStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Unit Price cannot be empty.", "Input Error", JOptionPane.WARNING_MESSAGE);
            priceField.requestFocus();
            return false;
        }

        // Validate numeric inputs
        try {
            int quantity = Integer.parseInt(quantityStr);
            if (quantity < 0) {
                 JOptionPane.showMessageDialog(this, "Quantity cannot be negative.", "Input Error", JOptionPane.WARNING_MESSAGE);
                 quantityField.requestFocus();
                 return false;
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid quantity format. Please enter a whole number.", "Input Error", JOptionPane.WARNING_MESSAGE);
            quantityField.requestFocus();
            return false;
        }

        try {
            double price = Double.parseDouble(priceStr);
             if (price < 0) {
                 JOptionPane.showMessageDialog(this, "Unit Price cannot be negative.", "Input Error", JOptionPane.WARNING_MESSAGE);
                 priceField.requestFocus();
                 return false;
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid price format. Please enter a number.", "Input Error", JOptionPane.WARNING_MESSAGE);
            priceField.requestFocus();
            return false;
        }

        return true; // All validations passed
    }

    // Logs user activity to the RecentActivities table
    private void logActivity(String activityType, String details) {
        String sql = "INSERT INTO RecentActivities (ActivityType, UserID, Details, ActivityDate) VALUES (?, ?, ?, NOW())"; // Added timestamp
        if (conn == null) {
            System.err.println("Cannot log activity: Database connection is null.");
            return;
        }
        if (currentUserId <= 0) {
             System.err.println("Cannot log activity: Invalid currentUserId (" + currentUserId + ")");
             // Optionally show a warning or prevent the action if user ID is crucial
             return;
        }
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, activityType);
            pstmt.setInt(2, currentUserId);
            pstmt.setString(3, details);
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                 System.out.println("Activity logged: " + activityType + " - " + details + " by UserID: " + currentUserId);
            } else {
                 System.err.println("Failed to log activity (no rows affected).");
            }
        } catch (SQLException e) {
            System.err.println("Error logging activity: " + e.getMessage());
            // Avoid showing JOptionPane here as it might interrupt workflow
        }
    }

    // >>> Pagination and Search Methods <<<

    /**
     * Fetches the total number of items, applying the current search and category filters.
     */
    private void fetchTotalItemCount() {
         String searchText = searchField.getText().trim().toLowerCase();
         String selectedCategory = (String) categoryFilter.getSelectedItem();

         // Construct the base SQL query
         String sql = "SELECT COUNT(*) AS total FROM Items i JOIN Categories c ON i.CategoryID = c.CategoryID";
         StringBuilder whereClause = new StringBuilder();

         // Add search condition if search text is not empty
         if (!searchText.isEmpty()) {
             whereClause.append(" WHERE LOWER(i.ItemName) LIKE ?");
         }

         // Add category filter condition if a specific category is selected
         boolean categorySelected = selectedCategory != null && !selectedCategory.equals("All Categories");
         if (categorySelected) {
             if (whereClause.length() == 0) {
                 whereClause.append(" WHERE");
             } else {
                 whereClause.append(" AND");
             }
             whereClause.append(" c.CategoryName = ?");
         }

         // Append the where clause to the SQL query BEFORE preparing the statement
         final String finalSql = sql + whereClause.toString();


        new Thread(new Runnable() {
            @Override
            public void run() {
                int count = 0;
                 try (Connection conn = DBConnection.getConnection();
                      // Prepare the statement with the complete SQL query
                     PreparedStatement pstmt = conn.prepareStatement(finalSql)) { // Use finalSql

                    int paramIndex = 1;
                    // Set search parameters if needed
                    if (!searchText.isEmpty()) {
                        pstmt.setString(paramIndex++, "%" + searchText + "%");
                    }

                    // Set category parameter if needed
                    if (categorySelected) {
                        pstmt.setString(paramIndex++, selectedCategory);
                    }


                    try (ResultSet rs = pstmt.executeQuery()) {
                        if (rs.next()) {
                            count = rs.getInt("total");
                        }
                    }

                } catch (SQLException e) {
                    e.printStackTrace();
                    System.err.println("Error fetching total item count: " + e.getMessage());
                    // Handle error appropriately, maybe set totalItems to -1 to indicate error
                }

                final int finalCount = count;
                 SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        totalItems = finalCount;
                        // Recalculate total pages based on the new total count
                        int totalPagesAfter = (int) Math.ceil((double) totalItems / itemsPerPage);

                        // Adjust current page if it exceeds the new total pages
                        if (currentPage > totalPagesAfter && totalPagesAfter > 0) {
                            currentPage = totalPagesAfter;
                        } else if (totalPagesAfter == 0 && totalItems > 0) {
                             // This case should ideally not happen if totalItems > 0
                             currentPage = 1;
                        } else if (totalPagesAfter == 0 && totalItems == 0) {
                             currentPage = 1; // Still show page 1 even if no items
                        }


                        updatePaginationControls(); // Update controls based on total count and current page
                        loadInventoryData(); // Fetch and display data for the current page with filter
                    }
                });
            }
        }).start();
    }


    // Loads inventory data from the database for the current page (using SwingWorker for background loading)
    // Modified to include pagination and search/filter
    private void loadInventoryData() {
        if (conn == null) {
            System.err.println("Cannot load inventory: Database connection is null.");
            JOptionPane.showMessageDialog(this, "Database connection is not available. Cannot load inventory.", "Connection Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String searchText = searchField.getText().trim().toLowerCase();
        String selectedCategory = (String) categoryFilter.getSelectedItem();
        int offset = (currentPage - 1) * itemsPerPage;

        // Build the SQL query dynamically
        StringBuilder sql = new StringBuilder(
                "SELECT i.ItemID, i.ItemName, c.CategoryName, i.CurrentQuantity, i.UnitPrice, i.MinimumQuantity, i.ItemImage "
                + "FROM Items i JOIN Categories c ON i.CategoryID = c.CategoryID");
        StringBuilder whereClause = new StringBuilder();

         // Add search condition if search text is not empty
         if (!searchText.isEmpty()) {
             whereClause.append(" WHERE LOWER(i.ItemName) LIKE ?");
         }

         // Add category filter condition if a specific category is selected
         boolean categorySelected = selectedCategory != null && !selectedCategory.equals("All Categories");
         if (categorySelected) {
             if (whereClause.length() == 0) {
                 whereClause.append(" WHERE");
             } else {
                 whereClause.append(" AND");
             }
             whereClause.append(" c.CategoryName = ?");
         }

        sql.append(whereClause.toString());
        sql.append(" ORDER BY i.ItemName LIMIT ? OFFSET ?"); // Add ordering and pagination

        final String finalSql = sql.toString(); // Make sql final for the inner class


        // Use SwingWorker to load data in the background
        new InventoryLoader(finalSql, searchText, selectedCategory, categorySelected, offset, itemsPerPage).execute();
    }

    // SwingWorker class to handle background database loading with pagination and filtering
    private class InventoryLoader extends SwingWorker<Void, Object[]> {

        private final String sql;
        private final String searchText;
        private final String selectedCategory;
        private final boolean categorySelected;
        private final int offset;
        private final int limit;

        // Constructor to receive parameters
        public InventoryLoader(String sql, String searchText, String selectedCategory, boolean categorySelected, int offset, int limit) {
            this.sql = sql;
            this.searchText = searchText;
            this.selectedCategory = selectedCategory;
            this.categorySelected = categorySelected;
            this.offset = offset;
            this.limit = limit;
             // Clear table on EDT before starting background task
             SwingUtilities.invokeLater(() -> tableModel.setRowCount(0));
        }


        @Override
        protected Void doInBackground() throws Exception {
            System.out.println("InventoryLoader: Starting background data load...");
            // The SQL query is now passed in the constructor

            try (Connection conn = DBConnection.getConnection(); // Get a new connection for the thread
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {

                int paramIndex = 1;
                // Set search parameters if needed
                if (!searchText.isEmpty()) {
                    pstmt.setString(paramIndex++, "%" + searchText + "%");
                }

                // Set category parameter if needed
                if (categorySelected) {
                    pstmt.setString(paramIndex++, selectedCategory);
                }

                // Set pagination parameters
                pstmt.setInt(paramIndex++, limit);
                pstmt.setInt(paramIndex++, offset);


                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        int itemId = rs.getInt("ItemID");
                        String itemName = rs.getString("ItemName");
                        String categoryName = rs.getString("CategoryName");
                        int currentQuantity = rs.getInt("CurrentQuantity");
                        double unitPrice = rs.getDouble("UnitPrice");
                        int minimumQuantity = rs.getInt("MinimumQuantity"); // Use this for status calculation
                        byte[] imageData = rs.getBytes("ItemImage");
                        ImageIcon thumbnailIcon = null;

                        // Create thumbnail if image data exists
                        if (imageData != null && imageData.length > 0) {
                            try {
                                ImageIcon originalIcon = new ImageIcon(imageData);
                                Image scaledImage = originalIcon.getImage().getScaledInstance(50, 50, Image.SCALE_SMOOTH);
                                thumbnailIcon = new ImageIcon(scaledImage);
                            } catch (Exception imgEx) {
                                System.err.println("Error creating thumbnail for ItemID " + itemId + ": " + imgEx.getMessage());
                                // thumbnailIcon remains null
                            }
                        }

                        // Calculate total value and determine status
                        double totalValue = currentQuantity * unitPrice;
                        String status;
                        if (currentQuantity <= 0) {
                            status = "Out of Stock";
                        } else if (currentQuantity <= minimumQuantity) {
                            status = "Low Stock";
                        } else {
                            status = "In Stock";
                        }

                        // Publish row data for processing on the EDT
                        publish(new Object[]{itemId, itemName, categoryName, currentQuantity, unitPrice, totalValue, status, thumbnailIcon});
                    }
                }
            } catch (SQLException e) {
                System.err.println("Error loading inventory data in background: " + e.getMessage());
                // Show error message on the EDT
                SwingUtilities.invokeLater(() ->
                    JOptionPane.showMessageDialog(Inventory.this, "Failed to load inventory data: " + e.getMessage(),
                            "Database Error", JOptionPane.ERROR_MESSAGE)
                );
            }
            return null;
        }

        // This method runs on the EDT to update the table model safely
        @Override
        protected void process(java.util.List<Object[]> chunks) {
            for (Object[] rowData : chunks) {
                 tableModel.addRow(rowData);
            }
        }

        @Override
        protected void done() {
            try {
                get(); // Check for exceptions during doInBackground
                System.out.println("InventoryLoader: Background data load finished successfully.");
            } catch (Exception e) {
                System.err.println("InventoryLoader: Error occurred during background execution.");
                e.printStackTrace();
                 // Optionally show a final error message if not shown already
            } finally {
                 // Re-enable search/filter components if they were disabled
            }
        }
    }


    // Searches the inventory based on text and category filters (now triggers total count fetch)
    private void searchInventory() {
        currentPage = 1; // Reset to first page on search
        fetchTotalItemCount(); // Fetch total count with filter and then display data
    }

     /**
     * Updates the state of the pagination buttons and the page info label.
     */
    private void updatePaginationControls() {
        int totalPages = (int) Math.ceil((double) totalItems / itemsPerPage);
        jLabelPageInfo.setText("Page " + currentPage + " of " + totalPages);

        jButtonPreviousPage.setEnabled(currentPage > 1);
        jButtonNextPage.setEnabled(currentPage < totalPages);

        // If there are no items (after filtering), disable both buttons
        if (totalItems <= 0) {
            jButtonPreviousPage.setEnabled(false);
            jButtonNextPage.setEnabled(false);
            jLabelPageInfo.setText("No items found");
        }
    }

    /**
     * Goes to the previous page of inventory data.
     */
    private void gotoPreviousPage() {
        if (currentPage > 1) {
            currentPage--;
            loadInventoryData(); // Fetch data for the new page
            updatePaginationControls(); // Update buttons
        }
    }

    /**
     * Goes to the next page of inventory data.
     */
    private void gotoNextPage() {
        int totalPages = (int) Math.ceil((double) totalItems / itemsPerPage);
        if (currentPage < totalPages) {
            currentPage++;
            loadInventoryData(); // Fetch data for the new page
            updatePaginationControls(); // Update buttons
        }
    }

    // >>> End Pagination Methods <<<


     /**
     * Loads details for a specific item ID from the database and populates the detail panel.
     * This is called when a table row is clicked.
     *
     * @param itemId The ID of the item to load.
     */
    private void loadItemDetails(int itemId) {
        System.out.println("Loading details for Item ID: " + itemId); // Debug
        String sql = "SELECT i.ItemName, i.CategoryID, c.CategoryName, i.CurrentQuantity, i.UnitPrice, i.ItemImage, i.ItemImageType "
                   + "FROM Items i JOIN Categories c ON i.CategoryID = c.CategoryID "
                   + "WHERE i.ItemID = ?";
        if (conn == null) {
            System.err.println("Cannot load item details: Database connection is null.");
            return;
        }

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, itemId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    // Retrieve data from ResultSet
                    String itemName = rs.getString("ItemName");
                    String categoryName = rs.getString("CategoryName"); // Get name directly
                    int currentQuantity = rs.getInt("CurrentQuantity");
                    double unitPrice = rs.getDouble("UnitPrice");
                    byte[] imageData = rs.getBytes("ItemImage");
                    selectedImageType = rs.getString("ItemImageType"); // Store image type if needed for saving

                    // Populate fields on the EDT
                    SwingUtilities.invokeLater(() -> {
                        itemIdField.setText(String.valueOf(itemId));
                        nameField.setText(itemName);
                        categoryField.setSelectedItem(categoryName); // Set by name
                        quantityField.setText(String.valueOf(currentQuantity));
                        priceField.setText(String.format("%.2f", unitPrice)); // Format price

                        // Handle image display
                        selectedImageFile = null; // Reset selected file from browse
                        if (imageData != null && imageData.length > 0) {
                            try {
                                ImageIcon imageIcon = new ImageIcon(imageData);
                                // Use the preferred size of the label for scaling target
                                int targetWidth = imageLabel.getPreferredSize().width - 10; // Subtract padding/border
                                int targetHeight = imageLabel.getPreferredSize().height - 10;
                                Image scaledImage = imageIcon.getImage().getScaledInstance(targetWidth, targetHeight, Image.SCALE_SMOOTH); // Scale to fit label's preferred size
                                imageLabel.setIcon(new ImageIcon(scaledImage));
                                imageLabel.setText(""); // Clear "No Image" text
                            } catch (Exception imgEx) {
                                imageLabel.setIcon(null);
                                imageLabel.setText("Error Displaying Image");
                                System.err.println("Error scaling/displaying image for ItemID " + itemId + ": " + imgEx.getMessage());
                            }
                        } else {
                            imageLabel.setIcon(null);
                            imageLabel.setText("No Image Available");
                        }

                        // Set state for editing
                        isNewItem = false; // We are viewing/editing an existing item
                        detailPanel.setVisible(true); // Show the details panel
                        enableActionButtons(true); // Enable Save, Delete, Cancel
                        System.out.println("Details loaded successfully for: " + itemName); // Debug
                    });

                } else {
                    // Handle case where item ID is not found
                    SwingUtilities.invokeLater(() ->
                        JOptionPane.showMessageDialog(this, "Item with ID " + itemId + " not found.", "Error", JOptionPane.ERROR_MESSAGE)
                    );
                    hideDetailsPanel(); // Hide panel if item not found
                }
            }
        } catch (SQLException e) {
            System.err.println("Error loading item details for ID " + itemId + ": " + e.getMessage());
            SwingUtilities.invokeLater(() ->
                JOptionPane.showMessageDialog(this, "Failed to load item details: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE)
            );
            hideDetailsPanel(); // Hide panel on error
        }
    }


    // Prepares the detail panel for adding a new item
    private void prepareNewItem() {
        isNewItem = true; // Set flag for adding new
        clearDetailsPanel(); // Clear all fields and image
        itemIdField.setText("AUTO"); // Indicate auto-generated ID
        detailPanel.setVisible(true); // Show the panel
        enableActionButtons(true); // Enable Save and Cancel, disable Delete
        deleteButton.setEnabled(false); // Explicitly disable delete for new items
        nameField.requestFocus(); // Set focus to the first editable field
    }

    // Saves the current item (either adds a new one or updates an existing one)
    private void saveItem() {
        if (!validateInput()) {
            return; // Stop if validation fails
        }

        // Retrieve values from fields
        String name = nameField.getText().trim();
        String categoryName = (String) categoryField.getSelectedItem();
        int quantity = Integer.parseInt(quantityField.getText().trim());
        double price = Double.parseDouble(priceField.getText().trim());

        // Get Category ID from selected name
        int categoryId = getCategoryId(categoryName);
        if (categoryId == -1) {
            JOptionPane.showMessageDialog(this, "Invalid category selected.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (conn == null) {
            JOptionPane.showMessageDialog(this, "Database connection is not available. Cannot save item.", "Connection Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        FileInputStream fis = null;
        String sql;
        String successMessage;
        String logActivityType;
        String logDetails;

        try {
            if (isNewItem) {
                // --- INSERT new item ---
                sql = "INSERT INTO Items (ItemName, CategoryID, CurrentQuantity, UnitPrice, "
                    + "ItemImage, ItemImageType, AddedBy, CreatedAt, MinimumQuantity, Status, Unit) " // Added more fields
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, NOW(), ?, ?, ?)"; // 10 placeholders + NOW()
                successMessage = "Item added successfully.";
                logActivityType = "Item Added";
                logDetails = "Item '" + name + "' added";

                try (PreparedStatement pstmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
                    pstmt.setString(1, name);
                    pstmt.setInt(2, categoryId);
                    pstmt.setInt(3, quantity);
                    pstmt.setDouble(4, price);

                    // Handle image insertion
                    if (selectedImageFile != null && selectedImageFile.exists()) {
                        fis = new FileInputStream(selectedImageFile);
                        pstmt.setBinaryStream(5, fis, (int) selectedImageFile.length());
                        pstmt.setString(6, selectedImageType);
                    } else {
                        pstmt.setNull(5, java.sql.Types.BLOB); // Set image to NULL if none selected
                        pstmt.setNull(6, java.sql.Types.VARCHAR); // Set image type to NULL
                    }

                    pstmt.setInt(7, currentUserId); // AddedBy
                    // DateAdded is handled by NOW() in SQL
                    pstmt.setInt(8, 10);      // Default MinimumQuantity (adjust as needed)
                    pstmt.setString(9, quantity > 0 ? "In Stock" : "Out of Stock"); // Initial Status
                    pstmt.setString(10, "pcs"); // Default Unit (adjust as needed)

                    int rowsAffected = pstmt.executeUpdate();

                     if (rowsAffected > 0) {
                         // Optionally get the generated ItemID if needed
                         try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                             if (generatedKeys.next()) {
                                 int newItemId = generatedKeys.getInt(1);
                                 System.out.println("New Item ID generated: " + newItemId);
                                 logDetails += " (ID: " + newItemId + ")"; // Add ID to log
                             }
                         }
                     } else {
                         throw new SQLException("Creating item failed, no rows affected.");
                     }
                }

            } else {
                // --- UPDATE existing item ---
                int itemId = Integer.parseInt(itemIdField.getText()); // Get ID from the field
                successMessage = "Item updated successfully.";
                logActivityType = "Item Updated";
                logDetails = "Item '" + name + "' (ID: " + itemId + ") updated";

                // Check if a new image was selected
                if (selectedImageFile != null && selectedImageFile.exists()) {
                    // Update WITH new image
                    sql = "UPDATE Items SET ItemName = ?, CategoryID = ?, CurrentQuantity = ?, UnitPrice = ?, "
                        + "ItemImage = ?, ItemImageType = ?, LastUpdatedAt = NOW(), MinimumQuantity = ?, Status = ?, Unit = ? "
                        + "WHERE ItemID = ?";
                    try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                        pstmt.setString(1, name);
                        pstmt.setInt(2, categoryId);
                        pstmt.setInt(3, quantity);
                        pstmt.setDouble(4, price);
                        fis = new FileInputStream(selectedImageFile);
                        pstmt.setBinaryStream(5, fis, (int) selectedImageFile.length());
                        pstmt.setString(6, selectedImageType);
                        // Other fields
                        pstmt.setInt(7, 10); // Min Qty
                        pstmt.setString(8, quantity > 0 ? "In Stock" : "Out of Stock"); // Status
                        pstmt.setString(9, "pcs"); // Unit
                        pstmt.setInt(10, itemId); // WHERE
                        System.out.println("Executing update WITH new image for ItemID: " + itemId);
                        pstmt.executeUpdate();
                    }
                } else {
                    // Update WITHOUT changing the image
                    sql = "UPDATE Items SET ItemName = ?, CategoryID = ?, CurrentQuantity = ?, UnitPrice = ?, "
                         + "LastUpdatedAt = NOW(), MinimumQuantity = ?, Status = ?, Unit = ? " // Exclude image columns
                         + "WHERE ItemID = ?";
                     try (PreparedStatement pstmtUpdateNoImage = conn.prepareStatement(sql)) {
                         pstmtUpdateNoImage.setString(1, name);
                         pstmtUpdateNoImage.setInt(2, categoryId);
                         pstmtUpdateNoImage.setInt(3, quantity);
                         pstmtUpdateNoImage.setDouble(4, price);
                         // LastUpdated handled by NOW()
                         pstmtUpdateNoImage.setInt(5, 10); // Min Qty
                         pstmtUpdateNoImage.setString(6, quantity > 0 ? "In Stock" : "Out of Stock"); // Status
                         pstmtUpdateNoImage.setString(7, "pcs"); // Unit
                         pstmtUpdateNoImage.setInt(8, itemId); // WHERE
                         System.out.println("Executing update WITHOUT image change for ItemID: " + itemId);
                         pstmtUpdateNoImage.executeUpdate();
                     }
                }
            } // End of if (isNewItem) else block

            // Common actions after successful save
            JOptionPane.showMessageDialog(this, successMessage, "Success", JOptionPane.INFORMATION_MESSAGE);
            logActivity(logActivityType, logDetails);
            fetchTotalItemCount(); // Refresh table data by refetching count and then data
            hideDetailsPanel(); // Hide the details panel

        } catch (SQLException | IOException e) {
            System.err.println("Error saving item: " + e.getMessage());
            e.printStackTrace(); // Print stack trace for detailed debugging
            JOptionPane.showMessageDialog(this, "Failed to save item: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            // Ensure FileInputStream is closed
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException ex) {
                    System.err.println("Error closing FileInputStream: " + ex.getMessage());
                }
            }
        }
    }


    // Retrieves the CategoryID for a given CategoryName
    private int getCategoryId(String categoryName) {
        String sql = "SELECT CategoryID FROM Categories WHERE CategoryName = ?";
        if (conn == null || categoryName == null) {
            System.err.println("Cannot get category ID: Connection is null or category name is null.");
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
            System.err.println("Error getting category ID for '" + categoryName + "': " + e.getMessage());
        }
        return -1; // Category not found or error occurred
    }

    // Deletes the currently selected item after confirmation
    private void deleteItem() {
        if (isNewItem || itemIdField.getText().isEmpty() || itemIdField.getText().equals("AUTO")) {
            JOptionPane.showMessageDialog(this, "No item selected to delete.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int itemId;
        try {
            itemId = Integer.parseInt(itemIdField.getText());
        } catch (NumberFormatException e) {
             JOptionPane.showMessageDialog(this, "Invalid Item ID selected.", "Error", JOptionPane.ERROR_MESSAGE);
             return;
        }

        String itemName = nameField.getText(); // Get name for confirmation message

        int result = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete item '" + itemName + "' (ID: " + itemId + ")?",
                "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (result == JOptionPane.YES_OPTION) {
            String sql = "DELETE FROM Items WHERE ItemID = ?";
            if (conn == null) {
                JOptionPane.showMessageDialog(this, "Database connection is not available. Cannot delete item.", "Connection Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, itemId);
                int rowsAffected = pstmt.executeUpdate();
                if (rowsAffected > 0) {
                    JOptionPane.showMessageDialog(this, "Item deleted successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
                    logActivity("Item Deleted", "Item '" + itemName + "' (ID: " + itemId + ") deleted"); // Log before hiding panel
                    fetchTotalItemCount(); // Refresh table by refetching count and then data
                    hideDetailsPanel(); // Hide details panel
                    // No duplicate logging needed here
                } else {
                    JOptionPane.showMessageDialog(this, "Failed to delete item (Item ID not found or error occurred).", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (SQLException e) {
                System.err.println("Error deleting item: " + e.getMessage());
                // Check for foreign key constraints
                if (e.getSQLState() != null && e.getSQLState().startsWith("23")) { // SQL state for integrity constraint violation
                     JOptionPane.showMessageDialog(this, "Failed to delete item: It might be referenced in other records (e.g., orders, transactions).", "Database Error", JOptionPane.ERROR_MESSAGE);
                } else {
                     JOptionPane.showMessageDialog(this, "Failed to delete item: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    // Opens a file chooser dialog to select an image file
    private void browseImage() {
        JFileChooser fileChooser = new JFileChooser();
        // Set default directory (optional)
        // fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Image Files", "jpg", "jpeg", "png", "gif");
        fileChooser.setFileFilter(filter);
        fileChooser.setAcceptAllFileFilterUsed(false); // Don't allow "All Files"

        int returnVal = fileChooser.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            selectedImageFile = fileChooser.getSelectedFile();
            // Extract file extension for image type
            String fileName = selectedImageFile.getName();
            int dotIndex = fileName.lastIndexOf('.');
            if (dotIndex > 0 && dotIndex < fileName.length() - 1) {
                selectedImageType = fileName.substring(dotIndex + 1).toLowerCase();
            } else {
                selectedImageType = null; // Unknown type
            }
            System.out.println("Selected image: " + selectedImageFile.getAbsolutePath() + ", Type: " + selectedImageType);
            displaySelectedImage(selectedImageFile.getAbsolutePath());
        }
    }

    // Displays the selected image in the imageLabel
    private void displaySelectedImage(String imagePath) {
        try {
            ImageIcon imageIcon = new ImageIcon(imagePath);
            // Scale image to fit the label while maintaining aspect ratio
            Image image = imageIcon.getImage();
            // Use the preferred size of the label for scaling target
            int targetWidth = imageLabel.getPreferredSize().width - 10; // Subtract padding/border
            int targetHeight = imageLabel.getPreferredSize().height - 10;

            // Calculate scaled dimensions, ensuring we don't scale up
            int originalWidth = image.getWidth(null);
            int originalHeight = image.getHeight(null);

            if (originalWidth <= 0 || originalHeight <= 0) {
                 throw new Exception("Invalid image dimensions");
            }

            int scaledWidth = originalWidth;
            int scaledHeight = originalHeight;

            if (originalWidth > targetWidth) {
                scaledWidth = targetWidth;
                scaledHeight = (scaledWidth * originalHeight) / originalWidth;
            }

            if (scaledHeight > targetHeight) {
                scaledHeight = targetHeight;
                scaledWidth = (scaledHeight * originalWidth) / originalHeight;
            }
            // Ensure scaled dimensions are positive
            scaledWidth = Math.max(1, scaledWidth);
            scaledHeight = Math.max(1, scaledHeight);


            Image scaledImage = image.getScaledInstance(scaledWidth, scaledHeight, Image.SCALE_SMOOTH);

            imageLabel.setIcon(new ImageIcon(scaledImage));
            imageLabel.setText(null); // Remove text
        } catch (Exception e) {
             System.err.println("Error loading image for display: " + e.getMessage());
             e.printStackTrace(); // Print stack trace
             imageLabel.setIcon(null);
             imageLabel.setText("Preview Error");
             selectedImageFile = null; // Invalidate selection if loading fails
             selectedImageType = null;
        }
    }

    // Clears all fields and resets the image in the details panel
    private void clearDetailsPanel() {
        itemIdField.setText("");
        nameField.setText("");
        if (categoryField.getItemCount() > 0) {
            categoryField.setSelectedIndex(0); // Select the first category (or handle no categories case)
        }
        quantityField.setText("");
        priceField.setText("");
        imageLabel.setIcon(null);
        imageLabel.setText("No Image Selected");
        imageLabel.setForeground(Color.LIGHT_GRAY); // Reset text color
        selectedImageFile = null; // Clear selected file reference
        selectedImageType = null; // Clear selected type
        isNewItem = true; // Default to new item state when cleared manually
    }

    // Enables or disables the Save, Delete, and Cancel buttons
    private void enableActionButtons(boolean enable) {
        // Check if buttons have been initialized before enabling/disabling
        if (saveButton != null) saveButton.setEnabled(enable);
        if (deleteButton != null) deleteButton.setEnabled(enable && !isNewItem); // Only enable delete if not a new item
        if (cancelButton != null) cancelButton.setEnabled(enable);
    }

    // Hides the details panel and clears its contents
    private void hideDetailsPanel() {
        detailPanel.setVisible(false);
        clearDetailsPanel();
        enableActionButtons(false); // Disable buttons when panel is hidden
        inventoryTable.clearSelection(); // Clear table selection
    }

    // >>> Add Category Functionality <<<

    /**
     * Shows a dialog to add a new category.
     */
    private void showAddCategoryDialog() {
        // Create a simple input dialog
        String newCategoryName = JOptionPane.showInputDialog(this,
                "Enter the name for the new category:",
                "Add New Category",
                JOptionPane.PLAIN_MESSAGE);

        // If the user entered a name and didn't cancel
        if (newCategoryName != null && !newCategoryName.trim().isEmpty()) {
            addNewCategory(newCategoryName.trim());
        } else if (newCategoryName != null) {
            // If user clicked OK but entered empty string
             JOptionPane.showMessageDialog(this, "Category name cannot be empty.", "Input Error", JOptionPane.WARNING_MESSAGE);
        }
        // If newCategoryName is null, user cancelled the dialog, do nothing.
    }

    /**
     * Adds a new category to the database and refreshes the category combo boxes.
     * @param categoryName The name of the new category.
     */
    private void addNewCategory(String categoryName) {
        if (conn == null) {
            JOptionPane.showMessageDialog(this, "Database connection is not available. Cannot add category.", "Connection Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Check if category already exists (optional but recommended)
        if (categoryExists(categoryName)) {
            JOptionPane.showMessageDialog(this, "Category '" + categoryName + "' already exists.", "Duplicate Category", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String sql = "INSERT INTO Categories (CategoryName) VALUES (?)"; // Assuming Description is nullable or has default
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, categoryName);
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                JOptionPane.showMessageDialog(this, "Category '" + categoryName + "' added successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
                logActivity("Category Added", "New category '" + categoryName + "' added"); // Log activity
                loadCategories(); // Refresh the category combo boxes
            } else {
                JOptionPane.showMessageDialog(this, "Failed to add category.", "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (SQLException e) {
            System.err.println("Error adding new category: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Failed to add category: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Checks if a category with the given name already exists in the database.
     * @param categoryName The name to check.
     * @return true if the category exists, false otherwise.
     */
    private boolean categoryExists(String categoryName) {
        String sql = "SELECT COUNT(*) FROM Categories WHERE CategoryName = ?";
         if (conn == null) {
            System.err.println("Cannot check category existence: Database connection is null.");
            return false; // Assume it doesn't exist if no connection
        }
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, categoryName);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0; // Return true if count is greater than 0
                }
            }
        } catch (SQLException e) {
             System.err.println("Error checking category existence: " + e.getMessage());
        }
        return false; // Assume it doesn't exist on error
    }

    // >>> End Add Category Functionality <<<

    
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
