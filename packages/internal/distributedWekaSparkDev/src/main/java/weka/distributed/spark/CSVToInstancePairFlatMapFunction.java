package weka.distributed.spark;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.spark.api.java.function.FlatMapFunction;

import scala.Tuple2;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Utils;
import weka.distributed.CSVToARFFHeaderMapTask;
import weka.distributed.CSVToARFFHeaderReduceTask;
import weka.distributed.DistributedWekaException;
import distributed.core.DistributedJob;

public class CSVToInstancePairFlatMapFunction implements
  FlatMapFunction<Iterator<Tuple2<Integer, Object>>, Instance> {

  /**
   * For serialization
   */
  private static final long serialVersionUID = 1282966807848508338L;

  protected Instances m_header;
  protected List<Instance> m_list = new ArrayList<Instance>();
  protected CSVToARFFHeaderMapTask m_rowHelper;
  protected String m_csvOpts;

  public CSVToInstancePairFlatMapFunction(Instances header, String csvOpts) {
    m_header = header;
    m_csvOpts = csvOpts;
  }

  @Override
  public Iterable<Instance> call(Iterator<Tuple2<Integer, Object>> split)
    throws IOException, DistributedWekaException {

    m_rowHelper = new CSVToARFFHeaderMapTask();
    try {
      m_header = CSVToARFFHeaderReduceTask.stripSummaryAtts(m_header);
      m_rowHelper.setOptions(Utils.splitOptions(m_csvOpts));
      m_rowHelper.initParserOnly(CSVToARFFHeaderMapTask
        .instanceHeaderToAttributeNameList(m_header));
    } catch (Exception e) {
      throw new DistributedWekaException(e);
    }

    while (split.hasNext()) {
      // accumulate String atts in memory
      Instance inst =
        DistributedJob.parseInstance(split.next()._2.toString(), m_rowHelper,
          m_header, false);
      m_list.add(inst);
    }

    return m_list;
  }
}
