package edu.ucsf.rbvi.chemViz2.internal.tasks;

import java.util.Collections;
import java.util.List;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTableUtil;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.task.NetworkTaskFactory;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;

import edu.ucsf.rbvi.chemViz2.internal.model.ChemInfoSettings;

public class HideResultsPanelTaskFactory extends ChemVizAbstractTaskFactory {
	ChemInfoSettings settings = null;

	public HideResultsPanelTaskFactory(ChemInfoSettings settings) {
		this.settings = settings;
	}

	public TaskIterator createTaskIterator() {
		return new TaskIterator(new HideResultsPanelTask(settings));
	}

}
