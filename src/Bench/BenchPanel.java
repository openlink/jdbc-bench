/*
 *  $Id$
 *
 *  jdbc-bench - a TPC-A and TPC-C like benchmark program for JDBC drivers
 *
 *  Copyright (C) 2000-2012 OpenLink Software <jdbc-bench@openlinksw.com>
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

import java.sql.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;


import javax.swing.*;
import javax.swing.border.*;


import org.apache.crimson.tree.*;

public class BenchPanel extends JPanel {

   String PACKAGE_NAME = "OpenLink JDBC Benchmark Utility";
   String PACKAGE_VERSION = "1.0";
   String PACKAGE_BUGREPORT = "jdbc-bench@openlinksw.com";

   boolean m_bUseFiles = false;
   Frame m_parentFrame = null;
   JTextArea m_logArea = new JTextArea(20,60);
   JScrollPane m_pane = null;

   // options
   public ConnectionPool pool = new ConnectionPool();
   JTable pool_table = new JTable(pool);
   LoginData results = null;

   static final String m_logLevels[] = {"Basic", "Verbose", "SQL debug"};

   // Dynamic
   final static int m_nMaxBranch = 10, m_nMaxTeller = 100, m_nMaxAccount = 1000;
   int m_nWarehouses,m_nDistricts,m_nItems,m_nCustomers,m_nOrders;
   int nWar,nDis,mIt,nCust,nOrd,nRd;
   RunThread thr = null;

   public Vector m_tpcaBench = new Vector();
   ThreadGroup m_tests = null;
   TPCCBench m_tpccBench[];
   Logger m_logger = null;
   String m_strFileName = null, m_strNowFunction = null;
   volatile int nbTrans = 0;
   volatile int nbThreads = 0;
   boolean isBenchWorked = false;

   // SQL statements
   //
   public String makeDropTable(String strTableName) {
      return "Drop table " + strTableName;
   }

   public String makeInsertAccount(String strTableName) {
      return "insert into " + strTableName + " (account, branch, balance, filler) values (?, ?, ?, ?)";
   }

   public String makeCreateIndex(String strIndexType, String strTableName, String strFields) {
      return "create " + (strIndexType == null ? "" : strIndexType) + " index " + strTableName + "ix on " + strTableName + "(" + strFields + ")";
   }

   public static final String strFiller = "fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff";

   public BenchPanel(String strDriver, String strURL, String strUserName,
      String strPassword, int nDriverIndex) throws Exception
   {
      doOpenConnection(strDriver,strURL,strUserName,strPassword,false);
      setDriver(nDriverIndex);
   }

   public void setLogger(Logger logger) {
      m_logger = logger;
   }


   public void setDriver(int nDriverIndex) throws Exception
   {
     int selection[] = pool_table.getSelectedRows();
     if (selection == null)
	 return;
     for (int nRow = 0; nRow < selection.length; nRow++) {
	pool.getConnection(selection[nRow]).setDriver(nDriverIndex);
	pool.fireTableRowsUpdated(selection[nRow], selection[nRow]);
     }
   }

   public void closeConnection(boolean bResults) {
     if (bResults) {
	   try {
	       if (results != null) {
		   results.doLogout();
		   log("Results connection is closed\n",0);
		   results = null;
               }
           } catch(SQLException ec) {
	       log("Error closing results connection : " + ec.getMessage() + "\n",0);
           }
     } else {
	   try {
	       pool.removeLoginsFromThePool(pool_table.getSelectedRows());
           } catch(SQLException e) {
	       log("Error closing connection(s) : " + e.getMessage() + "\n",0);
           }
     }
   }

   void openConnection(boolean bResults) {
     try {
	if (bResults) {
	     if (results != null) {
	       results.doLogout();
               log("Results connection is closed\n",0);
             }
	     results = null;

	     LoginData data = new LoginData("Results");
             data.setDetails(this, "Enter RESULTS connection data", pool.getLastConnection());
             results = data;
  	     log("Results Connection to " + results.strURL + " opened\n",0);
        } else {
             int selection[] = pool_table.getSelectedRows();
             if (selection == null || selection.length != 1) {
	        log("Please select a single connection for this operation\n", 0);
	        return;
             }
             LoginData data = pool.getConnection(selection[0]);
	     data.setDetails(this, "Enter connection data", pool.getLastConnection());
 	     log("Connection to " + data.strURL + " opened\n",0);
             pool_table.repaint();
	}
     } catch(Exception e) {
	 if (e.getMessage() != "Canceled")
            JOptionPane.showMessageDialog(this, e.getMessage(), "Error connecting", JOptionPane.ERROR_MESSAGE);
     }
   }

   public void doOpenConnection(String strDriver, String strURL, String strUserName,
      String strPassword, boolean bResults) throws Exception
   {
     if (strDriver != null)
	 DriverManager.registerDriver((java.sql.Driver)Class.forName(strDriver).newInstance());

     if (bResults) {
	if (results != null) {
           results.doLogout();
	   results = null;
	}
	results = new LoginData(strURL, strUserName, strPassword);
	log("Connection to " + strURL + " opened\n",0);
     } else {
	pool.addLoginToThePool(strURL, strUserName, strPassword);
	log("Connection to " + strURL + " opened\n", 0);
     }
   }

   public BenchPanel(boolean bUseFiles, Frame parentFrame) throws Exception {

      super(new BorderLayout(10,10));
      m_bUseFiles = bUseFiles; m_parentFrame = parentFrame;
      m_logArea.setTabSize(10);
      m_logArea.setFont(new java.awt.Font("Monospaced",java.awt.Font.PLAIN,12));
      m_logArea.setEditable(false);
      setLogger(new TheLogger(m_logArea));
      JMenuBar bar = new JMenuBar();

      JMenu fileMenu = new JMenu("File");

      if(m_bUseFiles) {
         fileMenu.add(new AbstractAction("Open...")         {
            public void actionPerformed(ActionEvent e) {
              JFileChooser chooser = new JFileChooser();
              chooser.setDialogTitle("Select file to open");
              SimpleFileFilter filter = new SimpleFileFilter("xml");
              chooser.setFileFilter(filter);
              chooser.setCurrentDirectory(new File("."));
              int returnVal = chooser.showOpenDialog(BenchPanel.this);
              if(returnVal == JFileChooser.APPROVE_OPTION)
                m_strFileName = chooser.getSelectedFile().getPath();
              else
                return;

              doLoadItems(m_strFileName, true);
            }
         });
      }

      fileMenu.add(new AbstractAction("Clear log")      {
         public void actionPerformed(ActionEvent e) {
            m_logArea.setText("");
         }
      });

      if(m_bUseFiles) {
         fileMenu.add(new AbstractAction("Save as...")         {
            public void actionPerformed(ActionEvent e) {
              JFileChooser chooser = new JFileChooser();
              chooser.setDialogTitle("Select output file name");
              SimpleFileFilter filter = new SimpleFileFilter("xml");
              chooser.setFileFilter(filter);
              chooser.setCurrentDirectory(new File("."));
              int returnVal = chooser.showSaveDialog(BenchPanel.this);
              if(returnVal == JFileChooser.APPROVE_OPTION) {
                String name = chooser.getSelectedFile().getPath();
                if (name.length() <= 4 || (name.length() > 4 && (! name.regionMatches(true, name.length() - 4, ".xml", 0, 4))))
                  m_strFileName = name+".xml";
                else
                  m_strFileName = name;
              } else
                 return;

               try {
                Results res = Results.InitItemsSaving(BenchPanel.this);
                res.SaveAllItems(pool);
                XmlDocument doc = res.DoneItemsSaving();
                FileOutputStream os = new FileOutputStream(m_strFileName);
                doc.write(os);
                os.close();
              } catch (Exception ex) {
                log(ex.toString()+"\n", 0);
              }

            }
         });

         fileMenu.addSeparator();
         fileMenu.add(new AbstractAction("Exit")         {
            public void actionPerformed(ActionEvent e) {
               closeConnection(true);
               System.exit(0);
            }
         });
      }
      bar.add(fileMenu);

/*
      JMenu tpccMenu = new JMenu("TPC-C");
      tpccMenu.add(new AbstractAction("Create tables & procedures ...") {
         public void actionPerformed(ActionEvent e) {
            try {
	       int selection[] = pool_table.getSelectedRows();
               log("Creating tables & procedures for TPC-C ...",0);
	       for (int nRow = 0; nRow < selection.length; nRow++)
		 pool.getConnection(selection[nRow]).createTest("TPCC","create");
               log("Done\n",0);
            } catch(SQLException err) { log("Error creating tables & procedures : " + err.getMessage() + "\n",0); }
         } });

      tpccMenu.add(new AbstractAction("Populate tables...") {
         public void actionPerformed(ActionEvent e) { tpccLoadData(); } });

      tpccMenu.add(new AbstractAction("Run Benchmark...") {
         public void actionPerformed(ActionEvent e) { tpccRun(); } });

      tpccMenu.add(new AbstractAction("Cleanup ...") {
         public void actionPerformed(ActionEvent e) {
            try {
	       int selection[] = pool_table.getSelectedRows();
               log("Cleaning up tables & procedures for TPC-C ...",0);
	       for (int nRow = 0; nRow < selection.length; nRow++)
		 pool.getConnection(selection[nRow]).parseAndExecute("drop","TPCC");
               log("Done\n",0);
            } catch(SQLException err) { log("Error cleaning up : " + err.getMessage() + "\n",0); }
         } });

      bar.add(tpccMenu);

      JMenu tpcdMenu = new JMenu("TPC-D");
      tpcdMenu.add(new AbstractAction("Create tables & procedures ...")      {
         public void actionPerformed(ActionEvent e) {
            if(m_Driver == null || m_conn == null) {
               log("Not connected\n",0);
               return;
            }
            try {
               log("Creating tables & procedures for TPC-D ...\n",0);
               m_Driver.createTest(m_conn,"TPCD","create");
               log("Created successfully\n",0);
            } catch(SQLException err) {
               log("Error creating tables & procedures : " + err.getMessage() + "\n",0);
              }
         }
      });

      tpcdMenu.add(new AbstractAction("Populate tables...")      {
         public void actionPerformed(ActionEvent e) {
            if(m_Driver == null || m_conn == null) {
               log("Not connected\n",0);
               return;
            }
            try {
               log("Loading data for TPC-D ...\n",0);
               m_Driver.createTest(m_conn,"TPCD","load");
               log("Done\n",0);
            } catch(SQLException err) {
               log("Error loading data : " + err.getMessage() + "\n",0);
              }
         }
      });

      tpcdMenu.add(new AbstractAction("Run Benchmark...")      {
         public void actionPerformed(ActionEvent e) {
            tpcdRun();
         }
      });

      tpcdMenu.add(new AbstractAction("Cleanup ...")      {
         public void actionPerformed(ActionEvent e) {
            if(m_Driver == null || m_conn == null) {
               log("Not connected\n",0);
               return;
            }
            try {
               log("Cleaning up tables & procedures for TPC-D ...\n",0);
               m_Driver.parseAndExecute(m_conn,"drop","TPCD");
               log("Done\n",0);
            } catch(SQLException err) {
               log("Error cleaning up : " + err.getMessage() + "\n",0);
              }
         }
      });
*/
      //bar.add(tpcdMenu);
      JMenu editMenu = new JMenu("Edit");
      editMenu.add(new AbstractAction("New Benchmark Item...")      {
         public void actionPerformed(ActionEvent e) {
	   Object val = JOptionPane.showInputDialog (BenchPanel.this,
	     "Name for the new TPC-A like test",
	     "New test",
	     JOptionPane.PLAIN_MESSAGE, null, null, "New Item");
	   if (val != null && val.toString() != null) {
             LoginData newLogin = new LoginData(val.toString());
             pool.addLoginToThePool(newLogin);
             pool_table.selectAll();
           }
         }
      });
      editMenu.add(new AbstractAction("Delete selected items...")      {
         public void actionPerformed(ActionEvent e) {
	    try {
               int[] selection = pool_table.getSelectedRows();
               if (selection != null)
	          pool.removeLoginsFromThePool(selection);
            } catch(SQLException ex) {
	       log("Error closing connection(s) : " + ex.getMessage() + "\n",0);
            }
         }
      });
      editMenu.add(new AbstractAction("Save selected item as...")      {
         public void actionPerformed(ActionEvent e) {
              String fileName = null;
              JFileChooser chooser = new JFileChooser();
              chooser.setDialogTitle("Select output file name");
              chooser.setCurrentDirectory(new File("."));
              SimpleFileFilter filter = new SimpleFileFilter("xml");
              chooser.setFileFilter(filter);
              int returnVal = chooser.showSaveDialog(BenchPanel.this);
              if(returnVal == JFileChooser.APPROVE_OPTION) {
                String name = chooser.getSelectedFile().getPath();
                if (name.length() <= 4 || (name.length() > 4 && (! name.regionMatches(true, name.length() - 4, ".xml", 0, 4))))
                  fileName = name+".xml";
                else
                  fileName = name;
              } else
                 return;

               try {
                Results res = Results.InitItemsSaving(BenchPanel.this);
                res.SaveSelectedItems(pool, pool_table.getSelectedRows());
                XmlDocument doc = res.DoneItemsSaving();
                FileOutputStream os = new FileOutputStream(fileName);
                doc.write(os);
                os.close();
              } catch (Exception ex) {
                log(ex.toString()+"\n", 0);
              }
         }
      });
      editMenu.addSeparator();
      editMenu.add(new AbstractAction("Login details...")      {
         public void actionPerformed(ActionEvent e) {
              openConnection(false);
         }
      });
      editMenu.add(new AbstractAction("Table details...")      {
         public void actionPerformed(ActionEvent e) {
              createTables();
         }
      });
      editMenu.add(new AbstractAction("Run details...")      {
         public void actionPerformed(ActionEvent e) {
            runDetails();
         }
      });
      editMenu.addSeparator();
      editMenu.add(new AbstractAction("Insert file...")      {
         public void actionPerformed(ActionEvent e) {
              String fileName = null;
              JFileChooser chooser = new JFileChooser();
              chooser.setDialogTitle("Select file to insert");
              SimpleFileFilter filter = new SimpleFileFilter("xml");
              chooser.setFileFilter(filter);
              chooser.setCurrentDirectory(new File("."));
              int returnVal = chooser.showOpenDialog(BenchPanel.this);
              if(returnVal == JFileChooser.APPROVE_OPTION)
                fileName = chooser.getSelectedFile().getPath();
              else
                 return;

              doLoadItems(fileName, false);
         }
      });
      bar.add(editMenu);

      JMenu actionMenu = new JMenu("Action");
      actionMenu.add(new AbstractAction("Create tables & procedures")      {
         public void actionPerformed(ActionEvent e) {
            doCreateTables();
         }
      });
      actionMenu.add(new AbstractAction("Drop tables & procedures")      {
         public void actionPerformed(ActionEvent e) {
            cleanUp();
         }
      });
      actionMenu.addSeparator();
      actionMenu.add(new AbstractAction("Run Selected")      {
         public void actionPerformed(ActionEvent e) {
              runSelected();
         }
      });
      bar.add(actionMenu);


      JMenu resultsMenu = new JMenu("Results");

      resultsMenu.add(new AbstractAction("Connect")      {
         public void actionPerformed(ActionEvent e) {
            openConnection(true);
         }
      });

      resultsMenu.add(new AbstractAction("Disconnect")      {
         public void actionPerformed(ActionEvent e) {
            closeConnection(true);
         }
      });

      resultsMenu.addSeparator();
      resultsMenu.add(new AbstractAction("Create the table")      {
         public void actionPerformed(ActionEvent e) {
            createResult();
         }
      });
      resultsMenu.add(new AbstractAction("Drop the table")      {
         public void actionPerformed(ActionEvent e) {
            dropResult();
         }
      });

      bar.add(resultsMenu);

      JMenu prefsMenu = new JMenu("Preferences");
      prefsMenu.add(new AbstractAction("Display refresh rate...")      {
         public void actionPerformed(ActionEvent e) {
	   Object val = JOptionPane.showInputDialog (BenchPanel.this,
	     "Refresh txn count for the progress bars",
	     "Enter option",
	     JOptionPane.PLAIN_MESSAGE,
	     null, null,
	     String.valueOf(TPCABench.prop_update_freq));
	   if (val != null && val.toString() != null)
	      {
	        int n = Integer.valueOf(val.toString()).intValue();
		if (n >= 1)
		  TPCABench.prop_update_freq = n;
	      }
         }
      });
      bar.add(prefsMenu);


