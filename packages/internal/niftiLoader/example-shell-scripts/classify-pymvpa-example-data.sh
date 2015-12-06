#!/bin/sh

# Script has been tested on OS X El Capitan. Assumes that WEKA is in Java's CLASSPATH (e.g., weka.jar).
# Also, the WEKA packages niftiLoader, multiInstanceFilters and LibLINEAR need to be installed.
# Requires WEKA versions > 3.7.13.

echo "Downloading example data"
curl http://www.pymvpa.org/files/pymvpa_exampledata.tar.bz2 > pymvpa_exampledata.tar.bz2

echo "Expanding archive"
tar -xzf pymvpa_exampledata.tar.bz2

echo "Changing into directory that has been created"
cd pymvpa-exampledata

echo "Converting into ARFF"
java weka.Run .NIfTIFileLoader bold.nii.gz -mask mask.nii.gz -attributes attributes_literal.txt > bold.arff

echo "Removing all instances corresponding to rest condition"
java weka.Run .RemoveWithValues -C 1 -L 1 -H < bold.arff > bold.noRest.arff

echo "Adding Cartesian product of class and session ID as last attribute"
java weka.Run .CartesianProduct -R 1-2 < bold.noRest.arff > bold.noRest.CP.arff

echo "Removing session ID attribute"
java weka.Run .Remove -R 2 < bold.noRest.CP.arff > bold.noRest.CP.R.arff

echo "Converting into relational format by collecting one bag for each combination of class value and session ID"
java weka.Run .PropositionalToMultiInstance -c first -no-weights -B last < bold.noRest.CP.R.arff > bold.noRest.CP.R.PTM.arff

echo "Aggregating relational values (i.e., volumes in one bag) by computing the centroid for each bag"
java weka.Run .RELAGGS -S -disable-min -disable-max -disable-stdev -disable-sum < bold.noRest.CP.R.PTM.arff > bold.noRest.CP.R.PTM.A.arff

echo "Creating training set using first 10 sessions"
java weka.Run .RemoveWithValues -C 1 -L 11,12,23,24,35,36,47,48,59,60,71,72,83,84,95,96 < bold.noRest.CP.R.PTM.A.arff > bold.noRest.CP.R.PTM.A.train.arff

echo "Creating test set using remaining 2 sessions"
java weka.Run .RemoveWithValues -C 1 -L 11,12,23,24,35,36,47,48,59,60,71,72,83,84,95,96 -V < bold.noRest.CP.R.PTM.A.arff > bold.noRest.CP.R.PTM.A.test.arff

echo "Using LibLINEAR to build an SVM classification model on the training set, performing evaluation on the test set (bag indicator is removed)"
java weka.Run .FilteredClassifier -F ".Remove -R 1" -W .LibLINEAR -t bold.noRest.CP.R.PTM.A.train.arff -T bold.noRest.CP.R.PTM.A.test.arff -o -- -Z

echo "Changing back into original directory"
cd ..

echo "Deleting data and directory"
rm pymvpa_exampledata.tar.bz2
rm -rf pymvpa-exampledata
