import weka.core.Utils
import weka.core.converters.ConverterUtils.DataSource

/**
 * Simple Groovy script to extract the attribute stats from a dataset.
 * Supports the following parameters:
 * -t dataset-filename
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 */

// get parameters
// 1. data
tmp = Utils.getOption('t', args)
if (tmp == '') throw new Exception('No dataset provided!')
dataset = DataSource.read(tmp)

// print stats
for (i = 0; i < dataset.numAttributes(); i++) {
  att   = dataset.attribute(i)
  stats = dataset.attributeStats(i)
  println "\n" + (i+1) + ". " + att.name()
  if (att.isNominal()) {
    println "Type: nominal"
    println "distinct: " + stats.distinctCount
    println "int: " + stats.intCount
    println "real: " + stats.realCount
    println "total: " + stats.totalCount
    println "unique: " + stats.uniqueCount
    println "label stats:"
    for (n = 0; n < stats.nominalCounts.length; n++) {
      println " - " + att.value(n) + ": " + stats.nominalCounts[n]
    }
  }
  else if (att.isNumeric()) {
    println "Type: numeric"
    println "distinct: " + stats.distinctCount
    println "int: " + stats.intCount
    println "real: " + stats.realCount
    println "total: " + stats.totalCount
    println "unique: " + stats.uniqueCount
    println "numeric stats:"
    println " - count: " + stats.numericStats.count
    println " - max: " + stats.numericStats.max
    println " - min: " + stats.numericStats.min
    println " - mean: " + stats.numericStats.mean
    println " - stdDev: " + stats.numericStats.stdDev
    println " - sum: " + stats.numericStats.sum
    println " - squmSq: " + stats.numericStats.sumSq
  }
  else {
    println "Unhandled attribute type: " + att.type()
  }
}
