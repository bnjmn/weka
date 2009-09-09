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
 *    AbstractStringDistanceFunction.java
 *    Copyright (C) 2008 Bruno Woltzenlogel Paleo (http://www.logic.at/people/bruno/ ; http://bruno-wp.blogspot.com/)
 *
 */

package weka.core;

/**
 * Computes the Levenshtein edit distance between two strings.
 *
 * @author Bruno Woltzenlogel Paleo
 * @version $Revision$
 */
public class EditDistance
    extends AbstractStringDistanceFunction {

  public EditDistance() {
  }

  public EditDistance(Instances data) {
    super(data);
  }

  /**
   * Calculates the distance (Levenshtein Edit Distance) between two strings
   *
   * @param stringA the first string
   * @param stringB the second string
   * @return the distance between the two given strings
   */
  double stringDistance(String stringA, String stringB) {
    int lengthA = stringA.length();
    int lengthB = stringB.length();

    double[][] distanceMatrix = new double[lengthA + 1][lengthB + 1];

    for (int i = 0; i <= lengthA; i++) {
      distanceMatrix[i][0] = i;
    }

    for (int j = 1; j <= lengthB; j++) {
      distanceMatrix[0][j] = j;
    }

    for (int i = 1; i <= lengthA; i++) {
      for (int j = 1; j <= lengthB; j++) {
        if (stringA.charAt(i - 1) == stringB.charAt(j - 1)) {
          distanceMatrix[i][j] = distanceMatrix[i - 1][j - 1];
        }
        else {
          distanceMatrix[i][j] = 1 + Math.min(distanceMatrix[i - 1][j],
                                              Math.min(distanceMatrix[i][j - 1],
                                                       distanceMatrix[i - 1][j - 1]));
        }
      }
    }
    return distanceMatrix[lengthA][lengthB];
  }

    
  /**
   * Returns a string describing this object.
   * 
   * @return 		a description of the evaluator suitable for
   * 			displaying in the explorer/experimenter gui
   */
  public String globalInfo() {
    return 
      "Implementing Levenshtein distance function.\n\n"
      + "One object defines not one distance but the data model in which "
      + "the distances between objects of that data model can be computed.\n\n"
      + "Attention: For efficiency reasons the use of consistency checks "
      + "(like are the data models of the two instances exactly the same), "
      + "is low.\n\n"
      + "For more information, see: http://en.wikipedia.org/wiki/Levenshtein_distance\n\n";
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