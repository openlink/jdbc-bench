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
import java.io.*;
import java.util.*;


import javax.swing.*;



class Driver {
   String m_strDriverName;
   String m_strITM;
   String m_strProcedure;
   String m_strDropProcedure;
   String m_strNewOrderProcName;
   String m_strPaymentProcName;
   String m_strDeliveryProcName;
   String m_strSLevelProcName;
   String m_strOStatProcName;
   String m_strShortName;
   DataType m_DTM;

   boolean m_bfIndex;
   boolean m_bPrimaryKey;
   boolean m_bSeparateDeliveryTransactions = true;
   boolean m_bUseCatalogs = false;

   public void setUseCatalogs(boolean bUseCatalogs) {
      m_bUseCatalogs = bUseCatalogs;
   }

   public boolean usesCatalogs() {
      return m_bUseCatalogs;
   }

   Driver(String strDriverName, String strShortName, int nDTMIndex, int nITMIndex,
     boolean bfIndex, boolean bPrimaryKeyDef, boolean bUseCatalogs)
   {
      m_strDriverName = strDriverName; m_strShortName = strShortName;
      m_DTM = DataType.getType(nDTMIndex);
      if(nITMIndex != -1)
       m_strITM = getIndexTypeMap(nITMIndex);
      m_bPrimaryKey = bPrimaryKeyDef; m_bUseCatalogs = bUseCatalogs;
   }

   Driver(String strDriverName, String strShortName, int nDTMIndex, int nITMIndex,
      boolean bfIndex, boolean bPrimaryKeyDef, boolean bUseCatalogs,
      String strProcName, String strDropProcedureText)
   {
      this (strDriverName,strShortName,nDTMIndex,nITMIndex,bfIndex,bPrimaryKeyDef,bUseCatalogs);
      /* TPC-A */
      m_strProcedure = getTextResource(this,"SQL/" + strProcName);
      m_strDropProcedure = strDropProcedureText;
   }

   Driver(String strDriverName, String strShortName, int nDTMIndex, int nITMIndex, boolean bfIndex, boolean bPrimaryKeyDef, boolean bUseCatalogs, String strProcName, String strDropProcedureText, boolean bSepDeliveryTransactions) {
      this (strDriverName,strShortName,nDTMIndex,nITMIndex,bfIndex,bPrimaryKeyDef,bUseCatalogs,strProcName,strDropProcedureText);
      // TPC-C resources
      m_bSeparateDeliveryTransactions = bSepDeliveryTransactions;
   }

   public static String getTextResource(Object scope, String name) {
      Class base = scope.getClass();
      InputStream is=base.getResourceAsStream(name);
      InputStreamReader rs; String s=new String();
      try {
          char text[]=new char[1024];
 	  rs=new InputStreamReader(is); int nch;
	  while((nch=rs.read(text,0,text.length))!=-1) s+=new String(text,0,nch);
      } catch(Exception e) {
    	    JOptionPane.showMessageDialog(null,"Cannot load text file : " + name,"VCon",JOptionPane.ERROR_MESSAGE);
        }
      return s;
   }

   public void setProcedure(Connection c) throws SQLException {
      if (m_strProcedure == null)
         throw new SQLException("This DBMS type doesn\'t support procedures");
      Statement stmt = null;
      try {
         stmt = c.createStatement();
         try {
            if(m_strDropProcedure != null) stmt.execute(m_strDropProcedure);
         } catch(SQLException e) { }
         stmt.execute(m_strProcedure);
      } finally {
    	    if(stmt != null) stmt.close();
    	    stmt = null;
        }
   }

   public void doExecute(Connection c, String strFileName, String strTestType) throws SQLException {
      String strFileContents = getTextResource(this,"SQL/" + strTestType + "/" + m_strDriverName + "/" + strFileName);
      Statement stmt = c.createStatement();
      try {
         stmt.execute(strFileContents);
      } catch(NoSuchElementException e) { }
        finally { stmt.close(); }
   }

   public void parseAndExecute(Connection c, String strListName, String strTestType) throws SQLException {
      String strFileContents = getTextResource(this,"SQL/" + strTestType + "/" + m_strDriverName + "/" + strListName);
      if(strListName == null || c == null)
         throw new SQLException("This DBMS type doesn't support " + strTestType);
      StringTokenizer statementList = new StringTokenizer(strFileContents,";");
      Statement stmt = c.createStatement();
      try {
         do {
            String strStatement = statementList.nextToken();
            try { stmt.execute(strStatement); }
            catch(SQLException e1) {
        	//					System.out.println(e1.getMessage());
            }
         } while(statementList.hasMoreTokens());
      } catch(NoSuchElementException e) { }
        finally { stmt.close(); }
   }

