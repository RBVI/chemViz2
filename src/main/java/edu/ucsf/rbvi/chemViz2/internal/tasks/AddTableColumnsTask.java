package edu.ucsf.rbvi.chemViz2.internal.tasks;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.ContainsTunables;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.util.ListMultipleSelection;


import edu.ucsf.rbvi.chemViz2.internal.model.ChemInfoSettings;
import edu.ucsf.rbvi.chemViz2.internal.model.Descriptor;
import edu.ucsf.rbvi.chemViz2.internal.model.DescriptorManager;
import edu.ucsf.rbvi.chemViz2.internal.model.TableUtils;
import edu.ucsf.rbvi.chemViz2.internal.ui.TableAttributeHandler;

public class AddTableColumnsTask extends AbstractTask implements ObservableTask {

	@Tunable(description="Columns to add", tooltip="Attribute columns to add to the table")
	public ListMultipleSelection<String> columns;

	@Tunable(description="Descriptors to add", tooltip="Descriptor columns to add to the table")
	public ListMultipleSelection<Descriptor> descriptors;

	private ChemInfoSettings settings;
	private DescriptorManager descriptorManager;

	public AddTableColumnsTask(ChemInfoSettings settings) {
		this.settings = settings;
		this.descriptorManager = settings.getDescriptorManager();
		List<String> columnNames = TableUtils.getColumnNames(settings.getCurrentNetwork(), CyNode.class);
		columnNames.addAll(TableUtils.getColumnNames(settings.getCurrentNetwork(), CyEdge.class));
		columns = new ListMultipleSelection<String>(columnNames);
		descriptors = new ListMultipleSelection<Descriptor>(descriptorManager.getDescriptorList(true));
	}

	public void run(final TaskMonitor taskMonitor) throws Exception {
		List<String> cols = columns.getSelectedValues();
		CyNetwork network = settings.getCurrentNetwork();
		if (cols != null && cols.size() > 0) {
			TableAttributeHandler.addColumnAttributes(network, descriptorManager, cols);
		}

		List<Descriptor> descs = descriptors.getSelectedValues();
		if (descs != null && descs.size() > 0) {
			TableAttributeHandler.addColumnDescriptors(network, descriptorManager, descs);
		}


	}

	@Override
	public <R> R getResults(Class <? extends R> type) {
		String settingsString = settings.getSettingsString();
		if (type.equals(String.class)) {
			return (R)settingsString;
		} else if (type.equals(Map.class)) {
			Map<String,String> sMap = new HashMap<>();
			String[] sArray = settingsString.split(";");
			for (String s: sArray) {
				String[] nv = s.split("=");
				sMap.put(nv[0],nv[1]);
			}
			return (R)sMap;
		}
		return null;
	}

}
