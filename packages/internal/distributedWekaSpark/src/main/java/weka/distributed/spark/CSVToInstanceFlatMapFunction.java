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
 *    CSVToInstanceFlatMapFunction
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.distributed.spark;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.spark.api.java.function.FlatMapFunction;

import weka.core.Instance;
import weka.core.Instances;
import weka.core.Utils;
import weka.distributed.CSVToARFFHeaderMapTask;
import weka.distributed.CSVToARFFHeaderReduceTask;
import weka.distributed.DistributedWekaException;
import distributed.core.DistributedJob;

/**
 * A flat map function to convert CSV string data into Instances
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class CSVToInstanceFlatMapFunction implements
  FlatMapFunction<Iterator<String>, Instance> {

  static final long serialVersionUID = -8451426962268660415L;

  protected Instances m_header;
  protected List<Instance> m_list = new ArrayList<Instance>();
  protected CSVToARFFHeaderMapTask m_rowHelper;
  protected String m_csvOpts;

  public CSVToInstanceFlatMapFunction(Instances header, String csvOpts) {
    m_header = header;
    m_csvOpts = csvOpts;
  }

  @Override
  public Iterable<Instance> call(Iterator<String> split) throws IOException,
    DistributedWekaException {

    m_rowHelper = new CSVToARFFHeaderMapTask();
    try {
      m_header = CSVToARFFHeaderReduceTask.stripSummaryAtts(m_header);
      m_header = m_header.stringFreeStructure();
      m_rowHelper.setOptions(Utils.splitOptions(m_csvOpts));
      m_rowHelper.initParserOnly(CSVToARFFHeaderMapTask
        .instanceHeaderToAttributeNameList(m_header));
    } catch (Exception e) {
      throw new DistributedWekaException(e);
    }

    while (split.hasNext()) {
      // accumulate String atts in memory
      String nextS = split.next();
      try {
        Instance inst =
          DistributedJob.parseInstance(nextS, m_rowHelper, m_header, false);
        m_list.add(inst);
      } catch (IOException ex) {
        System.err
          .println("[CSVToInstanceFlatMapFunction] Problem parsing row (discarded): "
            + nextS);
        // throw new DistributedWekaException(ex);
      }
    }

    return m_list;
  }
}
