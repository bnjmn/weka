/*
 *    Queue.java
 *    Copyright (C) 1999 Len Trigg
 *
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

package weka.core;

import java.io.*;

/** 
 * Class representing a FIFO queue.
 *
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @version 1.0
 */
public class Queue extends Object implements Serializable {

  protected class QueueNode implements Serializable {

    protected QueueNode m_Next;
    protected Object m_Contents;

    public QueueNode(Object contents) {

      m_Contents = contents;
      next(null);
    }

    public QueueNode next(QueueNode next) {

      return m_Next = next;
    }

    public QueueNode next() {

      return m_Next;
    }

    public Object contents(Object contents) {

      return m_Contents = contents;
    }

    public Object contents() {
      return m_Contents;
    }
  }

  // =================
  // Protected members
  // =================

  /** Store a reference to the head of the queue */
  protected QueueNode m_Head = null;

  /** Store a reference to the tail of the queue */
  protected QueueNode m_Tail = null;

  /** Store the current number of elements in the queue */
  protected int m_Size = 0;

  // ===============
  // Public methods.
  // ===============

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

  // ============
  // Test method.
  // ============

  /**
   * Main method for testing this class.
   *
   * @param argv should contain arguments to the filter: use -h for help
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








