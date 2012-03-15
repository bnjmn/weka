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
 *    SelectionPanel.java
 *    Copyright (C) 2003 DESS IAGL of Lille
 *
 */

package selection;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedList;

import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;

import rules.Rule;

import drawingTools.ColorArray;

public class RulesSelectionPanel extends JPanel implements ActionListener {

	private DataPanel panel;
	private Rule[] rules;
	private GridBagLayout gb;
	private boolean singleSelection = false;
	private JPanel rulesPanel;
	private JRadioButton[] buttons;
	private boolean isColored = false;
	private ActionListener listener;
	private GridBagConstraints gridBagConstraints;
	private JScrollPane scrollable;
	
	public RulesSelectionPanel(DataPanel aDataPanel) {

		panel = aDataPanel;
		initComponents();

	}

	public void initComponents() {

		gb = new GridBagLayout();
		this.setLayout(gb);

		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.gridwidth = 1;
		gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
		gridBagConstraints.weightx = 100.0;
		gridBagConstraints.weighty = 80.0;
		add(panel, gridBagConstraints);

		rulesPanel = new JPanel();
		FlowLayout fl = new FlowLayout(FlowLayout.CENTER, 5, 0);
		rulesPanel.setLayout(fl);
		scrollable = new JScrollPane();
		scrollable.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
		scrollable.setViewportView(rulesPanel);

		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 1;
		gridBagConstraints.gridwidth = 1;
		gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
		gridBagConstraints.weightx = 100.0;
		gridBagConstraints.weighty = 20.0;
		add(scrollable, gridBagConstraints);

	}

	public void setData(Rule[] rules, String[] criteres) {

		this.rules = rules;
		rulesPanel.removeAll();

		ButtonGroup bg = new ButtonGroup();
		buttons = new JRadioButton[rules.length];

		for (int i = 0; i < rules.length; i++) {
			buttons[i] = new JRadioButton(rules[i].getId());
			buttons[i].setToolTipText(rules[i].toString());
			buttons[i].addActionListener(this);
			if (isColored)
				if(rules.length <15)
					buttons[i].setForeground(ColorArray.colorArray[i]);
				else
					buttons[i].setForeground(Color.BLACK);	
			if (singleSelection)
				bg.add(buttons[i]);
			rulesPanel.add(buttons[i]);
		}
		
		panel.setData(rules, criteres);
		rulesPanel.repaint();
		scrollable.repaint();
		this.repaint();
	}

	public void setSingleSelection() {
		singleSelection = true;
	}

	public void setColored() {
		isColored = true;
	}
	
	public void setSelectedRules(Rule[] selectedRules) {
		if (selectedRules != null)
			for (int i = 0; i < rules.length; i++)
				for (int j = 0; j < selectedRules.length; j++) {
					if (rules[i] == selectedRules[j]) {
						buttons[i].setSelected(true);
					}
				}
	}

	public void actionPerformed(ActionEvent evt) {
		evt.setSource(this);
		listener.actionPerformed(evt);
	}

	public void addActionListener(ActionListener listener) {
		this.listener = listener;
	}

	public Rule[] getSelectedRules() {
		LinkedList selectedRules = new LinkedList();
		for (int i = 0; i < buttons.length; i++) {
			JRadioButton button = buttons[i];
			if (button.isSelected())
				selectedRules.add(rules[i]);
		}

		return (Rule[]) selectedRules.toArray(new Rule[selectedRules.size()]);
	}

}
