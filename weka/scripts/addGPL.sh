for f in `find -name "*.java"`
do
   cat weka/scripts/gpl.comment $f >$f.new
   mv $f.new $f
done
