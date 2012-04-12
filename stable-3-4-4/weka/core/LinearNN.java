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
 *    Copyright (C) 1999-2005 University of Waikato
 */


package weka.core;

import java.util.Enumeration;
import java.util.Vector;

/**
 * Linear search algorithm for nearest neighbours.
 *
 * @author  Ashraf M. Kibriya (amk14@waikato.ac.nz)
 * @version $Revision: 1.1 $
 */
public class LinearNN extends NearestNeighbourSearch {

  /**
   * A class for a heap to store the nearest k neighbours to an instance. 
   * The heap also takes care of cases where multiple neighbours are the same 
   * distance away.
   * i.e. the minimum size of the heap is k.
   */
  private class MyHeap {
    //m_heap[0].index containts the current size of the heap
    //m_heap[0].distance is unused.
    MyHeapElement m_heap[] = null;
    public MyHeap(int maxSize) {
      if((maxSize%2)==0)
        maxSize++;
      
      m_heap = new MyHeapElement[maxSize+1];
      m_heap[0] = new MyHeapElement(0, 0);
      //System.err.println("m_heap size is: "+m_heap.length);
    }
    public int size() {
      return m_heap[0].index;
    }
    public MyHeapElement peek() {
      return m_heap[1];
    }
    public MyHeapElement get() throws Exception  {
      if(m_heap[0].index==0)
        throw new Exception("No elements present in the heap");
      MyHeapElement r = m_heap[1];
      m_heap[1] = m_heap[m_heap[0].index];
      m_heap[0].index--;
      downheap();
      return r;
    }
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
      
      //      if(temp != null) {
      //        if(temp.distance == m_heap[1].distance)
      //          putKthNearest(temp.index, temp.distance);
      //        else if(temp.distance > m_heap[1].distance) {
      //          m_KthNearest = null;
      //          m_KthNearestSize = 0;
      //        }
      //      }
      
    }
    
