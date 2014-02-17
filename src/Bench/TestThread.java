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

import java.sql.*;
import javax.swing.*;

public class TestThread extends Thread 
{
   Logger m_log = null;

   Progress m_progress = null;

   Connection m_conn;

   String m_nowFunction = null;

   Driver m_Driver;

   int m_nNumRuns;

   boolean m_bRunText;

   boolean m_bRunPrepared;

   boolean m_bRunSProc;

   boolean m_bTrans;

   boolean m_bQuery;

   boolean m_bTransactionsUsed = false;

   int m_nTrans = 0;

   int m_nTrans1Sec = 0;

   int m_nTrans2Sec = 0;

   double m_nTimeSum = 0;

   int m_nMaxBranch = 10, m_nMaxTeller = 100, m_nMaxAccount = 1000;

   boolean m_bCloseConnection = false;
	
	long m_time;

   public int getNTrans()
   {
      return m_nTrans;
   }

   public int getTrans1Sec()
   {
      return m_nTrans1Sec;
   }

   public int getTrans2Sec()
   {
      return m_nTrans2Sec;
   }

   public double getTotalTime()
   {
      return m_nTimeSum;
   }

   public TestThread(ThreadGroup group, String strThreadName, String strURL, String strUserName, String strPassword, Logger logger, Progress progress, Driver driver, int nNumRuns, long start_time,boolean bRunText, boolean bRunPrepared, boolean bRunSProc, boolean bTrans, boolean bQuery, String strNowFunction) throws SQLException
   {
      this (group,strThreadName,DriverManager.getConnection(strURL,strUserName,strPassword),logger,progress,driver,nNumRuns,start_time,bRunText,bRunPrepared,bRunSProc,bTrans,bQuery,strNowFunction);
      m_bCloseConnection = true;
   }

   public TestThread(ThreadGroup group, String strThreadName, Connection conn, Logger logger, Progress progress, Driver driver, int nNumRuns, long start_time,boolean bRunText, boolean bRunPrepared, boolean bRunSProc, boolean bTrans, boolean bQuery, String strNowFunction) throws SQLException
   {
      super(group,strThreadName);
      m_log = logger;
      m_progress = progress;
      m_conn = conn;
      m_Driver = driver;
      if(m_Driver == null) throw new SQLException("Must supply a driver type");
      m_nNumRuns = nNumRuns;
      m_bRunText = bRunText;
      m_bRunPrepared = bRunPrepared;
      m_bRunSProc = bRunSProc;
      m_bTrans = bTrans;
      m_bQuery = bQuery;
      m_nowFunction = strNowFunction;
		m_time = start_time;
   }

   public void run()
   {
      m_nTrans = 0;
      m_nTrans1Sec = 0;
      m_nTrans2Sec = 0;
      m_nTimeSum = 0;
      if(m_nNumRuns <= 0)
         return;
      Statement stmt = null;
      try
      {
         stmt = m_conn.createStatement();
         ResultSet set;
         // get branch count
         log("select max(branch) from " + m_Driver.getBranchName() + "\n",2);
         set = stmt.executeQuery("select max(branch) from " + m_Driver.getBranchName());
         if(set != null && set.next())
         {
            m_nMaxBranch = set.getInt(1);
            set.close();
         }
         // get teller count
         log("select max(teller) from " + m_Driver.getTellerName() + "\n",2);
         set = stmt.executeQuery("select max(teller) from " + m_Driver.getTellerName());
         if(set != null && set.next())
         {
            m_nMaxTeller = set.getInt(1);
            set.close();
         }
         // get account count
         log("select max(account) from " + m_Driver.getAccountName() + "\n",2);
         set = stmt.executeQuery("select max(account) from " + m_Driver.getAccountName());
         if(set != null && set.next())
         {
            m_nMaxAccount = set.getInt(1);
            set.close();
         }
      }
      catch(SQLException e)
      {
         log("Error getting table limits : " + e.getMessage() + "\n",0);
      }
      finally
      {
         if(stmt != null)
            try
            {
               stmt.close();
            }
            catch(SQLException e)
            {
            }
         stmt = null;
      }
      //		System.out.println("Thread : " + getName() + " Branch :" + m_nMaxBranch + " Teller : " + m_nMaxTeller + " Account : " + m_nMaxAccount);
      if(m_bRunText)
         runTextTest();
      if(m_bRunPrepared)
         runPrepareTest();
      if(m_bRunSProc)
         runProcTest();
      if(m_bCloseConnection)
      {
         try
         {
            m_conn.close();
            log("Thread connection closed",2);
         }
         catch(SQLException e)
         {
         }
      }
      if(m_log != null) m_log.taskDone();
   }

