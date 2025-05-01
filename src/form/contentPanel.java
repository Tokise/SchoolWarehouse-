
package form;

import java.awt.BorderLayout;
import javax.swing.JPanel;

public class contentPanel extends javax.swing.JPanel {

   
    public contentPanel() {
        initComponents();
        this.setSize(864, 644);
         setLayout(new BorderLayout());
        
    }
    
    public void showForm(JPanel form) {
        removeAll();
        add(form, BorderLayout.CENTER);
        form.setVisible(true); // Ensure the form is visible
        repaint();
        revalidate();
    }
    
    
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        setPreferredSize(new java.awt.Dimension(834, 644));

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
