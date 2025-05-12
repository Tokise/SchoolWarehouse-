package modules;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.util.Date; // Import java.util.Date
import java.text.SimpleDateFormat;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;


public class ReceiptDialog extends JDialog {

    private JLabel titleLabel;
    private JTextArea receiptArea;
    private JButton printButton;
    private JButton closeButton;

    private int itemId;
    private String itemName;
    private int quantity;
    private String borrowerName;
    private String purpose;
    private Date expectedReturnDate;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private final SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");


    public ReceiptDialog(java.awt.Frame parent, boolean modal, int itemId, String itemName, int quantity, String borrowerName, String purpose, Date expectedReturnDate) {
        super(parent, modal);
        this.itemId = itemId;
        this.itemName = itemName;
        this.quantity = quantity;
        this.borrowerName = borrowerName;
        this.purpose = purpose;
        this.expectedReturnDate = expectedReturnDate;

        initComponents();
        setupDialog();
        generateReceiptContent();
    }

    private void setupDialog() {
        setTitle("Borrow Receipt");
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        setBackground(new Color(30, 30, 30));
        setResizable(true); // Allow resizing

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        topPanel.setOpaque(false);
        titleLabel = new JLabel("Item Borrow Receipt", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Verdana", Font.BOLD, 18));
        titleLabel.setForeground(Color.WHITE);
        topPanel.add(titleLabel);
        add(topPanel, BorderLayout.NORTH);

        receiptArea = new JTextArea();
        receiptArea.setEditable(false);
        receiptArea.setFont(new Font("Courier New", Font.PLAIN, 12)); // Use a monospaced font for receipt
        receiptArea.setBackground(new Color(50, 50, 50));
        receiptArea.setForeground(Color.WHITE);
        receiptArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // Add padding

        JScrollPane scrollPane = new JScrollPane(receiptArea);
        scrollPane.getViewport().setBackground(new Color(50, 50, 50));
        scrollPane.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = createButtonPanel();
        add(buttonPanel, BorderLayout.SOUTH);

        setPreferredSize(new Dimension(400, 500)); // Initial size
        pack();
        setLocationRelativeTo(getParent()); // Center the dialog
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        panel.setOpaque(false);

        printButton = new JButton("Print Receipt");
        styleActionButton(printButton, new Color(52, 152, 219)); // Blue for Print
        printButton.addActionListener((ActionEvent e) -> printReceipt());
        panel.add(printButton);

        closeButton = new JButton("Close");
        styleActionButton(closeButton, new Color(149, 165, 166)); // Gray for Close
        closeButton.addActionListener((ActionEvent e) -> dispose()); // Close the dialog
        panel.add(closeButton);

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


    private void generateReceiptContent() {
        StringBuilder receipt = new StringBuilder();
        receipt.append("========================================\n");
        receipt.append("       AssetWise Academia Warehouse\n");
        receipt.append("            Item Borrow Receipt\n");
        receipt.append("========================================\n");
        receipt.append("Transaction Date: ").append(dateTimeFormat.format(new Date())).append("\n");
        receipt.append("----------------------------------------\n");
        receipt.append(String.format("%-15s: %s\n", "Item ID", itemId));
        receipt.append(String.format("%-15s: %s\n", "Item Name", itemName));
        receipt.append(String.format("%-15s: %d\n", "Quantity", quantity));
        receipt.append("----------------------------------------\n");
        receipt.append(String.format("%-15s: %s\n", "Borrowed By", borrowerName));
        receipt.append(String.format("%-15s: %s\n", "Purpose", purpose));
        receipt.append(String.format("%-15s: %s\n", "Expected Return", expectedReturnDate != null ? dateFormat.format(expectedReturnDate) : "N/A"));
        receipt.append("========================================\n");
        receipt.append("Please return the item(s) by the\n");
        receipt.append("expected return date. Thank you!\n");
        receipt.append("========================================\n");


        receiptArea.setText(receipt.toString());
    }

    private void printReceipt() {
        PrinterJob printerJob = PrinterJob.getPrinterJob();
        printerJob.setPrintable(new Printable() {
            @Override
            public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {
                if (pageIndex > 0) {
                    return Printable.NO_SUCH_PAGE;
                }

                Graphics2D g2d = (Graphics2D) graphics;
                g2d.translate(pageFormat.getImageableX(), pageFormat.getImageableY());

                // Print the content of the JTextArea
                receiptArea.printAll(g2d);

                return Printable.PAGE_EXISTS;
            }
        });

        boolean doPrint = printerJob.printDialog();
        if (doPrint) {
            try {
                printerJob.print();
            } catch (PrinterException e) {
                JOptionPane.showMessageDialog(this, "Error during printing: " + e.getMessage(), "Printing Error", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
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
