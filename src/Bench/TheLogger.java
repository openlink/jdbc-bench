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


import javax.swing.*;


class TheLogger implements Logger {
   JTextArea m_logArea = null;
   int m_nOutLogLevel = 0,nb_threads;

   public TheLogger(JTextArea out) {
      m_logArea = out;
   }

   public synchronized void log(String strMessage, int nLevel) {
      if(nLevel <= m_nOutLogLevel)
	{
	  if (m_logArea.getLineCount() > 100)
	    m_logArea.setText("");
         m_logArea.append(strMessage);
      }
   }

   public synchronized void waitOn() throws InterruptedException {
   }

   synchronized public void taskDone() {
      nb_threads--; notify();
   }

   synchronized public void setLogLevel(int nNewLogLevel) {
      m_nOutLogLevel = nNewLogLevel;
   }

   synchronized public int getLogLevel() {
      return m_nOutLogLevel;
   }

   synchronized public int getNbThread() {
      return nb_threads;
   }
}


