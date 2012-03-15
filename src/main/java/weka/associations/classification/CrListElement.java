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
 *    CrListElement.java
 *    Copyright (C) 2004 Stefan Mutter
 *
 */
package weka.associations.classification;

import java.io.Serializable;


/**
 * Class for list elements in the associated list of a CrTree.
 * A CrTree(n-ary tree in child-sibling representation)
 * can store classification association rules(CARs) and allows pruning and classification.
 * Tree Structure described at:
 * W. Li, J. Han, J.Pei: CMAR: Accurate and Efficient Classification Based on Multiple
 * Class-Association Rules. In ICDM'01:369-376,2001.
 *
 * Each possible item of a rule premise is stored once in the associated list.
 * 
 * @author Stefan Mutter
 * @version $Revision$
 */

public class CrListElement implements Serializable{

    /** The successor in the list. */
    private CrListElement m_succ;

    /** The predecessor in the list. */
    private CrListElement m_pred;
    
    /** The attribute number. */
    private int m_listAttributeNumber;

    /** The attribute value. */
    private int m_listAttributeValue;
    
    /** Pointer to a CrNode. */
    private CrNode m_siblingNode;
    
    /** The minimum height of a CrNode (that stores the same as the CrListelement) in a CrTree. */
    private int m_minHeight;

               
    

    /**
     * Constructor that constructs a CrListElement and a CrNode
     * to which the list element is pointing.
     * @param pred the predecessor in the list
     * @param succ the successor in the list
     * @param num the attribute number
     * @param value the attribute value
     * @param i the minimum height
     */    
    public CrListElement(CrListElement pred, CrListElement succ,int num, int value, int i){ 
	
	this.m_pred = pred;
	this.m_succ = succ; 
	this.m_listAttributeNumber = num;
	this.m_listAttributeValue = value;
	this.m_minHeight=i;
	this.m_siblingNode = new CrNode(num, value ,i);
    }

    /**
     * Sets the predecessor
     * @param input a CrListElement
     */    
    public void setPred(CrListElement input){

	m_pred = input;
    }

    /**
     * Sets the sucessor.
     * @param input a CrListElement
     */    
    public void setSucc(CrListElement input){

	m_succ = input;
    }
    
    /**
     * Sets the minimum height of all nodes in a CrTree
     * that store the same item than the ListElement
     * @param i the minimum height
     */    
    public void setHeight(int i){

	m_minHeight = i;
    }

    /**
     * Gets the minimum height of all nodes in a CrTree
     * that store the same item than the ListElement
     * @return the minimum hieght that is stored in that CrListElement
     */    
    public int getHeight(){

	return m_minHeight;
    }

    /**
     * Gets the predecessor
     * @return the CrListElement that is preceding the actual element
     */    
    public CrListElement getPred(){

	return m_pred;
    }

    /**
     * Gets the successor
     * @return the CrListElement that comes after the actual element
     */    
    public CrListElement getSucc(){

	return m_succ;
    }

    /**
     * Gets the node to wich the CrListElement is pointing
     * @return a CrNode
     */    
    public CrNode getSiblingNode(){

	return m_siblingNode;
    }

    /**
     * Sets the node to wich the CrListElement is pointing
     * @param sibling a CrNode
     */    
    public void setSiblingNode(CrNode sibling){

	m_siblingNode = sibling;
    }

    /**
     * Gets the item that is stored in a CrListElement.
     * Items are stored as an integer array [attribute number, attribute value]
     * @return an integer array conating the item
     */    
    public int[] getContent(){

	int[] content = new int[2];
	content[0]=m_listAttributeNumber;
	content[1]=m_listAttributeValue;
	return content;
    }

    /**
     * Sets the item that is stored in a CrListElement.
     * Items are stored as an integer array [attribute number, attribute value]
     * @param insert an integer array containing the attribute number and the attribute value
     */    
    public void setContent(int[] insert){

	m_listAttributeNumber = insert[0];
	m_listAttributeValue = insert[1];
    }

    /**
     * Compares two CrListElements
     * @param num the attribute number
     * @param value the attribute value
     * @return true if the CrListElements store the same item, false otherwise
     */    
    public boolean equals(int num, int value){
	
	if((num==m_listAttributeNumber) && (value==m_listAttributeValue))
	    return true;
	return false;
    }
    
    /**
     * Methods that returns a string description for a CrListElement
     * @return a string description of a CrListElement
     */    
    public String toString(){
	
	return ("LE: "+m_listAttributeNumber+","+m_listAttributeValue);
    }
}







