#!/bin/bash
#
# a script that generates snapshot releases from a certain Weka tag
#
# FracPete, 2006-11-30
# mhall, 2008-10-02

# the usage of this script
function usage()
{
   echo 
   echo "usage: ${0##*/} -b <branch> -d <date> [-t <dir>] [-o <dir>]"
   echo "       [-c <svnurl>] [-j <dir>] [-q] [-h]"
   echo
   echo "Generates a snapshot-release of Weka."
   echo 
   echo " -h   this help"
   echo " -b   <branch>"
   echo "      subversion branch to create snapshot from (e.g. 'trunk', 'branches/book2ndEd_branch')"
   echo " -d   <date>"
   echo "      start date for changelog, e.g., '2008-07-16'"
   echo " -t   <dir>"
   echo "      the temp directory"
   echo "      currently: $TEMP_DIR"
   echo " -o   <dir>"
   echo "      the output directory for the snapshots"
   echo "      currently: $OUTPUT_DIR"
   echo " -c   <svnurl>"
   echo "      the subversion url string to use for getting the sources"
   echo "      currently: $SVN"
   echo " -j   <dir>"
   echo "      the Java JDK directory"
   echo "      currently: $JAVA"
   echo " -q   starts immediately ('quiet mode')"
   echo 
}

ROOT=`expr "$0" : '\(.*\)/'`
BRANCH=""
DATE=""
TODAY=`date "+%F"`
QUIET="no"
#CVS=":pserver:cvs_anon@cvs.scms.waikato.ac.nz:/usr/local/global-cvs/ml_cvs"
SVN="https://svn.scms.waikato.ac.nz/svn/weka"
OUTPUT_DIR="$ROOT/output"
TEMP_DIR="$ROOT/temp"
JAVA="/home/ml/jdk/jdk1.5.0_22"

# interprete parameters
while getopts ":hb:d:t:o:j:c:q" flag
do
   case $flag in
      b) BRANCH=$OPTARG
         ;;
      d) DATE=$OPTARG
         ;;
      t) TEMP_DIR=$OPTARG
         ;;
      o) OUTPUT_DIR=$OPTARG
         ;;
      j) JAVA=$OPTARG
         ;;
      c) SVN=$OPTARG
         ;;
      q) QUIET="yes"
         ;;
      h) usage
         exit 0
         ;;
      *) usage
         exit 1
         ;;
   esac
done

# ask user?
if [ "$QUIET" = "no" ]
then
   echo
   echo "Start snapshot generation with <Enter>, abort with <Ctrl+C>..."
   read
   echo
fi

echo "--> Started..."

# setup directories
FINISHED_DIR="$TEMP_DIR/finished"
PREFIX=`echo $ROOT | sed s/"\/.*"//g`
if [ ! "$PREFIX" = "" ]
then
   CURRENT="`pwd`/$ROOT"
else
   CURRENT="$ROOT"
fi
mkdir -p $TEMP_DIR
mkdir -p $OUTPUT_DIR

# setup ant
cd /home/ml/java
. ./antSetup
cd $CURRENT

# setup java
JAVA_HOME=$JAVA
JDK_HOME=$JAVA_HOME
export JAVA_HOME
export JDK_HOME

echo "Working on branch '$BRANCH'..."

