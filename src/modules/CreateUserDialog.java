/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JDialog.java to edit this template
 */
package modules; // Assuming this dialog is in the same package as Users

import Package1.User; // Assuming User is in Package1
// Adjust these import paths to match your actual package structure
import Package1.DBConnection; // Using DBConnection as per your provided code
import Package1.PasswordHasher; // Using PasswordHasher as per your provided code

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Dialog for creating a new user account.
 * Components are manually created and added to the dialog.
 * UI improved with focus indicators and better styling.
 *
 * @author Rei (and modified for dialog implementation with manual components and improved UI)
 */
public class CreateUserDialog extends javax.swing.JDialog {

    private User currentUser; // The admin user creating the new account
    private Users parentPanel; // Reference back to the Users panel to refresh the table

    // Declare Swing components for the form
    private javax.swing.JLabel jLabelTitle;
    private javax.swing.JLabel jLabelUsername;
    private javax.swing.JTextField jTextFieldUsername;
    private javax.swing.JLabel jLabelPassword;
    private javax.swing.JPasswordField jPasswordFieldPassword;
    private javax.swing.JLabel jLabelFullName;
    private javax.swing.JTextField jTextFieldFullName;
    private javax.swing.JLabel jLabelEmail;
    private javax.swing.JTextField jTextFieldEmail;
    private javax.swing.JLabel jLabelRole;
    private javax.swing.JComboBox<String> jComboBoxRole;
    private javax.swing.JButton jButtonCreateUser;

    // Define colors for UI
    private static final Color DARK_BACKGROUND = new Color(30, 30, 30);
    private static final Color MEDIUM_DARK_BACKGROUND = new Color(50, 50, 50);
    private static final Color LIGHT_GRAY_BORDER = new Color(70, 70, 70);
    private static final Color FOCUS_COLOR = new Color(41, 128, 185); // Blue color for focus
    private static final Color TEXT_COLOR = Color.WHITE;
    private static final Color SUCCESS_BUTTON_COLOR = new Color(46, 204, 113); // Green color

    /**
     * Creates new form CreateUserDialog
     * @param parent The parent Frame of the dialog.
     * @param modal Whether the dialog should be modal.
     * @param currentUser The logged-in Admin user creating the new account.
     * @param parentPanel The Users panel that opened this dialog.
     */
    public CreateUserDialog(java.awt.Frame parent, boolean modal, User currentUser, Users parentPanel) {
        super(parent, modal);
        this.currentUser = currentUser;
        this.parentPanel = parentPanel; // Store reference to the parent Users panel
        // Call the method to manually create and add components instead of initComponents()
        setupDialogComponents();
        setupDialogProperties(); // Set up dialog properties
    }

    /**
     * Manually initializes the Swing components and sets up the layout for the dialog.
     */
    private void setupDialogComponents() {
         // Set up the content pane layout first
        this.getContentPane().setLayout(new GridBagLayout());
        this.getContentPane().setBackground(DARK_BACKGROUND); // Set dark background for content pane
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 15, 8, 15); // Increased padding for better spacing


        // Initialize components
        jLabelTitle = new javax.swing.JLabel("Create New User Account");
        jLabelUsername = new javax.swing.JLabel("Username:");
        jTextFieldUsername = new javax.swing.JTextField(20);
        jLabelPassword = new javax.swing.JLabel("Password:"); // Corrected: Initialize as JLabel
        jPasswordFieldPassword = new javax.swing.JPasswordField(20);
        jLabelFullName = new javax.swing.JLabel("Full Name:");
        jTextFieldFullName = new javax.swing.JTextField(20);
        jLabelEmail = new javax.swing.JLabel("Email:");
        jTextFieldEmail = new javax.swing.JTextField(20);
        jLabelRole = new javax.swing.JLabel("Role:");
        jComboBoxRole = new javax.swing.JComboBox<>(new String[] { "Custodian", "Admin" });
        jButtonCreateUser = new javax.swing.JButton("Create User");

        // Basic styling
        jLabelTitle.setFont(new java.awt.Font("Segoe UI", 1, 18)); // Slightly larger title font
        jLabelTitle.setForeground(TEXT_COLOR);

        // Style labels
        jLabelUsername.setForeground(TEXT_COLOR);
        jLabelPassword.setForeground(TEXT_COLOR);
        jLabelFullName.setForeground(TEXT_COLOR);
        jLabelEmail.setForeground(TEXT_COLOR);
        jLabelRole.setForeground(TEXT_COLOR);

        // Style text fields and password field with focus listener for border effect
        styleTextField(jTextFieldUsername);
        stylePasswordField(jPasswordFieldPassword);
        styleTextField(jTextFieldFullName);
        styleTextField(jTextFieldEmail);

