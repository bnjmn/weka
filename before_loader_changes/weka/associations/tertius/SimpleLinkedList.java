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
 *    SimpleLinkedList.java
 *    Copyright (C) 2003 Peter A. Flach, Nicolas Lachiche
 *
 *    Thanks to Amelie Deltour for porting the original C code to Java
 *    and integrating it into Weka.
 */

package weka.associations.tertius;

import java.io.Serializable;
import java.util.Comparator;
import java.util.NoSuchElementException;

/**
 * @author Peter A. Flach
 * @author Nicolas Lachiche
 * @version $Revision: 1.5 $
 */
public class SimpleLinkedList implements Serializable {

  /** for serialization */
  private static final long serialVersionUID = -1491148276509976299L;

  public class LinkedListIterator
    implements Serializable {
    
    /** for serialization */
    private static final long serialVersionUID = -2448555236100426759L;
    
    Entry current = first;
    Entry lastReturned = null;
    
    public boolean hasNext() {
      return current.next != last;
    }

    public Object next() {
      if (current == last) {
	throw new NoSuchElementException();
      }
      current = current.next;
      lastReturned = current;
      return current.element;
    }

    public void remove() {
      if (lastReturned == last
	  || lastReturned == null) {
	throw new IllegalStateException();
      }
      lastReturned.previous.next = lastReturned.next;
      lastReturned.next.previous = lastReturned.previous;
      current = lastReturned.previous;
      lastReturned = null;
    }

    public void addBefore(Object o) {
      if (lastReturned == null) {
	throw new IllegalStateException();
      }
      Entry newEntry = new Entry(o, lastReturned, lastReturned.previous);
      lastReturned.previous.next = newEntry;
      lastReturned.previous = newEntry;
    }

  }

  public class LinkedListInverseIterator
    implements Serializable {
    
    /** for serialization */
    private static final long serialVersionUID = 6290379064027832108L;
    
    Entry current = last;
    Entry lastReturned = null;
    
    public boolean hasPrevious() {
      return current.previous != first;
    }

    public Object previous() {
      if (current == first) {
	throw new NoSuchElementException();
      }
      current = current.previous;
      lastReturned = current;
      return current.element;
    }

    public void remove() {
      if (lastReturned == first
	  || lastReturned == null) {
	throw new IllegalStateException();
      }
      lastReturned.previous.next = lastReturned.next;
      lastReturned.next.previous = lastReturned.previous;
      current = lastReturned.next;
      lastReturned = null;
    }

  }


  private static class Entry
    implements Serializable {
    
    /** for serialization */
    private static final long serialVersionUID = 7888492479685339831L;
    
    Object element;
    Entry next;
    Entry previous;
    
    Entry(Object element, Entry next, Entry previous) {
      this.element = element;
      this.next = next;
      this.previous = previous;
    }
  }

  private Entry first;
  private Entry last;

  public SimpleLinkedList() {
    first = new Entry(null, null, null);
    last = new Entry(null, null, null);
    first.next = last;
    last.previous = first;
  }

  public Object removeFirst() {
    if (first.next == last) {
      throw new NoSuchElementException();
    }
    Object result = first.next.element;
    first.next.next.previous = first;
    first.next = first.next.next;
    return result;
  }

  public Object getFirst() {
    if (first.next == last) {
      throw new NoSuchElementException();
    }
    return first.next.element;
  }

  public Object getLast() {
    if (last.previous == first) {
      throw new NoSuchElementException();
    }
    return last.previous.element;
  }

  public void addFirst(Object o) {
    Entry newEntry = new Entry(o, first.next, first);
    first.next.previous = newEntry;
    first.next = newEntry;
  }

  public void add(Object o) {
    Entry newEntry = new Entry(o, last, last.previous);
    last.previous.next = newEntry;
    last.previous = newEntry;
  }

  public void addAll(SimpleLinkedList list) {
    last.previous.next = list.first.next;    
    list.first.next.previous = last.previous;
    last = list.last;
  }

  public void clear() {
    first.next = last;
    last.previous = first;
  }

  public boolean isEmpty() {
    return first.next == last;
  }

  public LinkedListIterator iterator() {
    return new LinkedListIterator();
  }

  public LinkedListInverseIterator inverseIterator() {
    return new LinkedListInverseIterator();
  }

  public int size() {
    int result = 0;
    LinkedListIterator iter = new LinkedListIterator();
    while (iter.hasNext()) {
      result++;
      iter.next();
    }
    return result;
  }

  public void merge(SimpleLinkedList list, Comparator comp) {
    LinkedListIterator iter1 = this.iterator();
    LinkedListIterator iter2 = list.iterator();
    Object elem1 = iter1.next();
    Object elem2 = iter2.next();
    while (elem2 != null) {
      if ((elem1 == null)
	  || (comp.compare(elem2, elem1) < 0)) {	    
	iter1.addBefore(elem2);
	elem2 = iter2.next();
      } else {
	elem1 = iter1.next();
      }
    }
  }

  public void sort(Comparator comp) {
    LinkedListIterator iter = this.iterator();
    if (iter.hasNext()) {
      SimpleLinkedList lower = new SimpleLinkedList();
      SimpleLinkedList upper = new SimpleLinkedList();
      Object ref = iter.next();
      Object elem;
      while (iter.hasNext()) {
	elem = iter.next();
	if (comp.compare(elem, ref) < 0) {
	  lower.add(elem);
	} else {
	  upper.add(elem);
	}
      }
      lower.sort(comp);
      upper.sort(comp);
      clear();
      addAll(lower);
      add(ref);
      addAll(upper);
    } 
  }

  public String toString() {
    StringBuffer text = new StringBuffer();
    LinkedListIterator iter = iterator();
    text.append("[");
    while (iter.hasNext()) {
      text.append(String.valueOf(iter.next()));
      if (iter.hasNext()) {
	text.append(", ");
      }
    }
    text.append("]");
    return text.toString();
  }

    /**
     * Save the state of this <tt>LinkedList</tt> instance to a stream (that
     * is, serialize it).
     *
     * @serialData The size of the list (the number of elements it
     *		   contains) is emitted (int), followed by all of its
     * elements (each an Object) in the proper order.  */
    private synchronized void writeObject(java.io.ObjectOutputStream s)
        throws java.io.IOException {
	// Write out any hidden serialization magic
	s.defaultWriteObject();

        // Write out size
        s.writeInt(size());

	// Write out all elements in the proper order.
        for (Entry e = first.next; e != last; e = e.next)
            s.writeObject(e.element);
    }

    /**
     * Reconstitute this <tt>LinkedList</tt> instance from a stream (that is
     * deserialize it).
     */
    private synchronized void readObject(java.io.ObjectInputStream s)
        throws java.io.IOException, ClassNotFoundException {
	// Read in any hidden serialization magic
	s.defaultReadObject();

        // Read in size
        int size = s.readInt();

        // Initialize header
	first = new Entry(null, null, null);
	last = new Entry(null, null, null);
	first.next = last;
	last.previous = first;

	// Read in all elements in the proper order.
	for (int i=0; i<size; i++)
            add(s.readObject());
    }


}
