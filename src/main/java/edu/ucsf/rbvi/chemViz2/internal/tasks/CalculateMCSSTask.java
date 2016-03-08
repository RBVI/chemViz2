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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.cytoscape.command.util.EdgeList;
import org.cytoscape.command.util.NodeList;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyIdentifiable;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.group.CyGroup;
import org.cytoscape.group.CyGroupFactory;
import org.cytoscape.group.CyGroupManager;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;

import edu.ucsf.rbvi.chemViz2.internal.model.Compound;
import edu.ucsf.rbvi.chemViz2.internal.model.Compound.AttriType;
import edu.ucsf.rbvi.chemViz2.internal.model.ChemInfoSettings;
import edu.ucsf.rbvi.chemViz2.internal.model.TableUtils;
import edu.ucsf.rbvi.chemViz2.internal.smsd.mcss.JobType;
import edu.ucsf.rbvi.chemViz2.internal.smsd.mcss.MCSS;
import edu.ucsf.rbvi.chemViz2.internal.smsd.mcss.TaskUpdater;
import edu.ucsf.rbvi.chemViz2.internal.ui.CompoundPopup;

import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.smiles.SmilesGenerator;


/**
 * The CreateCompoundsTask fetches all of the compounds defined by the
 * object passed in its constructor and provides some methods to allow
 * the caller to fetch the compounds when the task is complete.
 */
public class CalculateMCSSTask extends AbstractCompoundTask implements TaskUpdater, ObservableTask {
	ChemInfoSettings settings;
	CyGroupManager groupManager;
	CyGroupFactory groupFactory;
	List<? extends CyIdentifiable> objectList;
	CyNetwork argNetwork;
	List<Compound> compoundList;
	IAtomContainer mcss = null;
	boolean calculationComplete = false;
	boolean haveGUI = true;
	static private Logger logger = LoggerFactory.getLogger(CalculateMCSSTask.class);

	@Tunable(description="Network to operate on", context="nogui")
	public CyNetwork network;

	NodeList nodeList = new NodeList(null);
	@Tunable(description="The list of nodes to calculate the MCSS for", context="nogui")
	public NodeList getnodeList() {
		if (network == null)
			network = settings.getCurrentNetwork();
		nodeList.setNetwork(network);
		return nodeList;
	}
	public void setnodeList(NodeList list) {};

	EdgeList edgeList = new EdgeList(null);
	@Tunable(description="The list of edges to calculate the MCSS for", context="nogui")
	public EdgeList getedgeList() {
		if (network == null)
			network = settings.getCurrentNetwork();
		edgeList.setNetwork(network);
		return edgeList;
	}
	public void setedgeList(EdgeList list) {};

	@Tunable(description="Show results in a popup window", context="nogui")
	public boolean showResult = false;

	@Tunable(description="Create a group of selected nodes", context="nogui")
	public boolean createGroup = false;

	/**
 	 * Creates the task.
 	 *
 	 */
  public CalculateMCSSTask(CyNetwork network, List<? extends CyIdentifiable> gObjList, 
	                         ChemInfoSettings settings, CyGroupManager groupManager, CyGroupFactory groupFactory,
	                         boolean showResult, boolean createGroup) {
		super(settings);
		this.objectList = gObjList;

		this.settings = settings;
		this.showResult = showResult;
		this.createGroup = createGroup;
		this.groupManager = groupManager;
		this.groupFactory = groupFactory;
		this.argNetwork = network;
	}

	// Used primarily for command version
  public CalculateMCSSTask(CyNetwork network, ChemInfoSettings settings, 
	                         CyGroupManager groupManager, CyGroupFactory groupFactory, boolean haveGUI) {
		super(settings);
		this.objectList = null;

		this.settings = settings;
		this.groupManager = groupManager;
		this.groupFactory = groupFactory;
		this.argNetwork = network;
		this.haveGUI = haveGUI;
	}

	public String getMCSSSmiles() {
		SmilesGenerator g = new SmilesGenerator().aromatic();
		try {
			return g.create(mcss);
		} catch (CDKException c) {
			return null;
		}
	}

