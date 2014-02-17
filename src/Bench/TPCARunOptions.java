/*
 *  $Id$
 *
 *  jdbc-bench - a TPC-A and TPC-C like benchmark program for JDBC drivers
 *
 *  Copyright (C) 2000-2014 OpenLink Software <jdbc-bench@openlinksw.com>
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
import java.util.Enumeration;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Connection;
import java.awt.event.*;


import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;



public class TPCARunOptions extends JDialog {
  JPanel panel1 = new JPanel();
  BorderLayout borderLayout1 = new BorderLayout();
  JPanel jCenter = new JPanel();
  JPanel jPanel2 = new JPanel();

  LoginData data;
  DatabaseMetaData md;
  TitledBorder titledBorder1_ThreadOptions = new TitledBorder("Threading Options");
  TitledBorder titledBorder2_RunOptions = new TitledBorder("Run Options");
  TitledBorder titledBorder_BatchOptions = new TitledBorder("Batch Optimization");
  TitledBorder titledBorder1_SQLOptions = new TitledBorder("SQL Options");
  TitledBorder titledBorder1_TransOptions = new TitledBorder("Transaction Isolation Levels");
  TitledBorder titledBorder1_ExecOptions = new TitledBorder("Execution Options");
  TitledBorder titledBorder1_ScrlOptions = new TitledBorder("Scrollable ResultSets");
  BorderLayout borderLayout3 = new BorderLayout();
  JPanel jPanel_North = new JPanel();
  GridLayout gridLayout1 = new GridLayout();
  ButtonGroup sqlOptions = new ButtonGroup();
  ButtonGroup threadingOptions = new ButtonGroup();
  ButtonGroup transOptions = new ButtonGroup();
  ButtonGroup scrsOptions = new ButtonGroup();
  JPanel jPanel_Left = new JPanel();
  JPanel jPanel_Left0 = new JPanel();
  JPanel jPanel_Left1 = new JPanel();
  JPanel jPanel_Right = new JPanel();
  JPanel jPanel_Right0 = new JPanel();
  GridLayout gridLayout3 = new GridLayout();
  GridLayout gridLayout4 = new GridLayout();
  JPanel jpn_Batch = new JPanel();
  JRadioButton R_txnReadComm = new JRadioButton("Read committed             ");
  JRadioButton R_txnSerial = new JRadioButton("Serializable              ");
  JPanel jpn_Trans = new JPanel();
  GridLayout gridLayout7 = new GridLayout();
  JRadioButton R_txnDriverDef = new JRadioButton("Driver Default              ", true);
  JRadioButton R_txnRepeatRead = new JRadioButton("Repeatable read              ");
  JRadioButton R_txnReadUncomm = new JRadioButton("Read uncommitted             ");
  JPanel jPanel1 = new JPanel();
  JPanel jpn_Scrs = new JPanel();
  JTextField traversalCount = new JTextField("1");
  GridLayout gridLayout8 = new GridLayout();
  JRadioButton R_scScrollSens = new JRadioButton("Scroll Sensitive");
  JRadioButton R_scScrollInsens = new JRadioButton("Scroll Insensitive");
  BorderLayout borderLayout4 = new BorderLayout();
  JRadioButton R_scNone = new JRadioButton("None (forward only)", true);
  JLabel jLabel4 = new JLabel();
  JRadioButton R_singleThreaded = new JRadioButton("Single thread", true);
  JPanel jpn_Thread = new JPanel();
  BorderLayout borderLayout2 = new BorderLayout();
  GridLayout gridLayout2 = new GridLayout();
  JPanel jPanel7 = new JPanel();
  JTextField numThreads = new JTextField();
  JLabel jLabel3 = new JLabel();
  JRadioButton R_multiThreaded = new JRadioButton("Multi thread");
  JRadioButton R_prepare = new JRadioButton("Prepare/Execute, bound params");
  JPanel jpn_SQL = new JPanel();
  JRadioButton R_execDirect = new JRadioButton("ExecDirect with SQLtext", true);
  JRadioButton R_sproc = new JRadioButton("Use Stored Procedures");
  GridLayout gridLayout5 = new GridLayout();
  JCheckBox C_Trans = new JCheckBox("Use Transactions");
  GridLayout gridLayout6 = new GridLayout();
  JPanel jpn_Exec = new JPanel();
  JCheckBox C_Query = new JCheckBox("Do 100 row query");
  JButton bCancel = new JButton();
  JButton bOk = new JButton();
  BorderLayout borderLayout5 = new BorderLayout();
  BorderLayout borderLayout6 = new BorderLayout();
  JCheckBox C_Batch = new JCheckBox();
  GridLayout gridLayout9 = new GridLayout();
  GridLayout gridLayout10 = new GridLayout();
  GridLayout gridLayout11 = new GridLayout();
  JPanel jPanel3 = new JPanel();
  BorderLayout borderLayout7 = new BorderLayout();
  JLabel jLabel1 = new JLabel();
  JTextField nBatchSize = new JTextField();
  GridBagLayout gridBagLayout1 = new GridBagLayout();


  public TPCARunOptions(Frame frame, String title, boolean modal, LoginData _data) {
    super(frame, title, modal);
    data = _data;
    try {
      jbInit();
      pack();

      BEOptListener listener0 = new BEOptListener(numThreads, R_multiThreaded, null, R_singleThreaded);
      numThreads.setEnabled(false);
      numThreads.setBackground(UIManager.getColor("control"));

      BEOptListener listener1 = new BEOptListener(traversalCount, R_scScrollSens, R_scScrollInsens, R_scNone);
      traversalCount.setEnabled(false);
      traversalCount.setBackground(UIManager.getColor("control"));

      BEOptListener listener2 = new BEOptListener(traversalCount, R_scScrollSens, R_scScrollInsens, R_scNone);
      traversalCount.setEnabled(false);
      traversalCount.setBackground(UIManager.getColor("control"));

      BEOptListenerC listener3 = new BEOptListenerC(nBatchSize, C_Batch);
      nBatchSize.setEnabled(false);
      nBatchSize.setBackground(UIManager.getColor("control"));

      R_scNone.setEnabled(false);
      R_scScrollInsens.setEnabled(false);
      R_scScrollSens.setEnabled(false);

      try {
        if (data.conn != null)
          md = data.conn.getMetaData();
      } catch (SQLException e) { }

      if (md != null)
        try {
          if (! md.supportsTransactionIsolationLevel(Connection.TRANSACTION_READ_UNCOMMITTED))
            R_txnReadUncomm.setEnabled(false);
          if (! md.supportsTransactionIsolationLevel(Connection.TRANSACTION_READ_COMMITTED))
            R_txnReadComm.setEnabled(false);
          if (! md.supportsTransactionIsolationLevel(Connection.TRANSACTION_REPEATABLE_READ))
            R_txnRepeatRead.setEnabled(false);
          if (! md.supportsTransactionIsolationLevel(Connection.TRANSACTION_SERIALIZABLE))
            R_txnSerial.setEnabled(false);

          if (! md.supportsBatchUpdates())
            C_Batch.setEnabled(false);


        } catch (SQLException e) { }

     if (data != null) {

        if (data.tpca.nThreads > 1) {
            numThreads.setText(Integer.toString(data.tpca.nThreads));
            R_multiThreaded.doClick();
        }

        traversalCount.setText(Integer.toString(data.tpca.travCount));

        if (data.tpca.bQuery)
          C_Query.doClick();

        C_Trans.setSelected(data.tpca.bTrans);

        switch(data.tpca.sqlOption) {
          case TPCABench.RUN_PREPARED:
            R_prepare.setSelected(true);  break;
          case TPCABench.RUN_SPROC:
            R_sproc.setSelected(true);     break;
          default:
            R_execDirect.setSelected(true);   break;
        }

        switch(data.tpca.txnOption) {
          case Connection.TRANSACTION_READ_UNCOMMITTED:
            R_txnReadUncomm.setSelected(true);  break;
          case Connection.TRANSACTION_READ_COMMITTED:
            R_txnReadComm.setSelected(true);    break;
          case Connection.TRANSACTION_REPEATABLE_READ:
            R_txnRepeatRead.setSelected(true);  break;
          case Connection.TRANSACTION_SERIALIZABLE:
            R_txnSerial.setSelected(true);      break;
          default:
            R_txnDriverDef.setSelected(true);   break;
        }


        switch(data.tpca.scrsOption) {
          case ResultSet.TYPE_FORWARD_ONLY: R_scNone.doClick();  break;
          case ResultSet.TYPE_SCROLL_INSENSITIVE: R_scScrollInsens.doClick(); break;
          case ResultSet.TYPE_SCROLL_SENSITIVE: R_scScrollSens.doClick();  break;
        }

        if (data.tpca.nBatchSize > 1) {
          C_Batch.doClick();
          nBatchSize.setText(Integer.toString(data.tpca.nBatchSize));
        } else {
          nBatchSize.setText("10");
        }

     }


    }
    catch(Exception ex) {
      ex.printStackTrace();
    }
  }

  void jbInit() throws Exception {
    panel1.setLayout(borderLayout1);
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
    this.getContentPane().setLayout(borderLayout5);
    jPanel_Right0.setLayout(borderLayout6);
    C_Batch.setText("Use Batch Optimization");
    jpn_Batch.setLayout(gridLayout9);
    gridLayout9.setRows(3);
    gridLayout9.setColumns(1);
    borderLayout6.setVgap(25);
    gridLayout4.setRows(2);
    jPanel_Left0.setLayout(gridLayout10);
    gridLayout10.setRows(2);
    gridLayout10.setColumns(1);
    jPanel_Left1.setLayout(gridLayout11);
    gridLayout11.setRows(2);
    gridLayout11.setColumns(1);
    jCenter.setLayout(gridBagLayout1);
    jPanel3.setLayout(borderLayout7);
    jLabel1.setText("No. Batch Items");
    nBatchSize.setToolTipText("");
    nBatchSize.setText("10");
    borderLayout7.setHgap(10);
    getContentPane().add(panel1, BorderLayout.CENTER);
    panel1.add(jCenter, BorderLayout.CENTER);

    titledBorder2_RunOptions.setBorder(BorderFactory.createEtchedBorder());
    titledBorder_BatchOptions.setBorder(BorderFactory.createEtchedBorder());
    titledBorder1_ThreadOptions.setBorder(BorderFactory.createEtchedBorder());
    titledBorder1_SQLOptions.setBorder(BorderFactory.createEtchedBorder());
    titledBorder1_TransOptions.setBorder(BorderFactory.createEtchedBorder());
    titledBorder1_ExecOptions.setBorder(BorderFactory.createEtchedBorder());
    titledBorder1_ScrlOptions.setBorder(BorderFactory.createEtchedBorder());

    jPanel_North.setLayout(gridLayout1);
    borderLayout3.setHgap(5);
    borderLayout3.setVgap(5);
    gridLayout1.setColumns(2);
    gridLayout1.setHgap(10);
    jPanel_Left.setLayout(gridLayout3);
    jPanel_Right.setLayout(gridLayout4);
    gridLayout3.setRows(2);
    gridLayout3.setColumns(1);
    gridLayout4.setColumns(1);
    jpn_Trans.setBorder(titledBorder1_TransOptions);
    jpn_Trans.setLayout(gridLayout7);
    gridLayout7.setRows(6);
    gridLayout7.setColumns(1);
    jPanel1.setLayout(borderLayout4);
    jpn_Scrs.setBorder(titledBorder1_ScrlOptions);
    jpn_Scrs.setLayout(gridLayout8);
    gridLayout8.setRows(6);
    gridLayout8.setColumns(1);
    borderLayout4.setHgap(10);
    jLabel4.setText("Traversal Count");
    jpn_Thread.setBorder(titledBorder1_ThreadOptions);
    jpn_Thread.setLayout(gridLayout2);
    borderLayout2.setHgap(10);
    gridLayout2.setRows(3);
    jPanel7.setLayout(borderLayout2);
    numThreads.setEnabled(false);
    numThreads.setText("2");
    jLabel3.setText("No. threads");
    jpn_Batch.setBorder(titledBorder_BatchOptions);
    jpn_SQL.setBorder(titledBorder1_SQLOptions);
    jpn_SQL.setLayout(gridLayout5);
    gridLayout5.setRows(3);
    gridLayout5.setColumns(1);
    gridLayout5.setHgap(3);



    gridLayout6.setRows(3);
    gridLayout6.setColumns(1);
    gridLayout6.setHgap(3);
    jpn_Exec.setBorder(titledBorder1_ExecOptions);
    jpn_Exec.setLayout(gridLayout6);
    C_Query.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        C_Query_actionPerformed(e);
      }
    });


    jCenter.add(jPanel_Left, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0
            ,GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(13, 18, 4, 0), 2, -39));


    jPanel_Left.add(jPanel_Left0, null);
    jPanel_Left.add(jPanel_Left1, null);
    jPanel_Left0.add(jpn_Thread, null);
    jpn_Thread.add(R_singleThreaded, null);
    jpn_Thread.add(R_multiThreaded, null);
    jpn_Thread.add(jPanel7, null);
    jPanel7.add(jLabel3, BorderLayout.WEST);
    jPanel7.add(numThreads, BorderLayout.CENTER);
    jPanel_Left0.add(jpn_Batch, null);
    jpn_Batch.add(C_Batch, null);
    jpn_Batch.add(jPanel3, null);
    jPanel3.add(jLabel1, BorderLayout.WEST);
    jPanel3.add(nBatchSize, BorderLayout.CENTER);
    jPanel_Left1.add(jpn_SQL, null);
    jpn_SQL.add(R_execDirect, null);
    jpn_SQL.add(R_prepare, null);
    jpn_SQL.add(R_sproc, null);
    jPanel_Left1.add(jpn_Exec, null);
    jpn_Exec.add(C_Trans, null);
    jpn_Exec.add(C_Query, null);


    jCenter.add(jPanel_Right, new GridBagConstraints(1, 0, 1, 1, 1.0, 1.0
            ,GridBagConstraints.CENTER, GridBagConstraints.BOTH, 
            new Insets(13, 0, 26, 19), 2, -5));


    jPanel_Right.add(jpn_Trans, null);
    jpn_Trans.add(R_txnDriverDef, null);
    jpn_Trans.add(R_txnReadUncomm, null);
    jpn_Trans.add(R_txnReadComm, null);
    jpn_Trans.add(R_txnRepeatRead, null);
    jpn_Trans.add(R_txnSerial, null);
    jPanel_Right.add(jpn_Scrs, null);
    jpn_Scrs.add(R_scNone, null);
    jpn_Scrs.add(R_scScrollInsens, null);
    jpn_Scrs.add(R_scScrollSens, null);
    jpn_Scrs.add(jPanel1, null);
    jPanel1.add(jLabel4, BorderLayout.WEST);
    jPanel1.add(traversalCount, BorderLayout.CENTER);
    this.getContentPane().add(jPanel2, BorderLayout.SOUTH);
    jPanel2.add(bOk, null);
    jPanel2.add(bCancel, null);
    threadingOptions.add(R_singleThreaded);
    threadingOptions.add(R_multiThreaded);
    sqlOptions.add(R_execDirect);
    sqlOptions.add(R_prepare);
    sqlOptions.add(R_sproc);
    transOptions.add(R_txnDriverDef);
    transOptions.add(R_txnReadUncomm);
    transOptions.add(R_txnReadComm);
    transOptions.add(R_txnRepeatRead);
    transOptions.add(R_txnSerial);
    scrsOptions.add(R_scNone);
    scrsOptions.add(R_scScrollInsens);
    scrsOptions.add(R_scScrollSens);

  }

  class BEOptListener implements ActionListener {

    private JTextField field;
    private JRadioButton on_btn1, on_btn2, off_btn;

    public BEOptListener(JTextField Field, JRadioButton onBtn1,
            JRadioButton onBtn2, JRadioButton offBtn)
    {
	    field=Field; on_btn1=onBtn1; on_btn2=onBtn2; off_btn=offBtn;
            if (on_btn1 != null)
	       on_btn1.addActionListener(this);
            if (on_btn2 != null)
	       on_btn2.addActionListener(this);
            off_btn.addActionListener(this);
    }

    public void actionPerformed(ActionEvent e) {
       Object src = e.getSource();
       if (src == off_btn) {
  	  field.setEnabled(false);
	  field.setBackground(UIManager.getColor("control"));
       } else if (src == on_btn1 || src == on_btn2) {
          field.setEnabled(true);
	  field.setBackground(new Color(255,255,255));
	  }
    }
  }

  class BEOptListenerC implements ActionListener {

    private JTextField field;
    private JCheckBox on_bx;

    public BEOptListenerC(JTextField Field, JCheckBox onBx)
    {
	    field=Field; on_bx=onBx;
            if (on_bx != null)
	       on_bx.addActionListener(this);
    }

    public void actionPerformed(ActionEvent e) {
       Object src = e.getSource();
       if (src == on_bx) {
          if (on_bx.isSelected()) {
            field.setEnabled(true);
	    field.setBackground(new Color(255,255,255));
          } else {
  	    field.setEnabled(false);
	    field.setBackground(UIManager.getColor("control"));
          }
      }
    }
  }

  void C_Query_actionPerformed(ActionEvent e) {
        boolean isSelected = C_Query.isSelected();
        if (!isSelected)
           R_scNone.doClick();

        if (md != null && isSelected) {
          try {
            if (md.supportsResultSetType(ResultSet.TYPE_SCROLL_SENSITIVE))
              R_scScrollSens.setEnabled(true);
            else
              R_scScrollSens.setEnabled(false);
            if (md.supportsResultSetType(ResultSet.TYPE_SCROLL_INSENSITIVE))
              R_scScrollInsens.setEnabled(true);
            else
              R_scScrollInsens.setEnabled(false);
          } catch (SQLException ex) {}
          R_scNone.setEnabled(isSelected);
        } else {

          R_scNone.setEnabled(isSelected);
          R_scScrollInsens.setEnabled(isSelected);
          R_scScrollSens.setEnabled(isSelected);

        }

  }

  void bOk_actionPerformed(ActionEvent e) {

    if (data != null) {
       if (R_singleThreaded.isSelected())
          data.tpca.nThreads = 1;
       else
          try {
            data.tpca.nThreads = Integer.valueOf(numThreads.getText()).intValue();
          } catch(Exception ex) {
            data.tpca.nThreads = 1;
          }


       if (! C_Batch.isSelected())
          data.tpca.nBatchSize = 1;
       else
          try {
            data.tpca.nBatchSize = Integer.valueOf(nBatchSize.getText()).intValue();
          } catch(Exception ex) {
            data.tpca.nBatchSize = 1;
          }


       if (R_scNone.isSelected())
          data.tpca.travCount = 1;
       else
          try {
            data.tpca.travCount = Integer.valueOf(traversalCount.getText()).intValue();
          } catch(Exception ex) {
            data.tpca.travCount = 1;
          }

       data.tpca.sqlOption = (R_execDirect.isSelected() ? TPCABench.RUN_TEXT :
                         (R_prepare.isSelected() ? TPCABench.RUN_PREPARED :
                           (R_sproc.isSelected() ? TPCABench.RUN_SPROC : 0)));

       data.tpca.txnOption = (R_txnReadUncomm.isSelected() ? Connection.TRANSACTION_READ_UNCOMMITTED :
                        (R_txnReadComm.isSelected() ? Connection.TRANSACTION_READ_COMMITTED :
                          (R_txnRepeatRead.isSelected() ? Connection.TRANSACTION_REPEATABLE_READ :
                            (R_txnSerial.isSelected() ? Connection.TRANSACTION_SERIALIZABLE :
                                                             TPCABench.TXN_DEFAULT))));


       data.tpca.scrsOption = (R_scScrollInsens.isSelected() ? ResultSet.TYPE_SCROLL_INSENSITIVE :
                         (R_scScrollSens.isSelected() ? ResultSet.TYPE_SCROLL_SENSITIVE :
                                                             ResultSet.TYPE_FORWARD_ONLY));


       data.tpca.bTrans = (data.tpca.nThreads > 1) ? true : C_Trans.isSelected();
       data.tpca.bQuery = C_Query.isSelected();
    }
    this.hide();
  }

  void bCancel_actionPerformed(ActionEvent e) {
    this.hide();
  }


}
