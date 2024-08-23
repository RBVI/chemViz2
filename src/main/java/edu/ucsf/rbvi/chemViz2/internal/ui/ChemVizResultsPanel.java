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
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Image;

import java.awt.BorderLayout;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

import java.text.DecimalFormat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JTextField;

import org.cytoscape.application.swing.CytoPanelState;
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
import org.cytoscape.util.swing.IconManager;
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
import edu.ucsf.rbvi.chemViz2.internal.tasks.CalculateNodeMCSSTaskFactory;
import edu.ucsf.rbvi.chemViz2.internal.tasks.ChemVizAbstractTaskFactory.Scope;
import edu.ucsf.rbvi.chemViz2.internal.tasks.SearchNodesTaskFactory;
import edu.ucsf.rbvi.chemViz2.internal.tasks.UpdateCompoundsTask;

public class ChemVizResultsPanel extends JPanel implements CytoPanelComponent, 
                                                           RowsSetListener, 
                                                           SetCurrentNetworkListener {
	private final Font iconFont;
	private final Font labelFont;
	private final Font textFont;
	private final String panelName;
	private final String labelAttribute;
	private final ChemInfoSettings settings;
	private final CompoundManager mgr;
	private CyNetwork network;
	private List<Compound> compoundList;
	private JScrollPane scrollPane;
	private JPanel compoundsPanel;

	private static final int LABEL_HEIGHT = 20;
	enum LabelType {ATTRIBUTE, TEXT};

	public ChemVizResultsPanel(final ChemInfoSettings settings, final String labelAttribute, 
	                           final String panelName) {
		this.panelName = panelName;
		this.labelAttribute = labelAttribute;
		this.settings = settings;

		this.mgr = settings.getCompoundManager();
		network = settings.getCurrentNetwork();

		IconManager iconManager = settings.getServiceRegistrar().getService(IconManager.class);
    iconFont = iconManager.getIconFont(17.0f);
    labelFont = new Font("SansSerif", Font.BOLD, 10);
    textFont = new Font("SansSerif", Font.PLAIN, 10);


		updateSelection();
		init();

		if (settings.getAutoShow()) {
			checkShow();
		}
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

		updateCompoundsPanel();
		if (settings.getAutoShow()) {
			checkShow();
		}
	}

	@Override
	public void handleEvent(RowsSetEvent e) {
		if (!e.containsColumn(CyNetwork.SELECTED))
			return;

		// It's a selection event and it's in our network
		updateCompoundsPanel();
	}

	public void checkShow() {
		// Get all nodes
		Collection<CyNode> nodeList = network.getNodeList();
		if (settings.hasNodeCompounds(nodeList)) {
			settings.getServiceRegistrar().registerService(this, CytoPanelComponent.class, new Properties());
		} else {
			settings.getServiceRegistrar().unregisterService(this, CytoPanelComponent.class);
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

	}

	private void updateCompoundsPanel() {
		if (compoundsPanel == null) return;
    compoundsPanel.removeAll();
    EasyGBC c = new EasyGBC();

		updateSelection();

		for (Compound cmpd: compoundList) {
			JPanel newPanel = createCompoundPanel(cmpd);
			if (newPanel == null)
				continue;
			newPanel.setAlignmentX( Component.LEFT_ALIGNMENT );

			compoundsPanel.add(newPanel, c.anchor("west").down().expandHoriz());
		}
    return ;
	}

	private void init() {
		setLayout(new GridBagLayout());

    EasyGBC c = new EasyGBC();

    JPanel controlPanel = createControlPanel();
    controlPanel.setBorder(BorderFactory.createEmptyBorder(0,10,0,0));
    add(controlPanel, c.anchor("west").down().noExpand());

    JPanel mainPanel = new JPanel();
    {
      mainPanel.setLayout(new GridBagLayout());
      // mainPanel.setBackground(defaultBackground);
      EasyGBC d = new EasyGBC();

      mainPanel.add(createCompoundsPanel(), d.down().anchor("west").expandHoriz());
      mainPanel.add(new JLabel(""), d.down().anchor("west").expandBoth());
    }
    JScrollPane scrollPane = new JScrollPane(mainPanel, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                                             JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    scrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);
    add(scrollPane, c.down().anchor("west").expandBoth());
  }

	private JPanel createControlPanel() {
		JPanel controlPanel = new JPanel();
    EasyGBC d = new EasyGBC();
    controlPanel.setLayout(new GridBagLayout());

		EasyGBC upperGBC = new EasyGBC();
		JPanel upperPanel = new JPanel(new GridBagLayout());
    {
			JCheckBox paintStructures = new JCheckBox("Paint Structures on Nodes");
      paintStructures.setFont(labelFont);
      paintStructures.setSelected(settings.getStructuresShown());
      paintStructures.addItemListener(new ItemListener() {
        public void itemStateChanged(ItemEvent e) {
					System.out.println("Item changed");
          settings.execute(
            settings.getPaintNodeStructuresTaskFactory().createTaskIterator(settings.getCurrentNetworkView(), Scope.ALLNODES, settings.getStructuresShown()), true);
        }
      });
      upperPanel.add(paintStructures, upperGBC.right().insets(0,10,0,0).noExpand());
    }

		controlPanel.add(upperPanel);

		JPanel lowerPanel = new JPanel();
    GridLayout layout2 = new GridLayout(1, 2);
    // GridLayout layout2 = new GridLayout(3,2);
    layout2.setVgap(0);
    lowerPanel.setLayout(layout2);
    {
      JButton smartButton = new JButton("SMARTS");
      smartButton.setToolTipText("SMARTS Search");
      smartButton.setFont(labelFont);
      lowerPanel.add(smartButton);
      smartButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          SearchNodesTaskFactory tf = new SearchNodesTaskFactory(settings, true, Scope.ALLNODES);
          settings.execute(tf.createTaskIterator(network), false);
        }
      });

      JButton mcssButton = new JButton("MCSS");
      mcssButton.setToolTipText("Maximum common substructure");
      mcssButton.setFont(labelFont);
      lowerPanel.add(mcssButton);
      mcssButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
					CalculateNodeMCSSTaskFactory tf = 
							new CalculateNodeMCSSTaskFactory(settings, null, null, true, false, Scope.ALLNODES);
          settings.execute(tf.createTaskIterator(network), false);
        }
      });
    }

		controlPanel.add(lowerPanel, d.down().anchor("west").expandHoriz());

		return controlPanel;
	}

	private JPanel createCompoundsPanel() {
    compoundsPanel = new JPanel();
    compoundsPanel.setLayout(new GridBagLayout());
    EasyGBC c = new EasyGBC();

    if (network != null) {
      for (Compound cmpd: compoundList) {
        JPanel newPanel = createCompoundPanel(cmpd);
        if (newPanel == null)
          continue;
        newPanel.setAlignmentX( Component.LEFT_ALIGNMENT );

        compoundsPanel.add(newPanel, c.anchor("west").down().expandHoriz());
      }
    }
    compoundsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
    return compoundsPanel;
	}

	private JPanel createCompoundPanel(Compound compound) {
		JPanel compoundPanel = new CompoundResultPanel(settings, network, compound);
    CollapsablePanel collapsablePanel = new CollapsablePanel(iconFont, getLabel(compound,labelAttribute), compoundPanel, false, 10);
    collapsablePanel.setBorder(BorderFactory.createEtchedBorder());
		return collapsablePanel;
	}

	private String getLabel(Compound compound, String labelAttribute) {
		LabelType labelType = LabelType.ATTRIBUTE;
		String textLabel = labelAttribute;

		// Get the right attributes
		if (labelAttribute != null && labelAttribute.startsWith("node.")) {
			textLabel = "Node "+labelAttribute.substring(5);
		} else if (labelAttribute != null && labelAttribute.startsWith("edge.")) {
			textLabel = "Edge "+labelAttribute.substring(5);
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

	private String wrap(String string, int width) {
		if (string.length() <= width)
			return string;
		String chunk = string.substring(0, width);
		return chunk+"\n"+wrap(string.substring(width,string.length()), width);
	}

}
