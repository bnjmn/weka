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

/**
 *    LightHashTable.java
 *    Copyright (c) 1995-97 by Len Trigg (trigg@cs.waikato.ac.nz).
 *    Java port to Weka by Abdelaziz Mahoui (am14@cs.waikato.ac.nz).
 *
 */


package weka.classifiers.lazy.kstar;

/*
 * @author Len Trigg (len@intelligenesis.net)
 * @author Abdelaziz Mahoui (am14@cs.waikato.ac.nz)
 * @version $Revision 1.0 $
 */
public class LightHashTable {

  /** The hash table data. */
  private transient TableEntry [] m_Table;

  /** The total number of entries in the hash table. */
  private transient int m_Count;

  /** Rehashes the table when count exceeds this threshold. */
  private int m_Threshold;

  /** The load factor for the hashtable. */
  private float m_LoadFactor;

  /** The default size of the hashtable */
  private static final int DEFAULT_TABLE_SIZE = 101;

  /** The default load factor for the hashtable */
  private static final float DEFAULT_LOAD_FACTOR = 0.75f;

  /** Accuracy value for equality */
  private static final double EPSILON = 1.0E-5;
  
  /**
   * Constructs a new hashtable with a default capacity and load factor.
   */
  public LightHashTable() {
    m_Table = new TableEntry[DEFAULT_TABLE_SIZE];
    m_LoadFactor = DEFAULT_LOAD_FACTOR;
    m_Threshold = (int)(DEFAULT_TABLE_SIZE * DEFAULT_LOAD_FACTOR);
    m_Count = 0;
  }
  
  /**
   * Tests if the specified double is a key in this hashtable.
   */
  public boolean containsKey(double key) {
    TableEntry [] table = m_Table;
    int hash = hashCode(key);
    int index = (hash & 0x7FFFFFFF) % table.length;
    for (TableEntry e = table[index] ; e != null ; e = e.next) {
      if ((e.hash == hash) && (Math.abs(e.key - key) < EPSILON)) {
	return true;
      }
    }
    return false;
  }
  
  /**
   * Inserts a new entry in the hashtable using the specified key. 
   * If the key already exist in the hashtable, do nothing.
   */
  public void insert(double key, double value, double pmiss) {
    // Makes sure the key is not already in the hashtable.
    TableEntry e, ne;
    TableEntry [] table = m_Table;
    int hash = hashCode(key);
    int index = (hash & 0x7FFFFFFF) % table.length;
    // start looking along the chain
    for (e = table[index] ; e != null ; e = e.next) {
      if ((e.hash == hash) && (Math.abs(e.key - key) < EPSILON)) {
	return;
      }
    }
    // At this point, key is not in table.
    // Creates a new entry.
    ne = new TableEntry( hash, key, value, pmiss, table[index] );
    // Put entry at the head of the chain.
    table[index] = ne;
    m_Count++;
    // Rehash the table if the threshold is exceeded
    if (m_Count >= m_Threshold) {
      rehash();
    }
  }
  
  /**
   * Returns the table entry to which the specified key is mapped in 
   * this hashtable.
   */
  public TableEntry getEntry(double key) {
    TableEntry [] table = m_Table;
    int hash = hashCode(key);
    int index = (hash & 0x7FFFFFFF) % table.length;
    for (TableEntry e = table[index] ; e != null ; e = e.next) {
      if ((e.hash == hash) && (Math.abs(e.key - key) < EPSILON)) {
	return e;
      }
    }
    return null;
  }
  
  /**
   * Returns the number of keys in this hashtable.
   */
  public int size() {
    return m_Count;
  }
  
  /**
   * Tests if this hashtable maps no keys to values.
   */
  public boolean isEmpty() {
    return m_Count == 0;
  }
  
  /**
   * Clears this hashtable so that it contains no keys.
   */
  public void clear() {
    TableEntry table[] = m_Table;
    for (int index = table.length; --index >= 0; ) {
      table[index] = null;
    }
    m_Count = 0;
  }

  /**
   * Rehashes the contents of the hashtable into a hashtable with a 
   * larger capacity. This method is called automatically when the 
   * number of keys in the hashtable exceeds this hashtable's capacity 
   * and load factor. 
   *
   */
  protected void rehash() {
    int oldCapacity = m_Table.length;
    TableEntry [] oldTable = m_Table;    
    int newCapacity = oldCapacity * 2 + 1;
    TableEntry [] newTable = new TableEntry[newCapacity];
    m_Threshold = (int)(newCapacity * m_LoadFactor);
    m_Table = newTable;
    TableEntry e, old;
    for (int i = oldCapacity ; i-- > 0 ;) {
      for (old = oldTable[i] ; old != null ; ) {
	e = old;
	old = old.next;
	int index = (e.hash & 0x7FFFFFFF) % newCapacity;
	e.next = newTable[index];
	newTable[index] = e;
      }
    }
  }
  
  /**
   * 
   */
  private int hashCode(double key) {
    long bits = Double.doubleToLongBits(key);
    return (int)(bits ^ (bits >> 32));
  }
  
  //#########################################################################//

  /**
   * Hashtable collision list.
   */
  class TableEntry {
    /** attribute value hash code */
    public int hash;
    /** attribute value */
    public double key;
    /** scale factor or stop parameter */
    public double value;
    /** transformation probability to missing value */
    public double pmiss;
    /** next table entry (separate chaining) */
    public TableEntry next = null;
    /** Constructor */
    TableEntry( int hash, double key, double value, double pmiss, TableEntry next ) {
      this.hash  = hash;
      this.key   = key;
      this.value = value;
      this.pmiss = pmiss;
      this.next  = next;
    }
  }  
  
  //#########################################################################//

  
} // LightHashTable
