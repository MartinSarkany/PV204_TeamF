package enotes;

import enotes.smartcard.CardCommunication;
import javax.swing.JOptionPane;


/**
 *
 * @author Sandeep
 */
public class ChangePINDialog extends javax.swing.JFrame {

    /**
     * Creates new form ChangePINDialog
     */
    public ChangePINDialog() {
        initComponents();
        NewPIN_jPasswordField.setText("");
        CurrentPIN_jPasswordField.setText("");
        ReenterNewPIN_jPasswordField.setText("");
        
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        NewPIN_jLabel = new javax.swing.JLabel();
        ReenterNewPIN_jLabel = new javax.swing.JLabel();
        NewPIN_jPasswordField = new javax.swing.JPasswordField();
        ReenterNewPIN_jPasswordField = new javax.swing.JPasswordField();
        ChangePINOK_jButton = new javax.swing.JButton();
        ChangePINCancel_jButton = new javax.swing.JButton();
        CurrentPIN_jLabel = new javax.swing.JLabel();
        CurrentPIN_jPasswordField = new javax.swing.JPasswordField();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Change PIN");
        setIconImages(null);
        setLocation(new java.awt.Point(750, 350));

        NewPIN_jLabel.setText("Enter New PIN");

        ReenterNewPIN_jLabel.setText("Re-Enter New PIN");

        NewPIN_jPasswordField.setText("jPasswordField1");

        ReenterNewPIN_jPasswordField.setText("jPasswordField2");

        ChangePINOK_jButton.setText("OK");
        ChangePINOK_jButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ChangePINOK_jButtonActionPerformed(evt);
            }
        });

        ChangePINCancel_jButton.setText("Cancel");
        ChangePINCancel_jButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ChangePINCancel_jButtonActionPerformed(evt);
            }
        });

        CurrentPIN_jLabel.setText("Current PIN");

        CurrentPIN_jPasswordField.setText("jPasswordField3");
        CurrentPIN_jPasswordField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                CurrentPIN_jPasswordFieldActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(ReenterNewPIN_jLabel)
                            .addComponent(CurrentPIN_jLabel)
                            .addComponent(NewPIN_jLabel))
                        .addGap(18, 18, 18)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(CurrentPIN_jPasswordField, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(NewPIN_jPasswordField, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(ReenterNewPIN_jPasswordField, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(40, 40, 40)
                        .addComponent(ChangePINOK_jButton)
                        .addGap(62, 62, 62)
                        .addComponent(ChangePINCancel_jButton)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(CurrentPIN_jLabel)
                    .addComponent(CurrentPIN_jPasswordField, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(NewPIN_jLabel)
                    .addComponent(NewPIN_jPasswordField, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(ReenterNewPIN_jLabel)
                    .addComponent(ReenterNewPIN_jPasswordField, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(ChangePINOK_jButton)
                    .addComponent(ChangePINCancel_jButton))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void ChangePINOK_jButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ChangePINOK_jButtonActionPerformed
        // TODO add your handling code here:
        //String pin1 = new String(CurrentPIN_jPasswordField.getPassword());
        String pin2 = new String(NewPIN_jPasswordField.getPassword());
        String pin3 = new String(ReenterNewPIN_jPasswordField.getPassword());
        if (!pin2.equals(pin3)) {
            JOptionPane.showMessageDialog(this, "The PINs do not match!");
            return;
        }
        
        char[] CurrentPIN_in_Array = CurrentPIN_jPasswordField.getPassword();
         
        byte[] currentPin = new byte[CurrentPIN_in_Array.length];

	for (int m = 0; m < currentPin.length; m++) {
	  currentPin[m] = (byte) (CurrentPIN_in_Array[m] - 48);
	 }
        if(!CardCommunication.verifyPIN(currentPin)){   
            JOptionPane.showMessageDialog(this, "PIN Verification Failed !");
            return;
        }
        
        //new pin
        char[] NewPIN_in_Array = NewPIN_jPasswordField.getPassword();
         
        byte[] newPin = new byte[NewPIN_in_Array.length];

	for (int m = 0; m < newPin.length; m++) {
	  newPin[m] = (byte) (NewPIN_in_Array[m] - 48);
	 }
        if(!CardCommunication.changePIN(newPin)){   
            JOptionPane.showMessageDialog(this, "Changing PIN Fails!");
            return;
        }
        JOptionPane.showMessageDialog(this, "PIN Changed !");
        this.setVisible(false);
        
    }//GEN-LAST:event_ChangePINOK_jButtonActionPerformed

    private void ChangePINCancel_jButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ChangePINCancel_jButtonActionPerformed
        this.setVisible(false);
    }//GEN-LAST:event_ChangePINCancel_jButtonActionPerformed

    private void CurrentPIN_jPasswordFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_CurrentPIN_jPasswordFieldActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_CurrentPIN_jPasswordFieldActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(ChangePINDialog.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(ChangePINDialog.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(ChangePINDialog.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(ChangePINDialog.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new ChangePINDialog().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton ChangePINCancel_jButton;
    private javax.swing.JButton ChangePINOK_jButton;
    private javax.swing.JLabel CurrentPIN_jLabel;
    private javax.swing.JPasswordField CurrentPIN_jPasswordField;
    private javax.swing.JLabel NewPIN_jLabel;
    private javax.swing.JPasswordField NewPIN_jPasswordField;
    private javax.swing.JLabel ReenterNewPIN_jLabel;
    private javax.swing.JPasswordField ReenterNewPIN_jPasswordField;
    // End of variables declaration//GEN-END:variables
}