//      JMenu windowMenu = new JMenu("Window");
//
//      windowMenu.add(new AbstractAction("Win 1")      {
//         public void actionPerformed(ActionEvent e) {
//            openConnection(true);
//         }
//      });
//      bar.add(windowMenu);

      JMenu helpMenu = new JMenu("Help");
      helpMenu.add(new AbstractAction("Log level")      {
         public void actionPerformed(ActionEvent e) {
            JComboBox logLevel = new JComboBox(m_logLevels);
            logLevel.setSelectedIndex(m_logger.getLogLevel());
            if(JOptionPane.OK_OPTION != JOptionPane.showOptionDialog(BenchPanel.this,logLevel,"Select Log Level",JOptionPane.OK_CANCEL_OPTION,JOptionPane.QUESTION_MESSAGE,null,null,null)) return;
            int nNewLogLevel = logLevel.getSelectedIndex();
            if(nNewLogLevel < 0) return;
            m_logger.setLogLevel(nNewLogLevel);
         }
      });
      helpMenu.add(new AbstractAction("About")      {
         public void actionPerformed(ActionEvent e) {
            JPanel pane = new JPanel(new GridLayout(12,1,25,0));
            pane.add(new JLabel(PACKAGE_NAME + " v. " + PACKAGE_VERSION, SwingConstants.LEFT));
            pane.add(new JLabel("(C) 2000-2012 OpenLink Software", SwingConstants.LEFT));
            pane.add(new JLabel("Please report all bugs to <" + PACKAGE_BUGREPORT + ">" , SwingConstants.LEFT));
            pane.add(new JLabel(""));
            pane.add(new JLabel("This utility is released under the GNU General Public License (GPL)", SwingConstants.LEFT));
            pane.add(new JLabel(""));
	    pane.add(new JLabel("Disclaimer: The benchmark in this application is loosely based", SwingConstants.LEFT));
	    pane.add(new JLabel("on the TPC-A standard benchmark, but this application", SwingConstants.LEFT));
	    pane.add(new JLabel("does not claim to be a full or precise implementation, nor are", SwingConstants.LEFT));
	    pane.add(new JLabel("the results obtained by this application necessarily comparable", SwingConstants.LEFT));
	    pane.add(new JLabel("to the vendor's published results.", SwingConstants.LEFT));

            JOptionPane.showMessageDialog(BenchPanel.this,pane,"About jdbc-bench",JOptionPane.INFORMATION_MESSAGE);
         }
      });
      bar.add(helpMenu);

      add(BorderLayout.NORTH,bar);
      JPanel center_pane = new JPanel(new GridLayout(2, 1, 0, 10));

      JScrollPane pool_pane = new JScrollPane(pool_table);
      center_pane.setBorder(BorderFactory.createEtchedBorder());
      center_pane.add(pool_pane);

      m_pane = new JScrollPane(m_logArea);
      m_pane.setBorder(BorderFactory.createEtchedBorder());
      center_pane.add(m_pane);

      add(BorderLayout.CENTER,center_pane);
   }

