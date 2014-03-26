/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.eas.designer.explorer.platform;

import com.eas.designer.explorer.actions.GotoAction;
import javax.swing.JFileChooser;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.util.NbBundle;

/**
 *
 * @author vv
 */
public class PlatformDirectoryPanel extends javax.swing.JPanel {

    /**
     * Creates new form PlatformDirectoryPanel
     */
    public PlatformDirectoryPanel() {
        initComponents();
    }
    
    public PlatformDirectoryPanel(String aPath) {
        this();
        pathTextField.setText(aPath);
        setErrorMsg();
        pathTextField.getDocument().addDocumentListener(new DocumentListener() {

            @Override
            public void insertUpdate(DocumentEvent e) {
                setErrorMsg();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                setErrorMsg();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                setErrorMsg();
            }
        });
    }
    
    private void setErrorMsg() {
        if (pathTextField.getText() == null || pathTextField.getText().isEmpty()) {
            lblError.setText(NbBundle.getMessage(PlatformDirectoryPanel.class, "LBL_Platform_Home_Not_Set")); //NOI18N
        } else {
            lblError.setText(""); //NOI18N
        }
    }
        
    protected String getDirectoryPath() {
        return pathTextField.getText();
    }
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        pathTextField = new javax.swing.JTextField();
        pathLabel = new javax.swing.JLabel();
        choosePathButton = new javax.swing.JButton();
        lblError = new javax.swing.JLabel();

        org.openide.awt.Mnemonics.setLocalizedText(pathLabel, org.openide.util.NbBundle.getMessage(PlatformDirectoryPanel.class, "PlatformDirectoryPanel.pathLabel.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(choosePathButton, "...");
        choosePathButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                choosePathButtonActionPerformed(evt);
            }
        });

        lblError.setForeground(new java.awt.Color(255, 0, 0));
        org.openide.awt.Mnemonics.setLocalizedText(lblError, "Error message"); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(pathLabel)
                        .addGap(15, 15, 15)
                        .addComponent(pathTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 257, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(choosePathButton))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(lblError)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(pathLabel)
                    .addComponent(pathTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(choosePathButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lblError)
                .addContainerGap(19, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void choosePathButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_choosePathButtonActionPerformed
        final JFileChooser fc = new JFileChooser(pathTextField.getText());
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (fc.showDialog(this, NbBundle.getMessage(PlatformDirectoryPanel.class, "CTL_Select_Directory")) == JFileChooser.APPROVE_OPTION) {
            pathTextField.setText(fc.getSelectedFile().getAbsolutePath());
        }
    }//GEN-LAST:event_choosePathButtonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton choosePathButton;
    private javax.swing.JLabel lblError;
    private javax.swing.JLabel pathLabel;
    private javax.swing.JTextField pathTextField;
    // End of variables declaration//GEN-END:variables
}
