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
 *    Copyright (C) 2004
 *    & Matthias Schubert (schubert@dbs.ifi.lmu.de)
 *    & Zhanna Melnikova-Albrecht (melnikov@cip.ifi.lmu.de)
 *    & Rainer Holzmann (holzmann@cip.ifi.lmu.de)
 */

package weka.clusterers.forOPTICSAndDBScan.Utils;

import weka.clusterers.forOPTICSAndDBScan.DataObjects.DataObject;
import weka.core.RevisionHandler;
import weka.core.RevisionUtils;

/**
 * <p>
 * EpsilonRange_ListElement.java <br/>
 * Authors: Rainer Holzmann, Zhanna Melnikova-Albrecht, Matthias Schubert <br/>
 * Date: Sep 7, 2004 <br/>
 * Time: 2:12:34 PM <br/>
 * $ Revision 1.4 $ <br/>
 * </p>
 *
 * @author Matthias Schubert (schubert@dbs.ifi.lmu.de)
 * @author Zhanna Melnikova-Albrecht (melnikov@cip.ifi.lmu.de)
 * @author Rainer Holzmann (holzmann@cip.ifi.lmu.de)
 * @version $Revision: 1.3 $
 */
public class EpsilonRange_ListElement
    implements RevisionHandler {

    /**
     * Holds the dataObject
     */
    private DataObject dataObject;

    /**
     * Holds the distance that was calculated for this dataObject
     */
    private double distance;

    // *****************************************************************************************************************
    // constructors
    // *****************************************************************************************************************

    /**
     * Constructs a new Element that is stored in the ArrayList which is
     * built in the k_nextNeighbourQuery-method from a specified database.
     * This structure is chosen to deliver not only the DataObjects that
     * are within the epsilon-range but also deliver the distances that
     * were calculated. This reduces the amount of distance-calculations
     * within some data-mining-algorithms.
     * @param distance The calculated distance for this dataObject
     * @param dataObject A dataObject that is within the epsilon-range
     */
    public EpsilonRange_ListElement(double distance, DataObject dataObject) {
        this.distance = distance;
        this.dataObject = dataObject;
    }

    // *****************************************************************************************************************
    // methods
    // *****************************************************************************************************************

    /**
     * Returns the distance that was calulcated for this dataObject
     * (The distance between this dataObject and the dataObject for which an epsilon-range-query
     * was performed.)
     * @return distance
     */
    public double getDistance() {
        return distance;
    }

    /**
     * Returns this dataObject
     * @return dataObject
     */
    public DataObject getDataObject() {
        return dataObject;
    }
    
    /**
     * Returns the revision string.
     * 
     * @return		the revision
     */
    public String getRevision() {
      return RevisionUtils.extract("$Revision: 1.3 $");
    }
}