   synchronized void log(String strMessage, int nLevel)
   {
      if(m_log != null)
         m_log.log(strMessage,nLevel);
   }

   void setProgressMinMax(int nMin, int nMax)
   {
      if(m_progress != null)
         m_progress.setProgressMinMax(nMin,nMax);
   }

   void setProgressValue(int nValue)
   {
      if(m_progress != null)
         m_progress.setProgressValue(nValue);
   }

   public void executeQuery() throws SQLException
   {
      log("select account, branch, balance, filler from " + m_Driver.getAccountName() + " where account < 101\n",2);
      Statement stmt = null;
      try
      {
         stmt = m_conn.createStatement();
         ResultSet set = stmt.executeQuery("select account, branch, balance, filler from " + m_Driver.getAccountName() + " where account < 101");
         while(set.next())
         {
            set.getInt(1);
            set.getInt(2);
            set.getDouble(3);
            set.getString(4);
         }
         set.close();
      }
      finally
      {
         if(stmt != null)
            stmt.close();
         stmt = null;
      }
   }

   void runProcTest()
   {
      setProgressMinMax(0,m_nNumRuns - 1);
      int nProgressStep = m_nNumRuns / 10 + 1;
      CallableStatement stmt = null;
      try
      {
         boolean m_bTransactionsUsed = m_bTrans && m_conn.getMetaData().supportsTransactions();
         stmt = m_conn.prepareCall("{call ODBC_BENCHMARK(?,?,?,?,?,?,?)}");
         int nAccNum, nBranchNum, nTellerNum;
         double dDelta, dBalance;
         log("Starting procedure benchmark for " + m_nNumRuns + ((m_time>0)?" min.\n":" runs\n"),0);
			java.util.Random rand = new java.util.Random(m_nMaxAccount+m_nMaxBranch+m_nMaxTeller);
         for(int nRun = 0;(m_time>0)?true:nRun < m_nNumRuns;nRun++)
         {
				if(m_time>0) {
					java.util.Date current = new java.util.Date();
					if((current.getTime()-m_time)>(m_nNumRuns*60000)) break;
				}
            try
            {
               nAccNum = (int)(rand.nextFloat() * rand.nextFloat() * (m_nMaxAccount - 1)) + 1;
               nBranchNum = (int)(rand.nextFloat() * (m_nMaxBranch - 1)) + 1;
               nTellerNum = (int)(rand.nextFloat() * (m_nMaxTeller - 1)) + 1;
               dDelta = ((double)((long)((((int)(rand.nextFloat() * (m_nMaxTeller - 1)) + 1) * (rand.nextFloat() > 0.5 ? -1 : 1)) * 100))) / 100;
               stmt.clearParameters();
               stmt.setInt(1,nRun + 1);
               stmt.setInt(2,nAccNum);
               stmt.setInt(3,nTellerNum);
               stmt.setInt(4,nBranchNum);
               stmt.setFloat(5,(float)dDelta);
               stmt.registerOutParameter(6,Types.FLOAT);
               stmt.setString(7,BenchPanel.strFiller.substring(0,22));
               java.util.Date startTime = new java.util.Date();
               log("{call ODBC_BENCHMARK(" + nRun + ", " + nAccNum + "," + nTellerNum + "," + nBranchNum + "," + dDelta + ",?,\'" + BenchPanel.strFiller.substring(0,22) + "\')}\n",2);
               stmt.execute();
               stmt.getFloat(6);
               if(m_bQuery)
                  executeQuery();
               java.util.Date endTime = new java.util.Date();
               m_nTrans += 1;
               double diff = endTime.getTime() - startTime.getTime();
               if(diff < 1000)
                  m_nTrans1Sec += 1;
               else
                  if(diff < 2000)
                     m_nTrans2Sec += 1;
               m_nTimeSum += diff / 1000;
            }
            catch(SQLException e1)
            {
               //					System.err.println(e1.getMessage());
               //                    e1.printStackTrace();
               break;
            }
            if(nRun % nProgressStep == 0)
               setProgressValue(nRun);
         //yield();
         }
         setProgressValue(m_nNumRuns - 1);
      }
      catch(SQLException e)
      {
         //JOptionPane.showMessageDialog(null, e.getMessage(), "SQL Error in proc test", JOptionPane.ERROR_MESSAGE);
         log("SQLError in procedure test : " + e.getMessage(),0);
      }
      finally
      {
         if(stmt != null)
            try
            {
               stmt.close();
            }
            catch(SQLException e)
            {
            }
         stmt = null;
      }
   }

