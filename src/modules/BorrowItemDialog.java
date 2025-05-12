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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Date; // Import java.util.Date
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import com.toedter.calendar.JDateChooser;


public class BorrowItemDialog extends JDialog {

    private JLabel itemLabel;
    private JLabel availableQuantityLabel;
    private JTextField borrowQuantityField;
    private JTextField borrowerNameField;
    private JTextField borrowerDepartmentField;
    private JTextField borrowerGradeLevelField;
    private JTextField borrowerSectionField;
    private JTextField schoolYearField;
    private JTextArea purposeArea;
    private JDateChooser expectedReturnDateField;
    private JLabel expectedReturnDateLabel; // Label for the return date field

    private JButton borrowButton;
    private JButton cancelButton;

    private Connection conn;
    private User kioskUser; // The logged-in Kiosk user performing the borrow
    private int selectedItemId;
    private String selectedItemName;
    private int maxAvailableQuantity;
    private boolean isMachineryItem; // Field to store isMachinery status


    // Callback interface to notify the parent (KioskDashboard)
    public interface BorrowCompleteListener {
        void onBorrowComplete();
    }

    private BorrowCompleteListener listener;

    // Updated constructor to accept isMachinery
    public BorrowItemDialog(java.awt.Frame parent, boolean modal, Connection conn, User kioskUser, int itemId, String itemName, int maxQuantity, boolean isMachinery, BorrowCompleteListener listener) {
        super(parent, modal);
        this.conn = conn;
        this.kioskUser = kioskUser;
        this.selectedItemId = itemId;
        this.selectedItemName = itemName;
        this.maxAvailableQuantity = maxQuantity;
        this.isMachineryItem = isMachinery; // Store the isMachinery status
        this.listener = listener;

        initComponents();
        setupDialog();
        populateFields(); // Populate item details
    }

    private void setupDialog() {
        setTitle("Request Item: " + selectedItemName);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        setBackground(new Color(30, 30, 30));
        setResizable(false); // Keep dialog fixed size

        JPanel formPanel = createFormPanel();
        JScrollPane formScrollPane = new JScrollPane(formPanel);
        formScrollPane.setBorder(BorderFactory.createEmptyBorder());
        formScrollPane.getViewport().setBackground(new Color(30, 30, 30));
        formScrollPane.setOpaque(false);

        JPanel buttonPanel = createButtonPanel();

        add(formScrollPane, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        setPreferredSize(new Dimension(450, 600)); // Initial size
        pack();
        setLocationRelativeTo(getParent()); // Center the dialog
    }

    private JPanel createFormPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 8, 5, 8);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        itemLabel = new JLabel("Item: " + selectedItemName);
        itemLabel.setFont(new Font("Verdana", Font.BOLD, 14));
        itemLabel.setForeground(Color.WHITE);

        availableQuantityLabel = new JLabel("Available: " + maxAvailableQuantity);
        availableQuantityLabel.setFont(new Font("Verdana", Font.PLAIN, 12));
        availableQuantityLabel.setForeground(Color.LIGHT_GRAY);

        borrowQuantityField = createEditableTextField();
        borrowerNameField = createEditableTextField();
        borrowerDepartmentField = createEditableTextField();
        borrowerGradeLevelField = createEditableTextField();
        borrowerSectionField = createEditableTextField();
        schoolYearField = createEditableTextField();
        purposeArea = new JTextArea(3, 15);
        purposeArea.setLineWrap(true);
        purposeArea.setWrapStyleWord(true);
        purposeArea.setFont(new Font("Verdana", Font.PLAIN, 12));
        purposeArea.setBackground(new Color(50, 50, 50));
        purposeArea.setForeground(Color.WHITE);
        purposeArea.setCaretColor(Color.WHITE);
        JScrollPane purposeScrollPane = new JScrollPane(purposeArea);
        purposeScrollPane.setBorder(BorderFactory.createLineBorder(Color.GRAY));

        expectedReturnDateField = new JDateChooser();
        expectedReturnDateField.setDateFormatString("yyyy-MM-dd");
        expectedReturnDateField.setFont(new Font("Verdana", Font.PLAIN, 12));
        expectedReturnDateField.setBackground(new Color(50, 50, 50));
        expectedReturnDateField.setForeground(Color.WHITE);

        expectedReturnDateLabel = new JLabel("Expected Return:"); // Initialize the label


        int y = 0;
        gbc.gridx = 0; gbc.gridy = y++; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        panel.add(itemLabel, gbc);

        gbc.gridy = y++;
        panel.add(availableQuantityLabel, gbc);

        gbc.gridwidth = 1; // Reset grid width
        addField(panel, gbc, y++, "Quantity:", borrowQuantityField, true);
        addField(panel, gbc, y++, "Name:", borrowerNameField, true);
        addField(panel, gbc, y++, "Department:", borrowerDepartmentField, true);
        addField(panel, gbc, y++, "Grade Level:", borrowerGradeLevelField, true);
        addField(panel, gbc, y++, "Section:", borrowerSectionField, true);
        addField(panel, gbc, y++, "School Year:", schoolYearField, true);
        addField(panel, gbc, y++, "Purpose:", purposeScrollPane, true);

