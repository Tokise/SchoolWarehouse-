
package Package1;

import com.sun.jdi.connect.spi.Connection;
import modules.Inventory;
import modules.Dashboard;
import modules.Users;
import modules.PurchaseOrder;
import modules.Reports;
import modules.Settings;
import javax.swing.*;
import java.awt.*;

public class DashBoardFrame1 extends javax.swing.JFrame {

    private User user;
    private Dashboard dashboard;
    private Inventory inventory;

    public DashBoardFrame1(User user) {
        initComponents();
        this.user = user;
        sidebar1.setDashboardFrame(this);
        sidebar1.setUser(user);
        this.setSize(1200, 700);
        this.setLocationRelativeTo(null);
        this.setVisible(true);
        showDashboard(); // Show dashboard by default
        contentPanel1.setLayout(new BorderLayout());

        // Set the current user ID in the Inventory module
        if (dashboard != null) {
            //  No direct way to access Inventory from dashboard here without knowing how it's created/managed
            //  Ideally, when Inventory is created, pass the user ID
            //  For now,  if Inventory is created elsewhere and accessible, modify that part
        }
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
    System.out.println("DashBoardFrame1: setForm(Inventory) called"); // ADD THIS
    System.out.println("DashBoardFrame1: User ID before setting: " + user.getUserId()); // ADD THIS
    inventory.setCurrentUserId(user.getUserId()); // Set the user ID here
    System.out.println("DashBoardFrame1: Setting Inventory's User ID to: " + user.getUserId()); // ADD THIS
    setForm((JPanel) inventory);
    }

    public void setForm(Users users) {
        setForm((JPanel) users);
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
    
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        header1 = new components.Header();
        sidebar1 = new components.Sidebar();
        contentPanel1 = new form.contentPanel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("DashBoard");
        setPreferredSize(new java.awt.Dimension(1088, 685));
        setResizable(false);

        sidebar1.setPreferredSize(new java.awt.Dimension(270, 596));

        javax.swing.GroupLayout contentPanel1Layout = new javax.swing.GroupLayout(contentPanel1);
        contentPanel1.setLayout(contentPanel1Layout);
        contentPanel1Layout.setHorizontalGroup(
            contentPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );
        contentPanel1Layout.setVerticalGroup(
            contentPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(sidebar1, javax.swing.GroupLayout.PREFERRED_SIZE, 259, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(contentPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, 846, Short.MAX_VALUE))
            .addComponent(header1, javax.swing.GroupLayout.DEFAULT_SIZE, 1105, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(header1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(1, 1, 1)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(sidebar1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(contentPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, 596, Short.MAX_VALUE)))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    public static void main(String args[]) {

        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run(User user) {
                new DashBoardFrame1(user).setVisible(true);
            }

            @Override
            public void run() {
                
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private form.contentPanel contentPanel1;
    private components.Header header1;
    private components.Sidebar sidebar1;
    // End of variables declaration//GEN-END:variables

 
}
