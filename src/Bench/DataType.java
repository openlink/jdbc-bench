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

class DataType {
   String m_strCharType,m_strFloatType,m_strIntType,m_strDateTimeType;

   public DataType(String strCharType, String strFloatType, String strIntType, String strDateTimeType) {
      //		System.out.println("DATATYPE: " + strCharType + " " +  strFloatType);
      m_strCharType = strCharType; m_strFloatType = strFloatType;
      m_strIntType = strIntType; m_strDateTimeType = strDateTimeType;
   }

   public String getCharType() {
      return m_strCharType;
   }

   public String getFloatType() {
      return m_strFloatType;
   }

   public String getIntType() {
      return m_strIntType;
   }

   public String getDateTimeType() {
      return m_strDateTimeType;
   }

   // static members
   // Constants for DATATYPE mapping structure
   //
   public static final String CHAR = "char";
   public static final String TEXT = "text";
   public static final String ALPHANUM = "alphanumeric";
   public static final String INT = "int";
   public static final String INTEGER = "integer";
   public static final String DATETIME = "datetime";
   public static final String TIMESTAMP = "timestamp";
   public static final String DATE = "date";
   public static final String NUMERIC = "numeric";
   public static final String NUMBER = "number";
   public static final String ORASHORT = "number(5,0)";
   public static final String ORALONG = "number(10,0)";
   public static final String QENUMERIC = "numeric(19,6)";
   public static final String LONG = "long";
   public static final String DOUBLE = "double";
   public static final String STRING = "string";
   public static final String INT4 = "Integer4";
   public static final String FLOAT8 = "Float8";
   public static final String FLOAT = "float";

   static DataType DataTypeMap[] = {//  		     lpChar        lpFloat       lpInt      lpDateTime
       new DataType(ALPHANUM,NUMBER,NUMBER,DATE), // 0
       new DataType(CHAR,FLOAT,INTEGER,DATETIME), // 1
       new DataType(CHAR,NUMERIC,NUMERIC,DATE), // 2
       new DataType(CHAR,NUMBER,ORALONG,DATE), // 3
       new DataType(CHAR,DOUBLE,LONG,DATETIME), // 4
       new DataType(CHAR,FLOAT,INT,DATETIME), // 5
       new DataType(TEXT,NUMBER,NUMBER,DATETIME), // 6
       new DataType(STRING,FLOAT8,INT4,DATE), // 7
       new DataType(CHAR,FLOAT,INTEGER,TIMESTAMP), // 8
       new DataType(CHAR,QENUMERIC,QENUMERIC,DATE), // 9
       new DataType(CHAR,FLOAT,INTEGER,DATE), // 10
       new DataType(CHAR,FLOAT,INTEGER,TIMESTAMP)// 11
   };

   public static DataType getType(int nIndex) {
      if(nIndex >= 0 && nIndex <= DataTypeMap.length) return DataTypeMap[nIndex];
      else return null;
   }

}
