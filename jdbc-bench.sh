#!/bin/sh
#
#  $Id$
#
#  jdbc-bench - a TPC-A and TPC-C like benchmark program for JDBC drivers
#  
#  Copyright (C) 2000-2008 OpenLink Software <jdbc-bench@openlinksw.com>
#  All Rights Reserved.
#  
#  This program is free software; you can redistribute it and/or modify
#  it under the terms of the GNU General Public License as published by
#  the Free Software Foundation; only Version 2 of the License dated
#  June 1991.
#  
#  This program is distributed in the hope that it will be useful,
#  but WITHOUT ANY WARRANTY; without even the implied warranty of
#  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#  GNU General Public License for more details.
#  
#  You should have received a copy of the GNU General Public License
#  along with this program; if not, write to the Free Software
#  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
#


echo ---------------------------------------------------------------------------
echo TPC-A and TPC-C like Benchmark Program for JDBC Drivers and Databases
echo from OpenLink Software
echo ---------------------------------------------------------------------------

CLASSPATH=./jdbc-bench.jar:./classes/crimson.jar:$CLASSPATH
export CLASSPATH

java -Djdbc.drivers=openlink.jdbc3.Driver BenchMain DRIVERTYPE=0
