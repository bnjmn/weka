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
 *    HelpFrame.java
 *    Copyright (C) 2003 DESS IAGL of Lille
 *
 */

package help;

import java.io.File;
import java.io.IOException;

import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

/**
 * @author beleg
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class HelpFrame extends JFrame implements HyperlinkListener {

	private JEditorPane pane;

	public HelpFrame(String file) throws IOException {

		File f = new File(file);
		

		pane = new JEditorPane(f.toURL());
		pane.setEditable(false);
		pane.addHyperlinkListener(this);
		JScrollPane scrollable = new JScrollPane();
		scrollable.setViewportView(pane);
		getContentPane().add(scrollable);
		setSize(640, 480);
		setTitle("ARViewer Help");
		addWindowListener(new java.awt.event.WindowAdapter() {
			public void windowClosing(java.awt.event.WindowEvent evt) {
				exitForm(evt);
			}
		});
	}

	
	/**
	 * @see javax.swing.event.HyperlinkListener#hyperlinkUpdate(javax.swing.event.HyperlinkEvent)
	 */
	public void hyperlinkUpdate(HyperlinkEvent e) {
		if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
			try {
				pane.setPage(e.getURL());
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}
	}
	
	/** Exit the Application */
		private void exitForm(java.awt.event.WindowEvent evt) {
			this.dispose();
		}
}
