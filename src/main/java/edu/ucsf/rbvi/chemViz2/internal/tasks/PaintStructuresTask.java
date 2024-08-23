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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cytoscape.command.util.EdgeList;
import org.cytoscape.command.util.NodeList;
import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyIdentifiable;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.VisualLexicon;
import org.cytoscape.view.model.VisualProperty;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualMappingFunctionFactory;
import org.cytoscape.view.vizmap.VisualStyle;
import org.cytoscape.view.vizmap.mappings.PassthroughMapping;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;

import edu.ucsf.rbvi.chemViz2.internal.model.ChemInfoSettings;
import edu.ucsf.rbvi.chemViz2.internal.model.Compound;
import edu.ucsf.rbvi.chemViz2.internal.model.Compound.AttriType;
import edu.ucsf.rbvi.chemViz2.internal.tasks.ChemVizAbstractTaskFactory.Scope;
import edu.ucsf.rbvi.chemViz2.internal.ui.CompoundTable;

/**
 * The CompoundtableTask fetches all of the compounds defined by the
 * object passed in its constructor and then creates a popup Dialog that provides
 * a 2D image of all of the compuonds defined.
 */
public class PaintStructuresTask extends AbstractCompoundTask {
	NodeList nodeList = new NodeList(null);
	@Tunable(description="The list of nodes to show the compound table for", context="nogui")
	public NodeList getnodeList() {
		nodeList.setNetwork(networkView.getModel());
		return nodeList;
	}
	public void setnodeList(NodeList list) {};

	CyIdentifiable context;
	CyNetworkView networkView;
	Scope scope;
	boolean remove = false;
	VisualLexicon lex;
	VisualMappingManager vmm;
	VisualMappingFunctionFactory passthroughFactory;
	String title = null;
	static final String PASSTHROUGH_COLUMN = "chemViz Passthrough";

	/**
 	 * Creates the task.
 	 *
 	 * @param view the view object that we're creating the popup for
 	 * @param dialog the settings dialog, which we use to pull the attribute names that contain the compound descriptors
 	 */
  public PaintStructuresTask(VisualMappingManager vmm, VisualMappingFunctionFactory passthroughFactory,
	                           VisualLexicon lex,
	                           CyNetworkView networkView, CyIdentifiable context, 
	                           Scope scope, ChemInfoSettings settings, boolean remove) {
		super(settings);
		this.scope = scope;
		this.networkView = networkView;
		this.context = context;
		this.vmm = vmm;
		this.passthroughFactory = passthroughFactory;
		this.lex = lex;
		this.remove = remove;
	}

	public void setDialogTitle(String title) {
		this.title = title;
	}

	/**
 	 * Runs the task
 	 */
	public void run(TaskMonitor taskMonitor) {
		CyNetwork network = networkView.getModel();
		List<CyIdentifiable> objectList = getObjectList(network, context, scope, nodeList.getValue(), null);
		String type = "node";

		List<Compound> compoundList = getCompounds(objectList, network,
  	                                           settings.getCompoundAttributes(type,AttriType.smiles),
  	                                           settings.getCompoundAttributes(type,AttriType.inchi),
		                                           settings.getMaxThreads());

		Map<CyNode, List<Compound>> idMap = new HashMap<CyNode, List<Compound>>();
		for (Compound cmpnd: compoundList) {
			CyNode source = (CyNode)cmpnd.getSource();
			if (!idMap.containsKey(source)) {
				idMap.put(source, new ArrayList<Compound>());
			}
			List<Compound> list = idMap.get(source);
			list.add(cmpnd);
		}

		if (!remove) {
			CyTable nodeTable = network.getDefaultNodeTable();
			// Create the column (if we need to)
			CyColumn column = nodeTable.getColumn(PASSTHROUGH_COLUMN);
			if (column == null)
				nodeTable.createColumn(PASSTHROUGH_COLUMN, String.class, false);
			settings.setStructuresShown(network, true);
		} else {
			settings.setStructuresShown(network, false);
		}

		// OK, now add this to our visual style
		VisualStyle style = vmm.getVisualStyle(networkView);
		VisualProperty cg1 = lex.lookup(CyNode.class, "NODE_CUSTOMGRAPHICS_2");
		PassthroughMapping pMapping = 
			(PassthroughMapping) passthroughFactory.createVisualMappingFunction(PASSTHROUGH_COLUMN, String.class, cg1);
		style.addVisualMappingFunction(pMapping);

		// Now, go through and apply it
		if (idMap.size() > 0 && !canceled) {
			for (CyNode object: idMap.keySet()) {
				CyRow row = network.getRow(object);
				List<Compound> objectCompoundList = idMap.get(object);
				if (objectCompoundList == null || objectCompoundList.size() == 0) continue;
				if (!remove)
					row.set(PASSTHROUGH_COLUMN, "chemviz:"+objectCompoundList.get(0).getSMILESString());
				else
					row.set(PASSTHROUGH_COLUMN, null);
				style.apply(row, networkView.getNodeView(object));
			}
		}
	}
}
