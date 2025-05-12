package modules;

import Package1.DBConnection;
import Package1.User;

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
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.text.SimpleDateFormat;
import java.util.Date; // Import java.util.Date
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import com.toedter.calendar.JDateChooser;

public class ItemDetailsDialog extends JDialog {

    private JTextField itemIdField;
    private JTextField nameField;
    private JTextArea descriptionArea;
    private JComboBox<String> categoryField;
    private JTextField quantityField;
    private JTextField reorderLevelField;
    private JComboBox<String> unitField;
    private JLabel imageLabel;
    private File selectedImageFile;
    private String selectedImageType;

    private JCheckBox isMachineryCheckBox;
    private JComboBox<String> machineStatusField;
    private JComboBox<String> itemConditionField;
    private JTextField locationField;
    private JTextField serialNumberField;
    private JDateChooser purchaseDateField;
    private JDateChooser warrantyExpiryDateField;

    private JButton saveButton;
    private JButton archiveButton;
    private JButton cancelButton;
    private JButton restoreButton;

    private Connection conn;
    private User currentUser;
    private int currentItemId = -1; // -1 indicates a new item

    // Flag to track if the dialog is currently adding a new item (true) or editing an existing one (false)
    private boolean isNewItem = true; // This variable is correctly declared here

    private boolean isArchived = false; // Track if the item is currently archived

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    // Callback interface to notify the parent (Inventory) when changes are made
    public interface ItemDetailsListener {
        void itemSavedOrArchived(); // Method name updated for clarity
    }

    private ItemDetailsListener listener;

    public ItemDetailsDialog(java.awt.Frame parent, boolean modal, Connection conn, User currentUser, ItemDetailsListener listener) {
        super(parent, modal);
        this.conn = conn;
        this.currentUser = currentUser;
        this.listener = listener;
        initComponents();
        setupDialog();
        loadCategories(); // Load categories when the dialog is created
    }

    private void setupDialog() {
        setTitle("Item Details");
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        setBackground(new Color(30, 30, 30));
        setResizable(true); // Allow resizing

        JPanel formPanel = createFormPanel();
        JScrollPane formScrollPane = new JScrollPane(formPanel);
        formScrollPane.setBorder(BorderFactory.createEmptyBorder());
        formScrollPane.getViewport().setBackground(new Color(30, 30, 30));
        formScrollPane.setOpaque(false);

        JPanel buttonPanel = createButtonPanel();

        add(formScrollPane, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        setPreferredSize(new Dimension(500, 700)); // Initial size
        pack();
        setLocationRelativeTo(getParent()); // Center the dialog
    }

    private JPanel createFormPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // Add padding

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 8, 5, 8);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        itemIdField = createNonEditableTextField();
        nameField = createEditableTextField();
        descriptionArea = new JTextArea(3, 15);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        descriptionArea.setFont(new Font("Verdana", Font.PLAIN, 12));
        descriptionArea.setBackground(new Color(50, 50, 50));
        descriptionArea.setForeground(Color.WHITE);
        descriptionArea.setCaretColor(Color.WHITE);
        JScrollPane descriptionScrollPane = new JScrollPane(descriptionArea);
        descriptionScrollPane.setBorder(BorderFactory.createLineBorder(Color.GRAY));


        categoryField = new JComboBox<>();
        categoryField.setFont(new Font("Verdana", Font.PLAIN, 12));
        categoryField.setBackground(new Color(50, 50, 50));
        categoryField.setForeground(Color.WHITE);
        ((JLabel)categoryField.getRenderer()).setHorizontalAlignment(SwingConstants.CENTER);


        quantityField = createEditableTextField();
        reorderLevelField = createEditableTextField();
        String[] units = {"pcs", "boxes", "packs", "reams", "liters", "kg", "meters", "rolls", "units", "sets"};
        unitField = new JComboBox<>(units);
        unitField.setEditable(true);
        unitField.setFont(new Font("Verdana", Font.PLAIN, 12));
        unitField.setBackground(new Color(50, 50, 50));
        unitField.setForeground(Color.WHITE);
         ((JLabel)unitField.getRenderer()).setHorizontalAlignment(SwingConstants.CENTER);


        isMachineryCheckBox = new JCheckBox("Is Machinery?");
        isMachineryCheckBox.setOpaque(false);
        isMachineryCheckBox.setForeground(Color.WHITE);
        isMachineryCheckBox.setFont(new Font("Verdana", Font.PLAIN, 12));
        isMachineryCheckBox.addActionListener(e -> toggleMachineSpecificFields(isMachineryCheckBox.isSelected()));

        machineStatusField = new JComboBox<>(new String[]{"Not Applicable", "Active", "Inactive", "Under Maintenance"});
        machineStatusField.setFont(new Font("Verdana", Font.PLAIN, 12));
        machineStatusField.setBackground(new Color(50, 50, 50));
        machineStatusField.setForeground(Color.WHITE);
         ((JLabel)machineStatusField.getRenderer()).setHorizontalAlignment(SwingConstants.CENTER);


