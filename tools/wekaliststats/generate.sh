#!/bin/bash
#
# generates some statistics of the Wekalist and makes them available on the
# Weka homepage.
#
# FracPete, 2009-01-20

ROOT=`expr "$0" : '\(.*\)/'`
URL="https://list.scms.waikato.ac.nz/mailman/htdig/wekalist"
OUT_DIR="/home/ml/public_html/weka/wekaliststats"
$ROOT/list_stats_html.sh -u $URL -n Wekalist -o $OUT_DIR -s $ROOT/list_stats.sh -p "stats" -H "index"

