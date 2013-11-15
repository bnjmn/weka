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
 *    DFSConverterUtils.java
 *    Copyright (C) 2013 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.core.converters;

import java.io.File;
import java.util.Enumeration;

import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.Utils;
import distributed.core.DistributedJobConfig;

/**
 * Utility routines for the HDFSSaver and HDFSLoader.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class DFSConverterUtils {

  /**
   * generates a string suitable for output on the command line displaying all
   * available options.
   * 
   * @param converter the saver to create the option string for
   * @return the option string
   */
  protected static String makeOptionStr(Object converter) {
    StringBuffer result;
    Option option;

    result = new StringBuffer();

    // build option string
    result.append("\n");
    result.append(converter.getClass().getName().replaceAll(".*\\.", ""));
    result.append(" options:\n\n");
    result.append("-i <file>\n\tThe input ARFF file to load\n");
    result
      .append("-incremental\n\tLoad and save incrementally (if supported)\n");

    // TODO handle arbitrary input file types
    // TODO options for incremental mode (if one or both of the input
    // file loader or the saver support it)

    if (converter instanceof OptionHandler) {
      Enumeration enm = ((OptionHandler) converter).listOptions();
      while (enm.hasMoreElements()) {
        option = (Option) enm.nextElement();
        result.append(option.synopsis() + "\n");
        result.append(option.description() + "\n");
      }
    }

    return result.toString();
  }

  /**
   * Run the supplied loader. Parses options and prints help if necessary.
   * 
   * @param loader the loader to run
   * @param options the options to set on the loader
   */
  public static void runLoader(Loader loader, String[] options) {
    // help request?
    try {
      String[] tmpOptions = options.clone();
      if (Utils.getFlag('h', tmpOptions)) {
        System.err.println("\nHelp requested\n" + makeOptionStr(loader));
        return;
      }
    } catch (Exception e) {
      // ignore it
    }

    try {
      boolean incremental = Utils.getFlag("incremental", options)
        && (loader instanceof IncrementalConverter);

      if (loader instanceof OptionHandler) {
        ((OptionHandler) loader).setOptions(options);
      }

      if (incremental) {
        Instances structure = loader.getStructure();
        System.out.println(structure);

        Instance temp;

        do {
          temp = loader.getNextInstance(structure);
          if (temp != null)
            System.out.println(temp);
        } while (temp != null);
      } else {
        System.out.println(loader.getDataSet());
      }
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  /**
   * Run the supplied saver. Parses options and prints help if necessary.
   * 
   * @param saver the saver to run
   * @param options the options to set on the saver
   */
  public static void runSaver(Saver saver, String[] options) {
    // help request?
    try {
      String[] tmpOptions = options.clone();
      if (Utils.getFlag('h', tmpOptions)) {
        System.err.println("\nHelp requested\n" + makeOptionStr(saver));
        return;
      }
    } catch (Exception e) {
      // ignore it
    }

    try {
      String inputFile = Utils.getOption('i', options);
      if (DistributedJobConfig.isEmpty(inputFile)) {
        throw new Exception("No input file specified!");
      }

      boolean incremental = Utils.getFlag("incremental", options)
        && (saver instanceof IncrementalConverter);

      if (saver instanceof OptionHandler) {
        ((OptionHandler) saver).setOptions(options);
      }

      // load input
      ArffLoader loader = new ArffLoader();
      File input = new File(inputFile);
      loader.setFile(input);

      if (incremental) {
        Instances structure = loader.getStructure();
        saver.setRetrieval(Saver.INCREMENTAL);
        saver.setInstances(structure);
        Instance nextI = loader.getNextInstance(structure);
        boolean done = false;
        while (!done) {
          saver.writeIncremental(nextI);
          if (nextI == null) {
            done = true;
          } else {
            nextI = loader.getNextInstance(structure);
          }
        }
      } else {
        saver.setInstances(loader.getDataSet());
        saver.writeBatch();
      }

    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }
}
