/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JPanel.java to edit this template
 */
package modules;

import Package1.User; // Assuming User is in Package1
// Adjust these import paths to match your actual package structure
import Package1.DBConnection; // Using DBConnection as per your provided code
import Package1.PasswordHasher; // Using PasswordHasher as per your provided code


import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Vector; // Import Vector for table model data

// Imports for Charts (assuming JFreeChart is available)
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PiePlot;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.title.TextTitle; // For chart titles

// Added imports for search
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


/**
 * JPanel for managing users, allowing Admin users to create new accounts
 * via a pop-up dialog and view existing users and user-related charts.
 * This version includes pagination and search for the user table.
 *
 * @author Rei (and modified for user creation dialog, list, and charts)
 */
public class Users extends javax.swing.JPanel {

    private User currentUser; // To hold the logged-in user's information

    // Declare Swing components as instance variables
    private javax.swing.JLabel jLabelPermission;
    private javax.swing.JLabel jLabelTitle;

    // Components for the user list table
    private JTable jTableUsers;
    private DefaultTableModel tableModelUsers;
    private JScrollPane jScrollPaneUsersTable;

    // Button to open the create user dialog
    private javax.swing.JButton jButtonOpenCreateUserDialog;

    // Components for charts
    private JPanel chartsPanel;
    private DefaultPieDataset roleDistributionDataset;
    private DefaultCategoryDataset userCreationDataset;

    // >>> Pagination Variables <<<
    private int currentPage = 1;
    private final int itemsPerPage = 10; // You can adjust this value
    private int totalUsers = 0;
    private JButton jButtonPreviousPage;
    private JButton jButtonNextPage;
    private JLabel jLabelPageInfo;
    // >>> End Pagination Variables <<<

    // >>> Search Variables and Components <<<
    private JTextField jTextFieldSearch;
    private JButton jButtonSearch;
    private String currentSearchText = ""; // To store the current search query
    // >>> End Search Variables and Components <<<


    /**
     * Creates new form Users
     */
    public Users() {
        // Call the method to manually create and add components
        setupComponents();
        // Initially hide or disable the create user button.
        // It will be shown/enabled in setCurrentUserId if the user is an Admin.
        setCreateUserComponentsVisibility(false); // This now controls the button visibility

        // Pagination components are initially added but buttons are disabled until total count is known
        updatePaginationControls();

        // Data fetching and chart display will happen after the user is set via setCurrentUserId
    }

