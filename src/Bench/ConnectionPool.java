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


public class ConnectionPool extends AbstractTableModel {

  Vector connections = new Vector();

  public static final int URL = 2;
  public static final int DBMS_NAME = 3;
  public static final int DRIVER_NAME = 4;
  public static final int DRIVER_VER = 5;

  public ConnectionPool()
    {
      connections.removeAllElements();
    }

  public synchronized LoginData getConnection(int nIndex) throws ArrayIndexOutOfBoundsException
    {
      return (LoginData)connections.elementAt(nIndex);
    }

  public synchronized LoginData getLastConnection()
    {
      try {
        for (int i = connections.size() - 1 ; i >= 0; i--) {
           LoginData data = (LoginData)connections.elementAt(i);
           if (data.conn != null)
              return data;
        }
      } catch (Exception e) {};
      return null;
    }

  public synchronized int getRowCount() { return connections.size(); }
  public int getColumnCount() { return 7; }

  public synchronized Object getValueAt(int row, int column)
    {
      Object ret = null;
      try
	{
	  LoginData data = getConnection(row);
	  switch (column)
	    {
	      case 0:
		  ret = "TPC-A";
		  break;
	      case 1:
		  ret = data.strItemName;
		  break;
	      case 2:
		  ret = data.strURL;
		  break;
	      case 3:
		  ret = data.strDBMSName;
		  break;
	      case 4:
		  ret = data.strDriverName;
		  break;
	      case 5:
		  ret = data.strDriverVer;
		  break;
	      case 6:
		  if (data.m_Driver != null)
		    ret = data.m_Driver.m_strDriverName;
		  else
		    ret = "<undefined>";
		  break;
	    }
	}
      catch (Exception e) {
      }
      return ret;
    }

  public synchronized LoginData addLoginToThePool(LoginData data)
    {
      int nPlace = getRowCount();
      connections.addElement(data);
      fireTableRowsInserted(nPlace, nPlace);
      return data;
    }

  public synchronized LoginData addLoginToThePool(String strURL, String strUID, String strPWD) throws SQLException
    {
      return addLoginToThePool(new LoginData(strURL, strUID, strPWD));
    }

  public synchronized void removeLoginsFromThePool(int rows[]) throws SQLException, ArrayIndexOutOfBoundsException
    {
      for (int nRow = rows.length - 1; nRow >= 0; nRow--)
	{
          getConnection(rows[nRow]).doLogout();
	  connections.removeElementAt(rows[nRow]);
	  fireTableRowsDeleted(rows[nRow], rows[nRow]);
	}
    }

  public synchronized void removeLoginFromThePool(int row) throws SQLException, ArrayIndexOutOfBoundsException
    {
      getConnection(row).doLogout();
      connections.removeElementAt(row);
      fireTableRowsDeleted(row, row);
    }

  public static final String Titles[] = {
	"Type",
	"Name",
	"URL",
	"DBMS",
	"Driver name",
	"Driver version",
	"DBMS type"
  };

  public String getColumnName(int nColumn)
    {
      return Titles[nColumn];
    }
  public void fireTableRowsUpdated(int n1, int n2)
    {
      super.fireTableRowsUpdated(n1, n2);
    }
}
