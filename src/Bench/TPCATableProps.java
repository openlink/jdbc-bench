/*
 *  $Id$
 *
 *  jdbc-bench - a TPC-A and TPC-C like benchmark program for JDBC drivers
 *
 *  Copyright (C) 2000-2020 OpenLink Software <jdbc-bench@openlinksw.com>
 *  All Rights Reserved.
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; only Version 2 of the License dated
 *  June 1991.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 */

package Bench;

import java.awt.*;
import java.awt.event.*;


import javax.swing.*;
import javax.swing.border.*;


public class TPCATableProps extends JDialog {
  JPanel panel1 = new JPanel();
  BorderLayout borderLayout0 = new BorderLayout();
  JPanel jCenter = new JPanel();
  JPanel jPanel_Button = new JPanel();

  BorderLayout borderLayout1 = new BorderLayout();
  JPanel jPanel_Left = new JPanel();
  JPanel jPanel_Right = new JPanel();
  BorderLayout borderLayout2 = new BorderLayout();
  JPanel jPanel2 = new JPanel();
  JPanel jPanel3 = new JPanel();
  GridLayout gridLayout1 = new GridLayout();
  GridLayout gridLayout2 = new GridLayout();
  BorderLayout borderLayout3 = new BorderLayout();
  JPanel jPanel1 = new JPanel();
  JPanel jPanel4 = new JPanel();
  JPanel jPanel5 = new JPanel();
  GridLayout gridLayout3 = new GridLayout();
  BorderLayout borderLayout4 = new BorderLayout();
  JPanel jPanel6 = new JPanel();
  JPanel jPanel7 = new JPanel();
  GridLayout gridLayout4 = new GridLayout();
  GridLayout gridLayout5 = new GridLayout();
  JLabel jLabel3 = new JLabel();
  JLabel jLabel1 = new JLabel();
  JLabel jLabel2 = new JLabel();
  JLabel jLabel5 = new JLabel();

  TitledBorder titledBorder_CTables = new TitledBorder("Create Tables");
  TitledBorder titledBorder_LTables = new TitledBorder("Load Tables");
  TitledBorder titledBorder_Indexes = new TitledBorder("Indexes");
  TitledBorder titledBorder_Records = new TitledBorder("Records to insert");

  JCheckBox createBranch = new JCheckBox("    Branch", true);
  JCheckBox createHistory = new JCheckBox("    History", true);
  JCheckBox createAccount = new JCheckBox("    Account", true);
  JCheckBox createTeller = new JCheckBox("    Teller", true);

  JCheckBox loadBranch = new JCheckBox("  Branch", true);
  JCheckBox loadAccount = new JCheckBox("  Account", true);
  JCheckBox loadTeller = new JCheckBox("  Teller", true);

  JCheckBox createProcedures = new JCheckBox("Create Procedures", true);
  JCheckBox createIndexes = new JCheckBox("Create Indexes", true);

  JTextField maxBranch = new JTextField();
  JTextField maxTeller = new JTextField();
  JTextField maxAccount = new JTextField();
  Driver drv[] = Driver.DriverMap;
  JComboBox driverType = new JComboBox(drv);
  JButton bCancel = new JButton();
  JButton bOk = new JButton();
  FlowLayout flowLayout1 = new FlowLayout();
  LoginData data;

  public TPCATableProps(Frame frame, String title, boolean modal, LoginData _data) {
    super(frame, title, modal);
    try {
      jbInit();
      pack();

      data = _data;
      driverType.setSelectedItem(data.m_Driver);
      maxBranch.setText(String.valueOf(data.tpca.nMaxBranch));
      maxTeller.setText(String.valueOf(data.tpca.nMaxTeller));
      maxAccount.setText(String.valueOf(data.tpca.nMaxAccount));

    }
    catch(Exception ex) {
      ex.printStackTrace();
    }
  }


