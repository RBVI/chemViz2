package edu.ucsf.rbvi.chemViz2.internal.tasks;

import java.util.Collections;
import java.util.List;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.task.NetworkTaskFactory;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;

import edu.ucsf.rbvi.chemViz2.internal.model.ChemInfoSettings;

public class SearchTaskFactory extends ChemVizAbstractTaskFactory 
                               implements NetworkTaskFactory {
	ChemInfoSettings settings = null;
	boolean haveGUI = false;

	public SearchTaskFactory(ChemInfoSettings settings, boolean haveGUI) {
		this.settings = settings;
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
		return new TaskIterator(new SMARTSSearchTask(network, settings, haveGUI));
	}

}
