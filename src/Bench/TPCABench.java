/*
 *  $Id$
 *
 *  jdbc-bench - a TPC-A and TPC-C like benchmark program for JDBC drivers
 *
 *  Copyright (C) 2000-2015 OpenLink Software <jdbc-bench@openlinksw.com>
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

import java.text.*;
import java.sql.*;
import java.awt.*;
import java.util.*;

import javax.swing.*;



public class TPCABench extends Thread {
  public final static int RUN_TEXT = 1;
  public final static int RUN_PREPARED = 2;
  public final static int RUN_SPROC = 3;

  public final static int TXN_DEFAULT = -1;

  Logger m_log = null;
  BenchPanel pane;
  Connection m_conn;
  String m_nowFunction = null, SQLError = null;
  Driver m_Driver;
  int m_nNumRuns;
  volatile int m_nTrans = 0;
  int m_nTrans1Sec = 0, m_nTrans2Sec = 0;
  int m_nMaxBranch = 10, m_nMaxTeller = 100, m_nMaxAccount = 1000;
  int m_sqlOptions, m_txnOptions, m_scrsOptions, m_travCount, m_BatchSize;
  boolean m_bTrans, m_bQuery;
  boolean m_bTransactionsUsed = false;
  boolean m_bCloseConnection = false;
  boolean cancel=false;
  volatile double m_nTimeSum = 0;
  long m_time;
  long m_masterStartTime;
  JProgressBar bar = null;
  JLabel tpsLabel = null;
  JPanel progress_pane = null;
  NumberFormat tpsForm = NumberFormat.getInstance();
  NumberFormat txnForm = NumberFormat.getInstance();
  public static int prop_update_freq = 10;

  public JComponent getProgressBar() { return progress_pane; }


  protected void setBarValue(int newValue)
    {
      SwingUtilities.invokeLater(
	  new Runnable() {
	    public void run() {
	      bar.setValue(m_nTrans);
	      double tpS = m_nTrans;
	      if (m_nTimeSum > 0)
		tpS = tpS / m_nTimeSum;
	      else
		tpS = 0;
	      tpsLabel.setText(txnForm.format(m_nTrans) + " txn (" + tpsForm.format(tpS) + " tps)");
	      if (m_time > 0 && pane != null && pane.m_tpcaBench != null) // if the bench runs for ... minutes
		{ // do rescaling of progress bars based on an estimate number of transactions for the specified time
		  // that's based on the fastest Thread (with greatest tps value)
		  long nCurTrans, nMaxTrans = 0;
		  int newMax;
		  long nDiff = System.currentTimeMillis() - m_time;
		  for (Enumeration el = pane.m_tpcaBench.elements(); el.hasMoreElements(); )
		    {
		      nCurTrans = ((TPCABench)el.nextElement()).m_nTrans;
		      if (nCurTrans > nMaxTrans)
			nMaxTrans = nCurTrans;
		    }
		  if (nDiff > 0)
		    newMax = (int)(nMaxTrans * m_nNumRuns * 60000 / nDiff);
		  else
		    newMax = 0;
		  if (newMax > 0)
                    for (Enumeration el = pane.m_tpcaBench.elements(); el.hasMoreElements(); )
                       ((TPCABench)el.nextElement()).bar.setMaximum(newMax);
		}
	    }
	  }
      );
    }

  public String getSQLError() { return SQLError; }

  public void cancel() {
    cancel=true;
  }

  public int getNTrans() {
    return m_nTrans;
  }

  public int getTrans1Sec() {
    return m_nTrans1Sec;
  }

  public int getTrans2Sec() {
    return m_nTrans2Sec;
  }

  public double getTotalTime() {
    return m_nTimeSum;
  }

  public TPCABench(ThreadGroup group, String strThreadName,
      String strURL, String strUserName, String strPassword,
      Logger logger, BenchPanel pane, Driver driver,
      int nNumRuns, long start_time,
      int sqlOptions, int txnOptions, int scrsOptions,
      boolean bTrans, boolean bQuery, int travCount, int nBatchSize,
      String strNowFunction
      ) throws SQLException
    {
      this (group,strThreadName,
	  DriverManager.getConnection(strURL,strUserName,strPassword),
	  logger, pane,driver,
	  nNumRuns, start_time,
	  sqlOptions, txnOptions, scrsOptions,
          bTrans, bQuery, travCount, nBatchSize,
          strNowFunction);
    m_bCloseConnection = true;
  }

  public TPCABench(ThreadGroup group, String strThreadName,
      Connection conn, Logger logger, BenchPanel pane, Driver driver,
      int nNumRuns, long start_time,
      int sqlOptions, int txnOptions, int scrsOptions,
      boolean bTrans, boolean bQuery, int travCount, int nBatchSize,
      String strNowFunction
      ) throws SQLException
    {
      super(group,strThreadName);
      m_log = logger; this.pane = pane;
      m_Driver = driver;
      m_conn = conn;
      if (m_Driver == null)
         throw new SQLException("Must supply a driver type");
      m_nNumRuns = nNumRuns;
      m_bTrans = bTrans; m_bQuery = bQuery;
      m_nowFunction = strNowFunction;
      m_time = start_time;

      m_sqlOptions = sqlOptions;
      m_txnOptions = txnOptions;
      m_scrsOptions = scrsOptions;
      m_travCount = travCount;
      m_BatchSize = nBatchSize;

      bar = new JProgressBar();
      bar.setOrientation(JProgressBar.HORIZONTAL);
      bar.setMinimum(0);
      bar.setMaximum(m_time > 0 ? 1000 : m_nNumRuns);

      tpsLabel = new JLabel("000000 txn (00000.00 tps)");
      txnForm.setParseIntegerOnly(true);
      tpsForm.setMinimumFractionDigits(2);
      tpsForm.setMaximumFractionDigits(2);

      progress_pane = new JPanel(new BorderLayout());
      progress_pane.add(BorderLayout.CENTER, bar);
      progress_pane.add(BorderLayout.NORTH, tpsLabel);
    }

  public void run()
    {

      m_nTrans = m_nTrans1Sec = m_nTrans2Sec = 0; m_nTimeSum = 0.0d;
      if(m_nNumRuns <= 0)
	return;

      m_masterStartTime = System.currentTimeMillis();
      Statement stmt = null;
      SQLError = null;
      try {
	  setBarValue(0);

          if (m_txnOptions != TXN_DEFAULT) {
             if (m_conn.getMetaData().supportsTransactionIsolationLevel(m_txnOptions))
                m_conn.setTransactionIsolation(m_txnOptions);
             else
                throw new SQLException("The transaction mode ("
                  + Driver.txn_isolation_name(m_txnOptions)
                  + ") is not supported");
          }

	  stmt = m_conn.createStatement();
	  ResultSet set;
	  // get branch count
	  log("select max(branch) from " + m_Driver.getBranchName() + "\n",2);
	  set = stmt.executeQuery("select max(branch) from " + m_Driver.getBranchName());
	  if(set != null && set.next()) {
	      m_nMaxBranch = set.getInt(1);
	      set.close();
          }
	  // get teller count
	  log("select max(teller) from " + m_Driver.getTellerName() + "\n",2);
	  set = stmt.executeQuery("select max(teller) from " + m_Driver.getTellerName());
	  if(set != null && set.next())  {
	      m_nMaxTeller = set.getInt(1);
	      set.close();
          }
	  // get account count
	  log("select max(account) from " + m_Driver.getAccountName() + "\n",2);
	  set = stmt.executeQuery("select max(account) from " + m_Driver.getAccountName());
	  if(set != null && set.next()) {
	      m_nMaxAccount = set.getInt(1);
	      set.close();
          }

      } catch(SQLException e) {
	  SQLError = new String(e.getMessage());
	  log ("[" + e.getSQLState() + "] " + SQLError + "\n", 1);
	  pane.oneThreadLess();
	  if (m_bCloseConnection) {
	      try {
		m_conn.close();
		log("Thread connection closed",2);
	      } catch(SQLException ee) { }
          }
	  return;
      } finally	{
	  if (stmt != null) {
	      try {
		  stmt.close();
              } catch(SQLException e) { }
	      stmt = null;
          }
      }
      try {
        switch(m_sqlOptions) {
            case RUN_TEXT:

              if (m_BatchSize > 1)
	        runTextTest_Batch();
              else

	        runTextTest();
              break;
            case RUN_PREPARED:

              if (m_BatchSize > 1)
 	        runPrepareTest_Batch();
              else

 	        runPrepareTest();
              break;
            case RUN_SPROC:
	      runProcTest();
              break;
        }
      } catch (SQLException e){
	  SQLError = new String(e.getMessage());
	  log ("[" + e.getSQLState() + "] " + SQLError + "\n", 1);
      }
      if (m_bCloseConnection) {
	  try {
	      m_conn.close();
	      log("Thread connection closed",2);
          } catch(SQLException ee) { }
      }

      pane.oneThreadLess();

      if (m_log != null)
	m_log.taskDone();
    }

  class LogRunner implements Runnable {
    int nLevel;
    String strMessage;
    Logger m_log;

    public LogRunner(String strMessage, int nLevel, Logger m_log)
      {
	this.strMessage = strMessage;
	this.nLevel = nLevel;
	this.m_log = m_log;
      }
    public void run()
      {
	if (m_log != null)
	  m_log.log(strMessage,nLevel);
      }
  }

  synchronized void log(String strMessage, int nLevel) {

    if (m_log != null)
      SwingUtilities.invokeLater(new LogRunner(strMessage, nLevel, m_log));
  }

  public void executeQuery() throws SQLException {
    log("select account, branch, balance, filler from " + m_Driver.getAccountName() + " where account < 101\n",2);
    Statement stmt = null;
    try {
      if (m_bTransactionsUsed)
        m_conn.commit();

      if (m_scrsOptions == ResultSet.TYPE_FORWARD_ONLY) { 

        stmt = m_conn.createStatement();
        ResultSet rs = stmt.executeQuery("select account, branch, balance, filler from " + m_Driver.getAccountName() + " where account < 101");
        while(rs.next()) {
	  rs.getInt(1); rs.getInt(2);  rs.getDouble(3); rs.getString(4);
        }

      } else {
        stmt = m_conn.createStatement(m_scrsOptions, ResultSet.CONCUR_READ_ONLY);
        ResultSet rs = stmt.executeQuery("select account, branch, balance, filler from " + m_Driver.getAccountName() + " where account < 101");
        for (int nRep = 0; nRep < m_travCount; nRep++) {
          while(rs.next()) {
	    rs.getInt(1); rs.getInt(2); rs.getDouble(3); rs.getString(4);
          }
          while(rs.previous()) {
	    rs.getInt(1); rs.getInt(2); rs.getDouble(3); rs.getString(4);
          }
        }
      }

    } finally {
      if (stmt != null) 
        stmt.close();
      stmt = null;
    }
  }

  void runProcTest() throws SQLException
  {
      int nAccNum = 1, nBranchNum = 1, nTellerNum = 1;
      double dDelta = 0.0, dBalance = 0.0;
      CallableStatement stmt = null;
      boolean isCreateParam = true;

      try {
	m_bTransactionsUsed = m_bTrans && m_conn.getMetaData().supportsTransactions();

	log("Starting procedure benchmark for " + m_nNumRuns + ((m_time>0)?" min.\n":" runs\n"),0);
	java.util.Random rand = new java.util.Random(System.currentTimeMillis() * hashCode());

	stmt = m_conn.prepareCall("{call ODBC_BENCHMARK(?,?,?,?,?,?,?)}");

	for (int nRun = 0; (m_time>0)?true:nRun < m_nNumRuns; nRun++) {
	  if (cancel || isInterrupted())
	     return;
	  yield();

	  if (m_time>0)	{
             long current = System.currentTimeMillis();
	     if ((current - m_time)>(m_nNumRuns*60000)) {
	        break;
             } else {
		if (nRun % prop_update_freq == 0)
		   setBarValue((int)((current - m_time)/1000));
             }
          } else if (nRun % prop_update_freq == 0) {
             setBarValue(nRun);
          }
	  try {
            if (isCreateParam) {
	      nAccNum = (int)(rand.nextFloat() * rand.nextFloat() * (m_nMaxAccount - 1)) + 1;
	      nBranchNum = (int)(rand.nextFloat() * (m_nMaxBranch - 1)) + 1;
	      nTellerNum = (int)(rand.nextFloat() * (m_nMaxTeller - 1)) + 1;
	      dDelta = ((double)((long)((((int)(rand.nextFloat() * (m_nMaxTeller - 1)) + 1) *
	 	      (rand.nextFloat() > 0.5 ? -1 : 1)) * 100))) / 100;
            }
	    stmt.clearParameters();
	    stmt.setInt(1,nRun + 1);
	    stmt.setInt(2,nAccNum);
	    stmt.setInt(3,nTellerNum);
	    stmt.setInt(4,nBranchNum);
	    stmt.setFloat(5,(float)dDelta);
	    stmt.registerOutParameter(6,Types.FLOAT);
	    stmt.setString(7,BenchPanel.strFiller.substring(0,22));

	    long startTime = System.currentTimeMillis();
	    log("{call ODBC_BENCHMARK(" + nRun + ", " + nAccNum + "," + nTellerNum + "," +
	        nBranchNum + "," + dDelta + ",?,\'" + BenchPanel.strFiller.substring(0,22) +
	        "\')}\n", 2);
	    stmt.execute();
	    stmt.getFloat(6);
	    if (m_bQuery)
	      executeQuery();
	    if(m_bTransactionsUsed)
	      m_conn.commit();

            isCreateParam = true;

	    long endTime = System.currentTimeMillis();
	    m_nTrans += 1; double diff = endTime - startTime;
	    pane.oneTransMore();
	    if (diff < 1000)
	      m_nTrans1Sec += 1;
  	    else if(diff < 2000)
	      m_nTrans2Sec += 1;
	    m_nTimeSum += diff / 1000;
	  } catch(SQLException e1) {
            boolean deadLock = false;
            SQLException ex = e1;
            while (ex != null) {
              if (ex.getSQLState().toString().equals("40001"))
                deadLock = true;
              ex = ex.getNextException();
            }
            if (deadLock) {
                try {
		  log("In deadlock handler\n", 1);
                  try {
                    this.sleep(100);
                  } catch (Exception e) {}
		  if (false && m_bTransactionsUsed)
		    m_conn.rollback();
		} catch (SQLException e2) {
                  log("***Error in rollback [" + e2.getSQLState() + "] : " + e2.getMessage() + "\n", 1);
		}
                isCreateParam = false;
		continue;
            }
	    throw e1;
          }
	}
      } finally	{
	 if (stmt != null)
	    try {
		stmt.close();
            } catch(SQLException e) {}
	 stmt = null;
      }
  }


  void runPrepareTest() throws SQLException
  {
    PreparedStatement updAccStmt = null, selAccStmt = null, updTellerStmt = null;
    PreparedStatement updBranchStmt = null, insHistStmt = null;
    int nAccNum = 1, nBranchNum = 1, nTellerNum = 1;
    double dDelta = 0.0, dBalance = 0.0;
    boolean isCreateParam = true;

    try {
      boolean bCurrentAutoCommit = m_conn.getAutoCommit();
      m_bTransactionsUsed = m_bTrans && m_conn.getMetaData().supportsTransactions();
      m_conn.setAutoCommit(!m_bTransactionsUsed);
      log("Starting SQL prepare/execute benchmark for " + m_nNumRuns + ((m_time>0)?" min.\n":" runs\n"),0);
      java.util.Random rand = new java.util.Random(System.currentTimeMillis() * hashCode());

      // prepare statements
      updAccStmt = m_conn.prepareStatement("UPDATE " +
	      m_Driver.getAccountName() + " SET balance = balance + ? WHERE account = ?");
      selAccStmt = m_conn.prepareStatement("SELECT balance FROM " +
	      m_Driver.getAccountName() + " WHERE account = ?");
      updTellerStmt = m_conn.prepareStatement("UPDATE " +
	      m_Driver.getTellerName() + " SET balance = balance + ? WHERE teller = ?");
      updBranchStmt = m_conn.prepareStatement("UPDATE " +
	      m_Driver.getBranchName() + " SET balance = balance + ? WHERE branch = ?");
      insHistStmt = m_conn.prepareStatement("INSERT INTO " +
	      m_Driver.getHistoryName() + " (histid, account, teller, branch, amount, timeoftxn, filler) VALUES (? , ? , ? , ? , ? , " + m_nowFunction + " , ?)");

      for (int nRun = 0; ((m_time>0) ? true : nRun < m_nNumRuns); nRun++) {
        if (cancel || isInterrupted())
	   return;
	yield();
	if (m_time>0) {
          long current = System.currentTimeMillis();
	  if ((current - m_time) > (m_nNumRuns * 60000)) {
	     break;
	  } else {
             if (nRun % prop_update_freq == 0)
		setBarValue((int)((current - m_time)/1000));
          }
	} else if (nRun % prop_update_freq == 0)
	   setBarValue(nRun);
        try {
          if (isCreateParam) {
		  nAccNum = (int)(rand.nextFloat() * rand.nextFloat() * (m_nMaxAccount - 1)) + 1;
		  nBranchNum = (int)(rand.nextFloat() * (m_nMaxBranch - 1)) + 1;
		  nTellerNum = (int)(rand.nextFloat() * (m_nMaxTeller - 1)) + 1;
		  dDelta = ((double)((long)((((int)(rand.nextFloat() * (m_nMaxTeller - 1)) + 1) * (rand.nextFloat() > 0.5 ? -1 : 1)) * 100))) / 100;
          }
	  // bind parameters
	  updAccStmt.setDouble(1,dDelta); updAccStmt.setInt(2,nAccNum);
	  //					System.out.println(nAccNum);
	  selAccStmt.setInt(1,nAccNum);
	  updTellerStmt.setDouble(1,dDelta); updTellerStmt.setInt(2,nTellerNum);
	  updBranchStmt.setDouble(1,dDelta); updBranchStmt.setInt(2,nBranchNum);
	  insHistStmt.setInt(1,nRun); insHistStmt.setInt(2,nAccNum);
	  insHistStmt.setInt(3,nTellerNum); insHistStmt.setInt(4,nBranchNum);
	  insHistStmt.setDouble(5,dDelta); insHistStmt.setString(6,BenchPanel.strFiller.substring(0,21));
	  long startTime = System.currentTimeMillis();
	  // execute statements
	  log("UPDATE " + m_Driver.getAccountName() + " SET balance = balance + " + dDelta + " WHERE account = " + nAccNum + "\n",2);
	  updAccStmt.executeUpdate();
	  log("SELECT balance FROM " + m_Driver.getAccountName() + " WHERE account = " + nAccNum + "\n",2);
	  ResultSet balanceSet = selAccStmt.executeQuery();
	  balanceSet.next(); dBalance = balanceSet.getFloat(1);
	  balanceSet.close();
	  log("UPDATE " + m_Driver.getTellerName() + " SET balance = balance + " + dDelta + " WHERE teller = " + nTellerNum + "\n",2);
	  updTellerStmt.executeUpdate();
	  log("UPDATE " + m_Driver.getBranchName() + " SET balance = balance + " + dDelta + " WHERE branch = " + nBranchNum + "\n",2);
	  updBranchStmt.executeUpdate();
	  log("INSERT INTO " + m_Driver.getHistoryName() + " (histid, account, teller, branch, amount, timeoftxn, filler) VALUES (" + nRun + " , " + nAccNum + " , " + nTellerNum + " , " + nBranchNum + " , " + dDelta + " , " + m_nowFunction + " , \'" + BenchPanel.strFiller.substring(0,21) + "\')\n",2);
	  insHistStmt.executeUpdate();
	  if(m_bQuery)
	    executeQuery();
	  if(m_bTransactionsUsed)
	    m_conn.commit();

          isCreateParam = true;

	  long endTime = System.currentTimeMillis();
	  m_nTrans += 1; double diff = endTime - startTime;
	  pane.oneTransMore();
	  if(diff < 1000)
	    m_nTrans1Sec += 1;
	  else if(diff < 2000)
	    m_nTrans2Sec += 1;
	  m_nTimeSum += diff / 1000;
        } catch (SQLException e1) {
            boolean deadLock = false;
            SQLException ex = e1;
            while (ex != null) {
              if (ex.getSQLState().toString().equals("40001"))
                deadLock = true;
              ex = ex.getNextException();
            }
            if (deadLock) {
                try {
		  log("In deadlock handler\n", 1);
                  try {
                    this.sleep(100);
                  } catch (Exception e) {}
		  if (false && m_bTransactionsUsed)
		    m_conn.rollback();
		} catch (SQLException e2) {
                  log("***Error in rollback [" + e2.getSQLState() + "] : " + e2.getMessage() + "\n", 1);
		}
                isCreateParam = false;
		continue;
            }
	    throw e1;
        }
      }
      m_conn.setAutoCommit(bCurrentAutoCommit);
    } finally {
	try {
	  if (updAccStmt != null)
            updAccStmt.close();
	  if (selAccStmt != null)
            selAccStmt.close();
	  if (updTellerStmt != null)
            updTellerStmt.close();
	  if (updBranchStmt != null)
            updBranchStmt.close();
	  if (insHistStmt != null)
            insHistStmt.close();
	} catch(SQLException e) { }
    }
  }

  void runTextTest() throws SQLException
  {
    Statement stmt = null;
    String tmp;
    int nAccNum = 1, nBranchNum = 1, nTellerNum = 1;
    double dDelta = 1, dBalance = 1;
    boolean isCreateParam = true;

    try	{
      boolean bCurrentAutoCommit = m_conn.getAutoCommit();
      m_bTransactionsUsed = m_bTrans && m_conn.getMetaData().supportsTransactions();
      m_conn.setAutoCommit(!m_bTransactionsUsed);
      stmt = m_conn.createStatement();

      log("Starting SQL text benchmark for " + m_nNumRuns + ((m_time>0)?" min.\n":" runs\n"),0);
      java.util.Random rand = new java.util.Random(System.currentTimeMillis() * hashCode());

      for (int nRun = 0; ((m_time>0) ? true : nRun < m_nNumRuns); nRun++) {
        if (cancel || isInterrupted())
	  return;

        yield();
	if (m_time>0) {
          long current = System.currentTimeMillis();
	  if ((current - m_time) > (m_nNumRuns*60000)) {
             break;
          } else {
	      if (nRun % prop_update_freq == 0)
	 	setBarValue((int)((current - m_time)/1000));
	  }
        } else if (nRun % prop_update_freq == 0)
	  setBarValue(nRun);

	try {
          if (isCreateParam) {
 	    nAccNum = (int)(rand.nextFloat() * rand.nextFloat() * (m_nMaxAccount - 1)) + 1;
	    nBranchNum = (int)(rand.nextFloat() * (m_nMaxBranch - 1)) + 1;
	    nTellerNum = (int)(rand.nextFloat() * (m_nMaxTeller - 1)) + 1;
	    dDelta = ((double)((long)((((int)(rand.nextFloat() * (m_nMaxTeller - 1)) + 1) * (rand.nextFloat() > 0.5 ? -1 : 1)) * 100))) / 100;
          }

	  long startTime = System.currentTimeMillis();

          tmp = "UPDATE " + m_Driver.getAccountName() + " SET balance = balance + "
                + dDelta + " WHERE account = " + nAccNum;
	   log(tmp + "\n",2);
          stmt.executeUpdate(tmp);
          tmp = "SELECT balance FROM " + m_Driver.getAccountName() + " WHERE account = " + nAccNum;
	   log(tmp + "\n",2);
          ResultSet balanceSet = stmt.executeQuery(tmp);
	  balanceSet.next();
	  dBalance = balanceSet.getDouble(1);
	  balanceSet.close();

          tmp = "UPDATE " + m_Driver.getTellerName() +
	      " SET balance = balance + " + dDelta +
	      " WHERE teller = " + nTellerNum;
	  log(tmp + "\n", 2);
	  stmt.executeUpdate(tmp);

	  tmp = "UPDATE " + m_Driver.getBranchName() +
	      " SET balance = balance + " + dDelta +
	      " WHERE branch = " + nBranchNum;
	  log(tmp + "\n", 2);
	  stmt.executeUpdate(tmp);

	  tmp = "INSERT INTO " + m_Driver.getHistoryName() +
	      " (histid, account, teller, branch, amount, timeoftxn, filler) VALUES (" +
	      nRun + " , " + nAccNum + " , " + nTellerNum + " , " + nBranchNum + " , " +
	      dDelta + " , " + m_nowFunction + " , \'" + BenchPanel.strFiller.substring(0,21) +
 	      "\')";
	  log(tmp + "\n", 2);
	  stmt.executeUpdate(tmp);

	  if (m_bQuery)
	    executeQuery();
	  if (m_bTransactionsUsed)
	    m_conn.commit();

          isCreateParam = true;

	  long endTime = System.currentTimeMillis();
	  m_nTrans += 1; long diff = endTime - startTime;
	  pane.oneTransMore();
	  if (diff < 1000)
	    m_nTrans1Sec += 1;
	  else if (diff < 2000)
	    m_nTrans2Sec += 1;
	  m_nTimeSum += ((double)diff) / 1000;
        } catch (SQLException e1) {
            boolean deadLock = false;
            SQLException ex = e1;
            while (ex != null) {
              if (ex.getSQLState().toString().equals("40001"))
                deadLock = true;
              ex = ex.getNextException();
            }
            if (deadLock) {
                try {
		  log("In deadlock handler\n", 1);
                  try {
                    this.sleep(100);
                  } catch (Exception e) {}
		  if (false && m_bTransactionsUsed)
		    m_conn.rollback();
		} catch (SQLException e2) {
                  log("***Error in rollback [" + e2.getSQLState() + "] : " + e2.getMessage() + "\n", 1);
		}
                isCreateParam = false;
		continue;
            }
	    throw e1;
        }
      }
      m_conn.setAutoCommit(bCurrentAutoCommit);
    } finally {
	if (stmt != null)
	   try {
	      stmt.close();
           } catch(SQLException e) { }
    }
  }




  void runTextTest_Batch() throws SQLException
  {
    Statement stmt = null;
    String tmp;
    int[] nAccNum = new int[m_BatchSize];
    int[] nBranchNum = new int[m_BatchSize];
    int[] nTellerNum = new int[m_BatchSize];
    double[] dDelta = new double[m_BatchSize];
    double[] dBalance = new double[m_BatchSize];
    boolean isCreateParam = true;

    try	{
      boolean bCurrentAutoCommit = m_conn.getAutoCommit();
      m_bTransactionsUsed = m_bTrans && m_conn.getMetaData().supportsTransactions();
      m_conn.setAutoCommit(!m_bTransactionsUsed);
      stmt = m_conn.createStatement();

      log("Starting SQL text benchmark for " + m_nNumRuns + ((m_time>0)?" min.\n":" runs\n"),0);
      java.util.Random rand = new java.util.Random(System.currentTimeMillis() * hashCode());

      for (int nRun = 0; ((m_time>0) ? true : nRun < m_nNumRuns); nRun++) {
        if (cancel || isInterrupted())
	  return;

        yield();
	if (m_time>0) {
          long current = System.currentTimeMillis();
	  if ((current - m_time) > (m_nNumRuns*60000)) {
             break;
          } else {
	      if (nRun % prop_update_freq == 0)
	 	setBarValue((int)((current - m_time)/1000));
	  }
        } else if (nRun % prop_update_freq == 0)
	  setBarValue(nRun);

	try {
          if (isCreateParam)
            for (int i = 0; i < m_BatchSize; i++) {
	      nAccNum[i] = (int)(rand.nextFloat() * rand.nextFloat() * (m_nMaxAccount - 1)) + 1;
	      nBranchNum[i] = (int)(rand.nextFloat() * (m_nMaxBranch - 1)) + 1;
	      nTellerNum[i] = (int)(rand.nextFloat() * (m_nMaxTeller - 1)) + 1;
	      dDelta[i] = ((double)((long)((((int)(rand.nextFloat() * (m_nMaxTeller - 1)) + 1)
                * (rand.nextFloat() > 0.5 ? -1 : 1)) * 100))) / 100;
            }

	  long startTime = System.currentTimeMillis();
          for(int i = 0; i < m_BatchSize; i++) {
            tmp = "UPDATE " + m_Driver.getAccountName() + " SET balance = balance + "
              + dDelta[i] + " WHERE account = " + nAccNum[i];
	    log(tmp + "\n",2);
            stmt.addBatch(tmp);
          }
          stmt.executeBatch();
          stmt.clearBatch();

          for(int i = 0; i < m_BatchSize; i++) {
            tmp = "SELECT balance FROM " + m_Driver.getAccountName()
              + " WHERE account = " + nAccNum[i];
	    log(tmp + "\n",2);
            ResultSet balanceSet = stmt.executeQuery(tmp);
	    balanceSet.next();
	    dBalance[i] = balanceSet.getDouble(1);
	    balanceSet.close();
          }

          for(int i = 0; i < m_BatchSize; i++) {
            tmp = "UPDATE " + m_Driver.getTellerName() +
              " SET balance = balance + " + dDelta[i] +
	      " WHERE teller = " + nTellerNum[i];
	    log(tmp + "\n", 2);
            stmt.addBatch(tmp);
          }
	  stmt.executeBatch();
          stmt.clearBatch();

          for(int i = 0; i < m_BatchSize; i++) {
	    tmp = "UPDATE " + m_Driver.getBranchName() +
	      " SET balance = balance + " + dDelta[i] +
	      " WHERE branch = " + nBranchNum[i];
	    log(tmp + "\n", 2);
            stmt.addBatch(tmp);
          }
	  stmt.executeBatch();
          stmt.clearBatch();

          for(int i = 0; i < m_BatchSize; i++) {
	    tmp = "INSERT INTO " + m_Driver.getHistoryName() +
	      " (histid, account, teller, branch, amount, timeoftxn, filler) VALUES ("
              + nRun + " , " + nAccNum[i] + " , " + nTellerNum[i] + " , "
              + nBranchNum[i] + " , " + dDelta[i] + " , " + m_nowFunction + " , \'"
              + BenchPanel.strFiller.substring(0,21) +
 	      "\')";
	    log(tmp + "\n", 2);
            stmt.addBatch(tmp);
          }
	  stmt.executeBatch();
          stmt.clearBatch();

	  if (m_bQuery)
	    executeQuery();
	  if (m_bTransactionsUsed)
	    m_conn.commit();

          isCreateParam = true;

	  long endTime = System.currentTimeMillis();
	  m_nTrans += m_BatchSize;
          long diff = endTime - startTime;
	  pane.addTrans(m_BatchSize);
	  if (diff < 1000)
	    m_nTrans1Sec += m_BatchSize;
	  else if (diff < 2000)
	    m_nTrans2Sec += m_BatchSize;
	  m_nTimeSum += ((double)diff) / 1000;
        } catch (SQLException e1) {
            boolean deadLock = false;
            SQLException ex = e1;
            while (ex != null) {
              if (ex.getSQLState().toString().equals("40001"))
                deadLock = true;
              ex = ex.getNextException();
            }
            if (deadLock) {
                try {
		  log("In deadlock handler\n", 1);
                  try {
                    this.sleep(100);
                  } catch (Exception e) {}
		  if (false && m_bTransactionsUsed)
		    m_conn.rollback();
		} catch (SQLException e2) {
                  log("***Error in rollback [" + e2.getSQLState() + "] : " + e2.getMessage() + "\n", 1);
		}
                isCreateParam = false;
		continue;
            }
	    throw e1;
        }
      }
      m_conn.setAutoCommit(bCurrentAutoCommit);
    } finally {
	if (stmt != null)
	   try {
	      stmt.close();
           } catch(SQLException e) { }
    }
  }


  void runPrepareTest_Batch() throws SQLException
  {
    PreparedStatement updAccStmt = null, selAccStmt = null, updTellerStmt = null;
    PreparedStatement updBranchStmt = null, insHistStmt = null;
    int[] nAccNum = new int[m_BatchSize];
    int[] nBranchNum = new int[m_BatchSize];
    int[] nTellerNum = new int[m_BatchSize];
    double[] dDelta = new double[m_BatchSize];
    double[] dBalance = new double[m_BatchSize];
    boolean isCreateParam = true;

    try {
      boolean bCurrentAutoCommit = m_conn.getAutoCommit();
      m_bTransactionsUsed = m_bTrans && m_conn.getMetaData().supportsTransactions();
      m_conn.setAutoCommit(!m_bTransactionsUsed);

      log("Starting SQL prepare/execute benchmark for " + m_nNumRuns + ((m_time>0)?" min.\n":" runs\n"),0);
      java.util.Random rand = new java.util.Random(System.currentTimeMillis() * hashCode());

      // prepare statements
      updAccStmt = m_conn.prepareStatement("UPDATE " +
	      m_Driver.getAccountName() + " SET balance = balance + ? WHERE account = ?");
      selAccStmt = m_conn.prepareStatement("SELECT balance FROM " +
	      m_Driver.getAccountName() + " WHERE account = ?");
      updTellerStmt = m_conn.prepareStatement("UPDATE " +
	      m_Driver.getTellerName() + " SET balance = balance + ? WHERE teller = ?");
      updBranchStmt = m_conn.prepareStatement("UPDATE " +
	      m_Driver.getBranchName() + " SET balance = balance + ? WHERE branch = ?");
      insHistStmt = m_conn.prepareStatement("INSERT INTO " +
	      m_Driver.getHistoryName() + " (histid, account, teller, branch, amount, timeoftxn, filler) VALUES (? , ? , ? , ? , ? , " + m_nowFunction + " , ?)");

      for (int nRun = 0; ((m_time>0) ? true : nRun < m_nNumRuns); nRun++) {
        if (cancel || isInterrupted())
	   return;
	yield();
	if (m_time>0) {
          long current = System.currentTimeMillis();
	  if ((current - m_time) > (m_nNumRuns * 60000)) {
	     break;
	  } else {
             if (nRun % prop_update_freq == 0)
		setBarValue((int)((current - m_time)/1000));
          }
	} else if (nRun % prop_update_freq == 0)
	   setBarValue(nRun);
        try {
          if (isCreateParam)
            for (int i = 0; i < m_BatchSize; i++) {
	      nAccNum[i] = (int)(rand.nextFloat() * rand.nextFloat() * (m_nMaxAccount - 1)) + 1;
	      nBranchNum[i] = (int)(rand.nextFloat() * (m_nMaxBranch - 1)) + 1;
	      nTellerNum[i] = (int)(rand.nextFloat() * (m_nMaxTeller - 1)) + 1;
	      dDelta[i] = ((double)((long)((((int)(rand.nextFloat() * (m_nMaxTeller - 1)) + 1) * (rand.nextFloat() > 0.5 ? -1 : 1)) * 100))) / 100;
            }

          for (int i = 0; i < m_BatchSize; i++) {
	    // bind parameters
 	    log("UPDATE " + m_Driver.getAccountName() + " SET balance = balance + "
              + dDelta + " WHERE account = " + nAccNum[i] + "\n",2);
	    updAccStmt.setDouble(1,dDelta[i]);
            updAccStmt.setInt(2,nAccNum[i]);
            updAccStmt.addBatch();

            log("UPDATE " + m_Driver.getTellerName() + " SET balance = balance + "
              + dDelta[i] + " WHERE teller = " + nTellerNum[i] + "\n",2);
	    updTellerStmt.setDouble(1,dDelta[i]);
            updTellerStmt.setInt(2,nTellerNum[i]);
            updTellerStmt.addBatch();

            log("UPDATE " + m_Driver.getBranchName() + " SET balance = balance + "
              + dDelta[i] + " WHERE branch = " + nBranchNum[i] + "\n",2);
	    updBranchStmt.setDouble(1,dDelta[i]);
            updBranchStmt.setInt(2,nBranchNum[i]);
            updBranchStmt.addBatch();

	    log("INSERT INTO " + m_Driver.getHistoryName()
              + " (histid, account, teller, branch, amount, timeoftxn, filler) VALUES ("
              + nRun + " , " + nAccNum[i] + " , " + nTellerNum[i] + " , "
              + nBranchNum[i] + " , " + dDelta[i] + " , " + m_nowFunction + " , \'"
              + BenchPanel.strFiller.substring(0,21) + "\')\n",2);
	    insHistStmt.setInt(1,nRun);
            insHistStmt.setInt(2,nAccNum[i]);
	    insHistStmt.setInt(3,nTellerNum[i]);
            insHistStmt.setInt(4,nBranchNum[i]);
	    insHistStmt.setDouble(5,dDelta[i]);
            insHistStmt.setString(6,BenchPanel.strFiller.substring(0,21));
            insHistStmt.addBatch();
          }
	  long startTime = System.currentTimeMillis();

          // execute statements
	  updAccStmt.executeBatch();
          updAccStmt.clearBatch();

          for(int i = 0; i < m_BatchSize; i++) {
	    log("SELECT balance FROM " + m_Driver.getAccountName()
              + " WHERE account = " + nAccNum[i] + "\n",2);

            selAccStmt.setInt(1,nAccNum[i]);
	    ResultSet balanceSet = selAccStmt.executeQuery();
	    balanceSet.next();
            dBalance[i] = balanceSet.getFloat(1);
	    balanceSet.close();
	  }

          updTellerStmt.executeBatch();
          updTellerStmt.clearBatch();

          updBranchStmt.executeBatch();
          updBranchStmt.clearBatch();

	  insHistStmt.executeBatch();
	  insHistStmt.clearBatch();
	  if(m_bQuery)
	    executeQuery();
	  if(m_bTransactionsUsed)
	    m_conn.commit();

          isCreateParam = true;

	  long endTime = System.currentTimeMillis();
	  m_nTrans += m_BatchSize;
          double diff = endTime - startTime;
	  pane.addTrans(m_BatchSize);
	  if(diff < 1000)
            m_nTrans1Sec += m_BatchSize;
          else if(diff < 2000)
	    m_nTrans2Sec += m_BatchSize;
          m_nTimeSum += diff / 1000;
        } catch (SQLException e1) {
            boolean deadLock = false;
            SQLException ex = e1;
            while (ex != null) {
              if (ex.getSQLState().toString().equals("40001"))
                deadLock = true;
              ex = ex.getNextException();
            }
            if (deadLock) {
                try {
		  log("In deadlock handler\n", 1);
                  try {
                    this.sleep(100);
                  } catch (Exception e) {}
		  if (false && m_bTransactionsUsed)
		    m_conn.rollback();
		} catch (SQLException e2) {
                  log("***Error in rollback [" + e2.getSQLState() + "] : " + e2.getMessage() + "\n", 1);
		}
                isCreateParam = false;
		continue;
            }
	    throw e1;
        }
      }
      m_conn.setAutoCommit(bCurrentAutoCommit);
    } finally {
	try {
	  if (updAccStmt != null)
            updAccStmt.close();
	  if (selAccStmt != null)
            selAccStmt.close();
	  if (updTellerStmt != null)
            updTellerStmt.close();
	  if (updBranchStmt != null)
            updBranchStmt.close();
	  if (insHistStmt != null)
            insHistStmt.close();
	} catch(SQLException e) { }
    }
  }


}

