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
 *    CriteriaSelectionPanel.java
 *    Copyright (C) 2003 DESS IAGL of Lille
 *
 */

package selection;

import javax.swing.*;
import javax.swing.border.TitledBorder;

import java.awt.*;
import sortedListPanel.*;

/**
 * @author beleg
 *
 * Panel that contains the JComboBoxes for selecting the criteria
 */
public class CriteriaSelectorPanel extends JPanel {

	private JComboBox box1;
	private JComboBox box2;
	private SorterComboBoxModel model1 = null;
	private SorterComboBoxModel model2 = null;

	public CriteriaSelectorPanel() {

		setLayout(new GridLayout(2, 1));

		box1 = new JComboBox();
		box2 = new JComboBox();

		add(box1);
		add(box2);

		setBorder(new TitledBorder("Criteria"));

	}

	public void setContents(String[] criteres) {

				
		if (model1 != null)
			model1.setElements(criteres);
		if (model2 != null)
			model2.setElements(criteres);
		else {
			model1 = new SorterComboBoxModel(criteres);
			model2 = new SorterComboBoxModel(criteres);
			box1.setModel(model1);
			box2.setModel(model2);
		}

	}
	
	public String[] getSelectedElements() {
	
		String[] res = new String[2];
		res[0] = (String) box1.getSelectedItem();
		res[1] = (String) box2.getSelectedItem();
		return res;
		
	}
}
