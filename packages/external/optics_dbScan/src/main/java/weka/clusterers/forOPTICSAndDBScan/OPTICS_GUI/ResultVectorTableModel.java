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

import weka.clusterers.forOPTICSAndDBScan.DataObjects.DataObject;
import weka.core.RevisionHandler;
import weka.core.RevisionUtils;
import weka.core.Utils;

import javax.swing.table.AbstractTableModel;

import java.util.ArrayList;

/**
 * <p>
 * ResultVectorTableModel.java <br/>
 * Authors: Rainer Holzmann, Zhanna Melnikova-Albrecht <br/>
 * Date: Sep 12, 2004 <br/>
 * Time: 9:23:31 PM <br/>
 * $ Revision 1.4 $ <br/>
 * </p>
 *
 * @author Zhanna Melnikova-Albrecht (melnikov@cip.ifi.lmu.de)
 * @author Rainer Holzmann (holzmann@cip.ifi.lmu.de)
 * @version $Revision$
 */
public class ResultVectorTableModel
    extends AbstractTableModel
    implements RevisionHandler {

    /** for serialization */
    private static final long serialVersionUID = -7732711470435549210L;

    /**
     * Holds the ClusterOrder (dataObjects with their r_dist and c_dist) for the GUI
     */
    private ArrayList resultVector;

    // *****************************************************************************************************************
    // constructors
    // *****************************************************************************************************************

    /**
     *  Constructs a default <code>DefaultTableModel</code>
     *  which is a table of zero columns and zero rows.
     */
    public ResultVectorTableModel(ArrayList resultVector) {
        this.resultVector = resultVector;
    }

    // *****************************************************************************************************************
    // methods
    // *****************************************************************************************************************

    /**
     * Returns the number of rows of this model.
     * The number of rows is the number of dataObjects stored in the resultVector
     * @return the number of rows of this model
     */
    public int getRowCount() {
        if (resultVector == null)
            return 0;
        else
            return resultVector.size();
    }

    /**
     * Returns the number of columns of this model.
     * The number of columns is 4 (dataObject.key, dataobject, c_dist, r_dist)
     * @return int The number of columns of this model
     */
    public int getColumnCount() {
        if (resultVector == null)
            return 0;

        return 4;
    }

    /**
     * Returns the value for the JTable for a given position.
     * @param row The row of the value
     * @param column The column of the value
     * @return value
     * */
    public Object getValueAt(int row, int column) {
        DataObject dataObject = (DataObject) resultVector.get(row);

        switch (column) {
            case 0:
                return dataObject.getKey();
            case 1:
                return dataObject;
            case 2:
                return ((dataObject.getCoreDistance() == DataObject.UNDEFINED) ?
                        "UNDEFINED" :
                        Utils.doubleToString(dataObject.getCoreDistance(), 3, 5));
            case 3:
                return ((dataObject.getReachabilityDistance() == DataObject.UNDEFINED) ?
                        "UNDEFINED" :
                        Utils.doubleToString(dataObject.getReachabilityDistance(), 3, 5));
            default:
                return "";
        }
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
