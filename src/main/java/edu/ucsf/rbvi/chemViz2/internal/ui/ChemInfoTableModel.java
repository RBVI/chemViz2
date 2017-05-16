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

package edu.ucsf.rbvi.chemViz2.internal.ui;

import java.io.IOException;
import java.io.File;
import java.io.FileWriter;

import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import edu.ucsf.rbvi.chemViz2.internal.model.ChemInfoSettings;
import edu.ucsf.rbvi.chemViz2.internal.model.Compound;
import edu.ucsf.rbvi.chemViz2.internal.model.HTMLObject;
import edu.ucsf.rbvi.chemViz2.internal.ui.CompoundColumn;

import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyIdentifiable;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;


public class ChemInfoTableModel extends AbstractTableModel {
	CyNetwork network;
	ChemInfoSettings settings;
	List<CompoundColumn> columns;
	List<Compound> compoundList = null;
	boolean hasNodes = false;
	boolean hasEdges = false;

	public ChemInfoTableModel(CyNetwork network, List<Compound> cList, ChemInfoSettings settings) {
		super();
		columns = new ArrayList<CompoundColumn>();
		compoundList = cList;
		updateNodesAndEdges();
		this.network = network;
		this.settings = settings;
	}

	public void setCompoundList(List<Compound> cList) { 
		compoundList = cList; 
		updateNodesAndEdges();
	}
	public List<Compound> getCompoundList() { return compoundList; }
	public CyNetwork getNetwork() { return network; }
	public ChemInfoSettings getSettings() { return settings; }

	public boolean hasNodes() { return hasNodes; }
	public boolean hasEdges() { return hasEdges; }

	public void addColumn(int columnNumber, CompoundColumn column) {
		columns.add(columnNumber, column);
		fireTableStructureChanged();
	}

	public void removeColumn(int columnNumber) {
		columns.remove(columnNumber);
		fireTableStructureChanged();
	}

	public void removeColumn(CompoundColumn column) {
		columns.remove(column);
		fireTableStructureChanged();
	}

	public boolean containsColumn(String columnName) {
		for (CompoundColumn col: columns) {
			if (col.getColumnName().equals(columnName))
				return true;
		}
		return false;
	}

	public int getColumnCount() { return columns.size(); }
	public int getRowCount() { return compoundList.size(); }

	public String getColumnName(int columnIndex) {
		CompoundColumn column = columns.get(columnIndex);
		return column.getColumnName();
	}

	public Class getColumnClass(int columnIndex) {
		CompoundColumn column = columns.get(columnIndex);
		return column.getColumnClass();
	}
	
	public Object getValueAt(int row, int col) {
		Compound cmpd = compoundList.get(row);
		CompoundColumn column = columns.get(col);
		return column.getValue(cmpd);
	}

	public CompoundColumn getColumnAt(int columnIndex) {
		return columns.get(columnIndex);
	}

	public boolean isCellEditable(int row, int col) {
		// Class columnType = columns.get(col).getColumnClass();
		return true;
	}

	private void updateNodesAndEdges() {
		for (Compound c: compoundList) {
			CyIdentifiable source = c.getSource();
			if (source != null && source instanceof CyNode)
				hasNodes = true;
			if (source != null && source instanceof CyEdge)
				hasEdges = true;
		}
	}
}