        // Conditionally add the Expected Return Date field and label based on IsMachinery
        if (isMachineryItem) { // Only add if it's a machinery item
             addField(panel, gbc, y++, expectedReturnDateLabel.getText(), expectedReturnDateField, true);
        } else {
             // If it's not a machinery, skip adding the field and label.
             // The row index 'y' is not incremented here as the field is not added.
        }


        gbc.gridx = 0;
        // Adjust the starting gridy for the spacer based on whether the return date field was added
        // If it's a machinery, the spacer goes after the return date row (y was incremented).
        // If it's not a machinery, the spacer goes after the purpose row (y was not incremented for return date).
        gbc.gridy = isMachineryItem ? y : y; // This line is a bit redundant, but keeps the logic clear
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.VERTICAL;
        panel.add(new JLabel(), gbc); // Spacer

        return panel;
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        panel.setOpaque(false);

        borrowButton = new JButton("Request Item");
        styleActionButton(borrowButton, new Color(46, 204, 113)); // Green for Borrow
        borrowButton.addActionListener((ActionEvent e) -> performBorrowTransaction());
        panel.add(borrowButton);

        cancelButton = new JButton("Cancel");
        styleActionButton(cancelButton, new Color(149, 165, 166)); // Gray for Cancel
        cancelButton.addActionListener((ActionEvent e) -> dispose()); // Close the dialog
        panel.add(cancelButton);

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

    private JTextField createEditableTextField() {
        JTextField textField = new JTextField(15);
        textField.setFont(new Font("Verdana", Font.PLAIN, 12));
        textField.setBackground(new Color(50, 50, 50));
        textField.setForeground(Color.WHITE);
        textField.setCaretColor(Color.WHITE);
        return textField;
    }

    private void addField(JPanel panel, GridBagConstraints gbc, int y, String labelText, Component component, boolean stretchHorizontally) {
        gbc.gridx = 0;
        gbc.gridy = y;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0.0;
        JLabel label = new JLabel(labelText);
        label.setForeground(Color.WHITE);
        label.setFont(new Font("Verdana", Font.PLAIN, 12));
        panel.add(label, gbc);

        gbc.gridx = 1;
        gbc.fill = stretchHorizontally ? GridBagConstraints.HORIZONTAL : GridBagConstraints.NONE;
        gbc.weightx = stretchHorizontally ? 1.0 : 0.0;
         if (component instanceof JScrollPane) {
             gbc.fill = GridBagConstraints.BOTH; // Allow description area to grow vertically
             gbc.weighty = 1.0; // Give vertical space
         } else {
             gbc.weighty = 0.0; // Reset weighty
         }

        panel.add(component, gbc);

        if (component instanceof JScrollPane) {
            gbc.weighty = 0.0; // Reset weighty after placing the description area
        }
    }

    private void populateFields() {
        // Item details are already passed to the constructor
        // borrowerNameField, etc., are left empty for the user to fill
    }

