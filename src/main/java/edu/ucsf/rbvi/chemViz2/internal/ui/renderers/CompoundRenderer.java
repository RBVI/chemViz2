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

import java.awt.Color;
import java.awt.Component;
import java.awt.Image;
import java.awt.Point;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;

import edu.ucsf.rbvi.chemViz2.internal.model.Compound;
import edu.ucsf.rbvi.chemViz2.internal.ui.CompoundTable;

import org.cytoscape.model.CyIdentifiable;

public class CompoundRenderer implements TableCellRenderer {

	private Map<CyIdentifiable,List<Integer>> rowMap;
	private TableRowSorter sorter;

	public CompoundRenderer(TableRowSorter sorter, Map<CyIdentifiable,List<Integer>> rm) {
		rowMap = rm;
		this.sorter = sorter;
	}

	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
	                                        boolean hasFocus, int viewRow, int viewColumn) {

		int row = sorter.convertRowIndexToModel(viewRow);
		int column = table.convertColumnIndexToModel(viewColumn);

		Compound c = (Compound)table.getModel().getValueAt(row, column);
		TableColumn clm = table.getColumnModel().getColumn(viewColumn);
		int width = clm.getPreferredWidth();
		if (width != table.getRowHeight())
			table.setRowHeight(width); // Note, this will trigger a repaint!
		Image resizedImage = c.getImage(width,width,Color.WHITE);
		JLabel l;
		if (resizedImage != null)
			l = new JLabel(new ImageIcon(resizedImage));
		else
			l = new JLabel("No Image Available", JLabel.CENTER);
		if (!rowMap.containsKey(c.getSource())) {
			rowMap.put(c.getSource(), new ArrayList<Integer>());
		}

		// Paint border
		if (isSelected) {
			l.setBorder(CompoundTable.SELECTED_BORDER);
		} else {
			l.setBorder(CompoundTable.CELL_BORDER);
		}

		rowMap.get(c.getSource()).add(Integer.valueOf(row));
		return l;
	}
}