    public void putBySubstitute(int i, double d) throws Exception {
      //      MyHeapElement me = m_heap[1];
      //      me.index = i;
      //      me.distance = d;
      //      downheap();
      //      me = m_heap[1];
      //      if(m_heap[0].index == (m_heap.length-1))
      //        if(distance == me.distance)
      //          putKthNearest(index, me.distance);
      //        else if(distance > me.distance) {
      //          m_KthNearest = null;
      //          m_KthNearestSize = 0;
      //        }
      
      MyHeapElement head = get();
      put(i, d);
      //      System.out.println("previous: "+head.distance+" current: "+m_heap[1].distance);
      if(head.distance == m_heap[1].distance) { //Utils.eq(head.distance, m_heap[1].distance)) {
        putKthNearest(head.index, head.distance);
      }
      else if(head.distance > m_heap[1].distance) { //Utils.gr(head.distance, m_heap[1].distance)) {
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
    int index;
    double distance;
    public MyHeapElement(int i, double d) {
      distance = d; index = i;
    }
  }
  
  
  /**
   * A class for storing data about a neighboring instance
   */ //better to change this into a heap element
  private class NeighborNode {

    /** The neighbor instance */
    private Instance m_Instance;

    /** The distance from the current instance to this neighbor */
    private double m_Distance;

    /** A link to the next neighbor instance */
    private NeighborNode m_Next;
    
    /**
     * Create a new neighbor node.
     *
     * @param distance the distance to the neighbor
     * @param instance the neighbor instance
     * @param next the next neighbor node
     */
    public NeighborNode(double distance, Instance instance, NeighborNode next){
      m_Distance = distance;
      m_Instance = instance;
      m_Next = next;
    }

    /**
     * Create a new neighbor node that doesn't link to any other nodes.
     *
     * @param distance the distance to the neighbor
     * @param instance the neighbor instance
     */
    public NeighborNode(double distance, Instance instance) {

      this(distance, instance, null);
    }
  } 

  /**
   * A class for a linked list to store the nearest k neighbours
   * to an instance. We use a list so that we can take care of
   * cases where multiple neighbours are the same distance away.
   * i.e. the minimum length of the list is k.
   */ //better to change this into a heap
  private class NeighborList {

    /** The first node in the list */
    private NeighborNode m_First;

    /** The last node in the list */
    private NeighborNode m_Last;

    /** The number of nodes to attempt to maintain in the list */
    private int m_Length = 1;
        
    /**
     * Creates the neighborlist with a desired length
     *
     * @param length the length of list to attempt to maintain
     */
    public NeighborList(int length) {
      m_Length = length;
    }

    /**
     * Gets whether the list is empty.
     *
     * @return true if so
     */
    public boolean isEmpty() {
      return (m_First == null);
    }

    /**
     * Gets the current length of the list.
     *
     * @return the current length of the list
     */
    public int currentLength() {
      int i = 0;
      NeighborNode current = m_First;
      while (current != null) {
	i++;
	current = current.m_Next;
      }
      return i;
    }

    /**
     * Inserts an instance neighbor into the list, maintaining the list
     * sorted by distance.
     *
     * @param distance the distance to the instance
     * @param instance the neighboring instance
     */
    public void insertSorted(double distance, Instance instance) {

      if (isEmpty()) {
	m_First = m_Last = new NeighborNode(distance, instance);
      } else {
	NeighborNode current = m_First;
	if (distance < m_First.m_Distance) {// Insert at head
	  m_First = new NeighborNode(distance, instance, m_First);
	} else { // Insert further down the list
	  for( ;(current.m_Next != null) && 
		 (current.m_Next.m_Distance < distance); 
	       current = current.m_Next);
	  current.m_Next = new NeighborNode(distance, instance,
					    current.m_Next);
	  if (current.equals(m_Last)) {
	    m_Last = current.m_Next;
	  }
	}

	// Trip down the list until we've got k list elements (or more if the
	// distance to the last elements is the same).
	int valcount = 0;
	for(current = m_First; current.m_Next != null; 
	    current = current.m_Next) {
	  valcount++;
	  if ((valcount >= m_Length) && (current.m_Distance != 
					 current.m_Next.m_Distance)) {
	    m_Last = current;
	    current.m_Next = null;
	    break;
	  }
	}
      }
    }

    /**
     * Prunes the list to contain the k nearest neighbors. If there are
     * multiple neighbors at the k'th distance, all will be kept.
     *
     * @param k the number of neighbors to keep in the list.
     */
    public void pruneToK(int k) {

      if (isEmpty()) {
	return;
      }
      if (k < 1) {
	k = 1;
      }
      int currentK = 0;
      double currentDist = m_First.m_Distance;
      NeighborNode current = m_First;
      for(; current.m_Next != null; current = current.m_Next) {
	currentK++;
	currentDist = current.m_Distance;
	if ((currentK >= k) && (currentDist != current.m_Next.m_Distance)) {
	  m_Last = current;
	  current.m_Next = null;
	  break;
	}
      }
    }

    /**
     * Prints out the contents of the neighborlist
     */
    public void printList() {

      if (isEmpty()) {
	System.out.println("Empty list");
      } else {
	NeighborNode current = m_First;
	while (current != null) {
	  System.out.println("Node: instance " + current.m_Instance 
			     + ", distance " + current.m_Distance);
	  current = current.m_Next;
	}
	System.out.println();
      }
    }
  }

  //debug
  private int m_Debug=0;

  /** Array holding the distances of the nearest neighbours. It is filled up
   *  both by nearestNeighbour() and kNearestNeighbours(). 
   */
  private double[] m_Distances;
    
  /** Constructor */
  public LinearNN() {
    super();
  }
  
  /** Constructor */
  public LinearNN(Instances insts) {
    super(insts);
    m_DistanceFunction.setInstances(insts);
  }
  
  /** Returns the nearest instance in the current neighbourhood to the supplied
   *  instance.
   * @param target - The instance to find the nearest neighbour for.
   * @exception - Throws an exception if the nearest neighbour could not be 
   *              found.
   */
  public Instance nearestNeighbour(Instance target) throws Exception {
    return (kNearestNeighbours(target, 1)).instance(0);
  }
  
  /** Returns k nearest instances in the current neighbourhood to the supplied
   *  instance.
   * @param target - The instance to find the k nearest neighbours for.
   * @param k - The number of nearest neighbours to find.
   * @exception - Throws an exception if the neighbours could not be found.
   */
  public Instances kNearestNeighbours(Instance target, int kNN) throws Exception {
  
    //debug
    boolean print=false;
//    if(target.value(0)==618) {  //target.toString().startsWith("1,1,1,1,1,3.544656,1,1,1")) {
////      m_Debug++;
////      if(m_Debug==3)
//        print=true;
//    }
//    else
//      print=false;
   
//    double distance;
//    NeighborList neighbourlist = new NeighborList(kNN);
//    Enumeration enum = m_Instances.enumerateInstances();
//    int i = 0;
//
//    while (enum.hasMoreElements()) {
//      Instance trainInstance = (Instance) enum.nextElement();
//      if (target != trainInstance) { // for hold-one-out cross-validation
////        if(target.value(0)==(308-1) && trainInstance.value(0)==(300-1)) {
////          print=true;
////        }
//        if(print==true) {
//          System.out.println("K: "+neighbourlist.currentLength());
//          if(neighbourlist.m_Last!=null)
//            System.out.println(" Max: "+neighbourlist.m_Last.m_Distance);
//        }
//        if(neighbourlist.isEmpty() || i < kNN)
//          distance = m_DistanceFunction.distance(target, trainInstance, print);
//        else
//          distance = m_DistanceFunction.distance(target, trainInstance, neighbourlist.m_Last.m_Distance, print);
//        
//	if (neighbourlist.isEmpty() || (i < kNN) || 
//	    (distance <= neighbourlist.m_Last.m_Distance)) {
//          if(print==true)
//            System.out.println("inserting: "+distance);
//	  neighbourlist.insertSorted(distance, trainInstance);
//	}
//	i++;
//      }
//    }
//
//    i = neighbourlist.currentLength();
//    Instances neighbours = new Instances(m_Instances, i);
//    m_Distances = new double[i];
//    NeighborNode current = neighbourlist.m_First;
//    i=0;
//    
//    while(current!=null) {
//      neighbours.add(current.m_Instance);
//      m_Distances[i] = current.m_Distance;
//      current = current.m_Next;
//      i++;
//    }

    MyHeap heap = new MyHeap(kNN);
    double distance; 
    for(int i=0; i<m_Instances.numInstances(); i++) {
      if(target == m_Instances.instance(i)) //for hold-one-out cross-validation
        continue;
      if(i<kNN) {
        if(print==true)
          System.out.println("K: "+(heap.size()+heap.noOfKthNearest()));
        distance = ((EuclideanDistance)m_DistanceFunction).distance(target, m_Instances.instance(i), Double.MAX_VALUE, print); //maxDistance, print); //null
        heap.put(i, distance);
      }
      else {
        MyHeapElement temp = heap.peek();
        if(print==true)
          System.out.println("K: "+(heap.size()+heap.noOfKthNearest()));
        distance = ((EuclideanDistance)m_DistanceFunction).distance(target, m_Instances.instance(i), temp.distance, print); //maxDistance, print);
        if(distance < temp.distance) { //Utils.sm(distance, temp.distance)) {
          //              h.get();
          //              h.put(i, distance[i]);
          heap.putBySubstitute(i, distance);
        }
        else if(distance == temp.distance) { //Utils.eq(distance, temp.distance)) {
          heap.putKthNearest(i, distance);
        }

      }
    }
    
    Instances neighbours = new Instances(m_Instances, (heap.size()+heap.noOfKthNearest()));
    m_Distances = new double[heap.size()+heap.noOfKthNearest()];
    int [] indices = new int[heap.size()+heap.noOfKthNearest()];
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
    
    //combSort11(m_Distances, indices);
    //super.insertionSort(indices, m_Distances);
    //super.insertionSort(m_Distances, indices);
    m_DistanceFunction.postProcessDistances(m_Distances);
    
    for(int k=0; k<indices.length; k++) {
      neighbours.add(m_Instances.instance(indices[k]));
    }
    
    //debug
//    if(target.value(0)==146) {
//      OOPS("Target: "+target+" found "+neighbours.numInstances()+" neighbours\n");
//      for(int k=0; k<neighbours.numInstances(); k++) {
//        System.out.println("Node: instance "+neighbours.instance(k)+", distance "+
//        m_Distances[k]);
//      }
//      System.out.println("");
//    }
     
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
   * @returns - distances[] array containing the distances of the 
   *            nearestNeighbours. The length and ordering of the array is the 
   *            same as that of the instances returned by nearestNeighbour 
   *            functions.
   * @exception Throws an exception if called before calling kNearestNeighbours
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
   * @param insts - The set of instances on which the nearest neighbour search
   * is carried out. Usually this set is the training set. 
   */
  public void setInstances(Instances insts) throws Exception {
    m_Instances = insts;
    m_DistanceFunction.setInstances(insts);
  }
  
  /** 
   * Updates the LinearNN to cater for the new added instance. This 
   * implementation only updates the ranges of the EuclideanDistance class, 
   * since our set of instances is passed by reference and should already have 
   * the newly added instance.
   * @param ins - The instance to add. Usually this is the instance that is
   * added to our neighbourhood i.e. the training instances.
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
   * @param ins - The instance to add the information of. Usually this is
   * the test instance supplied to update the range of attributes in the 
   * distance function.
   */
  public void addInstanceInfo(Instance ins) {
    if(m_Instances!=null)
      try{ update(ins); }
      catch(Exception ex) { ex.printStackTrace(); }
  }
    
  /**
   * Returns a string describing this nearest neighbour search algorithm.
   * @return a description of the algorithm for displaying in the 
   * explorer/experimenter gui
   */
  public String globalInfo() {
    return "Class implementing the brute force search algorithm for nearest "+
           "neighbour search.";  
  }

  /**  method for outputting error messages */
  private void OOPS(String s) {
    System.out.println(s);
  }
  
}