# clean up temp output directory for release
rm -Rf $TEMP_DIR/*
mkdir -p $FINISHED_DIR/weka

# create ant file
ANT_FILE=$TEMP_DIR/build_temp.xml
rm -f $ANT_FILE
echo "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" >> $ANT_FILE
echo "" >> $ANT_FILE
echo "<project name=\"weka-snapshots\" default=\"svn\" basedir=\".\">" >> $ANT_FILE
echo "" >> $ANT_FILE
echo "  <target name=\"svn\" depends=\"weka, wekadocs, wekaexamples\"/>" >> $ANT_FILE
echo "" >> $ANT_FILE
echo "  <target name=\"weka\">" >> $ANT_FILE
echo "" >> $ANT_FILE
echo "    <java classname=\"org.tmatesoft.svn.cli.SVN\" dir=\"./\" fork=\"true\">" >> $ANT_FILE
echo "      <arg value=\"co\"/>" >> $ANT_FILE
echo "      <arg value=\"$SVN/$BRANCH/weka\"/>" >> $ANT_FILE
echo "      <classpath>" >> $ANT_FILE
echo "        <pathelement location=\"\${ant.home}/lib/svnkit.jar\"/>" >> $ANT_FILE
echo "        <pathelement location=\"\${ant.home}/lib/svnkit-cli.jar\"/>" >> $ANT_FILE
echo "      </classpath>" >> $ANT_FILE
echo "    </java>" >> $ANT_FILE
echo "  </target>" >> $ANT_FILE

echo "" >> $ANT_FILE
echo "  <target name=\"wekadocs\">" >> $ANT_FILE
echo "" >> $ANT_FILE
echo "    <java classname=\"org.tmatesoft.svn.cli.SVN\" dir=\"./\" fork=\"true\">" >> $ANT_FILE
echo "      <arg value=\"co\"/>" >> $ANT_FILE
echo "      <arg value=\"$SVN/$BRANCH/wekadocs\"/>" >> $ANT_FILE
echo "      <classpath>" >> $ANT_FILE
echo "        <pathelement location=\"\${ant.home}/lib/svnkit.jar\"/>" >> $ANT_FILE
echo "        <pathelement location=\"\${ant.home}/lib/svnkit-cli.jar\"/>" >> $ANT_FILE
echo "      </classpath>" >> $ANT_FILE
echo "    </java>" >> $ANT_FILE
echo "  </target>" >> $ANT_FILE
echo "" >> $ANT_FILE

echo "" >> $ANT_FILE
echo "  <target name=\"wekaexamples\">" >> $ANT_FILE
echo "" >> $ANT_FILE
echo "    <java classname=\"org.tmatesoft.svn.cli.SVN\" dir=\"./\" fork=\"true\">" >> $ANT_FILE
echo "      <arg value=\"co\"/>" >> $ANT_FILE
echo "      <arg value=\"$SVN/$BRANCH/wekaexamples\"/>" >> $ANT_FILE
echo "      <classpath>" >> $ANT_FILE
echo "        <pathelement location=\"\${ant.home}/lib/svnkit.jar\"/>" >> $ANT_FILE
echo "        <pathelement location=\"\${ant.home}/lib/svnkit-cli.jar\"/>" >> $ANT_FILE
echo "      </classpath>" >> $ANT_FILE
echo "    </java>" >> $ANT_FILE
echo "  </target>" >> $ANT_FILE
echo "" >> $ANT_FILE

echo "  <target name=\"changelog\">" >> $ANT_FILE
echo "    <java classname=\"org.tmatesoft.svn.cli.SVN\" output=\"wekadocs/changelogs/CHANGELOG-current\" fork=\"true\">" >> $ANT_FILE
echo "      <arg value=\"log\"/>" >> $ANT_FILE
echo "      <arg value=\"-r\"/>" >> $ANT_FILE
echo "      <arg value=\"{$DATE}:{$TODAY}\"/>" >> $ANT_FILE
echo "      <arg value=\"-v\"/>" >> $ANT_FILE
echo "      <arg value=\"$SVN/$BRANCH/weka/src/main/java/weka\"/>" >> $ANT_FILE
echo "      <classpath>" >> $ANT_FILE
echo "        <pathelement location=\"\${ant.home}/lib/svnkit.jar\"/>" >> $ANT_FILE
echo "        <pathelement location=\"\${ant.home}/lib/svnkit-cli.jar\"/>" >> $ANT_FILE
echo "      </classpath>" >> $ANT_FILE
echo "    </java>" >> $ANT_FILE
echo "  </target>" >> $ANT_FILE
echo "</project>" >> $ANT_FILE

# check out 
echo "--> Performing checkout..."
cd $TEMP_DIR
ant -f build_temp.xml
cd $CURRENT

# compile source
echo "--> Compiling sources..."
TMP=$TEMP_DIR/weka
cd $TMP
ant exejar remotejar srcjar
cd $CURRENT
cp $TMP/dist/*.jar $FINISHED_DIR/weka
# include the junit.jar
cp -R $TMP/lib $FINISHED_DIR/weka
#include the build.xml
cp $TMP/build.xml $FINISHED_DIR/weka

# compile examples
echo "--> Compiling wekaexamples..."
TMP=$TEMP_DIR/wekaexamples
cp $TEMP_DIR/weka/dist/weka.jar $TMP/lib
if [ "$BRANCH" = "trunk" ]
then
    # copy the LibSVM.jar
    echo "---- copying LibSVM.jar -----"
   cp $TEMP_DIR/../lib/*.jar $TMP/lib
fi
cd $TMP
ant dist
ant docs
rm -rf build
rm lib/weka.jar
rm dist/weka.jar
find . -name .svn -type d -exec rm -Rf {} \;
cd ../
zip -r wekaexamples.zip wekaexamples
cd $CURRENT
cp $TEMP_DIR/wekaexamples.zip $FINISHED_DIR

# generate documentation
# 1. Javadoc
echo "--> Generating documentation..."
TMP=$TEMP_DIR/weka
cd $TMP
ant docs
cd $CURRENT
cp -R $TMP/doc $FINISHED_DIR
cd $CURRENT

if [ "$BRANCH" = "branches/book2ndEd-branch" ]
then
# 2. bayesnet
TMP=$TEMP_DIR/wekadocs/bayesnet
if [ -d "$TMP" ]
then
   cd $TMP
   echo "--> bayesnet (1)..."
   latex bayesnet.tex
   echo "--> bayesnet (2)..."
   latex bayesnet.tex
   echo "--> bayesnet (pdf)..."
   dvipdf bayesnet.dvi
   cd $CURRENT
   cp $TMP/bayesnet.pdf $FINISHED_DIR/BayesianNetClassifiers.pdf
fi

# 3. Experimenter tutorial
TMP=$TEMP_DIR/wekadocs/experimentertutorial
cd $TMP
echo "--> experimenter tutorial (1)..."
latex tutorial.tex
echo "--> experimenter tutorial (2)..."
latex tutorial.tex
echo "--> experimenter tutorial (pdf)..."
dvipdf tutorial.dvi
cd $CURRENT
cp $TMP/tutorial.pdf $FINISHED_DIR/ExperimenterTutorial.pdf

# 4. Explorer guide
TMP=$TEMP_DIR/wekadocs/explorerguide
cd $TMP
echo "--> explorer guide (1)..."
latex guide.tex
echo "--> explorer guide (2)..."
latex guide.tex
echo "--> explorer guide (pdf)..."
dvipdf guide.dvi
cd $CURRENT
cp $TMP/guide.pdf $FINISHED_DIR/ExplorerGuide.pdf

# 5. knowledgeflow
TMP=$TEMP_DIR/wekadocs/knowledgeflowtutorial
if [ -d "$TMP" ]
then
   cd $TMP
   echo "--> knowledgeflow tutorial (1)..."
   latex tutorial.tex
   echo "--> knowledgeflow tutorial (2)..."
   latex tutorial.tex
   echo "--> knowledgeflow tutorial (pdf)..."
   dvipdf tutorial.dvi
   cd $CURRENT
   cp $TMP/tutorial.pdf $FINISHED_DIR/KnowledgeFlowTutorial.pdf
fi
else
# 2. manual
TMP=$TEMP_DIR/wekadocs/manual
cd $TMP
echo "--> weka manual (1)..."
latex manual.tex
echo "--> weka manual (2)..."
latex manual.tex
echo "--> weka manual (pdf)..."
dvipdf manual.dvi
cd $CURRENT
cp $TMP/manual.pdf $FINISHED_DIR/WekaManual.pdf
fi

# 6. Changelog
echo "--> Changelog..."
cd $TEMP_DIR
ant -f build_temp.xml changelog
cd $CURRENT

# misc
TMP=$TEMP_DIR/wekadocs
cp $TMP/Tutorial.pdf $FINISHED_DIR
cp $TMP/README* $FINISHED_DIR
cp $TMP/COPYING $FINISHED_DIR
cp -R $TMP/changelogs $FINISHED_DIR
cp -R $TMP/data $FINISHED_DIR
cp $TMP/documentation.* $FINISHED_DIR
cp $TMP/weka.* $FINISHED_DIR
find $FINISHED_DIR -name .svn -type d -exec rm -Rf {} \;

# zip up everything
echo "--> Packing..."
cd $FINISHED_DIR
if [ "$BRANCH" = "trunk" ]
then
   ZIP="developer-branch.zip"
else
   ZIP=`echo $BRANCH".zip" | sed 's/branches\///g'`
fi
zip -r $ZIP *
cd $CURRENT
mv $FINISHED_DIR/$ZIP $OUTPUT_DIR

# finished!
echo "--> Finished!"

