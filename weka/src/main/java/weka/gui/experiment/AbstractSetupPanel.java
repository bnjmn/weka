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

/**
 * AbstractSetupPanel.java
 * Copyright (C) 2015 University of Waikato, Hamilton, NZ
 */

package weka.gui.experiment;

import weka.core.PluginManager;
import weka.experiment.Experiment;

import javax.swing.JPanel;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Ancestor for setup panels for experiments.
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision$
 */
public abstract class AbstractSetupPanel
  extends JPanel
  implements Comparable<AbstractSetupPanel> {

  /**
   * Returns the name of the panel.
   *
   * @return		the name
   */
  public abstract String getName();

  /**
   * Sets the panel used to switch between simple and advanced modes.
   *
   * @param modePanel the panel
   */
  public abstract void setModePanel(SetupModePanel modePanel);

  /**
   * Sets the experiment to configure.
   *
   * @param exp a value of type 'Experiment'
   * @return true if experiment could be configured, false otherwise
   */
  public abstract boolean setExperiment(Experiment exp);

  /**
   * Gets the currently configured experiment.
   *
   * @return the currently configured experiment.
   */
  public abstract Experiment getExperiment();

  /**
   * Hook method for cleaning up the interface after a switch.
   * <br>
   * Default implementation does nothing.
   */
  public void cleanUpAfterSwitch() {
  }

  /**
   * Adds a PropertyChangeListener who will be notified of value changes.
   *
   * @param l a value of type 'PropertyChangeListener'
   */
  public abstract void addPropertyChangeListener(PropertyChangeListener l);

  /**
   * Removes a PropertyChangeListener.
   *
   * @param l a value of type 'PropertyChangeListener'
   */
  public abstract void removePropertyChangeListener(PropertyChangeListener l);

  /**
   * Uses the name for comparison.
   *
   * @param o the other panel
   * @return <0 , 0, >0 if name is less than, equal or greater than this one
   * @see #getName()
   */
  public int compareTo(AbstractSetupPanel o) {
    return getName().compareTo(o.getName());
  }

  /**
   * Just returns the name of the panel.
   *
   * @return		the name
   */
  public String toString() {
    return getName();
  }

  /**
   * Returns a list of all available setup panels.
   *
   * @return		the available panels
   */
  public static AbstractSetupPanel[] getPanels() {
    List<AbstractSetupPanel>	result;
    List<String> 		names;
    Class			cls;
    AbstractSetupPanel		panel;

    result = new ArrayList<AbstractSetupPanel>();
    names  = PluginManager.getPluginNamesOfTypeList(AbstractSetupPanel.class.getName());
    for (String name: names) {
      try {
	cls   = Class.forName(name);
	panel = (AbstractSetupPanel) cls.newInstance();
	result.add(panel);
      }
      catch (Exception e) {
	System.err.println("Failed to instantiate setup panel: " + name);
	e.printStackTrace();
      }
    }

    Collections.sort(result);

    return result.toArray(new AbstractSetupPanel[result.size()]);
  }
}