/*******************************************************************************/
/*******************************************************************************/
   boolean executeNoCheck(Statement stmt, String strSQL) {
      boolean bSuccess = false;
      try {
         log(strSQL + "\n",2);
         stmt.execute(strSQL);
         bSuccess = true;
      } catch(SQLException e) {
         bSuccess = false;
         log(e.getMessage() + "\n",1);
      }
      return bSuccess;
   }

   public void setMaxTableLimits(LoginData data, int nMaxBranch,
      int nMaxTeller, int nMaxAccount)
  {
      if (nMaxBranch > 0)
         data.tpca.nMaxBranch = nMaxBranch;
      if (nMaxTeller > 0)
         data.tpca.nMaxTeller = nMaxTeller;
      if (nMaxAccount > 0)
         data.tpca.nMaxAccount = nMaxAccount;
   }


  public void doLoadItems(String fileName, boolean clear) {
    ItemLoader res = new ItemLoader(BenchPanel.this);
    Vector lst = res.LoadItemsFrom(fileName);
    try{
      if (lst != null && clear) {
        for (int i = 0; i < pool.getRowCount(); i++)
           pool.removeLoginFromThePool(i);
      }
      for (Enumeration i = lst.elements(); i.hasMoreElements(); ) {
        LoginData data = (LoginData)i.nextElement();
        if (data != null)
           pool.addLoginToThePool(data);
      }
    } catch (Exception ex) {
      log("Exception : "+ ex + "\n", 0);
    }
    pool_table.repaint();
  }

   public void doCreateTables()
  {
    String strIndexDef;

    int selection[] = pool_table.getSelectedRows();
    if (selection == null || selection.length == 0) {
       log("Please select connections for this operation\n", 0);
       return;
    }

    for (int item = selection[0]; item < selection.length; item++) {
      LoginData data = pool.getConnection(item);
      if (data.conn == null) {
        log(data.strItemName +": Not connected\n",0);
        continue;
      }
      if(data.m_Driver == null) {
         log(data.strItemName +": Driver type is not specified\n",0);
         continue;
      }

      Statement stmt = null;
      long startTime = System.currentTimeMillis();
      try {
         String strSQL;
         stmt = data.conn.createStatement();
         // create tables
         if(data.tpca.bCreateBranch) {
            log("Building table definition for " + data.m_Driver.getBranchName() + " ...",0);
            //                executeNoCheck(stmt, makeDropTable(m_Driver.getBranchName()));
            strSQL = data.m_Driver.makeCreateBranch(data.tpca.bCreateIndexes);
            executeNoCheck(stmt, "drop table " + data.m_Driver.getBranchName());
            log(strSQL + "\n",2);
            stmt.execute(strSQL);
            log("Done\n",0);
         }
         if(data.tpca.bCreateTeller) {
            log("Building table definition for " + data.m_Driver.getTellerName() + " ...",0);
            //                executeNoCheck(stmt, makeDropTable(m_Driver.getTellerName()));
            strSQL = data.m_Driver.makeCreateTeller(data.tpca.bCreateIndexes);
            executeNoCheck(stmt, "drop table " + data.m_Driver.getTellerName());
            log(strSQL + "\n",2);
            stmt.execute(strSQL);
            log("Done\n",0);
         }
         if(data.tpca.bCreateAccount) {
            log("Building table definition for " + data.m_Driver.getAccountName() + " ...",0);
            //                executeNoCheck(stmt, makeDropTable(m_Driver.getAccountName()));
            strSQL = data.m_Driver.makeCreateAccount(data.tpca.bCreateIndexes);
            executeNoCheck(stmt, "drop table " + data.m_Driver.getAccountName());
            log(strSQL + "\n",2);
            stmt.execute(strSQL);
            log("Done\n",0);
         }
         if(data.tpca.bCreateHistory) {
            log("Building table definition for " + data.m_Driver.getHistoryName() + " ...",0);
            //                executeNoCheck(stmt, makeDropTable(m_Driver.getHistoryName()));
            strSQL = data.m_Driver.makeCreateHistory();
            executeNoCheck(stmt, "drop table " + data.m_Driver.getHistoryName());
            log(strSQL + "\n",2);
            stmt.execute(strSQL);
            log("Done\n",0);
         }

         try {
           if (data.tpca.bCreateProcedures) {
	     log("\nAttempting to load the stored procedure text ...",0);
	     data.m_Driver.setProcedure(data.conn);
	     log("Done\n",0);
           }
         } catch(SQLException e) {
           log("\nLoad Procedures error : " + e.getMessage() + "\n",0);
         }

         if(data.m_Driver.mustCreateIndex() || data.tpca.bCreateIndexes) {
            log("\n",0);
            // create indices
            strIndexDef = makeCreateIndex(data.m_Driver.getIndexType(),
                data.m_Driver.getBranchName(), "branch");
            if(strIndexDef != null) {
               log(strIndexDef + "\n",0);
               stmt.execute(strIndexDef);
            }
            strIndexDef = makeCreateIndex(data.m_Driver.getIndexType(),
                data.m_Driver.getTellerName(), "teller");
            if(strIndexDef != null) {
               log(strIndexDef + "\n",0);
               stmt.execute(strIndexDef);
            }
            strIndexDef = makeCreateIndex(data.m_Driver.getIndexType(),
                data.m_Driver.getAccountName(), "account");
            if(strIndexDef != null) {
               log(strIndexDef + "\n",0);
               stmt.execute(strIndexDef);
            }
         }
         log("\n",0);
         // load the tables
         if (data.tpca.bLoadBranch)
            loadBranch(data, stmt);
         if (data.tpca.bLoadTeller)
            loadTeller(data, stmt);
         if (data.tpca.bLoadAccount)
            loadAccount(data, stmt);
      } catch(SQLException e) {
         log("\nCreate table error : " + e.getMessage() + "\n",0);
      } finally {
         if(stmt != null)
            try { stmt.close(); }
            catch(SQLException e) { }
         stmt = null;
      }

      long endTime = System.currentTimeMillis();
      Results.addResultsRecord(this, data, "TPC-A","Load tables/" + data.tpca.nMaxBranch
         + "/" + data.tpca.nMaxTeller
         + "/" + data.tpca.nMaxAccount,
         -1, (endTime - startTime) / 1000, -1, -1, -1, -1,"OK","");
    }
   }



   void createTables() {
      int selection[] = pool_table.getSelectedRows();
      if (selection == null || selection.length != 1)
	{
	  log("Please select a single connection for this operation\n", 0);
	  return;
	}
      LoginData data = pool.getConnection(selection[0]);

      if(data.conn == null) {
         log("Not connected\n",0);
         return;
      }
      JDialog   dlg = new TPCATableProps(null, "Table details | "+ data.strItemName, true, data);
      dlg.show();
      repaint();
   }


   void runSelected() {
     int selection[] = pool_table.getSelectedRows();
     if (selection == null || selection.length < 1)
	{
	  log("Please select one or more connections for this operation\n", 0);
	  return;
	}

     for(int i = 0; i < selection.length; i++)
       if (pool.getConnection(selection[i]).conn == null) {
         log("Not at all selected items are connected\n", 0);
         return;
       }

     RunSelected pane = new RunSelected();
     String options[] =  { "Start", "Run All", "Cancel"};
     int Response = JOptionPane.showOptionDialog(this,pane,"Run Duration",
            JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null,
            options,options[0]);
     if (Response == JOptionPane.CANCEL_OPTION || Response == -1)
        return;

     int nEndTime = 0;

     try {
       nEndTime = Integer.valueOf(pane.nEndTime.getText()).intValue();
     } catch(Exception e) {
       nEndTime = 0;
     }

     String xml_name = pane.sOutputFile.getText();
     RunAllThread allThread;
     if (Response == JOptionPane.YES_OPTION)
       // Start
       allThread = new RunAllThread(selection, nEndTime, xml_name, false);
     else
       // RunAll
       allThread = new RunAllThread(selection, nEndTime, xml_name, true);

     allThread.start();
   }

   void runDetails() {
     int selection[] = pool_table.getSelectedRows();
     if (selection == null || selection.length < 1)
        {
	  log("Please select one or more connections for this operation\n", 0);
	  return;
	}

     for(int i = 0; i < selection.length; i++)
       if (pool.getConnection(selection[i]).conn == null) {
         log("Not at all selected items are connected\n",0);
         return;
       }

     for(int i = 0; i < selection.length; i++) {
       LoginData data = pool.getConnection(selection[i]);

       if (data.dlg_runOpt == null)
          data.dlg_runOpt = new TPCARunOptions(null, "Run details | "+ data.strItemName, false, data);
       data.dlg_runOpt.show();
     }
   }

   public void oneTransMore() { nbTrans++; }
   public void addTrans(int count) { nbTrans += count; }
   public void oneThreadLess() { nbThreads--; }


   public void doRunTests(String titleDialog, int[] selection, int nEndTime, Results res) {

       //Block the start of a new tests, if a test is working already
      if (isBenchWorked) {
         log("Test is already run, wait when it is finished",0);
	 return;
      }
      isBenchWorked = true;

       log("\nDeleting from the History table ...",0);
       for (int nRow = 0; nRow < selection.length; nRow++) {
	   LoginData data = pool.getConnection(selection[nRow]);
	   Statement stmt = null;
	   try {
	       log("delete from " + data.m_Driver.getHistoryName() + "\n",2);
	       stmt = data.conn.createStatement();
	       stmt.executeUpdate("delete from " + data.m_Driver.getHistoryName());
	       data.conn.commit(); log("Done\n",0);
           } catch(SQLException e) {
	       log("Run tests error : " + e.getMessage(),0);
               isBenchWorked = false;
	       return;
           } finally {
	      if (stmt != null)
		 try {
		   stmt.close();
		 } catch(SQLException e) { }
           }
       }

       // check if an jdbc driver support a selected options
       for (int nRow = 0; nRow < selection.length; nRow++) {
	   LoginData data = pool.getConnection(selection[nRow]);
           data.tpca.supported = true;
           if (! data.isTxnModeSupported(data.tpca.txnOption))
              data.tpca.supported = false;
           if (! data.isResSetTypeSupported(data.tpca.scrsOption))
              data.tpca.supported = false;
           if ((! data.isBatchSupported()) && data.tpca.nBatchSize > 1)
              data.tpca.supported = false;
           if ((! data.m_Driver.supportsProcedures()) && data.tpca.sqlOption == TPCABench.RUN_SPROC)
              data.tpca.supported = false;
           if (data.tpca.nThreads > 1)
              data.tpca.bTrans = true;
       }

       m_tpcaBench.removeAllElements();
       java.util.Date startTime = new java.util.Date();
       m_tests = new ThreadGroup("JDBC Tests");

       int _nThreads = 0;
       try {
	   for (int nRow = 0; nRow < selection.length; nRow++) {
	     LoginData data = pool.getConnection(selection[nRow]);
             if (data.tpca.supported) {
               _nThreads += data.tpca.nThreads;
	       m_tpcaBench.addElement(new TPCABench(m_tests,
		   "Test Thread 0",
		   data.conn,
		   m_logger,
		   this,
		   data.m_Driver,
		   nEndTime,
		   startTime.getTime(),
                   data.tpca.sqlOption,
                   data.tpca.txnOption,
                   data.tpca.scrsOption,
		   data.tpca.bTrans,
                   data.tpca.bQuery,
                   data.tpca.travCount,
                   data.tpca.nBatchSize,
		   data.strNowFunction));
	       for(int nThread = 1; nThread < data.tpca.nThreads; nThread++) {
		   m_tpcaBench.addElement(new TPCABench(m_tests,
		       "Test thread " + nThread,
		       data.strURL, data.strUID,  data.strPWD,
		       m_logger,
		       this,
		       data.m_Driver,
		       nEndTime,
		       startTime.getTime(),
                       data.tpca.sqlOption,
                       data.tpca.txnOption,
                       data.tpca.scrsOption,
		       data.tpca.bTrans,
                       data.tpca.bQuery,
                       data.tpca.travCount,
                       data.tpca.nBatchSize,
		       data.strNowFunction));
               }
              }
           }
       } catch (SQLException e) {
	   log("Error in creating the threads : " + e.getMessage(), 0);
           isBenchWorked = false;
	   return;
       }

       // start the timer thread

       ((TheLogger)m_logger).nb_threads = _nThreads * selection.length;
       nbThreads=_nThreads;
       nbTrans=0;

       JDialog progress = new JDialog(m_parentFrame, titleDialog, false);
       Container thisContent = progress.getContentPane();

       JLabel jlabel = new JLabel("Transaction num. 100000");
       jlabel.setHorizontalAlignment(JLabel.CENTER);

       JLabel jlabel1 = new JLabel("Num. threads left " + nbThreads);
       jlabel1.setHorizontalAlignment(JLabel.CENTER);

       JButton cancelBtn = new JButton();
       cancelBtn.setActionCommand("Cancel");
       cancelBtn.setText("Cancel");

       JPanel masterProgressPane = new JPanel(new GridLayout(selection.length, 1, 0, 10));
       masterProgressPane.setBorder(BorderFactory.createEtchedBorder());
       int i = 0;
       for (int nRow = 0; nRow < selection.length; nRow++)
	 {
	   LoginData data = pool.getConnection(selection[nRow]);
           if (data.tpca.supported) {
	     JPanel progressPane = new JPanel(new GridLayout(data.tpca.nThreads, 1, 0, 5));
	     progressPane.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), data.strItemName));
	     for (int nThread = 0; nThread < data.tpca.nThreads; nThread++) {
	       progressPane.add(((TPCABench)m_tpcaBench.elementAt(i++)).getProgressBar());
             }
	     masterProgressPane.add(progressPane);
           }
	 }

       JPanel labelsPane = new JPanel(new BorderLayout());
       labelsPane.add(BorderLayout.NORTH, jlabel);
       labelsPane.add(BorderLayout.CENTER, jlabel1);

       BorderLayout thisLayout;
       JPanel jpanel = new JPanel(thisLayout = new BorderLayout());
       jpanel.add(BorderLayout.NORTH, labelsPane);
       jpanel.add(BorderLayout.CENTER, masterProgressPane);

       jpanel.add(BorderLayout.SOUTH, cancelBtn);

       thisContent.add(jpanel);

       progress.setSize(
	   thisLayout.preferredLayoutSize(jpanel).width + 150,
	   thisLayout.preferredLayoutSize(jpanel).height + 50
		       );
       progress.setLocation(
	   m_parentFrame.getLocation().x + (m_parentFrame.getSize().width - progress.getSize().width) / 2,
	   m_parentFrame.getLocation().y + (m_parentFrame.getSize().height - progress.getSize().height) / 2
			   );
       jlabel.setText("Transaction num. " + nbTrans);
       progress.setVisible(true);

       thr = new RunThread(nEndTime, jlabel, jlabel1, progress, cancelBtn);
       cancelBtn.addActionListener(thr);
       thr.start();

       // run the show
       long runStartTime = System.currentTimeMillis();

       for (Enumeration el = m_tpcaBench.elements(); el.hasMoreElements(); )
              ((TPCABench)el.nextElement()).start();

       try {
          for (Enumeration el = m_tpcaBench.elements(); el.hasMoreElements(); )
              ((TPCABench)el.nextElement()).join(nEndTime*60000+ 1000);
       } catch( InterruptedException ie ) {
       }

       for (Enumeration el = m_tpcaBench.elements(); el.hasMoreElements(); )
              ((TPCABench)el.nextElement()).interrupt();

       thr.interrupt();
       try {
        thr.join();
       } catch (InterruptedException e) { }

      isBenchWorked = false;
      res.printResults(runStartTime, selection);
   }




  void cleanUp() {
     int selection[] = pool_table.getSelectedRows();
     if (selection == null || selection.length == 0) {
       log("Please select connections for this operation\n", 0);
       return;
     }

     for (int item = selection[0]; item < selection.length; item++) {
       LoginData data = pool.getConnection(item);
       if (data.conn == null) {
         log(data.strItemName +": Not connected\n",0);
         continue;
       }
       if (data.m_Driver == null) {
         log(data.strItemName +": Driver type is not specified\n",0);
         continue;
       }

       if (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(this,
            "Drop TPC-A tables & procedure on :\n" + data.strURL,
            "Confirm tables & procedure drop",JOptionPane.YES_NO_OPTION))
       {
         Statement stmt = null;
         try {
           stmt = data.conn.createStatement();
            log("Cleaning up " + data.m_Driver.getBranchName() + " table ...",0);
            log("drop table " + data.m_Driver.getBranchName() + "\n",2);
            executeNoCheck(stmt, "drop table " + data.m_Driver.getBranchName());
            log("Done\n",0);

            log("Cleaning up " + data.m_Driver.getAccountName() + " table ...",0);
            log("drop table " + data.m_Driver.getAccountName() + "\n",2);
            executeNoCheck(stmt, "drop table " + data.m_Driver.getAccountName());
            log("Done\n",0);

            log("Cleaning up " + data.m_Driver.getTellerName() + " table ...",0);
            log("drop table " + data.m_Driver.getTellerName() + "\n",2);
            executeNoCheck(stmt, "drop table " + data.m_Driver.getTellerName());
            log("Done\n",0);

            log("Cleaning up " + data.m_Driver.getHistoryName() + " table ...",0);
            log("drop table " + data.m_Driver.getHistoryName() + "\n",2);
            executeNoCheck(stmt, "drop table " + data.m_Driver.getHistoryName());
            log("Done\n",0);

            String sql = data.m_Driver.m_strDropProcedure;
            if (sql != null) {
              log("Cleaning up procedures ...",0);
              log(sql + "\n",2);
              executeNoCheck(stmt, sql);
              log("Done\n",0);
            }

         } catch(SQLException e) {
            log("Cleanup error : " + e.getMessage() + "\n",0);
         } finally {
            if(stmt != null)
               try {
                 stmt.close();
               } catch(SQLException e) { }
         }
       }
      }
   }

   public void log(String message, int nLevel) {
      if(m_logger != null) m_logger.log(message,nLevel);
   }


   void createResult() {
     if (results == null) {
        log("Not Connected\n",0);
        return;
     }
     int nIndex = -1;
     if (results.m_Driver != null)
        for (int n = 0;n < Driver.DriverMap.length;n++)
          if (results.m_Driver == Driver.DriverMap[n])
            nIndex = n;

     Statement stmt = null;
     try {
        results.setDriver(nIndex);

        log("Creating results in " + results.strURL + "(" + results.strDBMSName + ") ...  \n",0);
        stmt = results.conn.createStatement();
        //executeNoCheck(stmt, "drop table results");
        String strSQL = results.m_Driver.makeCreateResults();
        log(strSQL + "\n",2);
        stmt.execute(strSQL);
        log("Done\n",0);
     } catch(Exception e) {
        log("Error creating results Table : " + e.getMessage() + "\n",0);
     } finally {
	if (stmt != null)
           try {
             stmt.close();
           } catch (SQLException ex) {}
     }
   }


   public void dropResult() {
     if (results == null) {
        log("Not Connected\n",0);
	return;
     }
     try {
	if (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(this,
           "Drop 'resultsj' table on :\n" + results.strURL + "("
           + results.strDBMSName + ")","Confirm RESULTS table drop", JOptionPane.YES_NO_OPTION))
	     doDropResult();
     } catch(SQLException e) {
        log("Error dropping results Table : " + e.getMessage() + "\n",0);
     }
     repaint();
   }

   public void doDropResult() throws SQLException {
     if (results == null) {
	log("Not Connected\n",0);
	return;
     }
     Statement stmt = null;
     try {
        log("Dropping results from " + results.strURL + "(" + results.strDBMSName + ") ... ",0);
        stmt = results.conn.createStatement();
        log("drop table results\n",2);
        stmt.execute("drop table results");
        log("Done\n",0);
     } finally {
        if (stmt != null)
	   stmt.close();
        stmt = null;
     }
   }



  /////////////// RunThread //////////////////////
  class RunThread extends Thread implements ActionListener {
    private JLabel jlabel,jlabel1;
    private JDialog dialog;
    private JButton jbutton;
    public SQLException exception=null;
    private boolean cancel=false;
    long durationMins;
    long nCtr = 0;
    long startTime;

    public RunThread(long durationMins, JLabel jlabel,JLabel jlabel1,JDialog dialog,JButton jbutton) {
      this.jlabel=jlabel; this.jlabel1=jlabel1;
      this.dialog=dialog; this.jbutton=jbutton;
      this.durationMins = durationMins;
    }

    public boolean isCanceled() {
      return cancel;
    }

    public void actionPerformed(ActionEvent e) {
      if (e.getSource() == jbutton) {
        log("\n*** Cancel ***\n",0);
        cancel = true;

        if (m_tpcaBench != null)
  	  for (Enumeration el = m_tpcaBench.elements(); el.hasMoreElements(); )
              ((TPCABench)el.nextElement()).cancel();
//        if (m_tpccBench != null)
//	  for (int i=0; i < m_tpccBench.length; i++)
//	      m_tpccBench[i].cancel();
      }
    }

    public void run() {

      if (jbutton != null && dialog != null && jlabel != null && jlabel1 != null)
        while(!dialog.isVisible() || !jbutton.isVisible() ||
             !jlabel.isVisible() || !jlabel1.isVisible())
         {
	   try {
             new Thread().sleep(100);
	   } catch(InterruptedException e) {}
         }

      nCtr = 0;
      startTime = System.currentTimeMillis();
      try {
        while (!cancel) {
          Thread.sleep(100);

	  SwingUtilities.invokeAndWait(
	     new Runnable() {
               public void run() {
		 if (durationMins > 0) {
                   long remainingSecs = (durationMins * 60) -
			  ((System.currentTimeMillis() - startTime)/1000);
                   jlabel.setText(remainingSecs + " s remaining");
		 } else {
		   jlabel.setText("Transaction num. "+nbTrans);
                 }

		 nCtr = nCtr >= 10000 ? 0 : nCtr + 1;
		 if (nCtr % 10 == 0)
  		   jlabel1.setText("Num. threads left "+nbThreads);
	       }
	     }
	  );
        }
      } catch (Exception e) {};

      try {
        SwingUtilities.invokeAndWait(
           new Runnable() {
	     public void run() { dialog.dispose(); }
           }
        );
      } catch (Exception e) {}
    }

  }

  ///////////////////// RunThread ///////////////////
  class RunAllThread extends Thread {

    int[] selection;
    int nEndTime;
    String xml_name;
    boolean bRunAll;

    RunAllThread(int[] _selection, int _nEndTime, 
    	String _xml_name, boolean _bRunAll)
    {
      nEndTime = _nEndTime;
      bRunAll  = _bRunAll;
      selection = _selection;
      xml_name = _xml_name;
    }

    public void run() {

      try{
        Results res = Results.InitResultsSaving(BenchPanel.this, nEndTime);

	if ( ! bRunAll ) {
	   doRunTests("   JDBC  Benchmark  in  progress ...   ", selection, nEndTime, res);
        } else {
          int travCount = 0;

          for (int fTrans = 0; fTrans <= 1; fTrans++) {
            for (int fQuery = 0; fQuery <= 1; fQuery++) {
              for (int nIsolation = 0; nIsolation < 5; nIsolation++) {
                if (fQuery == 0 && nIsolation > 0)
	          continue;

 	        for (int nCursor = 0; nCursor < 3; nCursor++) {

                  if (fQuery == 0 && nCursor > 0)
		    continue;

                  if (nCursor > 0)
                    travCount = 3;

		      // Loop around the SQL options
	          for (int nOption = 1; nOption <= 3; nOption++){
                      StringBuffer title = new StringBuffer();
                      switch (nOption) {
                        case TPCABench.RUN_TEXT: title.append("SQLExecute"); break;
                        case TPCABench.RUN_PREPARED: title.append("Prep-Execute"); break;
                        case TPCABench.RUN_SPROC: title.append("Stored proc"); break;
                      }
                      if (fTrans == 1)
                        title.append("/Trans");
                      if (fQuery == 1)
                        title.append("/Query");
                      switch(nIsolation) {
                        case 1:  title.append("/Uncommitted");  break;
                        case 2:  title.append("/Committed");    break;
                        case 3:  title.append("/Repeatable");  break;
                        case 4:  title.append("/Serializable"); break;
                      }
                      switch(nCursor){
                        case 1:  title.append("/Insensitive");  break;
                        case 2:  title.append("/Sensitive");    break;
                      }

                      for (int nRow = 0; nRow < selection.length; nRow++) {
                        LoginData.TPCA tpca = pool.getConnection(selection[nRow]).tpca;
                        tpca.sqlOption = nOption;
                        tpca.travCount = travCount;
                        tpca.bTrans = (fTrans == 0 ? false : true);
                        tpca.bQuery = (fQuery == 0 ? false : true);
                        switch(nIsolation) {
                          case 1:
                            tpca.txnOption = Connection.TRANSACTION_READ_UNCOMMITTED;
                            break;
                          case 2:
                            tpca.txnOption = Connection.TRANSACTION_READ_COMMITTED;
                            break;
                          case 3:
                            tpca.txnOption = Connection.TRANSACTION_REPEATABLE_READ;
                            break;
                          case 4:
                            tpca.txnOption = Connection.TRANSACTION_SERIALIZABLE;
                            break;
                          default:
                            tpca.txnOption = TPCABench.TXN_DEFAULT;
                        }

                        switch(nCursor){
                          case 1:
                            tpca.scrsOption = ResultSet.TYPE_SCROLL_INSENSITIVE;
                            break;
                          case 2:
                            tpca.scrsOption = ResultSet.TYPE_SCROLL_SENSITIVE;
                            break;
                          default:
                            tpca.scrsOption = ResultSet.TYPE_FORWARD_ONLY;
                        }

                      }
	              doRunTests(title.toString(), selection, nEndTime, res);
	              if (thr.isCanceled())
	                return;
                  }// SQL options

                } // Cursor

              } // Isolation
            } // Query
          } // Transactions
        }

        XmlDocument doc = res.DoneResultsSaving();
        try {
          FileOutputStream os = new FileOutputStream(xml_name);
          doc.write(os);
          os.close();
        } catch(Exception e) {
          log(e.toString(), 0);
        }

      } catch (Exception e) {
        System.out.println(e);
        e.printStackTrace();
      }

    }
  }


   class BranchThread extends Thread implements ActionListener {

	private int nMaxBranch, nBranch;
	private JLabel jlabel;
	private Statement stmt;
	private Driver m_Driver;
	private JDialog dialog;
	private JButton jbutton;
	public boolean cancel=false;
	public SQLException exception=null;

	public BranchThread(int _nMaxBranch, JLabel jlabel, Statement stmt,
            Driver m_Driver, JDialog dialog, JButton jbutton)
        {
	    this.nMaxBranch=_nMaxBranch;
            this.jlabel=jlabel;
            this.stmt=stmt;
            this.m_Driver=m_Driver;
	    this.dialog=dialog;
            this.jbutton=jbutton;
	}

	public void actionPerformed(ActionEvent e) {
	    if (e.getSource() == jbutton) {
		log("\n*** Cancel ***\n",0);
                cancel=true;
		nBranch=nMaxBranch;
                dialog.dispose();
		jbutton.removeActionListener(this);
	    }
	}

	public void run() {
	    while(!(dialog.isVisible() && jbutton.isVisible() && jlabel.isVisible()))
	    try {
              new Thread().sleep(100);
            } catch(InterruptedException e) { }

	    try {
	  	for(nBranch = 0; nBranch < nMaxBranch && !cancel; nBranch++) {
		    jlabel.setText("Create branch num "+nBranch+"/"+nMaxBranch);
                    jlabel.repaint();
         	    String strSQL = "insert into " + m_Driver.getBranchName()
                        + " (branch, fillerint, balance, filler) values ("
                        + (nBranch + 1) + "," + // Branch ID
         		(nBranch + 1) + "," + 1000 + "," + "\'" + strFiller + "\'" + ")";
         	    log(strSQL + "\n",2);
         	    stmt.executeUpdate(strSQL);
	   	}
	    } catch(SQLException e) {
               exception=e;
               dialog.dispose();
            }
	    dialog.dispose();
	}
    };

   void loadBranch(LoginData data, Statement stmt) throws SQLException {
      int nMaxBranch = data.tpca.nMaxBranch;
      log("Attempting to load " + nMaxBranch + " records into "
         + data.m_Driver.getBranchName() + " table ... ",0);

      JDialog dialog = new JDialog(m_parentFrame,"Creation of branch table ...",true);
      Container thisContent = dialog.getContentPane();
      JLabel jlabel = new JLabel("Create branch num. "+nMaxBranch+"/"+nMaxBranch);
      jlabel.setHorizontalAlignment(JLabel.CENTER);
      JButton jbutton = new JButton();
      jbutton.setActionCommand("Cancel"); jbutton.setText("Cancel");
      JPanel jpanel = new JPanel();
      BorderLayout thisLayout = new BorderLayout();
      jpanel.setLayout(thisLayout);
      jpanel.add(BorderLayout.NORTH,jlabel);
      jpanel.add(BorderLayout.SOUTH,jbutton);
      thisContent.add(jpanel);
      dialog.setSize(thisLayout.preferredLayoutSize(jpanel).width+50,thisLayout.preferredLayoutSize(jpanel).height+50);
      dialog.setLocation(m_parentFrame.getLocation().x+(m_parentFrame.getSize().width-dialog.getSize().width)/2,
			 m_parentFrame.getLocation().y+(m_parentFrame.getSize().height-dialog.getSize().height)/2);
      jlabel.setText("Create branch num. 1/"+nMaxBranch);
      BranchThread thr = new BranchThread(nMaxBranch, jlabel, stmt, data.m_Driver, dialog, jbutton);
      jbutton.addActionListener(thr);
      thr.start();
      dialog.setVisible(true);

      // stmt.close();
      if (thr.exception!=null)
        throw thr.exception;
      if (!thr.cancel)
        log("Done\n",0);
      repaint();
   }

   class TellerThread extends Thread implements ActionListener {

	private int nMaxTeller, nTeller, nMaxBranch;
	private JLabel jlabel;
	private Statement stmt;
	private Driver m_Driver;
	private JDialog dialog;
	private JButton jbutton;
	public boolean cancel=false;
	public SQLException exception=null;

	public TellerThread(int _nMaxBranch, int _nMaxTeller,JLabel jlabel,
            Statement stmt, Driver m_Driver, JDialog dialog, JButton jbutton)
        {
	    this.nMaxBranch = _nMaxBranch;
            this.nMaxTeller = _nMaxTeller;
            this.jlabel=jlabel;
            this.stmt=stmt;
            this.m_Driver=m_Driver;
	    this.dialog=dialog;
            this.jbutton=jbutton;
	}

	public void actionPerformed(ActionEvent e) {
	    if (e.getSource() == jbutton) {
		log("\n*** Cancel ***\n",0);
                cancel=true;
		nTeller=nMaxTeller;
                dialog.dispose();
		jbutton.removeActionListener(this);
	    }
	}

	public void run() {
	    while(!(dialog.isVisible() && jbutton.isVisible() && jlabel.isVisible()))
	    try {
              new Thread().sleep(100);
            } catch(InterruptedException e) { }

	    try {
	   	for(nTeller = 0; nTeller < nMaxTeller && !cancel; nTeller++) {
		    jlabel.setText("Create teller num "+nTeller+"/"+nMaxTeller);
                    jlabel.repaint();
         	    String strSQL = "insert into " + m_Driver.getTellerName()
                        + " (teller, branch, balance, filler) values ("
                        + (nTeller + 1) + "," + // Teller ID
         		((long)(Math.random() * nMaxBranch)) + "," +100000
                        + "," + "\'" + strFiller + "\'" +
         		" )";
         	    log(strSQL + "\n",2);
         	    stmt.executeUpdate(strSQL);
	   	}
	    } catch(SQLException e) {
               exception=e;
               dialog.dispose();
            }
	    dialog.dispose();
	}
    };

   void loadTeller(LoginData data, Statement stmt) throws SQLException {

      int nMaxBranch = data.tpca.nMaxBranch;
      int nMaxTeller = data.tpca.nMaxTeller;
      log("Attempting to load " + nMaxTeller + " records into "
        + data.m_Driver.getTellerName() + " table ... ",0);

      JDialog dialog = new JDialog(m_parentFrame,"Creation of teller table ...",true);
      Container thisContent = dialog.getContentPane();
      JLabel jlabel = new JLabel("Create teller num. "+nMaxTeller+"/"+nMaxTeller);
      jlabel.setHorizontalAlignment(JLabel.CENTER);
      JButton jbutton = new JButton();
      jbutton.setActionCommand("Cancel"); jbutton.setText("Cancel");
      JPanel jpanel = new JPanel();
      BorderLayout thisLayout = new BorderLayout();
      jpanel.setLayout(thisLayout);
      jpanel.add(BorderLayout.NORTH,jlabel);
      jpanel.add(BorderLayout.SOUTH,jbutton);
      thisContent.add(jpanel);
      dialog.setSize(thisLayout.preferredLayoutSize(jpanel).width+50,
         thisLayout.preferredLayoutSize(jpanel).height+50);
      dialog.setLocation(m_parentFrame.getLocation().x+(m_parentFrame.getSize().width-dialog.getSize().width)/2,
			 m_parentFrame.getLocation().y+(m_parentFrame.getSize().height-dialog.getSize().height)/2);
      jlabel.setText("Create teller num. 1/"+nMaxTeller);
      TellerThread thr = new TellerThread(nMaxBranch, nMaxTeller, jlabel, stmt,
        data.m_Driver, dialog, jbutton);
      jbutton.addActionListener(thr);
      thr.start();
      dialog.setVisible(true);

      // stmt.close();
      if (thr.exception!=null)
        throw thr.exception;
      if (!thr.cancel)
        log("Done\n",0);
      repaint();
   }

    class AccountThread extends Thread implements ActionListener {

	private int nMaxBranch, nMaxAccount, nAccount;
	private JLabel jlabel;
	private Statement stmt;
	private Driver m_Driver;
	private JDialog dialog;
	private JButton jbutton;
	public boolean cancel=false;
	public SQLException exception=null;

	public AccountThread(int _nMaxBranch, int _nMaxAccount, JLabel jlabel, Statement stmt,
            Driver m_Driver, JDialog dialog, JButton jbutton)
        {
	    this.nMaxBranch=_nMaxBranch;
	    this.nMaxAccount=_nMaxAccount;
            this.jlabel=jlabel;
            this.stmt=stmt;
            this.m_Driver=m_Driver;
	    this.dialog=dialog;
            this.jbutton=jbutton;
	}

	public void actionPerformed(ActionEvent e) {
	    if (e.getSource() == jbutton) {
		log("\n*** Cancel ***\n",0);
                cancel=true;
		nAccount=nMaxAccount;
                dialog.dispose();
		jbutton.removeActionListener(this);
	    }
	}

	public void run() {
	    while(!(dialog.isVisible() && jbutton.isVisible() && jlabel.isVisible()))
		try { new Thread().sleep(100); }
		catch(InterruptedException e) { }

	    try {
	   	for(nAccount = 0; nAccount < nMaxAccount && !cancel; nAccount++) {
		    jlabel.setText("Create account num "+nAccount+"/"+nMaxAccount);
                    jlabel.repaint();
   		    String strSQL = "insert into " + m_Driver.getAccountName()
                        + " (account, branch, balance, filler) values ("
                        + (nAccount + 1) + "," + // Account ID
	      	        ((long)(Math.random() * nMaxBranch)) + "," + 100000
                        + "," + "\'" + strFiller + "\'" +
	   		" )";
   	   	    log(strSQL + "\n",2);
	   	    stmt.executeUpdate(strSQL);
		}
	    } catch(SQLException e) {
               exception=e;
               dialog.dispose();
            }
	    dialog.dispose();
	}
    };

   void loadAccount(LoginData data, Statement stmt) throws SQLException {

      int nMaxBranch = data.tpca.nMaxBranch;
      int nMaxAccount = data.tpca.nMaxAccount;

      log("Attempting to load " + nMaxAccount + " records into "
        + data.m_Driver.getAccountName() + " table ... ",0);

      JDialog dialog = new JDialog(m_parentFrame,"Creation of account table ...",true);
      Container thisContent = dialog.getContentPane();
      JLabel jlabel = new JLabel("Create account num. "+nMaxAccount+"/"+nMaxAccount);
      jlabel.setHorizontalAlignment(JLabel.CENTER);
      JButton jbutton = new JButton();
      jbutton.setActionCommand("Cancel"); jbutton.setText("Cancel");
      JPanel jpanel = new JPanel();
      BorderLayout thisLayout = new BorderLayout();
      jpanel.setLayout(thisLayout);
      jpanel.add(BorderLayout.NORTH,jlabel);
      jpanel.add(BorderLayout.SOUTH,jbutton);
      thisContent.add(jpanel);
      dialog.setSize(thisLayout.preferredLayoutSize(jpanel).width+50,thisLayout.preferredLayoutSize(jpanel).height+50);
      dialog.setLocation(m_parentFrame.getLocation().x+(m_parentFrame.getSize().width-dialog.getSize().width)/2,
			 m_parentFrame.getLocation().y+(m_parentFrame.getSize().height-dialog.getSize().height)/2);
      jlabel.setText("Create account num. 1/"+nMaxAccount);
      AccountThread thr = new AccountThread(nMaxBranch, nMaxAccount, jlabel, stmt,
        data.m_Driver, dialog, jbutton);
      jbutton.addActionListener(thr);
      thr.start();
      dialog.setVisible(true);

      // stmt.close();
      if (thr.exception!=null)
        throw thr.exception;
      if (!thr.cancel)
        log("Done\n",0);
      repaint();
   }




   public void oneMoreWarehouse() { nWar++; }
   public void oneMoreDistrict() { nDis++; }
   public void oneMoreItem() { mIt++; }
   public void oneMoreCustomer() { nCust++; }
   public void oneMoreOrder() { nOrd++; }
   public void oneMoreRound() { nRd++; }
   public void resetWarehouse() { nWar=0; }
   public void resetDistrict() { nDis=0; }
   public void resetItem() { mIt=0; }
   public void resetCustomer() { nCust=0; }
   public void resetOrder() { nOrd=0; }
   public void resetRound() { nRd=0; }

