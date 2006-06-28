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

package weka.clusterers.forOPTICSAndDBScan.OPTICS_GUI;

import weka.clusterers.forOPTICSAndDBScan.DataObjects.DataObject;
import weka.core.FastVector;
import weka.core.Utils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

/**
 * <p>
 * GraphPanel.java <br/>
 * Authors: Rainer Holzmann, Zhanna Melnikova-Albrecht <br/>
 * Date: Sep 16, 2004 <br/>
 * Time: 10:28:19 AM <br/>
 * $ Revision 1.4 $ <br/>
 * </p>
 *
 * @author Zhanna Melnikova-Albrecht (melnikov@cip.ifi.lmu.de)
 * @author Rainer Holzmann (holzmann@cip.ifi.lmu.de)
 * @version $Revision: 1.2 $
 */
public class GraphPanel extends JComponent {

    /**
     * Holds the clustering results
     */
    private FastVector resultVector;

    /**
     * Holds the value that is multiplied with the original values of core- and reachability
     * distances in order to get better graphical views
     */
    private int verticalAdjustment;

    /**
     * Specifies the color for displaying core-distances
     */
    private Color coreDistanceColor;

    /**
     * Specifies the color for displaying reachability-distances
     */
    private Color reachabilityDistanceColor;

    /**
     * Specifies the width for displaying the distances
     */
    private int widthSlider;

    /**
     * Holds the flag for showCoreDistances
     */
    private boolean showCoreDistances;

    /**
     * Holds the flag for showrRechabilityDistances
     */
    private boolean showReachabilityDistances;

    /**
     * Holds the index of the last toolTip
     */
    private int recentIndex = -1;

    // *****************************************************************************************************************
    // constructors
    // *****************************************************************************************************************

    public GraphPanel(FastVector resultVector,
                      int verticalAdjustment,
                      boolean showCoreDistances,
                      boolean showReachbilityDistances) {
        this.resultVector = resultVector;
        this.verticalAdjustment = verticalAdjustment;
        coreDistanceColor = new Color(100, 100, 100);
        reachabilityDistanceColor = Color.orange;
        widthSlider = 5;
        this.showCoreDistances = showCoreDistances;
        this.showReachabilityDistances = showReachbilityDistances;

        addMouseMotionListener(new MouseHandler());
    }

    // *****************************************************************************************************************
    // methods
    // *****************************************************************************************************************

    /**
     * Draws the OPTICS Plot
     * @param g
     */
    protected void paintComponent(Graphics g) {
        if (isOpaque()) {
            Dimension size = getSize();
            g.setColor(getBackground());
            g.fillRect(0, 0, size.width, size.height);
        }

        int stepSize = 0;
        int cDist = 0;
        int rDist = 0;

        for (int vectorIndex = 0; vectorIndex < resultVector.size(); vectorIndex++) {
            double coreDistance = ((DataObject) resultVector.elementAt(vectorIndex)).getCoreDistance();
            double reachDistance = ((DataObject) resultVector.elementAt(vectorIndex)).getReachabilityDistance();

            if (coreDistance == DataObject.UNDEFINED)
                cDist = getHeight();
            else
                cDist = (int) (coreDistance * verticalAdjustment);

            if (reachDistance == DataObject.UNDEFINED)
                rDist = getHeight();
            else
                rDist = (int) (reachDistance * verticalAdjustment);

            int x = vectorIndex + stepSize;

            if (isShowCoreDistances()) {
                /**
                 * Draw coreDistance
                 */
                g.setColor(coreDistanceColor);
                g.fillRect(x, getHeight() - cDist, widthSlider, cDist);
            }

            if (isShowReachabilityDistances()) {
                int sizer = widthSlider;
                if (!isShowCoreDistances()) sizer = 0;
                /**
                 * Draw reachabilityDistance
                 */
                g.setColor(reachabilityDistanceColor);
                g.fillRect(x + sizer, getHeight() - rDist, widthSlider, rDist);
            }

            if (isShowCoreDistances() && isShowReachabilityDistances()) {
                stepSize += (widthSlider * 2);
            } else
                stepSize += widthSlider;
        }
    }

    /**
     * Sets a new resultVector
     * @param resultVector
     */
    public void setResultVector(FastVector resultVector) {
        this.resultVector = resultVector;
    }

    /**
     * Displays a toolTip for the selected DataObject
     * @param toolTip
     */
    public void setNewToolTip(String toolTip) {
        setToolTipText(toolTip);
    }

