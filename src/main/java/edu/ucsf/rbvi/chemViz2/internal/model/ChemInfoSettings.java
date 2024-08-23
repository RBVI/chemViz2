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

package edu.ucsf.rbvi.chemViz2.internal.model;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.events.SetCurrentNetworkEvent;
import org.cytoscape.application.events.SetCurrentNetworkListener;
import org.cytoscape.command.CommandExecutorTaskFactory;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyIdentifiable;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.CyTableUtil;
import org.cytoscape.model.events.ColumnCreatedEvent;
import org.cytoscape.model.events.ColumnCreatedListener;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.work.SynchronousTaskManager;
import org.cytoscape.work.TaskObserver;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.TaskManager;
import org.cytoscape.work.util.ListMultipleSelection;
import org.cytoscape.work.util.ListSingleSelection;

import edu.ucsf.rbvi.chemViz2.internal.model.CompoundManager;
import edu.ucsf.rbvi.chemViz2.internal.model.Compound.AttriType;
import edu.ucsf.rbvi.chemViz2.internal.model.DescriptorManager;
import edu.ucsf.rbvi.chemViz2.internal.tasks.PaintNodeStructuresTaskFactory;
import edu.ucsf.rbvi.chemViz2.internal.ui.ChemVizResultsPanel;
import edu.ucsf.rbvi.chemViz2.internal.ui.CompoundTable;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class ChemInfoSettings implements SetCurrentNetworkListener, ColumnCreatedListener {
	private static final String defaultSmilesAttributes[] = {"SMILES","Compounds","Compound","Smiles","smiles"};
	private static final String defaultInCHIAttributes[] = {"InCHI","inchi","InChi","InChI"};
	private static final Fingerprinter fingerprintList[] = {Fingerprinter.PUBCHEM, Fingerprinter.MACCS,
	                                                        Fingerprinter.CDK, Fingerprinter.ECFP4,
																													Fingerprinter.ECFP6,
																													Fingerprinter.ESTATE, Fingerprinter.EXTENDED, 
																													Fingerprinter.FCFP4, Fingerprinter.FCFP6,
	                                                        Fingerprinter.GRAPHONLY, Fingerprinter.HYBRIDIZATION, 
	                                                        Fingerprinter.KLEKOTAROTH, Fingerprinter.SUBSTRUCTURE};
	private CyApplicationManager manager = null;
	private CompoundManager compoundManager = null;
	private DescriptorManager descriptorManager = null;
	private CyServiceRegistrar serviceRegistrar = null;
	private final TaskManager<?,?> dialogTaskManager;
  private final SynchronousTaskManager<?> synchronousTaskManager;
  private final CommandExecutorTaskFactory commandExecutorTaskFactory;


	private boolean haveGUI = true;
	private CyNetwork network = null;
	private List<String> possibleAttributes = null;

	// Save state
	private ChemVizResultsPanel resultsPanel = null;
	private PaintNodeStructuresTaskFactory paintTaskFactory;
	private Map<CyNetwork, Boolean> structuresShown;
	private Map<CyNetwork, CompoundTable> tableMap;

	@Tunable(description="Maximum number of compounds to show in 2D structure popup", groups=" ")
	public int maxCompounds = 0;

	@Tunable(description="Minimum tanimoto value to consider for edge creation", groups=" ")
	public double tcCutoff = 0.50;

	@Tunable(description="Auto show results panel", groups="Depiction options")
	public boolean autoShow = true;

	@Tunable(description="Fingerprint algorithm to use", groups=" ")
	public ListSingleSelection<Fingerprinter> fingerprinter = 
		getListSingleSelection(Arrays.asList(fingerprintList), Fingerprinter.PUBCHEM);

	@Tunable(description="Maximum number of threads to use", groups=" ")
	public int maxThreads = 0;

	@Tunable(groups={"Attribute Settings", "SMILES Attributes"})
	public ListMultipleSelection<String> smilesAttributes = null;

	@Tunable(description="Attributes that contain InCHI strings", groups={"Attribute Settings", "InCHI Attributes"})
	public ListMultipleSelection<String> inChiAttributes =  null;

	@Tunable(description="Size of 2D node depiction as a % of node size", groups="Depiction options")
	public int nodeStructureSize = 100; 

	@Tunable(description="Attribute to use for image labels", groups="Depiction options")
	public ListSingleSelection<String> labelAttribute = null;

	public ChemInfoSettings(CyApplicationManager manager, CyServiceRegistrar registrar,
	                        CompoundManager cmpndManager, DescriptorManager descManager) {
		this.manager = manager;
		this.serviceRegistrar = registrar;
		this.network = manager.getCurrentNetwork();
		this.compoundManager = cmpndManager;
		this.descriptorManager = descManager;
		

		dialogTaskManager = registrar.getService(TaskManager.class);
    synchronousTaskManager = registrar.getService(SynchronousTaskManager.class);
    commandExecutorTaskFactory = registrar.getService(CommandExecutorTaskFactory.class);

		tableMap = new HashMap<>();
		structuresShown = new HashMap<>();
		updateAttributes(network);
	}

	public int getMaxCompounds() { return maxCompounds; }

	public double getTcCutoff() { return tcCutoff; }

	public int getMaxThreads() { return maxThreads; }

	public int getNodeStructureSize() { return nodeStructureSize; }

	public Fingerprinter getFingerprinter() { return fingerprinter.getSelectedValue(); }

	public String getLabelAttribute() { return labelAttribute.getSelectedValue(); }

	public CompoundManager getCompoundManager() { return compoundManager; }
	public DescriptorManager getDescriptorManager() { return descriptorManager; }
	public CyServiceRegistrar getServiceRegistrar() { return serviceRegistrar; }
	public CyNetwork getCurrentNetwork() { 
		network = manager.getCurrentNetwork();
		return network;
	}
	public CyNetworkView getCurrentNetworkView() { 
		return manager.getCurrentNetworkView();
	}
		

	public void execute(TaskIterator iterator) {
    execute(iterator, false);
  }

  public void execute(TaskIterator iterator, TaskObserver observer) {
    execute(iterator, observer, false);
  }

  public void execute(TaskIterator iterator, boolean synchronous) {
    if (synchronous) {
      synchronousTaskManager.execute(iterator);
    } else {
      dialogTaskManager.execute(iterator);
    }
  }

  public void execute(TaskIterator iterator, TaskObserver observer, boolean synchronous) {
    if (synchronous) {
      synchronousTaskManager.execute(iterator, observer);
    } else {
      dialogTaskManager.execute(iterator, observer);
    }
  }

  public void executeCommand(String namespace, String command,
                             Map<String, Object> args, TaskObserver observer) {
    TaskIterator ti = commandExecutorTaskFactory.createTaskIterator(namespace, command, args, observer);
    execute(ti, true);
  }


	public boolean hasNodeCompounds(Collection<CyNode> nodeSet) {
		if (network == null) return false;
		CyTable nodeTable = network.getDefaultNodeTable();
		List<String> attrsFound = getMatchingAttributes(nodeTable, getNodeCompoundAttributes());
		Collection idSet = nodeSet;
		return hasCompounds(idSet, nodeTable, attrsFound);
	}

	public boolean hasEdgeCompounds(Collection<CyEdge> edgeSet) {
		if (network == null) return false;
		CyTable edgeTable = network.getDefaultEdgeTable();
		List<String> attrsFound = getMatchingAttributes(edgeTable, getEdgeCompoundAttributes());
		Collection idSet = edgeSet;
		return hasCompounds(idSet, edgeTable, attrsFound);
	}

	private boolean hasCompounds(Collection<CyIdentifiable> objs, CyTable table, List<String> columns) {
		if (columns == null || columns.size() == 0) return false;
		if (objs == null) return true;

		for (CyIdentifiable obj: objs) {
			if (table.rowExists(obj.getSUID())) {
				CyRow row = table.getRow(obj.getSUID());
				for (String column: columns) {
					if (row.getRaw(column) != null) {
						return true;
					}
				}
			}
		}
		return false;
	}

	public List<String> getCompoundAttributes(String objType, AttriType type) {
		List<String> attributes = new ArrayList<String>();
		List<String> values;
		if (type == AttriType.smiles)
			values = smilesAttributes.getSelectedValues();
		else
			values = inChiAttributes.getSelectedValues();

		if (objType == null)
			return values;

		for (String s: values) {
			if (s.startsWith(objType))
				attributes.add(s.substring(5));
		}

		return attributes;
	}

	// At this point, we should only react to columns changed
	// in our current network.  If a new column is created
	// in a different network, we'll pick that up in the SetCurrentNetworkListener
	@Override
	public void handleEvent(ColumnCreatedEvent e) {
		if (this.network == null) return;
		if (e.getSource().equals(this.network.getDefaultEdgeTable()) ||
		    e.getSource().equals(this.network.getDefaultNodeTable())) {
			possibleAttributes = null;
			updateAttributes(this.network);
		}
	}

	@Override
	public void handleEvent(SetCurrentNetworkEvent e) {
		this.network = e.getNetwork();
		if (this.network == null) return;
		possibleAttributes = null;
		updateAttributes(network);
		updateSettings(getSettings(this.network));
	}

	public ChemVizResultsPanel getResultsPanel() {
		return resultsPanel;
	}

	public void setResultsPanel(ChemVizResultsPanel panel) {
		resultsPanel = panel;
	}

	public void setStructuresShown(CyNetwork network, boolean shown) { structuresShown.put(network, shown); }
	public boolean getStructuresShown(CyNetwork network) { 
		if (structuresShown.containsKey(network))
			return structuresShown.get(network);
		return false;
	}
	public void removeStructuresShown(CyNetwork network) { structuresShown.remove(network); }

	public PaintNodeStructuresTaskFactory getPaintNodeStructuresTaskFactory() { return paintTaskFactory; }
	public void setPaintNodeStructuresTaskFactory(PaintNodeStructuresTaskFactory factory) { 
		paintTaskFactory = factory;
	}

	public boolean getAutoShow() { return autoShow; }
	public void setAutoShow(boolean autoShow) { this.autoShow = autoShow; }

	public void addCompoundTable(CyNetwork network, CompoundTable table) { tableMap.put(network, table); }
	public CompoundTable getCompoundTable(CyNetwork network) { return tableMap.get(network); }
	public void removeCompoundTable(CyNetwork network) { tableMap.remove(network); }

	private void updateAttributes(CyNetwork network) {
		if (network == null) return;
		possibleAttributes = getPossibleAttributes(network);
		if (possibleAttributes == null) return;
		smilesAttributes = 
			getListMultipleSelection(possibleAttributes, Arrays.asList(defaultSmilesAttributes));
		inChiAttributes = 
			getListMultipleSelection(possibleAttributes, Arrays.asList(defaultInCHIAttributes));
		labelAttribute = 
			getListSingleSelection(possibleAttributes, CyNetwork.NAME);
	}

	// Returns a list of all possible attribute values for this network
	public List<String> getPossibleAttributes(CyNetwork network) {
		if (network == null) 
			return null;

		if (possibleAttributes == null) {
			List<String> nodeAttributeNames = new ArrayList<String>();
			List<String> edgeAttributeNames = new ArrayList<String>();
			nodeAttributeNames.add(CyNetwork.NAME);
			getAttributes(nodeAttributeNames, network.getDefaultNodeTable(), "node.");
			getAttributes(edgeAttributeNames, network.getDefaultEdgeTable(), "edge.");
			nodeAttributeNames.addAll(edgeAttributeNames);
			possibleAttributes = nodeAttributeNames;
		}

		return possibleAttributes;
	}

	public boolean haveGUI() { return haveGUI; }
	public void setHaveGUI(boolean hgui) { haveGUI = hgui; }

	private void getAttributes(List<String> names, CyTable table, String prefix) {
		Set<String> list = CyTableUtil.getColumnNames(table);
		for (String s: list) {
			if (s == CyNetwork.NAME)
				continue;
			Class type = table.getColumn(s).getType();
			if (type.equals(String.class) || type.equals(List.class)) {
				names.add(prefix+s);
			}
		}
		Collections.sort(names);
	}

	private List<String> getMatchingAttributes(CyTable table, List<String> compoundAttributes) {
		Set<String> columnNames = CyTableUtil.getColumnNames(table);

		List<String> columnsFound = new ArrayList<String>();
		for (String attribute: compoundAttributes) {
			if (attribute.startsWith("node.") || attribute.startsWith("edge."))
				attribute = attribute.substring(5);
			if (columnNames.contains(attribute))
				columnsFound.add(attribute);
		}

		return columnsFound;
	}

	private List<String> getEdgeCompoundAttributes() {
		List<String> attrList = new ArrayList<String>();
		attrList.addAll(getCompoundAttributes("edge", AttriType.smiles));
		attrList.addAll(getCompoundAttributes("edge", AttriType.inchi));
		return attrList;
	}

	private List<String> getNodeCompoundAttributes() {
		List<String> attrList = new ArrayList<String>();
		attrList.addAll(getCompoundAttributes("node", AttriType.smiles));
		attrList.addAll(getCompoundAttributes("node", AttriType.inchi));
		return attrList;
	}

	// The following routines just add default set values to the standard Cytoscape list objects
	private <T> ListSingleSelection<T> getListSingleSelection(List<T> list, T defaultValue) {
		// Adjusting the defaults?
		if (list == null) {
			return new ListSingleSelection<T>(defaultValue);
		}

		ListSingleSelection s = new ListSingleSelection(list);
		if (defaultValue != null && list.contains(defaultValue))
			s.setSelectedValue(defaultValue);
		return s;
	}

	private ListMultipleSelection<String> getListMultipleSelection(List<String> list, List<String> defaults) {
		ListMultipleSelection s = new ListMultipleSelection(list);
		if (defaults == null || defaults.size() == 0)
			return s;

		List<String> selectedValues = new ArrayList<String>();
		for (String str: list) {
			if (defaults.contains(str))
				selectedValues.add(str);
			else if ((str.startsWith("node.") || str.startsWith("edge.")) && defaults.contains(str.substring(5)))
				selectedValues.add(str);
		}
		if (selectedValues.size() > 0)
			s.setSelectedValues(selectedValues);

		return s;
	}

	private String getAttributeList(ListMultipleSelection<String> values) {
		String result = null;
		for (String s: values.getSelectedValues()) {
			if (result == null) 
				result = s;
			else
				result += ","+s;
		}
		return result;
	}

	private void setAttributeList(String[] attributes, ListMultipleSelection<String> attrs) {
		List<String> attrList = Arrays.asList(attributes);
		if (attrList != null && attrList.size() > 0)
			attrs.setSelectedValues(attrList);
	}

	private void updateSettings(String settings) {
		if (settings == null) return;
		String[] values = settings.split(";");
		for (String setting: values) {
			String[] nv = setting.split("=");
			if (nv[0].equals("maxCompounds")) {
				maxCompounds = Integer.parseInt(nv[1]); 
			} else if (nv[0].equals("maxThreads")) {
				maxThreads = Integer.parseInt(nv[1]); 
			} else if (nv[0].equals("nodeStructureSize")) {
				nodeStructureSize = Integer.parseInt(nv[1]); 
			} else if (nv[0].equals("tcCutoff")) {
				tcCutoff = Double.parseDouble(nv[1]); 
			} else if (nv[0].equals("fingerprinter")) {
				String fp = nv[1];
				for (Fingerprinter f: fingerprintList) {
					if (f.toString().equals(fp)) {
						fingerprinter.setSelectedValue(f);
					}
				}
			} else if (nv[0].equals("labelAttribute")) {
				labelAttribute.setSelectedValue(nv[1]);
			} else if (nv[0].equals("smilesAttributes")) {
				setAttributeList(nv[1].split(","),smilesAttributes);
			} else if (nv[0].equals("inChiAttributes")) {
				setAttributeList(nv[1].split(","),inChiAttributes);
			} else if (nv[0].equals("autoShow")) {
				autoShow = Boolean.parseBoolean(nv[1]);
			}
		}
	}

	public String getSettingsString() {
		String settings = "maxCompounds="+maxCompounds+";maxThreads="+maxThreads+";nodeStructureSize="+nodeStructureSize;
		settings += ";tcCutoff="+tcCutoff+";fingerprinter="+fingerprinter.getSelectedValue().toString();
		settings += ";labelAttribute="+labelAttribute.getSelectedValue();
		settings += ";smilesAttributes="+getAttributeList(smilesAttributes);
		settings += ";inChiAttributes="+getAttributeList(inChiAttributes);
		settings += ";autoShow="+autoShow;
		return settings;
	}

	private static String SETTINGS_COLUMN = "__ChemVizSettings";

	public void saveSettings() {
		CyNetwork currentNetwork = manager.getCurrentNetwork();
		CyTable netTable = currentNetwork.getTable(CyNetwork.class, CyNetwork.HIDDEN_ATTRS);
		if (!TableUtils.columnExists(netTable, SETTINGS_COLUMN)) {
			netTable.createColumn(SETTINGS_COLUMN, String.class, false);
		}
		netTable.getRow(currentNetwork.getSUID()).set(SETTINGS_COLUMN, getSettingsString());
	}

	public String getSettings(CyNetwork network) {
		if (network == null) 
			return null;
		CyTable netTable = network.getTable(CyNetwork.class, CyNetwork.HIDDEN_ATTRS);
		if (!TableUtils.columnExists(netTable, SETTINGS_COLUMN)) {
			return null;
		}
		return netTable.getRow(network.getSUID()).get(SETTINGS_COLUMN, String.class);
	}

}