/////////////////////////////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////////////////////////
/**********

//   void tpcdRun() {
//   }

   public void doTpccRun(int nThreads, int n_rounds, int local_w_id, int n_ware) throws SQLException
     {
       if (nbThreads > 0)
	 return;

       int selection[] = pool_table.getSelectedRows();
       if (selection == null || selection.length < 1)
	 {
	   log("Please select one or more connections for this operation\n", 0);
	   return;
	 }

       m_tpccBench = new TPCCBench[nThreads * selection.length];

       for (int nRow = 0; nRow < selection.length; nRow++)
	 {
	   LoginData data = pool.getConnection(selection[nRow]);
	   m_tpccBench[nRow * nThreads] = new TPCCBench(data.conn,
	       data.m_Driver,
	       n_rounds,
	       local_w_id,
	       n_ware,
	       m_logger,
	       this);
	   for (int nThread = 1; nThread < nThreads; nThread++)
	     {
	       m_tpccBench[nRow * nThreads + nThreads] = new TPCCBench(
		   data.strURL, data.strUID, data.strPWD,
		   data.m_Driver,
		   n_rounds,
		   local_w_id,
		   n_ware,
		   m_logger,
		   this);
	     }
	 }

       ((TheLogger)m_logger).nb_threads= nThreads * selection.length;
       nbThreads=nThreads; nbTrans=0;

       //resetRound();

       JDialog dialog = new JDialog(m_parentFrame,"TPCC benchmark running ...",false);
       Container thisContent = dialog.getContentPane();

       JLabel jlabel =
	   new JLabel("Running round num. "+ nbTrans + "/" + n_rounds * nThreads * selection.length);
       jlabel.setHorizontalAlignment(JLabel.CENTER);

       JLabel jlabel1 = new JLabel("Num. threads left " + nbThreads);
       jlabel1.setHorizontalAlignment(JLabel.CENTER);

       JButton jbutton = new JButton();
       jbutton.setActionCommand("Cancel");
       jbutton.setText("Cancel");

       JPanel masterProgressPane = new JPanel(new GridLayout(selection.length, 1, 0, 10));
       masterProgressPane.setBorder(BorderFactory.createEtchedBorder());
       for (int nRow = 0; nRow < selection.length; nRow++)
	 {
	   LoginData data = pool.getConnection(selection[nRow]);
	   JPanel progressPane = new JPanel(new GridLayout(nThreads, 1, 0, 5));
	   progressPane.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), data.strURL));
	   for (int nThread = 0; nThread < nThreads; nThread++)
	     progressPane.add(m_tpcaBench[nRow * nThreads + nThread].getProgressBar());
	   masterProgressPane.add(progressPane);
	 }

       JPanel labelsPane = new JPanel(new BorderLayout());
       labelsPane.add(BorderLayout.NORTH, jlabel);
       labelsPane.add(BorderLayout.CENTER, jlabel1);

       BorderLayout thisLayout;
       JPanel jpanel = new JPanel(thisLayout = new BorderLayout());
       jpanel.add(BorderLayout.NORTH, labelsPane);
       jpanel.add(BorderLayout.CENTER, masterProgressPane);
       jpanel.add(BorderLayout.SOUTH, jbutton);

       thisContent.add(jpanel);

       dialog.setSize(
	   thisLayout.preferredLayoutSize(jpanel).width+50,
	   thisLayout.preferredLayoutSize(jpanel).height+50
	   );
       dialog.setLocation(
	   m_parentFrame.getLocation().x+(m_parentFrame.getSize().width-dialog.getSize().width)/2,
	   m_parentFrame.getLocation().y+(m_parentFrame.getSize().height-dialog.getSize().height)/2
	   );

       dialog.setVisible(true);
       thr = new RunThread(0, jlabel, jlabel1, dialog, jbutton);
       jbutton.addActionListener(thr);
       thr.start();

       // run the show
       for(int nThread = 0; nThread < m_tpccBench.length; nThread++)
	 m_tpccBench[nThread].start();


       SwingUtilities.invokeLater(
	   new TPCCWaiterRunnable(selection, nThreads,  n_rounds, local_w_id, n_ware)
	   );
   }

   void tpccRun()
     {

       int selection[] = pool_table.getSelectedRows();
       if(selection == null || selection.length < 1)
	 {
	   log("Please select one or more connections for this operation\n", 0);
	   return;
	 }

       try
	 {
	   JTextField n_rounds = new JTextField("1",20);
	   JTextField local_w_id = new JTextField("1",20);
	   JTextField n_ware = new JTextField("1",20);
	   JTextField n_threads = new JTextField("1",20);
	   // interface
	   JPanel pane = new JPanel(new BorderLayout());
	   JPanel lp = new JPanel(new GridLayout(4,1));
	   lp.add(new JLabel("Number threads"));
	   lp.add(new JLabel("Rounds"));
	   lp.add(new JLabel("Local warehouse ID"));
	   lp.add(new JLabel("Number of warehouses"));
	   pane.add(BorderLayout.WEST,lp);
	   JPanel vp = new JPanel(new GridLayout(4,1));
	   vp.add(n_threads);
	   vp.add(n_rounds);
	   vp.add(local_w_id);
	   vp.add(n_ware);
	   pane.add(BorderLayout.EAST,vp);
	   if(JOptionPane.OK_OPTION != JOptionPane.showOptionDialog(this,pane,"Enter test parameters",JOptionPane.OK_CANCEL_OPTION,JOptionPane.QUESTION_MESSAGE,null,null,null))
	     return;

	   doTpccRun(Integer.valueOf(n_threads.getText()).intValue(),
	       Integer.valueOf(n_rounds.getText()).intValue(),
	       Integer.valueOf(local_w_id.getText()).intValue(),
	       Integer.valueOf(n_ware.getText()).intValue()
	       );
	 }
       catch(Exception e)
	 {
	   log("Error Running the TPC-C benchmark : " + e.getMessage() + "\n",1);
	 }
     }

   void tpccLoadData()
     {
       int selection[] = pool_table.getSelectedRows();
       if (selection == null || selection.length != 1)
	 {
	   log("Please select a single connection for this operation\n", 0);
	   return;
	 }
       LoginData data = pool.getConnection(selection[0]);

       int nIndex = -1;
       if(data.m_Driver != null)
	 for(int n = 0; n < Driver.DriverMap.length; n++)
	   if(data.m_Driver == Driver.DriverMap[n])
	     nIndex = n;

      try {
         data.setDriver(nIndex);
	 pool.fireTableRowsUpdated(selection[0], selection[0]);

	 // Build and display the options panel
	JPanel pane = new JPanel(new BorderLayout(10,10)),pane1=new JPanel(new GridLayout(5,1)),pane2=new JPanel(new GridLayout(5,1));
	pane1.add(new JLabel("Number of warehouses  ")); pane1.add(new JLabel("Number of districts per warehouse  "));
	pane1.add(new JLabel("Number of customers per district  ")); pane1.add(new JLabel("Number of orders per district  "));
	pane1.add(new JLabel("Maximum number of items  "));
	JTextField jtextfield1=new JTextField("1",7); jtextfield1.setHorizontalAlignment(JTextField.RIGHT);
	JTextField jtextfield2=new JTextField("10",7); jtextfield2.setHorizontalAlignment(JTextField.RIGHT);
	JTextField jtextfield3=new JTextField("3000",7); jtextfield3.setHorizontalAlignment(JTextField.RIGHT);
	JTextField jtextfield4=new JTextField("300",7); jtextfield4.setHorizontalAlignment(JTextField.RIGHT);
	JTextField jtextfield5=new JTextField("100000",7); jtextfield5.setHorizontalAlignment(JTextField.RIGHT);
	pane2.add(jtextfield1); pane2.add(jtextfield2); pane2.add(jtextfield3); pane2.add(jtextfield4); pane2.add(jtextfield5);
	pane.add(BorderLayout.CENTER,pane1); pane.add(BorderLayout.EAST,pane2);
	int Response = JOptionPane.showOptionDialog(this,pane,"TPCC Options ...",JOptionPane.OK_CANCEL_OPTION,JOptionPane.QUESTION_MESSAGE,null,null,null);
	if (Response == JOptionPane.CANCEL_OPTION || Response == -1) return;
	// Retrieve input parameters
	try {
	    int nWare = Integer.valueOf(jtextfield1.getText()).intValue(); int dWare = Integer.valueOf(jtextfield2.getText()).intValue();
	    int cDist = Integer.valueOf(jtextfield3.getText()).intValue(); int dOrd  = Integer.valueOf(jtextfield4.getText()).intValue();
	    int mItem = Integer.valueOf(jtextfield5.getText()).intValue();
	    if(nWare<1 || dWare<1 || cDist<1 || mItem<1 || dOrd<1) return;
   	      doTpccLoadData(data, nWare,dWare,cDist,mItem,dOrd);
	} catch(NumberFormatException e) { return; }
      } catch(Exception e) { log("Error loading the data : " + e.getMessage() + "\n",1); }
      repaint();
   }

   public void doTpccLoadData(LoginData data, int nWarehouses, int nDistricts,
       int nCustomers, int maxItem, int nOrders) throws SQLException
     {
       if(data == null)
	 throw new SQLException("Connection not open");
       if(data.m_Driver == null)
	 throw new SQLException("Unknown driver type");

      TPCCBench bench = new TPCCBench(data.conn, data.m_Driver, 0, 0, 0, m_logger, this);
      bench.numOrders=nOrders;
      bench.numDistricts=nDistricts;
      bench.reset();

      // Build panel to show what it does
      log("Attempting to load records into " + data.m_Driver.getTPCCWarehouseName() + " table ... ",0);
      resetWarehouse(); resetDistrict(); resetItem();
      JDialog dialog = new JDialog(m_parentFrame,"Creation of warehouse table ...",true);
      Container thisContent = dialog.getContentPane();
      JLabel jlabel1 = new JLabel("Create warehouse num. "+nWarehouses+"/"+nWarehouses);
      jlabel1.setHorizontalAlignment(JLabel.CENTER);
      JLabel jlabel2 = new JLabel("Create district num. "+nDistricts+"/"+nDistricts);
      jlabel2.setHorizontalAlignment(JLabel.CENTER);
      JLabel jlabel3 = new JLabel("Create item num. "+maxItem+"/"+maxItem);
      jlabel3.setHorizontalAlignment(JLabel.CENTER);
      JButton jbutton = new JButton(); jbutton.setActionCommand("Cancel"); jbutton.setText("Cancel");
      JPanel jpanel = new JPanel();
      GridLayout thisLayout = new GridLayout(4,1); jpanel.setLayout(thisLayout);
      jpanel.add(jlabel1); jpanel.add(jlabel2); jpanel.add(jlabel3); jpanel.add(jbutton);
      thisContent.add(jpanel);
      dialog.setSize(thisLayout.preferredLayoutSize(jpanel).width+50,thisLayout.preferredLayoutSize(jpanel).height+50);
      dialog.setLocation(m_parentFrame.getLocation().x+(m_parentFrame.getSize().width-dialog.getSize().width)/2,
			 m_parentFrame.getLocation().y+(m_parentFrame.getSize().height-dialog.getSize().height)/2);
      jlabel1.setText("Create warehouse num. 1/"+nWarehouses); jlabel2.setText("Create district num. 1/"+nDistricts); jlabel3.setText("Create item num. 1/"+maxItem);
      WarehouseThread thr = new WarehouseThread(data.m_Driver, nWarehouses,nDistricts,maxItem,jlabel1,jlabel2,jlabel3,dialog,jbutton,bench);
      jbutton.addActionListener(thr); thr.start(); dialog.setVisible(true);
      if(thr.exception!=null) throw thr.exception;
      if(!thr.cancel) log("Done\n",0);
      else return;
      repaint();

      log("Attempting to load records into " + data.m_Driver.getTPCCItemName() + " table ... ",0);
      resetWarehouse(); resetDistrict(); resetItem();
      dialog = new JDialog(m_parentFrame,"Creation of item table ...",true);
      thisContent = dialog.getContentPane();
      jlabel1 = new JLabel("Create item num. "+maxItem+"/"+maxItem);
      jlabel1.setHorizontalAlignment(JLabel.CENTER);
      jbutton = new JButton(); jbutton.setActionCommand("Cancel"); jbutton.setText("Cancel");
      jpanel = new JPanel();
      thisLayout = new GridLayout(2,1); jpanel.setLayout(thisLayout);
      jpanel.add(jlabel1); jpanel.add(jbutton);
      thisContent.add(jpanel);
      dialog.setSize(thisLayout.preferredLayoutSize(jpanel).width+50,thisLayout.preferredLayoutSize(jpanel).height+50);
      dialog.setLocation(m_parentFrame.getLocation().x+(m_parentFrame.getSize().width-dialog.getSize().width)/2,
			 m_parentFrame.getLocation().y+(m_parentFrame.getSize().height-dialog.getSize().height)/2);
      jlabel1.setText("Create item num. 1/"+maxItem);
      ItemThread thr1 = new ItemThread(data.m_Driver, maxItem,jlabel1,dialog,jbutton,bench);
      jbutton.addActionListener(thr1); thr1.start(); dialog.setVisible(true);
      if(thr1.exception!=null) throw thr.exception;
      if(!thr1.cancel) log("Done\n",0);
      else return;
      repaint();

      log("Attempting to load records into " + data.m_Driver.getTPCCCustomerName() + " table ... ",0);
      resetWarehouse(); resetDistrict(); resetCustomer();
      dialog = new JDialog(m_parentFrame,"Creation of customer table ...",true);
      thisContent = dialog.getContentPane();
      jlabel1 = new JLabel("In warehouse num. "+nWarehouses+"/"+nWarehouses);
      jlabel1.setHorizontalAlignment(JLabel.CENTER);
      jlabel2 = new JLabel("In district num. "+nDistricts+"/"+nDistricts);
      jlabel2.setHorizontalAlignment(JLabel.CENTER);
      jlabel3 = new JLabel("Create customer num. "+nCustomers+"/"+nCustomers);
      jlabel3.setHorizontalAlignment(JLabel.CENTER);
      jbutton = new JButton(); jbutton.setActionCommand("Cancel"); jbutton.setText("Cancel");
      jpanel = new JPanel();
      thisLayout = new GridLayout(4,1); jpanel.setLayout(thisLayout);
      jpanel.add(jlabel1); jpanel.add(jlabel2); jpanel.add(jlabel3); jpanel.add(jbutton);
      thisContent.add(jpanel);
      dialog.setSize(thisLayout.preferredLayoutSize(jpanel).width+50,thisLayout.preferredLayoutSize(jpanel).height+50);
      dialog.setLocation(m_parentFrame.getLocation().x+(m_parentFrame.getSize().width-dialog.getSize().width)/2,
			 m_parentFrame.getLocation().y+(m_parentFrame.getSize().height-dialog.getSize().height)/2);
      jlabel1.setText("In warehouse num. 1/"+nWarehouses); jlabel2.setText("In district num. 1/"+nDistricts); jlabel3.setText("Create customer num. 1/"+nCustomers);
      CustomerThread thr2 = new CustomerThread(data.m_Driver, nWarehouses,nDistricts,nCustomers,jlabel1,jlabel2,jlabel3,dialog,jbutton,bench,this);
      thr2.strNowFunction = data.strNowFunction;
      jbutton.addActionListener(thr2); thr2.start(); dialog.setVisible(true);
      if(thr2.exception!=null) throw thr2.exception;
      if(!thr2.cancel) log("Done\n",0);
      else return;
      repaint();

      log("Attempting to load records into " + data.m_Driver.getTPCCOrdersName() + " table ... ",0);
      resetWarehouse(); resetDistrict(); resetOrder();
      dialog = new JDialog(m_parentFrame,"Creation of order table ...",true);
      thisContent = dialog.getContentPane();
      jlabel1 = new JLabel("In warehouse num. "+nWarehouses+"/"+nWarehouses);
      jlabel1.setHorizontalAlignment(JLabel.CENTER);
      jlabel2 = new JLabel("In district num. "+nDistricts+"/"+nDistricts);
      jlabel2.setHorizontalAlignment(JLabel.CENTER);
      jlabel3 = new JLabel("Create order num. "+nOrders+"/"+nOrders);
      jlabel3.setHorizontalAlignment(JLabel.CENTER);
      jbutton = new JButton(); jbutton.setActionCommand("Cancel"); jbutton.setText("Cancel");
      jpanel = new JPanel();
      thisLayout = new GridLayout(4,1); jpanel.setLayout(thisLayout);
      jpanel.add(jlabel1); jpanel.add(jlabel2); jpanel.add(jlabel3); jpanel.add(jbutton);
      thisContent.add(jpanel);
      dialog.setSize(thisLayout.preferredLayoutSize(jpanel).width+50,thisLayout.preferredLayoutSize(jpanel).height+50);
      dialog.setLocation(m_parentFrame.getLocation().x+(m_parentFrame.getSize().width-dialog.getSize().width)/2,
			 m_parentFrame.getLocation().y+(m_parentFrame.getSize().height-dialog.getSize().height)/2);
      jlabel1.setText("In warehouse num. 1/"+nWarehouses); jlabel2.setText("In district num. 1/"+nDistricts); jlabel3.setText("Create order num. 1/"+nOrders);
      OrderThread thr3 = new OrderThread(data.m_Driver, nWarehouses,nDistricts,nCustomers,nOrders,maxItem,jlabel1,jlabel2,jlabel3,dialog,jbutton,bench,this);
      thr3.strNowFunction = data.strNowFunction;
      jbutton.addActionListener(thr3); thr3.start(); dialog.setVisible(true);
      if(thr3.exception!=null) throw thr3.exception;
      if(!thr3.cancel) log("Done\n",0);
      else return;
      repaint();
   }
*****************/

   //////////////////////// TPCCWaiterRunnable //////////////////////