    /**
     * Adjusts the size of this panel in respect of the shown content
     * @param serObject SERObject that contains the OPTICS clustering results
     */
    public void adjustSize(SERObject serObject) {
        int i = 0;
        if (isShowCoreDistances() && isShowReachabilityDistances())
            i = 10;
        else if ((isShowCoreDistances() && !isShowReachabilityDistances()) ||
                !isShowCoreDistances() && isShowReachabilityDistances())
            i = 5;
        setSize(new Dimension((i * serObject.getDatabaseSize()) +
                serObject.getDatabaseSize(), getHeight()));
        setPreferredSize(new Dimension((i * serObject.getDatabaseSize()) +
                serObject.getDatabaseSize(), getHeight()));
    }

    /**
     * Returns the flag for showCoreDistances
     * @return True or false
     */
    public boolean isShowCoreDistances() {
        return showCoreDistances;
    }

    /**
     * Sets the flag for showCoreDistances
     * @param showCoreDistances
     */
    public void setShowCoreDistances(boolean showCoreDistances) {
        this.showCoreDistances = showCoreDistances;
    }

    /**
     * Returns the flag for showReachabilityDistances
     * @return True or false
     */
    public boolean isShowReachabilityDistances() {
        return showReachabilityDistances;
    }

    /**
     * Sets the flag for showReachabilityDistances
     * @param showReachabilityDistances
     */
    public void setShowReachabilityDistances(boolean showReachabilityDistances) {
        this.showReachabilityDistances = showReachabilityDistances;
    }

    /**
     * Sets a new value for the vertical verticalAdjustment
     * @param verticalAdjustment
     */
    public void setVerticalAdjustment(int verticalAdjustment) {
        this.verticalAdjustment = verticalAdjustment;
    }

    /**
     * Sets a new color for the coreDistance
     * @param coreDistanceColor
     */
    public void setCoreDistanceColor(Color coreDistanceColor) {
        this.coreDistanceColor = coreDistanceColor;
        repaint();
    }

    /**
     * Sets a new color for the reachabilityDistance
     * @param reachabilityDistanceColor
     */
    public void setReachabilityDistanceColor(Color reachabilityDistanceColor) {
        this.reachabilityDistanceColor = reachabilityDistanceColor;
        repaint();
    }

    // *****************************************************************************************************************
    // inner classes
    // *****************************************************************************************************************

    private class MouseHandler extends MouseMotionAdapter {
        /**
         * Invoked when the mouse button has been moved on a component
         * (with no buttons no down).
         */
        public void mouseMoved(MouseEvent e) {
            showToolTip(e.getX());
        }

        /**
         * Shows a toolTip with the dataObjects parameters (c-dist, r-dist, key, attributes . . .)
         * @param x MouseCoordinate X
         * @return boolean
         */
        private boolean showToolTip(int x) {
            int i = 0;
            if (isShowCoreDistances() && isShowReachabilityDistances())
                i = 11;
            else if ((isShowCoreDistances() && !isShowReachabilityDistances()) ||
                    !isShowCoreDistances() && isShowReachabilityDistances() ||
                    !isShowCoreDistances() && !isShowReachabilityDistances())
                i = 6;
            if ((x / i) == recentIndex)
                return false;
            else
                recentIndex = x / i;
            DataObject dataObject = null;
            try {
                dataObject = (DataObject) resultVector.elementAt(recentIndex);
            } catch (Exception e) {
            }
            if (dataObject != null) {
                if (!isShowCoreDistances() && !isShowReachabilityDistances()) {
                    setNewToolTip("<html><body><b>Please select a distance" +
                            "</b></body></html>"
                    );
                } else
                    setNewToolTip("<html><body><table>" +
                            "<tr><td>DataObject:</td><td>" + dataObject + "</td></tr>" +
                            "<tr><td>Key:</td><td>" + dataObject.getKey() + "</td></tr>" +
                            "<tr><td>" +
                            (isShowCoreDistances() ? "<b>" : "") + "Core-Distance:" +
                            (isShowCoreDistances() ? "</b>" : "") +
                            "</td><td>" +
                            (isShowCoreDistances() ? "<b>" : "") +
                            ((dataObject.getCoreDistance() == DataObject.UNDEFINED) ? "UNDEFINED" :
                            Utils.doubleToString(dataObject.getCoreDistance(), 3, 5)) +
                            (isShowCoreDistances() ? "</b>" : "") +
                            "</td></tr>" +
                            "<tr><td>" +
                            (isShowReachabilityDistances() ? "<b>" : "") + "Reachability-Distance:" +
                            (isShowReachabilityDistances() ? "</b>" : "") +
                            "</td><td>" +
                            (isShowReachabilityDistances() ? "<b>" : "") +
                            ((dataObject.getReachabilityDistance() == DataObject.UNDEFINED) ? "UNDEFINED" :
                            Utils.doubleToString(dataObject.getReachabilityDistance(), 3, 5)) +
                            (isShowReachabilityDistances() ? "</b>" : "") +
                            "</td></tr>" +
                            "</table></body></html>"
                    );
            }
            return true;
        }
    }

}