    /**
     * Manually creates and adds the Swing components to the panel.
     * Sets up the layout for the create user button, the user list table, and charts.
     * This method is modified to include pagination and search controls.
     */
    private void setupComponents() {
        // Use BorderLayout for the main panel
        this.setLayout(new BorderLayout(10, 10));
        this.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // Add some padding
        this.setBackground(new Color(30, 30, 30)); // Set dark background

        // --- Top Panel (Title, Permission Message, Create Button) ---
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5)); // FlowLayout for top elements
        topPanel.setOpaque(false);

        jLabelTitle = new javax.swing.JLabel("User Management");
        jLabelTitle.setFont(new java.awt.Font("Segoe UI", 1, 18));
        jLabelTitle.setForeground(Color.WHITE); // Set text color
        topPanel.add(jLabelTitle);

        jLabelPermission = new javax.swing.JLabel("Loading permissions..."); // Initial text
        jLabelPermission.setFont(new java.awt.Font("Segoe UI", 0, 14));
        jLabelPermission.setForeground(new java.awt.Color(255, 0, 0)); // Red color for warning
        topPanel.add(jLabelPermission); // Add permission message to the top panel

        jButtonOpenCreateUserDialog = new javax.swing.JButton("Create New User");
        jButtonOpenCreateUserDialog.setFont(new Font("Segoe UI", Font.BOLD, 14));
        jButtonOpenCreateUserDialog.setBackground(new Color(46, 204, 113)); // Green color
        jButtonOpenCreateUserDialog.setForeground(Color.WHITE);
        jButtonOpenCreateUserDialog.setFocusPainted(false);
        // Add action listener to open the dialog (implementation below)
        jButtonOpenCreateUserDialog.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openCreateUserDialog();
            }
        });
        topPanel.add(jButtonOpenCreateUserDialog); // Add button to the top panel


        this.add(topPanel, BorderLayout.NORTH); // Add the top panel to the main panel's NORTH


        // --- Center Panel (Charts and Table) ---
        JPanel centerPanel = new JPanel(new GridLayout(2, 1, 0, 10)); // Use GridLayout for charts and table
        centerPanel.setOpaque(false);

        // Charts Panel
        chartsPanel = createChartsPanel(); // Method to create the charts panel
        centerPanel.add(chartsPanel);


        // User List Table Panel (Modified to include search and pagination controls)
        JPanel userListPanel = new JPanel(new BorderLayout()); // Use BorderLayout for table, search, and pagination
        userListPanel.setOpaque(false); // Make panel transparent
        userListPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.GRAY),
                "Existing Users", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
                javax.swing.border.TitledBorder.DEFAULT_POSITION,
                new Font("Segoe UI", Font.BOLD, 14), Color.WHITE)); // Add a titled border

        // >>> Search Panel <<<
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        searchPanel.setOpaque(false);

        JLabel searchLabel = new JLabel("Search:");
        searchLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        searchLabel.setForeground(Color.WHITE);
        searchPanel.add(searchLabel);

        jTextFieldSearch = new JTextField(20); // Text field for search input
        jTextFieldSearch.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        searchPanel.add(jTextFieldSearch);

        jButtonSearch = new JButton("Search"); // Button to trigger search
        stylePaginationButton(jButtonSearch); // Apply styling
        jButtonSearch.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                currentSearchText = jTextFieldSearch.getText().trim(); // Get search text
                currentPage = 1; // Reset to first page on search
                fetchTotalUserCount(); // Fetch total count with filter and then display data
            }
        });
        searchPanel.add(jButtonSearch);

        userListPanel.add(searchPanel, BorderLayout.NORTH); // Add search panel to the top of userListPanel
        // >>> End Search Panel <<<


        // Table Model and Table
        String[] columnNames = {"User ID", "Username", "Full Name", "Role", "Email", "Created By", "Created At", "Is Active"};
        tableModelUsers = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Make table non-editable
            }
        };
        jTableUsers = new JTable(tableModelUsers);

        // Style the table
        jTableUsers.setForeground(Color.WHITE);
        jTableUsers.setBackground(new Color(50, 50, 50)); // Darker background for table
        jTableUsers.setGridColor(new Color(70, 70, 70)); // Grid line color
        jTableUsers.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        jTableUsers.getTableHeader().setBackground(new Color(40, 40, 40)); // Header background
        jTableUsers.getTableHeader().setForeground(Color.WHITE); // Header text color
        jTableUsers.setRowHeight(25); // Increase row height for better readability
        jTableUsers.setSelectionBackground(new Color(70, 70, 70)); // Selection background
        jTableUsers.setSelectionForeground(Color.WHITE); // Selection text color

        // Add table to a scroll pane
        jScrollPaneUsersTable = new JScrollPane(jTableUsers);
        jScrollPaneUsersTable.getViewport().setBackground(new Color(50, 50, 50)); // Match viewport background
        jScrollPaneUsersTable.setBorder(BorderFactory.createLineBorder(Color.GRAY)); // Add border to scroll pane

        userListPanel.add(jScrollPaneUsersTable, BorderLayout.CENTER); // Add table to the center of userListPanel

        // >>> Pagination Controls Panel <<<
        JPanel paginationPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        paginationPanel.setOpaque(false);

        jButtonPreviousPage = new JButton("Previous");
        jButtonPreviousPage.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        stylePaginationButton(jButtonPreviousPage); // Apply styling
        jButtonPreviousPage.addActionListener(e -> gotoPreviousPage()); // Add action listener
        paginationPanel.add(jButtonPreviousPage);

        jLabelPageInfo = new JLabel("Page 1 of 1"); // Initial text
        jLabelPageInfo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        jLabelPageInfo.setForeground(Color.WHITE);
        paginationPanel.add(jLabelPageInfo);

        jButtonNextPage = new JButton("Next");
        jButtonNextPage.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        stylePaginationButton(jButtonNextPage); // Apply styling
        jButtonNextPage.addActionListener(e -> gotoNextPage()); // Add action listener
        paginationPanel.add(jButtonNextPage);

        userListPanel.add(paginationPanel, BorderLayout.SOUTH); // Add pagination controls to the bottom
        // >>> End Pagination Controls Panel <<<


        centerPanel.add(userListPanel); // Add the user list panel (now with search and pagination) to the center panel

        this.add(centerPanel, BorderLayout.CENTER); // Add the center panel to the main panel's CENTER

        // The permission message is now part of the topPanel and its visibility is controlled there.
        // The create user form components are no longer on this panel.
    }

    /**
     * Creates the panel containing user-related charts.
     * @return The JPanel containing the charts.
     */
    private JPanel createChartsPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 10, 0)); // Two charts side-by-side
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.GRAY),
                "User Analytics", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
                javax.swing.border.TitledBorder.DEFAULT_POSITION,
                new Font("Segoe UI", Font.BOLD, 14), Color.WHITE)); // Titled border

        // Chart 1: User Role Distribution (Pie Chart)
        roleDistributionDataset = new DefaultPieDataset();
        ChartPanel roleChartPanel = createPieChart("User Role Distribution", roleDistributionDataset);
        panel.add(roleChartPanel);

        // Chart 2: User Creation Over Time (Bar Chart - Placeholder)
        userCreationDataset = new DefaultCategoryDataset();
        ChartPanel creationChartPanel = createBarChart("User Creation Over Time", "Month", "Number of Users", userCreationDataset);
        panel.add(creationChartPanel);


        return panel;
    }

     /**
     * Helper method to create a Pie Chart.
     * @param title Chart title.
     * @param dataset The dataset for the chart.
     * @return A ChartPanel containing the pie chart.
     */
    private ChartPanel createPieChart(String title, DefaultPieDataset dataset) {
        JFreeChart chart = ChartFactory.createPieChart(
                title,
                dataset,
                true, // include legend
                true, // tooltips
                false // urls
        );
        customizeChart(chart);
        PiePlot plot = (PiePlot) chart.getPlot();
        customizePlot(plot);
        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setOpaque(false);
        // chartPanel.setPreferredSize(new Dimension(300, 250)); // Preferred size
        return chartPanel;
    }

    /**
     * Helper method to create a Bar Chart.
     * @param title Chart title.
     * @param categoryAxisLabel Label for the category axis.
     * @param valueAxisLabel Label for the value axis.
     * @param dataset The dataset for the chart.
     * @return A ChartPanel containing the bar chart.
     */
     private ChartPanel createBarChart(String title, String categoryAxisLabel, String valueAxisLabel, DefaultCategoryDataset dataset) {
        JFreeChart chart = ChartFactory.createBarChart(
                title,
                categoryAxisLabel,
                valueAxisLabel,
                dataset,
                PlotOrientation.VERTICAL,
                true, // include legend
                true, // tooltips
                false // urls
        );
        customizeChart(chart);
        CategoryPlot plot = (CategoryPlot) chart.getPlot();
        customizePlot(plot);
        // Customize bar renderer colors if needed
        BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setSeriesPaint(0, new Color(41, 128, 185)); // Example color

        plot.getDomainAxis().setLabelPaint(Color.WHITE); // Axis label color
        plot.getDomainAxis().setTickLabelPaint(Color.WHITE); // Axis tick label color
        plot.getRangeAxis().setLabelPaint(Color.WHITE); // Axis label color
        plot.getRangeAxis().setTickLabelPaint(Color.WHITE); // Axis tick label color
        plot.setRangeGridlinePaint(new Color(70, 70, 70)); // Grid line color


        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setOpaque(false);
        // chartPanel.setPreferredSize(new Dimension(300, 250)); // Preferred size
        return chartPanel;
    }

    /**
     * Helper method to customize common chart properties for dark theme.
     * @param chart The JFreeChart to customize.
     */
    private void customizeChart(JFreeChart chart) {
        chart.setBackgroundPaint(new Color(30, 30, 30)); // Chart background
        chart.getTitle().setPaint(Color.WHITE); // Title color
        // Ensure the title is added if it's not automatically
         if (chart.getTitle() == null) {
            chart.setTitle(new TextTitle(chart.getTitle().getText(), new Font("Segoe UI", Font.BOLD, 16)));
            chart.getTitle().setPaint(Color.WHITE);
        } else {
             chart.getTitle().setPaint(Color.WHITE);
        }


        if (chart.getLegend() != null) {
            chart.getLegend().setBackgroundPaint(new Color(30, 30, 30)); // Legend background
            chart.getLegend().setItemPaint(Color.WHITE); // Legend item color
        }
    }

    /**
     * Helper method to customize common plot properties for dark theme.
     * @param plot The Plot to customize.
     */
    private void customizePlot(org.jfree.chart.plot.Plot plot) {
        plot.setBackgroundPaint(new Color(40, 40, 40)); // Plot background
        plot.setOutlinePaint(new Color(30, 30, 30)); // Plot outline
    }


    /**
     * Opens the dialog for creating a new user.
     */
    private void openCreateUserDialog() {
        // Ensure only Admins can open the dialog
        if (currentUser != null && currentUser.isAdmin()) {
            // Pass the current user and a reference to this panel to the dialog
            CreateUserDialog createUserDialog = new CreateUserDialog(
                (JFrame) SwingUtilities.getWindowAncestor(this), // Get parent frame
                true, // Modal dialog
                currentUser, // Pass the logged-in user
                this // Pass a reference to this Users panel
            );
            createUserDialog.setLocationRelativeTo(this); // Center dialog relative to Users panel
            createUserDialog.setVisible(true);

            // After the dialog is closed (and if a user was created), refresh the table
            // The dialog should call fetchAndDisplayUsers() on this panel upon success.
             fetchTotalUserCount(); // Refresh data after dialog close by refetching total count and then data

        } else {
            JOptionPane.showMessageDialog(this, "You do not have permission to create users.", "Permission Denied", JOptionPane.ERROR_MESSAGE);
        }
    }


    /**
     * Sets the current logged-in user for this panel.
     * Based on the user's role, enables/disables create user functionality.
     * This method uses SwingUtilities.invokeLater to ensure GUI updates
     * happen safely on the Event Dispatch Thread.
     *
     * @param user The logged-in User object.
     */
    public void setCurrentUserId(User user) {
        this.currentUser = user; // Assign to currentUser instance variable

        // Defer GUI updates to the Swing event dispatch thread
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                // Now, jLabelPermission and jButtonOpenCreateUserDialog should be initialized if setupComponents ran
                if (jLabelPermission != null && jButtonOpenCreateUserDialog != null) {
                     // Use currentUser for the check
                     if (currentUser != null && currentUser.isAdmin()) {
                        // If the user is an Admin, hide permission message and show create button
                        jLabelPermission.setVisible(false);
                        jButtonOpenCreateUserDialog.setVisible(true);
                     } else {
                        // If not an Admin, show permission message and hide create button
                        jLabelPermission.setText("You do not have permission to create users.");
                        jLabelPermission.setVisible(true);
                        jButtonOpenCreateUserDialog.setVisible(false);
                     }
                } else {
                    System.err.println("jLabelPermission or jButtonOpenCreateUserDialog is null after invokeLater in setCurrentUserId!");
                    // Log a severe error if components are still null
                }
            }
        });

        // Fetch and display users whenever the current user is set (e.g., after login)
        // Start from the first page when the user is set
        currentPage = 1; // Reset to first page
        currentSearchText = ""; // Clear search text on user set
        if (jTextFieldSearch != null) {
             SwingUtilities.invokeLater(() -> jTextFieldSearch.setText("")); // Clear search field on EDT
        }
        fetchTotalUserCount(); // Fetch total count first and then display data

        // Also refresh charts when user is set (if user role affects chart data)
        fetchAndDisplayCharts();
    }

     /**
      * Controls the visibility of the components related to user creation.
      * In this version, it primarily controls the visibility of the "Create New User" button.
      * @param visible true to make components visible, false to hide them.
      */
    private void setCreateUserComponentsVisibility(boolean visible) {
        // Ensure the button is not null before setting visibility
        if (jButtonOpenCreateUserDialog != null) {
            jButtonOpenCreateUserDialog.setVisible(visible);
        }

        // Repaint and revalidate the panel to reflect component visibility changes
        this.revalidate();
        this.repaint();
    }


    /**
     * Action performed when the "Create User" button in the DIALOG is clicked.
     * This method is now part of the CreateUserDialog class.
     * This method remains here as a placeholder or if you need to trigger
     * something on the Users panel from the dialog.
     * The actual database insertion logic is in CreateUserDialog.
     */
    private void jButtonCreateUserActionPerformed(java.awt.event.ActionEvent evt) {
        // This method is now handled by the CreateUserDialog class.
        // You can remove this method or keep it if you need to add
        // any specific actions on the Users panel when the dialog's
        // create button is clicked (e.g., showing a temporary message).
        System.out.println("jButtonCreateUserActionPerformed called on Users panel (should be in dialog)");
    }

    // >>> Pagination and Search Methods <<<

    /**
     * Fetches the total number of users, applying the current search filter.
     */
    private void fetchTotalUserCount() {
         // Construct the base SQL query
         String sql = "SELECT COUNT(*) AS total FROM Users";
         StringBuilder whereClause = new StringBuilder();

         // Add search condition if search text is not empty
         if (currentSearchText != null && !currentSearchText.isEmpty()) {
             whereClause.append(" WHERE Username LIKE ? OR FullName LIKE ? OR Email LIKE ? OR Role LIKE ?");
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

                    // Set search parameters if needed
                    if (currentSearchText != null && !currentSearchText.isEmpty()) {
                        String searchTerm = "%" + currentSearchText.toLowerCase() + "%";
                        pstmt.setString(1, searchTerm); // Search in Username
                        pstmt.setString(2, searchTerm); // Search in FullName
                        pstmt.setString(3, searchTerm); // Search in Email
                        pstmt.setString(4, searchTerm); // Search in Role
                    }


                    try (ResultSet rs = pstmt.executeQuery()) {
                        if (rs.next()) {
                            count = rs.getInt("total");
                        }
                    }

                } catch (SQLException e) {
                    e.printStackTrace();
                    System.err.println("Error fetching total user count: " + e.getMessage());
                    // Handle error appropriately, maybe set totalUsers to -1 to indicate error
                }

                final int finalCount = count;
                 SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        totalUsers = finalCount;
                        // Recalculate total pages based on the new total count
                        int totalPagesBefore = (int) Math.ceil((double) (totalUsers - (totalUsers % itemsPerPage)) / itemsPerPage);
                        int totalPagesAfter = (int) Math.ceil((double) totalUsers / itemsPerPage);

                        // Adjust current page if it exceeds the new total pages
                        if (currentPage > totalPagesAfter && totalPagesAfter > 0) {
                            currentPage = totalPagesAfter;
                        } else if (totalPagesAfter == 0 && totalUsers > 0) {
                             // This case should ideally not happen if totalUsers > 0
                             currentPage = 1;
                        } else if (totalPagesAfter == 0 && totalUsers == 0) {
                             currentPage = 1; // Still show page 1 even if no users
                        }


                        updatePaginationControls(); // Update controls based on total count and current page
                        fetchAndDisplayUsers(); // Fetch and display data for the current page with filter
                    }
                });
            }
        }).start();
    }


    /**
     * Fetches user data from the database for the current page, applying the current search filter,
     * and populates the user list table.
     */
    public void fetchAndDisplayUsers() { // Made public so dialog can call it
        // Calculate offset
        int offset = (currentPage - 1) * itemsPerPage;

        // Base SQL query
        String sql = "SELECT UserID, Username, FullName, Email, Role, CreatedBy, CreatedAt, IsActive FROM Users";
        StringBuilder whereClause = new StringBuilder();

        // Add search condition if search text is not empty
        if (currentSearchText != null && !currentSearchText.isEmpty()) {
            whereClause.append(" WHERE Username LIKE ? OR FullName LIKE ? OR Email LIKE ? OR Role LIKE ?");
        }

        final String finalSql = sql + whereClause.toString() + " ORDER BY UserID ASC LIMIT ? OFFSET ?"; // Make sql final after building


        // Run database operation in a separate thread to avoid blocking the EDT
        new Thread(new Runnable() {
            @Override
            public void run() {
                Vector<Vector<Object>> data = new Vector<>(); // Use a local vector to store data

                try (Connection conn = DBConnection.getConnection();
                     // Prepare the statement with the complete SQL query
                     PreparedStatement pstmt = conn.prepareStatement(finalSql)) { // Use finalSql

                    int paramIndex = 1;
                    // Set search parameters if needed
                    if (currentSearchText != null && !currentSearchText.isEmpty()) {
                        String searchTerm = "%" + currentSearchText.toLowerCase() + "%";
                        pstmt.setString(paramIndex++, searchTerm); // Search in Username
                        pstmt.setString(paramIndex++, searchTerm); // Search in FullName
                        pstmt.setString(paramIndex++, searchTerm); // Search in Email
                        pstmt.setString(paramIndex++, searchTerm); // Search in Role
                    }

                    pstmt.setInt(paramIndex++, itemsPerPage); // Set LIMIT parameter
                    pstmt.setInt(paramIndex++, offset); // Set OFFSET parameter

                    try (ResultSet rs = pstmt.executeQuery()) {
                        while (rs.next()) {
                            Vector<Object> row = new Vector<>();
                            row.add(rs.getInt("UserID"));
                            row.add(rs.getString("Username"));
                            row.add(rs.getString("FullName"));
                            row.add(rs.getString("Email"));
                            row.add(rs.getString("Role"));
                            // Fetch creator's username instead of ID for better display
                            int createdById = rs.getInt("CreatedBy");
                            String createdByUsername = (createdById > 0) ? fetchUsername(createdById) : "System";
                            row.add(createdByUsername);
                            row.add(rs.getTimestamp("CreatedAt"));
                            row.add(rs.getBoolean("IsActive"));
                            data.add(row); // Add row to the local vector
                        }
                    }

                } catch (SQLException e) {
                    e.printStackTrace();
                    // Show error message on the EDT
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                             JOptionPane.showMessageDialog(Users.this, "Error fetching user data: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
                             // Clear table on error
                             if (tableModelUsers != null) {
                                tableModelUsers.setRowCount(0);
                             }
                        }
                    });
                     return; // Exit the thread's run method on error
                }

                // Update the table model on the EDT after fetching all data
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                         if (tableModelUsers != null) {
                            tableModelUsers.setRowCount(0); // Clear existing data
                            for (Vector<Object> row : data) {
                                tableModelUsers.addRow(row); // Add all fetched data
                            }
                            // updatePaginationControls(); // Update controls after data is loaded
                            // Note: updatePaginationControls is called after fetchTotalUserCount now
                         } else {
                             System.err.println("tableModelUsers is null when trying to update table!");
                         }
                    }
                });
            }
        }).start(); // Start the new thread
    }

    /**
     * Updates the state of the pagination buttons and the page info label.
     */
    private void updatePaginationControls() {
        int totalPages = (int) Math.ceil((double) totalUsers / itemsPerPage);
        jLabelPageInfo.setText("Page " + currentPage + " of " + totalPages);

        jButtonPreviousPage.setEnabled(currentPage > 1);
        jButtonNextPage.setEnabled(currentPage < totalPages);

        // If there are no users (after filtering), disable both buttons
        if (totalUsers <= 0) {
            jButtonPreviousPage.setEnabled(false);
            jButtonNextPage.setEnabled(false);
            jLabelPageInfo.setText("No users found");
        }
    }

    /**
     * Goes to the previous page of user data.
     */
    private void gotoPreviousPage() {
        if (currentPage > 1) {
            currentPage--;
            fetchAndDisplayUsers(); // Fetch data for the new page
            updatePaginationControls(); // Update buttons
        }
    }

    /**
     * Goes to the next page of user data.
     */
    private void gotoNextPage() {
        int totalPages = (int) Math.ceil((double) totalUsers / itemsPerPage);
        if (currentPage < totalPages) {
            currentPage++;
            fetchAndDisplayUsers(); // Fetch data for the new page
            updatePaginationControls(); // Update buttons
        }
    }

    /**
     * Helper method to style pagination/search buttons for dark theme.
     */
    private void stylePaginationButton(JButton button) {
        button.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        button.setBackground(new Color(70, 70, 70)); // Slightly lighter gray
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.GRAY, 1),
            BorderFactory.createEmptyBorder(3, 8, 3, 8) // Padding
        ));
    }

    // >>> End Pagination and Search Methods <<<


     /**
      * Fetches the full name of a user given their UserID.
      * @param userId The ID of the user.
      * @return The full name of the user, or "Unknown User" if not found.
      */
    private String fetchUsername(int userId) {
        String fullName = "Unknown User"; // Default value

        String sql = "SELECT FullName FROM Users WHERE UserID = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    fullName = rs.getString("FullName");
                }
            }

        } catch (SQLException e) {
            System.err.println("Error fetching full name for user ID " + userId + ": " + e.getMessage());
            e.printStackTrace();
            fullName = "Error Fetching Name"; // Indicate an error occurred
        }

        return fullName;
    }


    /**
     * Fetches data for the charts and updates the chart datasets.
     */
    void fetchAndDisplayCharts() {
        // Run database operations for charts in a separate thread
        new Thread(new Runnable() {
            @Override
            public void run() {
                // Fetch data for Role Distribution Pie Chart
                fetchRoleDistributionData();

                // Fetch data for User Creation Over Time Bar Chart (Placeholder implementation)
                fetchUserCreationData();

                // Chart panels will automatically update when their datasets change
            }
        }).start(); // Start the new thread
    }

    /**
     * Fetches data for the User Role Distribution Pie Chart.
     */
    private void fetchRoleDistributionData() {
        // Ensure dataset is initialized before clearing
        if (roleDistributionDataset == null) {
            System.err.println("roleDistributionDataset is null in fetchRoleDistributionData!");
            return;
        }
        // Clear dataset on the EDT before adding new data
         SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                roleDistributionDataset.clear();
            }
        });


        String sql = "SELECT Role, COUNT(*) AS RoleCount FROM Users GROUP BY Role";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                final String role = rs.getString("Role");
                final double roleCount = rs.getDouble("RoleCount");
                // Add value to dataset on the EDT
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        roleDistributionDataset.setValue(role, roleCount);
                    }
                });
            }

        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Error fetching role distribution data: " + e.getMessage());
            // Add an error entry to the chart on EDT
             SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                     roleDistributionDataset.setValue("Error", 1);
                }
            });
        }
    }

     /**
      * Fetches data for the User Creation Over Time Bar Chart.
      * (Placeholder implementation - you'll need to adjust the SQL based on your needs)
      */
    private void fetchUserCreationData() {
         // Ensure dataset is initialized before clearing
        if (userCreationDataset == null) {
             System.err.println("userCreationDataset is null in fetchUserCreationData!");
             return;
        }
        // Clear dataset on the EDT before adding new data
         SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                userCreationDataset.clear();
            }
        });


        // Example SQL: Count users created per month for the last 12 months
        // Adjust this query based on how you want to represent "creation over time"
        String sql = "SELECT DATE_FORMAT(CreatedAt, '%Y-%m') AS CreationMonth, COUNT(*) AS UserCount " +
                     "FROM Users " +
                     "WHERE CreatedAt >= DATE_SUB(CURDATE(), INTERVAL 12 MONTH) " +
                     "GROUP BY CreationMonth " +
                     "ORDER BY CreationMonth";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                final String month = rs.getString("CreationMonth");
                final double userCount = rs.getDouble("UserCount");
                 // Add value to dataset on the EDT
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        userCreationDataset.addValue(userCount, "Users Created", month);
                    }
                });
            }

        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Error fetching user creation data: " + e.getMessage());
             // Add an error entry to the chart on EDT
             SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    userCreationDataset.addValue(0, "Users Created", "Error");
                }
            });
        }
    }
    
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 400, Short.MAX_VALUE)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 300, Short.MAX_VALUE)
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
    // End of variables declaration//GEN-END:variables
}
