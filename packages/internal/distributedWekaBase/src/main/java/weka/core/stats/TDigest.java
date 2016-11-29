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
 *    TDigest
 *    Copyright (C) 2016 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.core.stats;

import com.tdunning.math.stats.AVLTreeDigest;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Wrapper for TDigest quantile estimators. Currently provides the clearspring
 * analytics version and the latest and greatest from Ted Dunning.
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public abstract class TDigest implements Serializable {

  protected static final int STREAM_VERSION = 0;
  protected static final int T_DUNNING_VERSION = 1;
  private static final long serialVersionUID = -1312032238695779213L;

  protected static int s_versionToUse = T_DUNNING_VERSION;

  /**
   * Factory method. Currently returns Ted's AVL tree method
   *
   * @param compression the compression level to use
   * @return a TDigest estimator
   */
  public static TDigest createTDigest(double compression) {
    return s_versionToUse == STREAM_VERSION ? new StreamTDigest(compression)
      : new TDunningTDigest(compression);
  }

  /**
   * Add a point to the estimator
   *
   * @param x the point to add
   */
  public void add(double x) {
    add(x, 1);
  }

  /**
   * Add a point to the estimator
   *
   * @param x the point to add
   * @param w the weight of the point
   */
  public abstract void add(double x, int w);

  /**
   * CDF
   *
   * @param x point at which the CDF should be evaluated
   * @return the approximate fraction of all samples that were <= x
   */
  public abstract double cdf(double x);

  /**
   * Quantile estimate
   *
   * @param x which quantile to return
   * @return the quantile estimate
   */
  public abstract double quantile(double x);

  /**
   * Encode the estimator into a byte buffer
   *
   * @param buff the buffer to hold the encoded estimator
   */
  public abstract void asBytes(ByteBuffer buff);

  /**
   * More compact encoding
   *
   * @param buff the buffer to hold the encoded estiamtor
   */
  public abstract void asSmallBytes(ByteBuffer buff);

  /**
   * Number of bytes required for the standard encoding
   *
   * @return the number of bytes required
   */
  public abstract int byteSize();

  /**
   * Number of bytes required for the compact encoding
   *
   * @return the number of bytes required
   */
  public abstract int smallByteSize();

  /**
   * Get the current compression level
   *
   * @return the compression level
   */
  public abstract double compression();

  /**
   * Decode a TDigest estimator from a byte buffer
   *
   * @param buff the buffer
   * @return the decoded TDigest estimator
   */
  public static TDigest fromBytes(ByteBuffer buff) {
    if (s_versionToUse == STREAM_VERSION) {
      return StreamTDigest.fromBytes(buff);
    } else {
      return TDunningTDigest.fromBytes(buff);
    }
  }

  /**
   * Merge a collection of TDigest estimators into one
   *
   * @param compression the compression level
   * @param subData the collection of estimators to merge
   * @return a single merged estimator
   */
  public static TDigest merge(double compression, Iterable<TDigest> subData) {
    if (s_versionToUse == STREAM_VERSION) {
      return StreamTDigest.merge(compression, subData);
    } else {
      return TDunningTDigest.merge(compression, subData);
    }
  }

  /**
   * Wrapper for the clearspring analytics implementation
   */
  protected static class StreamTDigest extends TDigest {

    private static final long serialVersionUID = -3285851974002485443L;
    protected com.clearspring.analytics.stream.quantile.TDigest m_delegate;

    public StreamTDigest(double compression) {
      m_delegate =
        new com.clearspring.analytics.stream.quantile.TDigest(compression,
          new Random(1));
    }

    @Override
    public void add(double x, int w) {
      m_delegate.add(x, w);
    }

    @Override
    public double cdf(double x) {
      return m_delegate.cdf(x);
    }

    @Override
    public double quantile(double x) {
      return m_delegate.quantile(x);
    }

    @Override
    public void asBytes(ByteBuffer buff) {
      m_delegate.asBytes(buff);
    }

    @Override
    public void asSmallBytes(ByteBuffer buff) {
      m_delegate.asSmallBytes(buff);
    }

    @Override
    public int byteSize() {
      return m_delegate.byteSize();
    }

    @Override
    public int smallByteSize() {
      return m_delegate.smallByteSize();
    }

    @Override
    public double compression() {
      return m_delegate.compression();
    }

    public static TDigest fromBytes(ByteBuffer buff) {
      StreamTDigest result = new StreamTDigest(100.0);
      result.m_delegate =
        com.clearspring.analytics.stream.quantile.TDigest.fromBytes(buff);
      return result;
    }

    public static TDigest merge(double compression, Iterable<TDigest> subData) {
      StreamTDigest result = new StreamTDigest(100.0);
      List<com.clearspring.analytics.stream.quantile.TDigest> elements =
        new ArrayList<>();
      Iterator<TDigest> iter = subData.iterator();
      while (iter.hasNext()) {
        elements.add(((StreamTDigest) iter.next()).m_delegate);
      }

      com.clearspring.analytics.stream.quantile.TDigest merged =
        com.clearspring.analytics.stream.quantile.TDigest.merge(compression,
          elements);

      result.m_delegate = merged;
      return result;
    }
  }

  /**
   * Wrapper for Ted Dunning's AVL tree based implementation
   */
  protected static class TDunningTDigest extends TDigest {

    private static final long serialVersionUID = -6176977305286611713L;
    protected com.tdunning.math.stats.AVLTreeDigest m_delegate;

    public TDunningTDigest(double compression) {
      m_delegate = new AVLTreeDigest(compression);
    }

    @Override
    public void add(double x, int w) {
      m_delegate.add(x, w);
    }

    @Override
    public double cdf(double x) {
      return m_delegate.cdf(x);
    }

    @Override
    public double quantile(double x) {
      return m_delegate.quantile(x);
    }

    @Override
    public void asBytes(ByteBuffer buff) {
      m_delegate.asBytes(buff);
    }

    @Override
    public void asSmallBytes(ByteBuffer buff) {
      m_delegate.asSmallBytes(buff);
    }

    @Override
    public int byteSize() {
      return m_delegate.byteSize();
    }

    @Override
    public int smallByteSize() {
      return m_delegate.smallByteSize();
    }

    @Override
    public double compression() {
      return m_delegate.compression();
    }

    public static TDigest fromBytes(ByteBuffer buff) {
      TDunningTDigest result = new TDunningTDigest(100.0);
      result.m_delegate = AVLTreeDigest.fromBytes(buff);
      return result;
    }

    public static TDigest merge(double compression, Iterable<TDigest> subData) {
      TDunningTDigest result = new TDunningTDigest(compression);
      List<AVLTreeDigest> digests = new ArrayList<>();
      for (TDigest d : subData) {
        digests.add(((TDunningTDigest) d).m_delegate);
      }
      result.m_delegate = delegateMerge(compression, digests);

      return result;
    }

    protected static AVLTreeDigest delegateMerge(double compression,
      List<AVLTreeDigest> subData) {
      int n = Math.max(1, subData.size() / 4);

      AVLTreeDigest r = new AVLTreeDigest(compression);
      if (subData.get(0).isRecording()) {
        r.recordAllData();
      }

      for (int i = 0; i < subData.size(); i += n) {
        if (n > 1) {
          r.add(delegateMerge(compression,
            subData.subList(i, Math.min(i + n, subData.size()))));
        } else {
          r.add(subData.get(i));
        }
      }
      return r;
    }
  }
}
