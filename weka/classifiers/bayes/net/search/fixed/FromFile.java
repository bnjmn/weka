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
 * FromFile.java
 * Copyright (C) 2004 Remco Bouckaert
 * 
 */
package weka.classifiers.bayes.net.search.fixed;

import weka.classifiers.bayes.net.search.SearchAlgorithm;
import weka.classifiers.bayes.BayesNet;
import weka.classifiers.bayes.net.BIFReader;
import weka.classifiers.bayes.net.ParentSet;
import weka.core.*;
import java.util.*;

/** The FromFile reads the structure of a Bayes net from a file
 * in BIFF format.
 * 
 * @author Remco Bouckaert
 * @version $Revision: 1.2 $
 */
public class FromFile extends SearchAlgorithm {
	/** name of file to read structure from **/
	String m_sBIFFile = "";

	public void buildStructure (BayesNet bayesNet, Instances instances) throws Exception {
		// read network structure in BIF format
		BIFReader bifReader = new BIFReader();
		bifReader.processFile(m_sBIFFile);
		// copy parent sets
        for (int iAttribute = 0; iAttribute < instances.numAttributes(); iAttribute++) {
        	ParentSet parentSet = bifReader.getParentSet(iAttribute);
        	bayesNet.getParentSet(iAttribute);
        	for (int iParent = 0; iParent < parentSet.getNrOfParents(); iParent++) {
        		bayesNet.getParentSet(iAttribute).addParent(parentSet.getParent(iParent), instances);
        	}
        }
	} // buildStructure

    /**
     * Set name of network in BIF file to read structure from
     * @param sBIFFile
     */
    public void setBIFFile(String sBIFFile) {
    	m_sBIFFile = sBIFFile;
    }

    /**
     * Get name of network in BIF file to read structure from
     * @return BIF file name
     */
    public String getBIFFile() {
        return m_sBIFFile;
    }

	/**
	 * Returns an enumeration describing the available options.
	 *
	 * @return an enumeration of all the available options.
	 */
	public Enumeration listOptions() {
	  Vector newVector = new Vector(0);

	  newVector.addElement(new Option("\tName of file containing network structure in BIF format\n", 
					 "B", 1, "-B <BIF File>"));
	  return newVector.elements();
	}

	/**
	 * Parses a given list of options. Valid options are:<p>
	 *
	 * -B
	 * Set the random order to true (default false). <p>
	 *
	 * For other options see search algorithm.
	 *
	 * @param options the list of options as an array of strings
	 * @exception Exception if an option is not supported
	 */
	public void setOptions(String[] options) throws Exception {
     setBIFFile( Utils.getOption('B', options));
	}

	/**
	 * Gets the current settings of the search algorithm.
	 *
	 * @return an array of strings suitable for passing to setOptions
	 */
	public String [] getOptions() {
	  String [] options  = new String [2];
	  int current = 0;
	  options[current++] = "-B";
	  options[current++] = "" + getBIFFile();
	  // Fill up rest with empty strings, not nulls!
	  while (current < options.length) {
		options[current++] = "";
	  }
	  return options;
	}

} // class FromFile
