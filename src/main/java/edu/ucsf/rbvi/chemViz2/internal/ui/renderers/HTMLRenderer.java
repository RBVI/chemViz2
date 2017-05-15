/*
  Copyright (c) 2006, 2007, 2008 The Cytoscape Consortium (www.cytoscape.org)

  The Cytoscape Consortium is:
  - Institute for Systems Biology
  - University of California San Diego
  - Memorial Sloan-Kettering Cancer Center
  - Institut Pasteur
  - Agilent Technologies

  This library is free software; you can redistribute it and/or modify it
  under the terms of the GNU Lesser General Public License as published
  by the Free Software Foundation; either version 2.1 of the License, or
  any later version.

  This library is distributed in the hope that it will be useful, but
  WITHOUT ANY WARRANTY, WITHOUT EVEN THE IMPLIED WARRANTY OF
  MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.  The software and
  documentation provided hereunder is on an "as is" basis, and the
  Institute for Systems Biology and the Whitehead Institute
  have no obligations to provide maintenance, support,
  updates, enhancements or modifications.  In no event shall the
  Institute for Systems Biology and the Whitehead Institute
  be liable to any party for direct, indirect, special,
  incidental or consequential damages, including lost profits, arising
  out of the use of this software and its documentation, even if the
  Institute for Systems Biology and the Whitehead Institute
  have been advised of the possibility of such damage.  See
  the GNU Lesser General Public License for more details.

  You should have received a copy of the GNU Lesser General Public License
  along with this library; if not, write to the Free Software Foundation,
  Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
*/

package edu.ucsf.rbvi.chemViz2.internal.ui.renderers;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.event.MouseEvent;

import java.net.URI;

import javax.swing.JTextPane;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.HyperlinkEvent;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

import edu.ucsf.rbvi.chemViz2.internal.model.HTMLObject;

public class HTMLRenderer extends JTextPane implements TableCellRenderer {
	private final DefaultTableCellRenderer adaptee = new DefaultTableCellRenderer();

	public HTMLRenderer () {
		super();
	}

	public Component getTableCellRendererComponent(JTable table, final Object value, boolean isSelected,
	                                        boolean hasFocus, int row, int column) {
		adaptee.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
		this.setContentType("text/html");
		this.setText(value.toString());
		this.setCursor(new Cursor(Cursor.HAND_CURSOR));
		this.addHyperlinkListener(new MyHyperlinkListener());
		this.setEditable(false);
		this.setEnabled(true);
		setForeground(adaptee.getForeground());
		setBackground(adaptee.getBackground());
		return this;
	}

	public void processMouseEvent(MouseEvent e) {
		e.setSource(this);
		// System.out.println("mouseEvent: "+e);
		super.processMouseEvent(e);
	}

	class MyHyperlinkListener implements HyperlinkListener {
		public void hyperlinkUpdate(HyperlinkEvent e) {
			System.out.println("hyperlink: "+e);
			if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
				System.out.println("Hyperlink: "+e.getDescription());
			}
		}
	}
}
