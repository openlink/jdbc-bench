/*
 *  $Id$
 *
 *  jdbc-bench - a TPC-A and TPC-C like benchmark program for JDBC drivers
 *
 *  Copyright (C) 2000-2018 OpenLink Software <jdbc-bench@openlinksw.com>
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
import javax.swing.table.*;



public class LoginData
{
  public String strURL;
  public String strUID;
  public String strPWD;
  public String strDriver;
  public Connection conn = null;

  public String strItemName;
  public String strDBMSName;
  public String strDBMSVer;
  public String strDriverName;
  public String strDriverVer;
  public String strNowFunction;

  public Driver m_Driver;

  public JDialog dlg_runOpt;

  public TPCA tpca = new TPCA();

  public LoginData(String _name)
    {
      strItemName = _name;
    }

  public LoginData(String _strURL, String _strUID, String _strPWD)  throws SQLException
    {
      strItemName = "New Item";
      strURL = _strURL;
      strUID = _strUID;
      strPWD = _strPWD;
      doLogin();
    }


  public void setDetails(JComponent parent, String strCaption, LoginData last) throws Exception
    {
      String str;
      JTextField driver, url, uid;
      JPasswordField pwd;

      if (strDriverName == null)

        driver = new JTextField(last == null ? "openlink.jdbc3.Driver" : last.strDriverName, 40);

      else
        driver = new JTextField(strDriverName, 40);

      if (strURL == null)
        url = new JTextField(last == null ? "jdbc:openlink://localhost/SVT=Oracle 8.1.x/Database=OR8i/UID=scott/PWD=tiger/" : last.strURL, 40);
      else
        url = new JTextField(strURL, 40);

      if (this.strUID == null) {
        uid = new JTextField(last == null ? "" : last.strUID, 40);
        pwd = new JPasswordField(last == null ? "" : last.strPWD, 40);
      } else {
        uid = new JTextField(strUID, 40);
        pwd = new JPasswordField("", 40);
      }

      // interface
      JPanel pane = new JPanel(new BorderLayout(10,10));
      JPanel labels_pane = new JPanel(new GridLayout(4,1));
      labels_pane.add(new JLabel("JDBC Driver"));
      labels_pane.add(new JLabel("URL"));
      labels_pane.add(new JLabel("User"));
      labels_pane.add(new JLabel("Password"));
      pane.add(BorderLayout.WEST,labels_pane);
      JPanel values_pane = new JPanel(new GridLayout(4,1));
      values_pane.add(driver);
      values_pane.add(url);
      values_pane.add(uid);
      values_pane.add(pwd);
      pane.add(BorderLayout.CENTER,values_pane);

      if (JOptionPane.OK_OPTION ==
	  JOptionPane.showOptionDialog(null, pane, strCaption,
	    JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, null, null)
         )
	{

		strDriver = driver.getText(); 
		strURL = url.getText(); 
		strUID = uid.getText(); 
		strPWD = new String(pwd.getPassword());


          if (strDriver != null && strDriver.trim().length() == 0) strDriver = null;
          if (strURL != null && strURL.trim().length() == 0) strURL = null;
          if (strUID != null && strUID.trim().length() == 0) strUID = null;
          if (strPWD != null && strPWD.trim().length() == 0) strPWD = null;

          if (strDriver != null)
	      DriverManager.registerDriver((java.sql.Driver)Class.forName(strDriver).newInstance());
	  doLogin();
	}
      else
	throw new Exception("Canceled");
    }


  public synchronized void doLogin() throws SQLException
    {
      if (conn != null)
	doLogout();

      conn = DriverManager.getConnection(strURL, strUID, strPWD);
      try
	{
	  DatabaseMetaData meta = conn.getMetaData();
	  strDBMSName = new String(meta.getDatabaseProductName());
	  strDBMSVer = new String(meta.getDatabaseProductVersion());
	  strDriverName = new String(meta.getDriverName());
	  strDriverVer = new String(meta.getDriverVersion());

	  int nDriver = -1;
	  for (nDriver = 0; nDriver < Driver.DriverMap.length; nDriver++)
	    {
	      if (-1 != strDBMSName.toUpperCase().indexOf(Driver.DriverMap[nDriver].m_strShortName.toUpperCase()))
		break;
	      if (-1 != strDBMSName.toUpperCase().indexOf(Driver.DriverMap[nDriver].m_strDriverName.toUpperCase()))
		break;
	    }
	  if (nDriver == Driver.DriverMap.length)
            try {
              setDriver(-1);
            } catch (Exception e) {
    	      m_Driver = Driver.getDriver(11); //ANSI
            }
          else
  	    m_Driver = Driver.getDriver(nDriver);

	  String timeFuncs = meta.getTimeDateFunctions();
          strNowFunction = null;
	  java.util.StringTokenizer TimeFunctions = new java.util.StringTokenizer(timeFuncs,",");
	  while(TimeFunctions.hasMoreTokens()) {
	      if (TimeFunctions.nextToken().trim().equalsIgnoreCase("NOW")) {
		  strNowFunction = "{fn now()}";
		  break;
              }
          }
          if (strNowFunction == null) {
	    TimeFunctions = new java.util.StringTokenizer(timeFuncs,",");
	    while(TimeFunctions.hasMoreTokens()) {
	      String strNextToken = TimeFunctions.nextToken().toUpperCase().trim();
              if(strNextToken.equals("CURDATE")) {
		  strNowFunction = "{fn curdate()}";
		  break;
              } else if(strNextToken.equals("SYSDATE")) {
		  strNowFunction = "sysdate";
		  break;
              }
            }
          }
          if (m_Driver.m_strProcedure != null)
              tpca.bCreateProcedures = true;
	}
      catch (SQLException e)
	{
	  doLogout();
	  throw e;
	}
    }

  public synchronized void doLogout() throws SQLException
    {
      if (conn == null)
	return;

      conn.close();
      conn = null;
      strDBMSName = null;
      strDBMSVer = null;
      strDriverName = null;
      strDriverVer = null;
      strNowFunction = null;
    }

  public void setDriver(int nDriverIndex) throws Exception
    {
      if(nDriverIndex == -1)
	{
	  if (conn == null)
	    return;

	  JList driversList = new JList(Driver.DriverMap);
	  JScrollPane list_pane = new JScrollPane(driversList);
	  JLabel DBMSName = new JLabel(strDBMSName);

	  DBMSName.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),"DBMS name"));
	  list_pane.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),"Choose DBMS type"));
	  JPanel pane = new JPanel(new BorderLayout(10,10));
	  pane.add(BorderLayout.NORTH,DBMSName);
	  pane.add(BorderLayout.CENTER,list_pane);

	  if(JOptionPane.OK_OPTION !=
	      JOptionPane.showOptionDialog(null, pane, "Choose a driver type",
		JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null,null,null) ||
	      driversList.getSelectedIndex() == -1)
	    {
	      throw new Exception("The Driver type is unknown.\nExiting");
	    }
	  m_Driver = Driver.getDriver(driversList.getSelectedIndex());
	}
    else
      m_Driver = Driver.getDriver(nDriverIndex);
  }

  public void createTest(String strTEST, String strFile) throws SQLException
    {
      if (m_Driver == null || conn == null)
	return;
      m_Driver.createTest(conn, strTEST, strFile);
    }

  public void parseAndExecute(String strFile, String strTEST) throws SQLException
    {
      if (m_Driver == null || conn == null)
	return;
      m_Driver.parseAndExecute(conn, strFile, strTEST);
    }


  public boolean isTxnModeSupported(int mode) {
     if (mode == TPCABench.TXN_DEFAULT)
       return true;

     DatabaseMetaData md = null;
     try {
      if (conn != null)
          md = conn.getMetaData();
          if (md != null && md.supportsTransactionIsolationLevel(mode))
              return true;
          else
              return false;
     } catch (SQLException e) {
       return false;
     }
  }

  public boolean isResSetTypeSupported(int rsType) {
     DatabaseMetaData md = null;

     try {
      if (conn != null)
          md = conn.getMetaData();
          if (md != null && md.supportsResultSetType(rsType))
              return true;
          else
              return false;

     } catch (SQLException e) {
       return false;
     }

  }


  public boolean isBatchSupported() {

     DatabaseMetaData md = null;
     try {
      if (conn != null)
          md = conn.getMetaData();
          if (md != null && md.supportsBatchUpdates())
              return true;
          else
              return false;

     } catch (SQLException e) {
       return false;
     }

  }


  public void setTableDetails(boolean _bCreateBranch, boolean _bCreateTeller,
      boolean _bCreateAccount, boolean _bCreateHistory,
      boolean _bLoadBranch, boolean _bLoadTeller,
      boolean _bLoadAccount, boolean _bCreateIndexes,  boolean _bCreateProcedures,
      int _nMaxBranch, int _nMaxTeller, int _nMaxAccount)
  {
    tpca.bCreateBranch = _bCreateBranch;
    tpca.bCreateTeller = _bCreateTeller;
    tpca.bCreateAccount = _bCreateAccount;
    tpca.bCreateHistory = _bCreateHistory;
    tpca.bLoadBranch = _bLoadBranch;
    tpca.bLoadTeller = _bLoadTeller;
    tpca.bLoadAccount = _bLoadAccount;
    tpca.bCreateIndexes = _bCreateIndexes;
    tpca.bCreateProcedures = _bCreateProcedures;
    tpca.nMaxBranch = _nMaxBranch;
    tpca.nMaxTeller = _nMaxTeller;
    tpca.nMaxAccount = _nMaxAccount;
  }



  public class TPCA {
    boolean bCreateBranch = true;
    boolean bCreateTeller = true;
    boolean bCreateAccount = true;
    boolean bCreateHistory = true;
    boolean bLoadBranch = true;
    boolean bLoadTeller = true;
    boolean bLoadAccount = true;
    boolean bCreateIndexes = true;
    boolean bCreateProcedures;
    int     nMaxBranch = BenchPanel.m_nMaxBranch;
    int     nMaxTeller = BenchPanel.m_nMaxTeller;
    int     nMaxAccount = BenchPanel.m_nMaxAccount;

    int nThreads = 1;
    int sqlOption = TPCABench.RUN_TEXT;
    int txnOption = TPCABench.TXN_DEFAULT;

    int scrsOption = java.sql.ResultSet.TYPE_FORWARD_ONLY;

    boolean bTrans = false;
    boolean bQuery = false;
    int travCount = 1;
    int nBatchSize = 1;

    boolean supported = true;

  }

}
