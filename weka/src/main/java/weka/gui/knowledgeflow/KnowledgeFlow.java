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
 *    KnowledgeFlow.java
 *    Copyright (C) 2016 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.knowledgeflow;

import weka.core.Copyright;
import weka.core.Version;

import java.util.Arrays;
import java.util.List;

/**
 * Launcher class for the Weka Knowledge Flow. Displays a splash screen and
 * launches the actual Knowledge Flow app (KnowledgeFlowApp).
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public class KnowledgeFlow {
  public static void main(String[] args) {
    List<String> message =
      Arrays.asList("WEKA Knowledge Flow", "Version " + Version.VERSION,
        "(c) " + Copyright.getFromYear() + " - " + Copyright.getToYear(),
        "The University of Waikato", "Hamilton, New Zealand");
    weka.gui.SplashWindow.splash(
      ClassLoader.getSystemResource("weka/gui/weka_icon_new.png"), message);
    weka.gui.SplashWindow.invokeMain("weka.gui.knowledgeflow.KnowledgeFlowApp",
      args);
    weka.gui.SplashWindow.disposeSplash();
  }
}
