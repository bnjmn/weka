#!/bin/bash
#
# This script generates mailman mailing lists statistics based on an archive
# and outputs them to a CSV file.
#
# FracPete

# the usage of this script
function usage()
{
   echo 
   echo "usage: ${0##*/} -u <url> -o <file> [-h]"
   echo
   echo "Generates mailing list statistics based on mailman list archives"
   echo "generated with htdig and outputs them to a CSV file."
   echo 
   echo " -h   this help"
   echo " -u   <url>"
   echo "      the URL of the archive to retrieve"
   echo " -o   <file>"
   echo "      the output CSV file"
   echo 
}

# variables
ROOT=`expr "$0" : '\(.*\)/'`
URL=""
OUTPUT=""

# interprete parameters
while getopts ":hu:o:" flag
do
   case $flag in
      u) URL=$OPTARG
         ;;
      o) OUTPUT=$OPTARG
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

if [ "$OUTPUT" = "" ]
then
  echo
  echo "ERROR: no output file provided!"
  echo
  usage
  exit 3
fi

# setup wget
WGET="wget --quiet"
if [ "`echo $URL | sed s/":.*"//g`" = "https" ]
then
  # does wget version support --no-check-certificate option?
  if [ `wget --no-check-certificate 2>&1| grep unrecognized | wc -l` -eq 0 ]
  then
    WGET="$WGET --no-check-certificate"
  fi
fi

# obtain index.html
INDEX="$OUTPUT.tmp.index"
$WGET "--output-document=$INDEX" "$URL"

# get year-months
YEAR_MONTHS=`cat "$INDEX" | grep "\[ Text" | sed s/".*href=\""//g | sed s/"\.txt.*"//g`

# generate CSV file
echo "Year,Month,\"Size in KB\",Posts" > "$OUTPUT"
for ym in $YEAR_MONTHS
do
  echo -n "."
  YEAR=`echo $ym | sed s/"-.*"//g`
  MONTH=`echo $ym | sed s/".*-"//g`
  SIZE=`cat "$INDEX" | grep "$ym" | grep "\[ Text" | sed s/".*\[ Text "//g | sed s/" KB.*"//g`

  POSTS_INDEX=$OUTPUT.tmp.posts
  POSTS_URL="$URL/$ym/date.html"
  $WGET --output-document=$POSTS_INDEX $POSTS_URL 
  POSTS=`cat $POSTS_INDEX | grep "<A NAME=" | wc -l`

  echo "$YEAR,$MONTH,$SIZE,$POSTS" >> "$OUTPUT"
done
echo

# clean up
rm -f "$OUTPUT".tmp.*

