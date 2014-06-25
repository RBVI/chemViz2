package edu.ucsf.rbvi.chemViz2.internal.tasks;

import java.util.Collections;
import java.util.List;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.group.CyGroupFactory;
import org.cytoscape.group.CyGroupManager;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyTableUtil;
import org.cytoscape.task.NetworkTaskFactory;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;

import edu.ucsf.rbvi.chemViz2.internal.model.ChemInfoSettings;

public class CalculateMCSSTaskFactory extends ChemVizAbstractTaskFactory 
                                          implements NetworkTaskFactory {
	ChemInfoSettings settings = null;
	CyGroupManager groupManager;
	CyGroupFactory groupFactory;
	boolean haveGUI;

	public CalculateMCSSTaskFactory(ChemInfoSettings settings, CyGroupManager groupManager, 
	                                CyGroupFactory groupFactory, boolean haveGUI) {
		this.settings = settings;
		this.groupManager = groupManager;
		this.groupFactory = groupFactory;
		this.haveGUI = haveGUI;
	}

	public TaskIterator createTaskIterator() {
		return null;
	}

	public boolean isReady(CyNetwork network) {
		if (settings.hasEdgeCompounds(network.getEdgeList())||
		    settings.hasNodeCompounds(network.getNodeList()))
			return true;

		return false;
	}

	public TaskIterator createTaskIterator(CyNetwork network) {
		return new TaskIterator(new CalculateMCSSTask(network, settings, groupManager, groupFactory, haveGUI));
	}

}
