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

import org.apache.crimson.tree.*;
import org.w3c.dom.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;
import javax.xml.parsers.*;
import java.sql.*;
import java.util.*;
import java.io.IOException;
import java.io.File;

public class ItemLoader extends DefaultHandler {
   BenchPanel  pane;
   XmlDocument doc;
   Vector items = new Vector();
   LoginData curItem;

   public ItemLoader(BenchPanel _pane){
    pane = _pane;
   }

   public Vector LoadItemsFrom(String fileName) {
     SAXParserFactory factory = SAXParserFactory.newInstance();
     factory.setValidating(true);
     try {
       // Parse the input
       SAXParser saxParser = factory.newSAXParser();
       saxParser.parse( new File(fileName), this);
       return items;
     } catch (SAXParseException spe) {
       // Error generated by the parser
       pane.log("\n** Parsing error" + ", line " + spe.getLineNumber()
        + ", uri " + spe.getSystemId()+ "\n", 0);
       pane.log("   " + spe.getMessage() + "\n", 0 );
     } catch (SAXException sxe) {
       // Error generated by this application (or a parser-initialization error)
       pane.log("\n** SAX error " + sxe, 0);
     } catch (ParserConfigurationException pce) {
       // Parser with specified options can't be built
       pane.log("\n** SAX config error " + pce, 0);
     } catch (IOException ioe) {
       // I/O error
       pane.log("\n** IO error " + ioe, 0);
     }
     return null;
  }

    public void startElement(String namespaceURI,
                             String lname, // local name
                             String name, // qualified name
                             Attributes attr) throws SAXException
    {
      if (name.equals("test")) {
         String id = attr.getValue("id");
         String type = attr.getValue("type");
         if (type.equals("tpc_a"))
           curItem = new LoginData(id);
      } else if (name.equals("dsn") && curItem != null) {
          curItem.strURL = attr.getValue("name");
          curItem.strUID = attr.getValue("uid");
          curItem.strDriverName = attr.getValue("Driver");
      } else if (name.equals("aschema") && curItem != null) {
          curItem.tpca.bCreateProcedures = attr.getValue("procedures").equals("1");
          curItem.tpca.bCreateIndexes = attr.getValue("indexes").equals("1");
      } else if (name.equals("table") && curItem != null) {
          String tname = attr.getValue("name");
          boolean create = attr.getValue("create").equals("1");
          boolean load = attr.getValue("load").equals("1");
          int count = 0;
          try {
            count = Integer.parseInt(attr.getValue("count"));
          } catch (Exception e) {}
          if (tname.equals("branch")) {
            curItem.tpca.bCreateBranch = create;
            curItem.tpca.bLoadBranch = load;
            curItem.tpca.nMaxBranch = count;
          } else if (tname.equals("teller")) {
            curItem.tpca.bCreateTeller = create;
            curItem.tpca.bLoadTeller = load;
            curItem.tpca.nMaxTeller = count;
          } else if (tname.equals("account")) {
            curItem.tpca.bCreateAccount = create;
            curItem.tpca.bLoadAccount = load;
            curItem.tpca.nMaxAccount = count;
          } else if (tname.equals("history")) {
            curItem.tpca.bCreateHistory = create;
          }
      } else if (name.equals("arun") && curItem != null) {
          curItem.tpca.nThreads = 0;
          try {
            curItem.tpca.nThreads = Integer.parseInt(attr.getValue("threads"));
          } catch (Exception e) {}
          curItem.tpca.travCount = 0;
          try {
            curItem.tpca.travCount = Integer.parseInt(attr.getValue("traversal"));
          } catch (Exception e) {}
          curItem.tpca.bQuery = attr.getValue("query").equals("1");
          curItem.tpca.bTrans = attr.getValue("transactions").equals("1");
          String txn = attr.getValue("isolation");
          String scrs = attr.getValue("cursor");
          String type = attr.getValue("type");
          try {
            curItem.tpca.nBatchSize = Integer.parseInt(attr.getValue("batchitems"));
          } catch (Exception e) {}

          if (txn.equals("Uncommitted"))
            curItem.tpca.txnOption = Connection.TRANSACTION_READ_UNCOMMITTED;
          else if (txn.equals("Committed"))
            curItem.tpca.txnOption = Connection.TRANSACTION_READ_COMMITTED;
          else if (txn.equals("Repeatable"))
            curItem.tpca.txnOption = Connection.TRANSACTION_REPEATABLE_READ;
          else if (txn.equals("Serializable"))
            curItem.tpca.txnOption = Connection.TRANSACTION_SERIALIZABLE;
          else
            curItem.tpca.txnOption = TPCABench.TXN_DEFAULT;


          if (scrs.equals("Insensitive"))
            curItem.tpca.scrsOption = ResultSet.TYPE_SCROLL_INSENSITIVE;
          else if (scrs.equals("Sensitive"))
            curItem.tpca.scrsOption = ResultSet.TYPE_SCROLL_SENSITIVE;
          else
            curItem.tpca.scrsOption = ResultSet.TYPE_FORWARD_ONLY;


          if (type.equals("prepare"))
            curItem.tpca.sqlOption = TPCABench.RUN_PREPARED;
          else if (type.equals("procedures"))
            curItem.tpca.sqlOption = TPCABench.RUN_SPROC;
          else
            curItem.tpca.sqlOption = TPCABench.RUN_TEXT;
      }
    }

    public void endElement(String namespaceURI,
                           String lname, // simple name
                           String name  // qualified name
                           ) throws SAXException
    {
      if (name.equals("test") && curItem != null) {
          items.addElement(curItem);
          curItem = null;
      }
    }

    // treat validation errors as fatal
    public void error(SAXParseException e) throws SAXParseException
    {
        throw e;
    }

    // dump warnings too
    public void warning(SAXParseException err)
    throws SAXParseException
    {
       pane.log("** Warning"
            + ", line " + err.getLineNumber()
            + ", uri " + err.getSystemId()+"\n",0);
       pane.log("   " + err.getMessage()+"\n", 0);
    }

}
