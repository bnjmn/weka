/*
 *    Queue.java
 *    Copyright (C) 1999 Len Trigg
 *
 */

package weka.core;

import java.io.*;

/** 
 * Class representing a FIFO queue.
 *
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @version $Revision: 1.4 $
 */
public class Queue extends Object implements Serializable {

  /**
   * Represents one node in the queue.
   */
  protected class QueueNode implements Serializable {

    /** The next node in the queue */
    protected QueueNode m_Next;

    /** The nodes contents */
    protected Object m_Contents;

    /** 
     * Creates a queue node with the given contents 
     */
    public QueueNode(Object contents) {

      m_Contents = contents;
      next(null);
    }

    /**
     * Sets the next node in the queue, and returns it.
     */
    public QueueNode next(QueueNode next) {

      return m_Next = next;
    }

    /**
     * Gets the next node in the queue. 
     */
    public QueueNode next() {

      return m_Next;
    }

    /**
     * Sets the contents of the node.
     */
    public Object contents(Object contents) {

      return m_Contents = contents;
    }

    /**
     * Returns the contents in the node.
     */
    public Object contents() {

      return m_Contents;
    }
  }

  /** Store a reference to the head of the queue */
  protected QueueNode m_Head = null;

  /** Store a reference to the tail of the queue */
  protected QueueNode m_Tail = null;

  /** Store the current number of elements in the queue */
  protected int m_Size = 0;

  /**
   * Removes all objects from the queue.
   */
  public final synchronized void removeAllElements() {

    m_Size = 0;
    m_Head = null;
    m_Tail = null;
  }

  /**
   * Appends an object to the back of the queue.
   *
   * @param item the object to be appended
   * @return the object appended
   */
  public synchronized Object push(Object item) {

    QueueNode newNode = new QueueNode(item);
    if (m_Head == null) {
      m_Head = m_Tail = newNode;
    } else {
      m_Tail = m_Tail.next(newNode);
    }
    m_Size++;
    return item;
  }

  /**
   * Pops an object from the front of the queue.
   *
   * @return the object at the front of the queue
   * @exception Exception if the queue is empty
   */
  public synchronized Object pop() throws Exception {

    if (m_Head == null) {
      throw new Exception("Queue is empty");
    }
    Object retval = m_Head.contents();
    m_Size--;
    m_Head = m_Head.next();
    if (m_Head == null) {
      m_Tail = null;
    }
    return retval;
  }

  /**
   * Gets object from the front of the queue.
   *
   * @return the object at the front of the queue
   * @exception Exception if the queue is empty
   */
  public synchronized Object peek() throws Exception {
    
    if (m_Head == null) {
      throw new Exception("Queue is empty");
    }
    return m_Head.contents();
  }

  /**
   * Checks if queue is empty.
   * 
   * @return true if queue is empty
   */
  public boolean empty() {

    return (m_Head == null);
  }

  /** 
   * Gets queue's size.
   *
   * @return size of queue
   */
  public int size() {

    return m_Size;
  }

  /**
   * Produces textual description of queue.
   *
   * @return textual description of queue
   */
  public String toString() {

    String retval = "Queue Contents "+m_Size+" elements\n";
    QueueNode current = m_Head;
    if (current == null) {
      return retval + "Empty\n";
    } else {
      while (current != null) {
	retval += current.contents().toString()+"\n";
	current = current.next();
      }
    }
    return retval;
  }

  /**
   * Main method for testing this class.
   *
   * @param argv a set of strings that are pushed on a test queue
   */
  public static void main(String [] argv) {

    try {
      Queue queue = new Queue();
      for(int i = 0; i < argv.length; i++) {
	queue.push(argv[i]);
      }
      System.out.println("After Pushing");
      System.out.println(queue.toString());
      System.out.println("Popping...");
      while (!queue.empty()) {
	System.out.println(queue.pop().toString());
      }
    } catch (Exception ex) {
      System.out.println(ex.getMessage());
    }
  }
}








