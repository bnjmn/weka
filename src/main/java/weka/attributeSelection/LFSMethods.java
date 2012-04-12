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
 *    LFSMethods.java
 *    Copyright (C) 2007 Martin Guetlein
 *
 */
package weka.attributeSelection;

import weka.core.FastVector;
import weka.core.Instances;
import weka.core.RevisionHandler;
import weka.core.RevisionUtils;
import weka.core.Utils;

import java.io.Serializable;
import java.util.BitSet;
import java.util.Hashtable;

/**
 * @author Martin Guetlein (martin.guetlein@gmail.com)
 * @version $Revision: 1.3 $
 */
public class LFSMethods
  implements RevisionHandler {
  
  /** max-size of array bestGroupOfSize, should be suffient */
  private final static int MAX_SUBSET_SIZE = 200;
  private BitSet m_bestGroup;
  private double m_bestMerit;
  private int m_evalsTotal;
  private int m_evalsCached;
  private BitSet[] m_bestGroupOfSize = new BitSet[MAX_SUBSET_SIZE];

  /**
   * empty constructor
   *
   * methods are not static because of access to inner class Link2 and
   * LinkedList2
   *
   */
  public LFSMethods() {
  }

  /**
   * @return best group found by forwardSearch/floatingForwardSearch
   */
  public BitSet getBestGroup() {
    return m_bestGroup;
  }

  /**
   * @return merit of best group found by forwardSearch/floatingForwardSearch
   */
  public double getBestMerit() {
    return m_bestMerit;
  }

  /**
   * @return best group of size found by forwardSearch
   */
  public BitSet getBestGroupOfSize(int size) {
    return m_bestGroupOfSize[size];
  }

  /**
   * @return number of cached / not performed evaluations
   */
  public int getNumEvalsCached() {
    return m_evalsCached;
  }

  /**
   * @return number totally performed evaluations
   */
  public int getNumEvalsTotal() {
    return m_evalsTotal;
  }

  /**
   * @return ranking (integer array) of attributes in data with evaluator (sorting is NOT stable!)
   */
  public int[] rankAttributes(Instances data, SubsetEvaluator evaluator,
                              boolean verbose) throws Exception {
    if (verbose) {
      System.out.println("Ranking attributes with " +
                         evaluator.getClass().getName());
    }

    double[] merit = new double[data.numAttributes()];
    BitSet group = new BitSet(data.numAttributes());

    for (int k = 0; k < data.numAttributes(); k++) {
      if (k != data.classIndex()) {
        group.set(k);
        merit[k] -= evaluator.evaluateSubset(group);
        m_evalsTotal++;
        group.clear(k);
      } else {
        merit[k] = Double.MAX_VALUE;
      }

      if (verbose) {
        System.out.println(k + ": " + merit[k]);
      }
    }

    int[] ranking = Utils.sort(merit);

    if (verbose) {
      System.out.print("Ranking [ ");

      for (int i = 0; i < ranking.length; i++) {
        System.out.print(ranking[i] + " ");
      }

      System.out.println("]\n");
    }

    return ranking;
  }

  /**
   * Performs linear forward selection
   *
   * @param cacheSize         chacheSize (times number of instances) to store already evaluated sets
   * @param startGroup        start group for search (can be null)
   * @param ranking                ranking of attributes (as produced by rankAttributes), no ranking would be [0,1,2,3,4..]
   * @param k                                number of top k attributes that are taken into account
   * @param incrementK        true -> fixed-set, false -> fixed-width
   * @param maxStale                number of times the search proceeds even though no improvement was found (1 = hill-climbing)
   * @param forceResultSize        stopping criteria changed from no-improvement (forceResultSize=-1) to subset-size
   * @param data
   * @param evaluator
   * @param verbose
   * @return                                BitSet, that cotains the best-group found
   * @throws Exception
   */
  public BitSet forwardSearch(int cacheSize, BitSet startGroup, int[] ranking,
                              int k, boolean incrementK, int maxStale, int forceResultSize,
                              Instances data, SubsetEvaluator evaluator, boolean verbose)
    throws Exception {
    if ((forceResultSize > 0) && (maxStale > 1)) {
      throw new Exception("Forcing result size only works for maxStale=1");
    }

    if (verbose) {
      System.out.println("Starting forward selection");
    }

    BitSet bestGroup;
    BitSet tempGroup;
    int bestSize = 0;
    int tempSize = 0;
    double bestMerit;
    double tempMerit = 0;
    Link2 link;
    LinkedList2 list = new LinkedList2(maxStale);
    Hashtable alreadyExpanded = new Hashtable(cacheSize * data.numAttributes());
    int insertCount = 0;
    int stale = 0;
    boolean improvement;
    int thisK = k;
    int evalsTotal = 0;
    int evalsCached = 0;

    bestGroup = (BitSet) startGroup.clone();

    String hashKey = bestGroup.toString();
    bestMerit = evaluator.evaluateSubset(bestGroup);

    if (verbose) {
      System.out.print("Group: ");
      printGroup(bestGroup, data.numAttributes());
      System.out.println("Merit: " + tempMerit);
      System.out.println("----------");
    }

    alreadyExpanded.put(hashKey, new Double(bestMerit));
    insertCount++;
    bestSize = bestGroup.cardinality();

    //the list is only used if best-first search is applied
    if (maxStale > 1) {
      Object[] best = new Object[1];
      best[0] = bestGroup.clone();
      list.addToList(best, bestMerit);
    }

    while (stale < maxStale) {
      improvement = false;

      //best-first: take first elem from list
      if (maxStale > 1) {
        if (list.size() == 0) {
          stale = maxStale;

          break;
        }

        link = list.getLinkAt(0);
        tempGroup = (BitSet) (link.getData()[0]);
        tempGroup = (BitSet) tempGroup.clone();
        list.removeLinkAt(0);

        tempSize = 0;

        for (int i = 0; i < data.numAttributes(); i++) {
          if (tempGroup.get(i)) {
            tempSize++;
          }
        }
      } else //hill-climbing 
        {
          tempGroup = (BitSet) bestGroup.clone();
          tempSize = bestSize;
        }

      //set number of top k attributes that are taken into account
      if (incrementK) {
        thisK = Math.min(Math.max(thisK, k + tempSize), data.numAttributes());
      } else {
        thisK = k;
      }

      //temporarilly add attributes to current set
      for (int i = 0; i < thisK; i++) {
        if ((ranking[i] == data.classIndex()) || tempGroup.get(ranking[i])) {
          continue;
        }

        tempGroup.set(ranking[i]);
        tempSize++;
        hashKey = tempGroup.toString();

        if (!alreadyExpanded.containsKey(hashKey)) {
          evalsTotal++;
          tempMerit = evaluator.evaluateSubset(tempGroup);

          if (insertCount > (cacheSize * data.numAttributes())) {
            alreadyExpanded = new Hashtable(cacheSize * data.numAttributes());
            insertCount = 0;
          }

          alreadyExpanded.put(hashKey, new Double(tempMerit));
          insertCount++;
        } else {
          evalsCached++;
          tempMerit = ((Double) alreadyExpanded.get(hashKey)).doubleValue();
        }

        if (verbose) {
          System.out.print("Group: ");
          printGroup(tempGroup, data.numAttributes());
          System.out.println("Merit: " + tempMerit);
        }

        if (((tempMerit - bestMerit) > 0.00001) ||
            ((forceResultSize >= tempSize) && (tempSize > bestSize))) {
          improvement = true;
          stale = 0;
          bestMerit = tempMerit;
          bestSize = tempSize;
          bestGroup = (BitSet) (tempGroup.clone());
          m_bestGroupOfSize[bestSize] = (BitSet) (tempGroup.clone());
        }

        if (maxStale > 1) {
          Object[] add = new Object[1];
          add[0] = tempGroup.clone();
          list.addToList(add, tempMerit);
        }

        tempGroup.clear(ranking[i]);
        tempSize--;
      }

      if (verbose) {
        System.out.println("----------");
      }

      //handle stopping criteria
      if (!improvement || (forceResultSize == bestSize)) {
        stale++;
      }

      if ((forceResultSize > 0) && (bestSize == forceResultSize)) {
        break;
      }
    }

    if (verbose) {
      System.out.println("Best Group: ");
      printGroup(bestGroup, data.numAttributes());
      System.out.println();
    }

    m_bestGroup = bestGroup;
    m_bestMerit = bestMerit;
    m_evalsTotal += evalsTotal;
    m_evalsCached += evalsCached;

    return bestGroup;
  }

  /**
   * Performs linear floating forward selection
   * ( the stopping criteria cannot be changed to a specific size value )
   *
   *
   * @param cacheSize         chacheSize (times number of instances) to store already evaluated sets
   * @param startGroup        start group for search (can be null)
   * @param ranking                ranking of attributes (as produced by rankAttributes), no ranking would be [0,1,2,3,4..]
   * @param k                                number of top k attributes that are taken into account
   * @param incrementK        true -> fixed-set, false -> fixed-width
   * @param maxStale                number of times the search proceeds even though no improvement was found (1 = hill-climbing)
   * @param data
   * @param evaluator
   * @param verbose
   * @return                                BitSet, that cotains the best-group found
   * @throws Exception
   */
  public BitSet floatingForwardSearch(int cacheSize, BitSet startGroup,
                                      int[] ranking, int k, boolean incrementK, int maxStale, Instances data,
                                      SubsetEvaluator evaluator, boolean verbose) throws Exception {
    if (verbose) {
      System.out.println("Starting floating forward selection");
    }

    BitSet bestGroup;
    BitSet tempGroup;
    int bestSize = 0;
    int tempSize = 0;
    double bestMerit;
    double tempMerit = 0;
    Link2 link;
    LinkedList2 list = new LinkedList2(maxStale);
    Hashtable alreadyExpanded = new Hashtable(cacheSize * data.numAttributes());
    int insertCount = 0;
    int backtrackingSteps = 0;
    boolean improvement;
    boolean backward;
    int thisK = k;
    int evalsTotal = 0;
    int evalsCached = 0;

    bestGroup = (BitSet) startGroup.clone();

    String hashKey = bestGroup.toString();
    bestMerit = evaluator.evaluateSubset(bestGroup);

    if (verbose) {
      System.out.print("Group: ");
      printGroup(bestGroup, data.numAttributes());
      System.out.println("Merit: " + tempMerit);
      System.out.println("----------");
    }

    alreadyExpanded.put(hashKey, new Double(bestMerit));
    insertCount++;
    bestSize = bestGroup.cardinality();

    if (maxStale > 1) {
      Object[] best = new Object[1];
      best[0] = bestGroup.clone();
      list.addToList(best, bestMerit);
    }

    backward = improvement = true;

    while (true) {
      // we are search in backward direction -> 
      // continue backward search as long as a new best set is found
      if (backward) {
        if (!improvement) {
          backward = false;
        }
      }
      // we are searching forward ->  
      // stop search or start backward step
      else {
        if (!improvement && (backtrackingSteps >= maxStale)) {
          break;
        }

        backward = true;
      }

      improvement = false;

      // best-first: take first elem from list
      if (maxStale > 1) {
        if (list.size() == 0) {
          backtrackingSteps = maxStale;

          break;
        }

        link = list.getLinkAt(0);
        tempGroup = (BitSet) (link.getData()[0]);
        tempGroup = (BitSet) tempGroup.clone();
        list.removeLinkAt(0);

        tempSize = 0;

        for (int i = 0; i < data.numAttributes(); i++) {
          if (tempGroup.get(i)) {
            tempSize++;
          }
        }
      } else //hill-climbing
        {
          tempGroup = (BitSet) bestGroup.clone();
          tempSize = bestSize;
        }

      //backward search only makes sense for set-size bigger than 2
      if (backward && (tempSize <= 2)) {
        backward = false;
      }

      //set number of top k attributes that are taken into account
      if (incrementK) {
        thisK = Math.max(thisK,
                         Math.min(Math.max(thisK, k + tempSize), data.numAttributes()));
      } else {
        thisK = k;
      }

      //temporarilly add/remove attributes to/from current set
      for (int i = 0; i < thisK; i++) {
        if (ranking[i] == data.classIndex()) {
          continue;
        }

        if (backward) {
          if (!tempGroup.get(ranking[i])) {
            continue;
          }

          tempGroup.clear(ranking[i]);
          tempSize--;
        } else {
          if ((ranking[i] == data.classIndex()) || tempGroup.get(ranking[i])) {
            continue;
          }

          tempGroup.set(ranking[i]);
          tempSize++;
        }

        hashKey = tempGroup.toString();

        if (!alreadyExpanded.containsKey(hashKey)) {
          evalsTotal++;
          tempMerit = evaluator.evaluateSubset(tempGroup);

          if (insertCount > (cacheSize * data.numAttributes())) {
            alreadyExpanded = new Hashtable(cacheSize * data.numAttributes());
            insertCount = 0;
          }

          alreadyExpanded.put(hashKey, new Double(tempMerit));
          insertCount++;
        } else {
          evalsCached++;
          tempMerit = ((Double) alreadyExpanded.get(hashKey)).doubleValue();
        }

        if (verbose) {
          System.out.print("Group: ");
          printGroup(tempGroup, data.numAttributes());
          System.out.println("Merit: " + tempMerit);
        }

        if ((tempMerit - bestMerit) > 0.00001) {
          improvement = true;
          backtrackingSteps = 0;
          bestMerit = tempMerit;
          bestSize = tempSize;
          bestGroup = (BitSet) (tempGroup.clone());
        }

        if (maxStale > 1) {
          Object[] add = new Object[1];
          add[0] = tempGroup.clone();
          list.addToList(add, tempMerit);
        }

        if (backward) {
          tempGroup.set(ranking[i]);
          tempSize++;
        } else {
          tempGroup.clear(ranking[i]);
          tempSize--;
        }
      }

      if (verbose) {
        System.out.println("----------");
      }

      if ((maxStale > 1) && backward && !improvement) {
        Object[] add = new Object[1];
        add[0] = tempGroup.clone();
        list.addToList(add, Double.MAX_VALUE);
      }

      if (!backward && !improvement) {
        backtrackingSteps++;
      }
    }

    if (verbose) {
      System.out.println("Best Group: ");
      printGroup(bestGroup, data.numAttributes());
      System.out.println();
    }

    m_bestGroup = bestGroup;
    m_bestMerit = bestMerit;
    m_evalsTotal += evalsTotal;
    m_evalsCached += evalsCached;

    return bestGroup;
  }

  /**
   * Debug-out
   */
  protected static void printGroup(BitSet tt, int numAttribs) {
    System.out.print("{ ");

    for (int i = 0; i < numAttribs; i++) {
      if (tt.get(i) == true) {
        System.out.print((i + 1) + " ");
      }
    }

    System.out.println("}");
  }

  // Inner classes
  /**
   * Class for a node in a linked list. Used in best first search.
   * Copied from BestFirstSearch
   *
   * @author Mark Hall (mhall@cs.waikato.ac.nz)
   */
  public class Link2
    implements Serializable, RevisionHandler {
    
    /** for serialization. */
    private static final long serialVersionUID = -7422719407475185086L;
    
    /* BitSet group; */
    Object[] m_data;
    double m_merit;

    // Constructor
    public Link2(Object[] data, double mer) {
      // group = (BitSet)gr.clone();
      m_data = data;
      m_merit = mer;
    }

    /** Get a group */
    public Object[] getData() {
      return m_data;
    }

    public String toString() {
      return ("Node: " + m_data.toString() + "  " + m_merit);
    }
    
    /**
     * Returns the revision string.
     * 
     * @return		the revision
     */
    public String getRevision() {
      return RevisionUtils.extract("$Revision: 1.3 $");
    }
  }

  /**
   * Class for handling a linked list. Used in best first search. Extends the
   * Vector class.
   *
   * @author Mark Hall (mhall@cs.waikato.ac.nz)
   */
  public class LinkedList2
    extends FastVector {
    
    /** for serialization. */
    private static final long serialVersionUID = -7776010892419656105L;
    
    // Max number of elements in the list
    int m_MaxSize;

    // ================
    // Public methods
    // ================
    public LinkedList2(int sz) {
      super();
      m_MaxSize = sz;
    }

    /**
     * removes an element (Link) at a specific index from the list.
     *
     * @param index
     *            the index of the element to be removed.
     */
    public void removeLinkAt(int index) throws Exception {
      if ((index >= 0) && (index < size())) {
        removeElementAt(index);
      } else {
        throw new Exception("index out of range (removeLinkAt)");
      }
    }

    /**
     * returns the element (Link) at a specific index from the list.
     *
     * @param index
     *            the index of the element to be returned.
     */
    public Link2 getLinkAt(int index) throws Exception {
      if (size() == 0) {
        throw new Exception("List is empty (getLinkAt)");
      } else {
        if ((index >= 0) && (index < size())) {
          return ((Link2) (elementAt(index)));
        } else {
          throw new Exception("index out of range (getLinkAt)");
        }
      }
    }

    /**
     * adds an element (Link) to the list.
     *
     * @param gr
     *            the attribute set specification
     * @param mer
     *            the "merit" of this attribute set
     */
    public void addToList(Object[] data, double mer) throws Exception {
      Link2 newL = new Link2(data, mer);

      if (size() == 0) {
        addElement(newL);
      } else {
        if (mer > ((Link2) (firstElement())).m_merit) {
          if (size() == m_MaxSize) {
            removeLinkAt(m_MaxSize - 1);
          }

          // ----------
          insertElementAt(newL, 0);
        } else {
          int i = 0;
          int size = size();
          boolean done = false;

          // ------------
          // don't insert if list contains max elements an this
          // is worst than the last
          if ((size == m_MaxSize) &&
              (mer <= ((Link2) (lastElement())).m_merit)) {
          }
          // ---------------
          else {
            while ((!done) && (i < size)) {
              if (mer > ((Link2) (elementAt(i))).m_merit) {
                if (size == m_MaxSize) {
                  removeLinkAt(m_MaxSize - 1);
                }

                // ---------------------
                insertElementAt(newL, i);
                done = true;
              } else {
                if (i == (size - 1)) {
                  addElement(newL);
                  done = true;
                } else {
                  i++;
                }
              }
            }
          }
        }
      }
    }
    
    /**
     * Returns the revision string.
     * 
     * @return		the revision
     */
    public String getRevision() {
      return RevisionUtils.extract("$Revision: 1.3 $");
    }
  }
  
  /**
   * Returns the revision string.
   * 
   * @return		the revision
   */
  public String getRevision() {
    return RevisionUtils.extract("$Revision: 1.3 $");
  }
}
