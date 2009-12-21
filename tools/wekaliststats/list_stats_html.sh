#!/bin/bash
#
# This script generates mailman mailing lists statistics based on an archive
# and outputs them to as HTML and CSV.
#
# FracPete

# the usage of this script
function usage()
{
   echo 
   echo "usage: ${0##*/} -u <url> -o <dir> -n <name> [-s <file>] [-p <prefix>]"
   echo "                [-H <prefix>] [-h]"
   echo
   echo "Generates mailing list statistics based on mailman list archives"
   echo "generated with htdig and outputs them to HTML and CSV."
   echo "Uses $SCRIPT to generate the CSV file, which is the basis for the"
   echo "HTML output."
   echo 
   echo " -h   this help"
   echo " -u   <url>"
   echo "      the URL of the archive to retrieve"
   echo " -o   <dir>"
   echo "      the output directory"
   echo " -n   <name>"
   echo "      the name of the list"
   echo " -s   <file>"
   echo "      the name of the script to execute for generating the CSV stats"
   echo " -p   <prefix>"
   echo "      the name prefix for all files, currently: $PREFIX"
   echo " -H   <prefix>"
   echo "      the name prefix for the HTML page, currently: $PREFIX_HTML"
   echo 
}

# variables
ROOT=`expr "$0" : '\(.*\)/'`
URL=""
OUTPUT=""
NAME=""
PREFIX="stats"
PREFIX_HTML="index"
SCRIPT="list_stats.sh"

# interprete parameters
while getopts ":hu:o:n:s:p:H:" flag
do
   case $flag in
      u) URL=$OPTARG
         ;;
      o) OUTPUT=$OPTARG
         ;;
      n) NAME=$OPTARG
         ;;
      s) SCRIPT=$OPTARG
         ;;
      p) PREFIX=$OPTARG
         ;;
      H) PREFIX_HTML=$OPTARG
         ;;
      h) usage
         exit 0
         ;;
      *) usage
         exit 1
         ;;
   esac
done

# everything provided?
if [ "$URL" = "" ]
then
  echo
  echo "ERROR: no URL provided!"
  echo
  usage
  exit 2
fi

if [ "$OUTPUT" = "" ] || [ ! -d "$OUTPUT" ]
then
  echo
  echo "ERROR: no or invalid output directory provided!"
  echo
  usage
  exit 3
fi

if [ "$NAME" = "" ]
then
  echo
  echo "ERROR: no mailing list name provided!"
  echo
  usage
  exit 4
fi

if [ ! -x "$SCRIPT" ]
then
  echo
  echo "ERROR: no script provided or not executable!"
  echo
  usage
  exit 5
fi

# dependent variables
GP_DATA="$OUTPUT/gnuplot.data"
GP_SCRIPT="$OUTPUT/gnuplot.script"

# generate CSV
CSV="$OUTPUT/$PREFIX.csv"
$SCRIPT -u "$URL" -o "$CSV"

# gnuplot data
tac "$CSV" | grep -v "Year" | cut -f3,4 -d"," | grep -n ".*" | sed s/"[:,]"/"\t"/g > $GP_DATA

# gnuplot script
# - generate xtics
TMP=`tac $CSV | grep -v "Year" | cut -f1,2 -d"," | sed s/"\(,\)\([a-zA-Z][a-z][a-z]\)\([a-z]*\)"/"-\2"/g | grep -n ".*" | sed s/"Feb\|Mar\|Apr\|May\|Jun\|Aug\|Sep\|Oct\|Nov\|Dec"/"xx"/g | sed s/"\([0-9]*\):\(.*\)"/"\"\2\" \1, "/g`
TMP=`echo $TMP | sed s/",$"//g | sed s/"[0-9]*-xx"//g`
XTICS=`echo "set xtics ($TMP)"`

# - output script
echo "# generated: `date`" > $GP_SCRIPT
# the following line sets the mulitpliers for the x and y axis
# increase them as time progresses
echo "set size 2,0.6" >> $GP_SCRIPT
echo "set terminal png" >> $GP_SCRIPT
echo "set output \"$OUTPUT/$PREFIX.png\"" >> $GP_SCRIPT
echo "$XTICS" >> $GP_SCRIPT
echo "plot \"$GP_DATA\" using 1:2 title \"Size in KB\" with lines, \\" >> $GP_SCRIPT
echo "     \"$GP_DATA\" using 1:3 title \"Number of posts\" with lines" >> $GP_SCRIPT

# generate image
gnuplot $GP_SCRIPT
rm $GP_DATA
rm $GP_SCRIPT

# generate HTML
HTML="$OUTPUT/$PREFIX_HTML.html"
echo "<!-- generated: `date` -->" > $HTML
echo "<html>" >> $HTML
echo "<head>" >> $HTML
echo "<title>$NAME - Statistics</title>" >> $HTML
echo "<link rel=\"shortcut icon\" href=\"favicon.ico\" />" >> $HTML
echo "<link rel=\"stylesheet\" href=\"http://cs.waikato.ac.nz/ml/main.css\"/>" >> $HTML
echo "</head>" >> $HTML
echo "<body>" >> $HTML
echo "<div align=\"center\">" >> $HTML
echo "<h3>$NAME - Statistics</h3>" >> $HTML
echo "<table cellpadding=\"10\">" >> $HTML
echo "<tr><td>Generated</td><td>`date`</td></tr>" >> $HTML
echo "<tr><td>CSV file of statistics</td><td><a href=\"$PREFIX.csv\">$PREFIX.csv</a></td></tr>" >> $HTML
echo "<tr><td valign=\"top\">Graph</td><td><a href=\"$PREFIX.png\" title=\"Click for full-size image\"><img src=\"$PREFIX.png\" width=\"400\"/></a></td></tr>" >> $HTML
echo "</table>" >> $HTML
echo "</div>" >> $HTML
echo "</body>" >> $HTML
echo "</html>" >> $HTML
