/*
 *  $Id$
 *
 *  jdbc-bench - a TPC-A and TPC-C like benchmark program for JDBC drivers
 *
 *  Copyright (C) 2000-2008 OpenLink Software <jdbc-bench@openlinksw.com>
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

import javax.swing.*;


public class TPCCBench extends Thread {

   class TestTimer {
      public java.util.Date m_startTime = null,m_endTime = null,m_initTime;
      public long m_nTotalTime = 0,m_nMinTime = Long.MAX_VALUE,m_nMaxTime = 0;
      public long m_nSamples = 0;

      public void start() {
         m_endTime = null;
         m_startTime = new java.util.Date();
      }

      public long stop() {
         m_endTime = new java.util.Date();
         long nDifference = m_endTime.getTime() - m_startTime.getTime();
         m_nTotalTime += nDifference;
         if(m_nMinTime > nDifference) m_nMinTime = nDifference;
         if(m_nMaxTime < nDifference) m_nMaxTime = nDifference;
         m_nSamples += 1;
         return nDifference;
      }

      public void reset() {
         m_startTime = null; m_endTime = null; m_nTotalTime = 0;
         m_nMinTime = Long.MAX_VALUE; m_nMaxTime = 0; m_nSamples = 0;
         m_initTime = new java.util.Date();
      }

      public void print(Logger log, String strName) {
         java.util.Date endTime = new java.util.Date();
         if(log != null) {
            StringBuffer strOutput = new StringBuffer(""); String prov;
            strOutput.append(strName); strOutput.append("\t");
            strOutput.append(prov=""+m_nMinTime); strOutput.append("\t");
            strOutput.append(m_nTotalTime / m_nSamples); strOutput.append("\t");
            strOutput.append(m_nMaxTime); strOutput.append("\t");
            strOutput.append(m_nTotalTime); strOutput.append("\t\t");
            strOutput.append((m_nTotalTime * 100) / (endTime.getTime() - m_initTime.getTime()));
            strOutput.append("%\t"); strOutput.append(m_nSamples);
            strOutput.append(" times\n");
            log.log(strOutput.toString(),0);
         }
      }
   }

   int m_local_w_id = 1, m_n_ware = 1, m_n_rounds = 1,numOrders,numDistricts;
   Bench.Driver m_driver;
   Connection m_conn;
   Logger m_log;
   BenchPanel pane;
   String SQLError = null;
   JProgressBar bar = new JProgressBar();

   public JComponent getProgressBar() { return bar; }

   protected void setBarValue(int newValue) 
     { 
       bar.setValue(newValue);
     }

  public String getSQLError() { return SQLError; }

   // constants
   public static final int DIST_PER_WARE = 10;
   public static final int CUST_PER_DIST = 3000;
   public static final int ORD_PER_DIST = 300;
   public static final int MAXITEMS = 100000;
   static String m_lastNameArray[] = {"BAR", "OUGHT", "ABLE", "PRI", "PRES", "ESE", "ANTI", "CALLY", "ATION", "EING"};

   // results
   TestTimer m_newOrderTimer = new TestTimer();
   TestTimer m_paymentTimer = new TestTimer();
   TestTimer m_deliveryTimer = new TestTimer();
   TestTimer m_slevelTimer = new TestTimer();
   TestTimer m_ostatTimer = new TestTimer();
   boolean cancel = false, bCloseConnection = false;
   double m_nTPCCSum = 0,m_nTotalTime = 0;

   //statements
   Statement m_newOrderStmt = null;
   Statement m_paymentStmt = null;
   Statement m_deliveryStmt = null;
   Statement m_slevelStmt = null;
   Statement m_ostatStmt = null;

   public double getAverageTPC() {
      return m_nTPCCSum / m_n_rounds;
   }

   public double getTotalTime() {
      return m_nTotalTime;
   }

   public void cancel() {
      cancel=true;
   }
	
   public void reset() {
      cancel=false;
   }

   public TPCCBench(Connection conn, Bench.Driver driver, int n_rounds, int local_w_id, int n_ware, Logger logger, BenchPanel pane) {
      m_conn = conn; m_driver = driver; m_log = logger;
      this.pane = pane; m_local_w_id = local_w_id;
      m_n_ware = n_ware; m_n_rounds = n_rounds;
   }

   public TPCCBench(String strURL, String strUID, String strPWD, Bench.Driver driver, int n_rounds, int local_w_id, int n_ware, Logger logger, BenchPanel pane) throws SQLException
     {
       this(DriverManager.getConnection(strURL, strUID, strPWD), driver, n_rounds, local_w_id, n_ware, logger, pane);
       bCloseConnection = true;
   }

   synchronized void log(String strmessage, int nlevel) {
      if(m_log != null) m_log.log(strmessage,nlevel);
   }

   static int randomNumber(int x, int y)
   {
      int nReturnValue = (new Double((new java.util.Random().nextDouble() * (y - x)) + x)).intValue();
      //		System.out.println("x = " + x + " y = " + y + " Result = " + nReturnValue);
      return nReturnValue;
   }

   static int NURand(int a, int x, int y)
   {
      return ((((randomNumber(0,a) | randomNumber(x,y)) + 1234567) % (y - x + 1)) + x);
   }

   int random_c_id() {
      return randomNumber(1,3000);
   }

   int random_i_id() {
      return randomNumber(1,100000);
   }

   int make_supply_w_id() {
      if(m_n_ware > 1 && randomNumber(0,99) < 10) {
         int n, n_tries = 0;
         do {
            n = randomNumber(1,m_n_ware);
            n_tries++;
         } while(n == m_local_w_id && n_tries < 10);
         return m_local_w_id;
      }
      else return m_local_w_id;
   }

   static char chars[] = {'a', 'b', 'c', 'd', 'e', 'f', 'e', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z'};

   static String makeAlphaString(int sz1, int sz2) {
      StringBuffer strResult = new StringBuffer();
      int sz = randomNumber(sz1,sz2);
      for(int inx = 0;inx < sz;inx++) strResult.append(chars[inx % 24]);
      return strResult.toString();
   }

   static char digits[] = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'};

   static String makeNumberString(int sz, int sz2) {
      StringBuffer strResult = new StringBuffer();
      for(int inx = 0;inx < sz;inx++) strResult.append(digits[inx % 10]);
      return strResult.toString();
   }

   void new_order(String strNewOrderName,int nDist) throws SQLException {
      int d_id = m_local_w_id,w_id = randomNumber(1,nDist),c_id = random_c_id(),temp;
      int ol_cnt = 10,all_local = 1;
      int ol_i_id[] = {random_i_id(), random_i_id(), random_i_id(), random_i_id(), random_i_id(), random_i_id(), random_i_id(), random_i_id(), random_i_id(), random_i_id()};
      int ol_supply_w_id[] = {make_supply_w_id(), make_supply_w_id(), make_supply_w_id(), make_supply_w_id(), make_supply_w_id(), make_supply_w_id(), make_supply_w_id(), make_supply_w_id(), make_supply_w_id(), make_supply_w_id()};
      int ol_qty[] = {5, 5, 5, 5, 5, 5, 5, 5, 5, 5};
      if(m_newOrderStmt == null) m_newOrderStmt = m_conn.createStatement();
      StringBuffer strSQL = new StringBuffer("{call " + strNewOrderName + " (");
      strSQL.append(w_id); strSQL.append(", "); strSQL.append(d_id); strSQL.append(", ");
      strSQL.append(c_id); strSQL.append(", "); strSQL.append(ol_cnt); strSQL.append(", ");
      strSQL.append(all_local); strSQL.append(", ");
      for(int n = 0;n < 10;n++) {
         strSQL.append(ol_i_id[n]); strSQL.append(", "); strSQL.append(ol_supply_w_id[n]);
         strSQL.append(", "); strSQL.append(ol_qty[n]);
         if(n < 9) strSQL.append(", ");
      }
      strSQL.append(")}"); m_newOrderTimer.start();
      try {
         log("Executing new_order\n",2);
         if(m_newOrderStmt.execute(strSQL.toString())) get_stmt_result_sets(m_newOrderStmt,strNewOrderName);
         else log("no result sets\n",2);
         log(strNewOrderName + " done\n",2);
      } catch(SQLException e) { log("SQL Error executing " + strNewOrderName + " : " + e.getMessage() + "\n",1); }
      m_newOrderTimer.stop();
   }

   void get_stmt_result_sets(Statement stmt, String strCallName) throws SQLException {
      ResultSet set;
      int nUpdateCount = -1;
      set = stmt.getResultSet();
      if(set != null) {
         log("Processing result set for " + strCallName + "\n",2);
         while(set.next());
      }
      else {
         nUpdateCount = stmt.getUpdateCount();
         if(nUpdateCount == -1) log("No results for " + strCallName + "\n",2);
         else log("Update count " + nUpdateCount + " for " + strCallName + "\n",2);
      }
      stmt.getMoreResults();
   }

   String lastName(int num) {
      StringBuffer str = new StringBuffer(m_lastNameArray[num / 100]);
      str.append(m_lastNameArray[(num / 10) % 10]); str.append(m_lastNameArray[num % 10]);
      return str.toString();
   }

   void payment(String strPaymentName,int nDist) throws SQLException {
      int w_id = m_local_w_id,d_id = randomNumber(1,nDist),c_id = random_c_id();
      String c_last = "POITIER";
      double amount = 100.00;
      if(m_paymentStmt == null) m_paymentStmt = m_conn.createStatement();
      if(randomNumber(0,100) < 60) {
         c_id = 0;
         c_last = lastName(randomNumber(0,999));
      }
      StringBuffer strSQL = new StringBuffer("{call " + strPaymentName + " (");
      strSQL.append(w_id); strSQL.append(", "); strSQL.append(w_id); strSQL.append(", "); strSQL.append(amount);
      strSQL.append(", "); strSQL.append(d_id); strSQL.append(", "); strSQL.append(d_id); strSQL.append(", ");
      strSQL.append(c_id); strSQL.append(", \'"); strSQL.append(c_last); strSQL.append("\' )}");
      log(strSQL.toString() + "\n",2);
      m_paymentTimer.start();
      try {
         if(m_paymentStmt.execute(strSQL.toString())) get_stmt_result_sets(m_paymentStmt,strPaymentName);
         else log("no result sets\n",2);
         log(strPaymentName + " done\n",2);
      } catch(SQLException e) { log("SQL Error executing " + strPaymentName + " : " + e.getMessage() + "\n",1); }
      m_paymentTimer.stop();
   }

   void delivery_1(String strDeliveryName, int d_id) throws SQLException {
      int carrier_id = 13,w_id = m_local_w_id;
      if(m_deliveryStmt == null) m_deliveryStmt = m_conn.createStatement();
      StringBuffer strSQL;
      strSQL = new StringBuffer("{call " + strDeliveryName + " (");
      strSQL.append(w_id); strSQL.append(", "); strSQL.append(carrier_id);
      if(d_id > 0) {
         strSQL.append(", "); strSQL.append(d_id);
      }
      strSQL.append(")}");
      log(strSQL.toString() + "\n",2);
      try {
         if(m_deliveryStmt.execute(strSQL.toString())) get_stmt_result_sets(m_deliveryStmt,strDeliveryName);
         else log("no result sets\n",2);
      } catch(SQLException e) { log("SQL Error executing " + strDeliveryName + " : " + e.getMessage() + "\n",1); }
   }

   void slevel(String strSLevelName,int nDist) throws SQLException {
      int w_id = m_local_w_id,d_id = randomNumber(1,nDist),threshold = 20;
      if(m_slevelStmt == null) m_slevelStmt = m_conn.createStatement();
      StringBuffer strSQL = new StringBuffer("{call " + strSLevelName + " (");
      strSQL.append(w_id); strSQL.append(", "); strSQL.append(d_id); strSQL.append(", ");
      strSQL.append(threshold); strSQL.append(")}");
      log(strSQL.toString() + "\n",2);
      m_slevelTimer.start();
      try {
         if(m_slevelStmt.execute(strSQL.toString())) get_stmt_result_sets(m_slevelStmt,strSLevelName);
         else log("no result sets\n",2);
      } catch(SQLException e) { log("SQL Error executing " + strSLevelName + " : " + e.getMessage() + "\n",1); }
      m_slevelTimer.stop();
   }

   void ostat(String strOStatName,int nDist) throws SQLException {
      int w_id = m_local_w_id,d_id = randomNumber(1,nDist),c_id = random_c_id();
      String c_last = "EPOITIER";
      if(m_ostatStmt == null) m_ostatStmt = m_conn.createStatement();
      if(randomNumber(0,100) < 60) {
         c_id = 0;
         c_last = lastName(randomNumber(0,999));
      }
      StringBuffer strSQL = new StringBuffer("{call " + strOStatName + " (");
      strSQL.append(w_id); strSQL.append(", "); strSQL.append(d_id); strSQL.append(", ");
      strSQL.append(c_id); strSQL.append(", \'"); strSQL.append(c_last); strSQL.append("\')}");
      log(strSQL.toString() + "\n",2);
      m_ostatTimer.start();
      try {
         if(m_ostatStmt.execute(strSQL.toString())) get_stmt_result_sets(m_ostatStmt,strOStatName);
         else log("no result sets\n",2);
      } catch(SQLException e) { log("SQL Error executing " + strOStatName + " : " + e.getMessage() + "\n",1); }
      m_ostatTimer.stop();
   }

   public void run() 
     {
       m_newOrderTimer.reset(); 
       m_paymentTimer.reset(); 
       m_deliveryTimer.reset();
       m_slevelTimer.reset(); 
       m_ostatTimer.reset();
       try 
	 {
	   log("\nStarting TPC-C benchmark for " + m_n_rounds + " runs\n",0);
	   m_nTPCCSum = 0; m_nTotalTime = 0;
	   for(int nRound = 0;nRound < m_n_rounds && !cancel;nRound++) 
	     {
	       java.util.Date start10Pack = new java.util.Date();
	       
	       for(int n = 0;n < 10;n++) 
		 {
		   new_order("new_order",numOrders);
		   payment("payment",numDistricts);
		 }
	       
	       m_deliveryTimer.start();
	       if(m_driver.separateDeliveryTransactions())
		 for(int n = 1;n <= 10 && !cancel;n++) 
		   delivery_1("delivery_1",n);
	       else 
		 delivery_1("delivery",0);
	       m_deliveryTimer.stop();
	       log("delivery done\n",2);

	       slevel("slevel",numDistricts);
	       ostat("ostat",numDistricts);

	       java.util.Date end10Pack = new java.util.Date();

	       long nDifference = end10Pack.getTime() - start10Pack.getTime();
	       long nTPCC = 600000 / nDifference;
	       m_nTPCCSum += nTPCC;
	       m_nTotalTime += ((double)nDifference) / 1000;
	       
	       log("\nStatistics round num. "+(nRound+1)+"/"+m_n_rounds+" : "+nTPCC+" tpmC\n",0);
	       log("\t\tMin time\tAvg time\tMax time\tTotal time\t%\tSamples\n",0);
	       
	       m_newOrderTimer.print(m_log,"New order\t");
	       m_paymentTimer.print(m_log,"Payment\t");
	       m_deliveryTimer.print(m_log,"Delivery\t");
	       m_slevelTimer.print(m_log,"Stock level");
	       m_ostatTimer.print(m_log,"Order status");

	       m_newOrderTimer.reset(); 
	       m_paymentTimer.reset(); 
	       m_deliveryTimer.reset();
	       m_slevelTimer.reset(); 
	       m_ostatTimer.reset();
	       pane.oneTransMore();
	     }
	   if(m_newOrderStmt != null) 
	     {
	       m_newOrderStmt.close();
	       m_newOrderStmt = null;
	     }
	   if(m_paymentStmt != null) 
	     {
	       m_paymentStmt.close();
	       m_paymentStmt = null;
	     }
	   if(m_deliveryStmt != null) 
	     {
	       m_deliveryStmt.close();
	       m_deliveryStmt = null;
	     }
	   if(m_slevelStmt != null) 
	     {
	       m_slevelStmt.close();
	       m_slevelStmt = null;
	     }
	   if(m_ostatStmt != null) 
	     {
	       m_ostatStmt.close();
	       m_ostatStmt = null;
	     }
      }
      catch(SQLException e) {
         log("SQL Error : " + e.getMessage() + "\n",0);
      }

      if(bCloseConnection) 
	{
	  try 
	    {
	      m_conn.close();
	      log("Thread connection closed",2);
	    } catch(SQLException ee) { }
	}

      pane.oneThreadLess();

      if (m_log != null)
	m_log.taskDone();
   }

   void loadWarehouse(String strWarehouseName, String strDistrictName, 
       String strStockName, int count_ware, int nDist,int mItems) throws SQLException 
     {
       long w_id; String w_name,w_street_1,w_street_2,w_city,w_state,w_zip; float w_tax,w_ytd;

       Statement stmt = m_conn.createStatement(); //log("Loading WAREHOUSE\n",0);
       pane.resetWarehouse();
       try 
	 {
	   for(w_id = 1;w_id <= count_ware && !cancel;w_id++) 
	     {
	       w_name = makeAlphaString(6,10); 
	       w_street_1 = makeAlphaString(10,18); 
	       w_street_2 = makeAlphaString(10,18);
	       w_city = makeAlphaString(10,18); 
	       w_state = makeAlphaString(2,2); 
	       w_zip = makeAlphaString(9,9);
	       w_tax = ((float)randomNumber(10,20)) / 100; 
	       w_ytd = 3000000;

	       StringBuffer strSQL = 
		   new StringBuffer("insert into " + strWarehouseName + 
		       " (w_id, w_name, w_street_1, w_street_2, w_city, w_state, w_zip, w_tax, w_ytd) values (");

	       strSQL.append(w_id); strSQL.append(", \'"); 
	       strSQL.append(w_name); strSQL.append("\', \'");
	       strSQL.append(w_street_1); strSQL.append("\', \'"); 
	       strSQL.append(w_street_2); strSQL.append("\', \'"); 
	       strSQL.append(w_city); strSQL.append("\', \'");
	       strSQL.append(w_state); strSQL.append("\', \'"); 
	       strSQL.append(w_zip); strSQL.append("\', "); 
	       strSQL.append(w_tax); strSQL.append(", ");
	       strSQL.append(w_ytd); strSQL.append(")");

	       log(strSQL.toString() + "\n",2);
	       stmt.execute(strSQL.toString());
	       loadDistrict(strDistrictName,w_id,nDist);
	       pane.oneMoreWarehouse();
	     }
	   if(!cancel) 
	     loadStock(strStockName,1,count_ware,mItems);
	 }
       finally 
	 { 
	   stmt.close(); 
	   stmt = null;
	 }
     }

   void loadDistrict(String strDistrictName, long w_id, int nDist) throws SQLException 
     {
       long d_id,d_w_id = w_id,d_next_o_id = 3001L;
       String d_name,d_street_1,d_street_2,d_city,d_state,d_zip;
       float d_tax,d_ytd = 30000;
       Statement stmt = m_conn.createStatement(); //log("\nLoading DISTRICT\n",0);
       pane.resetDistrict();
       try 
	 {
	   for(d_id = 1;d_id <= nDist && !cancel;d_id++) 
	     {
	       d_name = makeAlphaString(6,10); 
	       d_street_1 = makeAlphaString(10,18);
	       d_street_2 = makeAlphaString(10,18); 
	       d_city = makeAlphaString(10,18);
	       d_state = makeAlphaString(2,2); 
	       d_zip = makeAlphaString(9,9);
	       d_tax = ((float)randomNumber(10,20)) / 100;

	       StringBuffer strSQL = 
		   new StringBuffer("insert into " + strDistrictName + 
		       " (d_id, d_w_id, d_name, d_street_1, d_street_2, d_city, d_state, d_zip, d_tax, d_ytd, d_next_o_id) values (");
	       strSQL.append(d_id); strSQL.append(", "); 
	       strSQL.append(d_w_id); strSQL.append(", \'"); 
	       strSQL.append(d_name); strSQL.append("\' , \'");
	       strSQL.append(d_street_1); strSQL.append("\' , \'"); 
	       strSQL.append(d_street_2); strSQL.append("\' , \'"); 
	       strSQL.append(d_city); strSQL.append("\' , \'");
	       strSQL.append(d_state); strSQL.append("\' , \'"); 
	       strSQL.append(d_zip); strSQL.append("\' , "); 
	       strSQL.append(d_tax); strSQL.append(", ");
	       strSQL.append(d_ytd); strSQL.append(", "); 
	       strSQL.append(d_next_o_id);
	       strSQL.append(")");

	       log(strSQL.toString() + "\n",2);
	       stmt.execute(strSQL.toString());
	       pane.oneMoreDistrict();
	     }
	 } 
       finally 
	 { 
	   stmt.close();
	   stmt = null; 
	 }
     }

   void loadStock(String strStockName, int w_id_from, int w_id_to, int mItems) throws SQLException 
     {
       int w_id,s_i_id,s_w_id,s_quantity,pos;
       String s_dist_01,s_dist_02,s_dist_03,s_dist_04,s_dist_05,
         s_dist_06,s_dist_07,s_dist_08,s_dist_09,s_dist_10;
       String s_data; 
       boolean orig[] = new boolean[mItems];

      //log("\nLoading STOCK for Wid=" + w_id_from + "-" + w_id_to + "\n",0);
       for(int i = 0;i < mItems / 10;i++) 
	 orig[i] = false;
      for(int i = 0;i < mItems / 10;i++) 
	{
	  do 
	    { 
	      pos = randomNumber(0,mItems); 
	    }
	  while(orig[pos]); 
	  orig[pos] = true;
	}
      pane.resetItem();
      Statement stmt = m_conn.createStatement();
      try 
	{
	  for(s_i_id = 1;s_i_id <= mItems && !cancel;s_i_id++) 
	    {
	      for(w_id = w_id_from;w_id <= w_id_to && !cancel;w_id++) 
		{
		  /* generate stock data */
		  s_w_id = w_id;
		  s_quantity = randomNumber(10,100); 
		  s_dist_01 = makeAlphaString(24,24);
		  s_dist_02 = makeAlphaString(24,24); 
		  s_dist_03 = makeAlphaString(24,24);
		  s_dist_04 = makeAlphaString(24,24); 
		  s_dist_05 = makeAlphaString(24,24);
		  s_dist_06 = makeAlphaString(24,24); 
		  s_dist_07 = makeAlphaString(24,24);
		  s_dist_08 = makeAlphaString(24,24); 
		  s_dist_09 = makeAlphaString(24,24);
		  s_dist_10 = makeAlphaString(24,24); 
		  s_data = makeAlphaString(26,50);

		  if(orig[s_i_id - 1]) 
		    {
		      StringBuffer strPlace = new StringBuffer(s_data);
		      pos = randomNumber(0,s_data.length() - 8);
		      strPlace.setCharAt(pos,'o'); 
		      strPlace.setCharAt(pos + 1,'r');
		      strPlace.setCharAt(pos + 2,'i'); 
		      strPlace.setCharAt(pos + 3,'g');
		      strPlace.setCharAt(pos + 4,'i'); 
		      strPlace.setCharAt(pos + 5,'n');
		      strPlace.setCharAt(pos + 6,'a'); 
		      strPlace.setCharAt(pos + 7,'l');
		      s_data = strPlace.toString();
		    }
		  StringBuffer strSQL = 
		      new StringBuffer("insert into " + strStockName + 
			  " (s_i_id, s_w_id, s_quantity, s_dist_01, s_dist_02, s_dist_03, s_dist_04, s_dist_05, s_dist_06, s_dist_07, s_dist_08, s_dist_09, s_dist_10, s_data, s_ytd, s_cnt_order, s_cnt_remote) VALUES (");
		  
		  strSQL.append(s_i_id); strSQL.append(", "); 
		  strSQL.append(s_w_id); strSQL.append(", ");
		  strSQL.append(s_quantity); strSQL.append(", \'"); 
		  strSQL.append(s_dist_01); strSQL.append("\' , \'");
		  strSQL.append(s_dist_02); strSQL.append("\' , \'"); 
		  strSQL.append(s_dist_03); strSQL.append("\' , \'");
		  strSQL.append(s_dist_04); strSQL.append("\' , \'"); 
		  strSQL.append(s_dist_05); strSQL.append("\' , \'");
		  strSQL.append(s_dist_06); strSQL.append("\' , \'"); 
		  strSQL.append(s_dist_07); strSQL.append("\' , \'");
		  strSQL.append(s_dist_08); strSQL.append("\' , \'"); 
		  strSQL.append(s_dist_09); strSQL.append("\' , \'");
		  strSQL.append(s_dist_10); strSQL.append("\' , \'"); 
		  strSQL.append(s_data); strSQL.append("\', ");
		  strSQL.append("0, 0, 0)"); 

		  log(strSQL.toString() + "\n",2);
		  stmt.execute(strSQL.toString());
		}
	      pane.oneMoreItem();
	    }
	} 
      finally { 
	stmt.close(); 
      }
   }

   void scrap_log() {
   }

   void loadItems(String strItemName,int mItems) throws SQLException 
     {
       int i_id,idatasiz,pos;
       String i_name,i_data;
       double i_price;
       boolean orig[] = new boolean[mItems];
       //log("\nLoading ITEM ...\n",2);
       for(int i = 0;i < mItems;i++) 
	 orig[i] = false;
       for(int i = 0;i < mItems;i++) 
	 {
	   do 
	     { 
	       pos = randomNumber(0,mItems); 
	     }
	   while(orig[pos]); 
	   orig[pos] = true;
	 }
       Statement stmt = m_conn.createStatement();
       try 
	 {
	   for(i_id = 1;i_id <= mItems && !cancel;i_id++) 
	     {
	       i_name = makeAlphaString(14,24); 
	       i_price = ((double)randomNumber(100,10000)) / 100.0;
	       i_data = makeAlphaString(26,50);
	       if(orig[i_id - 1]) 
		 {
		   pos = randomNumber(0,i_data.length() - 8);
		   StringBuffer strPlace = new StringBuffer(i_data);
		   strPlace.setCharAt(pos,'o'); 
		   strPlace.setCharAt(pos + 1,'r');
		   strPlace.setCharAt(pos + 2,'i'); 
		   strPlace.setCharAt(pos + 3,'g');
		   strPlace.setCharAt(pos + 4,'i'); 
		   strPlace.setCharAt(pos + 5,'n');
		   strPlace.setCharAt(pos + 6,'a'); 
		   strPlace.setCharAt(pos + 7,'l');
		   i_data = strPlace.toString();
		 }
	       StringBuffer strSQL = 
		   new StringBuffer("insert into " + strItemName + 
		       " (i_id, i_name, i_price, i_data) values (");
	       strSQL.append(i_id); strSQL.append(", \'"); 
	       strSQL.append(i_name); strSQL.append("\' , ");
	       strSQL.append(i_price); strSQL.append(" , \'"); 
	       strSQL.append(i_data); strSQL.append("\'");
	       strSQL.append(")");
	       
	       log(strSQL.toString() + "\n",2);

	       stmt.execute(strSQL.toString());
	       pane.oneMoreItem();
	     }
	 } 
       finally 
	 { 
	   stmt.close(); 
	   stmt = null;
	 }
     }

   void loadCustomer(String strCustomerName, String strHistoryName, int nCountWare, 
       int nDist, int nCust,String strNowFunction, BenchPanel pane) throws SQLException 
     {
       int w_id, d_id,c_id,c_d_id,c_w_id,fill = 0, h_fill = 0;
       String c_first,c_middle,c_last,c_street_1,c_street_2,c_city,
           c_state,c_zip,c_phone,c_credit,c_data_1,c_data_2,h_data;
       double c_credit_lim,c_discount,c_balance,h_amount;
       Statement histmt = m_conn.createStatement(), csstmt = m_conn.createStatement();
       try 
	 {
	   for(w_id = 1;w_id <= nCountWare && !cancel;w_id++) 
	     {
	       if(pane!=null) 
		 pane.resetDistrict();
	       for(d_id = 1;d_id <= nDist && !cancel;d_id++) 
		 {
		   //log("\nLoading CUSTOMER for DID=" + d_id + ", WID=" + w_id + "\n",0);
		   if(pane!=null) 
		     pane.resetCustomer();
		   for(c_id = 1;c_id <= nCust && !cancel;c_id++) 
		     {
		       c_d_id = d_id; 
		       c_w_id = w_id;
		       c_first = makeAlphaString(8,15); 
		       c_data_1 = makeAlphaString(240,240);
		       c_data_2 = makeAlphaString(240,240); 
		       c_middle = "J";
		       if(c_id <= 1000) 
			 c_last = lastName(c_id - 1);
		       else 
			 c_last = lastName(NURand(255,0,999));
		       c_street_1 = makeAlphaString(10,18); 
		       c_street_2 = makeAlphaString(10,18);
		       c_city = makeAlphaString(10,18); 
		       c_state = makeAlphaString(2,2);
		       c_zip = makeAlphaString(9,9); 
		       c_phone = makeNumberString(16,16);
		       if(randomNumber(0,1) > 0) 
			 c_credit = "GC";
		       else 
			 c_credit = "BC";
		       c_credit_lim = 500; 
		       c_discount = ((double)randomNumber(0,50)) / 100.00;
		       c_balance = 10.0;
		       StringBuffer strSQL = 
			   new StringBuffer("insert into " + strCustomerName + 
			       " (c_id, c_d_id, c_w_id, c_first, c_middle, c_last, c_street_1, c_street_2, c_city, c_state, c_zip, c_phone, c_since, c_credit, c_credit_lim, c_discount, c_balance, c_data_1, c_data_2, c_ytd_payment, c_cnt_payment, c_cnt_delivery) values (");

		       strSQL.append(c_id); strSQL.append(", "); 
		       strSQL.append(c_d_id); strSQL.append(", ");
		       strSQL.append(c_w_id); strSQL.append(", \'"); 
		       strSQL.append(c_first); strSQL.append("\' , \'");
		       strSQL.append(c_middle); strSQL.append("\' , \'"); 
		       strSQL.append(c_last); strSQL.append("\' , \'");
		       strSQL.append(c_street_1); strSQL.append("\' , \'"); 
		       strSQL.append(c_street_2); strSQL.append("\' , \'");
		       strSQL.append(c_city); strSQL.append("\' , \'"); 
		       strSQL.append(c_state); strSQL.append("\' , \'");
		       strSQL.append(c_zip); strSQL.append("\' , \'"); 
		       strSQL.append(c_phone); strSQL.append("\' , ");
		       strSQL.append(strNowFunction); strSQL.append(" , \'"); 
		       strSQL.append(c_credit); strSQL.append("\' , ");
		       strSQL.append(c_credit_lim); strSQL.append(", "); 
		       strSQL.append(c_discount); strSQL.append(", ");
		       strSQL.append(c_balance); strSQL.append(", \'"); 
		       strSQL.append(c_data_1); strSQL.append("\' , \'");
		       strSQL.append(c_data_2); strSQL.append("\'"); 
		       strSQL.append(", 10.0, 1, 0)");

		       log(strSQL.toString() + "\n",2);

		       csstmt.execute(strSQL.toString());

		       h_amount = 10; 
		       h_data = makeAlphaString(12,24);
		       strSQL = new StringBuffer("insert into " + strHistoryName + 
			   " ( h_c_id, h_c_d_id, h_c_w_id, h_w_id, h_d_id, h_date, h_amount, h_data) values (");
		       strSQL.append(c_id); strSQL.append(", "); 
		       strSQL.append(c_d_id); strSQL.append(", "); 
		       strSQL.append(c_w_id); strSQL.append(", "); 
		       strSQL.append(c_w_id); strSQL.append(", "); 
		       strSQL.append(c_d_id); strSQL.append(", ");
		       strSQL.append(strNowFunction); strSQL.append(", "); 
		       strSQL.append(h_amount); strSQL.append(", \'");
		       strSQL.append(h_data); strSQL.append("\'"); 
		       strSQL.append(")"); 

		       log(strSQL.toString() + "\n",2);
		       histmt.execute(strSQL.toString());
		       if(pane!=null) 
			 pane.oneMoreCustomer();
		     }
		   if(pane!=null) 
		     pane.oneMoreDistrict();
		 }
	       if(pane!=null) 
		 pane.oneMoreWarehouse();
	     }
	 } 
       finally 
	 { 
	   csstmt.close();
	   csstmt = null; 
	   histmt.close();
	   histmt = null; 
	 }
     }

   void loadOrder(String strOrdersName, String strNewOrderName, 
       String strOrderLineName, String strHistoryName, int nCountWare,int nDist,
       int nCust,int mItems,int nOrd, String strNowFunction,BenchPanel pane) throws SQLException 
     {
       int o_id,o_c_id,o_d_id,o_w_id,o_carrier_id,o_ol_cnt,ol,ol_i_id,ol_supply_w_id,ol_quantity,ol_amount;
       String ol_dist_info; StringBuffer strSQL;
       int ol_o_id,ol_o_d_id,ol_o_w_id;
       Statement ostmt = m_conn.createStatement(), 
           nostmt = m_conn.createStatement(), 
	   olstmt = m_conn.createStatement();
       try 
	 {
	   for(int w_id = 1;w_id <= nCountWare && !cancel;w_id++) 
	     {
	       if(pane!=null) 
		 pane.resetDistrict();
	       for(int d_id = 1;d_id <= nDist && !cancel;d_id++) 
		 {
		   //log("\nLoading ORDERS for D=" + d_id + ", W=" + w_id + "\n",0);
		   if(pane!=null) 
		     pane.resetOrder();
		   for(o_id = 1;o_id <= nOrd && !cancel;o_id++) 
		     {
		       o_d_id = d_id; 
		       o_w_id = w_id; 
		       o_c_id = randomNumber(1,nCust);
		       o_carrier_id = randomNumber(1,10); 
		       o_ol_cnt = randomNumber(5,15);
		       if(o_id > nOrd - 900) 
			 {
			   strSQL = new StringBuffer("insert into " + strNewOrderName + 
			       " (no_o_id, no_d_id, no_w_id) values (");

			   strSQL.append(o_id); strSQL.append(", "); 
			   strSQL.append(o_d_id); strSQL.append(", ");
			   strSQL.append(o_w_id); strSQL.append(")");

			   log(strSQL.toString() + "\n",2);
			   nostmt.execute(strSQL.toString());
			 }
		       for(ol = 1;ol <= o_ol_cnt && !cancel;ol++) 
			 {
			   ol_o_id = o_id; 
			   ol_o_d_id = o_d_id; 
			   ol_o_w_id = o_w_id; 
			   ol_i_id = randomNumber(1,mItems);
			   ol_supply_w_id = o_w_id; 
			   ol_quantity = 5; 
			   ol_amount = 0;
			   ol_dist_info = makeAlphaString(24,24);

			   strSQL = new StringBuffer("insert into " + strOrderLineName + 
			       " (ol_o_id, ol_d_id, ol_w_id, ol_number, ol_i_id, ol_supply_w_id, ol_quantity, ol_amount, ol_dist_info, ol_delivery_d) values (");

			   strSQL.append(ol_o_id); strSQL.append(" , "); 
			   strSQL.append(ol_o_d_id); strSQL.append(" , "); 
			   strSQL.append(ol_o_w_id); strSQL.append(" , ");
			   strSQL.append(ol); strSQL.append(" , "); 
			   strSQL.append(ol_i_id); strSQL.append(" , "); 
			   strSQL.append(ol_supply_w_id); strSQL.append(" , ");
			   strSQL.append(ol_quantity); strSQL.append(" , "); 
			   strSQL.append(ol_amount); strSQL.append(" , \'"); 
			   strSQL.append(ol_dist_info); strSQL.append("\'");
			   strSQL.append(",NULL)");

			   log(strSQL.toString() + "\n",2);
			   olstmt.execute(strSQL.toString());
			 }
		       strSQL = new StringBuffer("insert into " + strOrdersName + 
			   " (o_id, o_c_id, o_d_id, o_w_id, o_entry_d, o_carrier_id, o_ol_cnt, o_all_local) values (");
		       strSQL.append(o_id); strSQL.append(", "); 
		       strSQL.append(o_c_id); strSQL.append(", ");
		       strSQL.append(o_d_id); strSQL.append(", "); 
		       strSQL.append(o_w_id); strSQL.append(", ");
		       strSQL.append(strNowFunction); strSQL.append(", "); 
		       strSQL.append(o_carrier_id); strSQL.append(", "); 
		       strSQL.append(o_ol_cnt); strSQL.append(", "); 
		       strSQL.append("1)");
		       
		       log(strSQL.toString() + "\n",2);
		       ostmt.execute(strSQL.toString());

		       if(pane!=null) 
			 pane.oneMoreOrder();
		     }
		   if(pane!=null) 
		     pane.oneMoreDistrict();
		 }
	       if(pane!=null) 
		 pane.oneMoreWarehouse();
	     }
	 } 
       finally 
	 { 
	   ostmt.close(); 
	   nostmt.close(); 
	   olstmt.close(); 
	   ostmt = null;
	   nostmt = null;
	   olstmt = null;
	 }
     }

   public void loadData(String strNowFunction, 
       int nWarehouses,int nDistricts, int nCustomers,
       int maxItem,int nOrders) 
     {
       try 
	 {
	   log("\nTPCC Data Load Started ... ",0);
	   numOrders=nOrders; 
	   numDistricts=nDistricts;
	   loadWarehouse(m_driver.getTPCCWarehouseName(),
	       m_driver.getTPCCDistrictName(),
	       m_driver.getTPCCStockName(),
	       nWarehouses,
	       nDistricts,
	       maxItem);
	   loadItems(m_driver.getTPCCItemName(), maxItem);
	   loadCustomer(m_driver.getTPCCCustomerName(),
	       m_driver.getTPCCHistoryName(),
	       nWarehouses,
	       nDistricts,
	       nCustomers,
	       strNowFunction,
	       null);

	   loadOrder(m_driver.getTPCCOrdersName(),
	       m_driver.getTPCCNewOrderName(),
	       m_driver.getTPCCOrderLineName(),
	       m_driver.getTPCCHistoryName(),
	       nWarehouses,
	       nDistricts,
	       nCustomers,
	       nOrders,
	       maxItem,
	       strNowFunction,
	       null);
	   
	   log("Done.\n",0);

	 } 
       catch(Exception e) 
	 { 
	   log("\nSQL Error loading the data : " + e.getMessage() + "\n",0); 
	 }
     }
}
