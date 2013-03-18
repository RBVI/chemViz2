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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.cytoscape.model.CyIdentifiable;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.group.CyGroup;
import org.cytoscape.group.CyGroupFactory;
import org.cytoscape.group.CyGroupManager;
import org.cytoscape.work.TaskMonitor;

import edu.ucsf.rbvi.chemViz2.internal.model.Compound;
import edu.ucsf.rbvi.chemViz2.internal.model.Compound.AttriType;
import edu.ucsf.rbvi.chemViz2.internal.model.ChemInfoSettings;
import edu.ucsf.rbvi.chemViz2.internal.model.TableUtils;
import edu.ucsf.rbvi.chemViz2.internal.ui.CompoundPopup;

import org.openscience.cdk.Molecule;
import org.openscience.cdk.aromaticity.CDKHueckelAromaticityDetector;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IMolecule;
import org.openscience.cdk.isomorphism.UniversalIsomorphismTester;
import org.openscience.cdk.smiles.SmilesGenerator;


/**
 * The CreateCompoundsTask fetches all of the compounds defined by the
 * object passed in its constructor and provides some methods to allow
 * the caller to fetch the compounds when the task is complete.
 */
public class CalculateMCSSTask extends AbstractCompoundTask {
	ChemInfoSettings settings;
	CyGroupManager groupManager;
	CyGroupFactory groupFactory;
	List<? extends CyIdentifiable> objectList;
	CyNetwork network;
	String type;
	List<Compound> compoundList;
	IMolecule mcss = null;
	boolean showResult = false;
	boolean createGroup = false;
	boolean calculationComplete = false;
	static private Logger logger = LoggerFactory.getLogger(CalculateMCSSTask.class);

	/**
 	 * Creates the task.
 	 *
 	 */
  public CalculateMCSSTask(CyNetwork network, List<? extends CyIdentifiable> gObjList, 
	                         ChemInfoSettings settings, CyGroupManager groupManager, CyGroupFactory groupFactory,
	                         boolean showResult, boolean createGroup) {
		super(settings);
		this.objectList = gObjList;
		
		if (gObjList.get(0) instanceof CyNode)
			type = "node";
		else
			type = "edge";
		this.settings = settings;
		this.showResult = showResult;
		this.createGroup = createGroup;
		this.groupManager = groupManager;
		this.groupFactory = groupFactory;
		this.network = network;
	}

	public String getMCSSSmiles() {
		SmilesGenerator g = new SmilesGenerator();
		g.setUseAromaticityFlag(true);
		return g.createSMILES(mcss);
	}

	/**
 	 * Runs the task -- this will get all of the compounds, fetching the images (if necessary) and creates the popup.
 	 */
	public void run(TaskMonitor taskMonitor) {
		int maxThreads = settings.getMaxThreads();
		compoundList = getCompounds(objectList, network,
                                settings.getCompoundAttributes(type,AttriType.smiles),
                                settings.getCompoundAttributes(type,AttriType.inchi), maxThreads);

		int nThreads = Runtime.getRuntime().availableProcessors()-1;
		if (maxThreads > 0) nThreads = maxThreads;

		List<IAtomContainer> mcssList = Collections.synchronizedList(new ArrayList<IAtomContainer>(compoundList.size()));
		for (Compound c: compoundList) {
			mcssList.add(c.getMolecule());
		}

		int pass = 0;
		while (mcssList.size() > 1) {
			mcssList = calculateMCSS(mcssList, nThreads);
			pass++;
		}
		mcss = (IMolecule)mcssList.get(0);

		calculationComplete = true;	
		if (showResult) {
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

	private List<IAtomContainer> calculateMCSS(List<IAtomContainer>mcssList, int nThreads) {
		List<IAtomContainer> newMCSSList = Collections.synchronizedList(new ArrayList<IAtomContainer>(nThreads));
		List<GetMCSSTask> taskList = new ArrayList<GetMCSSTask>();

		if (nThreads == 1) {
			GetMCSSTask task = new GetMCSSTask(mcssList, newMCSSList);
			task.call();
		} else {
			int step = (int)Math.ceil((double)mcssList.size()/(double)nThreads);
			if (step < 2) step = 2; // Can't have a step size of less than 2
			for (int i = 0; i < mcssList.size(); i=i+step) {
				int endPoint = i+step;
				if (endPoint > mcssList.size())
					endPoint = mcssList.size();
				taskList.add(new GetMCSSTask(mcssList.subList(i, endPoint), newMCSSList));
			}

			ExecutorService threadPool = Executors.newFixedThreadPool(nThreads);
			try {
				threadPool.invokeAll(taskList);
			} catch (Exception e) {
				logger.warn("Execution exception: "+e);
			}
		}
		return newMCSSList;
	}

	private class GetMCSSTask implements Callable <IAtomContainer> {
		List<IAtomContainer> mcssList;
		List<IAtomContainer> resultsList;
		IAtomContainer mcss = null;

		public GetMCSSTask(List<IAtomContainer>mcssList, List<IAtomContainer>resultsList) {
			this.mcssList = mcssList;
			this.resultsList = resultsList;
		}

		public IAtomContainer call() {
			mcss = mcssList.get(0);
			try {
				for (int index = 1; index < mcssList.size(); index++) {
					List<IAtomContainer> overlap = UniversalIsomorphismTester.getOverlaps(mcss, mcssList.get(index));
					mcss = maximumStructure(overlap);
					if (mcss == null) break;
				}
			} catch (CDKException e) {
				System.out.println("CDKException: "+e);
			}
			resultsList.add(mcss);
			return mcss;
		}
	
		private IMolecule maximumStructure(List<IAtomContainer> mcsslist) {
			int maxmcss = -99999999;
			IAtomContainer maxac = null;
			if (mcsslist == null || mcsslist.size() == 0) return null;
			for (IAtomContainer a: mcsslist) {
				if (a.getAtomCount() > maxmcss) {
					maxmcss = a.getAtomCount();
					maxac = a;
				}
			}
			return new Molecule(maxac);
		}

		public IAtomContainer get() { 
			if (mcss == null) 
				return call();
			else
				return mcss; 
		}
	}
}
