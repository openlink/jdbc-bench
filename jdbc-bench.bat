@echo off
REM
REM  jdbc-bench - a TPC-A and TPC-C like benchmark program for JDBC drivers
REM  
REM  Copyright (C) 2000-2015 OpenLink Software <jdbc-bench@openlinksw.com>
REM  All Rights Reserved.
REM  
REM  This program is free software; you can redistribute it and/or modify
REM  it under the terms of the GNU General Public License as published by
REM  the Free Software Foundation; only Version 2 of the License dated
REM  June 1991.
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

echo -------------------------------------------------------------------------- 
echo TPC-A and TPC-C like Benchmark Program for JDBC Drivers and Databases 
echo from OpenLink Software 
echo -------------------------------------------------------------------------- 

set CLASSPATH=.;.\jdbc-bench.jar;.\classes\crimson.jar
java -Djdbc.drivers=openlink.jdbc4.Driver BenchMain DRIVERTYPE=0
