AC_PREREQ(2.59)
AC_REVISION([$Id$])
#
#  This file is part of the OpenLink Java Bench project
#
#  Copyright (C) 1999-2013 OpenLink Software
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

##########################################################################
##									##
##  Initialization							##
##									##
##########################################################################
AC_INIT([OpenLink JDBC Benchmark Utility],
        [1.0],
	[jdbc-bench@openlinksw.com],
	[jdbc-bench])

AC_CONFIG_SRCDIR([src/BenchMain.java])
AC_CONFIG_AUX_DIR(admin)
AM_INIT_AUTOMAKE([1.9 gnu dist-zip])
AM_MAINTAINER_MODE


##########################################################################
##									##
##  Initialization							##
##									##
##########################################################################
ac_jdk_path="none"
AC_ARG_WITH(jdk, [dnl
AC_HELP_STRING([--with-jdk=DIR], [use the Sun JDK 1.3.x, 1.4.x, 1.5.x or 1.6.x at the specified location])dnl
], [ 
    case "${withval}" in
      yes) 	
          AC_MSG_ERROR("You need to specify a directory")
	  ;;

      no)	
	  ac_jdk_path="none"  
	  ;;
    
      *)	
	  if test -d "${withval}" ; then
	      ac_jdk_path="${withval}"
	  else	
	      AC_MSG_ERROR("Directory specified with --with-jdk does not exist")
	  fi
	  ;;
    esac
    ])
AC_MSG_RESULT(done)


#AC_PROG_JAVA_WORKS (jdk_path, min_acceptable_ver, max_acceptable_ver)
AC_DEFUN([AC_PROG_JAVA_WORKS], [
ac_jw_jdk="$1"
ac_jw_min_minor="$2"
ac_jw_max_minor="$3"
ac_cv_prog_java_works=no
#echo macro called with ac_jw_jdk="$ac_jw_jdk" ac_jw_min_minor="$ac_jw_min_minor" ac_jw_max_minor="$ac_jw_max_minor"
if test "x$ac_jw_jdk" != "xnone" ; then
  AC_MSG_CHECKING([for working Java 1.$ac_jw_min_minor.x-1.$ac_jw_max_minor.x in $ac_jw_jdk])
  #echo ac_jw_jdk/bin/java="$ac_jw_jdk/bin/java"
  ac_java_version=`$ac_jw_jdk/bin/java -version 2>&1 | grep 'java version' | cut -f 2 -d '"'`
  #echo ac_java_version="$ac_java_version"
  ac_java_ver_major=`echo $ac_java_version | cut -f 1 -d .`
  ac_java_ver_minor=`echo $ac_java_version | cut -f 2 -d .`
  #echo ac_java_ver_major="$ac_java_ver_major"
  #echo ac_java_ver_minor="$ac_java_ver_minor"
  if test "x$ac_java_ver_major" != "x1" -o "0$ac_java_ver_minor" -lt "0$ac_jw_min_minor" -o "0$ac_java_ver_minor" -gt "0$ac_jw_max_minor" ; then 
      AC_MSG_ERROR([wrong version ($ac_java_ver_major.$ac_java_ver_minor)])
  fi
  JAVAC=$ac_jw_jdk/bin/javac
  JAVA=$ac_jw_jdk/bin/java
  JAVA_TEST=Test.java
  CLASS_TEST=Test.class
  TEST=Test
  changequote(, )dnl
  cat << \EOF > $JAVA_TEST
/* [#]line __oline__ "configure" */
public class Test {
public static void main (String args[]) {
  System.exit (0);
} }
EOF
  changequote([, ])dnl
  if AC_TRY_COMMAND($JAVAC $JAVACFLAGS $JAVA_TEST) && test -s $CLASS_TEST; then
    :
  else
    echo "configure: failed program was:" >&AC_FD_CC
    cat $JAVA_TEST >&AC_FD_CC
    AC_MSG_ERROR(The Java compiler $JAVAC failed (see config.log, check the CLASSPATH?))
  fi
  if AC_TRY_COMMAND($JAVA $JAVAFLAGS $TEST) >/dev/null 2>&1; then
    ac_cv_prog_java_works=yes
  else
    echo "configure: failed program was:" >&AC_FD_CC
    cat $JAVA_TEST >&AC_FD_CC
    AC_MSG_ERROR(The Java VM $JAVA failed (see config.log, check the CLASSPATH?))
  fi
  rm -fr $JAVA_TEST $CLASS_TEST Test.uue
  AC_MSG_RESULT(found)
fi  
])

AC_PROG_JAVA_WORKS($ac_jdk_path, 3, 6)
ac_jdk_works="$ac_cv_prog_java_works"
AM_CONDITIONAL(JDK, test "x$ac_jdk_works" = "xyes")
AC_SUBST(ac_jdk_path)


########################################################################## 
##									##
##  Generate Makefiles etc.						##
##									##
##########################################################################
AC_CONFIG_FILES([
    Makefile 
])
AC_OUTPUT