/*************
   class TPCCWaiterRunnable implements Runnable {

     int nThreads;
     int nRounds;
     int local_w_id;
     int n_ware;
     int selection[];

     TPCCWaiterRunnable(int selection[], int nThreads, int nRounds, int local_w_id, int n_ware)
       {
	 this.nThreads = nThreads;
	 this.nRounds = nRounds;
	 this.local_w_id = local_w_id;
	 this.n_ware = n_ware;
	 this.selection = selection;
       }

     public void run()
       {
	 try {
	   if (thr.cancel)
	     for(int nThread = 0; nThread < m_tpccBench.length; nThread++)
	       {
		 m_tpccBench[nThread].interrupt();
		 m_tpccBench[nThread].join();
	       }
	 } catch (InterruptedException e) {};

	 if (nbThreads > 0)
	   {
	     Thread.yield();
	     SwingUtilities.invokeLater(this);
	     return;
	   }

	 thr.interrupt();

	 if(!thr.cancel)
	   {
	     log("\nTest Completed\n",0);
	     for (int nRow = 0; nRow < selection.length; nRow++)
	       {
		 LoginData data = pool.getConnection(selection[nRow]);
		 double avgTPC = 0;
		 double nMaxTime = -1;
		 for (int nThread = 0; nThread < nThreads; nThread++)
		   {
		     avgTPC += m_tpccBench[nRow * nThreads + nThread].getAverageTPC();
		     if (nMaxTime < m_tpccBench[nRow * nThreads + nThread].getTotalTime())
		       nMaxTime = m_tpccBench[nRow * nThreads + nThread].getTotalTime();
		   }
		 addResultsRecord(data,
		     "TPC-C","Run/" + local_w_id + "/" + n_ware + "/" + nThreads,
		     avgTPC,
		     nMaxTime,
		     nRounds * nThreads,
		     -1,-1,-1);
	       }
	   }
	 m_tpccBench = null;
       }
   }
************/
   ///////////////////// OrderThread /////////////////////////////////
