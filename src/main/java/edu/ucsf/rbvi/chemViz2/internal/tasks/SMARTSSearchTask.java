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

package edu.ucsf.rbvi.chemViz2.internal.tasks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.cytoscape.model.CyIdentifiable;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;

import edu.ucsf.rbvi.chemViz2.internal.model.ChemInfoSettings;
import edu.ucsf.rbvi.chemViz2.internal.model.Compound;
import edu.ucsf.rbvi.chemViz2.internal.model.Compound.AttriType;
import edu.ucsf.rbvi.chemViz2.internal.tasks.ChemVizAbstractTaskFactory.Scope;
import edu.ucsf.rbvi.chemViz2.internal.ui.CompoundTable;

import org.openscience.cdk.smiles.smarts.SMARTSQueryTool;

/**
 * The SMARTSSearchTask selects input from the user to get a SMARTS string and
 * searches through the list of objects passed in its constructor and then creates 
 * a JTable of compounds that match the SMARTS string.
 */
public class SMARTSSearchTask extends AbstractCompoundTask {
	List<? extends CyIdentifiable> objectList;
	ChemInfoSettings settings;
	CompoundTable	compoundTable = null;
	List<String> columnList = null;
	CyNetwork network;
	Scope scope;
	boolean showTable;

	@Tunable(description="Enter the SMARTS search string")
	public String searchString;

	/**
 	 * Creates the task.
 	 *
 	 * @param selection the group of graph objects that should be included in the table
 	 * @param settings the settings object, which we use to pull the attribute names that contain the compound descriptors
 	 */
	public SMARTSSearchTask(CyNetwork network, List<? extends CyIdentifiable> selection, 
	                        Scope scope, boolean showTable, ChemInfoSettings settings) {
		super(settings);
		this.objectList = selection;
		this.settings = settings;
		this.compoundCount = 0;
		this.network = network;
		this.scope = scope;
		this.showTable = showTable;
	}

	public SMARTSSearchTask(CyNetwork network, List<? extends CyIdentifiable> selection, 
	                        Scope scope, ChemInfoSettings settings, List<String> columnList) {
		super(settings);
		this.objectList = selection;
		this.settings = settings;
		this.compoundCount = 0;
		this.columnList = columnList;
		this.network = network;
		this.scope = scope;
	}

	public String getTitle() {
		return "Searching Compounds";
	}

	/**
 	 * Runs the task -- this will get all of the compounds, fetching the images (if necessary) and creates the table.
 	 */
	public void run(TaskMonitor taskMonitor) {
		String type = "node";
		if (scope == Scope.ALLEDGES || scope == Scope.SELECTEDEDGES)
			type = "edge";

		List<Compound> cList = getCompounds(objectList, network,
  	                                    settings.getCompoundAttributes(type,AttriType.smiles),
  	                                    settings.getCompoundAttributes(type,AttriType.inchi),
		                                    settings.getMaxThreads());


		if (cList.size() > 0 && !canceled) {
			List<Compound> matches = new ArrayList<Compound>();
			try {
				SMARTSQueryTool queryTool = new SMARTSQueryTool(searchString);
				for (Compound compound: cList) {
					boolean status = queryTool.matches(compound.getMolecule());
					if (status && queryTool.countMatches() > 0)
						matches.add(compound);
				}
			} catch (Exception cdkException) {
				throw new RuntimeException("CDK Exception: "+cdkException.getMessage(), cdkException);
			}
			if (matches == null || matches.size() == 0)
				return;

			if (showTable)
				compoundTable = new CompoundTable(network, matches, columnList, settings);
		}
	}

	public void closePopup() {
		if (compoundTable != null) {
			compoundTable.dispose();
			compoundTable = null;
		}
	}

}