        itemConditionField = new JComboBox<>(new String[]{"Not Applicable", "Good", "Fair", "Damaged", "Needs Repair"});
        itemConditionField.setFont(new Font("Verdana", Font.PLAIN, 12));
        itemConditionField.setBackground(new Color(50, 50, 50));
        itemConditionField.setForeground(Color.WHITE);
         ((JLabel)itemConditionField.getRenderer()).setHorizontalAlignment(SwingConstants.CENTER);


        locationField = createEditableTextField();
        serialNumberField = createEditableTextField();
        purchaseDateField = new JDateChooser();
        purchaseDateField.setDateFormatString("yyyy-MM-dd");
        purchaseDateField.setFont(new Font("Verdana", Font.PLAIN, 12));
        purchaseDateField.setBackground(new Color(50, 50, 50));
        purchaseDateField.setForeground(Color.WHITE);


        warrantyExpiryDateField = new JDateChooser();
        warrantyExpiryDateField.setDateFormatString("yyyy-MM-dd");
        warrantyExpiryDateField.setFont(new Font("Verdana", Font.PLAIN, 12));
        warrantyExpiryDateField.setBackground(new Color(50, 50, 50));
        warrantyExpiryDateField.setForeground(Color.WHITE);


        toggleMachineSpecificFields(false); // Initial state

        int y = 0;
        addField(panel, gbc, y++, "Item ID:", itemIdField, false);
        addField(panel, gbc, y++, "Name:", nameField, true);
        addField(panel, gbc, y++, "Description:", descriptionScrollPane, true);
        addField(panel, gbc, y++, "Category:", categoryField, true);
        addField(panel, gbc, y++, "Quantity:", quantityField, true);
        addField(panel, gbc, y++, "Unit:", unitField, true);
        addField(panel, gbc, y++, "Reorder Lvl:", reorderLevelField, true);

        gbc.gridx = 0; gbc.gridy = y++; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        panel.add(isMachineryCheckBox, gbc);
        gbc.gridwidth = 1;

        addField(panel, gbc, y++, "Machine Status:", machineStatusField, true);
        addField(panel, gbc, y++, "Item Condition:", itemConditionField, true);
        addField(panel, gbc, y++, "Location:", locationField, true);
        addField(panel, gbc, y++, "Serial No.:", serialNumberField, true);
        addField(panel, gbc, y++, "Purchase Date:", purchaseDateField, true);
        addField(panel, gbc, y++, "Warranty End:", warrantyExpiryDateField, true);

        gbc.gridx = 0;
        gbc.gridy = y++;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
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
        panel.add(imagePanel, gbc);

        gbc.gridx = 0;
        gbc.gridy = y;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.VERTICAL;
        panel.add(new JLabel(), gbc); // Spacer

        return panel;
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        panel.setOpaque(false);

        saveButton = new JButton("Save");
        styleActionButton(saveButton, new Color(46, 204, 113)); // Green for Save
        saveButton.addActionListener((ActionEvent e) -> saveItem());
        panel.add(saveButton);

        archiveButton = new JButton("Archive"); // Button to archive
        styleActionButton(archiveButton, new Color(231, 76, 60)); // Red for Archive
        archiveButton.addActionListener((ActionEvent e) -> archiveItem());
        panel.add(archiveButton);

        restoreButton = new JButton("Restore"); // Button to restore from archive
        styleActionButton(restoreButton, new Color(52, 152, 219)); // Blue for Restore
        restoreButton.addActionListener((ActionEvent e) -> restoreItem());
        restoreButton.setVisible(false); // Initially hidden
        panel.add(restoreButton);


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

