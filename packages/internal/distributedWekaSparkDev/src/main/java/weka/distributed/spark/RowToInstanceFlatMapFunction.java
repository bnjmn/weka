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
 *    RowToInstanceFlatMapFunction
 *    Copyright (C) 2016 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.distributed.spark;

import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.sql.Row;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Utils;
import weka.distributed.CSVToARFFHeaderMapTask;
import weka.distributed.CSVToARFFHeaderReduceTask;
import weka.distributed.DistributedWekaException;

import java.util.ArrayList;
import java.util.IllegalFormatException;
import java.util.Iterator;
import java.util.List;

/**
 * A flat map function to convert an {@code Object[]} array to {@code Instance}
 * s.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public class RowToInstanceFlatMapFunction implements
  FlatMapFunction<Iterator<Row>, Instance> {

  private static final long serialVersionUID = 6799719822725955936L;

  /** Header without summary data */
  protected Instances m_header;

  protected String m_mapTaskOpts;

  protected RowToInstanceIterable m_rowToInstance;

  protected boolean m_makeSparse;

  public RowToInstanceFlatMapFunction(Instances header, String mapTaskOpts,
    boolean sparse) throws DistributedWekaException {
    m_header = header;
    m_mapTaskOpts = mapTaskOpts;
    m_makeSparse = sparse;
  }

  @Override
  public Iterable<Instance> call(Iterator<Row> split)
    throws DistributedWekaException {
    if (m_rowToInstance == null) {
      m_rowToInstance =
        new RowToInstanceIterable(m_header, m_mapTaskOpts, m_makeSparse, split);
    }
    return m_rowToInstance;
  }

  protected static class RowToInstanceIterable implements
    Iterable<weka.core.Instance>, Iterator<weka.core.Instance> {

    protected Iterator<Row> m_rowIterator;

    /** Header without summary data */
    protected Instances m_header;

    /** Row helper for default nominal vals etc. */
    protected CSVToARFFHeaderMapTask m_rowHelper;

    /** A reusable array */
    protected Object[] m_reUsableRow;

    /** True if sparse instances are to be created */
    protected boolean m_makeSparse;

    public RowToInstanceIterable(Instances header, String mapTaskOpts,
      boolean sparse, Iterator<Row> rowIterator)
      throws DistributedWekaException {
      m_rowIterator = rowIterator;

      m_header = CSVToARFFHeaderReduceTask.stripSummaryAtts(header);
      m_header = m_header.stringFreeStructure();
      m_rowHelper = new CSVToARFFHeaderMapTask();
      m_makeSparse = sparse;
      try {
        m_rowHelper.setOptions(Utils.splitOptions(mapTaskOpts));
        List<String> namesForInitialization = new ArrayList<>();
        for (int i = 0; i < m_header.numAttributes(); i++) {
          namesForInitialization.add(m_header.attribute(i).name());
        }
        // this is just so that nominal att specs and default values (if any)
        // get initialized properly
        m_rowHelper.initParserOnly(namesForInitialization);
      } catch (Exception ex) {
        throw new DistributedWekaException(ex);
      }
    }

    @Override
    public Iterator<Instance> iterator() {
      return this;
    }

    @Override
    public boolean hasNext() {
      return m_rowIterator.hasNext();
    }

    @Override
    public weka.core.Instance next() {
      m_reUsableRow = new Object[m_header.numAttributes()];

      Row next = m_rowIterator.next();
      if (next.size() != m_header.numAttributes()) {
        throw new IllegalArgumentException("DataFrame row has a different "
          + "number of values than attributes specified in the ARFF header: "
          + next.size() + " vs " + m_header.numAttributes());
      }
      for (int i = 0; i < m_header.numAttributes(); i++) {
        m_reUsableRow[i] = next.get(i);
      }

      weka.core.Instance inst = null;
      try {
        inst =
          m_rowHelper.makeInstanceFromObjectRow(m_header, false, m_reUsableRow,
            m_makeSparse);
      } catch (Exception e) {
        e.printStackTrace();
        throw new IllegalStateException(e);
      }

      return inst;
    }

    @Override
    public void remove() {
    }
  }
}