  void jbInit() throws Exception {
    panel1.setLayout(borderLayout0);
    bCancel.setSelected(true);
    bCancel.setText("Cancel");
    bCancel.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        bCancel_actionPerformed(e);
      }
    });
    bOk.setText("   Ok   ");
    bOk.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        bOk_actionPerformed(e);
      }
    });
    jPanel_Button.setLayout(flowLayout1);
    flowLayout1.setHgap(10);
    flowLayout1.setVgap(10);
    getContentPane().add(panel1);
    panel1.add(jCenter, BorderLayout.CENTER);
    this.getContentPane().add(jPanel_Button, BorderLayout.SOUTH);
    jPanel_Button.add(bOk, null);
    jPanel_Button.add(bCancel, null);

    titledBorder_LTables.setBorder(BorderFactory.createEtchedBorder());
    titledBorder_Indexes.setBorder(BorderFactory.createEtchedBorder());
    titledBorder_Records.setBorder(BorderFactory.createEtchedBorder());
    titledBorder_CTables.setBorder(BorderFactory.createEtchedBorder());

    borderLayout1.setHgap(10);
    borderLayout1.setVgap(10);
    jCenter.setLayout(borderLayout1);
    jPanel_Left.setLayout(borderLayout2);
    borderLayout2.setHgap(10);
    borderLayout2.setVgap(10);
    jPanel2.setLayout(gridLayout1);
    jPanel3.setLayout(gridLayout2);
    gridLayout1.setRows(4);
    gridLayout2.setRows(3);
    jPanel2.setBorder(titledBorder_CTables);
    jPanel3.setBorder(titledBorder_LTables);
    jPanel_Right.setLayout(borderLayout3);
    borderLayout3.setHgap(10);
    borderLayout3.setVgap(10);
    jPanel1.setBorder(titledBorder_Indexes);
    jPanel4.setBorder(titledBorder_Records);
    jPanel4.setLayout(borderLayout4);
    jPanel5.setLayout(gridLayout3);
    gridLayout3.setRows(2);
    gridLayout3.setColumns(1);
    jPanel6.setLayout(gridLayout4);
    jPanel7.setLayout(gridLayout5);
    gridLayout4.setRows(3);
    gridLayout4.setColumns(1);
    gridLayout5.setRows(3);
    gridLayout5.setColumns(1);
    jLabel3.setText("Number of Branches");
    jLabel1.setText("Number of Tellers");
    jLabel2.setText("Number of Accounts");
    jLabel5.setText("Schema for DBMS");
    borderLayout4.setHgap(10);
    borderLayout4.setVgap(10);
    driverType.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        driverType_actionPerformed(e);
      }
    });
    jCenter.add(jPanel_Left, BorderLayout.WEST);
    jPanel_Left.add(jPanel2, BorderLayout.CENTER);
    jPanel2.add(createBranch, null);
    jPanel2.add(createTeller, null);
    jPanel2.add(createAccount, null);
    jPanel2.add(createHistory, null);
    jPanel_Left.add(jPanel3, BorderLayout.SOUTH);
    jPanel3.add(loadBranch, null);
    jPanel3.add(loadTeller, null);
    jPanel3.add(loadAccount, null);
    jCenter.add(jPanel_Right, BorderLayout.CENTER);
    jPanel_Right.add(jPanel1, BorderLayout.NORTH);
    jPanel1.add(createIndexes, null);
    jPanel1.add(createProcedures, null);
    jPanel_Right.add(jPanel4, BorderLayout.CENTER);
    jPanel4.add(jPanel6, BorderLayout.WEST);
    jPanel6.add(jLabel3, null);
    jPanel6.add(jLabel1, null);
    jPanel6.add(jLabel2, null);
    jPanel4.add(jPanel7, BorderLayout.CENTER);
    jPanel7.add(maxBranch, null);
    jPanel7.add(maxTeller, null);
    jPanel7.add(maxAccount, null);
    jPanel_Right.add(jPanel5, BorderLayout.SOUTH);
    jPanel5.add(jLabel5, null);
    jPanel5.add(driverType, null);

  }

  void driverType_actionPerformed(ActionEvent e) {
     JComboBox driver = (JComboBox)e.getSource();
     if (Driver.getDriver(driver.getSelectedIndex()).m_strProcedure != null) {
        createProcedures.setSelected(true);
        createProcedures.setEnabled(true);
     } else {
        createProcedures.setSelected(false);
        createProcedures.setEnabled(false);
     }
  }

  void bOk_actionPerformed(ActionEvent e) {

    if (data != null) {
       int nMaxBranch = 0;
       int nMaxTeller = 0;
       int nMaxAccount = 0;
       int selDriverIndex = driverType.getSelectedIndex();

       try {
         if (selDriverIndex != -1)
           data.setDriver(selDriverIndex);
         nMaxBranch = Integer.valueOf(maxBranch.getText()).intValue();
         nMaxTeller = Integer.valueOf(maxTeller.getText()).intValue();
         nMaxAccount = Integer.valueOf(maxAccount.getText()).intValue();
       } catch (Exception ex) {}
       if (nMaxBranch < 0)
         nMaxBranch = BenchPanel.m_nMaxBranch;
       if (nMaxTeller < 0)
         nMaxTeller = BenchPanel.m_nMaxTeller;
       if (nMaxAccount < 0)
         nMaxAccount = BenchPanel.m_nMaxAccount;

       data.setTableDetails(createBranch.isSelected(),
                     createTeller.isSelected(),
                     createAccount.isSelected(),
                     createHistory.isSelected(),
                     loadBranch.isSelected(),
                     loadTeller.isSelected(),
                     loadAccount.isSelected(),
                     createIndexes.isSelected(),
                     createProcedures.isSelected(),
                     nMaxBranch,
                     nMaxTeller,
                     nMaxAccount);

    }
    this.dispose();
  }

  void bCancel_actionPerformed(ActionEvent e) {
    this.dispose();
  }

}
