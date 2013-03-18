package edu.ucsf.rbvi.chemViz2.internal.tasks;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;

import edu.ucsf.rbvi.chemViz2.internal.model.ChemInfoSettings;

public class ChemInfoSettingsTaskFactory extends AbstractTaskFactory {
	ChemInfoSettings settings = null;

	public ChemInfoSettingsTaskFactory(ChemInfoSettings settings) {
		this.settings = settings;
	}

	public TaskIterator createTaskIterator() {
		return new TaskIterator(new ChemInfoSettingsTask(settings));
	}
}