/***********
   class OrderThread extends Thread implements ActionListener {

	private int nOrders,nWarehouses,nDistricts,nCustomers,mItem;
	private JLabel jlabel1,jlabel2,jlabel3;
	private JDialog dialog;
	private JButton jbutton;
	private TPCCBench bench;
	private BenchPanel pane;
	public boolean cancel=false,end=false;
	public SQLException exception=null;
	public String strNowFunction = null;
	Driver m_Driver;

	public OrderThread(Driver m_Driver, int nWarehouses,int nDistricts,int nCustomers,int nOrders,int mItem,JLabel jlabel1,JLabel jlabel2,JLabel jlabel3,JDialog dialog,JButton jbutton,TPCCBench bench,BenchPanel pane) {
	    this.nOrders=nOrders; this.nDistricts=nDistricts; this.nWarehouses=nWarehouses;
	    this.nCustomers=nCustomers; this.mItem=mItem;
	    this.jlabel1=jlabel1; this.jlabel2=jlabel2; this.jlabel3=jlabel3;
	    this.dialog=dialog; this.jbutton=jbutton; this.bench=bench;
	    this.pane=pane;
	    this.m_Driver = m_Driver;
	}

	public void actionPerformed(ActionEvent e) {
	    if (e.getSource() == jbutton) {
		log("\n*** Cancel ***\n",0); end=cancel=true; bench.cancel();
		dialog.dispose(); jbutton.removeActionListener(this);
	    }
	}

	public void run() {
	    while(!(dialog.isVisible() && jbutton.isVisible() && jlabel1.isVisible() && jlabel2.isVisible() && jlabel3.isVisible()))
		try { new Thread().sleep(100); }
		catch(InterruptedException e) { }
	    Thread thr = new Thread() {
		public void run() {
		    while(!end) {
			jlabel1.setText("In warehouse num. "+nWar+"/"+nWarehouses);
			jlabel2.setText("In district num. "+nDis+"/"+nDistricts);
			jlabel3.setText("Create order num. "+nOrd+"/"+nOrders);
			try { new Thread().sleep(500); }
			catch(Exception e) { }
		    }
		}
	    }; thr.start();

	    try {
      		bench.loadOrder(m_Driver.getTPCCOrdersName(),m_Driver.getTPCCNewOrderName(),m_Driver.getTPCCOrderLineName(),m_Driver.getTPCCHistoryName(),nWarehouses,nDistricts,nCustomers,mItem,nOrders,strNowFunction,pane);
		end=true; thr=null;
	    } catch(SQLException e) { exception=e; }

	    dialog.dispose();
	}
    };
******************/

    ////////////////// CustomerThread //////////////////////////////////
