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
 *    DataSink
 *    Copyright (C) 2016 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.distributed.spark;

import weka.core.Option;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;

/**
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public abstract class DataSink extends SparkJob {

  private static final long serialVersionUID = -3107879522154123160L;

  public DataSink() {
    super("Data sink", "Save data from data frames");
  }

  @Override
  public Enumeration<Option> listOptions() {
    Vector<Option> opts = new Vector<Option>();
    Enumeration<Option> e = super.listOptions();
    while (e.hasMoreElements()) {
      opts.add(e.nextElement());
    }

    opts.addAll(Option.listOptionsForClassHierarchy(this.getClass(),
      SparkJob.class));

    return opts.elements();
  }

  @Override
  public void setOptions(String[] opts) throws Exception {
    super.setOptions(opts);

    Option.setOptionsForHierarchy(opts, this, SparkJob.class);
  }

  @Override
  public String[] getOptions() {
    List<String> opts = new ArrayList<String>();
    opts.addAll(Arrays.asList(super.getOptions()));

    opts.addAll(Arrays.asList(Option.getOptionsForHierarchy(this, SparkJob.class)));

    return opts.toArray(new String[opts.size()]);
  }

  public String[] getBaseAndDSOptionsOnly() {
    List<String> opts = new ArrayList<String>();
    opts.addAll(Arrays.asList(super.getBaseOptionsOnly()));
    opts.addAll(Arrays.asList(Option.getOptions(this, this.getClass())));
    return opts.toArray(new String[opts.size()]);
  }
}
