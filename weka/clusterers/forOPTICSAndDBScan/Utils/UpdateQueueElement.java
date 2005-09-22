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

/**
 * <p>
 * UpdateQueueElement.java <br/>
 * Authors: Rainer Holzmann, Zhanna Melnikova-Albrecht, Matthias Schubert <br/>
 * Date: Aug 31, 2004 <br/>
 * Time: 6:43:18 PM <br/>
 * $ Revision 1.4 $ <br/>
 * </p>
 *
 * @author Matthias Schubert (schubert@dbs.ifi.lmu.de)
 * @author Zhanna Melnikova-Albrecht (melnikov@cip.ifi.lmu.de)
 * @author Rainer Holzmann (holzmann@cip.ifi.lmu.de)
 * @version $Revision: 1.2 $
 */
public class UpdateQueueElement {

    /**
     * Holds the priority for the object (in this case: the reachability-distance)
     */
    private double priority;

    /**
     * Holds the original object
     */
    private Object o;

    /**
     * Holds the key for this object
     */
    private String objectKey;

    // *****************************************************************************************************************
    // constructors
    // *****************************************************************************************************************
    public UpdateQueueElement(double priority, Object o, String objectKey) {
        this.priority = priority;
        this.o = o;
        this.objectKey = objectKey;
    }

    // *****************************************************************************************************************
    // methods
    // *****************************************************************************************************************

    /**
     * Returns the priority for this object
     * @return priority
     */
    public double getPriority() {
        return priority;
    }

    /**
     * Returns the object
     * @return
     */
    public Object getObject() {
        return o;
    }

    /**
     * Returns the key
     * @return objectKey
     */
    public String getObjectKey() {
        return objectKey;
    }

    // *****************************************************************************************************************
    // inner classes
    // *****************************************************************************************************************

}