   void runPrepareTest()
   {
      setProgressMinMax(0,m_nNumRuns - 1);
      int nProgressStep = m_nNumRuns / 10 + 1;
      try
      {
         boolean m_bTransactionsUsed = m_bTrans && m_conn.getMetaData().supportsTransactions();
         boolean bCurrentAutoCommit = m_conn.getAutoCommit();
         m_conn.setAutoCommit(!m_bTransactionsUsed);
         int nAccNum, nBranchNum, nTellerNum;
         double dDelta, dBalance;
         log("Starting SQL prepare/execute benchmark for " + m_nNumRuns + ((m_time>0)?" min.\n":" runs\n"),0);
			java.util.Random rand = new java.util.Random(m_nMaxAccount+m_nMaxBranch+m_nMaxTeller);
         for(int nRun = 0;(m_time>0)?true:nRun < m_nNumRuns;nRun++)
         {
				if(m_time>0) {
					java.util.Date current = new java.util.Date();
					if((current.getTime()-m_time)>(m_nNumRuns*60000)) break;
				}
            PreparedStatement updAccStmt = null, selAccStmt = null, updTellerStmt = null, updBranchStmt = null, insHistStmt = null;
            try
            {
               nAccNum = (int)(rand.nextFloat() * rand.nextFloat() * (m_nMaxAccount - 1)) + 1;
               nBranchNum = (int)(rand.nextFloat() * (m_nMaxBranch - 1)) + 1;
               nTellerNum = (int)(rand.nextFloat() * (m_nMaxTeller - 1)) + 1;
               dDelta = ((double)((long)((((int)(rand.nextFloat() * (m_nMaxTeller - 1)) + 1) * (rand.nextFloat() > 0.5 ? -1 : 1)) * 100))) / 100;
               // prepare statements
               updAccStmt = m_conn.prepareStatement("UPDATE " + m_Driver.getAccountName() + " SET balance = balance + ? WHERE account = ?");
               selAccStmt = m_conn.prepareStatement("SELECT balance FROM " + m_Driver.getAccountName() + " WHERE account = ?");
               updTellerStmt = m_conn.prepareStatement("UPDATE " + m_Driver.getTellerName() + " SET balance = balance + ? WHERE teller = ?");
               updBranchStmt = m_conn.prepareStatement("UPDATE " + m_Driver.getBranchName() + " SET balance = balance + ? WHERE branch = ?");
               insHistStmt = m_conn.prepareStatement("INSERT INTO " + m_Driver.getHistoryName() + " (histid, account, teller, branch, amount, timeoftxn, filler) VALUES (? , ? , ? , ? , ? , " + m_nowFunction + " , ?)");
               // bind parameters
               updAccStmt.setDouble(1,dDelta);
               updAccStmt.setInt(2,nAccNum);
               //					System.out.println(nAccNum);
               selAccStmt.setInt(1,nAccNum);
               updTellerStmt.setDouble(1,dDelta);
               updTellerStmt.setInt(2,nTellerNum);
               updBranchStmt.setDouble(1,dDelta);
               updBranchStmt.setInt(2,nBranchNum);
               insHistStmt.setInt(1,nRun);
               insHistStmt.setInt(2,nAccNum);
               insHistStmt.setInt(3,nTellerNum);
               insHistStmt.setInt(4,nBranchNum);
               insHistStmt.setDouble(5,dDelta);
               insHistStmt.setString(6,BenchPanel.strFiller.substring(0,21));
               java.util.Date startTime = new java.util.Date();
               // execute statements
               log("UPDATE " + m_Driver.getAccountName() + " SET balance = balance + " + dDelta + " WHERE account = " + nAccNum + "\n",2);
               updAccStmt.executeUpdate();
               log("SELECT balance FROM " + m_Driver.getAccountName() + " WHERE account = " + nAccNum + "\n",2);
               ResultSet balanceSet = selAccStmt.executeQuery();
               balanceSet.next();
               dBalance = balanceSet.getFloat(1);
               //					if (balanceSet == null)
               //						System.out.println("balanceSet is NULL");
               //					else {
               //						String strBalance = balanceSet.getString(1);
               //						System.out.println(balanceSet.wasNull() ? "SQL NULL" : strBalance);
               //					}
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
               java.util.Date endTime = new java.util.Date();
               m_nTrans += 1;
               double diff = endTime.getTime() - startTime.getTime();
               if(diff < 1000)
                  m_nTrans1Sec += 1;
               else
                  if(diff < 2000)
                     m_nTrans2Sec += 1;
               m_nTimeSum += diff / 1000;
            }
            catch(SQLException e1)
            {
               //System.err.println(e1.getMessage());
               break;
            }
            finally
            {
               try
               {
                  if(updAccStmt != null)
                     updAccStmt.close();
                  if(selAccStmt != null)
                     selAccStmt.close();
                  if(updTellerStmt != null)
                     updTellerStmt.close();
                  if(updBranchStmt != null)
                     updBranchStmt.close();
                  if(insHistStmt != null)
                     insHistStmt.close();
               }
               catch(SQLException e)
               {
               }
            }
            if(nRun % nProgressStep == 0)
               setProgressValue(nRun);
         //yield();
         }
         setProgressValue(m_nNumRuns - 1);
         m_conn.setAutoCommit(bCurrentAutoCommit);
      }
      catch(SQLException e)
      {
         // JOptionPane.showMessageDialog(null, e.getMessage(), "SQL Error in Text test", JOptionPane.ERROR_MESSAGE);
         log("SQLError in prepare test : " + e.getMessage(),0);
      }
   }

