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

  You should have received a copy of the GNU Lesser General Public 
  License along with this library; if not, write to the Free Software 
  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.  
*/ 

package edu.ucsf.rbvi.chemViz2.internal.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Frame;
import java.awt.Image;

import java.awt.BorderLayout;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

import java.text.DecimalFormat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import org.cytoscape.model.CyIdentifiable;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTableUtil;
import org.cytoscape.model.events.RowsSetEvent;
import org.cytoscape.model.events.RowsSetListener;
import org.cytoscape.application.events.SetCurrentNetworkEvent;
import org.cytoscape.application.events.SetCurrentNetworkListener;
import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.util.swing.OpenBrowser;
import org.cytoscape.work.SynchronousTaskManager;
import org.cytoscape.work.TaskIterator;

import edu.ucsf.rbvi.chemViz2.internal.model.Compound;
import edu.ucsf.rbvi.chemViz2.internal.model.ChemInfoSettings;
import edu.ucsf.rbvi.chemViz2.internal.model.CompoundManager;
import edu.ucsf.rbvi.chemViz2.internal.model.Compound.AttriType;
import edu.ucsf.rbvi.chemViz2.internal.model.Descriptor;
import edu.ucsf.rbvi.chemViz2.internal.model.DescriptorManager;
import edu.ucsf.rbvi.chemViz2.internal.model.TableUtils;
import edu.ucsf.rbvi.chemViz2.internal.model.descriptors.HTMLFormulaDescriptor;
import edu.ucsf.rbvi.chemViz2.internal.model.descriptors.SmilesDescriptor;
import edu.ucsf.rbvi.chemViz2.internal.tasks.UpdateCompoundsTask;

