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
 *    ARulesViewer.java
 *    Copyright (C) 2010 University of Waikato
 *
 */

package weka.gui.visualize.plugins;

import weka.gui.visualize.plugins.AssociationRuleVisualizePlugin;

import java.io.Serializable;
import javax.swing.JMenuItem;
import javax.swing.JFrame;
import java.util.List;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import ar.ARPanel;
import weka.associations.AssociationRule;
import weka.associations.AssociationRules;
import weka.associations.Item;
import weka.core.Utils;

/**
 * Small class that implements 
 * weka.gui.visualize.plugins.AssociationRuleVisualizePlugin and links to
 * the ARPanel viewer.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class ARulesViewer implements Serializable, 
  AssociationRuleVisualizePlugin {
  
  public JMenuItem getVisualizeMenuItem(AssociationRules rules, String name) {
    final String xmlS = mapRulesToXML(rules.getRules());
    final String nameF = name;
    
    JMenuItem result = new JMenuItem("AR rules viewer");
    result.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        // TODO
        display(xmlS, nameF);
      }
    });
    
    return result;
  }
  
  protected String mapRulesToXML(List<AssociationRule> rules) {
    StringBuffer temp = new StringBuffer();
    
    temp.append("<?xml version=\"1.0\" encoding=\"iso-8859-15\"?>\n");
    temp.append("<RULES>\n");
    for (AssociationRule r : rules) {
      mapRuleToXML(r, temp);
    }
    
    temp.append("</RULES>\n");
    
    return temp.toString();
  }
  
  protected void mapRuleToXML(AssociationRule rule, StringBuffer result) {
    result.append("  <RULE>\n    <LHS>");
    for (Item b : rule.getPremise()) {
      result.append("\n      ");
      mapItemToXML(b, result);
    }
    result.append("\n    </LHS>\n    <RHS>");
    for (Item b : rule.getConsequence()) {
      result.append("\n      ");
      mapItemToXML(b, result);
    }
    result.append("\n    </RHS>");
    
    // metrics
    // do support first
    result.append("\n    <CRITERE name=\"support\" value=\"" 
        + rule.getTotalSupport() + "\"/>");
    
    String[] metricNames = rule.getMetricNamesForRule();
    double[] metricValues = null;
    try {
      metricValues = rule.getMetricValuesForRule();
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    for (int i = 0; i < metricNames.length; i++) {
      result.append("\n    ");
      result.append("<CRITERE name=\"" + metricNames[i] + "\" value=\" "
          + Utils.doubleToString(metricValues[i], 2) + "\"/>");      
    }
    result.append("\n  </RULE>\n");    
  }
  
  protected void mapItemToXML(Item b, StringBuffer result) {
    result.append("<ITEM name=\"" + b.getAttribute().name() + 
                  "\" value=\"" + b.getComparisonAsString() + 
                  b.getItemValueAsString() + "\"/>");
  }
  
  protected void display(String xmlRules, String name) {
    final JFrame frame;
    
    // display rules
    final ARPanel panel = new ARPanel();
    panel.setRulesXML(xmlRules);
    frame = new JFrame("AR Rules Viewer [" + name + "]");
    
    frame.addWindowListener(new java.awt.event.WindowAdapter() {
      public void windowClosing(java.awt.event.WindowEvent e) {
        panel.freeResources();
        frame.dispose();
      }
    });
    frame.setSize(800, 600);
    frame.setContentPane(panel);
    frame.setVisible(true);
  }
}
