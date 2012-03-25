/*
 *  $Id$
 *
 *  jdbc-bench - a TPC-A and TPC-C like benchmark program for JDBC drivers
 *
 *  Copyright (C) 2000-2012 OpenLink Software <jdbc-bench@openlinksw.com>
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

import java.awt.*;
import java.awt.event.*;
import java.io.File;

import javax.swing.*;


public class RunSelected extends JPanel {
  GridLayout gridLayout1 = new GridLayout();
  JPanel jPanel1 = new JPanel();
  JPanel jPanel2 = new JPanel();
  JLabel jLabel1 = new JLabel();
  JTextField nEndTime = new JTextField("1");
  JLabel jLabel2 = new JLabel();
  JTextField sOutputFile = new JTextField("results.xml");
  JButton jButton1 = new JButton("Select...");
  BorderLayout borderLayout1 = new BorderLayout();
  BorderLayout borderLayout2 = new BorderLayout();

  public RunSelected() {
    try {
      jbInit();
    }
    catch(Exception ex) {
      ex.printStackTrace();
    }
  }
  void jbInit() throws Exception {
    gridLayout1.setRows(2);
    gridLayout1.setColumns(1);
    gridLayout1.setHgap(10);
    gridLayout1.setVgap(10);
    this.setLayout(gridLayout1);
    jLabel1.setText("Test durations (mins)");
    jPanel1.setLayout(borderLayout1);
    jLabel2.setText("Output file name");
    jPanel2.setLayout(borderLayout2);
    borderLayout1.setHgap(10);
    borderLayout1.setVgap(10);
    borderLayout2.setHgap(10);
    borderLayout2.setVgap(10);
    jButton1.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        jButton1_actionPerformed(e);
      }
    });
    this.add(jPanel1, null);
    jPanel1.add(jLabel1, BorderLayout.WEST);
    jPanel1.add(nEndTime, BorderLayout.CENTER);
    this.add(jPanel2, null);
    jPanel2.add(jLabel2, BorderLayout.WEST);
    jPanel2.add(sOutputFile, BorderLayout.CENTER);
    jPanel2.add(jButton1, BorderLayout.EAST);
  }

  void jButton1_actionPerformed(ActionEvent e) {
    JFileChooser chooser = new JFileChooser();
    chooser.setDialogTitle("Select output file name");
    SimpleFileFilter filter = new SimpleFileFilter("xml");
    chooser.setFileFilter(filter);
    chooser.setCurrentDirectory(new File("."));
    int returnVal = chooser.showSaveDialog(RunSelected.this);
    if(returnVal == JFileChooser.APPROVE_OPTION) {
       String name = chooser.getSelectedFile().getPath();
       if (name.length() <= 4 || (name.length() > 4 && (! name.regionMatches(true, name.length() - 4, ".xml", 0, 4))))
         sOutputFile.setText(name+".xml");
       else
         sOutputFile.setText(name);
    }
  }

}
