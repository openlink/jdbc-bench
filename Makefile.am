#
#  This file is part of the OpenLink JBench project
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
TARGETS         = jbench.jar
JDK             = @ac_jdk_path@
JDK_FLAGS       = -Xlint -O -g:none
JAVAC           = $(JDK)/bin/javac
JAVAH           = $(JDK)/bin/javah
JAVADOC         = $(JDK)/bin/javadoc
JAR             = $(JDK)/bin/jar
endif

BUILT_SOURCES	= jbench.jar
DISTCLEANFILES	= jbench.jar

JBENCH_SRC	=  \
		$(srcdir)/src/*.java  \
		$(srcdir)/src/Bench/*.java

JBENCH_CLASSPATH = $(srcdir)/classes/crimson.jar

jbench.jar: $(JBENCH_SRC)
	-mkdir out.jbench
	$(JAVAC) $(JOPT) -d out.jbench \
		-classpath "$(JBENCH_CLASSPATH)" \
		$(JBENCH_SRC)
	(cd src; find Bench/SQL -type f -print | grep -v CVS | cpio -vdump ../out.jbench )
	$(JAR) cvf jbench.jar -C out.jbench .


clean-local:
	-rm -rf out.jbench

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
	$(srcdir)/jbench.jar \
	$(srcdir)/jbench.bat \
	$(srcdir)/jbench.dtd \
	$(srcdir)/jbench.sh \
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
