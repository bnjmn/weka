#!/bin/bash
#
# a script that generates snapshot releases from certain Weka tags and
# updates the ML directory
#
# FracPete, 2006-11-30

# the usage of this script
function usage()
{
   echo 
   echo "usage: ${0##*/} [-t <dir>] [-o <dir>] [-c <svnurl>] [-q]"
   echo "       [-4 <dir>] [-5 <dir>] [-H <file>] [-a <email>] [-h]"
   echo
   echo "Generates snapshot-releases of Weka."
   echo 
   echo " -h   this help"
   echo " -t   <dir>"
   echo "      the temp directory"
   echo "      currently: $TEMP_DIR"
   echo " -o   <dir>"
   echo "      the output directory for the snapshots"
   echo "      currently: $OUTPUT_DIR"
   echo " -c   <svnurl>"
   echo "      the subversion url string to use for getting the sources"
   echo "      currently: $SVN"
   echo " -H   <file>"
   echo "      the HTML template to use"
   echo " -a   <email>"
   echo "      the email address of the admin, to which the log files get sent"
   echo "      currently: $ADMIN"
   echo " -q   starts immediately ('quiet mode')"
   echo 
}


# counts the lines of the branch file and returns the result in TMP
function count_branches()
{
   TMP=`cat $CONF | grep -v "^$\|^#" | wc -l | sed s/" "*//g`
}

# returns the specified branch (in line LINE and column COL) from the branch 
# file in TMP
function get_value()
{
   TMP=`cat $CONF | grep -v "^$\|^#" | head -n $LINE | tail -n 1 | cut -f$COL`
}

ROOT=`expr "$0" : '\(.*\)/'`
CONF=`echo $0 | sed s/"\.sh"/".conf"/g`
QUIET="no"
#CVS=":pserver:cvs_anon@cvs.scms.waikato.ac.nz:/usr/local/global-cvs/ml_cvs"
SVN="https://svn.scms.waikato.ac.nz/svn/weka"
OUTPUT_DIR="$ROOT/output"
TEMP_DIR="$ROOT/temp"
HTML="`echo $0 | sed s/"\.sh$"/".html"/g`"
MAILRC="`echo $0 | sed s/"\.sh$"/".muttrc"/g`"
SENDMAIL="/usr/bin/mutt -F $MAILRC"
ADMIN="mhall@cs.waikato.ac.nz"

# interprete parameters
while getopts ":ht:o:c:H:a:q" flag
do
   case $flag in
      t) TEMP_DIR=$OPTARG
         ;;
      o) OUTPUT_DIR=$OPTARG
         ;;
      c) SVN=$OPTARG
         ;;
      H) HTML=$OPTARG
         ;;
      a) ADMIN=$OPTARG
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

START_TIME=`date`
echo
echo "Start: $START_TIME"
echo

# setup directories
mkdir -p $TEMP_DIR
mkdir -p $OUTPUT_DIR

# traverse tags
count_branches;COUNT=$TMP
for ((i=1; i<=$COUNT; i++))
do
   # get values from conf file
   LINE=$i
   COL="1";get_value;BRANCH=$TMP
   COL="2";get_value;JDK=$TMP
   COL="3";get_value;START_DATE=$TMP

   # log file
   LOG="`echo $0 | sed s/"\.sh"//g`-$BRANCH.log"
   rm -f $LOG

   $ROOT/weka_snapshot.sh -c "$SVN" -j "$JDK" -b $BRANCH -d "$START_DATE" -q -t "$TEMP_DIR" -o "$OUTPUT_DIR" | tee $LOG
done

END_TIME=`date`
echo
echo "End: $END_TIME"
echo

# generate HTML
if [ -f "$HTML" ]
then
   OUT_HTML="$OUTPUT_DIR/`echo $HTML | sed s/".*\/"//g`"

   # split template
   csplit --quiet --prefix=$ROOT/xx $HTML /"<\!-- downloads -->"/+1

   # footer
   cat $ROOT/xx00 > $OUT_HTML

   # files
   echo "<table>" >> $OUT_HTML
   for i in $OUTPUT_DIR/*.zip
   do
      FILE=`echo $i | sed s/".*\/"//g`
      SIZE=`du -b $i | cut -f1 | awk 'BEGIN{FS=OFS=""}{for (i=1;i<NF;i++) if (!((NF-i)%3)) $i=$i","}1'`
      echo "<tr>" >> $OUT_HTML
      echo "<td><a href=\"$FILE\">$FILE</a> ($SIZE Bytes)</td>" >> $OUT_HTML
      echo "</tr>" >> $OUT_HTML
   done
   echo "<tr><td>&nbsp;</td></tr>" >> $OUT_HTML
   echo "<tr><td>Generation: $END_TIME</td></tr>" >> $OUT_HTML
   echo "</table>" >> $OUT_HTML
   
   # footer
   cat $ROOT/xx01 >> $OUT_HTML
   
   # clean up
   rm $ROOT/xx*
fi

# mail log file(s)
ZIP="$ROOT/weka_snapshots.zip"
zip $ZIP $ROOT/*.log
SENDMAIL_FILE="$ROOT/weka_snapshots.eml"
echo -e "Snapshot logs\nStart: $START_TIME\nEnd: $END_TIME" > $SENDMAIL_FILE
echo "my_hdr Reply-To: $ADMIN" > $MAILRC
echo -e "\n" | $SENDMAIL -x -s "Weka snapshots" -i "$SENDMAIL_FILE" -a "$ZIP" "$ADMIN"
rm $ZIP
rm $ROOT/*.log
rm $SENDMAIL_FILE

# clean up
rm -fR $TEMP_DIR
rm -f $MAILRC

