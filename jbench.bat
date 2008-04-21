@echo off
REM
REM  JBench - a JDBC Benchmark program
REM  Copyright (C) 2002 OpenLink Software.
REM
REM  This program is free software; you can redistribute it and/or modify
REM  it under the terms of the GNU General Public License as published by
REM  the Free Software Foundation; either version 2 of the License, or
REM  (at your option) any later version.
REM
REM  This program is distributed in the hope that it will be useful,
REM  but WITHOUT ANY WARRANTY; without even the implied warranty of
REM  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
REM  GNU General Public License for more details.
REM
REM  You should have received a copy of the GNU General Public License
REM  along with this program; if not, write to the Free Software
REM  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
REM
REM  This is unpublished proprietary trade secret of OpenLink Software.
REM  This source code may not be copied, disclosed, distributed, demonstrated
REM  or licensed except as authorized by OpenLink Software.
REM
REM  To learn more about this product, or any other product in our
REM  portfolio, please check out our web site at:
REM
REM      http://www.openlinksw.com
REM
REM  or contact us at:
REM
REM      general.information@openlinksw.com
REM
REM  If you have any technical questions, please contact our support
REM  staff at:
REM
REM      technical.support@openlinksw.com
REM


echo -------------------------------------------------------------------------- 
echo TPC-Like Benchmark Program for JDBC Drivers and Databases 
echo from OpenLink Software 
echo -------------------------------------------------------------------------- 

set OPLCLASSPATH=.;.\classes\jbench15.jar;.\classes\crimson.jar
runjava2 . jdbc.drivers openlink.jdbc3.Driver BenchMain "DRIVERTYPE=0"
