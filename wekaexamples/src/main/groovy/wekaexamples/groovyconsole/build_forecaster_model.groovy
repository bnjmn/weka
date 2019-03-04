// From Eibe's Wekalist post:
// https://list.waikato.ac.nz/pipermail/wekalist/2016-December/041976.html

data = weka.core.converters.ConverterUtils.DataSource.read("/Users/eibe/IdeaProjects/Weka/trunk/wekadocs/data/airline.arff")
forecaster = new weka.classifiers.timeseries.WekaForecaster();
forecaster.setOptions(weka.core.Utils.splitOptions("-F passenger_numbers -G Date -quarter -month -W \"weka.classifiers.meta.AttributeSelectedClassifier -E \\\"weka.attributeSelection.WrapperSubsetEval -B weka.classifiers.functions.RBFRegressor -F 5 -T 0.01 -R 1 -E DEFAULT -- -N 2 -R 0.01 -L 1.0E-6 -C 2 -P 1 -E 1 -S 1\\\" -W weka.classifiers.functions.RBFRegressor\" -prime 12"))
forecaster.buildForecaster(data, System.out)
println(forecaster)
