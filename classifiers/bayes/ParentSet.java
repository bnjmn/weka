
/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

/*
 * ParentSet.java
 * Copyright (C) 2001 Remco Bouckaert
 * 
 */
package weka.classifiers.bayes;

import weka.core.*;

/**
 * Helper class for Bayes Network classifiers. Provides datastructures to
 * represent a set of parents in a graph.
 * 
 * @author Remco Bouckaert (rrb@xm.co.nz)
 * @version $Revision: 1.4 $
 */
public class ParentSet {

  /**
   * Holds indexes of parents
   */
  private int[] m_nParents;

  /**
   * returns index parent of parent specified by index
   * 
   * @param iParent Index of parent
   */
  public int GetParent(int iParent) {
    return m_nParents[iParent];
  } 

  /**
   * sets index parent of parent specified by index
   * 
   * @param iParent Index of parent
   * @param nNode: index of the node that becomes parent
   */
  public void SetParent(int iParent, int nNode) {
	m_nParents[iParent] = nNode;
  } // SetParent


  /**
   * Holds number of parents
   */
  private int m_nNrOfParents = 0;

  /**
   * returns number of parents
   * @return number of parents
   */
  public int GetNrOfParents() {
    return m_nNrOfParents;
  } 

  /**
   * test if node is contained in parent set
   * @param iNode: node to test for
   * @return number of parents
   */
	public boolean Contains(int iNode) {
		for (int iParent = 0; iParent < m_nNrOfParents; iParent++) {
			if (m_nParents[iParent] == iNode) {
				return true;
			}
		}
		return false;
	}
  /**
   * Holds cardinality  of parents (= number of instantiations the parents can take)
   */
  private int m_nCardinalityOfParents = 1;

  /**
   * returns cardinality of parents
   */
  public int GetCardinalityOfParents() {
    return m_nCardinalityOfParents;
  } 

  /**
   * default constructor
   */
  public ParentSet() {
    m_nParents = new int[10];
    m_nNrOfParents = 0;
    m_nCardinalityOfParents = 1;
  }    // ParentSet

  /**
   * constructor
   * @param nMaxNrOfParents upper bound on nr of parents
   */
  public ParentSet(int nMaxNrOfParents) {
    m_nParents = new int[nMaxNrOfParents];
    m_nNrOfParents = 0;
    m_nCardinalityOfParents = 1;
  }    // ParentSet

  /**
   * copy constructor
   * @param other other parent set
   */
  public ParentSet(ParentSet other) {
    m_nNrOfParents = other.m_nNrOfParents;
    m_nCardinalityOfParents = other.m_nCardinalityOfParents;
    m_nParents = new int[m_nNrOfParents];

    for (int iParent = 0; iParent < m_nNrOfParents; iParent++) {
      m_nParents[iParent] = other.m_nParents[iParent];
    } 
  }    // ParentSet

  /**
   * reserve memory for parent set
   * 
   * @param nSize maximum size of parent set to reserver memory for
   */
  public void MaxParentSetSize(int nSize) {
    m_nParents = new int[nSize];
  }    // MaxParentSetSize
 
  /**
   * Add parent to parent set and update internals (specifically the cardinality of the parent set)
   * 
   * @param nParent parent to add
   * @param _Instances used for updating the internals
   */
  public void AddParent(int nParent, Instances _Instances) {
   if (m_nNrOfParents == 10) {
	// reserve more memory
	int [] nParents = new int[50];
        for (int i = 0; i < m_nNrOfParents; i++) {
            nParents[i] = m_nParents[i];
        }
        m_nParents = nParents;
   }
    m_nParents[m_nNrOfParents] = nParent;
    m_nNrOfParents++;
    m_nCardinalityOfParents *= _Instances.attribute(nParent).numValues();
  }    // AddParent

  /**
   * Delete last added parent from parent set and update internals (specifically the cardinality of the parent set)
   * 
   * @param _Instances used for updating the internals
   */
  public void DeleteLastParent(Instances _Instances) {
    m_nNrOfParents--;
    m_nCardinalityOfParents = 
      m_nCardinalityOfParents 
      / _Instances.attribute(m_nParents[m_nNrOfParents]).numValues();
  }    // DeleteLastParent
 
}      // class ParentSet




