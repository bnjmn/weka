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
 * IndividualLiteral.java
 *
 */

package weka.associations.tertius;

public class IndividualLiteral extends AttributeValueLiteral {

  private int m_type;
  public static int INDIVIDUAL_PROPERTY = 0;
  public static int PART_PROPERTY = 1;
  
  public IndividualLiteral(Predicate predicate, String value, int index,
			   int sign, int missing, int type) {

    super(predicate, value, index, sign, missing);
    m_type = type;
  }
  
  public int getType() {

    return m_type;
  }

}