    private boolean validateInput() {
        String borrowQtyStr = borrowQuantityField.getText().trim();
        String borrowerName = borrowerNameField.getText().trim();
        String purpose = purposeArea.getText().trim();
        // Get date regardless of visibility, but will only be validated if isMachineryItem is true
        Date expectedReturnDate = expectedReturnDateField.getDate();


        if (borrowQtyStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Quantity cannot be empty.", "Input Error", JOptionPane.WARNING_MESSAGE);
            borrowQuantityField.requestFocus();
            return false;
        }
        int borrowQty;
        try {
            borrowQty = Integer.parseInt(borrowQtyStr);
            if (borrowQty <= 0) {
                JOptionPane.showMessageDialog(this, "Quantity must be a positive number.", "Input Error", JOptionPane.WARNING_MESSAGE);
                borrowQuantityField.requestFocus();
                return false;
            }
            if (borrowQty > maxAvailableQuantity) {
                JOptionPane.showMessageDialog(this, "Quantity (" + borrowQty + ") exceeds available quantity (" + maxAvailableQuantity + ").", "Input Error", JOptionPane.WARNING_MESSAGE);
                 borrowQuantityField.requestFocus();
                return false;
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid Quantity format. Must be a number.", "Input Error", JOptionPane.WARNING_MESSAGE);
            borrowQuantityField.requestFocus();
            return false;
        }

        if (borrowerName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Name cannot be empty.", "Input Error", JOptionPane.WARNING_MESSAGE);
            borrowerNameField.requestFocus();
            return false;
        }

        if (purpose.isEmpty()) {
             JOptionPane.showMessageDialog(this, "Purpose cannot be empty.", "Input Error", JOptionPane.WARNING_MESSAGE);
             purposeArea.requestFocus();
            return false;
        }

        // Only validate Expected Return Date if the item IS a machinery
        if (isMachineryItem && expectedReturnDate == null) {
             JOptionPane.showMessageDialog(this, "Expected Return Date cannot be empty for this item.", "Input Error", JOptionPane.WARNING_MESSAGE);
             expectedReturnDateField.requestFocus();
            return false;
        }


        return true;
    }

    private void performBorrowTransaction() {
        if (!validateInput()) {
            return;
        }
         if (conn == null) {
            JOptionPane.showMessageDialog(this, "Database connection is not available.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
         if (kioskUser == null || kioskUser.getUserId() <= 0) {
            JOptionPane.showMessageDialog(this, "No valid Kiosk user session. Cannot perform transaction.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }


        int borrowQty = Integer.parseInt(borrowQuantityField.getText().trim());
        String borrowerName = borrowerNameField.getText().trim();
        String borrowerDepartment = borrowerDepartmentField.getText().trim();
        String borrowerGradeLevel = borrowerGradeLevelField.getText().trim();
        String borrowerSection = borrowerSectionField.getText().trim();
        String schoolYear = schoolYearField.getText().trim();
        String purpose = purposeArea.getText().trim();
        // Get the date only if it's a machinery item
        Date expectedReturnDate = isMachineryItem ? expectedReturnDateField.getDate() : null;


        // Use TransactionType 'Issued' for borrowing
        String sql = "INSERT INTO Transactions (ItemID, TransactionType, Quantity, UserID, IssuedToPersonName, IssuedToDepartment, IssuedToGradeLevel, IssuedToSection, SchoolYear, Purpose, ExpectedReturnDate) " +
                     "VALUES (?, 'Issued', ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, selectedItemId);
            pstmt.setInt(2, borrowQty);
            pstmt.setInt(3, kioskUser.getUserId()); // The Kiosk user performing the issue
            pstmt.setString(4, borrowerName);
            pstmt.setString(5, borrowerDepartment.isEmpty() ? null : borrowerDepartment);
            pstmt.setString(6, borrowerGradeLevel.isEmpty() ? null : borrowerGradeLevel);
            pstmt.setString(7, borrowerSection.isEmpty() ? null : borrowerSection);
            pstmt.setString(8, schoolYear.isEmpty() ? null : schoolYear);
            pstmt.setString(9, purpose);
            // Set ExpectedReturnDate to SQL NULL if it's not a machinery item or the date is null
            if (expectedReturnDate != null) {
                 pstmt.setDate(10, new java.sql.Date(expectedReturnDate.getTime()));
            } else {
                 pstmt.setNull(10, Types.DATE); // Set to SQL NULL
            }


            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                JOptionPane.showMessageDialog(this, "Item(s) borrowed successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);

                // Log the activity to the RecentActivities table
                // Ensure you have a logActivity method accessible (e.g., in DBConnection or a utility class)
                String logDetails = String.format("Borrowed %d unit(s) of '%s' (ID: %d) to %s.",
                                                  borrowQty, selectedItemName, selectedItemId, borrowerName);
                logActivity("Transaction: Issued", logDetails); // Corrected ActivityType string


                // Generate and show receipt
                generateAndShowReceipt(selectedItemId, selectedItemName, borrowQty, borrowerName, purpose, expectedReturnDate);

                // Notify the listener (KioskDashboard) to refresh the available items list
                if (listener != null) {
                    listener.onBorrowComplete();
                }

                dispose(); // Close the dialog after successful transaction
            } else {
                JOptionPane.showMessageDialog(this, "Failed to record borrowing transaction.", "Error", JOptionPane.ERROR_MESSAGE);
            }

        } catch (SQLException e) {
            e.printStackTrace();
             // Check for insufficient stock error from trigger
            if (e.getSQLState() != null && e.getSQLState().equals("45000")) {
                 JOptionPane.showMessageDialog(this, e.getMessage(), "Transaction Error", JOptionPane.ERROR_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Database error during borrowing: " + e.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

     // Assuming this method exists in DBConnection or a logging utility
     private void logActivity(String activityType, String details) {
        if (conn == null || kioskUser == null || kioskUser.getUserId() <= 0) {
            System.err.println("Cannot log activity: DB connection null or invalid UserID (" + (kioskUser != null ? kioskUser.getUserId() : "null") + ")");
            return;
        }
        String sql = "INSERT INTO RecentActivities (ActivityType, UserID, UserName, Details, ActivityDate) VALUES (?, ?, ?, ?, NOW())";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, activityType);
            pstmt.setInt(2, kioskUser.getUserId());
            pstmt.setString(3, kioskUser.getUsername()); // Log the kiosk user's username
            pstmt.setString(4, details);
            pstmt.executeUpdate();
            System.out.println("Activity logged: Type=" + activityType + ", User=" + kioskUser.getUsername() + ", Details=" + details); // Debug print logging success
        } catch (SQLException e) {
            System.err.println("Error logging activity: " + e.getMessage());
            e.printStackTrace();
        }
    }


    private void generateAndShowReceipt(int itemId, String itemName, int quantity, String borrowerName, String purpose, Date expectedReturnDate) {
        // Create and show the receipt dialog
        ReceiptDialog receiptDialog = new ReceiptDialog(
            (Frame) SwingUtilities.getWindowAncestor(this),
            true, // modal
            itemId,
            itemName,
            quantity,
            borrowerName,
            purpose,
            expectedReturnDate
        );
        receiptDialog.setVisible(true);
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
