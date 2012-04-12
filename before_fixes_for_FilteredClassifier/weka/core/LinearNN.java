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
 *    LinearNN.java
 *    Copyright (C) 1999-2006 University of Waikato
 */

package weka.core;

import java.util.Enumeration;
import java.util.Vector;

/**
 <!-- globalinfo-start -->
 * Class implementing the brute force search algorithm for nearest neighbour search.
 * <p/>
 <!-- globalinfo-end -->
 * 
 <!-- options-start -->
 * Valid options are: <p/>
 * 
 * <pre> -S
 *  Skip identical instances (distances equal to zero).
 * </pre>
 * 
 <!-- options-end -->
 *
 * @author  Ashraf M. Kibriya (amk14@waikato.ac.nz)
 * @version $Revision: 1.4 $
 */
public class LinearNN
  extends NearestNeighbourSearch
  implements OptionHandler {
  
  /** for serialization */
  static final long serialVersionUID = -7318654963683219025L;

  /**
   * A class for a heap to store the nearest k neighbours to an instance. 
   * The heap also takes care of cases where multiple neighbours are the same 
   * distance away.
   * i.e. the minimum size of the heap is k.
   */
  private class MyHeap {
    //m_heap[0].index containts the current size of the heap
    //m_heap[0].distance is unused.
    /** the heap elements - first one contains the size */
    MyHeapElement m_heap[] = null;
    
    /**
     * constructor
     * 
     * @param maxSize		the max size of the heap
     */
    public MyHeap(int maxSize) {
      if((maxSize%2)==0)
        maxSize++;
      
      m_heap = new MyHeapElement[maxSize+1];
      m_heap[0] = new MyHeapElement(0, 0);
      //System.err.println("m_heap size is: "+m_heap.length);
    }
    
    /**
     * the size of the heap
     * 
     * @return the size
     */
    public int size() {
      return m_heap[0].index;
    }
    
    /**
     * peek at the first element
     * 
     * @return the first element
     */
    public MyHeapElement peek() {
      return m_heap[1];
    }
    
    /**
     * returns the next element on the heap and removes it
     * 
     * @return the next element
     * @throws Exception if no elements in heap
     */
    public MyHeapElement get() throws Exception  {
      if(m_heap[0].index==0)
        throw new Exception("No elements present in the heap");
      MyHeapElement r = m_heap[1];
      m_heap[1] = m_heap[m_heap[0].index];
      m_heap[0].index--;
      downheap();
      return r;
    }
    
    /**
     * adds a new element with the given values to the heap
     * 
     * @param i			the index
     * @param d			the distance
     * @throws Exception 	if index is to large
     */
    public void put(int i, double d) throws Exception {
      if((m_heap[0].index+1)>(m_heap.length-1))
        throw new Exception("the number of elements cannot exceed the "+
        "initially set maximum limit");
      m_heap[0].index++;
      //      MyHeapElement temp = m_heap[m_heap[0].index];
      m_heap[m_heap[0].index] = new MyHeapElement(i, d);
      //m_heap[m_heap[0].index].index = i;
      //m_heap[m_heap[0].index].distance = d;
      //System.err.print("new size: "+(int)m_heap[0]+", ");
      upheap();
    }
    
    /**
     * 
     * @param i			the index
     * @param d			the distance
     * @throws Exception	if it cannot be substituted
     */
    public void putBySubstitute(int i, double d) throws Exception {
      MyHeapElement head = get();
      put(i, d);
      if(head.distance == m_heap[1].distance) {
        putKthNearest(head.index, head.distance);
      }
      else if(head.distance > m_heap[1].distance) {
        m_KthNearest = null;
        m_KthNearestSize = 0;
        initSize = 10;
      }
      else if(head.distance < m_heap[1].distance) {
        throw new Exception("The substituted element is smaller than the "+
        "head element. put() should have been called "+
        "in place of putBySubstitute()");
      }
    }
    
    MyHeapElement m_KthNearest[] = null;
    
    int m_KthNearestSize = 0;
    
    int initSize=10;
    
    public int noOfKthNearest() {
      return m_KthNearestSize;
    }
    
    /**
     * 
     * @param i		the index
     * @param d		the distance
     */
    public void putKthNearest(int i,  double d) {
      if(m_KthNearest==null) {
        m_KthNearest = new MyHeapElement[initSize];
      }
      if(m_KthNearestSize>=m_KthNearest.length) {
        initSize += initSize;
        MyHeapElement temp[] = new MyHeapElement[initSize];
        System.arraycopy(m_KthNearest, 0, temp, 0, m_KthNearest.length);
        m_KthNearest = temp;
      }
      m_KthNearest[m_KthNearestSize++] = new MyHeapElement(i, d);
    }
    
    /**
     * returns the Kth nearest element
     * 
     * @return the Kth nearest elemen
     */
    public MyHeapElement getKthNearest() {
      if(m_KthNearestSize==0)
        return null;
      m_KthNearestSize--;
      return m_KthNearest[m_KthNearestSize];
    }
    
    private void upheap() {
      int i = m_heap[0].index;
      MyHeapElement temp;
      while( i > 1  && m_heap[i].distance>m_heap[i/2].distance) {
        temp = m_heap[i];
        m_heap[i] = m_heap[i/2];
        i = i/2;
        m_heap[i] = temp; //this is i/2 done here to avoid another division.
      }
    }
    
    private void downheap() {
      int i = 1;
      MyHeapElement temp;
      while( ( (2*i) <= m_heap[0].index &&
      m_heap[i].distance < m_heap[2*i].distance )
      ||
      ( (2*i+1) <= m_heap[0].index &&
      m_heap[i].distance < m_heap[2*i+1].distance) ) {
        if((2*i+1)<=m_heap[0].index) {
          if(m_heap[2*i].distance>m_heap[2*i+1].distance) {
            temp = m_heap[i];
            m_heap[i] = m_heap[2*i];
            i = 2*i;
            m_heap[i] = temp;
          }
          else {
            temp = m_heap[i];
            m_heap[i] = m_heap[2*i+1];
            i = 2*i+1;
            m_heap[i] = temp;
          }
        }
        else {
          temp = m_heap[i];
          m_heap[i] = m_heap[2*i];
          i = 2*i;
          m_heap[i] = temp;
        }
      }
    }
  }
  
  /**
   * A class for storing data about a neighboring instance
   */
  private class MyHeapElement {
    /** the index */
    int index;
    
    /** the distance */
    double distance;
    
    /**
     * constructir
     * 
     * @param i the index
     * @param d the distance
     */
    public MyHeapElement(int i, double d) {
      distance = d; index = i;
    }
  }
  

  /** Array holding the distances of the nearest neighbours. It is filled up
   *  both by nearestNeighbour() and kNearestNeighbours(). 
   */
  private double[] m_Distances;
  
  
  /** Whether to skip instances from the neighbours that are identical to the query instance. */
  private boolean m_SkipIdentical=false;
  
  /** Constructor */
  public LinearNN() {
    super();
  }
  
  /** 
   * Constructor
   * 
   * @param insts 	the instances to use
   */
  public LinearNN(Instances insts) {
    super(insts);
    m_DistanceFunction.setInstances(insts);
  }
  
  /**
   * Returns a string describing this nearest neighbour search algorithm.
   * 
   * @return a description of the algorithm for displaying in the 
   * explorer/experimenter gui
   */
  public String globalInfo() {
    return "Class implementing the brute force search algorithm for nearest "+
           "neighbour search.";  
  }
  
  /**
   * Returns an enumeration describing the available options.
   *
   * @return an enumeration of all the available options.
   */
  public Enumeration listOptions() {
    Vector newVector = new Vector();
    
    newVector.add(new Option(
	"\tSkip identical instances (distances equal to zero).\n",
	"S", 1,"-S"));
    
    return newVector.elements();
  }
  
  /**
   * Parses a given list of options. <p/>
   *
   <!-- options-start -->
   * Valid options are: <p/>
   * 
   * <pre> -S
   *  Skip identical instances (distances equal to zero).
   * </pre>
   * 
   <!-- options-end -->
   *
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {
    super.setOptions(options);

    setSkipIdentical(Utils.getFlag('S', options));
  }

  /**
   * Gets the current settings.
   *
   * @return an array of strings suitable for passing to setOptions()
   */
  public String[] getOptions() {
    Vector        result;
    String[]      options;
    int           i;
    
    result = new Vector();
    
    options = super.getOptions();
    for (i = 0; i < options.length; i++)
      result.add(options[i]);
    
    if (getSkipIdentical())
      result.add("-S");

    return (String[]) result.toArray(new String[result.size()]);
  }
  
  /** 
   * Returns the nearest instance in the current neighbourhood to the supplied
   * instance.
   * 
   * @param target The instance to find the nearest neighbour for.
   * @return the nearest instance
   * @throws Exception if the nearest neighbour could not be found.
   */
  public Instance nearestNeighbour(Instance target) throws Exception {
    return (kNearestNeighbours(target, 1)).instance(0);
  }
  
  /** 
   * Returns k nearest instances in the current neighbourhood to the supplied
   * instance.
   * 
   * @param target The instance to find the k nearest neighbours for.
   * @param kNN The number of nearest neighbours to find.
   * @return the k nearest instances
   * @throws Exception if the neighbours could not be found.
   */
  public Instances kNearestNeighbours(Instance target, int kNN) throws Exception {
  
    //debug
    boolean print=false;
    
    MyHeap heap = new MyHeap(kNN);
    double distance; 
    int firstkNN=0;
    for(int i=0; i<m_Instances.numInstances(); i++) {
      if(target == m_Instances.instance(i)) //for hold-one-out cross-validation
        continue;
      if(firstkNN<kNN) {
        if(print==true)
          System.out.println("K: "+(heap.size()+heap.noOfKthNearest()));
        distance = ((EuclideanDistance)m_DistanceFunction).distance(target, m_Instances.instance(i), Double.MAX_VALUE, print);
        if(m_SkipIdentical && distance==0.0)
          if(i<m_Instances.numInstances()-1)
            continue;
          else
            heap.put(i, distance);
        heap.put(i, distance);
        firstkNN++;
      }
      else {
        MyHeapElement temp = heap.peek();
        if(print==true)
          System.out.println("K: "+(heap.size()+heap.noOfKthNearest()));
        distance = ((EuclideanDistance)m_DistanceFunction).distance(target, m_Instances.instance(i), temp.distance, print);
        if(m_SkipIdentical && distance==0.0)
          continue;
        if(distance < temp.distance) { 
          heap.putBySubstitute(i, distance);
        }
        else if(distance == temp.distance) { 
          heap.putKthNearest(i, distance);
        }

      }
    }
    
    Instances neighbours = new Instances(m_Instances, (heap.size()+heap.noOfKthNearest()));
    m_Distances = new double[heap.size()+heap.noOfKthNearest()];
    int[] indices = new int[heap.size()+heap.noOfKthNearest()];
    int i=1; MyHeapElement h;
    while(heap.noOfKthNearest()>0) {
      h = heap.getKthNearest();
      indices[indices.length-i] = h.index;
      m_Distances[indices.length-i] = h.distance;
      i++;
    }
    while(heap.size()>0) {
      h = heap.get();
      indices[indices.length-i] = h.index;
      m_Distances[indices.length-i] = h.distance;
      i++;
    }
    
    m_DistanceFunction.postProcessDistances(m_Distances);
    
    for(int k=0; k<indices.length; k++) {
      neighbours.add(m_Instances.instance(indices[k]));
    }
    
    return neighbours;    
  }
  
  /** 
   * Returns the distances of the k nearest neighbours. The kNearestNeighbours
   * or nearestNeighbour must always be called before calling this function. If
   * this function is called before calling either the kNearestNeighbours or 
   * the nearestNeighbour, then it throws an exception. If, however, if either
   * of the nearestNeighbour functions are called at any point in the 
   * past then no exception is thrown and the distances of the training set from
   * the last supplied target instance (to either one of the nearestNeighbour 
   * functions) is/are returned.
   *
   * @return distances[] array containing the distances of the 
   *         nearestNeighbours. The length and ordering of the array is the 
   *         same as that of the instances returned by nearestNeighbour 
   *         functions.
   * @throws Exception an exception if called before calling kNearestNeighbours
   *            or nearestNeighbours.
   */
  public double[] getDistances() throws Exception {
    if(m_Distances==null)
      throw new Exception("No distances available. Please call either "+
                          "kNearestNeighbours or nearestNeighbours first.");
    return m_Distances;    
  }

  /** 
   * Sets the instances comprising the current neighbourhood.
   * 
   * @param insts The set of instances on which the nearest neighbour search
   * is carried out. Usually this set is the training set. 
   */
  public void setInstances(Instances insts) {
    m_Instances = insts;
    m_DistanceFunction.setInstances(insts);
  }
  
  /** 
   * Updates the LinearNN to cater for the new added instance. This 
   * implementation only updates the ranges of the EuclideanDistance class, 
   * since our set of instances is passed by reference and should already have 
   * the newly added instance.
   * 
   * @param ins The instance to add. Usually this is the instance that is
   * added to our neighbourhood i.e. the training instances.
   * @throws Exception if the provided instance is either null or the 
   * distance function cannot work with this instance.
   */
  public void update(Instance ins) throws Exception {
    if(m_Instances==null)
      throw new Exception("No instances supplied yet. Cannot update without"+
                          "supplying a set of instances first.");
    m_DistanceFunction.update(ins);
  }
  
  /** 
   * Adds the given instance info. This implementation updates the range
   * datastructures of the EuclideanDistance.
   * 
   * @param ins The instance to add the information of. Usually this is
   * the test instance supplied to update the range of attributes in the 
   * distance function.
   */
  public void addInstanceInfo(Instance ins) {
    if(m_Instances!=null)
      try{ update(ins); }
      catch(Exception ex) { ex.printStackTrace(); }
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String skipIdenticalTipText() {
    return "Whether to skip identical instances (with distance 0 to the target)";
  }
  
  /**
   * Sets the property to skip identical instances (with distance zero from 
   * the target) from the set of neighbours returned.
   * 
   * @param skip if true, identical intances are skipped
   */
  public void setSkipIdentical(boolean skip) {
    m_SkipIdentical = skip;
  }
  
  /**
   * Gets whether if identical instances are skipped from the neighbourhood.
   * 
   * @return true if identical instances are skipped
   */
  public boolean getSkipIdentical() {
    return m_SkipIdentical;
  }
}