    private JTextField createNonEditableTextField() {
        JTextField textField = new JTextField(15);
        textField.setFont(new Font("Verdana", Font.PLAIN, 12));
        textField.setEditable(false);
        textField.setBackground(Color.DARK_GRAY);
        textField.setForeground(Color.LIGHT_GRAY);
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

     private void toggleMachineSpecificFields(boolean enabled) {
        // Only toggle if the dialog is in an editable state
        boolean dialogEditable = nameField.isEditable(); // Check if the name field is editable as a proxy for dialog editable state
        machineStatusField.setEnabled(enabled && dialogEditable);
        reorderLevelField.setEnabled(!enabled && dialogEditable); // Disable reorder level for machinery only if editable

        if (!enabled) {
            machineStatusField.setSelectedItem("Not Applicable");
        } else {
             if ("Not Applicable".equals(machineStatusField.getSelectedItem())) {
                machineStatusField.setSelectedItem("Active");
            }
            reorderLevelField.setText("0"); // Set reorder level to 0 or empty for machinery
        }
    }


    private void loadCategories() {
        categoryField.removeAllItems();
        if (conn == null) {
            System.err.println("Cannot load categories: DB connection is null.");
            return;
        }

        String sql = "SELECT CategoryName FROM Categories ORDER BY CategoryName";
        try (PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                categoryField.addItem(rs.getString("CategoryName"));
            }
        } catch (SQLException e) {
            System.err.println("Error loading categories: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Failed to load categories: " + e.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void loadItemDetails(int itemId) {
        this.currentItemId = itemId;
        isNewItem = false; // Loading an existing item

        if (conn == null) {
             JOptionPane.showMessageDialog(this, "Database not connected. Cannot load item details.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String sql = "SELECT i.*, c.CategoryName FROM Items i LEFT JOIN Categories c ON i.CategoryID = c.CategoryID WHERE i.ItemID = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, itemId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    itemIdField.setText(String.valueOf(rs.getInt("ItemID")));
                    nameField.setText(rs.getString("ItemName"));
                    descriptionArea.setText(rs.getString("Description"));
                    categoryField.setSelectedItem(rs.getString("CategoryName"));
                    quantityField.setText(String.valueOf(rs.getInt("Quantity")));
                    reorderLevelField.setText(String.valueOf(rs.getInt("ReorderLevel")));
                    unitField.setSelectedItem(rs.getString("Unit"));

                    boolean isMachinery = rs.getBoolean("IsMachinery");
                    isMachineryCheckBox.setSelected(isMachinery);
                    // Machine status and item condition will be set below based on archived status

                    locationField.setText(rs.getString("Location"));
                    serialNumberField.setText(rs.getString("SerialNumber"));

                    Date pDate = rs.getDate("PurchaseDate");
                    purchaseDateField.setDate(pDate);
                    Date wDate = rs.getDate("WarrantyExpiryDate");
                    warrantyExpiryDateField.setDate(wDate);

                    isArchived = rs.getBoolean("IsArchived"); // Get archived status from DB

                    if (isArchived) {
                         setTitle("View Archived Item Details");
                         setFieldsEditable(false); // Set editable based on archived status
                         saveButton.setVisible(false);
                         archiveButton.setVisible(false);
                         restoreButton.setVisible(true);
                         // Set machine status and item condition for archived items (they are non-editable)
                         machineStatusField.setSelectedItem(rs.getString("MachineStatus"));
                         itemConditionField.setSelectedItem(rs.getString("ItemCondition"));
                    } else {
                         setTitle("Edit Item Details");
                         setFieldsEditable(true); // Set editable based on archived status
                         saveButton.setVisible(true);
                         archiveButton.setVisible(true);
                         restoreButton.setVisible(false);
                         // Set machine status and item condition for non-archived items (they are editable or not based on isMachinery)
                         machineStatusField.setSelectedItem(rs.getString("MachineStatus"));
                         itemConditionField.setSelectedItem(rs.getString("ItemCondition"));
                    }

                    // Ensure machinery specific fields are toggled correctly based on checkbox state AND editable state
                    toggleMachineSpecificFields(isMachineryCheckBox.isSelected());


                    byte[] imageData = null;
                    try {
                         imageData = rs.getBytes("ItemImage");
                    } catch(SQLException exImg) {
                        System.err.println("Error reading image blob: " + exImg.getMessage());
                    }
                    selectedImageFile = null; // Clear selected file when loading from DB
                    if (imageData != null && imageData.length > 0) {
                        try {
                            ImageIcon iIcon = new ImageIcon(imageData);
                            Image originalImage = iIcon.getImage();
                            // Scale image for display in the label
                             Image scaledImage = originalImage.getScaledInstance(
                                    imageLabel.getPreferredSize().width > 0 ? imageLabel.getPreferredSize().width : 200,
                                    imageLabel.getPreferredSize().height > 0 ? imageLabel.getPreferredSize().height : 150,
                                    Image.SCALE_SMOOTH);
                            imageLabel.setIcon(new ImageIcon(scaledImage));
                            imageLabel.setText(null);
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
                    selectedImageType = rs.getString("ItemImageType");

                    setVisible(true); // Show the dialog
                } else {
                    JOptionPane.showMessageDialog(this, "Item with ID " + itemId + " not found.", "Not Found", JOptionPane.WARNING_MESSAGE);
                    dispose(); // Close dialog if item not found
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading item details: " + e.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
            dispose(); // Close dialog on error
        }
    }

    public void prepareNewItem() {
        isNewItem = true; // Preparing a new item
        currentItemId = -1;
        isArchived = false; // New items are not archived
        setTitle("Add New Item");
        clearFields();
        setFieldsEditable(true); // New items are always editable
        saveButton.setVisible(true);
        archiveButton.setVisible(false); // Hide archive for new items
        restoreButton.setVisible(false); // Hide restore for new items

        // Fetch and display the next available ID (preview)
        int nextId = getNextAvailableItemId();
        if (nextId > 0) {
            itemIdField.setText(String.valueOf(nextId));
        } else {
            itemIdField.setText("Error getting ID"); // Indicate if fetching failed
        }

        setVisible(true); // Show the dialog
        nameField.requestFocus();
        toggleMachineSpecificFields(false); // Start with machinery fields disabled for new item
    }

     private int getNextAvailableItemId() {
        if (conn == null) {
            System.err.println("Cannot get next item ID: DB connection is null.");
            return -1; // Indicate failure
        }
        // This query finds the maximum current ID and adds 1.
        // It assumes auto-increment behavior and is a preview.
        // The actual ID is assigned by the DB on INSERT.
        String sql = "SELECT MAX(ItemID) FROM Items";
        try (PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                // If there are existing items, maxId will be > 0. Add 1.
                // If no items exist, maxId will be 0 (or null, handled by getInt(1)), so nextId will be 1.
                return rs.getInt(1) + 1;
            } else {
                // Should not happen with MAX(), but as a fallback
                return 1;
            }
        } catch (SQLException e) {
            System.err.println("Error fetching next available ItemID: " + e.getMessage());
            e.printStackTrace();
            return -1; // Indicate failure
        }
    }


    private void saveItem() {
        if (!validateInput()) {
            return;
        }
        if (conn == null) {
            JOptionPane.showMessageDialog(this, "Database connection is not available.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
         if (currentUser == null || currentUser.getUserId() <= 0) {
            JOptionPane.showMessageDialog(this, "No valid user session. Cannot save item.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }


        String itemName = nameField.getText().trim();
        String description = descriptionArea.getText().trim();
        String categoryName = (String) categoryField.getSelectedItem();
        int quantity = Integer.parseInt(quantityField.getText().trim());
        int reorderLevel = reorderLevelField.isEnabled() ? Integer.parseInt(reorderLevelField.getText().trim()) : 0;
        String unit = unitField.getSelectedItem().toString().trim();

        boolean isMachinery = isMachineryCheckBox.isSelected();
        String machineStatus = isMachinery ? (String) machineStatusField.getSelectedItem() : "Not Applicable";
        String itemCondition = (String) itemConditionField.getSelectedItem();
        String location = locationField.getText().trim();
        String serialNumber = serialNumberField.getText().trim();
        Date purchaseDateVal = purchaseDateField.getDate(); // purchaseDateVal is a Date object
        Date warrantyExpiryDateVal = warrantyExpiryDateField.getDate(); // warrantyExpiryDateVal is a Date object

        int categoryId = getCategoryId(categoryName);
         if (categoryId == -1 && categoryName != null && !categoryName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Selected category '" + categoryName + "' not found. Please refresh categories.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }


        String sql;
        String successMessage;
        String logActivityType;
        String logDetails;

        FileInputStream fis = null;
        try {
            conn.setAutoCommit(false);

            if (isNewItem) {
                 // Ensure IsArchived is FALSE for new items
                sql = "INSERT INTO Items (ItemName, Description, CategoryID, Quantity, ReorderLevel, Unit, " +
                      "IsMachinery, MachineStatus, ItemCondition, Location, SerialNumber, PurchaseDate, WarrantyExpiryDate, " +
                      "ItemImage, ItemImageType, AddedBy, IsArchived, CreatedAt) " + // Include IsArchived
                      "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())";
                successMessage = "Item added successfully.";
                logActivityType = "Item Added";
            } else {
                sql = "UPDATE Items SET ItemName=?, Description=?, CategoryID=?, Quantity=?, ReorderLevel=?, Unit=?, " +
                      "IsMachinery=?, MachineStatus=?, ItemCondition=?, Location=?, SerialNumber=?, PurchaseDate=?, WarrantyExpiryDate=?, " +
                      (selectedImageFile != null ? "ItemImage=?, ItemImageType=?, " : "") +
                      "UpdatedAt=NOW() WHERE ItemID=?";
                successMessage = "Item updated successfully.";
                logActivityType = "Item Updated";
            }

            try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                int paramIndex = 1;
                pstmt.setString(paramIndex++, itemName);
                pstmt.setString(paramIndex++, description);
                if (categoryId != -1) pstmt.setInt(paramIndex++, categoryId); else pstmt.setNull(paramIndex++, Types.INTEGER);
                pstmt.setInt(paramIndex++, quantity);
                pstmt.setInt(paramIndex++, reorderLevel);
                pstmt.setString(paramIndex++, unit);
                pstmt.setBoolean(paramIndex++, isMachinery);
                pstmt.setString(paramIndex++, machineStatus);
                pstmt.setString(paramIndex++, itemCondition);
                pstmt.setString(paramIndex++, location.isEmpty() ? null : location);
                pstmt.setString(paramIndex++, serialNumber.isEmpty() ? null : serialNumber);
                // Use getTime() method from the java.util.Date object
                pstmt.setDate(paramIndex++, purchaseDateVal != null ? new java.sql.Date(purchaseDateVal.getTime()) : null);
                pstmt.setDate(paramIndex++, warrantyExpiryDateVal != null ? new java.sql.Date(warrantyExpiryDateVal.getTime()) : null);

                if (isNewItem) {
                    if (selectedImageFile != null && selectedImageFile.exists()) {
                        fis = new FileInputStream(selectedImageFile);
                        pstmt.setBinaryStream(paramIndex++, fis, (int) selectedImageFile.length());
                        pstmt.setString(paramIndex++, selectedImageType);
                    } else {
                        pstmt.setNull(paramIndex++, Types.LONGVARBINARY);
                        pstmt.setNull(paramIndex++, Types.VARCHAR);
                    }
                    pstmt.setInt(paramIndex++, currentUser.getUserId());
                    pstmt.setBoolean(paramIndex++, false); // New items are not archived
                } else {
                    if (selectedImageFile != null && selectedImageFile.exists()) {
                        fis = new FileInputStream(selectedImageFile);
                        pstmt.setBinaryStream(paramIndex++, fis, (int) selectedImageFile.length());
                        pstmt.setString(paramIndex++, selectedImageType);
                    }
                    pstmt.setInt(paramIndex++, currentItemId); // Use the existing ID for update
                }

                int rowsAffected = pstmt.executeUpdate();

                if (rowsAffected > 0) {
                    int savedItemId = currentItemId;
                    if (isNewItem) { // This block is for a newly saved item
                         try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                            if (generatedKeys.next()) {
                                savedItemId = generatedKeys.getInt(1);
                                // Update the dialog's currentItemId to the newly generated one
                                this.currentItemId = savedItemId;
                                itemIdField.setText(String.valueOf(savedItemId));
                                // *** After a successful insert, the item is no longer 'new' ***
                                isNewItem = false; // Update the flag
                                setTitle("Edit Item Details"); // Change title after saving new item
                                archiveButton.setVisible(true); // Show archive button now that it's an existing item
                                restoreButton.setVisible(false); // Ensure restore button is hidden
                                setFieldsEditable(true); // Ensure fields are still editable after saving a new item
                            } else {
                                // Handle case where generated keys are not returned (shouldn't happen with AUTO_INCREMENT)
                                System.err.println("Warning: Failed to retrieve generated key for new item.");
                                // The dialog state might be slightly inconsistent if ID isn't updated
                            }
                         }
                         logDetails = "Item '" + itemName + "' (ID: " + savedItemId + ") added.";
                    } else { // This block is for an updated existing item
                         savedItemId = Integer.parseInt(itemIdField.getText());
                         logDetails = "Item '" + itemName + "' (ID: " + savedItemId + ") updated.";
                         // For an updated item, the editable state and button visibility
                         // should already be correct based on its archived status,
                         // which was set in loadItemDetails. No need to change them here.
                    }


                    conn.commit();
                    JOptionPane.showMessageDialog(this, successMessage, "Success", JOptionPane.INFORMATION_MESSAGE);
                    logActivity(logActivityType, logDetails);
                    if (listener != null) {
                        listener.itemSavedOrArchived(); // Notify listener on save
                    }
                    // Keep the dialog open for further edits after save, or close if preferred
                    // dispose(); // Uncomment to close after save
                } else {
                    conn.rollback();
                    JOptionPane.showMessageDialog(this, "Failed to save item. No rows affected.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }

        } catch (SQLException | IOException e) {
            try { if (conn != null) conn.rollback(); } catch (SQLException exRb) { System.err.println("Rollback failed: " + exRb.getMessage());}
            e.printStackTrace();
            // Provide a more specific error message if possible, though the DB trigger error is handled externally
            String errorMsg = "Failed to save item: " + e.getMessage();
             if (e instanceof SQLException && ((SQLException)e).getSQLState() != null && ((SQLException)e).getSQLState().startsWith("23")) {
                 errorMsg += "\nThis might be caused by a database constraint violation (e.g., foreign key, unique constraint).";
             } else if (e instanceof SQLException && e.getMessage() != null && e.getMessage().contains("Cannot update table")) {
                  errorMsg += "\nThis is likely caused by a database trigger or stored procedure attempting to modify the same table.";
             }
            JOptionPane.showMessageDialog(this, errorMsg, "DB Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            if (fis != null) {
                try { fis.close(); } catch (IOException ex) { /* ignore */ }
            }
            try { if (conn != null) conn.setAutoCommit(true); } catch (SQLException exAc) { /* ignore */ }
        }
    }

    private void archiveItem() {
         // Check if it's an existing, non-archived item before allowing archive
         if (currentItemId == -1 || isNewItem || isArchived) {
            JOptionPane.showMessageDialog(this, "Cannot archive this item. It must be an existing, non-archived item.", "Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String itemName = nameField.getText();
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to archive item '" + itemName + "' (ID: " + currentItemId + ")?\n" +
                "Archived items will not appear in the main inventory list but can be restored.",
                "Confirm Archive", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            if (conn == null) {
                JOptionPane.showMessageDialog(this, "Database connection is not available.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            String sql = "UPDATE Items SET IsArchived = TRUE, UpdatedAt = NOW() WHERE ItemID = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, currentItemId);
                int rowsAffected = pstmt.executeUpdate();
                if (rowsAffected > 0) {
                    JOptionPane.showMessageDialog(this, "Item '" + itemName + "' archived successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
                    logActivity("Item Archived", "Item '" + itemName + "' (ID: " + currentItemId + ") archived by user " + (currentUser != null ? currentUser.getUsername() : "Unknown"));
                    isArchived = true; // Update dialog state
                    setFieldsEditable(false); // Make fields non-editable after archiving
                    saveButton.setVisible(false);
                    archiveButton.setVisible(false);
                    restoreButton.setVisible(true);
                    setTitle("View Archived Item Details");

                    if (listener != null) {
                        listener.itemSavedOrArchived(); // Notify listener on archive
                    }
                    // Optionally close the dialog after archiving
                    // dispose();
                } else {
                    JOptionPane.showMessageDialog(this, "Item not found or could not be archived.", "Error", JOptionPane.WARNING_MESSAGE);
                }
            } catch (SQLException e) {
                 JOptionPane.showMessageDialog(this, "Database error during archiving: " + e.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        }
    }

     private void restoreItem() {
         // Check if it's an existing, archived item before allowing restore
         if (currentItemId == -1 || isNewItem || !isArchived) {
            JOptionPane.showMessageDialog(this, "Cannot restore this item. It must be an existing, archived item.", "Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String itemName = nameField.getText();
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to restore item '" + itemName + "' (ID: " + currentItemId + ")?\n" +
                "This item will be visible in the main inventory list again.",
                "Confirm Restore", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            if (conn == null) {
                JOptionPane.showMessageDialog(this, "Database connection is not available.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            String sql = "UPDATE Items SET IsArchived = FALSE, UpdatedAt = NOW() WHERE ItemID = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, currentItemId);
                int rowsAffected = pstmt.executeUpdate();
                if (rowsAffected > 0) {
                    JOptionPane.showMessageDialog(this, "Item '" + itemName + "' restored successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
                    logActivity("Item Restored", "Item '" + itemName + "' (ID: " + currentItemId + ") restored by user " + (currentUser != null ? currentUser.getUsername() : "Unknown"));
                    isArchived = false; // Update dialog state
                    setFieldsEditable(true); // Make fields editable again after restoring
                    saveButton.setVisible(true);
                    archiveButton.setVisible(true);
                    restoreButton.setVisible(false);
                     setTitle("Edit Item Details");

                    if (listener != null) {
                        listener.itemSavedOrArchived(); // Notify listener on restore
                    }
                    // Optionally close the dialog after restoring
                    // dispose();
                } else {
                    JOptionPane.showMessageDialog(this, "Item not found or could not be restored.", "Error", JOptionPane.WARNING_MESSAGE);
                }
            } catch (SQLException e) {
                 JOptionPane.showMessageDialog(this, "Database error during restoring: " + e.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        }
    }


    private int getCategoryId(String categoryName) {
        if (conn == null || categoryName == null || categoryName.trim().isEmpty()) {
            return -1;
        }
        String sql = "SELECT CategoryID FROM Categories WHERE CategoryName = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, categoryName);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("CategoryID");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting CategoryID for '" + categoryName + "': " + e.getMessage());
        }
        return -1;
    }

    private void browseImage() {
        // Only allow browsing if the fields are editable (i.e., not an archived item being viewed)
        if (!nameField.isEditable()) { // Use nameField editable state as a proxy
             JOptionPane.showMessageDialog(this, "Cannot change image for an archived item.", "Action Blocked", JOptionPane.WARNING_MESSAGE);
             return;
        }

        JFileChooser fileChooser = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Images (JPG, PNG, GIF)", "jpg", "jpeg", "png", "gif");
        fileChooser.setFileFilter(filter);
        fileChooser.setAcceptAllFileFilterUsed(false);

        int returnValue = fileChooser.showOpenDialog(this);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            selectedImageFile = fileChooser.getSelectedFile();
            String fileName = selectedImageFile.getName();
            int dotIndex = fileName.lastIndexOf('.');
            if (dotIndex > 0 && dotIndex < fileName.length() - 1) {
                selectedImageType = fileName.substring(dotIndex + 1).toLowerCase();
            } else {
                selectedImageType = null;
            }
            displaySelectedImage(selectedImageFile.getAbsolutePath());
        }
    }

    private void displaySelectedImage(String imagePath) {
        try {
            ImageIcon originalIcon = new ImageIcon(imagePath);
            Image originalImage = originalIcon.getImage();

            // Calculate scaled dimensions based on label size, with fallbacks
            int labelWidth = imageLabel.getWidth() > 0 ? imageLabel.getWidth() - 10 : 190;
            int labelHeight = imageLabel.getHeight() > 0 ? imageLabel.getHeight() - 10 : 140;

            if (labelWidth <= 0) labelWidth = 190;
            if (labelHeight <= 0) labelHeight = 140;

            Image scaledImage = originalImage.getScaledInstance(labelWidth, labelHeight, Image.SCALE_SMOOTH);
            imageLabel.setIcon(new ImageIcon(scaledImage));
            imageLabel.setText(null); // Remove text when image is displayed
        } catch (Exception e) {
            imageLabel.setIcon(null);
            imageLabel.setText("Preview Error");
            imageLabel.setForeground(Color.RED);
            selectedImageFile = null;
            selectedImageType = null;
            System.err.println("Error displaying selected image: " + e.getMessage());
        }
    }

    private void clearFields() {
        itemIdField.setText("");
        nameField.setText("");
        descriptionArea.setText("");
        if (categoryField.getItemCount() > 0) categoryField.setSelectedIndex(-1);
        quantityField.setText("");
        reorderLevelField.setText("");
        if (unitField.getItemCount() > 0) unitField.setSelectedIndex(0); else unitField.setSelectedItem("");

        isMachineryCheckBox.setSelected(false);
        machineStatusField.setSelectedItem("Not Applicable");
        itemConditionField.setSelectedItem("Not Applicable");
        locationField.setText("");
        serialNumberField.setText("");
        purchaseDateField.setDate(null);
        warrantyExpiryDateField.setDate(null);

        toggleMachineSpecificFields(false); // Reset machinery fields state

        imageLabel.setIcon(null);
        imageLabel.setText("No Image Selected");
        imageLabel.setForeground(Color.LIGHT_GRAY);
        selectedImageFile = null;
        selectedImageType = null;
    }

    private void setFieldsEditable(boolean editable) {
        // Determine the actual editable state based on whether it's a new item or if 'editable' is true
        // New items are always editable, existing items' editable state depends on the 'editable' parameter (which is based on archived status)
        boolean actualEditable = isNewItem ? true : editable;

        nameField.setEditable(actualEditable);
        descriptionArea.setEditable(actualEditable);
        categoryField.setEnabled(actualEditable); // Use setEnabled for JComboBox
        quantityField.setEditable(actualEditable);
        reorderLevelField.setEditable(actualEditable);
        unitField.setEditable(actualEditable); // JComboBox is editable, but the editor component needs to be enabled
        // Enable/disable the editor component of the JComboBox if it's editable
        if (unitField.getEditor().getEditorComponent() instanceof JTextField) {
             ((JTextField)unitField.getEditor().getEditorComponent()).setEditable(actualEditable);
             ((JTextField)unitField.getEditor().getEditorComponent()).setBackground(actualEditable ? new Color(50, 50, 50) : Color.DARK_GRAY);
             ((JTextField)unitField.getEditor().getEditorComponent()).setForeground(actualEditable ? Color.WHITE : Color.LIGHT_GRAY);
             ((JTextField)unitField.getEditor().getEditorComponent()).setCaretColor(actualEditable ? Color.WHITE : Color.LIGHT_GRAY);
        }


        isMachineryCheckBox.setEnabled(actualEditable);
        // Machine status depends on both dialog editable state and checkbox being selected
        machineStatusField.setEnabled(actualEditable && isMachineryCheckBox.isSelected());
        itemConditionField.setEnabled(actualEditable); // Use setEnabled for JComboBox
        locationField.setEditable(actualEditable);
        serialNumberField.setEditable(actualEditable);
        purchaseDateField.setEnabled(actualEditable); // Use setEnabled for JDateChooser
        warrantyExpiryDateField.setEnabled(actualEditable); // Use setEnabled for JDateChooser

        // Image browse button should also be enabled/disabled based on actualEditable
        Component[] imagePanelComponents = ((JPanel)imageLabel.getParent()).getComponents();
        for (Component comp : imagePanelComponents) {
            if (comp instanceof JButton) {
                comp.setEnabled(actualEditable);
            }
        }

        // Adjust colors based on actualEditable
        Color editableBg = new Color(50, 50, 50);
        Color nonEditableBg = Color.DARK_GRAY;
        Color editableFg = Color.WHITE;
        Color nonEditableFg = Color.LIGHT_GRAY;

        nameField.setBackground(actualEditable ? editableBg : nonEditableBg);
        nameField.setForeground(actualEditable ? editableFg : nonEditableFg);
        descriptionArea.setBackground(actualEditable ? editableBg : nonEditableBg);
        descriptionArea.setForeground(actualEditable ? editableFg : nonEditableFg);
        quantityField.setBackground(actualEditable ? editableBg : nonEditableBg);
        quantityField.setForeground(actualEditable ? editableFg : nonEditableFg);
        reorderLevelField.setBackground(actualEditable ? editableBg : nonEditableBg);
        reorderLevelField.setForeground(actualEditable ? editableFg : nonEditableFg);
        locationField.setBackground(actualEditable ? editableBg : nonEditableBg);
        locationField.setForeground(actualEditable ? editableFg : nonEditableFg);
        serialNumberField.setBackground(actualEditable ? editableBg : nonEditableBg);
        serialNumberField.setForeground(actualEditable ? editableFg : nonEditableFg);

         // The toggleMachineSpecificFields method is called separately based on the checkbox state,
         // but its internal logic also respects the enabled state set here.
    }


    private boolean validateInput() {
        String name = nameField.getText().trim();
        String quantityStr = quantityField.getText().trim();
        String reorderLevelStr = reorderLevelField.getText().trim();
        Object unitObj = unitField.getSelectedItem();

        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Item Name cannot be empty.", "Input Error", JOptionPane.WARNING_MESSAGE);
            nameField.requestFocusInWindow(); // Use requestFocusInWindow for dialogs
            return false;
        }
        if (categoryField.getSelectedIndex() < 0 && categoryField.getItemCount() > 0) {
            JOptionPane.showMessageDialog(this, "Please select a category.", "Input Error", JOptionPane.WARNING_MESSAGE);
            categoryField.requestFocusInWindow();
            return false;
        } else if (categoryField.getItemCount() == 0) {
             JOptionPane.showMessageDialog(this, "No categories available. Please add a category first.", "Input Error", JOptionPane.WARNING_MESSAGE);
            return false;
        }

        if (quantityStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Quantity cannot be empty.", "Input Error", JOptionPane.WARNING_MESSAGE);
            quantityField.requestFocusInWindow();
            return false;
        }
        try {
            int quantity = Integer.parseInt(quantityStr);
            if (quantity < 0) {
                JOptionPane.showMessageDialog(this, "Quantity cannot be negative.", "Input Error", JOptionPane.WARNING_MESSAGE);
                quantityField.requestFocusInWindow();
                return false;
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid Quantity format. Must be a number.", "Input Error", JOptionPane.WARNING_MESSAGE);
            quantityField.requestFocusInWindow();
            return false;
        }

        if (unitObj == null || unitObj.toString().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Unit cannot be empty.", "Input Error", JOptionPane.WARNING_MESSAGE);
            unitField.requestFocusInWindow();
            return false;
        }

        if (reorderLevelField.isEnabled() && reorderLevelStr.isEmpty()) { // Only validate if enabled
            JOptionPane.showMessageDialog(this, "Reorder Level cannot be empty.", "Input Error", JOptionPane.WARNING_MESSAGE);
            reorderLevelField.requestFocusInWindow();
            return false;
        }
        if (reorderLevelField.isEnabled()) { // Only validate format if enabled
            try {
                int reorderLevel = Integer.parseInt(reorderLevelStr);
                if (reorderLevel < 0) {
                    JOptionPane.showMessageDialog(this, "Reorder Level cannot be negative.", "Input Error", JOptionPane.WARNING_MESSAGE);
                    reorderLevelField.requestFocusInWindow();
                    return false;
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Invalid Reorder Level format. Must be a number.", "Input Error", JOptionPane.WARNING_MESSAGE);
                reorderLevelField.requestFocusInWindow();
                return false;
            }
        }


        Date purchaseDateVal = purchaseDateField.getDate();
        Date warrantyDateVal = warrantyExpiryDateField.getDate();

        if (purchaseDateVal != null && warrantyDateVal != null && purchaseDateVal.after(warrantyDateVal)) {
            JOptionPane.showMessageDialog(this, "Purchase Date cannot be after Warranty Expiry Date.", "Input Error", JOptionPane.WARNING_MESSAGE);
            purchaseDateField.requestFocusInWindow();
            return false;
        }

        return true;
    }

     private void logActivity(String activityType, String details) {
        if (conn == null || currentUser == null || currentUser.getUserId() <= 0) {
            System.err.println("Cannot log activity: DB connection null or invalid UserID (" + (currentUser != null ? currentUser.getUserId() : "null") + ")");
            return;
        }
        String sql = "INSERT INTO RecentActivities (ActivityType, UserID, UserName, Details, ActivityDate) VALUES (?, ?, ?, ?, NOW())";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, activityType);
            pstmt.setInt(2, currentUser.getUserId());
            pstmt.setString(3, currentUser.getUsername());
            pstmt.setString(4, details);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error logging activity: " + e.getMessage());
        }
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
