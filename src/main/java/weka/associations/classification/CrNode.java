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
 *    CrNode.java
 *    Copyright (C) 2004 Stefan Mutter
 *
 */

package weka.associations.classification;

import weka.core.FastVector;
import java.io.Serializable;

/**
 * Class for nodes in a CrTree.
 * A CrTree(n-ary tree in child-sibling representation)
 * can store classification association rules(CARs) and allows pruning and classification.
 * Tree Structure described at:
 * W. Li, J. Han, J.Pei: CMAR: Accurate and Efficient Classification Based on Multiple
 * Class-Association Rules. In ICDM'01:369-376,2001.
 *
 * The CRrTree is implemented following the child-sibling reperesentation of n-ary trees.
 * Therefore each node has a poinnter to its parent and one pointer to one of its children.
 * Nodes having the same parent node are conneected in an doubly connected list (list of siblings). 
 * Each nodes stores an item of an rule's premise. The consequnce and additional information is 
 * stored in a FastVector. Therefore rules are stored as a path.
 * 
 * @author Stefan Mutter
 * @version $Revision$
 */

public class CrNode implements Serializable{

    /**the attribuite number stored in the node . */
    private int m_attributeNumber;

     /**The attribute value stored in the node . */
    private int m_attributeValue;

     /**FastVector storing the content of a node (a rules's consequence and additional information). */
    private FastVector m_content;
    
     /** Pointer to a child node. */
    private CrNode m_treeChild;

     /** Pointer to the parent node. */
    private CrNode m_treeParent;
    
     /** Pointer to the next sibling node. */
    private CrNode m_nextSibling;

     /** Pointer to the previous sibling node. */
    private CrNode m_lastSibling;
    
     /** Pointer to the next node in the associated list. */
    private CrNode m_nextListSibling;
    
     /** Pointer to the previous node in the associated list. */
    private CrNode m_lastListSibling;
    
     /** Pointer to an entry in the associated list. */
    private CrListElement m_listSibling;
    
     /** Height of the node in the tree. */
    private int m_height;

   

    /**
     * Constructor of a CrNode. Takes an attribute and an attribute value of an rule premise
     * and the height of the node in the CrTree as arguments.
     * @param num the attribute of a rule premise
     * @param value the attribute value of a rule premise
     * @param i the height of the node in the CrTree
     */    
    public CrNode (int num, int value, int i){

	m_attributeNumber = num;
	m_attributeValue = value;
	m_content = new FastVector();
	m_treeChild = m_nextSibling =  m_lastSibling=null;
	m_treeParent = m_nextListSibling = m_lastListSibling = null;
	m_listSibling = null;
	m_height = i;
    }

    /**
     * Sets the pointer to a child node.
     * @param input the child node
     */    
    public void setTreeChild(CrNode input){

	m_treeChild = input;
    }

    /**
     * Sets the pointer to a parent node
     * @param input the parent node
     */    
    public void setTreeParent(CrNode input){

	m_treeParent = input;
    }

    /**
     * Sets the pointer to the next sibling node
     * @param input a sibling node
     */    
    public void setNextSibling(CrNode input){

	m_nextSibling = input;
    }

    /**
     * Sets the pointer to the previous sibling node
     * @param input a sibling node
     */    
    public void setLastSibling(CrNode input){

	m_lastSibling = input;
    }

    /**
     * Sets the pointer to an element in the associated list.
     * @param input a list element out of the associated list
     */    
    public void setListSibling(CrListElement input){

	m_listSibling = input;
    }

    
    /**
     *  Sets the pointer to the next node in the associated list.
     * @param input a node
     */    
    public void setNextListSibling(CrNode input){

	m_nextListSibling = input;
    }

    /**
     * Sets the pointer to the previous node in the associated list.
     * @param input a node
     */    
    public void setLastListSibling(CrNode input){

	m_lastListSibling = input;
    }


    
    /**
     * Gets the child node
     * @return the child node of the actual node or null if it is not existing
     */    
    public CrNode getTreeChild(){

	return m_treeChild;
    }

    /**
     * Gets the parent node of the actual node.
     * @return the parent node or null if it is the root node
     */    
    public CrNode getTreeParent(){

	return m_treeParent;
    }

    /**
     * Gets the CrListElement that the current node is pointing to
     * @return a list element or null
     */    
    public CrListElement getListSibling(){

	return m_listSibling;
    }

    /**
     * Gets the next sibling node in a CrTree
     * @return the next sibling node
     */    
    public CrNode getNextSibling(){

	return m_nextSibling;
    }

    /**
     * Gets the previous sibling node.
     * @return the previous sibling node
     */    
    public CrNode getLastSibling(){

	return m_lastSibling;
    }

    /**
     * Gets the next node in the associated list.
     * @return the next node in the associated list
     */    
    public CrNode getNextListSibling(){

	return m_nextListSibling;
    }

    /**
     * Gets the previous node in the associated list.
     * @return the previous node in the associated list
     */    
    public CrNode getLastListSibling(){

	return m_lastListSibling;
    }


    /**
     * Gets the item from a rule's premise that is stored in a CrNode.
     * (An item is a attribute value pair stored in an int array with
     * two elements [attributeNumber, attributeValue]).
     * @return an integer array conating the attribute number and the attribute value
     */    
    public int[] getPathInfo(){
	
	int[] pathInfo = new int[2];
	pathInfo[0]=m_attributeNumber;
	pathInfo[1]=m_attributeValue;
	return pathInfo;
    }

    /**
     * Gets the FastVector storing possibly a rule's consequence and additional measures
     * @return a FastVector containing a rule's consequence and its measures
     */    
    public FastVector getContent(){

	return m_content;
    }
    
    /**
     * Stores a rule's consequence and its measures in a FastVector
     * @param input a FastVector containing a ruls' consequence
     */    
    public void setNewContent(FastVector input){

	m_content = input;;
    }
    
    /**
     * Adds an element to the content FastVector of a node.
     * Theis FastVector stores a rule's consequence and addiational measures.
     * @param data an Object that should be added to the FastVector
     */    
    public void addContent(Object data){

	m_content.addElement(data);
    }

    /**
     * Gets the height of a node in a CrTree
     * @return the height
     */    
    public int getHeight(){

	return m_height;
    }
   
    /**
     * Sets the height of a node in a CrTree
     * @param i the height
     */    
    public void setHeight(int i){
	 
	m_height = i;
    }

    /**
     * Method that compares two nodes and returns true if their premise's items are the same.
     * @param num the attribute number
     * @param value the attribute value
     * @return true if the nodes store the same item, false otherwise
     */    
    public boolean equals(int num, int value){
	
	if((num==m_attributeNumber) && (value==m_attributeValue))
	    return true;
	return false;
    }

    /**
     * Method for printing CrNodes
     * @return a string description of a CrNode
     */    
    public String toString(){
	
	StringBuffer text = new StringBuffer();
	text.append("("+m_attributeNumber+" , "+m_attributeValue+") | ");
	if(m_content.size()==0)
	    text.append("no Class");
	else{
	    if(m_content.size()==1)
		text.append(((Integer)(((FastVector)m_content.firstElement()).firstElement())).intValue());
	    else{
		text.append(((Integer)(((FastVector)m_content.firstElement()).firstElement())).intValue());
		//text.append(((Integer)(((FastVector)m_content.lastElement()).firstElement())).intValue());
	    }
	}
	return text.toString();
    }

}