public class ChemVizResultsPanel extends JPanel implements CytoPanelComponent, 
                                                           RowsSetListener, 
                                                           SetCurrentNetworkListener,
	                                                         ComponentListener {
	private String panelName;
	private String labelAttribute;
	private CyNetwork network;
	private List<Compound> compoundList;
	private CompoundManager mgr;
	private ChemInfoSettings settings;
	private Map<Component, Compound> imageMap;
	private JScrollPane scrollPane;
	private JPanel outerPanel;
	private JSplitPane splitPane;
	private OpenBrowser openBrowser;

	private static final int LABEL_HEIGHT = 20;
	enum LabelType {ATTRIBUTE, TEXT};

	public ChemVizResultsPanel(ChemInfoSettings settings, String labelAttribute, 
	                           String panelName) {
		this.panelName = panelName;
		this.labelAttribute = labelAttribute;
		this.settings = settings;
		mgr = settings.getCompoundManager();
		network = settings.getCurrentNetwork();
		imageMap = new HashMap<Component, Compound>();
		openBrowser = (OpenBrowser)settings.getServiceRegistrar().getService(OpenBrowser.class);

    setLayout(new BorderLayout());

		outerPanel = new JPanel();
		outerPanel.setSize(300, 400);

    // scrollPane = new JScrollPane(outerPanel);
    add(BorderLayout.CENTER, outerPanel);

		updateSelection();
	}

	@Override
	public Component getComponent() {
		return this;
	}

	@Override
	public CytoPanelName getCytoPanelName() {
		return CytoPanelName.EAST;
	}

	@Override
	public Icon getIcon() {
		return null;
	}

	@Override
	public String getTitle() {
		return panelName;
	}
	

	// @Override 
	public String getIdentifier() {
		return panelName;
	}

	@Override
	public void handleEvent(SetCurrentNetworkEvent e) {
		if (e.getNetwork() != null)
			this.network = e.getNetwork();

		updateSelection();
	}

	@Override
	public void handleEvent(RowsSetEvent e) {
		if (!e.containsColumn(CyNetwork.SELECTED))
			return;

		// It's a selection event and it's in our network
		updateSelection();
	}

	@Override
	public void componentHidden(ComponentEvent e) {}

	@Override
	public void componentMoved(ComponentEvent e) {}

	@Override
	public void componentShown(ComponentEvent e) {}

	@Override
	public void componentResized(ComponentEvent e) {
		if (e.getComponent() instanceof JSplitPane) {
			JSplitPane splitPane = (JSplitPane)e.getComponent();
			return;
		}

		if (!(e.getComponent() instanceof JPanel)) 
			return;

		JPanel panel = (JPanel)e.getComponent();
		Component[] components = panel.getComponents();

		// Get our new width
		int width = panel.getWidth()-10;
		int height = panel.getHeight()-10;
		JLabel labelComponent = null;

		if (compoundList.size() > 1) {
			// If we have two components, component 0 is the image and
			// component 1 is the label
			labelComponent = (JLabel)components[0];
			height = height-LABEL_HEIGHT;
		} else if (compoundList.size() == 1) {
			labelComponent = (JLabel)components[1];
			if (width < height)
				height = width;
			else
				width = height;

			if (width > 310) {
				width = 300;
				height = 300;
			}
		}

		if (imageMap.containsKey(panel)) {
			Image img = imageMap.get(panel).getImage(width,height, Color.WHITE);
			if (img != null) {
				labelComponent.setIcon(new ImageIcon(img));
				labelComponent.setSize(width, height);
			}
		}
	}

	private void updateSelection() {
		List<CyIdentifiable> selectionList = 
			new ArrayList<CyIdentifiable>(CyTableUtil.getNodesInState(network, CyNetwork.SELECTED, true));
		selectionList.addAll(CyTableUtil.getEdgesInState(network, CyNetwork.SELECTED, true));

		// This will make sure all of the compounds for the selected objects are created
		SynchronousTaskManager taskManager = (SynchronousTaskManager)settings.getServiceRegistrar().getService(SynchronousTaskManager.class);
		UpdateCompoundsTask updateTask = new UpdateCompoundsTask(network, selectionList, settings);
		taskManager.execute(new TaskIterator(updateTask));

		compoundList = new ArrayList<Compound>();

		for (CyIdentifiable id: selectionList) {
			List<Compound> c = mgr.getCompounds(network, id);
			if (c != null && c.size() > 0)
				compoundList.addAll(c);
		}

		createPanel();
	}

	private void createPanel() {
		int width = outerPanel.getWidth();
		outerPanel.removeAll();

		int structureCount = compoundList.size();
		if (structureCount == 0) {
			outerPanel.repaint();
			return;
		}

		// If we only have one, provide HTML table
		if (compoundList.size() == 1) {
			addInfoPanel(width, compoundList.get(0));
		} else {
			// If we have multiple, show the structures
			addGrid(width, structureCount);
		}
		this.revalidate();
		this.repaint();
		if (compoundList.size() == 1) {
			splitPane.setDividerLocation(0.5);
			this.revalidate();
		}
	}

	private void addGrid(int width, int structureCount) {

		// How many images do we have?
		int nCols = (int)Math.sqrt((double)structureCount);

		GridLayout layout = new GridLayout(nCols, structureCount/nCols, 1, 1);
		outerPanel.setLayout(layout);

		for (Compound compound: compoundList) {
			// Get the image
			Image img = compound.getImage(width/nCols, width/nCols-LABEL_HEIGHT, Color.WHITE);
			JPanel panel = new JPanel();
			BoxLayout bl = new BoxLayout(panel, BoxLayout.Y_AXIS);
			panel.setLayout(bl);

			JLabel label = new JLabel(new ImageIcon(img));
			panel.add(label);

			// label.setLocation(0, 0);
			if (label != null) {
				String textLabel = getLabel(compound, labelAttribute);
			
				JTextField tf = new JTextField(textLabel.toString());
				tf.setHorizontalAlignment(JTextField.CENTER);
				tf.setEditable(false);
				tf.setBorder(null);
				panel.add(tf);
				tf.setSize(width/nCols, LABEL_HEIGHT);
				// tf.setLocation(width/nCols, width/nCols-LABEL_HEIGHT);
			}
			panel.setBackground(Color.WHITE);
			panel.setOpaque(true);
			panel.setBorder(BorderFactory.createEtchedBorder());
			panel.addComponentListener(this);
			imageMap.put(panel, compound);
			panel.setSize(width/nCols, width/nCols);
			outerPanel.add(panel);
		}
	}

	static String PUBCHEM = "http://pubchem.ncbi.nlm.nih.gov/search/search.cgi?cmd=search&q_type=dt&simp_schtp=fs&q_data=";
	static String CHEMSPIDER = "http://www.chemspider.com/smiles?";
	static String CHEMBL = "http://www.ebi.ac.uk/chembl/compound/inspect/";

	private void addInfoPanel(int width, Compound compound) {
    outerPanel.setLayout(new BorderLayout());
		splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true);
		splitPane.addComponentListener(this);
		splitPane.setResizeWeight(0.5);

		{
			JPanel structurePanel = new JPanel();
			structurePanel.setBackground(Color.WHITE);
			structurePanel.setOpaque(true);
			String textLabel = getLabel(compound, labelAttribute);
			BorderLayout bl2 = new BorderLayout();
			structurePanel.setLayout(bl2);
			JLabel tf = new JLabel(textLabel.toString(), JLabel.CENTER);
			structurePanel.add(tf, BorderLayout.NORTH);

			// Now add our image
			Image img = compound.getImage(width, width, Color.WHITE);
			structurePanel.add(new JLabel(new ImageIcon(img)), BorderLayout.CENTER);
			imageMap.put(structurePanel, compound);
			structurePanel.setMinimumSize(new Dimension(100, 100+LABEL_HEIGHT));
			structurePanel.setSize(width, width+LABEL_HEIGHT+10);
			structurePanel.addComponentListener(this);
			splitPane.setTopComponent(structurePanel);
		}

		// Finally, add the table
		final OpenBrowser ob = openBrowser;
		JEditorPane textArea = new JEditorPane("text/html", null);
		textArea.setEditable(false);
		textArea.addHyperlinkListener(new HyperlinkListener() {
			public void hyperlinkUpdate(HyperlinkEvent e) {
				if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
					ob.openURL(e.getURL().toString());
				}
			}
		});

		String message = "<h2 style=\"margin-left: 5px;margin-bottom: 0px;\">CrossLinks</h2>";
		message += "<table style=\"margin-left: 10px;margin-top: 0px;\"><tr><td><a href=\""+PUBCHEM+compound.getMoleculeString()+"\">PubChem</a></td>";
		message += "<td><a href=\""+CHEMSPIDER+compound.getMoleculeString()+"\">ChemSpider</a> </td>";
		String chemblID = getChEMBLID(compound);
		if (chemblID != null)
			message += "<td><a href=\""+CHEMBL+chemblID+"\">ChEMBL</a> </td></tr></table>";
		else
			message += "</tr></table>";
		
		message += "<h2 style=\"margin-left: 5px;margin-bottom: 0px;\">Chemical Descriptor</h2>";
		message = addDescriptors(message, compound, "htmlformula", "mass", "weight", "roff", "acceptors", "donors", "alogp2", "smiles");
		textArea.setText(message);
		JScrollPane scrollPane = new JScrollPane(textArea);
		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		splitPane.setBottomComponent(scrollPane);
		outerPanel.add(BorderLayout.CENTER, splitPane);
	}

	private String getLabel(Compound compound, String labelAttribute) {
		LabelType labelType = LabelType.ATTRIBUTE;
		String textLabel = labelAttribute;

		// Get the right attributes
		if (labelAttribute != null && labelAttribute.startsWith("node.")) {
			textLabel = labelAttribute.substring(5);
		} else if (labelAttribute != null && labelAttribute.startsWith("edge.")) {
			textLabel = labelAttribute.substring(5);
		} else if (labelAttribute == null) {
			textLabel = CyNetwork.NAME;
		} else if (labelAttribute != CyNetwork.NAME){
			labelType = LabelType.TEXT;
		}

		if (labelType == LabelType.ATTRIBUTE) {
			textLabel = TableUtils.getLabelAttribute(compound.getNetwork(), 
			                                         compound.getSource(), textLabel);
			if (textLabel == null)
				textLabel = TableUtils.getName(compound.getNetwork(), compound.getSource());
		}
		return textLabel;
	}

	private String addDescriptors(String message, Compound compound, String... descriptors) {
		DescriptorManager manager = settings.getDescriptorManager();
		message += "<table style=\"margin-left: 10px;\">";
		for (String descriptor: descriptors) {
			Descriptor d;
			if (descriptor.equals("htmlformula"))
				d = new HTMLFormulaDescriptor();
			else if (descriptor.equals("smiles"))
				d = new SmilesDescriptor();
			else
				d = manager.getDescriptor(descriptor);
			if (d == null) continue;
			message += "<tr><td style=\"font-weight:bold;color:blue;\">"+d.toString()+"</td>";
			if (descriptor.equals("smiles")) {
				message += "<td></td></tr><tr><td colspan=\"2\" style=\"margin-left: 5px;\">"+wrap((String)d.getDescriptor(compound), 30)+"</td></tr>";
			} else {
				message += "<td style=\"text-align:right;\">"+getFormattedDescriptor(d, compound)+"</td></tr>";
			}
		}
		message+="</table>";
		return message;
	}

	private String getFormattedDescriptor(Descriptor d, Compound compound) {
		Class type = d.getClassType();
		Object value = d.getDescriptor(compound);
		if (type.equals(Double.class)) {
			DecimalFormat df = new DecimalFormat("#.##");
			return df.format(value);
		}
		return value.toString();
	}

	private String wrap(String string, int width) {
		if (string.length() <= width)
			return string;
		String chunk = string.substring(0, width);
		return chunk+"\n"+wrap(string.substring(width,string.length()), width);
	}

	private String getChEMBLID(Compound compound) {
		try {
			// Get the SMILES string
			String smiles = compound.getMoleculeString();
			// Construct the query
			URL query = new URL("https://www.ebi.ac.uk/chemblws/compounds/smiles/"+URLEncoder.encode(smiles, "UTF-8"));
			// System.out.println("Query = "+query.toString());

			URLConnection connection = query.openConnection();
			// Get the XML back
			BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
	
			String line;
			StringBuilder xml = new StringBuilder();
			while ((line = in.readLine()) != null) {
				// System.out.println("Line = "+line);
				xml.append(line);
			}
			in.close();

			String response = xml.toString();
			// System.out.println("xml = "+response);

			// Find the ChEMBL ID
			int start = response.indexOf("<chemblId>");
			// System.out.println("Start = "+start);
			if (start < 0) return null;
	
			start = start + 10;
	
			int end = response.indexOf("</chemblId>");
			// System.out.println("end = "+end);
			// System.out.println("id = "+xml.substring(start, end));
			return xml.substring(start, end);
		} catch (Exception e) { 
			e.printStackTrace();
			return null;
		}
	}
			
}
