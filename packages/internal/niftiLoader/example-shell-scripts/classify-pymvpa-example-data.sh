#!/bin/bash

# A script to replicate the 12-fold leave-one-run-out cross-validation
# experiment from http://www.pymvpa.org/tutorial_classifiers.html, but
# without the domain-specific detrending and normalisation steps
# described at http://www.pymvpa.org/tutorial_mappers.html#basic-preprocessing.
# Instead, standard WEKA normalization is applied before the linear
# SVM is built (-Z flag for LibLINEAR). Average accuracy across the
# 12-folds obtained using this script is 80.21%, slightly better than
# the 78.13% reported at the above URL.
#
# The script has been tested on OS X El Capitan. It assumes that WEKA
# is in Java's CLASSPATH (e.g., weka.jar).  Also, the WEKA packages
# niftiLoader, multiInstanceFilters and LibLINEAR need to be
# installed using the WEKA package manager. The script requires WEKA versions > 3.7.13.

echo "Downloading example data from http://data.pymvpa.org/datasets/tutorial_data/tutorial_data-0.2.tar.gz"
curl http://data.pymvpa.org/datasets/tutorial_data/tutorial_data-0.2.tar.gz > tutorial_data-0.2.tar.gz

echo "Expanding archive"
tar -xzf tutorial_data-0.2.tar.gz

echo "Changing into the data directory that has been created"
cd tutorial_data/data

echo "Converting into ARFF"
java weka.Run .NIfTIFileLoader bold.nii.gz -mask mask_vt.nii.gz -attributes attributes.txt > bold.arff

echo "Removing all instances corresponding to rest condition"
java weka.Run .RemoveWithValues -C 1 -L 1 -H < bold.arff > bold.noRest.arff

echo "Adding Cartesian product of class and run ID as last attribute"
java weka.Run .CartesianProduct -R 1-2 < bold.noRest.arff > bold.noRest.CP.arff

echo "Removing run ID attribute"
java weka.Run .Remove -R 2 < bold.noRest.CP.arff > bold.noRest.CP.R.arff

echo "Converting into relational format by collecting one bag for each combination of class value and run ID"
java weka.Run .PropositionalToMultiInstance -c first -no-weights -B last < bold.noRest.CP.R.arff > bold.noRest.CP.R.PTM.arff

echo "Aggregating relational values (i.e., volumes in one bag) by computing the centroid for each bag"
java weka.Run .RELAGGS -S -disable-min -disable-max -disable-stdev -disable-sum < bold.noRest.CP.R.PTM.arff > bold.noRest.CP.R.PTM.A.arff

echo "Running 12-fold cross-validation, leaving out each run in turn"
for run in 1 2 3 4 5 6 7 8 9 10 11 12;
do
    echo "Creating training set for run $run" #order of values: scissors_x_0,...,scissors_x_11,face_x_0,...,face_x_11,cat_x_0,...,cat_x_11,shoe_x_0,...,shoe_x_11,house_x_0,...,house_x_11,scrambledpix_x_0,...,scrambledpix_x_11,bottle_x_0,...,bottle_x_11,chair_x_0,...,chair_x_11
    java weka.Run .RemoveWithValues -C 1 -L $run,$(($run+12)),$(($run+24)),$(($run+36)),$(($run+48)),$(($run+60)),$(($run+72)),$(($run+84)) < bold.noRest.CP.R.PTM.A.arff > bold.noRest.CP.R.PTM.A.train."$run".arff

    echo "Creating test set for run $run" #order of values: scissors_x_0,...,scissors_x_11,face_x_0,...,face_x_11,cat_x_0,...,cat_x_11,shoe_x_0,...,shoe_x_11,house_x_0,...,house_x_11,scrambledpix_x_0,...,scrambledpix_x_11,bottle_x_0,...,bottle_x_11,chair_x_0,...,chair_x_11
    java weka.Run .RemoveWithValues -C 1 -L $run,$(($run+12)),$(($run+24)),$(($run+36)),$(($run+48)),$(($run+60)),$(($run+72)),$(($run+84)) -V < bold.noRest.CP.R.PTM.A.arff > bold.noRest.CP.R.PTM.A.test."$run".arff

    echo "Using LibLINEAR to build an SVM classification model on the training set, performing evaluation on the test set (bag indicator is removed)"
    java weka.Run .FilteredClassifier -F ".Remove -R 1" -W .LibLINEAR -t bold.noRest.CP.R.PTM.A.train."$run".arff -T bold.noRest.CP.R.PTM.A.test."$run".arff -o -v -- -Z | grep "Correctly Classified Instances"
done

echo "Changing back into original directory"
cd ../..

echo "Deleting data and directory"
rm tutorial_data-0.2.tar.gz
rm -rf tutorial_data
