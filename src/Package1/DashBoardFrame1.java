package Package1;

import modules.Inventory;
import modules.Dashboard;
import modules.Users; // Ensure this import is correct
import modules.PurchaseOrder;
import modules.Reports;
import modules.Settings;
import javax.swing.*;
import java.awt.*;

public class DashBoardFrame1 extends javax.swing.JFrame {

    private User user;
    private Dashboard dashboard;
    private Inventory inventory;
    private Users usersPanel; // Keep a reference to the Users panel

    public DashBoardFrame1(User user) {
        initComponents();
        this.user = user;
        sidebar1.setDashboardFrame(this);
        sidebar1.setUser(user);
        this.setSize(1200, 710);
        this.setLocationRelativeTo(null);
        this.setVisible(true);
        showDashboard(); // Show dashboard by default
        contentPanel1.setLayout(new BorderLayout());
        header2.setParentFrame(this);
    }

    void showDashboard() {
        dashboard = new Dashboard();
        setForm(dashboard);
    }

    public void setForm(JPanel form) {
        contentPanel1.removeAll();
        contentPanel1.add(form, BorderLayout.CENTER);
        contentPanel1.repaint();
        contentPanel1.revalidate();
    }

    // Additional overloaded methods to handle specific panel types
    public void setForm(Dashboard dashboard) {
        setForm((JPanel) dashboard);
    }

    public void setForm(Inventory inventory) {
        this.inventory = inventory; // Store the Inventory instance
        System.out.println("DashBoardFrame1: setForm(Inventory) called");
        // Ensure user is not null before accessing getUserId()
        if (user != null) {
            System.out.println("DashBoardFrame1: User ID before setting Inventory: " + user.getUserId());
            inventory.setCurrentUserId(user.getUserId()); // Set the user ID here
            System.out.println("DashBoardFrame1: Setting Inventory's User ID to: " + user.getUserId());
        } else {
            System.err.println("DashBoardFrame1: User is null when trying to set Inventory's User ID!");
        }
        setForm((JPanel) inventory);
    }

    // Modified setForm for Users panel to pass the current user *after* adding the panel
    public void setForm(Users users) {
        this.usersPanel = users; // Store the Users panel instance

        // First, set the form in the content panel
        setForm((JPanel) usersPanel);

        // Then, use invokeLater to call setCurrentUserId after the UI updates
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (DashBoardFrame1.this.user != null) {
                    // Corrected method name to setCurrentUserId
                    usersPanel.setCurrentUserId(DashBoardFrame1.this.user); // Pass the logged-in user to the Users panel
                } else {
                    // Handle case where user is not set (shouldn't happen if login works)
                    System.err.println("Error: Current user not set in DashBoardFrame1 after invokeLater for Users form!");
                    // Optionally show an error message on the Users panel itself
                }
            }
        });
    }

    public void setForm(PurchaseOrder purchaseOrder) {
        setForm((JPanel) purchaseOrder);
    }

    public void setForm(Reports reports) {
        setForm((JPanel) reports);
    }

    public void setForm(Settings settings) {
        setForm((JPanel) settings);
    }

    public void logout() {
        this.dispose();
        LoginFrame loginFrame = new LoginFrame();
        loginFrame.setVisible(true);
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        contentPanel1 = new form.contentPanel();
        header2 = new components.Header();
        sidebar1 = new components.Sidebar();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("AssetWise Academia");
        setPreferredSize(new java.awt.Dimension(1200, 710));
        setResizable(false);

        javax.swing.GroupLayout contentPanel1Layout = new javax.swing.GroupLayout(contentPanel1);
        contentPanel1.setLayout(contentPanel1Layout);
        contentPanel1Layout.setHorizontalGroup(
            contentPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );
        contentPanel1Layout.setVerticalGroup(
            contentPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 625, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(sidebar1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(contentPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, 931, Short.MAX_VALUE))
            .addGroup(layout.createSequentialGroup()
                .addComponent(header2, javax.swing.GroupLayout.PREFERRED_SIZE, 1200, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(header2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(1, 1, 1)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(contentPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, 625, Short.MAX_VALUE)
                    .addComponent(sidebar1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
        );

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    public static void main(String args[]) {

        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run(User user) {
                new DashBoardFrame1(user).setVisible(true);
            }

            @Override
            public void run() {
                System.out.println("DashBoardFrame1 main method called without a user.");
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private form.contentPanel contentPanel1;
    private components.Header header2;
    private components.Sidebar sidebar1;
    // End of variables declaration//GEN-END:variables

}
