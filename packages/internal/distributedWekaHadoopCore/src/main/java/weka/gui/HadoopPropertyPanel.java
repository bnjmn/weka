package weka.gui;

import distributed.core.DistributedJobConfig;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JTable;
import java.awt.BorderLayout;
import java.util.HashMap;
import java.util.Map;

/**
 * Panel for editing user-defined properties to set on the Hadoop
 * Configuration object
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 */
public class HadoopPropertyPanel extends JPanel {

  /** For serialization */
  private static final long serialVersionUID = 7587461519469576557L;

  /** The JTable for configuring properties */
  protected InteractiveTablePanel
    m_table = new InteractiveTablePanel(
    new String[] { "Property", "Value", "" });

  /**
   * Constructor
   *
   * @param properties a map of properties to edit
   */
  public HadoopPropertyPanel(Map<String, String> properties) {
    setLayout(new BorderLayout());
    setBorder(BorderFactory.createTitledBorder("User defined properties"));
    add(m_table, BorderLayout.CENTER);

    // populate table with supplied properties
    if (properties != null) {
      int row = 0;
      JTable table = m_table.getTable();
      for (Map.Entry<String, String> e : properties.entrySet()) {
        String prop = e.getKey();
        String val = e.getValue();

        // make sure to skip internal weka properties!!
        if (!DistributedJobConfig.isEmpty(val) && !prop.startsWith("*")) {
          table.getModel().setValueAt(prop, row, 0);
          table.getModel().setValueAt(val, row, 1);
          ((InteractiveTableModel) table.getModel()).addEmptyRow();
          row++;
        }
      }
    }
  }

  /**
   * Get the properties being edited
   *
   * @return the map of properties being edited
   */
  public Map<String, String> getProperties() {
    Map<String, String> result = new HashMap<String, String>();
    JTable table = m_table.getTable();
    int numRows = table.getModel().getRowCount();

    for (int i = 0; i < numRows; i++) {
      String paramName = table.getValueAt(i, 0).toString();
      String paramValue = table.getValueAt(i, 1).toString();
      if (paramName.length() > 0 && paramValue.length() > 0) {
        result.put(paramName, paramValue);
      }
    }

    return result;
  }
}
