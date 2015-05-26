package weka.distributed.hadoop;

import java.io.IOException;
import java.util.List;

import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.LongWritable;

import weka.distributed.RecommenderSimilarityMapTask.PartialSimilarity;

public class RecommenderSimilarityHadoopCombiner extends
  RecommenderSimilarityHadoopReducer {

  protected LongWritable m_outKey = new LongWritable();
  protected BytesWritable m_outValue = new BytesWritable();

  public RecommenderSimilarityHadoopCombiner() {
    super();
    m_runningAsCombiner = true;
  }

  @Override
  public void reduce(LongWritable key, Iterable<BytesWritable> values,
    Context context) throws IOException {

    super.reduce(key, values, context);

    // now just need to emit the aggregated partial sims for this row
    List<PartialSimilarity> partiallyAggregatedForThisMatrixRow = m_task
      .getCurrentCombinerMatrixRow();

    for (PartialSimilarity sim : partiallyAggregatedForThisMatrixRow) {
      m_outKey.set(key.get()); // matrix row index
      byte[] serializedPartialSim = RecommenderSimilarityHadoopMapper
        .simToBytes(sim);
      m_outValue.set(serializedPartialSim, 0, serializedPartialSim.length);

      try {
        context.write(m_outKey, m_outValue);
      } catch (InterruptedException e) {
        throw new IOException(e);
      }
    }
  }
}
