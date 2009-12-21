#!/bin/bash
#
# runs several tests (see conf dir) and sends results per email
#
# FracPete, 2007-05-28

# the usage of the script
function usage()
{
   echo
   echo "usage: ${0##*/} -q [-t <project>] [-g] [-h]"
   echo 
   echo "Runs tests on all available projects or a specific one."
   echo
   echo " -h   this help"
   echo " -q   suppresses the 'Press anykey...'" 
   echo " -g   disables the GUI (e.g., if run as cronjob)"
   echo "      runs Java in 'headless' mode"
   echo " -t   <project>"
   echo "      tests only this project instead of all"
   echo
}

# reads Property in TMP from the config-file CONFIG and returns it in TMP
# if property cannot be found, an empty string is returned
function get_property()
{
   COUNT=`cat $CONFIG | grep "^$TMP=" | wc -l | sed s/" "*//g`
   if [ $COUNT -gt 0 ]
   then
      TMP=`cat $CONFIG | grep "^$TMP=" | sed s/^.*=//g`
   else
      TMP=""
   fi
}

# variables
ROOT=`expr "$0" : '\(.*\)/'`
cd $ROOT
ROOT="."
CURRENT=`pwd`
CONFIG_DIR=$ROOT"/conf"
TMP_DIR=$ROOT"/tmp"

# interprete parameters
PROJECTS=""
QUIET="no"
HEADLESS="false"
while getopts ":hqgt:" flag
do
   case $flag in
      q) QUIET="yes"
         ;;
      t) PROJECTS=$OPTARG
         ;;
      g) HEADLESS="true"
         ;;
      h) usage
         exit 0
         ;;
      *) usage
         exit 1
         ;;
   esac
done

# all projects?
if [ "$PROJECTS" = "" ]
then
   PROJECTS=`ls $CONFIG_DIR/*.conf | sed s/"\.conf\|.*\/"//g`
fi

# quiet?
if [ "$QUIET" = "no" ]
   then
   echo
   echo "Testing the following project(s):"
   echo "$PROJECTS"
   echo
   echo "Press Enter to start..."
   read
fi

# test all projects
echo "Testing:"
for i in $PROJECTS
do
   echo "- $i"
   
   # variables
   CONFIG=$CONFIG_DIR"/$i.conf"
   TMP="active";     get_property; ACTIVE=$TMP
   TMP="tag";        get_property; TAG=$TMP
   TMP="smtp_host";  get_property; SMTP_HOST=$TMP
   TMP="sender";     get_property; SENDER=$TMP
   TMP="recipients"; get_property; RECIPIENTS=$TMP
   TMP="build";      get_property; BUILD=$TMP
   TMP="target";     get_property; TARGET=$TMP
   TMP="lib";        get_property; LIB=$TMP
   TMP="references"; get_property; REFERENCES=$TMP
   TMP="jdk"; get_property; JDK=$TMP

   # is the project active?
   if [ ! "$ACTIVE" = "yes" ]
   then
      echo "Project '$i' not active - skipped."
      continue
   fi

   # clean up
   rm -Rf $TMP_DIR
   mkdir $TMP_DIR

   # copy files
#   cp -R $REFERENCES $TMP_DIR/wekarefs
    cp $BUILD $TMP_DIR/build.xml

   # run tests
   CURRENT=`pwd`
   cd /home/ml/java
   . ./antSetup
   export JAVA_HOME=$JDK
   echo "Using $JAVA_HOME"
   export TZ="NZ"
   cd $CURRENT
   cd $TMP_DIR
   ant -lib $LIB -Dheadless=$HEADLESS -Dweka_branch=$TAG -Dmail_smtp_host=$SMTP_HOST -Dmail_sender=$SENDER -Dmail_recipients=$RECIPIENTS $TARGET
   cd $CURRENT
done

# Finished!
rm -Rf $TMP_DIR
echo
echo "Finished!"
echo


