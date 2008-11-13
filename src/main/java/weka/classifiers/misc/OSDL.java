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
 *    OSDL.java
 *    Copyright (C) 2004 Stijn Lievens
 *
 */

package weka.classifiers.misc;

import weka.classifiers.misc.monotone.OSDLCore;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.RevisionUtils;

/**
 <!-- globalinfo-start -->
 * This class is an implementation of the Ordinal Stochastic Dominance Learner.<br/>
 * Further information regarding the OSDL-algorithm can be found in:<br/>
 * <br/>
 * S. Lievens, B. De Baets, K. Cao-Van (2006). A Probabilistic Framework for the Design of Instance-Based Supervised Ranking Algorithms in an Ordinal Setting. Annals of Operations Research..<br/>
 * <br/>
 * Kim Cao-Van (2003). Supervised ranking: from semantics to algorithms.<br/>
 * <br/>
 * Stijn Lievens (2004). Studie en implementatie van instantie-gebaseerde algoritmen voor gesuperviseerd rangschikken.<br/>
 * <br/>
 * For more information about supervised ranking, see<br/>
 * <br/>
 * http://users.ugent.be/~slievens/supervised_ranking.php
 * <p/>
 <!-- globalinfo-end -->
 *
 <!-- technical-bibtex-start -->
 * BibTeX:
 * <pre>
 * &#64;article{Lievens2006,
 *    author = {S. Lievens and B. De Baets and K. Cao-Van},
 *    journal = {Annals of Operations Research},
 *    title = {A Probabilistic Framework for the Design of Instance-Based Supervised Ranking Algorithms in an Ordinal Setting},
 *    year = {2006}
 * }
 * 
 * &#64;phdthesis{Cao-Van2003,
 *    author = {Kim Cao-Van},
 *    school = {Ghent University},
 *    title = {Supervised ranking: from semantics to algorithms},
 *    year = {2003}
 * }
 * 
 * &#64;mastersthesis{Lievens2004,
 *    author = {Stijn Lievens},
 *    school = {Ghent University},
 *    title = {Studie en implementatie van instantie-gebaseerde algoritmen voor gesuperviseerd rangschikken},
 *    year = {2004}
 * }
 * </pre>
 * <p/>
 <!-- technical-bibtex-end -->
 *
 <!-- options-start -->
 * Valid options are: <p/>
 * 
 * <pre> -D
 *  If set, classifier is run in debug mode and
 *  may output additional info to the console</pre>
 * 
 * <pre> -C &lt;REG|WSUM|MAX|MED|RMED&gt;
 *  Sets the classification type to be used.
 *  (Default: MED)</pre>
 * 
 * <pre> -B
 *  Use the balanced version of the Ordinal Stochastic Dominance Learner</pre>
 * 
 * <pre> -W
 *  Use the weighted version of the Ordinal Stochastic Dominance Learner</pre>
 * 
 * <pre> -S &lt;value of interpolation parameter&gt;
 *  Sets the value of the interpolation parameter (not with -W/T/P/L/U)
 *  (default: 0.5).</pre>
 * 
 * <pre> -T
 *  Tune the interpolation parameter (not with -W/S)
 *  (default: off)</pre>
 * 
 * <pre> -L &lt;Lower bound for interpolation parameter&gt;
 *  Lower bound for the interpolation parameter (not with -W/S)
 *  (default: 0)</pre>
 * 
 * <pre> -U &lt;Upper bound for interpolation parameter&gt;
 *  Upper bound for the interpolation parameter (not with -W/S)
 *  (default: 1)</pre>
 * 
 * <pre> -P &lt;Number of parts&gt;
 *  Determines the step size for tuning the interpolation
 *  parameter, nl. (U-L)/P (not with -W/S)
 *  (default: 10)</pre>
 * 
 <!-- options-end -->
 *
 * More precisely, this is a simple extension of the OSDLCore class, 
 * so that the OSDLCore class can be used within the WEKA environment.
 * The problem with OSDLCore is that it implements both
 * <code> classifyInstance </code> and <code> distributionForInstance </code>
 * in a non trivial way.
 * <p>
 * One can evaluate a model easily with the method <code> evaluateModel </code>
 * from the <code> Evaluation </code> class.  However, for nominal classes
 * they do the following: they use <code> distributionForInstance </code> 
 * and then pick the class with maximal probability.  This procedure
 * is <b> not </b> valid for a ranking algorithm, since this destroys
 * the required monotonicity property.
 * </p>
 * <p> 
 * This class reimplements <code> distributionForInstance </code> in the 
 * following way:  first <code> classifyInstance </code> of 
 * <code> OSDLCore </code>  is used and the chosen label then gets 
 * assigned probability one.  This ensures that the classification 
 * accuracy is calculated correctly, but possibly some other statistics 
 * are no longer meaningful.
 * </p>
 *
 * @author Stijn Lievens (stijn.lievens@ugent.be)
 * @version $Revision: 1.2 $
 */
public class OSDL
  extends OSDLCore {

  /** for serialization */
  private static final long serialVersionUID = -4534219825732505381L;

  /**
   * Use <code> classifyInstance </code> from <code> OSDLCore </code> and
   * assign probability one to the chosen label.
   * The implementation is heavily based on the same method in 
   * the <code> Classifier </code> class.
   * 
   * @param instance the instance to be classified
   * @return an array containing a single '1' on the index
   * that <code> classifyInstance </code> returns.
   */
  public double[] distributionForInstance(Instance instance) {

    // based on the code from the Classifier class
    double[] dist = new double[instance.numClasses()];
    int classification = 0;
    switch (instance.classAttribute().type()) {
      case Attribute.NOMINAL:
	try {
	  classification = 
	    (int) Math.round(classifyInstance(instance));
	} catch (Exception e) {
	  System.out.println("There was a problem with classifyIntance");
	  System.out.println(e.getMessage());
	  e.printStackTrace();
	}
	if (Instance.isMissingValue(classification)) {
	  return dist;
	} 
	dist[classification] = 1.0;
	return dist;
	
      case Attribute.NUMERIC:
	try {
	  dist[0] = classifyInstance(instance);
	} catch (Exception e) {
	  System.out.println("There was a problem with classifyIntance");
	  System.out.println(e.getMessage());
	  e.printStackTrace();
	}
	return dist;
	
      default:
	return dist;
    }
  }    
  
  /**
   * Returns the revision string.
   * 
   * @return		the revision
   */
  public String getRevision() {
    return RevisionUtils.extract("$Revision: 1.2 $");
  }

  /**
   * Main method for testing this class and for using it from the
   * command line.
   *
   * @param args array of options for both the classifier <code>
   * OSDL </code> and for <code> evaluateModel </code>
   */
  public static void main(String[] args) {
    runClassifier(new OSDL(), args);
  }
}
