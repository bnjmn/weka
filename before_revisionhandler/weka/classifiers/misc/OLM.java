/*
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

/*
 *    OLM.java
 *    Copyright (C) 2004 Stijn Lievens
 *
 */

package weka.classifiers.misc;

import weka.classifiers.RandomizableClassifier;
import weka.classifiers.misc.monotone.Coordinates;
import weka.classifiers.misc.monotone.DiscreteDistribution;
import weka.classifiers.misc.monotone.EnumerationIterator;
import weka.classifiers.misc.monotone.InstancesComparator;
import weka.classifiers.misc.monotone.InstancesUtil;
import weka.classifiers.misc.monotone.MultiDimensionalSort;
import weka.core.Attribute;
import weka.core.Capabilities;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.SelectedTag;
import weka.core.Tag;
import weka.core.TechnicalInformation;
import weka.core.TechnicalInformationHandler;
import weka.core.Utils;
import weka.core.Capabilities.Capability;
import weka.core.TechnicalInformation.Field;
import weka.core.TechnicalInformation.Type;
import weka.estimators.DiscreteEstimator;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Vector;

/**
 <!-- globalinfo-start -->
 * This class is an implementation of the Ordinal Learning Method<br/>
 * Further information regarding the algorithm and variants can be found in:<br/>
 * <br/>
 * Arie Ben-David (1992). Automatic Generation of Symbolic Multiattribute Ordinal Knowledge-Based DSSs: methodology and Applications. Decision Sciences. 23:1357-1372.<br/>
 * <br/>
 * Lievens, Stijn (2003-2004). Studie en implementatie van instantie-gebaseerde algoritmen voor gesuperviseerd rangschikken..
 * <p/>
 <!-- globalinfo-end -->
 *
 <!-- technical-bibtex-start -->
 * BibTeX:
 * <pre>
 * &#64;article{Ben-David1992,
 *    author = {Arie Ben-David},
 *    journal = {Decision Sciences},
 *    pages = {1357-1372},
 *    title = {Automatic Generation of Symbolic Multiattribute Ordinal Knowledge-Based DSSs: methodology and Applications},
 *    volume = {23},
 *    year = {1992}
 * }
 * 
 * &#64;mastersthesis{Lievens2003-2004,
 *    author = {Lievens, Stijn},
 *    school = {Ghent University},
 *    title = {Studie en implementatie van instantie-gebaseerde algoritmen voor gesuperviseerd rangschikken.},
 *    year = {2003-2004}
 * }
 * </pre>
 * <p/>
 <!-- technical-bibtex-end -->
 * 
 <!-- options-start -->
 * Valid options are: <p/>
 * 
 * <pre> -S &lt;num&gt;
 *  Random number seed.
 *  (default 1)</pre>
 * 
 * <pre> -D
 *  If set, classifier is run in debug mode and
 *  may output additional info to the console</pre>
 * 
 * <pre> -C &lt;CL|REG&gt;
 *  Sets the classification type to be used.
 *  (Default: REG)</pre>
 * 
 * <pre> -A &lt;MEAN|MED|MAX&gt;
 *  Sets the averaging type used in phase 1 of the classifier.
 *  (Default: MEAN)</pre>
 * 
 * <pre> -N &lt;NONE|EUCL|HAM&gt;
 *  If different from NONE, a nearest neighbour rule is fired when the
 *  rule base doesn't contain an example smaller than the instance
 *  to be classified
 *  (Default: NONE).</pre>
 * 
 * <pre> -E &lt;MIN|MAX|BOTH&gt;
 *  Sets the extension type, i.e. the rule base to use.
 *  (Default: MIN)</pre>
 * 
 * <pre> -sort
 *  If set, the instances are also sorted within the same class
 *  before building the rule bases</pre>
 * 
 <!-- options-end -->
 *
 * @author Stijn Lievens (stijn.lievens@ugent.be)
 * @version $Revision: 1.1 $
 */
