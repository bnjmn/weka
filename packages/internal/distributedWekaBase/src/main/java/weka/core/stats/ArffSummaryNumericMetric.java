package weka.core.stats;

import weka.core.Attribute;
import weka.core.Utils;

/**
 * An enumerated utility type for the various numeric summary metrics that are
 * computed.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 */
public enum ArffSummaryNumericMetric {
  COUNT("count") {
    @Override
    public double valueFromAttribute(Attribute att) {
      String value = att.value(COUNT.ordinal());
      return toValue(value, toString());
    }

    @Override
    public String makeAttributeValue(double value) {
      return toString() + value;
    }
  },
  SUM("sum") {
    @Override
    public double valueFromAttribute(Attribute att) {
      String value = att.value(SUM.ordinal());
      return toValue(value, toString());
    }

    @Override
    public String makeAttributeValue(double value) {
      return toString() + value;
    }
  },
  SUMSQ("sumSq") {
    @Override
    public double valueFromAttribute(Attribute att) {
      String value = att.value(SUMSQ.ordinal());
      return toValue(value, toString());
    }

    @Override
    public String makeAttributeValue(double value) {
      return toString() + value;
    }
  },
  MIN("min") {
    @Override
    public double valueFromAttribute(Attribute att) {
      String value = att.value(MIN.ordinal());
      return toValue(value, toString());
    }

    @Override
    public String makeAttributeValue(double value) {
      return toString() + value;
    }
  },
  MAX("max") {
    @Override
    public double valueFromAttribute(Attribute att) {
      String value = att.value(MAX.ordinal());
      return toValue(value, toString());
    }

    @Override
    public String makeAttributeValue(double value) {
      return toString() + value;
    }
  },
  MISSING("missing") {
    @Override
    public double valueFromAttribute(Attribute att) {
      String value = att.value(MISSING.ordinal());
      return toValue(value, toString());
    }

    @Override
    public String makeAttributeValue(double value) {
      return toString() + value;
    }
  },
  MEAN("mean") {
    @Override
    public double valueFromAttribute(Attribute att) {
      String value = att.value(MEAN.ordinal());
      return toValue(value, toString());
    }

    @Override
    public String makeAttributeValue(double value) {
      return toString() + value;
    }
  },
  STDDEV("stdDev") {
    @Override
    public double valueFromAttribute(Attribute att) {
      String value = att.value(STDDEV.ordinal());
      return toValue(value, toString());
    }

    @Override
    public String makeAttributeValue(double value) {
      return toString() + value;
    }
  },
  FIRSTQUARTILE("firstQuartile") {
    @Override
    public double valueFromAttribute(Attribute att) {
      if (FIRSTQUARTILE.ordinal() > att.numValues() - 1) {
        return Utils.missingValue();
      }
      String value = att.value(FIRSTQUARTILE.ordinal());
      return toValue(value, toString());
    }

    @Override
    public String makeAttributeValue(double value) {
      return toString() + value;
    }
  },
  MEDIAN("median") {
    @Override
    public double valueFromAttribute(Attribute att) {
      if (MEDIAN.ordinal() > att.numValues() - 1) {
        return Utils.missingValue();
      }
      String value = att.value(MEDIAN.ordinal());
      return toValue(value, toString());
    }

    @Override
    public String makeAttributeValue(double value) {
      return toString() + value;
    }
  },
  THIRDQUARTILE("thirdQuartile") {
    @Override
    public double valueFromAttribute(Attribute att) {
      if (THIRDQUARTILE.ordinal() > att.numValues() - 1) {
        return Utils.missingValue();
      }
      String value = att.value(THIRDQUARTILE.ordinal());
      return toValue(value, toString());
    }

    @Override
    public String makeAttributeValue(double value) {
      return toString() + value;
    }
  };

  private final String m_name;

  ArffSummaryNumericMetric(String name) {
    m_name = name + "_";
  }

  /**
   * Extracts the value of this particular metric from the summary Attribute
   * 
   * @param att the summary attribute to extract the metric from
   * @return the value of this particular metric
   */
  public abstract double valueFromAttribute(Attribute att);

  /**
   * Makes the internal encoded version of this metric given it's value as a
   * double
   * 
   * @param value the value of the metric
   * @return the internal representation of this metric
   */
  public abstract String makeAttributeValue(double value);

  @Override
  public String toString() {
    return m_name;
  }

  /**
   * Extracts the value of the metric from the string representation
   * 
   * @param v the string representation
   * @param name the name of the attribute that the metric belongs to
   * @return the value of the metric
   */
  public double toValue(String v, String name) {
    v = v.replace(name, "");

    return Double.parseDouble(v);
  }
}
