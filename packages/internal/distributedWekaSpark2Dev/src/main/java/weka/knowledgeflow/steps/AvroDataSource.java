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
 *    AvroDataSource
 *    Copyright (C) 2016 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.knowledgeflow.steps;

import weka.gui.knowledgeflow.KFGUIConsts;

/**
 * Knowledge Flow step wrapping the AvroDataSource
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
@KFStep(name = "AvroDataSource", category = "Spark",
  toolTipText = "Loads data from a Avro file",
  iconPath = KFGUIConsts.BASE_ICON_PATH + "DefaultDataSource.gif")
public class AvroDataSource extends AbstractDataSource {

  private static final long serialVersionUID = 3345988039878734649L;

  public AvroDataSource() {
    super();

    m_dataSource = new weka.distributed.spark.AvroDataSource();
  }
}