        // Style combo box
        jComboBoxRole.setBackground(MEDIUM_DARK_BACKGROUND);
        jComboBoxRole.setForeground(TEXT_COLOR);
        jComboBoxRole.setFont(new Font("Segoe UI", Font.PLAIN, 12)); // Consistent font
        // Add padding to the combo box
        jComboBoxRole.setBorder(new CompoundBorder(
            new LineBorder(LIGHT_GRAY_BORDER, 1), // Outer border
            new EmptyBorder(3, 5, 3, 5) // Inner padding
        ));


        // Style button
        jButtonCreateUser.setFont(new Font("Segoe UI", Font.BOLD, 14));
        jButtonCreateUser.setBackground(SUCCESS_BUTTON_COLOR); // Green color
        jButtonCreateUser.setForeground(TEXT_COLOR);
        jButtonCreateUser.setFocusPainted(false);
        jButtonCreateUser.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(SUCCESS_BUTTON_COLOR.darker(), 1), // Darker border
            BorderFactory.createEmptyBorder(8, 15, 8, 15) // Increased padding
        ));


        // Add components to the dialog using GridBagLayout
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(15, 10, 15, 10); // More space around title
        this.getContentPane().add(jLabelTitle, gbc);

        gbc.gridwidth = 1;
        gbc.gridy++;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.insets = new Insets(8, 10, 8, 10); // Standard padding for form rows
        this.getContentPane().add(jLabelUsername, gbc);
        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL; // Make text field fill space
        this.getContentPane().add(jTextFieldUsername, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.NONE; // Reset fill
        this.getContentPane().add(jLabelPassword, gbc);
        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL; // Make password field fill space
        this.getContentPane().add(jPasswordFieldPassword, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.NONE; // Reset fill
        this.getContentPane().add(jLabelFullName, gbc);
        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL; // Make text field fill space
        this.getContentPane().add(jTextFieldFullName, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.NONE; // Reset fill
        this.getContentPane().add(jLabelEmail, gbc);
        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL; // Make text field fill space
        this.getContentPane().add(jTextFieldEmail, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.NONE; // Reset fill
        this.getContentPane().add(jLabelRole, gbc);
        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL; // Make combo box fill space
        this.getContentPane().add(jComboBoxRole, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.NONE; // Reset fill
        gbc.insets = new Insets(20, 10, 10, 10); // More space above button
        this.getContentPane().add(jButtonCreateUser, gbc);

        // Add action listener to the create button
        jButtonCreateUser.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                jButtonCreateUserActionPerformed(evt);
            }
        });
    }

    /**
     * Sets up the dialog properties (title, close operation, resizable, pack).
     */
    private void setupDialogProperties() {
        this.setTitle("Create New User");
        this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        this.setResizable(false);
        this.pack(); // Pack the dialog to fit components
    }

    /**
     * Applies dark theme styling and focus listener to a JTextField.
     * @param textField The JTextField to style.
     */
    private void styleTextField(JTextField textField) {
        textField.setBackground(MEDIUM_DARK_BACKGROUND);
        textField.setForeground(TEXT_COLOR);
        textField.setCaretColor(TEXT_COLOR); // Set caret color
        textField.setBorder(new CompoundBorder(
            new LineBorder(LIGHT_GRAY_BORDER, 1), // Default border
            new EmptyBorder(5, 5, 5, 5) // Padding
        ));
        textField.setFont(new Font("Segoe UI", Font.PLAIN, 12)); // Consistent font

        // Add focus listener for border color change
        textField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                textField.setBorder(new CompoundBorder(
                    new LineBorder(FOCUS_COLOR, 1), // Focus border
                    new EmptyBorder(5, 5, 5, 5) // Padding
                ));
            }

            @Override
            public void focusLost(FocusEvent e) {
                textField.setBorder(new CompoundBorder(
                    new LineBorder(LIGHT_GRAY_BORDER, 1), // Default border
                    new EmptyBorder(5, 5, 5, 5) // Padding
                ));
            }
        });
    }

     /**
     * Applies dark theme styling and focus listener to a JPasswordField.
     * @param passwordField The JPasswordField to style.
     */
    private void stylePasswordField(JPasswordField passwordField) {
        passwordField.setBackground(MEDIUM_DARK_BACKGROUND);
        passwordField.setForeground(TEXT_COLOR);
        passwordField.setCaretColor(TEXT_COLOR); // Set caret color
        passwordField.setBorder(new CompoundBorder(
            new LineBorder(LIGHT_GRAY_BORDER, 1), // Default border
            new EmptyBorder(5, 5, 5, 5) // Padding
        ));
        passwordField.setFont(new Font("Segoe UI", Font.PLAIN, 12)); // Consistent font

        // Add focus listener for border color change
        passwordField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                passwordField.setBorder(new CompoundBorder(
                    new LineBorder(FOCUS_COLOR, 1), // Focus border
                    new EmptyBorder(5, 5, 5, 5) // Padding
                ));
            }

            @Override
            public void focusLost(FocusEvent e) {
                passwordField.setBorder(new CompoundBorder(
                    new LineBorder(LIGHT_GRAY_BORDER, 1), // Default border
                    new EmptyBorder(5, 5, 5, 5) // Padding
                ));
            }
        });
    }


    /**
     * Action performed when the "Create User" button is clicked in the dialog.
     * Validates input, hashes password, and attempts to insert the new user
     * into the database.
     * @param evt The action event.
     */
    private void jButtonCreateUserActionPerformed(java.awt.event.ActionEvent evt) {
        // Permission check is done before opening the dialog, but can add one here too
        if (currentUser == null || !currentUser.isAdmin()) {
            JOptionPane.showMessageDialog(this, "You do not have permission to create users.", "Permission Denied", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String username = jTextFieldUsername.getText().trim();
        String password = new String(jPasswordFieldPassword.getPassword());
        String fullName = jTextFieldFullName.getText().trim();
        String email = jTextFieldEmail.getText().trim();
        String role = (String) jComboBoxRole.getSelectedItem(); // Get selected role

        // Basic validation
        if (username.isEmpty() || password.isEmpty() || fullName.isEmpty() || email.isEmpty() || role == null) {
            JOptionPane.showMessageDialog(this, "Please fill in all fields.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Hash the password
        String hashedPassword = PasswordHasher.hashPassword(password);
        if (hashedPassword == null) {
            JOptionPane.showMessageDialog(this, "Error processing password.", "Creation Failed", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Database insertion
        String sql = "INSERT INTO Users (Username, Password, FullName, Email, Role, CreatedBy, IsActive) VALUES (?, ?, ?, ?, ?, ?, ?)";

        // Run database operation in a separate thread to avoid blocking the EDT
        new Thread(new Runnable() {
            @Override
            public void run() {
                try (Connection conn = DBConnection.getConnection();
                     PreparedStatement pstmt = conn.prepareStatement(sql)) {

                    pstmt.setString(1, username);
                    pstmt.setString(2, hashedPassword);
                    pstmt.setString(3, fullName);
                    pstmt.setString(4, email);
                    pstmt.setString(5, role);
                    pstmt.setInt(6, currentUser.getUserId()); // Set the creator's UserID
                    pstmt.setBoolean(7, true); // Set IsActive to true by default

                    int rowsAffected = pstmt.executeUpdate();

                    // Show success/failure message and update UI on the EDT
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            if (rowsAffected > 0) {
                                JOptionPane.showMessageDialog(CreateUserDialog.this, "User '" + username + "' created successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
                                // Clear input fields after successful creation
                                jTextFieldUsername.setText("");
                                jPasswordFieldPassword.setText("");
                                jTextFieldFullName.setText("");
                                jTextFieldEmail.setText("");
                                jComboBoxRole.setSelectedIndex(0); // Reset role dropdown

                                // Notify the parent Users panel to refresh the table and charts
                                if (parentPanel != null) {
                                    parentPanel.fetchAndDisplayUsers();
                                    parentPanel.fetchAndDisplayCharts(); // Also refresh charts
                                }

                            } else {
                                JOptionPane.showMessageDialog(CreateUserDialog.this, "Failed to create user.", "Creation Failed", JOptionPane.ERROR_MESSAGE);
                            }
                        }
                    });


                } catch (SQLException e) {
                    e.printStackTrace();
                    // Handle specific SQL errors, e.g., duplicate entry for Username or Email
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            if (e.getSQLState() != null && e.getSQLState().startsWith("23")) { // SQLState for integrity constraint violation
                                 JOptionPane.showMessageDialog(CreateUserDialog.this, "Username or Email already exists.", "Creation Failed", JOptionPane.WARNING_MESSAGE);
                            } else {
                                JOptionPane.showMessageDialog(CreateUserDialog.this, "Database error: " + e.getMessage(), "Creation Failed", JOptionPane.ERROR_MESSAGE);
                            }
                        }
                    });
                } catch (RuntimeException e) {
                     // Catch exceptions from PasswordHasher
                     SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            JOptionPane.showMessageDialog(CreateUserDialog.this, "Error during user creation: " + e.getMessage(), "Creation Failed", JOptionPane.ERROR_MESSAGE);
                            e.printStackTrace();
                        }
                    });
                }
            }
        }).start(); // Start the new thread
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
