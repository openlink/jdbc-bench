/*
 *  $Id$
 *
 *  jdbc-bench - a TPC-A and TPC-C like benchmark program for JDBC drivers
 *
 *  Copyright (C) 2000-2013 OpenLink Software <jdbc-bench@openlinksw.com>
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

import org.apache.crimson.tree.*;
import org.w3c.dom.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;
import javax.xml.parsers.*;
import java.sql.*;
import java.util.*;
import java.io.IOException;
import java.io.File;

public class Results {


   BenchPanel  pane;
   XmlDocument doc;
   Element     root;
   int         nEndTime;

   public Results(BenchPanel _pane, int _nEndTime){
    pane = _pane;
    nEndTime = _nEndTime;

    doc = new XmlDocument();
    doc.setDoctype(null, "jdbc-bench.dtd", null);
   }

   public static Results InitResultsSaving(BenchPanel _pane, int _nEndTime) {
      Results res = new Results(_pane, _nEndTime);

      res.root = (Element) res.doc.createElement("run");
      res.root.setAttribute("duration", Integer.toString(_nEndTime));

      return res;
   }

   public XmlDocument DoneResultsSaving() {
      doc.appendChild(root);
      doc.getDocumentElement().normalize();

      return doc;
   }


   public static Results InitItemsSaving(BenchPanel _pane) {
      Results res = new Results(_pane, 0);
      res.root = (Element) res.doc.createElement("tests");
      return res;
   }


   public void SaveAllItems(ConnectionPool pool) {
      for (int nRow = 0; nRow < pool.getRowCount(); nRow++)
        root.appendChild(create_test(pool.getConnection(nRow)));
   }

   public void SaveSelectedItems(ConnectionPool pool, int[] selection) {
      for (int nRow = 0; nRow < selection.length; nRow++)
        root.appendChild(create_test(pool.getConnection(selection[nRow])));
   }

   public XmlDocument DoneItemsSaving() {
      doc.appendChild(root);
      doc.getDocumentElement().normalize();

      return doc;
   }


   public synchronized void printResults(long startingTime, int[] selection)
   {
     if (!pane.thr.isCanceled())
       {
	 pane.log("\nTest Completed\n",0);
         long currTime = System.currentTimeMillis();
	 double dDiff = currTime - startingTime;
	 double allTimes = 0;
         int i = 0;
         boolean wasErrors;
         String message = "";
         String state = "OK";

         root.setAttribute("end", new java.util.Date(currTime).toString());

	 for (int nRow = 0; nRow < selection.length; nRow++) {
           LoginData data = pane.pool.getConnection(selection[nRow]);
           wasErrors = false;
           if (data.tpca.supported) {
	     long nTrans = 0, nTrans1Sec = 0, nTrans2Sec = 0;
	     double dTotalTime = 0;
	     int nThread;
	     for(nThread = 0; nThread < data.tpca.nThreads; nThread++)
		{
		     TPCABench currThread = (TPCABench)pane.m_tpcaBench.elementAt(i++);
		     if (currThread.isAlive()) {
                        currThread.interrupt();
           	        pane.log("\nSome threads are hanging - the run is invalid\n", 0);
                        wasErrors = true;
                        message = "Some threads are hanging - the run is invalid";
                        state = "ER";
                     }
		     if (currThread.SQLError != null) {
			 pane.log("***Error : Thread " + nThread + " : " + currThread.SQLError, 0);
                         wasErrors = true;
                         message = currThread.SQLError;
                         state = "ER";
                     }
                     if (!wasErrors) {
		       nTrans += currThread.getNTrans();
		       nTrans1Sec += currThread.getTrans1Sec();
		       nTrans2Sec += currThread.getTrans2Sec();
                       if (currThread.getTotalTime() > dTotalTime)
		         dTotalTime = currThread.getTotalTime();
                     }
		}
             allTimes += dTotalTime;
             boolean bRealTrans = data.tpca.bTrans;

	     if (wasErrors) {
	       pane.log("\nSome threads ended with an error - the run is invalid\n", 0);
	       allTimes = -1;
               dTotalTime = 0;
               nTrans = -1;
               nTrans1Sec = -1;
               nTrans2Sec = -1;
             } else {
	        try {
	          bRealTrans &= data.conn.getMetaData().supportsTransactions();
	        } catch (SQLException e) {};
             }

             root.appendChild( printStats(data, bRealTrans, dTotalTime,
                nTrans, nTrans1Sec, nTrans2Sec, state, message, wasErrors));

           } else {
             //unsupported task
//FIXME	     Results.printStats(this, data, data.tpca.bTrans, 0, -1, -1, -1);
           }
         }
	 if (allTimes > 0)
	   pane.log("Environmental overhead for the run =" +
	        (
                 (dDiff / 1000) - (allTimes/selection.length)
                ) + " s\n", 0);
       }

     pane.m_tpcaBench.removeAllElements();
   }


  public Element printStats(LoginData data,
      boolean bUseTransactions,  double dTotalTime,
      long nTotalTransactions, long nTrans1Sec, long nTrans2Sec,
      String state, String message, boolean wasErrors)
  {
    int nThreads = data.tpca.nThreads;
    boolean bQuery = data.tpca.bQuery;
    int txnOption = data.tpca.txnOption;
    int scrsOption = data.tpca.scrsOption;
    int traversalCount = data.tpca.travCount;

    StringBuffer strTestType = new StringBuffer(nThreads + (nThreads == 1 ? " thread" : " threads"));
    if (data.tpca.nBatchSize > 1)
      strTestType.append("/ "+ data.tpca.nBatchSize + " BatchItems");
    switch(data.tpca.sqlOption) {
         case TPCABench.RUN_TEXT:
  	    strTestType.append("/SQL execute");
            break;
         case TPCABench.RUN_PREPARED:
	    strTestType.append("/Prepare-Execute");
            break;
         case TPCABench.RUN_SPROC:
  	    strTestType.append("/Stored proc");
            break;
    }
    if (nEndTime > 0)
	 strTestType.append("/for " + nEndTime + " min");

    strTestType.append(bUseTransactions ? "/Txn" : "");
    strTestType.append(bQuery ? "/Query" : "");

    if (bQuery && scrsOption != ResultSet.TYPE_FORWARD_ONLY) {
          strTestType.append("/" + Driver.cursor_type_name(scrsOption));
          strTestType.append(traversalCount > 1 ? "/" + traversalCount + " Traversals": "");
    }

    if (txnOption != TPCABench.TXN_DEFAULT)
        strTestType.append("/" + Driver.txn_isolation_name(txnOption));

    String strResultTestType = strTestType.toString();
    double tps = -1, sub1s = -1, sub2s = -1, trntime = -1;
    // System.out.println("STAT : TOTAL TIME : " + dTotalTime + " : TRANS : " + nTotalTransactions + " : TRAN1 : " + nTrans1Sec + " : TRAN2 : " + nTrans2Sec);
    pane.log("\nStatistics :\n",0);
    if (wasErrors)
      pane.log("\nError :" + message  + "\n",0);
    pane.log("\tSQL Options used:\t\t\t" + strResultTestType + "\n",0);
    pane.log("\tTransaction time :\t\t\t" + dTotalTime + "\n",0);
    pane.log("\tTotal transactions:\t\t\t" + nTotalTransactions + "\n",0);
    pane.log("\tTransactions per second:\t\t" +
	  (dTotalTime > 0 ?
	   String.valueOf(tps = (double)(nTotalTransactions / dTotalTime)) :
	   "Infinity") +
	  "\n",
	  0);
    pane.log("\t% less than 1 second:\t\t" +
	  (nTotalTransactions > 0 ?
	   String.valueOf(sub1s = (double)(nTrans1Sec * 100 / nTotalTransactions)) :
	   "Not defined") +
	  "\n",
	  0);
    pane.log("\t% 1 < n < 2 seconds:\t\t" +
	  (nTotalTransactions > 0 ?
	   String.valueOf(sub2s = (double)(nTrans2Sec * 100 / nTotalTransactions)) :
	   "Not defined") +
	  "\n",
	  0);
    pane.log("\t% more than 2 seconds:\t\t" +
	  (nTotalTransactions > 0 ?
	   String.valueOf((double)((nTotalTransactions - nTrans2Sec - nTrans1Sec) * 100 /
	       nTotalTransactions)) :
	   "Not defined") + "\n",
	  0);
    pane.log("\tAverage processing time:\t\t" +
	  (nTotalTransactions > 0 ?
	   String.valueOf(trntime = (double)(dTotalTime / nTotalTransactions)) :
	   "Not defined") +
	  "\n",
	  0);

    addResultsRecord(pane, data, "TPC-A",strResultTestType,tps,dTotalTime,
	nTotalTransactions,sub1s,sub2s,trntime, state, message);
    return addXmlResults(data, bUseTransactions,
      dTotalTime, nTotalTransactions, nTrans1Sec, nTrans2Sec,
      tps, sub1s, sub2s, trntime, state, message);
  }

  public static void addResultsRecord(BenchPanel pane, LoginData data,
      String strTestType, String strResultTestType,
      double tps, double dTotalTime, long nTotalTransactions,
      double sub1s,  double sub2s, double trntime,
      String state, String message)
  {
    String strNowFunction = (pane.results != null ? pane.results.strNowFunction : data.strNowFunction);
    StringBuffer strSQL = new StringBuffer("insert into resultsj (btype, runt, url, options, tps, tottime, ntrans, sub1s, sub2s, trntime, drvrname, drvrver, state, message) values (\'");
    strSQL.append(strTestType); strSQL.append("\', ");
    strSQL.append(strNowFunction); strSQL.append(" , ");
    strSQL.append("\'" + data.strURL + "\'"); strSQL.append(" , ");
    strSQL.append("\'" + strResultTestType + "\'");
    strSQL.append(" , ");
    if(tps != -1)
      strSQL.append(tps);
    else
      strSQL.append("null");
    strSQL.append(" , ");

    strSQL.append(dTotalTime);
    strSQL.append(" , ");

    if(nTotalTransactions != -1)
      strSQL.append(nTotalTransactions);
    else
      strSQL.append("null");
    strSQL.append(" , ");

    if(sub1s != -1)
      strSQL.append(sub1s);
    else
      strSQL.append("null");
    strSQL.append(" , ");

    if(sub2s != -1)
      strSQL.append(sub2s);
    else
      strSQL.append("null");
    strSQL.append(" , ");

    if(trntime != -1)
      strSQL.append(trntime);
    else
      strSQL.append("null");
    strSQL.append(" , ");

    strSQL.append(data.strDriverName == null ? "null" : "\'" + data.strDriverName + "\'");
    strSQL.append(" , ");

    strSQL.append(data.strDriverVer == null ? "null" : "\'" + data.strDriverVer + "\'");
    strSQL.append(" , ");
    strSQL.append("\'" + state + "\'");
    strSQL.append(" , ");
    strSQL.append("\'" + message + "\'");
    strSQL.append(" ) ");

    Statement stmt = null;
    boolean isSaved = false;
    try {

      if (pane.results != null)
	stmt = pane.results.conn.createStatement();
      else
	stmt = data.conn.createStatement();

      pane.log(strSQL + "\n",2);
      stmt.executeUpdate(strSQL.toString());
      isSaved = true;
    } catch(Exception e) {
        pane.log("Error inserting into results table : " + e.getMessage() + "\n",1);
    } finally {
       if (isSaved) {
         if (pane.results != null)
           pane.log("Adding result row in " + pane.results.strURL + "(" +
                     pane.results.strDBMSName + ")\n", 1);
         else
           pane.log("Results written to the results table using main connection \n", 0);
       }
       if (stmt != null)
	  try {
	   stmt.close();
          } catch(SQLException e) { }
    }
  }

  public Element addXmlResults(LoginData data,
      boolean bUseTransactions,
      double dTotalTime, long nTotalTransactions,
      long nTrans1Sec, long nTrans2Sec,
      double tps, double sub1s,  double sub2s, double trntime,
      String state, String message)
  {
    Element el = (Element) doc.createElement("aresult");
      el.setAttribute("state", state);
      el.setAttribute("message", message);

    el.appendChild(create_test(data));

    el.appendChild(create_simple_elm("TransactionTime", Double.toString(dTotalTime)));
    el.appendChild(create_simple_elm("Transactions", Long.toString(nTotalTransactions)));
    el.appendChild(create_simple_elm("Transactions1Sec", Long.toString(nTrans1Sec)));
    el.appendChild(create_simple_elm("Transactions2Sec", Long.toString(nTrans2Sec)));
    el.appendChild(create_simple_elm("TransactionsPerSec", Double.toString(tps)));
    el.appendChild(create_simple_elm("Sub1SecPct", Double.toString(sub1s)));
    el.appendChild(create_simple_elm("Sub2SecPct", Double.toString(sub2s)));
    el.appendChild(create_simple_elm("AvgProcTime", Double.toString(trntime)));

    return el;
  }

/////////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////

  public Element create_simple_elm(String name, String value) {
    Element el = (Element) doc.createElement(name);
    el.appendChild( doc.createTextNode(value));
    return el;
  }

  public Element create_test(LoginData data) {
     Element el = (Element) doc.createElement("test");
      el.setAttribute("id", data.strItemName);
      el.setAttribute("type", "tpc_a");
     el.appendChild(create_login(data));
     el.appendChild(create_aschema(data));
     el.appendChild(create_arun(data));
     return el;
  }

  public Element create_login(LoginData data) {
     Element el = (Element) doc.createElement("login");
     el.appendChild(create_dsn(data));
     return el;
  }


  public Element create_aschema(LoginData data) {
     LoginData.TPCA tpca = data.tpca;
     Element el = (Element) doc.createElement("aschema");
      el.setAttribute("procedures", (tpca.bCreateProcedures ? "1" : "0"));
      el.setAttribute("indexes", (tpca.bCreateIndexes ? "1" : "0"));
     el.appendChild(create_table("branch", (tpca.bCreateBranch ? 1 : 0),
            (tpca.bLoadBranch ? 1 : 0), tpca.nMaxBranch));
     el.appendChild(create_table("teller", (tpca.bCreateTeller ? 1 : 0),
            (tpca.bLoadTeller ? 1 : 0), tpca.nMaxTeller));
     el.appendChild(create_table("account", (tpca.bCreateAccount ? 1 : 0),
            (tpca.bLoadAccount ? 1 : 0), tpca.nMaxBranch));
     el.appendChild(create_table("history",(tpca.bCreateHistory ? 1 : 0), 0, 0));
     return el;
  }

  public Element create_arun(LoginData data) {
     LoginData.TPCA tpca = data.tpca;
     Element el = (Element) doc.createElement("arun");
      el.setAttribute("threads", Integer.toString(tpca.nThreads));
      el.setAttribute("transactions", (tpca.bTrans ? "1" : "0"));
      el.setAttribute("query", (tpca.bQuery ? "1" : "0"));
      el.setAttribute("isolation", Driver.txn_isolation_name(tpca.txnOption));
      el.setAttribute("cursor", Driver.cursor_type_name(tpca.scrsOption));
      el.setAttribute("traversal", Integer.toString(tpca.travCount));
      el.setAttribute("type", sqlOption_name(tpca.sqlOption));
      el.setAttribute("batchitems", Integer.toString(data.tpca.nBatchSize));
     return el;
  }

  public Element create_dsn(LoginData data) {
     Element el = (Element) doc.createElement("dsn");
      el.setAttribute("name", (data.strURL == null ? "": data.strURL));
      el.setAttribute("uid", (data.strUID == null ? "": data.strUID));
      el.setAttribute("DBMS", (data.strDBMSName == null ? "": data.strDBMSName));
      el.setAttribute("DBMSVer", (data.strDBMSVer == null ? "": data.strDBMSVer));
      el.setAttribute("Driver", (data.strDriverName == null ? "": data.strDriverName));
      el.setAttribute("DriverVer", (data.strDriverVer == null ? "": data.strDriverVer));
     return el;
  }

  public Element create_table(String name, int create, int load, int count) {
     Element el = (Element) doc.createElement("table");
      el.setAttribute("name", name);
      el.setAttribute("create", Integer.toString(create));
      el.setAttribute("load",  Integer.toString(load));
      el.setAttribute("count", Integer.toString(count));
     return el;
  }


  public String sqlOption_name(int sqlOption) {
    switch(sqlOption) {
      case TPCABench.RUN_TEXT:
  	return "execute";
      case TPCABench.RUN_PREPARED:
	return "prepare";
      case TPCABench.RUN_SPROC:
  	return "procedure";
    }
    return "";
  }

}