   public void createTest(Connection c, String strTestType, String strDir) throws SQLException {
      String strListName = getTextResource(this,"SQL/" + strTestType + "/" + m_strDriverName + "/" + strDir + "/list");
      if(strListName == null || c == null)
         throw new SQLException("This DBMS type doesn't support " + strTestType);
      StringTokenizer filesList = new StringTokenizer(strListName,"\n\r");
      Statement stmt = c.createStatement();
      try {
         do {
            String strFile = filesList.nextToken();
            if(strFile.charAt(0) == '!') parseAndExecute(c,strDir + "/" + strFile.substring(1),strTestType);
            else doExecute(c,strDir + "/" + strFile,strTestType);
         } while(filesList.hasMoreTokens());
      } catch(NoSuchElementException e) { }
        finally { stmt.close(); }
   }

   DataType getDataType() {
      return m_DTM;
   }

   String getIndexType() {
      return m_strITM;
   }

   public String toString() {
      return m_strDriverName;
   }

   public boolean mustCreateIndex() {
      return m_bfIndex;
   }

   public boolean supportsProcedures() {
      return (m_strProcedure != null);
   }

   // SQL constructs
   // TPC-A table names
   public String getBranchName() {
      return (m_bUseCatalogs ? "tpca..branch" : "branch");
   }

   public String getTellerName() {
      return (m_bUseCatalogs ? "tpca..teller" : "teller");
   }

   public String getAccountName() {
      return (m_bUseCatalogs ? "tpca..account" : "account");
   }

   public String getHistoryName() {
      return (m_bUseCatalogs ? "tpca..history" : "history");
   }

   // TPC-C table names
   public String getTPCCWarehouseName() {
      return (m_bUseCatalogs ? "tpcc..warehouse" : "warehouse");
   }

   public String getTPCCDistrictName() {
      return (m_bUseCatalogs ? "tpcc..district" : "district");
   }

   public String getTPCCStockName() {
      return (m_bUseCatalogs ? "tpcc..stock" : "stock");
   }

   public String getTPCCItemName() {
      return (m_bUseCatalogs ? "tpcc..item" : "item");
   }

   public String getTPCCCustomerName() {
      return (m_bUseCatalogs ? "tpcc..customer" : "customer");
   }

   public String getTPCCHistoryName() {
      return (m_bUseCatalogs ? "tpcc..history" : "history");
   }

   public String getTPCCOrdersName() {
      return (m_bUseCatalogs ? "tpcc..orders" : "orders");
   }

   public String getTPCCNewOrderName() {
      return (m_bUseCatalogs ? "tpcc..new_order" : "new_order");
   }

   public String getTPCCOrderLineName() {
      return (m_bUseCatalogs ? "tpcc..order_line" : "order_line");
   }

   public String makeCreateBranch(boolean bCreateIndexes) {
      StringBuffer strSQL = new StringBuffer("create table ");
      strSQL.append(getBranchName()); strSQL.append("(branch ");
      strSQL.append(getDataType().getIntType()); strSQL.append(", fillerint ");
      strSQL.append(getDataType().getIntType()); strSQL.append(", balance ");
      strSQL.append(getDataType().getFloatType()); strSQL.append(", filler ");
      strSQL.append(getDataType().getCharType()); strSQL.append("(84)");
      strSQL.append(")");
      return strSQL.toString();
   }

   public String makeCreateTeller(boolean bCreateIndexes) {
      StringBuffer strSQL = new StringBuffer("create table ");
      strSQL.append(getTellerName()); strSQL.append(" (teller ");
      strSQL.append(getDataType().getIntType()); strSQL.append(", branch ");
      strSQL.append(getDataType().getIntType()); strSQL.append(", balance ");
      strSQL.append(getDataType().getFloatType()); strSQL.append(", filler ");
      strSQL.append(getDataType().getCharType()); strSQL.append("(84)");
      strSQL.append(")");
      return strSQL.toString();
   }

   public String makeCreateAccount(boolean bCreateIndexes) {
      StringBuffer strSQL = new StringBuffer("create table ");
      strSQL.append(getAccountName()); strSQL.append(" (account ");
      strSQL.append(getDataType().getIntType()); strSQL.append(", branch ");
      strSQL.append(getDataType().getIntType()); strSQL.append(", balance ");
      strSQL.append(getDataType().getFloatType()); strSQL.append(", filler ");
      strSQL.append(getDataType().getCharType()); strSQL.append("(84)");
      strSQL.append(")");
      return strSQL.toString();
   }

   public String makeCreateHistory() {
      StringBuffer strSQL = new StringBuffer("create table ");
      strSQL.append(getHistoryName()); strSQL.append(" (histid ");
      strSQL.append(getDataType().getIntType()); strSQL.append(", account ");
      strSQL.append(getDataType().getIntType()); strSQL.append(", teller ");
      strSQL.append(getDataType().getIntType()); strSQL.append(", branch ");
      strSQL.append(getDataType().getIntType()); strSQL.append(", amount ");
      strSQL.append(getDataType().getFloatType()); strSQL.append(", timeoftxn ");
      strSQL.append(getDataType().getDateTimeType()); strSQL.append(", filler ");
      strSQL.append(getDataType().getCharType()); strSQL.append("(22)");
      strSQL.append(")");
      return strSQL.toString();
   }

