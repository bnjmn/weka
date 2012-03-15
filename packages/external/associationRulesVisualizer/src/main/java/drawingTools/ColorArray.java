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
 *    ColorArray.java
 *    Copyright (C) 2003 DESS IAGL of Lille
 *
 */

package drawingTools;

import java.awt.Color;

/**
 * @author Sebastien
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class ColorArray {

	public static Color[] colorArray = new Color[16];
	static {
    colorArray[0]  = new Color(  0,   0, 255);//blue
    colorArray[1]  = new Color(  0, 255,   0);//green
    colorArray[2]  = new Color(255,   0,   0);//red
    
    colorArray[3]  = new Color(255, 128,   0);//orange

    colorArray[4]  = new Color(  0, 255, 255);//light blue
    colorArray[5]  = new Color(255, 255,   0);//yellow
    colorArray[6]  = new Color(255,   0, 255);//pink
    
    colorArray[7]  = new Color(160,   0,   0);//dark red
    colorArray[8]  = new Color(  0, 160,   0);//dark green
    colorArray[9]  = new Color(  0,   0, 160);//dark blue
    
    colorArray[10] = new Color(255, 128, 255);//light pink
    colorArray[11] = new Color(128, 255, 255);//light blue
    colorArray[12] = new Color(255, 255, 128);//light yellow
    
    colorArray[13] = new Color(128, 128,   0);//dark yellow
    colorArray[14] = new Color(128,   0, 128);//mauve
    colorArray[15] = new Color(  0, 128, 128);//light pink
    

    
	}


}
