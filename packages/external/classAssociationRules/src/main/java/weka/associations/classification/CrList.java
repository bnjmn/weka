/*
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 *    CrList.java
 *    Copyright (C) 2004 Stefan Mutter
 *
 */
package weka.associations.classification;

import java.io.Serializable;

/**
 * Class for the associated list of a CrTree.
 * A CrTree(n-ary tree in child-sibling representation)
 * can store classification association rules(CARs) and allows pruning and classification.
 * Tree Structure described at:
 * W. Li, J. Han, J.Pei: CMAR: Accurate and Efficient Classification Based on Multiple
 * Class-Association Rules. In ICDM'01:369-376,2001.
 *
 * 
 * @author Stefan Mutter
 * @version $Revision$
 */

public class CrList implements Serializable{

    /** The head of the list. */
    private CrListElement m_head;

    /** The tail of the list. */
    private CrListElement m_tail;

    /** The actual element. */
    private CrListElement m_element;

   
   
   /**
     * Constructor that creates an empty list.
     */                     
    public CrList(){ 
	
	m_head = new CrListElement(null,null,-1,-1,-1);
	m_tail = new CrListElement(m_head,null,-1,-1,-1);
	m_head.setSucc(m_tail);
	
    }
    
    
    /**
     * Inserts a new element at the end of the list and returns it.
     * @param num The attribute number that is stored in a CrListElement
     * @param value The attribute value that is stored in a CrListElement
     * @param i the minimum height
     * @return the inserted CrListElement
     */    
    public CrListElement insertListElement(int num, int value, int i){ 
   
	m_element = new CrListElement(m_tail.getPred(), m_tail , num, value, i);
	(m_tail.getPred()).setSucc(m_element);
	m_tail.setPred(m_element);
	return m_element;
	
    }

    /**
     * Deletes the CrListElement out of the CrList
     * @param element The CrListElement to delete
     */    
    public void deleteListElement(CrListElement element){ 
	
	if(!isEmpty()){
	    (element.getPred()).setSucc(element.getSucc());
	    (element.getSucc()).setPred(element.getPred());
	}
    }
    
    /**
     * Returns whether or not the CrList is empty.
     * @return true if the CrList is empty, false otherwise
     */    
    public boolean isEmpty(){          
	return m_tail.getPred() == m_head;
    }

    /**
     * Gets the head of the CrList
     * @return the head element of the CrList
     */    
    public CrListElement getHead(){
	
	return m_head;
    }

    /**
     * Returns a string description of the CrList
     * @return a string description of the CrList
     */    
    public String toString(){
	
	StringBuffer text = new StringBuffer();
	int i =1;

	text.append("CrList:\n");
	if(isEmpty()){
	    text.append("CrList is empty");
	    return text.toString();
	}
	CrListElement actual =m_head.getSucc();
	while(actual!=m_tail){
	    int[] elements = new int[2];
	    elements = actual.getContent();
	    text.append("Element "+i+":\t"+"("+elements[0]+" , "+elements[1]+")\n");
	    text.append("\tsibling node: "+(actual.getSiblingNode()).toString()
			+"\n\tmin. height: "+actual.getHeight()+"\n");
	    actual=actual.getSucc();
	    i++;
	}
	return text.toString();
    }

    /**
     * Searches for a CrListElement with the specified attribute number and
     * attribute value.
     * @param num the attribute number
     * @param value the attribute value
     * @return the CrListElement that stores that items, or null otherwise
     */    
    public CrListElement searchListElement(int num, int value){

	if (isEmpty()) 
	    return null;
	CrListElement actual = m_head.getSucc();
	do{
	    if(actual.equals(num,value))
		return actual;
	    actual = actual.getSucc();
	    
	} while(actual != m_tail);
	return null;
    }

    /**
     * Gets the index and the minimum height of a CrListElement in a CrList
     * @param num the attribute number
     * @param value the attribute value
     * @return an integer array containing the list index and the minimum height of the CRListElement
     * that stores the item.
     * If no CrListElement is found the array contains two zeros.
     */    
    public int[] searchListIndex(int num, int value){

	int[] result= new int[2];
	result[0]=result[1]=0;    
       	if (isEmpty()) 
	    return result;
	CrListElement actual = m_head.getSucc();
	do{
	    result[0]++;
	    if(actual.equals(num,value)){
		result[1]=actual.getHeight();
		return result;
	    }
	    actual = actual.getSucc();
	    
	} while(actual != m_tail);
	result[0]=result[1]=0;
	return result;
    }
    
}