   void runTextTest()
   {
      Statement stmt = null;
      try
      {
         //			System.out.println("Thread :" + getName() + " Text test entered");
         setProgressMinMax(0,m_nNumRuns - 1);
         int nProgressStep = m_nNumRuns / 10 + 1;
         //			System.out.println("Thread :" + getName() + " before commit state");
         boolean m_bTransactionsUsed = m_bTrans && m_conn.getMetaData().supportsTransactions();
         //			System.out.println("Thread :" + getName() + " are transactions used");
         boolean bCurrentAutoCommit = m_conn.getAutoCommit();
         //			System.out.println("Thread :" + getName() + "got currentCommit");
         m_conn.setAutoCommit(!m_bTransactionsUsed);
         //			System.out.println("Thread :" + getName() + " commit state set");
         stmt = m_conn.createStatement();
         int nAccNum, nBranchNum, nTellerNum;
         double dDelta, dBalance;
         log("Starting SQL text benchmark for " + m_nNumRuns + ((m_time>0)?" min.\n":" runs\n"),0);
			java.util.Random rand = new java.util.Random(m_nMaxAccount+m_nMaxBranch+m_nMaxTeller);
         for(int nRun = 0;(m_time>0)?true:nRun < m_nNumRuns;nRun++)
         {
				if(m_time>0) {
					java.util.Date current = new java.util.Date();
					if((current.getTime()-m_time)>(m_nNumRuns*60000)) break;
				}
            try
            {
               nAccNum = (int)(rand.nextFloat() * rand.nextFloat() * (m_nMaxAccount - 1)) + 1;
               nBranchNum = (int)(rand.nextFloat() * (m_nMaxBranch - 1)) + 1;
               nTellerNum = (int)(rand.nextFloat() * (m_nMaxTeller - 1)) + 1;
               dDelta = ((double)((long)((((int)(rand.nextFloat() * (m_nMaxTeller - 1)) + 1) * (rand.nextFloat() > 0.5 ? -1 : 1)) * 100))) / 100;
               java.util.Date startTime = new java.util.Date();
               log("UPDATE " + m_Driver.getAccountName() + " SET balance = balance + " + dDelta + " WHERE account = " + nAccNum + "\n",2);
               stmt.executeUpdate("UPDATE " + m_Driver.getAccountName() + " SET balance = balance + " + dDelta + " WHERE account = " + nAccNum);
               log("SELECT balance FROM " + m_Driver.getAccountName() + " WHERE account = " + nAccNum + "\n",2);
               ResultSet balanceSet = stmt.executeQuery("SELECT balance FROM " + m_Driver.getAccountName() + " WHERE account = " + nAccNum);
               balanceSet.next();
               dBalance = balanceSet.getDouble(1);
               balanceSet.close();
               log("UPDATE " + m_Driver.getTellerName() + " SET balance = balance + " + dDelta + " WHERE teller = " + nTellerNum + "\n",2);
               stmt.executeUpdate("UPDATE " + m_Driver.getTellerName() + " SET balance = balance + " + dDelta + " WHERE teller = " + nTellerNum);
               log("UPDATE " + m_Driver.getBranchName() + " SET balance = balance + " + dDelta + " WHERE branch = " + nBranchNum + "\n",2);
               stmt.executeUpdate("UPDATE " + m_Driver.getBranchName() + " SET balance = balance + " + dDelta + " WHERE branch = " + nBranchNum);
               log("INSERT INTO " + m_Driver.getHistoryName() + " (histid, account, teller, branch, amount, timeoftxn, filler) VALUES (" + nRun + " , " + nAccNum + " , " + nTellerNum + " , " + nBranchNum + " , " + dDelta + " , " + m_nowFunction + " , \'" + BenchPanel.strFiller.substring(0,21) + "\')\n",2);
               stmt.executeUpdate("INSERT INTO " + m_Driver.getHistoryName() + " (histid, account, teller, branch, amount, timeoftxn, filler) VALUES (" + nRun + " , " + nAccNum + " , " + nTellerNum + " , " + nBranchNum + " , " + dDelta + " , " + m_nowFunction + " , \'" + BenchPanel.strFiller.substring(0,21) + "\')");
               if(m_bQuery)
                  executeQuery();
               //					System.out.println("Done query");
               if(m_bTransactionsUsed)
                  m_conn.commit();
               java.util.Date endTime = new java.util.Date();
               //					System.out.println("Done");
               m_nTrans += 1;
               long diff = endTime.getTime() - startTime.getTime();
               if(diff < 1000)
                  m_nTrans1Sec += 1;
               else
                  if(diff < 2000)
                     m_nTrans2Sec += 1;
               m_nTimeSum += ((double)diff) / 1000;
            }
            catch(SQLException e1)
            {
               //System.err.println(e1.getMessage());
               //e1.printStackTrace();
               break;
            }
            if(nRun % nProgressStep == 0)
               setProgressValue(nRun);
         //yield();
         }
         setProgressValue(m_nNumRuns - 1);
         m_conn.setAutoCommit(bCurrentAutoCommit);
      }
      catch(SQLException e)
      {
         //e.printStackTrace();
         //JOptionPane.showMessageDialog(null, e.getMessage(), "SQL Error in Text test", JOptionPane.ERROR_MESSAGE);
         log("SQLError in text test : " + e.getMessage(),0);
      }
      finally
      {
         if(stmt != null)
            try
            {
               stmt.close();
            }
            catch(SQLException e)
            {
            }
      }
   }


}