/***************
    class CustomerThread extends Thread implements ActionListener {

	private int nCustomers,nWarehouses,nDistricts;
	private JLabel jlabel1,jlabel2,jlabel3;
	private JDialog dialog;
	private JButton jbutton;
	private TPCCBench bench;
	private BenchPanel pane;
	public boolean cancel=false,end=false;
	public SQLException exception=null;
	public String strNowFunction = null;
	Driver m_Driver;

	public CustomerThread(Driver m_Driver, int nWarehouses,int nDistricts,int nCustomers,JLabel jlabel1,JLabel jlabel2,JLabel jlabel3,JDialog dialog,JButton jbutton,TPCCBench bench,BenchPanel pane) {
	    this.nCustomers=nCustomers; this.nDistricts=nDistricts; this.nWarehouses=nWarehouses;
	    this.jlabel1=jlabel1; this.jlabel2=jlabel2; this.jlabel3=jlabel3;
	    this.dialog=dialog; this.jbutton=jbutton; this.bench=bench;
	    this.pane=pane;
	    this.m_Driver = m_Driver;
	}

	public void actionPerformed(ActionEvent e) {
	    if (e.getSource() == jbutton) {
		log("\n*** Cancel ***\n",0); end=cancel=true; bench.cancel();
		dialog.dispose(); jbutton.removeActionListener(this);
	    }
	}

	public void run() {
	    while(!(dialog.isVisible() && jbutton.isVisible() && jlabel1.isVisible() && jlabel2.isVisible() && jlabel3.isVisible()))
		try { new Thread().sleep(100); }
		catch(InterruptedException e) { }
	    Thread thr = new Thread() {
		public void run() {
		    while(!end) {
			jlabel1.setText("In warehouse num. "+nWar+"/"+nWarehouses);
			jlabel2.setText("In district num. "+nDis+"/"+nDistricts);
			jlabel3.setText("Create customer num. "+nCust+"/"+nCustomers);
			try { new Thread().sleep(500); }
			catch(Exception e) { }
		    }
		}
	    }; thr.start();

	    try {
      		bench.loadCustomer(m_Driver.getTPCCCustomerName(),m_Driver.getTPCCHistoryName(),nWarehouses,nDistricts,nCustomers,strNowFunction,pane);
		end=true; thr=null;
	    } catch(SQLException e) { exception=e; }

	    dialog.dispose();
	}
    };
***********/
    ///////////////// WarehouseThread ////////////////////////////////
