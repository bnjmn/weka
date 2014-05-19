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
 *    Copyright (C) 2004
 *    & Matthias Schubert (schubert@dbs.ifi.lmu.de)
 *    & Zhanna Melnikova-Albrecht (melnikov@cip.ifi.lmu.de)
 *    & Rainer Holzmann (holzmann@cip.ifi.lmu.de)
 */

package weka.clusterers.forOPTICSAndDBScan.OPTICS_GUI;

import weka.core.FastVector;
import weka.core.RevisionHandler;
import weka.core.RevisionUtils;
import weka.core.DistanceFunction;

import java.io.Serializable;

import java.util.ArrayList;

/**
 * <p>
 * SERObject.java <br/>
 * Authors: Rainer Holzmann, Zhanna Melnikova-Albrecht <br/>
 * Date: Sep 15, 2004 <br/>
 * Time: 9:43:00 PM <br/>
 * $ Revision 1.4 $ <br/>
 * </p>
 *
 * @author Zhanna Melnikova-Albrecht (melnikov@cip.ifi.lmu.de)
 * @author Rainer Holzmann (holzmann@cip.ifi.lmu.de)
 * @version $Revision$
 */
public class SERObject
    implements Serializable, RevisionHandler {

    /** for serialization */
    private static final long serialVersionUID = -6022057864970639151L;
  
    private ArrayList resultVector;
    private int databaseSize;
    private int numberOfAttributes;
    private double epsilon;
    private int minPoints;
    private boolean opticsOutputs;
  private DistanceFunction distanceFunction;
    private int numberOfGeneratedClusters;
    private String elapsedTime;

    // *****************************************************************************************************************
    // constructors
    // *****************************************************************************************************************

    public SERObject(ArrayList resultVector,
                     int databaseSize,
                     int numberOfAttributes,
                     double epsilon,
                     int minPoints,
                     boolean opticsOutputs,
                     DistanceFunction distance,
                     int numberOfGeneratedClusters,
                     String elapsedTime) {
        this.resultVector = resultVector;
        this.databaseSize = databaseSize;
        this.numberOfAttributes = numberOfAttributes;
        this.epsilon = epsilon;
        this.minPoints = minPoints;
        this.opticsOutputs = opticsOutputs;
        this.distanceFunction = distance;
        this.numberOfGeneratedClusters = numberOfGeneratedClusters;
        this.elapsedTime = elapsedTime;
    }

    // *****************************************************************************************************************
    // methods
    // *****************************************************************************************************************

    /**
     * Returns the resultVector
     * @return ArrayList resultVector
     */
    public ArrayList getResultVector() {
        return resultVector;
    }

    /**
     * Returns the database's size
     * @return int databaseSize
     */
    public int getDatabaseSize() {
        return databaseSize;
    }

    /**
     * Returns the number of Attributes of the specified database
     * @return int numberOfAttributes
     */
    public int getNumberOfAttributes() {
        return numberOfAttributes;
    }

    /**
     * Returns the value of epsilon
     * @return double epsilon
     */
    public double getEpsilon() {
        return epsilon;
    }

    /**
     * Returns the number of minPoints
     * @return int minPoints
     */
    public int getMinPoints() {
        return minPoints;
    }

    /**
     * Returns the flag for writing actions
     * @return True if the outputs are to write to a file, else false
     */
    public boolean isOpticsOutputs() {
        return opticsOutputs;
    }

    /**
     * Returns the distance-type
     * @return Distance Distance-type
     */
  public DistanceFunction getDistanceFunction() {
        return distanceFunction;
    }

    /**
     * Returns the number of generated clusters
     * @return int numberOfGeneratedClusters
     */
    public int getNumberOfGeneratedClusters() {
        return numberOfGeneratedClusters;
    }

    /**
     * Returns the elapsed-time
     * @return String elapsedTime
     */
    public String getElapsedTime() {
        return elapsedTime + " sec";
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
