
package modules;
import Package1.User; 
import Package1.DBConnection; 
import Package1.PasswordHasher; 
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Vector; 
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PiePlot;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.title.TextTitle; 
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;



public class Users extends javax.swing.JPanel {

    private User currentUser; 

  
    private javax.swing.JLabel jLabelPermission;
    private javax.swing.JLabel jLabelTitle;

   
    private JTable jTableUsers;
    private DefaultTableModel tableModelUsers;
    private JScrollPane jScrollPaneUsersTable;

    private javax.swing.JButton jButtonOpenCreateUserDialog;

    private JPanel chartsPanel;
    private DefaultPieDataset roleDistributionDataset;
    private DefaultCategoryDataset userCreationDataset;

   
    private int currentPage = 1;
    private final int itemsPerPage = 10; 
    private int totalUsers = 0;
    private JButton jButtonPreviousPage;
    private JButton jButtonNextPage;
    private JLabel jLabelPageInfo;
   

   
    private JTextField jTextFieldSearch;
    private JButton jButtonSearch;
    private String currentSearchText = ""; 


    public Users() {
       
        setupComponents();
        setCreateUserComponentsVisibility(false);
        updatePaginationControls();

    }

  
    private void setupComponents() {
      
        this.setLayout(new BorderLayout(10, 10));
        this.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); 
        this.setBackground(new Color(30, 30, 30)); 

     
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5)); 
        topPanel.setOpaque(false);

        jLabelTitle = new javax.swing.JLabel("User Management");
        jLabelTitle.setFont(new java.awt.Font("Segoe UI", 1, 18));
        jLabelTitle.setForeground(Color.WHITE); 
        topPanel.add(jLabelTitle);

        jLabelPermission = new javax.swing.JLabel("Loading permissions..."); 
        jLabelPermission.setFont(new java.awt.Font("Segoe UI", 0, 14));
        jLabelPermission.setForeground(new java.awt.Color(255, 0, 0)); 
        topPanel.add(jLabelPermission); 

        jButtonOpenCreateUserDialog = new javax.swing.JButton("Create New User");
        jButtonOpenCreateUserDialog.setFont(new Font("Segoe UI", Font.BOLD, 14));
        jButtonOpenCreateUserDialog.setBackground(new Color(46, 204, 113)); // Green color
        jButtonOpenCreateUserDialog.setForeground(Color.WHITE);
        jButtonOpenCreateUserDialog.setFocusPainted(false);

        jButtonOpenCreateUserDialog.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openCreateUserDialog();
            }
        });
        topPanel.add(jButtonOpenCreateUserDialog);


        this.add(topPanel, BorderLayout.NORTH); 


        JPanel centerPanel = new JPanel(new GridLayout(2, 1, 0, 10)); 
        centerPanel.setOpaque(false);

       
        chartsPanel = createChartsPanel(); 
        centerPanel.add(chartsPanel);

        JPanel userListPanel = new JPanel(new BorderLayout()); 
        userListPanel.setOpaque(false); 
        userListPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.GRAY),
                "Existing Users", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
                javax.swing.border.TitledBorder.DEFAULT_POSITION,
                new Font("Segoe UI", Font.BOLD, 14), Color.WHITE)); 


        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        searchPanel.setOpaque(false);

        JLabel searchLabel = new JLabel("Search:");
        searchLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        searchLabel.setForeground(Color.WHITE);
        searchPanel.add(searchLabel);

        jTextFieldSearch = new JTextField(20); 
        jTextFieldSearch.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        searchPanel.add(jTextFieldSearch);

        jButtonSearch = new JButton("Search"); 
        stylePaginationButton(jButtonSearch); 
        jButtonSearch.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                currentSearchText = jTextFieldSearch.getText().trim(); // Get search text
                currentPage = 1; 
                fetchTotalUserCount(); 
            }
        });
        searchPanel.add(jButtonSearch);

        userListPanel.add(searchPanel, BorderLayout.NORTH); 
  


        // Table Model and Table
        String[] columnNames = {"User ID", "Username", "Full Name", "Role", "Email", "Created By", "Created At", "Is Active"};
        tableModelUsers = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; 
            }
        };
        jTableUsers = new JTable(tableModelUsers);

        // Style the table
        jTableUsers.setForeground(Color.WHITE);
        jTableUsers.setBackground(new Color(50, 50, 50)); 
        jTableUsers.setGridColor(new Color(70, 70, 70)); 
        jTableUsers.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        jTableUsers.getTableHeader().setBackground(new Color(40, 40, 40)); 
        jTableUsers.getTableHeader().setForeground(Color.WHITE); 
        jTableUsers.setRowHeight(25); 
        jTableUsers.setSelectionBackground(new Color(70, 70, 70)); 
        jTableUsers.setSelectionForeground(Color.WHITE); 

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
        stylePaginationButton(jButtonPreviousPage); 
        jButtonPreviousPage.addActionListener(e -> gotoPreviousPage()); 
        paginationPanel.add(jButtonPreviousPage);

        jLabelPageInfo = new JLabel("Page 1 of 1"); 
        jLabelPageInfo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        jLabelPageInfo.setForeground(Color.WHITE);
        paginationPanel.add(jLabelPageInfo);

        jButtonNextPage = new JButton("Next");
        jButtonNextPage.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        stylePaginationButton(jButtonNextPage); 
        jButtonNextPage.addActionListener(e -> gotoNextPage()); 
        paginationPanel.add(jButtonNextPage);

        userListPanel.add(paginationPanel, BorderLayout.SOUTH); 
        // >>> End Pagination Controls Panel <<<
        centerPanel.add(userListPanel); 

        this.add(centerPanel, BorderLayout.CENTER); 

        
    }

    
    private JPanel createChartsPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 10, 0)); // 
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.GRAY),
                "User Analytics", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
                javax.swing.border.TitledBorder.DEFAULT_POSITION,
                new Font("Segoe UI", Font.BOLD, 14), Color.WHITE));

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
     
        BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setSeriesPaint(0, new Color(41, 128, 185)); 

        plot.getDomainAxis().setLabelPaint(Color.WHITE); 
        plot.getDomainAxis().setTickLabelPaint(Color.WHITE); 
        plot.getRangeAxis().setLabelPaint(Color.WHITE); 
        plot.getRangeAxis().setTickLabelPaint(Color.WHITE); 
        plot.setRangeGridlinePaint(new Color(70, 70, 70)); 


        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setOpaque(false);
        // chartPanel.setPreferredSize(new Dimension(300, 250)); // Preferred size
        return chartPanel;
    }

    
    private void customizeChart(JFreeChart chart) {
        chart.setBackgroundPaint(new Color(30, 30, 30)); 
        chart.getTitle().setPaint(Color.WHITE); 
        
         if (chart.getTitle() == null) {
            chart.setTitle(new TextTitle(chart.getTitle().getText(), new Font("Segoe UI", Font.BOLD, 16)));
            chart.getTitle().setPaint(Color.WHITE);
        } else {
             chart.getTitle().setPaint(Color.WHITE);
        }


        if (chart.getLegend() != null) {
            chart.getLegend().setBackgroundPaint(new Color(30, 30, 30)); 
            chart.getLegend().setItemPaint(Color.WHITE); 
        }
    }

    
    private void customizePlot(org.jfree.chart.plot.Plot plot) {
        plot.setBackgroundPaint(new Color(40, 40, 40));
        plot.setOutlinePaint(new Color(30, 30, 30)); 
    }


    /**
     * Opens the dialog for creating a new user.
     */
    private void openCreateUserDialog() {
       
        if (currentUser != null && currentUser.isAdmin()) {
          
            CreateUserDialog createUserDialog = new CreateUserDialog(
                (JFrame) SwingUtilities.getWindowAncestor(this), 
                true, 
                currentUser, 
                this 
            );
            createUserDialog.setLocationRelativeTo(this); 
            createUserDialog.setVisible(true);

             fetchTotalUserCount(); 

        } else {
            JOptionPane.showMessageDialog(this, "You do not have permission to create users.", "Permission Denied", JOptionPane.ERROR_MESSAGE);
        }
    }


   
    public void setCurrentUserId(User user) {
        this.currentUser = user; 

    
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                           if (jLabelPermission != null && jButtonOpenCreateUserDialog != null) {
                    
                     if (currentUser != null && currentUser.isAdmin()) {
                      
                        jLabelPermission.setVisible(false);
                        jButtonOpenCreateUserDialog.setVisible(true);
                     } else {
                     
                        jLabelPermission.setText("You do not have permission to create users.");
                        jLabelPermission.setVisible(true);
                        jButtonOpenCreateUserDialog.setVisible(false);
                     }
                } else {
                    System.err.println("jLabelPermission or jButtonOpenCreateUserDialog is null after invokeLater in setCurrentUserId!");
                    
                }
            }
        });

        currentPage = 1;
        currentSearchText = "";
        if (jTextFieldSearch != null) {
             SwingUtilities.invokeLater(() -> jTextFieldSearch.setText("")); 
        }
        fetchTotalUserCount(); 

        
        fetchAndDisplayCharts();
    }

   
    private void setCreateUserComponentsVisibility(boolean visible) {
        // Ensure the button is not null before setting visibility
        if (jButtonOpenCreateUserDialog != null) {
            jButtonOpenCreateUserDialog.setVisible(visible);
        }

        // Repaint and revalidate the panel to reflect component visibility changes
        this.revalidate();
        this.repaint();
    }


  
    private void jButtonCreateUserActionPerformed(java.awt.event.ActionEvent evt) {
      
        System.out.println("jButtonCreateUserActionPerformed called on Users panel (should be in dialog)");
    }

    // >>> Pagination and Search Methods <<<

    /**
     * Fetches the total number of users, applying the current search filter.
     */
    private void fetchTotalUserCount() {
      
         String sql = "SELECT COUNT(*) AS total FROM Users";
         StringBuilder whereClause = new StringBuilder();

       
         if (currentSearchText != null && !currentSearchText.isEmpty()) {
             whereClause.append(" WHERE Username LIKE ? OR FullName LIKE ? OR Email LIKE ? OR Role LIKE ?");
         }

         
         final String finalSql = sql + whereClause.toString();


        new Thread(new Runnable() {
            @Override
            public void run() {
                int count = 0;
                 try (Connection conn = DBConnection.getConnection();
                     
                     PreparedStatement pstmt = conn.prepareStatement(finalSql)) { 

                   
                    if (currentSearchText != null && !currentSearchText.isEmpty()) {
                        String searchTerm = "%" + currentSearchText.toLowerCase() + "%";
                        pstmt.setString(1, searchTerm); 
                        pstmt.setString(2, searchTerm); 
                        pstmt.setString(3, searchTerm); 
                        pstmt.setString(4, searchTerm); 
                    }


                    try (ResultSet rs = pstmt.executeQuery()) {
                        if (rs.next()) {
                            count = rs.getInt("total");
                        }
                    }

                } catch (SQLException e) {
                    e.printStackTrace();
                    System.err.println("Error fetching total user count: " + e.getMessage());
                   
                }

                final int finalCount = count;
                 SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        totalUsers = finalCount;
                       
                        int totalPagesBefore = (int) Math.ceil((double) (totalUsers - (totalUsers % itemsPerPage)) / itemsPerPage);
                        int totalPagesAfter = (int) Math.ceil((double) totalUsers / itemsPerPage);

                       
                        if (currentPage > totalPagesAfter && totalPagesAfter > 0) {
                            currentPage = totalPagesAfter;
                        } else if (totalPagesAfter == 0 && totalUsers > 0) {
                             
                             currentPage = 1;
                        } else if (totalPagesAfter == 0 && totalUsers == 0) {
                             currentPage = 1; 
                        }


                        updatePaginationControls(); 
                        fetchAndDisplayUsers(); 
                    }
                });
            }
        }).start();
    }


 
    public void fetchAndDisplayUsers() { 
       
        int offset = (currentPage - 1) * itemsPerPage;

        String sql = "SELECT UserID, Username, FullName, Email, Role, CreatedBy, CreatedAt, IsActive FROM Users";
        StringBuilder whereClause = new StringBuilder();

        if (currentSearchText != null && !currentSearchText.isEmpty()) {
            whereClause.append(" WHERE Username LIKE ? OR FullName LIKE ? OR Email LIKE ? OR Role LIKE ?");
        }

        final String finalSql = sql + whereClause.toString() + " ORDER BY UserID ASC LIMIT ? OFFSET ?"; // Make sql final after building


      
        new Thread(new Runnable() {
            @Override
            public void run() {
                Vector<Vector<Object>> data = new Vector<>(); 

                try (Connection conn = DBConnection.getConnection();
                  
                     PreparedStatement pstmt = conn.prepareStatement(finalSql)) { 

                    int paramIndex = 1;
                  
                    if (currentSearchText != null && !currentSearchText.isEmpty()) {
                        String searchTerm = "%" + currentSearchText.toLowerCase() + "%";
                        pstmt.setString(paramIndex++, searchTerm); 
                        pstmt.setString(paramIndex++, searchTerm);
                        pstmt.setString(paramIndex++, searchTerm); 
                        pstmt.setString(paramIndex++, searchTerm); 
                    }

                    pstmt.setInt(paramIndex++, itemsPerPage); 
                    pstmt.setInt(paramIndex++, offset); 

                    try (ResultSet rs = pstmt.executeQuery()) {
                        while (rs.next()) {
                            Vector<Object> row = new Vector<>();
                            row.add(rs.getInt("UserID"));
                            row.add(rs.getString("Username"));
                            row.add(rs.getString("FullName"));
                            row.add(rs.getString("Email"));
                            row.add(rs.getString("Role"));
                            int createdById = rs.getInt("CreatedBy");
                            String createdByUsername = (createdById > 0) ? fetchUsername(createdById) : "System";
                            row.add(createdByUsername);
                            row.add(rs.getTimestamp("CreatedAt"));
                            row.add(rs.getBoolean("IsActive"));
                            data.add(row); 
                        }
                    }

                } catch (SQLException e) {
                    e.printStackTrace();
                   
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
                     return; 
                }

                // Update the table model on the EDT after fetching all data
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                         if (tableModelUsers != null) {
                            tableModelUsers.setRowCount(0); 
                            for (Vector<Object> row : data) {
                                tableModelUsers.addRow(row); 
                            }
                           
                         } else {
                             System.err.println("tableModelUsers is null when trying to update table!");
                         }
                    }
                });
            }
        }).start(); 
    }

    /**
     * Updates the state of the pagination buttons and the page info label.
     */
    private void updatePaginationControls() {
        int totalPages = (int) Math.ceil((double) totalUsers / itemsPerPage);
        jLabelPageInfo.setText("Page " + currentPage + " of " + totalPages);

        jButtonPreviousPage.setEnabled(currentPage > 1);
        jButtonNextPage.setEnabled(currentPage < totalPages);

    
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
            fetchAndDisplayUsers(); 
            updatePaginationControls(); 
        }
    }

    /**
     * Goes to the next page of user data.
     */
    private void gotoNextPage() {
        int totalPages = (int) Math.ceil((double) totalUsers / itemsPerPage);
        if (currentPage < totalPages) {
            currentPage++;
            fetchAndDisplayUsers(); 
            updatePaginationControls(); 
        }
    }

    /**
     * Helper method to style pagination/search buttons for dark theme.
     */
    private void stylePaginationButton(JButton button) {
        button.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        button.setBackground(new Color(70, 70, 70)); 
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.GRAY, 1),
            BorderFactory.createEmptyBorder(3, 8, 3, 8)
        ));
    }

    // >>> End Pagination and Search Methods <<<


    private String fetchUsername(int userId) {
        String fullName = "Unknown User"; 

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
            fullName = "Error Fetching Name"; 
        }

        return fullName;
    }


    /**
     * Fetches data for the charts and updates the chart datasets.
     **/
    void fetchAndDisplayCharts() {

        new Thread(new Runnable() {
            @Override
            public void run() {
              
                fetchRoleDistributionData();

                fetchUserCreationData();

            }
        }).start(); 
    }

    /**
     * Fetches data for the User Role Distribution Pie Chart.
     */
    private void fetchRoleDistributionData() {

        if (roleDistributionDataset == null) {
            System.err.println("roleDistributionDataset is null in fetchRoleDistributionData!");
            return;
        }
     
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
       
        if (userCreationDataset == null) {
             System.err.println("userCreationDataset is null in fetchUserCreationData!");
             return;
        }
     
         SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                userCreationDataset.clear();
            }
        });


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