/*************
    class WarehouseThread extends Thread implements ActionListener {

	private int nWarehouses,nDistricts,mItem;
	private JLabel jlabel1,jlabel2,jlabel3;
	private JDialog dialog;
	private JButton jbutton;
	private TPCCBench bench;
	public boolean cancel=false,end=false;
	public SQLException exception=null;
	Driver m_Driver;

	public WarehouseThread(Driver m_Driver, int nWarehouses,int nDistricts,int mItem,JLabel jlabel1,JLabel jlabel2,JLabel jlabel3,JDialog dialog,JButton jbutton,TPCCBench bench) {
	    this.nWarehouses=nWarehouses; this.nDistricts=nDistricts; this.mItem=mItem;
	    this.jlabel1=jlabel1; this.jlabel2=jlabel2; this.jlabel3=jlabel3;
	    this.dialog=dialog; this.jbutton=jbutton; this.bench=bench;
	    this.m_Driver = m_Driver;
	}

	public void actionPerformed(ActionEvent e) {
	    if (e.getSource() == jbutton) {
		log("\n*** Cancel ***\n",0); end=cancel=true; bench.cancel();
		dialog.dispose(); jbutton.removeActionListener(this);
	    }
	}

	public void run() {
	    while(!(dialog.isVisible() && jbutton.isVisible() && jlabel1.isVisible() && jlabel2.isVisible() && jlabel3.isVisible()))
	        try { new Thread().sleep(100); }
		catch(InterruptedException e) { }
	    Thread thr = new Thread() {
	    	public void run() {
		    while(!end) {
			jlabel1.setText("Create warehouse num. "+nWar+"/"+nWarehouses); jlabel1.repaint();
			jlabel2.setText("Create district num. "+nDis+"/"+nDistricts); jlabel2.repaint();
			jlabel3.setText("Create item num. "+mIt+"/"+mItem); jlabel3.repaint();
			try { new Thread().sleep(500); }
			catch(Exception e) { }
		    }
		}
	    }; thr.start();

	    try {
		bench.loadWarehouse(m_Driver.getTPCCWarehouseName(),m_Driver.getTPCCDistrictName(),m_Driver.getTPCCStockName(),nWarehouses,nDistricts,mItem);
		end=true; thr=null;
	    } catch(SQLException e) { exception=e; }

	    dialog.dispose();
	}
    };
************/
   /////////////////// ItemThread //////////////////////////////////
/****
    class ItemThread extends Thread implements ActionListener {

	private int mItem;
	private JLabel jlabel1;
	private JDialog dialog;
	private JButton jbutton;
	private TPCCBench bench;
	public boolean cancel=false,end=false;
	public SQLException exception=null;
	Driver m_Driver;

	public ItemThread(Driver m_Driver, int mItem,JLabel jlabel1,JDialog dialog,JButton jbutton,TPCCBench bench) {
	    this.mItem=mItem; this.jlabel1=jlabel1;
	    this.dialog=dialog; this.jbutton=jbutton; this.bench=bench;
	    this.m_Driver = m_Driver;
	}

	public void actionPerformed(ActionEvent e) {
	    if (e.getSource() == jbutton) {
		log("\n*** Cancel ***\n",0); end=cancel=true; bench.cancel();
		dialog.dispose(); jbutton.removeActionListener(this);
	    }
	}

	public void run() {
	    while(!(dialog.isVisible() && jbutton.isVisible() && jlabel1.isVisible()))
	    	try { new Thread().sleep(100); }
		catch(InterruptedException e) { }
	    Thread thr = new Thread() {
		public void run() {
	    	    while(!end) {
			jlabel1.setText("Create item num. "+mIt+"/"+mItem); jlabel1.repaint();
			try { new Thread().sleep(500); }
			catch(Exception e) { }
		    }
		}
	    }; thr.start();

	    try {
      		bench.loadItems(m_Driver.getTPCCItemName(),mItem);
		end=true; thr=null;
	    } catch(SQLException e) { exception=e; }

	    dialog.dispose();
	}
    };
*******/


}
