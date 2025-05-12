package modules;

import Package1.DBConnection;
import Package1.User;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;

// Import the dialog for borrowing details
import modules.BorrowItemDialog.BorrowCompleteListener;


public class KioskDashboard extends javax.swing.JPanel implements BorrowCompleteListener { // Implement the listener interface

    private JTable availableItemsTable;
    private JTextField searchField;
    private DefaultTableModel tableModel;

    private Connection conn = null;
    private User currentUser; // The logged-in Kiosk user

    private int currentPage = 1;
    private final int itemsPerPage = 10;
    private int totalAvailableItems = 0;
    private JButton jButtonPreviousPage;
    private JButton jButtonNextPage;
    private JLabel jLabelPageInfo;

    // Keep track of selected item for borrowing
    private int selectedItemId = -1;
    private String selectedItemName = null;

    // SwingWorker for loading data
    private AvailableItemsLoader currentDataLoader;


    public KioskDashboard() {
        initComponents();
        setupKioskDashboardPanel();
        if (!connectToDatabase()) {
            JOptionPane.showMessageDialog(this, "Database connection failed. Kiosk features disabled.", "Connection Error", JOptionPane.ERROR_MESSAGE);
        } else {
             // Initial load: fetch total count, which will then trigger data load
            fetchTotalAvailableItemCount();
        }
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        if (this.currentUser != null) {
            System.out.println("KioskDashboard: User object set. UserID: " + this.currentUser.getUserId() + ", Username: " + this.currentUser.getUsername());
        } else {
            System.out.println("KioskDashboard: Current user is null.");
        }
        currentPage = 1;
        fetchTotalAvailableItemCount(); // Refresh data based on user (though Kiosk view is generic)
    }

     // This method might be called from DashBoardFrame1
    public void setCurrentUserId(int userId) {
         System.out.println("KioskDashboard: setCurrentUserId(int) called with UserID: " + userId);
        if (userId <= 0) {
            System.err.println("KioskDashboard: Invalid UserID passed to setCurrentUserId: " + userId);
            setCurrentUser(null);
            return;
        }

        // Fetch the full user object
        User user = fetchUserById(userId);
        if (user == null) {
            System.err.println("KioskDashboard: Failed to fetch user details for UserID: " + userId + ". Kiosk operations requiring a user may fail.");
        }
        setCurrentUser(user);
    }