	/**
 	 * Runs the task -- this will get all of the compounds, fetching the images (if necessary) and creates the popup.
 	 */
	public void run(TaskMonitor taskMonitor) {
		this.monitor = taskMonitor;
		if (monitor != null) monitor.setTitle("Calculating MCSS");

		if (network == null && argNetwork == null)
			network = settings.getCurrentNetwork();
		else if (network == null)
			network = argNetwork;


		int maxThreads = settings.getMaxThreads();
		setStatus("Getting compounds");
		if (objectList == null)
			objectList = getObjectList(network, null, null,
			                           nodeList.getValue(), edgeList.getValue());

		if (objectList == null || objectList.size() == 0) {
			monitor.showMessage(TaskMonitor.Level.ERROR, "Nothing selected");
			return;
		}
		String type = "node";
		if (objectList.get(0) instanceof CyEdge)
			type = "edge";

		compoundList = getCompounds(objectList, network,
                                settings.getCompoundAttributes(type,AttriType.smiles),
                                settings.getCompoundAttributes(type,AttriType.inchi), maxThreads);

		int nThreads = Runtime.getRuntime().availableProcessors()-1;
		if (maxThreads > 0) nThreads = maxThreads;

		List<IAtomContainer> targetList = Collections.synchronizedList(new ArrayList<IAtomContainer>(compoundList.size()));
		for (Compound c: compoundList) {
			if (c.getCompoundType().equals(Compound.CompoundType.REACTION)) {
				monitor.showMessage(TaskMonitor.Level.WARN, "Can't do MCSS on reactions.  Skipping "+c.toString());
				continue;
			}
			if (c.getMolecule() != null)
				targetList.add(c.getMolecule());
		}

		if (targetList.size() > 1) {
			MCSS mcssJob = new MCSS(targetList, JobType.SINGLE, (TaskUpdater)this, nThreads);
			Collection<IAtomContainer> calculatedMCSS = mcssJob.getCalculateMCSS();
			if (calculatedMCSS != null && calculatedMCSS.size() == 1)
				mcss = calculatedMCSS.iterator().next();
		} else {
			mcss = targetList.get(0);
		}

		if (mcss == null) {
			monitor.showMessage(TaskMonitor.Level.ERROR, "No MCSS returned");
			return;
		}

		calculationComplete = true;	
		if (haveGUI && showResult) {
			String mcssSmiles = getMCSSSmiles();
			String label = mcssSmiles;
			Compound c = new Compound(settings, null, null, null, mcssSmiles, mcss, AttriType.smiles);
			CompoundPopup popup = new CompoundPopup(network, Collections.singletonList(c), null, label, "Maximum Common SubStructure");
		}

		if (createGroup) {
			List<CyNode> nodeList = new ArrayList<CyNode>();
			HashSet<CyGroup> groupSet = null;
			String groupName = "";
			boolean newGroup = false;

			// Only create a new group if the nodes aren't already in a group
			for (CyIdentifiable obj: objectList) {
				if (!(obj instanceof CyNode)) {
					// System.out.println("obj "+obj.toString()+" is not a node");
					return;
				}

				CyNode node = (CyNode) obj;
				nodeList.add(node);
				groupName += ","+TableUtils.getName(network, node);

				if (newGroup)
					continue;

				List<CyGroup> myGroups = groupManager.getGroupsForNode(node, network);
				if (myGroups != null && myGroups.size() > 0) {
					if (groupSet == null) {
						groupSet = new HashSet<CyGroup>(myGroups);
					} else {
						groupSet.retainAll(myGroups);
						if (groupSet.size() == 0)
							newGroup = true;
					}
				} else {
					newGroup = true;
				}
			}

			CyGroup group = null;
			String smilesString = getMCSSSmiles();

			if (!newGroup) {
				// All nodes already are part of at least one group
				group = groupSet.iterator().next(); // Get the first group
			} else {
				if (groupName.length() > 16)
					groupName = groupName.substring(0,16)+"...";
				group = groupFactory.createGroup(network,  nodeList, null, true);
				group.collapse(network);
				CyNode node = group.getGroupNode();
				CyRow row = network.getRow(node);
				row.set(CyNetwork.NAME, "MCSS of ["+groupName.substring(1)+"]");
			}

			CyNode node = group.getGroupNode();
			CyRow row = network.getRow(node);
			String attribute = compoundList.get(0).getAttribute(); // get the attribute
			row.set(attribute, getMCSSSmiles());
		}
	}

	public List<Compound>getCompoundList() { return compoundList; }

	public void setTotalCount(int count) {
		totalObjects = count;
		objectCount = 0;
	}

	public <R> R getResults(Class<? extends R> type) {
		// Right now, we only support SMILES strings as a return
		return (R)getMCSSSmiles();
	}

	public synchronized void incrementCount() {
		updateMonitor();
	}

	public synchronized void updateStatus(String status) {
		setStatus(status);
	}

	public synchronized void logException(String className, Level level, String message, Exception exception) {
		if (message == null) message = exception.getMessage();

		if (level == Level.SEVERE) {
			logger.error("Fatal error in "+className+": "+message, exception);
		}

		if (level == Level.WARNING) 
			logger.warn("Error in "+className+": "+message);
		else if (level == Level.INFO)
			logger.info(className+": "+message);
		else 
			logger.debug(className+": "+message);
	}

}