public class OLM
  extends RandomizableClassifier 
  implements TechnicalInformationHandler {

  /** for serialization */
  private static final long serialVersionUID = 3722951802290935192L;

  /**
   * Round the real value that is returned by the original algorithm 
   * to the nearest label.
   */
  public static final int CT_ROUNDED = 0;

  /**
   * No rounding is performed during classification, this is the
   * classification is done in a regression like way.
   */
  public static final int CT_REAL = 1;

  /** the classification types */
  public static final Tag[] TAGS_CLASSIFICATIONTYPES = {
    new Tag(CT_ROUNDED, "CL", "Round to nearest label"),
    new Tag(CT_REAL, "REG", "Regression-like classification")
  };

  /**
   * Use the mean for averaging in phase 1.  This is in fact a 
   * non ordinal procedure.  The scores used for averaging are the internal
   * values of WEKA.
   */
  public static final int AT_MEAN = 0; 

  /**
   * Use the median for averaging in phase 1.  The possible values
   * are in the extended set of labels, this is labels in between the
   * original labels are possible.
   */
  public static final int AT_MEDIAN = 1;

  /**
   * Use the mode for averaging in phase 1.  The label
   * that has maximum frequency is used.  If there is more 
   * than one label that has maximum frequency, the lowest 
   * one is prefered.
   */
  public static final int AT_MAXPROB = 2;

  /** the averaging types */
  public static final Tag[] TAGS_AVERAGINGTYPES = {
    new Tag(AT_MEAN, "MEAN", "Mean"),
    new Tag(AT_MEDIAN, "MED","Median"),
    new Tag(AT_MAXPROB, "MAX", "Max probability")
  };

  /** 
   * No nearest neighbour rule will be fired when 
   * classifying an instance for which there  is no smaller rule 
   * in the rule base?
   */
  public static final int DT_NONE = -1;

  /**
   * Use the Euclidian distance whenever a nearest neighbour 
   * rule is fired.
   */
  public static final int DT_EUCLID = 0;

  /** 
   * Use the Hamming distance, this is the  number of 
   * positions in which the instances differ, whenever a 
   * nearest neighbour rule is fired
   */
  public static final int DT_HAMMING = 1;

  /** the distance types */
  public static final Tag[] TAGS_DISTANCETYPES = {
    new Tag(DT_NONE, "NONE", "No nearest neighbor"),
    new Tag(DT_EUCLID, "EUCL", "Euclidean"),
    new Tag(DT_HAMMING, "HAM", "Hamming")
  };

  /**
   * Use only the minimal extension, as in the original algorithm 
   * of Ben-David.
   */
  public static final int ET_MIN = 0;

  /**
   * Use only the maximal extension.  In this case an algorithm
   * dual to the original one is performed.
   */
  public static final int ET_MAX = 1;

  /**
   * Combine both the minimal and maximal extension, and use the 
   * midpoint of the resulting interval as prediction.
   */
  public static final int ET_BOTH = 2;

  /** the mode types */
  public static final Tag[] TAGS_EXTENSIONTYPES = {
    new Tag(ET_MIN, "MIN", "Minimal extension"),
    new Tag(ET_MAX, "MAX", "Maximal extension"),
    new Tag(ET_BOTH, "BOTH", "Minimal and maximal extension")
  };

  /** 
   * The training examples, used temporarily.
   * m_train is cleared after the rule base is built.
   */
  private Instances m_train;

  /** Number of classes in the original m_train */
  private int m_numClasses;

  /** 
   * The rule base, should be consistent and contain no 
   * redundant rules.  This is the rule base as in the original
   * algorithm of Ben-David.
   */
  private Instances m_baseMin;

  /** 
   * This is a complentary rule base, using the maximal rather
   * than the minimal extension.
   */
  private Instances m_baseMax; 

  /** 
   * Map used in the method buildClassifier in order to quickly
   * gather all info needed for phase 1.  This is a map containing
   * (Coordinates, DiscreteEstimator)-pairs.
   */
  private Map m_estimatedDistributions;

  /** classification type */
  private int m_ctype = CT_REAL;

  /** averaging type */
  private int m_atype = AT_MEAN;

  /** distance type */
  private int m_dtype = DT_EUCLID;

  /** mode type */
  private int m_etype = ET_MIN;

  /** 
   * Should the instances be sorted such that minimal (resp. maximal)
   * elements (per class) are treated first when building m_baseMin 
   * (resp. m_baseMax).
   */
  private boolean m_sort = false;

  /**
   * Returns a string describing the classifier.
   * @return a description suitable for displaying in the 
   * explorer/experimenter gui
   */
  public String globalInfo() {
    return "This class is an implementation of the Ordinal Learning "
    + "Method\n" 
    + "Further information regarding the algorithm and variants "
    + "can be found in:\n\n"
    + getTechnicalInformation().toString();
  }

  /**
   * Returns default capabilities of the classifier.
   *
   * @return      the capabilities of this classifier
   */
  public Capabilities getCapabilities() {
    Capabilities result = super.getCapabilities();

    // attributes
    result.enable(Capability.NOMINAL_ATTRIBUTES);

    // class
    result.enable(Capability.NOMINAL_CLASS);
    result.enable(Capability.MISSING_CLASS_VALUES);

    // instances
    result.setMinimumNumberInstances(0);

    return result;
  }

  /**
   * Returns an instance of a TechnicalInformation object, containing 
   * detailed information about the technical background of this class,
   * e.g., paper reference or book this class is based on.
   * 
   * @return the technical information about this class
   */
  public TechnicalInformation getTechnicalInformation() {
    TechnicalInformation 	result;
    TechnicalInformation 	additional;

    result = new TechnicalInformation(Type.ARTICLE);
    result.setValue(Field.AUTHOR, "Arie Ben-David");
    result.setValue(Field.YEAR, "1992");
    result.setValue(Field.TITLE, "Automatic Generation of Symbolic Multiattribute Ordinal Knowledge-Based DSSs: methodology and Applications");
    result.setValue(Field.JOURNAL, "Decision Sciences");
    result.setValue(Field.PAGES, "1357-1372");
    result.setValue(Field.VOLUME, "23");
    
    additional = result.add(Type.MASTERSTHESIS);
    additional.setValue(Field.AUTHOR, "Lievens, Stijn");
    additional.setValue(Field.YEAR, "2003-2004");
    additional.setValue(Field.TITLE, "Studie en implementatie van instantie-gebaseerde algoritmen voor gesuperviseerd rangschikken.");
    additional.setValue(Field.SCHOOL, "Ghent University");

    return result;
  }

  /**
   * Returns the tip text for this property.
   *
   * @return tip text for this property suitable for 
   * displaying in the explorer/experimenter gui
   */
  public String classificationTypeTipText() {
    return "Sets the classification type.";
  }

  /**
   * Sets the classification type.
   *
   * @param value the classification type to be set.
   */
  public void setClassificationType(SelectedTag value) {
    if (value.getTags() == TAGS_CLASSIFICATIONTYPES)
      m_ctype = value.getSelectedTag().getID();
  }

  /**
   * Gets the classification type.
   *
   * @return the classification type
   */
  public SelectedTag getClassificationType() {
    return new SelectedTag(m_ctype, TAGS_CLASSIFICATIONTYPES);
  }

  /**
   * Returns the tip text for this property.
   *
   * @return tip text for this property suitable for 
   * displaying in the explorer/experimenter gui
   */
  public String averagingTypeTipText() {
    return "Choses the way in which the distributions are averaged in " 
    + "the first phase of the algorithm.";
  }

  /**
   * Sets the averaging type to use in phase 1 of the algorithm.  
   *
   * @param value the averaging type to use
   */
  public void setAveragingType(SelectedTag value) {
    if (value.getTags() == TAGS_AVERAGINGTYPES)
      m_atype = value.getSelectedTag().getID();
  }

  /**
   * Gets the averaging type.
   *
   * @return the averaging type
   */
  public SelectedTag getAveragingType() {
    return new SelectedTag(m_atype, TAGS_AVERAGINGTYPES);
  }

  /**
   * Returns the tip text for this property.
   *
   * @return tip text for this property suitable for 
   * displaying in the explorer/experimenter gui
   */
  public String distanceTypeTipText() {
    return "Sets the distance that is to be used by the nearest neighbour "
    + "rule";
  }

  /**
   * Sets the distance type to be used by a nearest neighbour rule (if any).
   *
   * @param value the distance type to use
   */
  public void setDistanceType(SelectedTag value) {
    if (value.getTags() == TAGS_DISTANCETYPES)
      m_dtype = value.getSelectedTag().getID();
  }

  /** 
   * Gets the distance type used by a nearest neighbour rule (if any).
   *
   * @return the distance type
   */
  public SelectedTag getDistanceType() {
    return new SelectedTag(m_dtype, TAGS_DISTANCETYPES);
  }

  /**
   * Returns the tip text for this property.
   *
   * @return tip text for this property suitable for 
   * displaying in the explorer/experimenter gui
   */
  public String extensionTypeTipText() {
    return "Sets the extension type to use.";
  }

  /**
   * Sets the extension type to use.
   * The minimal extension is the one used by 
   * Ben-David in the original algorithm.  The maximal extension is
   * a completely dual variant of the minimal extension.  When using
   * both, then the midpoint of the interval determined by both
   * extensions is returned.
   *
   * @param value the extension type to use
   */
  public void setExtensionType(SelectedTag value) {
    if (value.getTags() == TAGS_EXTENSIONTYPES)
      m_etype = value.getSelectedTag().getID();
  }

  /**
   * Gets the extension type.
   *
   * @return the extension type
   */
  public SelectedTag getExtensionType() {
    return new SelectedTag(m_etype, TAGS_EXTENSIONTYPES);
  }

  /**
   * Returns the tip text for this property.
   *
   * @return tip text for this property suitable for 
   * displaying in the explorer/experimenter gui
   */
  public String sortTipText() {
    return "If true, the instances are also sorted within the classes " 
    + "prior to building the rule bases.";
  }

  /**
   * Sets if the instances are to be sorted prior to building the rule bases.
   *
   * @param sort if <code> true </code> the instances will be sorted
   */
  public void setSort(boolean sort) {
    m_sort = sort;
  }

  /**
   * Returns if the instances are sorted prior to building the rule bases.
   * 
   * @return <code> true </code> if instances are sorted prior to building
   * the rule bases, <code> false </code> otherwise.
   */
  public boolean getSort() {
    return m_sort;
  }

  /**
   * Returns the tip text for this property.
   *
   * @return tip text for this property suitable for 
   * displaying in the explorer/experimenter gui
   */
  public String seedTipText() {
    return "Sets the seed that is used to randomize the instances prior "
    + "to building the rule bases";
  }

  /**
   * Return the number of examples in the minimal rule base.
   * The minimal rule base is the one that corresponds to the 
   * rule base of Ben-David.
   *
   * @return the number of examples in the minimal rule base
   */
  public int getSizeRuleBaseMin() {
    return m_baseMin.numInstances();
  }

  /**
   * Return the number of examples in the maximal rule base.
   * The maximal rule base is built using an algorithm
   * dual to that for building the minimal rule base.
   *
   * @return the number of examples in the maximal rule base
   */
  public int getSizeRuleBaseMax() {
    return m_baseMax.numInstances();
  }

  /** 
   * Classifies a given instance according to the current settings
   * of the classifier.
   * 
   * @param instance the instance to be classified
   * @return a <code> double </code> that represents the classification,
   * this could either be the internal value of a label, when rounding is 
   * on, or a real number.
   */
  public double classifyInstance(Instance instance) {
    double classValueMin = -1;
    double classValueMax = -1;
    double classValue;

    if (m_etype == ET_MIN || m_etype == ET_BOTH) {
      classValueMin = classifyInstanceMin(instance);
    }

    if (m_etype == ET_MAX || m_etype == ET_BOTH) {
      classValueMax = classifyInstanceMax(instance);
    }

    switch (m_etype) {
      case ET_MIN: 
	classValue = classValueMin;
	break;
      case ET_MAX:
	classValue = classValueMax;
	break;
      case ET_BOTH:
	classValue = (classValueMin + classValueMax) / 2;
	break;
      default:
	throw new IllegalStateException("Illegal mode type!");
    }

    // round if necessary and return 
    return (m_ctype == CT_ROUNDED ? Utils.round(classValue) : classValue);
  }

  /**
   * Classify <code> instance </code> using the minimal rule base.
   * Rounding is never performed, this is the responsability
   * of <code> classifyInstance </code>.
   *
   * @param instance the instance to be classified
   * @return the classification according to the minimal rule base
   */
  private double classifyInstanceMin(Instance instance) {
    double classValue = -1;
    if (m_baseMin == null) {
      throw new IllegalStateException
      ("Classifier has not yet been built");
    }

    Iterator it = new EnumerationIterator(m_baseMin.enumerateInstances());
    while (it.hasNext()) {
      Instance r = (Instance) it.next();

      // we assume that rules are ordered in decreasing class value order
      // so that the first one that we encounter is immediately the
      // one with the biggest class value
      if (InstancesUtil.smallerOrEqual(r, instance)) {
	classValue = r.classValue();
	break;
      }
    }

    // there is no smaller rule in the database
    if (classValue == -1) {
      if (m_dtype != DT_NONE) { 
	Instance[] nn = nearestRules(instance, m_baseMin);
	classValue = 0;
	// XXX for the moment we only use the mean to extract a 
	// classValue; other possibilities might be included later
	for (int i = 0; i < nn.length; i++) {
	  classValue += nn[i].classValue();
	}
	classValue /= nn.length;
      }
      else {
	classValue = 0; // minimal class value
      }
    }

    return classValue; // no rounding!
  }

  /**
   * Classify <code> instance </code> using the maximal rule base.
   * Rounding is never performed, this is the responsability
   * of <code> classifyInstance </code>.
   * 
   * @param instance the instance to be classified
   * @return the classification according to the maximal rule base
   */
  private double classifyInstanceMax(Instance instance) {
    double classValue = -1;
    if (m_baseMax == null) {
      throw new IllegalStateException
      ("Classifier has not yet been built");
    }

    Iterator it = new EnumerationIterator(m_baseMax.enumerateInstances());
    while (it.hasNext()) {
      Instance r = (Instance) it.next();

      // we assume that rules are ordered in increasing class value order
      // so that the first bigger one we encounter will be the one 
      // with the smallest label
      if (InstancesUtil.smallerOrEqual(instance, r)) {
	classValue = r.classValue();
	break;
      }
    }

    // there is no bigger rule in the database
    if (classValue == -1) {
      if (m_dtype != DT_NONE) { 
	// XXX see remark in classifyInstanceMin 
	Instance[] nn = nearestRules(instance, m_baseMax);
	classValue = 0;
	for (int i = 0; i < nn.length; i++) {
	  classValue += nn[i].classValue();
	}
	classValue /= nn.length;
      }
      else {
	classValue = m_numClasses - 1; // maximal label 
      }
    }

    return classValue;
  }

  /**
   * Find the instances in <code> base </code> that are, 
   * according to the current distance type, closest 
   * to <code> instance </code>.
   *
   * @param instance the instance around which one looks
   * @param base the instances to choose from
   * @return an array of <code> Instance </code> which contains
   * the instances closest to <code> instance </code>
   */
  private Instance[] nearestRules(Instance instance, Instances base) {
    double min = Double.POSITIVE_INFINITY;
    double dist = 0;
    double[] instanceDouble = InstancesUtil.toDataDouble(instance);
    ArrayList nn = new ArrayList();
    Iterator it = new EnumerationIterator(base.enumerateInstances());
    while(it.hasNext()) {
      Instance r = (Instance) it.next();
      double[] rDouble = InstancesUtil.toDataDouble(r);
      switch (m_dtype) {
	case DT_EUCLID: 
	  dist = euclidDistance(instanceDouble, rDouble);
	  break;
	case DT_HAMMING: 
	  dist = hammingDistance(instanceDouble, rDouble);
	  break;
	default:
	  throw new IllegalArgumentException("distance type is not valid");
      }
      if (dist < min) {
	min = dist;
	nn.clear();
	nn.add(r);
      } else if (dist == min) {
	nn.add(r);
      }
    }

    nn.trimToSize();
    return (Instance[]) nn.toArray(new Instance[0]);
  }

  /**
   * Build the OLM classifier, meaning that the rule bases 
   * are built.
   *
   * @param instances the instances to use for building the rule base
   * @throws Exception if <code> instances </code> cannot be handled by
   * the classifier.
   */
  public void buildClassifier(Instances instances) throws Exception {
    getCapabilities().testWithFail(instances);

    // copy the dataset 
    m_train = new Instances(instances);
    m_numClasses = m_train.numClasses();

    // new dataset in which examples with missing class value are removed
    m_train.deleteWithMissingClass();

    // build the Map for the estimatedDistributions 
    m_estimatedDistributions = new HashMap(m_train.numInstances() / 2);

    // cycle through all instances 
    Iterator it = new EnumerationIterator(m_train.enumerateInstances());
    while (it.hasNext() == true) {
      Instance instance = (Instance) it.next();
      Coordinates c = new Coordinates(instance);

      // get DiscreteEstimator from the map
      DiscreteEstimator df = 
	(DiscreteEstimator) m_estimatedDistributions.get(c);

      // if no DiscreteEstimator is present in the map, create one 
      if (df == null) {
	df = new DiscreteEstimator(instances.numClasses(), 0);
      }
      df.addValue(instance.classValue(), instance.weight()); // update
      m_estimatedDistributions.put(c, df); // put back in map
    }

    // Create the attributes for m_baseMin and m_baseMax. 
    // These are identical to those of m_train, except that the 
    // class is set to 'numeric'
    // The class attribute is moved to the back 
    FastVector newAtts = new FastVector(m_train.numAttributes());
    Attribute classAttribute = null;
    for (int i = 0; i < m_train.numAttributes(); i++) {
      Attribute att = m_train.attribute(i);
      if (i != m_train.classIndex()) {
	newAtts.addElement(att.copy());
      } else {
	classAttribute = new Attribute(att.name()); //numeric attribute 
      }
    }
    newAtts.addElement(classAttribute);

    // original training instances are replaced by an empty set
    // of instances
    m_train = new Instances(m_train.relationName(), newAtts, 
	m_estimatedDistributions.size());
    m_train.setClassIndex(m_train.numAttributes() - 1);


    // We cycle through the map of estimatedDistributions and
    // create one Instance for each entry in the map, with 
    // a class value that is calculated from the distribution of
    // the class values
    it = m_estimatedDistributions.keySet().iterator();
    while(it.hasNext()) {
      // XXX attValues must be here, otherwise things go wrong
      double[] attValues = new double[m_train.numAttributes()];
      Coordinates cc = (Coordinates) it.next();
      DiscreteEstimator df = 
	(DiscreteEstimator) m_estimatedDistributions.get(cc);
      cc.getValues(attValues);
      switch(m_atype) {
	case AT_MEAN:
	  attValues[attValues.length - 1] = (new DiscreteDistribution(df)).mean();
	  break;
	case AT_MEDIAN:
	  attValues[attValues.length - 1] = (new DiscreteDistribution(df)).median();
	  break;
	case AT_MAXPROB:
	  attValues[attValues.length - 1] = (new DiscreteDistribution(df)).modes()[0];
	  break;
	default: 
	  throw new IllegalStateException("Not a valid averaging type");
      }

      // add the instance, we give it weight one 
      m_train.add(new Instance(1, attValues));
    }

    if (m_Debug == true) {
      System.out.println("The dataset after phase 1 :");
      System.out.println(m_train.toString());
    }

    /* Shuffle to training instances, to prevent the order dictated
     * by the map to play an important role in how the rule bases
     * are built. 
     */
    m_train.randomize(new Random(getSeed()));

    if (m_sort == false) {
      // sort the instances only in increasing class order
      m_train.sort(m_train.classIndex());
    } else {
      // sort instances completely
      Comparator[] cc = new Comparator[m_train.numAttributes()];
      // sort the class, increasing 
      cc[0] = new InstancesComparator(m_train.classIndex());
      // sort the attributes, decreasing
      for (int i = 1; i < cc.length; i++) {
	cc[i] = new InstancesComparator(i - 1, true);
      }

      // copy instances into an array
      Instance[] tmp = new Instance[m_train.numInstances()];
      for (int i = 0; i < tmp.length; i++) {
	tmp[i] = m_train.instance(i);
      }
      MultiDimensionalSort.multiDimensionalSort(tmp, cc);

      // copy sorted array back into Instances
      m_train.delete();                
      for (int i = 0; i < tmp.length; i++) {
	m_train.add(tmp[i]);
      }
    }

    // phase 2: building the rule bases themselves
    m_baseMin = 
      new Instances(m_train, m_estimatedDistributions.size() / 4); 
    phaseTwoMin();
    m_baseMax = 
      new Instances(m_train, m_estimatedDistributions.size() / 4); 
    phaseTwoMax();
  }

  /**
   * This implements the second phase of the OLM algorithm.
   * We build the rule base  m_baseMin, according to the conflict
   * resolution mechanism described in the thesis. 
   */
  private void phaseTwoMin() {

    // loop through instances backwards, this is biggest class labels first
    for (int i = m_train.numInstances() - 1; i >=0; i--) {
      Instance e = m_train.instance(i);

      // if the example is redundant with m_base, we discard it
      if (isRedundant(e) == false) {
	// how many examples are redundant if we would add e
	int[] redundancies = makesRedundant(e);
	if (redundancies[0] == 1 
	    && causesReversedPreference(e) == false) {
	  // there is one example made redundant be e, and 
	  // adding e doesn't cause reversed preferences  
	  // so we replace the indicated rule by e
	  m_baseMin.delete(redundancies[1]);
	  m_baseMin.add(e);
	  continue;
	}

	if (redundancies[0] == 0) {
	  // adding e causes no redundancies, what about 
	  // reversed preferences ?
	  int[] revPref = reversedPreferences(e);
	  if (revPref[0] == 1) {
	    // there would be one reversed preference, we 
	    // prefer the example e because it has a lower label
	    m_baseMin.delete(revPref[1]);
	    m_baseMin.add(e);
	    continue;
	  }

	  if (revPref[0] == 0) {
	    // this means: e causes no redundancies and no 
	    // reversed preferences.  We can simply add it.
	    m_baseMin.add(e);
	  }
	}
      }
    }
  }

  /** 
   * This implements the second phase of the OLM algorithm.
   * We build the rule base  m_baseMax .
   */
  private void phaseTwoMax() {

    // loop through instances, smallest class labels first
    for (int i = 0; i < m_train.numInstances(); i++) {
      Instance e = m_train.instance(i);

      // if the example is redundant with m_base, we discard it
      if (isRedundantMax(e) == false) {
	// how many examples are redundant if we would add e
	int[] redundancies = makesRedundantMax(e);
	if (redundancies[0] == 1 
	    && causesReversedPreferenceMax(e) == false) {
	  // there is one example made redundant be e, and 
	  // adding e doesn't cause reversed preferences  
	  // so we replace the indicated rule by e
	  m_baseMax.delete(redundancies[1]);
	  m_baseMax.add(e);
	  continue;
	}

	if (redundancies[0] == 0) {
	  // adding e causes no redundancies, what about 
	  // reversed preferences ?
	  int[] revPref = reversedPreferencesMax(e);
	  if (revPref[0] == 1) {
	    // there would be one reversed preference, we 
	    // prefer the example e because it has a lower label
	    m_baseMax.delete(revPref[1]);
	    m_baseMax.add(e);
	    continue;
	  }

	  if (revPref[0] == 0) {
	    // this means: e causes no redundancies and no 
	    // reversed preferences.  We can simply add it.
	    m_baseMax.add(e);
	  }
	}
      }
    }
  }

  /**
   * Returns a string description of the classifier.  In debug
   * mode, the rule bases are added to the string representation
   * as well.  This means that the description can become rather
   * lengthy.
   *
   * @return a <code> String </code> describing the classifier.
   */
  public String toString() {
    StringBuffer sb = new StringBuffer();
    sb.append("OLM\n===\n\n");
    if (m_etype == ET_MIN || m_etype == ET_BOTH) {
      if (m_baseMin != null) {
	sb.append("Number of examples in the minimal rule base = " 
	    + m_baseMin.numInstances() + "\n");
      } else {
	sb.append("minimal rule base not yet created");
      }
    }

    if (m_etype == ET_MAX || m_etype == ET_BOTH) {
      if (m_baseMax != null) {
	sb.append("Number of examples in the maximal rule base = " 
	    + m_baseMax.numInstances() + "\n");
      } else {
	sb.append("maximal rule base not yet created");
      }
    }

    if (m_Debug == true) {

      if (m_etype == ET_MIN || m_etype == ET_BOTH) {
	sb.append("The minimal rule base is \n");
	if (m_baseMin != null) {
	  sb.append(m_baseMin.toString());
	} else {
	  sb.append(".... not yet created");
	}
      }

      if (m_etype == ET_MAX || m_etype == ET_BOTH) {
	sb.append("The second rule base is \n");
	if (m_baseMax != null) {
	  sb.append(m_baseMax.toString());
	} else {
	  sb.append(".... not yet created");
	}
      }
    }

    sb.append("\n");
    return sb.toString();
  }

  /**
   * Is <code> instance </code> redundant wrt the 
   * current <code> m_baseMin </code>.
   * This mean we are looking if there is an element in 
   * <code> m_baseMin </code> smaller than <code> instance </code>
   * and with the same class.
   *
   * @param instance the instance of which the redundancy needs to be 
   * checked
   * @return <code> true </code> if <code> instance </code>
   * is redundant, <code> false </code> otherwise
   */
  private boolean isRedundant(Instance instance) {
    Iterator it = new EnumerationIterator(m_baseMin.enumerateInstances());
    while(it.hasNext()) {
      Instance r = (Instance) it.next();
      if (instance.classValue() == r.classValue()
	  && InstancesUtil.smallerOrEqual(r, instance) ) {
	return true;
      }
    }
    return false;
  }

  /**
   * Is <code> instance </code> redundant wrt the 
   * current <code> m_baseMax </code>.
   * This is dual to the previous method, this means that 
   * <code> instance </code> is redundant if there exists
   * and example greater of equal than <code> instance </code>
   * with the same class value.
   * 
   * @param instance the instance of which the redundancy needs to be 
   * checked
   * @return <code> true </code> if <code> instance </code>
   * is redundant, <code> false </code> otherwise
   */
  private boolean isRedundantMax(Instance instance) {
    Iterator it = new EnumerationIterator(m_baseMax.enumerateInstances());
    while(it.hasNext()) {
      Instance r = (Instance) it.next();
      if (instance.classValue() == r.classValue()
	  && InstancesUtil.smallerOrEqual(instance, r) ) {
	return true;
      }
    }
    return false;
  }

  /**
   * If we were to add <code> instance </code>, how many
   * and which elements from <code> m_baseMin </code> would 
   * be made redundant by <code> instance </code>.
   * 
   * @param instance the instance of which the influence is to be determined
   * @return an array containing two components 
   * [0] is 0 if instance makes no elements redundant;
   *     is 1 if there is one rule that is made redundant by instance;
   *     is 2 if there is more than one rule that is made redundant; 
   * [1] if [0] == 1, this entry gives the element that is made redundant
   */
  private int[] makesRedundant(Instance instance) {
    int[] ret = new int[2];
    for (int i = 0; i < m_baseMin.numInstances(); i++) {
      Instance r = m_baseMin.instance(i);
      if (r.classValue() == instance.classValue() && 
	  InstancesUtil.smallerOrEqual(instance, r)) {
	if (ret[0] == 0) {
	  ret[0] = 1;
	  ret[1] = i;
	} else { // this means ret[0] == 1
	  ret[0] = 2;
	  return ret;
	}
      }
    }
    return ret;
  }

  /**
   * If we were to add <code> instance </code>, how many
   * and which elements from <code> m_baseMax </code> would 
   * be made redundant by <code> instance </code>.
   * This method is simply dual to <code> makesRedundant </code>.
   * 
   * @param instance the instance to add
   * @return an array containing two components 
   * [0] is 0 if instance makes no elements redundant;
   *     is 1 if there is one rule that is made redundant by instance;
   *     is 2 if there is more than one rule that is made redundant; 
   * [1] if [0] == 1, this entry gives the element that is made redundant
   */
  private int[] makesRedundantMax(Instance instance) {
    int[] ret = new int[2];
    for (int i = 0; i < m_baseMax.numInstances(); i++) {
      Instance r = m_baseMax.instance(i);
      if (r.classValue() == instance.classValue() && 
	  InstancesUtil.smallerOrEqual(r, instance)) {
	if (ret[0] == 0) {
	  ret[0] = 1;
	  ret[1] = i;
	} else { // this means ret[0] == 1
	  ret[0] = 2;
	  return ret;
	}
      }
    }
    return ret;
  }

  /**
   * Checks if adding <code> instance </code> to <code> m_baseMin </code>
   * causes reversed preference amongst the elements of <code>
   * m_baseMin </code>.
   *
   * @param instance the instance of which the influence needs to be 
   * determined
   * @return <code> true </code> if adding <code> instance </code> 
   * to the rule base would cause reversed preference among the rules, 
   * <code> false </code> otherwise.
   */
  private boolean causesReversedPreference(Instance instance) {
    Iterator it = new EnumerationIterator(m_baseMin.enumerateInstances());
    while (it.hasNext()) {
      Instance r = (Instance) it.next();
      if (instance.classValue() > r.classValue() && 
	  InstancesUtil.smallerOrEqual(instance, r)) {
	// in the original version of OLM this should not happen
	System.err.println
	("Should not happen in the original OLM algorithm");
	return true;
      } else if (r.classValue() > instance.classValue() &&
	  InstancesUtil.smallerOrEqual(r, instance)) {
	return true;
      }
    }	    
    return false;
  }

  /**
   * Checks if adding <code> instance </code> to <code> m_baseMax </code>
   * causes reversed preference amongst the elements of 
   * <code> m_baseMax </code>.
   * 
   * @param instance the instance to add
   * @return true if adding <code> instance </code> to the rule 
   * base would cause reversed preference among the rules, 
   * false otherwise.
   */
  private boolean causesReversedPreferenceMax(Instance instance) {
    Iterator it = new EnumerationIterator(m_baseMax.enumerateInstances());
    while (it.hasNext()) {
      Instance r = (Instance) it.next();
      if (instance.classValue() > r.classValue() && 
	  InstancesUtil.smallerOrEqual(instance, r)) {
	return true;
      } else if (r.classValue() > instance.classValue() &&
	  InstancesUtil.smallerOrEqual(r, instance)) {
	return true;
      }
    }	    
    return false;
  }

  /**
   * Find indices of the elements from <code> m_baseMin </code>
   * that are inconsistent with instance.
   * 
   * @param instance the instance of which the influence needs to be
   * determined
   * @return an array containing two components 
   * [0] is 0 if instance is consistent with all present elements 
   *     is 1 if instance is inconsistent (reversed preference) with
   *           exactly one element
   *     is 2 if instance is inconsistent with more than one element
   * [1] if [0] == 1, this entry gives the index of the 
   *                  element that has reversed preference wrt to 
   *                  <code> instance </code>
   */
  private int[] reversedPreferences(Instance instance) {
    int[] revPreferences = new int[2];
    for (int i = 0; i < m_baseMin.numInstances(); i++) {
      Instance r = m_baseMin.instance(i);
      if (instance.classValue() < r.classValue() &&
	  InstancesUtil.smallerOrEqual(r, instance) ) {
	if (revPreferences[0] == 0) {
	  revPreferences[0] = 1;
	  revPreferences[1] = i;
	} else {
	  revPreferences[0] = 2;
	  return revPreferences;
	}
      }
    }
    return revPreferences;
  }

  /**
   * Find indices of the elements from <code> m_baseMin </code>
   * that are inconsistent with instance.
   * 
   * @param instance the instance of which the influence needs to be
   * determined
   * @return an array containing two components 
   * [0] is 0 if instance is consistent with all present elements 
   *     is 1 if instance is inconsistent (reversed preference) with
   *           exactly one element
   *     is 2 if instance is inconsistent with more than one element
   * [1] if [0] == 1, this entry gives the index of the 
   *                  element that has reversed preference wrt to 
   *                  <code> instance </code>
   */
  private int[] reversedPreferencesMax(Instance instance) {
    int[] revPreferences = new int[2];
    for (int i = 0; i < m_baseMax.numInstances(); i++) {
      Instance r = m_baseMax.instance(i);
      if (instance.classValue() > r.classValue() &&
	  InstancesUtil.smallerOrEqual(instance, r) ) {
	if (revPreferences[0] == 0) {
	  revPreferences[0] = 1;
	  revPreferences[1] = i;
	} else {
	  revPreferences[0] = 2;
	  return revPreferences;
	}
      }
    }
    return revPreferences;
  }

  /**
   * Calculates the square of the Euclidian distance between
   * two arrays of doubles.  The arrays should have the same length.
   *
   * @param a1 the first array
   * @param a2 the second array
   * @return the square of the Euclidian distance between the two
   * arrays <code> a1 </code> and <code> a2 </code> 
   */
  private double euclidDistance(double[] a1, double[] a2) {
    double dist = 0;
    for (int i = 0; i < a1.length; i++) {
      dist += (a1[i] - a2[i]) * (a1[i] - a2[i]);
    }
    return dist;
  }

  /** 
   * Calculates the Hamming distances between two arrays, this
   * means one counts the number of positions in which these
   * two array differ.  The arrays should be of the same length.
   * 
   * @param a1 the first array
   * @param a2 the second array
   * @return the requested Hamming distance
   * The Hamming distance between a1 and a2, this is 
   * the number of positions in which a1 and a2 differ
   */
  private int hammingDistance(double[] a1, double[] a2) {
    int dist = 0;
    for (int i = 0; i < a1.length; i++) {
      dist += (a1[i] == a2[i]) ? 0 : 1;
    }
    return dist;
  }

  /**
   * Get an enumeration of all available options for this classifier.
   * 
   * @return an enumeration of available options
   */
  public Enumeration listOptions() {
    Vector options = new Vector();

    Enumeration enm = super.listOptions();
    while (enm.hasMoreElements())
      options.addElement(enm.nextElement());

    String description = 
      "\tSets the classification type to be used.\n" +
      "\t(Default: " + new SelectedTag(CT_REAL, TAGS_CLASSIFICATIONTYPES) + ")";
    String synopsis = "-C " + Tag.toOptionList(TAGS_CLASSIFICATIONTYPES);
    String name = "C";
    options.addElement(new Option(description, name, 1, synopsis));

    description = 
      "\tSets the averaging type used in phase 1 of the classifier.\n" +
      "\t(Default: " + new SelectedTag(AT_MEAN, TAGS_AVERAGINGTYPES) + ")";
    synopsis = "-A " + Tag.toOptionList(TAGS_AVERAGINGTYPES);
    name = "A";
    options.addElement(new Option(description, name, 1, synopsis));

    description =
      "\tIf different from " + new SelectedTag(DT_NONE, TAGS_DISTANCETYPES) + ", a nearest neighbour rule is fired when the\n" +
      "\trule base doesn't contain an example smaller than the instance\n" + 
      "\tto be classified\n" +
      "\t(Default: " + new SelectedTag(DT_NONE, TAGS_DISTANCETYPES) + ").";
    synopsis = "-N " + Tag.toOptionList(TAGS_DISTANCETYPES);
    name = "N";
    options.addElement(new Option(description, name, 1, synopsis));

    description = "\tSets the extension type, i.e. the rule base to use."
      + "\n\t(Default: " + new SelectedTag(ET_MIN, TAGS_EXTENSIONTYPES) + ")";
    synopsis = "-E " + Tag.toOptionList(TAGS_EXTENSIONTYPES);
    name = "E";
    options.addElement(new Option(description, name, 1, synopsis));

    description = 
      "\tIf set, the instances are also sorted within the same class\n"
      + "\tbefore building the rule bases"; 
    synopsis = "-sort";
    name = "sort";
    options.addElement(new Option(description, name, 0, synopsis));

    return options.elements();
  }

  /** 
   * Parses the options for this object. <p/>
   *
   <!-- options-start -->
   * Valid options are: <p/>
   * 
   * <pre> -S &lt;num&gt;
   *  Random number seed.
   *  (default 1)</pre>
   * 
   * <pre> -D
   *  If set, classifier is run in debug mode and
   *  may output additional info to the console</pre>
   * 
   * <pre> -C &lt;CL|REG&gt;
   *  Sets the classification type to be used.
   *  (Default: REG)</pre>
   * 
   * <pre> -A &lt;MEAN|MED|MAX&gt;
   *  Sets the averaging type used in phase 1 of the classifier.
   *  (Default: MEAN)</pre>
   * 
   * <pre> -N &lt;NONE|EUCL|HAM&gt;
   *  If different from NONE, a nearest neighbour rule is fired when the
   *  rule base doesn't contain an example smaller than the instance
   *  to be classified
   *  (Default: NONE).</pre>
   * 
   * <pre> -E &lt;MIN|MAX|BOTH&gt;
   *  Sets the extension type, i.e. the rule base to use.
   *  (Default: MIN)</pre>
   * 
   * <pre> -sort
   *  If set, the instances are also sorted within the same class
   *  before building the rule bases</pre>
   * 
   <!-- options-end -->
   *
   * @param options an array of strings containing the options 
   * @throws Exception if there are options that have invalid arguments.
   */
  public void setOptions(String[] options) throws Exception {
    String args;

    // classification type
    args = Utils.getOption('C', options);
    if (args.length() != 0) 
      setClassificationType(new SelectedTag(args, TAGS_CLASSIFICATIONTYPES));
    else
      setClassificationType(new SelectedTag(CT_REAL, TAGS_CLASSIFICATIONTYPES));

    // averaging type
    args = Utils.getOption('A', options);
    if (args.length() != 0) 
      setAveragingType(new SelectedTag(args, TAGS_AVERAGINGTYPES));
    else
      setAveragingType(new SelectedTag(AT_MEAN, TAGS_AVERAGINGTYPES));

    // distance type
    args = Utils.getOption('N', options); 
    if (args.length() != 0)
      setDistanceType(new SelectedTag(args, TAGS_DISTANCETYPES));
    else
      setDistanceType(new SelectedTag(DT_NONE, TAGS_DISTANCETYPES));

    // extension type
    args = Utils.getOption('E', options);
    if (args.length() != 0) 
      setExtensionType(new SelectedTag(args, TAGS_EXTENSIONTYPES));
    else
      setExtensionType(new SelectedTag(ET_MIN, TAGS_EXTENSIONTYPES));

    // sort ? 
    setSort(Utils.getFlag("sort", options));
    
    super.setOptions(options);
  }

  /** 
   * Gets an array of string with the current options of the classifier.
   *
   * @return an array suitable as argument for <code> setOptions </code>
   */
  public String[] getOptions() {
    int       	i;
    Vector    	result;
    String[]  	options;

    result = new Vector();

    options = super.getOptions();
    for (i = 0; i < options.length; i++)
      result.add(options[i]);

    // classification type
    result.add("-C");
    result.add("" + getClassificationType());

    result.add("-A");
    result.add("" + getAveragingType());

    result.add("-N");
    result.add("" + getDistanceType());

    result.add("-E");
    result.add("" + getExtensionType());

    if (getSort())
      result.add("-sort");

    return (String[]) result.toArray(new String[result.size()]);
  }

  /**
   * Main method for testing this class.
   * 
   * @param args the command line arguments
   */
  public static void main(String[] args) {
    runClassifier(new OLM(), args);
  }
}
