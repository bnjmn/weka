/**
 *    KStarWrapper.java
 *    Copyright (c) 1995-97 by Len Trigg (trigg@cs.waikato.ac.nz).
 *    Java port to Weka by Abdelaziz Mahoui (am14@cs.waikato.ac.nz).
 *
 */

package weka.classifiers.kstar;

/*
 * @author Len Trigg (len@intelligenesis.net)
 * @author Abdelaziz Mahoui (am14@cs.waikato.ac.nz)
 * @version $Revision 1.0 $
 */
public class KStarWrapper {

  /** used/reused to hold the sphere size */
  public double sphere = 0.0;

  /** used/reused to hold the actual entropy */
  public double actEntropy = 0.0;

  /** used/reused to hold the random entropy */
  public double randEntropy = 0.0;

  /** used/reused to hold the average transformation probability */
  public double avgProb = 0.0;

  /** used/reused to hold the smallest transformation probability */
  public double minProb = 0.0;

}