   public String makeCreateResults() {
      StringBuffer strSQL = new StringBuffer("create table resultsj");
      strSQL.append(" (runt "); strSQL.append(getDataType().getDateTimeType());
      strSQL.append(", url "); strSQL.append(getDataType().getCharType()); strSQL.append("(254)");
      strSQL.append(", options ");  strSQL.append(getDataType().getCharType()); strSQL.append("(128)");
      strSQL.append(", tps "); strSQL.append(getDataType().getFloatType());
      strSQL.append(", tottime "); strSQL.append(getDataType().getFloatType());
      strSQL.append(", ntrans "); strSQL.append(getDataType().getIntType());
      strSQL.append(", sub1s "); strSQL.append(getDataType().getIntType());
      strSQL.append(", sub2s "); strSQL.append(getDataType().getIntType());
      strSQL.append(", trntime "); strSQL.append(getDataType().getFloatType());
      strSQL.append(", btype "); strSQL.append(getDataType().getCharType()); strSQL.append("(10)");
      strSQL.append(", drvrname "); strSQL.append(getDataType().getCharType()); strSQL.append("(128)");
      strSQL.append(", drvrver "); strSQL.append(getDataType().getCharType());  strSQL.append("(128)");
      strSQL.append(", state "); strSQL.append(getDataType().getCharType());  strSQL.append("(2)");
      strSQL.append(", message "); strSQL.append(getDataType().getCharType());  strSQL.append("(128)");
      strSQL.append(")");
      // memory max dirty disks
      return strSQL.toString();
   }

   boolean separateDeliveryTransactions() {
      return m_bSeparateDeliveryTransactions;
   }

   // static members
   public static final String IndexTypeMap[] = {//Index Type
       "unique", "unique clustered"};

   public static final Driver DriverMap[] = {
     	   //szDBMSName			ShortName	DTM Idx	ITM Idx	fIdx	PK 	Catalog
       //TPC-A proc def		Drop procedure stmt
       //TPC-C	Sep delivery trans
new Driver("Virtuoso",			"Virtuoso",	11,	0,	false,	true,	false,
    "tran.virt",		"drop procedure ODBC_BENCHMARK",
    true),
new Driver("Microsoft SQL Server",	"MS SQL Server",	5,	0,	false,	true,	false,
    "tran.sql",			"DROP PROCEDURE	 ODBC_BENCHMARK",
    false),
new Driver("ORACLE7",			"Oracle7",	3,	0,	false,	true,	false,
    "tran.ora",			"drop procedure ODBC_BENCHMARK"),
new Driver("ORACLE",			"Oracle",	3,	0,	false,	true,	false,
    "tran.ora",			"drop procedure ODBC_BENCHMARK"),
new Driver("OpenIngres",	"Ingres II",		10,	0,	false,	false,	false,
    "tran.ingres",              "drop procedure ODBC_BENCHMARK"),
new Driver("ACCESS",		"ACCESS",		4,	0,	false,	false,	false),
new Driver("DBASE",		"DBASE",		2,	0,	false,	false,	false),
new Driver("FOXPRO",		"FOXPRO",		2,	0,	false,	false,	false),
new Driver("EXCEL",		"EXCEL",		6,	-1,	false,	false,	false),
new Driver("PARADOX",		"PARADOX",		0,	0,	true,	false,	false),
new Driver("TEXT",		"TEXT",			1,	-1,	false,	false,	false),
new Driver("BTRIEVE",		"BTRIEVE",		7,	0,	false,	false,	false),
new Driver("ANSI",		"ANSI",			8,	0,	false,	false,	false),
new Driver("Sybase",		"Sybase",		5,	0,	false,	true,	false,
    "tran.sybase",			"DROP PROCEDURE	 ODBC_BENCHMARK",
    false),
new Driver("Intersolv dBASE",	"ISLDBASE",		9,	0,	false,	false,	false),
new Driver("Informix",		"Informix",		10,	0,	false,	false,	false),
new Driver("Ingres",		"Ingres",		10,	0,	false,	false,	false),
new Driver("IBM DB2",		"DB2",			8,	0,	false,	false,	false)
   };

   public static Driver getDriver(int nIndex) {
      return ((nIndex >= 0 && nIndex < DriverMap.length) ? DriverMap[nIndex] : null);
   }

   public static String getIndexTypeMap(int nIndex) {
      return ((nIndex >= 0 && nIndex < IndexTypeMap.length) ? IndexTypeMap[nIndex] : null);
   }


   public static String txn_isolation_name(int txn_isolation) {
    switch (txn_isolation) {
      case Connection.TRANSACTION_READ_UNCOMMITTED:
        return "Uncommitted";
      case Connection.TRANSACTION_READ_COMMITTED:
        return "Committed";
      case Connection.TRANSACTION_REPEATABLE_READ:
        return "Repeatable";
      case Connection.TRANSACTION_SERIALIZABLE:
        return "Serializable";
      default:
        return "Default";
    }
   }


   public static String cursor_type_name (int nCursorType) {

    return
      (nCursorType == ResultSet.TYPE_SCROLL_SENSITIVE ? "Sensitive" :
        (nCursorType == ResultSet.TYPE_SCROLL_INSENSITIVE ? "Insensitive" : "ForwardOnly"));

   }


}
