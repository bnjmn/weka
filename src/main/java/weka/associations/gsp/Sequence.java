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
 * Sequence.java
 * Copyright (C) 2007 Sebastian Beer
 *
 */

package weka.associations.gsp;

import weka.core.FastVector;
import weka.core.Instances;
import weka.core.RevisionHandler;
import weka.core.RevisionUtils;

import java.io.Serializable;
import java.util.Enumeration;

/**
 * Class representing a sequence of elements/itemsets.
 * 
 * @author  Sebastian Beer
 * @version $Revision$
 */
public class Sequence
  implements Cloneable, Serializable, RevisionHandler {

  /** for serialization */
  private static final long serialVersionUID = -5001018056339156390L;

  /** the support count of the Sequence */
  protected int m_SupportCount;
  
  /** ordered list of the comprised elements/itemsets */
  protected FastVector m_Elements;

  /**
   * Constructor.
   */
  public Sequence() {
    m_SupportCount = 0;
    m_Elements = new FastVector();
  }

  /**
   * Constructor accepting a set of elements as parameter.
   * 
   * @param elements 		the Elements of the Sequence
   */
  public Sequence(FastVector elements) {
    m_SupportCount = 0;
    m_Elements = elements;
  }

  /**
   * Constructor accepting an int value as parameter to set the support count.
   * 
   * @param supportCount 	the support count to set
   */
  public Sequence(int supportCount) {
    m_SupportCount = supportCount;
    m_Elements = new FastVector();
  }

  /**
   * Generates all possible candidate k-Sequences and prunes the ones that 
   * contain an infrequent (k-1)-Sequence.
   * 
   * @param kMinusOneSequences 	the set of (k-1)-Sequences, used for verification
   * @return 			the generated set of k-candidates
   * @throws CloneNotSupportedException
   */
  public static FastVector aprioriGen(FastVector kMinusOneSequences) throws CloneNotSupportedException {
    FastVector allCandidates = generateKCandidates(kMinusOneSequences);
    FastVector prunedCandidates = pruneCadidates(allCandidates, kMinusOneSequences);

    return prunedCandidates;
  }

  /**
   * Deletes Sequences of a given set which don't meet the minimum support 
   * count threshold.
   * 
   * @param sequences 		the set Sequences to be checked
   * @param minSupportCount 	the minimum support count
   * @return 			the set of Sequences after deleting
   */
  public static FastVector deleteInfrequentSequences(FastVector sequences, long minSupportCount) {
    FastVector deletedSequences = new FastVector();
    Enumeration seqEnum = sequences.elements();

    while (seqEnum.hasMoreElements()) {
      Sequence currentSeq = (Sequence) seqEnum.nextElement();
      long curSupportCount = currentSeq.getSupportCount();

      if (curSupportCount >= minSupportCount) {
	deletedSequences.addElement(currentSeq);
      }
    }
    return deletedSequences;
  }

  /**
   * Generates candidate k-Sequences on the basis of a given (k-1)-Sequence set.
   * 
   * @param kMinusOneSequences 	the set of (k-1)-Sequences
   * @return 			the set of candidate k-Sequences
   * @throws CloneNotSupportedException
   */
  protected static FastVector generateKCandidates(FastVector kMinusOneSequences) throws CloneNotSupportedException {
    FastVector candidates = new FastVector();
    FastVector mergeResult = new FastVector();

    for (int i = 0; i < kMinusOneSequences.size(); i++) {
      for (int j = 0; j < kMinusOneSequences.size(); j++) {
	Sequence originalSeq1 = (Sequence) kMinusOneSequences.elementAt(i);
	Sequence seq1 = originalSeq1.clone();
	Sequence originalSeq2 = (Sequence) kMinusOneSequences.elementAt(j);
	Sequence seq2 = originalSeq2.clone();
	Sequence subseq1 = seq1.deleteEvent("first");
	Sequence subseq2 = seq2.deleteEvent("last");

	if (subseq1.equals(subseq2)) {
	  //seq1 and seq2 are 1-sequences
	  if ((subseq1.getElements().size() == 0) && (subseq2.getElements().size() == 0)) {
	    if (i >= j) {
	      mergeResult = merge(seq1, seq2, true, true);
	    } else {
	      mergeResult = merge(seq1, seq2, true, false);
	    }
	    //seq1 and seq2 are k-sequences
	  } else {
	    mergeResult = merge(seq1, seq2, false, false);
	  }
	  candidates.appendElements(mergeResult);
	}
      }
    }
    return candidates;
  }

  /**
   * Merges two Sequences in the course of candidate generation. Differentiates 
   * between merging 1-Sequences and k-Sequences, k > 1.
   * 
   * @param seq1 		Sequence at first position
   * @param seq2 		Sequence at second position
   * @param oneElements 	true, if 1-Elements should be merged, else false
   * @param mergeElements 	true, if two 1-Elements were not already merged 
   * 				(regardless of their position), else false
   * @return 			set of resulting Sequences
   */
  protected static FastVector merge(Sequence seq1, Sequence seq2, boolean oneElements, boolean mergeElements) {
    FastVector mergeResult = new FastVector();

    //merge 1-sequences
    if (oneElements) {
      Element element1 = (Element) seq1.getElements().firstElement();
      Element element2 = (Element) seq2.getElements().firstElement();
      Element element3 = null;
      if (mergeElements) {
	for (int i = 0; i < element1.getEvents().length; i++) {
	  if (element1.getEvents()[i] > -1) {
	    if (element2.getEvents()[i] > -1) {
	      break;
	    } else {
	      element3 = Element.merge(element1, element2);
	    }
	  }
	}
      }
      FastVector newElements1 = new FastVector();
      //generate <{x}{y}>
      newElements1.addElement(element1);
      newElements1.addElement(element2);
      mergeResult.addElement(new Sequence(newElements1));
      //generate <{x,y}>
      if (element3 != null) {
	FastVector newElements2 = new FastVector();
	newElements2.addElement(element3);
	mergeResult.addElement(new Sequence(newElements2));
      }

      return mergeResult;
      //merge k-sequences, k > 1
    } else {
      Element lastElementSeq1 = (Element) seq1.getElements().lastElement();
      Element lastElementSeq2 = (Element) seq2.getElements().lastElement();
      Sequence resultSeq = new Sequence();
      FastVector resultSeqElements = resultSeq.getElements();

      //if last two events/items belong to the same element/itemset
      if (lastElementSeq2.containsOverOneEvent()) {
	for (int i = 0; i < (seq1.getElements().size()-1); i++) {
	  resultSeqElements.addElement(seq1.getElements().elementAt(i));
	}
	resultSeqElements.addElement(Element.merge(lastElementSeq1, lastElementSeq2));
	mergeResult.addElement(resultSeq);

	return mergeResult;
	//if last two events/items belong to different elements/itemsets
      } else {
	for (int i = 0; i < (seq1.getElements().size()); i++) {
	  resultSeqElements.addElement(seq1.getElements().elementAt(i));
	}
	resultSeqElements.addElement(lastElementSeq2);
	mergeResult.addElement(resultSeq);

	return mergeResult;
      }
    }
  }

  /**
   * Converts a set of 1-Elements into a set of 1-Sequences.
   * 
   * @param elements 		the set of 1-Elements
   * @return 			the set of 1-Sequences
   */
  public static FastVector oneElementsToSequences(FastVector elements) {
    FastVector sequences = new FastVector();
    Enumeration elementEnum = elements.elements();

    while (elementEnum.hasMoreElements()) {
      Sequence seq = new Sequence();
      FastVector seqElements = seq.getElements();
      seqElements.addElement(elementEnum.nextElement());
      sequences.addElement(seq);
    }
    return sequences;
  }

  /**
   * Prints a set of Sequences as String output.
   * 
   * @param setOfSequences	the set of sequences
   */
  public static void printSetOfSequences(FastVector setOfSequences) {
    Enumeration seqEnum = setOfSequences.elements();
    int i = 1;

    while(seqEnum.hasMoreElements()) {
      Sequence seq = (Sequence) seqEnum.nextElement();
      System.out.print("[" + i++ + "]" + " " + seq.toString());
    }
  }

  /**
   * Prunes a k-Sequence of a given candidate set if one of its (k-1)-Sequences 
   * is infrequent.
   * 
   * @param allCandidates 	the set of all potential k-Sequences
   * @param kMinusOneSequences 	the set of (k-1)-Sequences for verification
   * @return 			the set of the pruned candidates
   */
  protected static FastVector pruneCadidates(FastVector allCandidates, FastVector kMinusOneSequences) {
    FastVector prunedCandidates = new FastVector();
    boolean isFrequent;
    //for each candidate
    for (int i = 0; i < allCandidates.size(); i++) {
      Sequence candidate = (Sequence) allCandidates.elementAt(i);
      isFrequent = true;
      FastVector canElements = candidate.getElements();
      //generate each possible (k-1)-sequence and verify if it's frequent
      for (int j = 0; j < canElements.size(); j++) {
	if(isFrequent) {
	  Element origElement = (Element) canElements.elementAt(j);
	  int[] origEvents = origElement.getEvents();

	  for (int k = 0; k < origEvents.length; k++) {
	    if (origEvents[k] > -1) {
	      int helpEvent = origEvents[k];
	      origEvents[k] = -1;

	      if (origElement.isEmpty()) {
		canElements.removeElementAt(j);
		//check if the (k-1)-sequence is contained in the set of kMinusOneSequences
		int containedAt = kMinusOneSequences.indexOf(candidate);
		if (containedAt != -1) {
		  origEvents[k] = helpEvent;
		  canElements.insertElementAt(origElement, j);
		  break;
		} else {
		  isFrequent = false;
		  break;
		}
	      } else {
		//check if the (k-1)-sequence is contained in the set of kMinusOneSequences
		int containedAt = kMinusOneSequences.indexOf(candidate);
		if (containedAt != -1) {
		  origEvents[k] = helpEvent;
		  continue;
		} else {
		  isFrequent = false;
		  break;
		}
	      }
	    }
	  }
	} else {
	  break;
	}
      }
      if (isFrequent) {
	prunedCandidates.addElement(candidate);
      }
    }
    return prunedCandidates;
  }

  /**
   * Returns a String representation of a set of Sequences where the numeric 
   * value of each event/item is represented by its respective nominal value.
   * 
   * @param setOfSequences 	the set of Sequences
   * @param dataSet 		the corresponding data set containing the header 
   * 				information
   * @param filterAttributes	the attributes to filter out
   * @return 			the String representation
   */
  public static String setOfSequencesToString(FastVector setOfSequences, Instances dataSet, FastVector filterAttributes) {
    StringBuffer resString = new StringBuffer();
    Enumeration SequencesEnum = setOfSequences.elements();
    int i = 1;
    boolean printSeq;

    while(SequencesEnum.hasMoreElements()) {
      Sequence seq = (Sequence) SequencesEnum.nextElement();
      Integer filterAttr = (Integer) filterAttributes.elementAt(0);
      printSeq = true;

      if (filterAttr.intValue() != -1) {
	for (int j=0; j < filterAttributes.size(); j++) {
	  filterAttr = (Integer) filterAttributes.elementAt(j);
	  FastVector seqElements = seq.getElements();

	  if (printSeq) {
	    for (int k=0; k < seqElements.size(); k++) {
	      Element currentElement = (Element) seqElements.elementAt(k);
	      int[] currentEvents = currentElement.getEvents();

	      if (currentEvents[filterAttr.intValue()] != -1) {
		continue;
	      } else {
		printSeq = false;
		break;
	      }
	    }
	  }
	}
      }
      if (printSeq) {
	resString.append("[" + i++ + "]" + " " + seq.toNominalString(dataSet));
      }
    }
    return resString.toString();
  }

  /**
   * Updates the support count of a set of Sequence candidates according to a 
   * given set of data sequences.
   * 
   * @param candidates 		the set of candidates
   * @param dataSequences 	the set of data sequences
   */
  public static void updateSupportCount(FastVector candidates, FastVector dataSequences) {
    Enumeration canEnumeration = candidates.elements();

    while(canEnumeration.hasMoreElements()){
      Enumeration dataSeqEnumeration = dataSequences.elements();
      Sequence candidate = (Sequence) canEnumeration.nextElement();

      while(dataSeqEnumeration.hasMoreElements()) {
	Instances dataSequence = (Instances) dataSeqEnumeration.nextElement();

	if (candidate.isSubsequenceOf(dataSequence)) {
	  candidate.setSupportCount(candidate.getSupportCount() + 1);
	}
      }
    }
  }

  /**
   * Returns a deep clone of a Sequence.
   * 
   * @return 		the cloned Sequence
   */
  public Sequence clone() {
    try {
      Sequence clone = (Sequence) super.clone();

      clone.setSupportCount(m_SupportCount);
      FastVector cloneElements = new FastVector(m_Elements.size());

      for (int i = 0; i < m_Elements.size(); i++) {
	Element helpElement = (Element) m_Elements.elementAt(i);
	cloneElements.addElement(helpElement.clone());
      }
      clone.setElements(cloneElements);

      return clone;
    } catch (CloneNotSupportedException exc) {
      exc.printStackTrace();
    }
    return null;
  }

  /**
   * Deletes either the first or the last event/item of a Sequence. If the 
   * deleted event/item is the only value in the Element, it is removed, as well.
   * 
   * @param position 		the position of the event/item (first or last)
   * @return 			the Sequence with either the first or the last 
   * 				event/item deleted
   */
  protected Sequence deleteEvent(String position) {
    Sequence cloneSeq = clone();

    if (position.equals("first")) {
      Element element = (Element) cloneSeq.getElements().firstElement();
      element.deleteEvent("first");
      if (element.isEmpty()) {
	cloneSeq.getElements().removeElementAt(0);
      }
      return cloneSeq;
    }
    if (position.equals("last")) {
      Element element = (Element) cloneSeq.getElements().lastElement();
      element.deleteEvent("last");
      if (element.isEmpty()) {
	cloneSeq.getElements().removeElementAt(m_Elements.size()-1);
      }
      return cloneSeq;
    }
    return null;
  }

  /**
   * Checks if two Sequences are equal.
   * 
   * @return 			true, if the two Sequences are equal, else false
   */
  public boolean equals(Object obj) {
    Sequence seq2 = (Sequence) obj;
    FastVector seq2Elements = seq2.getElements();

    for (int i = 0; i < m_Elements.size(); i++) {
      Element thisElement = (Element) m_Elements.elementAt(i);
      Element seq2Element = (Element) seq2Elements.elementAt(i);
      if (!thisElement.equals(seq2Element)) {
	return false;
      }
    }
    return true;
  }

  /**
   * Returns the Elements of the Sequence.
   * 
   * @return 			the Elements
   */
  protected FastVector getElements() {
    return m_Elements;
  }

  /**
   * Returns the support count of the Sequence.
   * 
   * @return 			the support count
   */
  protected int getSupportCount() {
    return m_SupportCount;
  }

  /**
   * Checks if the Sequence is subsequence of a given data sequence.
   * 
   * @param dataSequence 	the data sequence to verify against
   * @return 			true, if the Sequnce is subsequence of the data 
   * 				sequence, else false
   */
  protected boolean isSubsequenceOf(Instances dataSequence) {
    FastVector elements = getElements();
    Enumeration elementEnum = elements.elements();
    Element curElement = (Element) elementEnum.nextElement();

    for (int i = 0; i < dataSequence.numInstances(); i++) {
      if (curElement.isContainedBy(dataSequence.instance(i))) {
	if (!elementEnum.hasMoreElements()) {
	  return true;
	} else {
	  curElement = (Element) elementEnum.nextElement();
	  continue;
	}
      }
    }
    return false;
  }

  /**
   * Sets the Elements of the Sequence.
   * 
   * @param elements 		the Elements to set
   */
  protected void setElements(FastVector elements) {
    m_Elements = elements;
  }

  /**
   * Sets the support count of the Sequence.
   * 
   * @param supportCount 	the support count to set
   */
  protected void setSupportCount(int supportCount) {
    m_SupportCount = supportCount;
  }

  /**
   * Returns a String representation of a Sequences where the numeric value 
   * of each event/item is represented by its respective nominal value.
   * 
   * @param dataSet 		the corresponding data set containing the header 
   * 				information
   * @return 			the String representation
   */
  public String toNominalString(Instances dataSet) {
    String result = "";

    result += "<";

    for (int i = 0; i < m_Elements.size(); i++) {
      Element element = (Element) m_Elements.elementAt(i);
      result += element.toNominalString(dataSet);
    }
    result += "> (" + getSupportCount() + ")\n";

    return result;
  }

  /**
   * Returns a String representation of a Sequence.
   * 
   * @return 			the String representation
   */
  public String toString() {
    String result = "";

    result += "Sequence Output\n";
    result += "------------------------------\n";
    result += "Support Count: " + getSupportCount() + "\n";
    result += "contained elements/itemsets:\n";

    for (int i = 0; i < m_Elements.size(); i++) {
      Element element = (Element) m_Elements.elementAt(i);
      result += element.toString();
    }
    result += "\n\n";

    return result;
  }
  
  /**
   * Returns the revision string.
   * 
   * @return		the revision
   */
  public String getRevision() {
    return RevisionUtils.extract("$Revision$");
  }
}