     private User fetchUserById(int userId) {
        if (conn == null) {
            if (!connectToDatabase()) {
                 System.err.println("KioskDashboard.fetchUserById: Database connection is not available.");
                 return null;
            }
        }
        String sql = "SELECT UserID, Username, FullName, Role FROM Users WHERE UserID = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    User user = new User();
                    user.setUserId(rs.getInt("UserID"));
                    user.setUsername(rs.getString("Username"));
                    user.setFullName(rs.getString("FullName"));
                    user.setRole(rs.getString("Role"));
                    return user;
                } else {
                    System.err.println("KioskDashboard.fetchUserById: No user found with UserID: " + userId);
                }
            }
        } catch (SQLException e) {
            System.err.println("KioskDashboard.fetchUserById: SQL error fetching user with ID " + userId + ": " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }


    private boolean connectToDatabase() {
        try {
            conn = DBConnection.getConnection();
            if (conn != null && !conn.isClosed()) {
                System.out.println("Database connected successfully in KioskDashboard");
                return true;
            } else {
                System.err.println("Failed to establish database connection in KioskDashboard.");
                return false;
            }
        } catch (SQLException ex) {
            Logger.getLogger(KioskDashboard.class.getName()).log(Level.SEVERE, "Database connection error in KioskDashboard", ex);
            return false;
        }
    }

    private void setupKioskDashboardPanel() {
        this.setLayout(new BorderLayout(10, 10));
        this.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        this.setBackground(new Color(30, 30, 30));

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);


        JPanel searchPanel = createSearchPanel();
        topPanel.add(searchPanel, BorderLayout.SOUTH);

        this.add(topPanel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setOpaque(false);

        JPanel tablePanel = createAvailableItemsTablePanel();
        centerPanel.add(tablePanel, BorderLayout.CENTER);

        this.add(centerPanel, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        bottomPanel.setOpaque(false);

        JButton borrowButton = new JButton("Request Item");
        styleActionButton(borrowButton, new Color(52, 152, 219)); // Blue for Borrow
        borrowButton.addActionListener((ActionEvent e) -> openBorrowItemDialog());
        bottomPanel.add(borrowButton);

        this.add(bottomPanel, BorderLayout.SOUTH);

        // Add mouse listener to the table to select an item
        availableItemsTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent evt) {
                int selectedRow = availableItemsTable.getSelectedRow();
                if (selectedRow >= 0) {
                    int modelRow = availableItemsTable.convertRowIndexToModel(selectedRow);
                    Object itemIdObj = tableModel.getValueAt(modelRow, 0);
                    Object itemNameObj = tableModel.getValueAt(modelRow, 1);
                     if (itemIdObj != null && itemNameObj != null) {
                        try {
                            selectedItemId = Integer.parseInt(itemIdObj.toString());
                            selectedItemName = itemNameObj.toString();
                            System.out.println("Item selected: ID=" + selectedItemId + ", Name=" + selectedItemName); // Debug
                        } catch (NumberFormatException e) {
                            System.err.println("Error parsing Item ID from table: " + itemIdObj);
                            selectedItemId = -1; // Reset on error
                            selectedItemName = null;
                        }
                    } else {
                         selectedItemId = -1; // Reset if data is null
                         selectedItemName = null;
                    }
                } else {
                     selectedItemId = -1; // Reset if no row is selected
                     selectedItemName = null;
                }
            }
        });
    }

    private JPanel createSearchPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        panel.setOpaque(false);

        JLabel searchLabel = new JLabel("Search Available Items:");
        searchLabel.setFont(new Font("Verdana", Font.BOLD, 14));
        searchLabel.setForeground(Color.WHITE);
        panel.add(searchLabel);

        searchField = new JTextField(25);
        searchField.setFont(new Font("Verdana", Font.PLAIN, 14));
        panel.add(searchField);

        JButton searchBtn = new JButton("Search");
        searchBtn.setFont(new Font("Verdana", Font.BOLD, 14));
        searchBtn.setBackground(new Color(41, 128, 185));
        searchBtn.setForeground(Color.WHITE);
        searchBtn.addActionListener((ActionEvent e) -> searchAvailableItems());
        panel.add(searchBtn);

        return panel;
    }

    private JPanel createAvailableItemsTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.GRAY),
                "Available Items", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
                javax.swing.border.TitledBorder.DEFAULT_POSITION,
                new Font("Verdana", Font.BOLD, 14), Color.WHITE));

        String[] columns = {"ID", "Name", "Category", "Qty", "Unit", "Status", "Condition", "Location", "Image"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                 if (columnIndex == 8) {
                    return ImageIcon.class;
                }
                return super.getColumnClass(columnIndex);
            }
        };

        availableItemsTable = new JTable(tableModel);
        availableItemsTable.setForeground(Color.WHITE);
        availableItemsTable.setBackground(new Color(30, 30, 30));
        availableItemsTable.setGridColor(new Color(50, 50, 50));
        availableItemsTable.setSelectionBackground(new Color(41, 128, 185));
        availableItemsTable.setSelectionForeground(Color.WHITE);
        availableItemsTable.setFont(new Font("Verdana", Font.PLAIN, 12));
        availableItemsTable.setRowHeight(60);
        availableItemsTable.setAutoCreateRowSorter(true); // Enable sorting

        availableItemsTable.getTableHeader().setFont(new Font("Verdana", Font.BOLD, 12));
        availableItemsTable.getTableHeader().setBackground(new Color(40, 40, 40));
        availableItemsTable.getTableHeader().setForeground(Color.WHITE);
        availableItemsTable.getTableHeader().setReorderingAllowed(false);

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);

        for (int i = 0; i < availableItemsTable.getColumnCount(); i++) {
             if (i != 8) { // Apply center renderer to all columns except the image column
                availableItemsTable.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
             }
        }

         // Custom renderer for Status column to color text
         availableItemsTable.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                String status = value != null ? value.toString() : "";
                if ("Low Stock".equalsIgnoreCase(status)) {
                    c.setForeground(new Color(231, 76, 60)); // Red
                } else if ("Out of Stock".equalsIgnoreCase(status)) {
                    c.setForeground(Color.ORANGE); // Orange
                } else if ("In Stock".equalsIgnoreCase(status)) {
                    c.setForeground(new Color(46, 204, 113)); // Green
                } else {
                    c.setForeground(isSelected ? table.getSelectionForeground() : Color.WHITE); // Default color
                }
                c.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
                setHorizontalAlignment(JLabel.CENTER);
                return c;
            }
        });

         // Custom renderer for Image column
         availableItemsTable.getColumnModel().getColumn(8).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                label.setText(""); // Clear any default text
                if (value instanceof ImageIcon) {
                    ImageIcon originalIcon = (ImageIcon) value;
                    // Scale image for table display
                    Image scaledImage = originalIcon.getImage().getScaledInstance(-1, 50, Image.SCALE_SMOOTH); // Scale height to 50px, maintain aspect ratio
                    label.setIcon(new ImageIcon(scaledImage));
                } else {
                    label.setIcon(null); // No image available
                }
                label.setHorizontalAlignment(JLabel.CENTER);
                label.setVerticalAlignment(JLabel.CENTER);
                 label.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
                return label;
            }
        });


        // Set preferred column widths
        availableItemsTable.getColumnModel().getColumn(0).setPreferredWidth(40); // ID
        availableItemsTable.getColumnModel().getColumn(1).setPreferredWidth(150); // Name
        availableItemsTable.getColumnModel().getColumn(2).setPreferredWidth(100); // Category
        availableItemsTable.getColumnModel().getColumn(3).setPreferredWidth(50);  // Qty
        availableItemsTable.getColumnModel().getColumn(4).setPreferredWidth(60);  // Unit
        availableItemsTable.getColumnModel().getColumn(5).setPreferredWidth(80);  // Status
        availableItemsTable.getColumnModel().getColumn(6).setPreferredWidth(90);  // Condition
        availableItemsTable.getColumnModel().getColumn(7).setPreferredWidth(100); // Location
        availableItemsTable.getColumnModel().getColumn(8).setPreferredWidth(70);  // Image


        JScrollPane scrollPane = new JScrollPane(availableItemsTable);
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


     private void styleActionButton(JButton button, Color bgColor) {
        button.setFont(new Font("Verdana", Font.BOLD, 14));
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(bgColor.darker(), 1),
                BorderFactory.createEmptyBorder(5, 15, 5, 15)
        ));
    }

     private void stylePaginationButton(JButton button) {
        button.setFont(new Font("Verdana", Font.PLAIN, 12));
        button.setBackground(new Color(70, 70, 70));
        button.setForeground(Color.WHITE);
         button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.GRAY, 1), BorderFactory.createEmptyBorder(3, 8, 3, 8)));
    }


    private void fetchTotalAvailableItemCount() {
        if (conn == null) return;
        String searchText = searchField.getText().trim().toLowerCase();

        // Query to count items that are not archived and have quantity > 0
        StringBuilder sqlBuilder = new StringBuilder("SELECT COUNT(*) AS total FROM Items i");

        sqlBuilder.append(" WHERE i.IsArchived = FALSE AND i.Quantity > 0");

        if (!searchText.isEmpty()) {
            sqlBuilder.append(" AND (LOWER(i.ItemName) LIKE ? OR LOWER(i.Description) LIKE ? OR LOWER(i.SerialNumber) LIKE ?)");
        }

        final String sql = sqlBuilder.toString();

        new SwingWorker<Integer, Void>() {
            @Override
            protected Integer doInBackground() throws Exception {
                int count = 0;
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    int paramIndex = 1;
                    if (!searchText.isEmpty()) {
                        String searchTerm = "%" + searchText + "%";
                        pstmt.setString(paramIndex++, searchTerm);
                        pstmt.setString(paramIndex++, searchTerm);
                        pstmt.setString(paramIndex++, searchTerm);
                    }
                    try (ResultSet rs = pstmt.executeQuery()) {
                        if (rs.next()) {
                            count = rs.getInt("total");
                        }
                    }
                } catch (SQLException e) {
                    System.err.println("Error fetching total available item count: " + e.getMessage());
                }
                return count;
            }

            @Override
            protected void done() {
                try {
                    totalAvailableItems = get();
                    int totalPages = (int) Math.ceil((double) totalAvailableItems / itemsPerPage);
                    totalPages = Math.max(totalPages, 1); // Ensure at least 1 page if totalItems > 0
                    if (currentPage > totalPages) {
                        currentPage = totalPages;
                    }
                    updatePaginationControls();
                    // After fetching total count and updating pagination, refresh the table data
                    refreshTableData();
                } catch (Exception e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(KioskDashboard.this, "Error updating available item count: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    // New method to initiate table data loading
    private void refreshTableData() {
         if (conn == null) {
            System.err.println("Cannot refresh table data: DB connection is null.");
            SwingUtilities.invokeLater(() -> tableModel.setRowCount(0));
            return;
        }

        // Cancel any previous data loading task
        if (currentDataLoader != null && !currentDataLoader.isDone()) {
             System.out.println("Cancelling previous data loader task."); // Debug print
            currentDataLoader.cancel(true);
        }

        String searchText = searchField.getText().trim().toLowerCase();
        int offset = (currentPage - 1) * itemsPerPage;

        StringBuilder sqlBuilder = new StringBuilder(
            "SELECT i.ItemID, i.ItemName, c.CategoryName, i.Quantity, i.Unit, i.Status, " +
            "i.ItemCondition, i.Location, i.ItemImage " +
            "FROM Items i " +
            "LEFT JOIN Categories c ON i.CategoryID = c.CategoryID"
        );

        // Only fetch items that are not archived and have quantity > 0
        sqlBuilder.append(" WHERE i.IsArchived = FALSE AND i.Quantity > 0");

        if (!searchText.isEmpty()) {
            sqlBuilder.append(" AND (LOWER(i.ItemName) LIKE ? OR LOWER(i.Description) LIKE ? OR LOWER(i.SerialNumber) LIKE ?)");
        }
        sqlBuilder.append(" ORDER BY i.ItemID DESC LIMIT ? OFFSET ?");

        final String finalSql = sqlBuilder.toString();
        currentDataLoader = new AvailableItemsLoader(finalSql, searchText, offset, itemsPerPage);
        currentDataLoader.execute();
    }


    private class AvailableItemsLoader extends SwingWorker<Void, Object[]> {
        private final String sql;
        private final String searchText;
        private final int offset;
        private final int limit;

        public AvailableItemsLoader(String sql, String searchText, int offset, int limit) {
            this.sql = sql;
            this.searchText = searchText;
            this.offset = offset;
            this.limit = limit;
             System.out.println("AvailableItemsLoader created for page " + (offset / limit + 1)); // Debug print
            // Clear the table model on the Event Dispatch Thread (EDT)
            // before loading new data to prevent duplication when changing pages or searching.
            SwingUtilities.invokeLater(() -> {
                 System.out.println("Clearing table model on EDT before loading page " + (offset / limit + 1)); // Debug print
                 tableModel.setRowCount(0); // Clear table on EDT
            });
        }

        @Override
        protected Void doInBackground() throws Exception {
             System.out.println("AvailableItemsLoader doInBackground started for page " + (offset / limit + 1)); // Debug print
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                int paramIndex = 1;
                if (!searchText.isEmpty()) {
                    String searchTerm = "%" + searchText + "%";
                    pstmt.setString(paramIndex++, searchTerm);
                    pstmt.setString(paramIndex++, searchTerm);
                    pstmt.setString(paramIndex++, searchTerm);
                }
                pstmt.setInt(paramIndex++, limit);
                pstmt.setInt(paramIndex++, offset);

                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        // Check if the task has been cancelled
                        if (isCancelled()) {
                            System.out.println("AvailableItemsLoader task cancelled.");
                            break;
                        }
                        ImageIcon thumb = null;
                        byte[] imgData = rs.getBytes("ItemImage");
                        if (imgData != null && imgData.length > 0) {
                            try {
                                ImageIcon orig = new ImageIcon(imgData);
                                // Use SCALE_SMOOTH for better table image quality
                                Image scaled = orig.getImage().getScaledInstance(-1, 50, Image.SCALE_SMOOTH);
                                thumb = new ImageIcon(scaled);
                            } catch (Exception ix) {
                                System.err.println("Error scaling image for table: " + ix.getMessage());
                            }
                        }
                        publish(new Object[]{
                            rs.getInt("ItemID"),
                            rs.getString("ItemName"),
                            rs.getString("CategoryName"),
                            rs.getInt("Quantity"),
                            rs.getString("Unit"),
                            rs.getString("Status"),
                            rs.getString("ItemCondition"),
                            rs.getString("Location"),
                            thumb
                        });
                    }
                }
            } catch (SQLException e) {
                System.err.println("Error in AvailableItemsLoader: " + e.getMessage());
                 SwingUtilities.invokeLater(() ->
                    JOptionPane.showMessageDialog(KioskDashboard.this, "Error loading available item data: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE)
                );
            }
            return null;
        }

        @Override
        protected void process(java.util.List<Object[]> chunks) {
             System.out.println("AvailableItemsLoader process called with chunk size: " + chunks.size()); // Debug print
            // Only add rows if the task is not cancelled
            if (!isCancelled()) {
                for (Object[] rowData : chunks) {
                    tableModel.addRow(rowData);
                }
            } else {
                 System.out.println("AvailableItemsLoader process skipping adding rows due to cancellation.");
            }
        }
         @Override
        protected void done() {
             System.out.println("AvailableItemsLoader done called. Is cancelled: " + isCancelled()); // Debug print
            try {
                // Check if the task was cancelled before processing results
                if (!isCancelled()) {
                    get(); // Check for exceptions from doInBackground
                }
            } catch (Exception e) {
                e.printStackTrace();
                // Only show error dialog if the task was not cancelled
                if (!isCancelled()) {
                     JOptionPane.showMessageDialog(KioskDashboard.this, "Failed to complete available items loading: " + e.getMessage(), "Loading Error", JOptionPane.ERROR_MESSAGE);
                }
            } finally {
                 // Ensure currentDataLoader is set to null on completion or cancellation
                 currentDataLoader = null;
            }
        }
    }


    private void searchAvailableItems() {
        currentPage = 1;
        fetchTotalAvailableItemCount(); // Fetch total count for new search, which triggers refreshTableData
    }

    private void updatePaginationControls() {
        int totalPages = (int) Math.ceil((double) totalAvailableItems / itemsPerPage);
        totalPages = Math.max(totalPages, 1); // Ensure at least 1 page if totalItems > 0
        jLabelPageInfo.setText("Page " + currentPage + " of " + totalPages);
        jButtonPreviousPage.setEnabled(currentPage > 1);
        jButtonNextPage.setEnabled(currentPage < totalPages);
        if (totalAvailableItems <= 0) {
            jLabelPageInfo.setText("No available items found");
        }
    }

    private void gotoPreviousPage() {
        if (currentPage > 1) {
            currentPage--;
            refreshTableData(); // Refresh data for the new page
            updatePaginationControls(); // Update controls based on new page
        }
    }

    private void gotoNextPage() {
        int totalPages = (int) Math.ceil((double) totalAvailableItems / itemsPerPage);
         totalPages = Math.max(totalPages, 1);
        if (currentPage < totalPages) {
            currentPage++;
            refreshTableData(); // Refresh data for the new page
            updatePaginationControls(); // Update controls based on new page
        }
    }

    // Method to open the BorrowItemDialog
    private void openBorrowItemDialog() {
         if (selectedItemId == -1) {
            JOptionPane.showMessageDialog(this, "Please select request item.", "No Item Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Fetch the maximum available quantity and IsMachinery status for the selected item
        ItemDetailsForBorrow itemDetails = fetchItemDetailsForBorrow(selectedItemId);

         if (itemDetails == null || itemDetails.getQuantity() <= 0) {
             JOptionPane.showMessageDialog(this, "Selected item is no longer available or details could not be fetched.", "Item Unavailable", JOptionPane.WARNING_MESSAGE);
             // Refresh the list as it might be outdated
             fetchTotalAvailableItemCount(); // Fetch total count, which triggers refreshTableData
             selectedItemId = -1; // Deselect the item
             selectedItemName = null;
             return;
         }


        // Pass the current frame, modal status, connection, current user, selected item details, max quantity, IsMachinery status, and this listener
        BorrowItemDialog dialog = new BorrowItemDialog(
            (Frame) SwingUtilities.getWindowAncestor(this),
            true,
            conn,
            currentUser,
            selectedItemId,
            selectedItemName,
            itemDetails.getQuantity(), // Pass the fetched quantity
            itemDetails.isMachinery(), // Pass the fetched IsMachinery status
            this // Pass this KioskDashboard as the listener
        );
        dialog.setVisible(true);

        // After the dialog is closed (either by borrowing or cancelling),
        // the onBorrowComplete method (implemented from BorrowCompleteListener)
        // will be called to refresh the list.
    }

    // New method to fetch quantity and IsMachinery status
    private ItemDetailsForBorrow fetchItemDetailsForBorrow(int itemId) {
         if (conn == null) {
             System.err.println("Cannot fetch item details for borrow: DB connection is null.");
             return null;
         }
         // Assuming 'IsMachinery' is a BOOLEAN column in your Items table
         String sql = "SELECT Quantity, IsMachinery FROM Items WHERE ItemID = ? AND IsArchived = FALSE";
         try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
             pstmt.setInt(1, itemId);
             try (ResultSet rs = pstmt.executeQuery()) {
                 if (rs.next()) {
                     int quantity = rs.getInt("Quantity");
                     boolean isMachinery = rs.getBoolean("IsMachinery");
                     return new ItemDetailsForBorrow(quantity, isMachinery);
                 }
             }
         } catch (SQLException e) {
             System.err.println("Error fetching item details for ItemID " + itemId + ": " + e.getMessage());
             e.printStackTrace();
         }
         return null; // Return null if item not found or error occurs
    }

    // Helper class to return multiple values from fetchItemDetailsForBorrow
    private static class ItemDetailsForBorrow {
        private final int quantity;
        private final boolean isMachinery;

        public ItemDetailsForBorrow(int quantity, boolean isMachinery) {
            this.quantity = quantity;
            this.isMachinery = isMachinery;
        }

        public int getQuantity() {
            return quantity;
        }

        public boolean isMachinery() {
            return isMachinery;
        }
    }


    public void onBorrowComplete() {
        // This method is called by BorrowItemDialog after a successful borrow transaction
        System.out.println("Request operation completed. Refreshing Kiosk Dashboard.");
        selectedItemId = -1; // Deselect the item after borrowing
        selectedItemName = null;
        fetchTotalAvailableItemCount(); // Refresh the list of available items
    }
    
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 400, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 300, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
}
