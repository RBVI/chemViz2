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
import edu.ucsf.rbvi.chemViz2.internal.ui.ChemVizResultsPanel;

public class ShowResultsPanelTaskFactory extends ChemVizAbstractTaskFactory {
	ChemInfoSettings settings = null;

	public ShowResultsPanelTaskFactory(ChemInfoSettings settings) {
		this.settings = settings;

		// If we're doing autoShow, create the resultsPanel now.  We won't actually show it until
		// we open up a network that has compounds
		if (settings.getAutoShow()) {
			ShowResultsPanelTask task = new ShowResultsPanelTask(settings, true);
			task.run(null);
		}
	}

	public TaskIterator createTaskIterator() {
		return new TaskIterator(new ShowResultsPanelTask(settings, false));
	}

}
