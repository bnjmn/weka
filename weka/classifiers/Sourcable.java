/*
 *    Sourcable.java
 *    Copyright (C) 1999 Len Trigg
 *
 */

package weka.classifiers;

/** 
 * Interface for classifiers that can be converted to Java source.
 *
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @version $Revision: 1.2 $
 */
public interface Sourcable {

  /**
   * Returns a string that describes the classifier as source. The
   * classifier will be contained in a class with the given name (there may
   * be auxiliary classes),
   * and will contain a method with the signature:
   * <pre><code>
   * public static double classify(Object [] i);
   * </code></pre>
   * where the array <code>i</code> contains elements that are either
   * Double, String, with missing values represented as null. The generated
   * code is public domain and comes with no warranty.
   *
   * @param className the name that should be given to the source class.
   * @return the object source described by a string
   * @exception Exception if the souce can't be computed
   */
  public String toSource(String className) throws Exception;
}








