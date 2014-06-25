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
import java.util.List;

import org.cytoscape.command.util.EdgeList;
import org.cytoscape.command.util.NodeList;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyIdentifiable;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;

import edu.ucsf.rbvi.chemViz2.internal.model.ChemInfoSettings;
import edu.ucsf.rbvi.chemViz2.internal.model.Compound;
import edu.ucsf.rbvi.chemViz2.internal.model.Compound.AttriType;
import edu.ucsf.rbvi.chemViz2.internal.tasks.ChemVizAbstractTaskFactory.Scope;
import edu.ucsf.rbvi.chemViz2.internal.ui.CompoundPopup;

/**
 * The CreatePopupTask fetches all of the compounds defined by the
 * object passed in its constructor and then creates a popup Dialog that provides
 * a 2D image of all of the compuonds defined.
 */
public class CompoundPopupTask extends AbstractCompoundTask {
	@Tunable(description="Network to operate on", context="nogui")
	public CyNetwork network;

	NodeList nodeList = new NodeList(null);
	@Tunable(description="The list of nodes to show the compound popup for", context="nogui")
	public NodeList getnodeList() {
		if (network == null)
			network = settings.getCurrentNetwork();
		nodeList.setNetwork(network);
		return nodeList;
	}
	public void setnodeList(NodeList list) {};

	EdgeList edgeList = new EdgeList(null);
	@Tunable(description="The list of edges to show the compound popup for", context="nogui")
	public EdgeList getedgeList() {
		if (network == null)
			network = settings.getCurrentNetwork();
		edgeList.setNetwork(network);
		return edgeList;
	}
	public void setedgeList(EdgeList list) {};

	CyIdentifiable context;
	CyNetwork argNetwork;
	String label;
	Scope scope;
	CompoundPopup compoundPopup = null;
	String title = null;

	/**
 	 * Creates the task.
 	 *
 	 * @param view the view object that we're creating the popup for
 	 * @param dialog the settings dialog, which we use to pull the attribute names that contain the compound descriptors
 	 */
  public CompoundPopupTask(CyNetwork network, CyIdentifiable context, String label, Scope scope, 
	                         ChemInfoSettings settings) {
		super(settings);
		this.scope = scope;
		this.argNetwork = network;
		this.context = context;
		this.label = label; // Overrides settings.getLabelAttribute()
	}

	public void setDialogTitle(String title) {
		this.title = title;
	}

	/**
 	 * Runs the task -- this will get all of the compounds, fetching the images (if necessary) and creates the popup.
 	 */
	public void run(TaskMonitor taskMonitor) {
		if (network == null && argNetwork == null)
			network = settings.getCurrentNetwork();
		else if (network == null)
			network = argNetwork;

		List<CyIdentifiable> objectList = getObjectList(network, context, scope,
		                                                nodeList.getValue(), edgeList.getValue());
		if (objectList == null || objectList.size() == 0)
			return;

		String type = "node";
		if (objectList.get(0) instanceof CyEdge)
			type = "edge";

		if (label == null && objectList != null && objectList.size() > 0)
			this.label = settings.getLabelAttribute();

		List<Compound> compoundList = getCompounds(objectList, network,
 	                                             settings.getCompoundAttributes(type,AttriType.smiles),
 	                                             settings.getCompoundAttributes(type,AttriType.inchi),
	                                             settings.getMaxThreads());
		if (compoundList.size() > 0 && !canceled) {
			if (objectList != null && objectList.size() == 1) {
				compoundPopup = new CompoundPopup(network, compoundList, objectList, null, title);
			} else {
				if (label.equals("ID"))
					compoundPopup = new CompoundPopup(network, compoundList, objectList, type+".name", title);
				else
					compoundPopup = new CompoundPopup(network, compoundList, objectList, label, title);
			}
		}
	}

	public void closePopup() {
		if (compoundPopup != null) {
			compoundPopup.dispose();
			compoundPopup = null;
		}
	}
}
