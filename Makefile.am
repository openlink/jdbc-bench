#
#  This file is part of the OpenLink JDBC Benchmark project
#
#  Copyright (C) 1999-2008 OpenLink Software
#
#  This project is free software; you can redistribute it and/or modify it
#  under the terms of the GNU General Public License as published by the
#  Free Software Foundation; only version 2 of the License, dated June 1991
#
#  This project is distributed in the hope that it will be useful, but
#  WITHOUT ANY WARRANTY; without even the implied warranty of
#  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
#  General Public License for more details.
#
#  You should have received a copy of the GNU General Public License
#  along with this program; if not, write to the Free Software Foundation,
#  Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
#

AUTOMAKE_OPTIONS	= gnu 1.7 dist-zip

if JDK
TARGETS         = jdbc-bench.jar
JDK             = @ac_jdk_path@
JDK_FLAGS       = -Xlint -O -g:none
JAVAC           = $(JDK)/bin/javac
JAVAH           = $(JDK)/bin/javah
JAVADOC         = $(JDK)/bin/javadoc
JAR             = $(JDK)/bin/jar
endif

BUILT_SOURCES	= jdbc-bench.jar
DISTCLEANFILES	= jdbc-bench.jar

JDBC_BENCH_SRC	=  \
		$(srcdir)/src/*.java  \
		$(srcdir)/src/Bench/*.java

JDBC_BENCH_CLASSPATH = $(srcdir)/classes/crimson.jar

jdbc-bench.jar: $(JDBC_BENCH_SRC)
	-mkdir out.jdbc-bench
	$(JAVAC) $(JDK_FLAGS) -d out.jdbc-bench \
		-classpath "$(JDBC_BENCH_CLASSPATH)" \
		$(JDBC_BENCH_SRC)
	(cd src; find Bench/SQL -type f -print | grep -v CVS | cpio -vdump ../out.jdbc-bench )
	$(JAR) cvf jdbc-bench.jar -C out.jdbc-bench .


clean-local:
	-rm -rf out.jdbc-bench

# ----------------------------------------------------------------------
#
#  Maintainers only
#
# ----------------------------------------------------------------------

if MAINTAINER_MODE

#
#  Create a source snapshot package
#
snapshot:
	$(MAKE) dist distdir=$(PACKAGE)-`date +"%Y%m%d"`



#
#  Generate ChangeLog
#
changelog:
	cvs2cl --no-wrap --hide-filenames --utc --prune --window 3600

endif


# ----------------------------------------------------------------------
#
#  Distributable Files
#
# ----------------------------------------------------------------------

EXTRA_DIST = \
	$(srcdir)/AUTHORS \
	$(srcdir)/ChangeLog \
	$(srcdir)/COPYING \
	$(srcdir)/INSTALL \
	$(srcdir)/NEWS \
	$(srcdir)/README \
	$(srcdir)/classes/crimson.jar \
	$(srcdir)/jdbc-bench.jar \
	$(srcdir)/jdbc-bench.bat \
	$(srcdir)/jdbc-bench.dtd \
	$(srcdir)/jdbc-bench.sh \
	$(srcdir)/src/*.java \
	$(srcdir)/src/Bench/*.java \
	$(srcdir)/src/Bench/SQL/*.sql \
	$(srcdir)/src/Bench/SQL/TPCC/MSSQL/create/*.sql \
	$(srcdir)/src/Bench/SQL/TPCC/MSSQL/create/list \
	$(srcdir)/src/Bench/SQL/TPCC/MSSQL/drop \
	$(srcdir)/src/Bench/SQL/TPCC/Virtuoso/create/*.sql \
	$(srcdir)/src/Bench/SQL/TPCC/Virtuoso/create/list \
	$(srcdir)/src/Bench/SQL/TPCC/Virtuoso/drop \
	$(srcdir)/src/Bench/SQL/tran.ingres \
	$(srcdir)/src/Bench/SQL/tran.ora \
	$(srcdir)/src/Bench/SQL/tran.sybase \
	$(srcdir)/src/Bench/SQL/tran.virt
