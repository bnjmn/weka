for f in `find weka -name "*.java"`
do
   cat weka/scripts/webMind.comment $f >$f.new
   mv $f.new $f
done
