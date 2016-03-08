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

package edu.ucsf.rbvi.chemViz2.internal.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Frame;
import java.awt.Image;

import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyIdentifiable;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;

import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;

import edu.ucsf.rbvi.chemViz2.internal.depict.DepictionGenerator;
import edu.ucsf.rbvi.chemViz2.internal.model.Compound;
import edu.ucsf.rbvi.chemViz2.internal.model.Compound.AttriType;
import edu.ucsf.rbvi.chemViz2.internal.model.TableUtils;

public class CompoundPopup extends JDialog implements ComponentListener {
	
	private List<Compound> compoundList;
	private Map<Component, Compound> imageMap;
	private String labelAttribute;
	private CyNetwork network;
	private static final int LABEL_HEIGHT = 20;
	enum LabelType {ATTRIBUTE, TEXT};

	public CompoundPopup(CyNetwork network, List<Compound> compoundList, List<CyIdentifiable> objectList, 
	                     String labelAttribute, String dialogTitle) {
		super();

		this.compoundList = compoundList;
		this.imageMap = new HashMap<Component, Compound>();
		this.labelAttribute = labelAttribute;
		this.network = network;

		if (dialogTitle == null) {
			if (objectList != null && objectList.size() > 0) 
				setTitle(getObjectTitle(objectList));
			else
				setTitle("2D Structures");
		} else {
			setTitle(dialogTitle);
		}

		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setBackground(Color.BLACK);

		addImages(400);
		pack();
		setVisible(true);
	}

	private String getObjectTitle(List<CyIdentifiable> objectList) {
		CyIdentifiable go = objectList.get(0);
		if (go instanceof CyNode) {
			if (objectList.size() == 1) {
				return("2D Structures for Node "+TableUtils.getName(network, go));
			} else {
				return("2D Structures for Selected Nodes");
			}
		} else  {
			if (objectList.size() == 1) {
				return("2D Structures for Edge "+TableUtils.getName(network, go));
			} else {
				return("2D Structures for Selected Edges");
			}
		}
	}

	public void componentHidden(ComponentEvent e) {}
	public void componentMoved(ComponentEvent e) {}
	public void componentShown(ComponentEvent e) {}
	public void componentResized(ComponentEvent e) {
		if (!(e.getComponent() instanceof JPanel))
			return;

		JPanel panel = (JPanel)e.getComponent();
		Component[] components = panel.getComponents();

		// If we have two components, component 0 is the image and
		// component 1 is the label
		JLabel label = (JLabel)components[0];

		// Get our new width
		int width = panel.getWidth();
		int height = panel.getHeight();
		// System.out.println("New size = "+width+"x"+height);

		if (imageMap.containsKey(label)) {
			Image img = imageMap.get(label).getImage(width, height-LABEL_HEIGHT, Color.WHITE, false);
			if (img != null) {
				label.setIcon(new ImageIcon(img));
				label.setSize(width, height-LABEL_HEIGHT);
			}
		}
	}


	// TODO: Add labels on image squares
	private void addImages(int width) {
		// How many images do we have?
		int structureCount = compoundList.size();
		int nCols = (int)Math.sqrt((double)structureCount);
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

		GridLayout layout = new GridLayout(nCols, structureCount/nCols, 1, 1);
		setLayout(layout);

		for (Compound compound: compoundList) {
			if (labelType == LabelType.ATTRIBUTE) {
				textLabel = TableUtils.getLabelAttribute(compound.getNetwork(), 
				                                         compound.getSource(), textLabel);
				if (textLabel == null)
					textLabel = TableUtils.getName(compound.getNetwork(), compound.getSource());
			}
			compound.setTitle(textLabel);

			// Get the image
			Image img = compound.getImage(width/nCols, width/nCols-LABEL_HEIGHT, Color.WHITE, false);

			JPanel panel = new JPanel();
			BoxLayout bl = new BoxLayout(panel, BoxLayout.Y_AXIS);
			panel.setLayout(bl);

			JLabel label = new JLabel(new ImageIcon(img));
			imageMap.put(label, compound);
			panel.add(label);

			JTextField tf = new JTextField(textLabel.toString());
			tf.setHorizontalAlignment(JTextField.CENTER);
			tf.setEditable(false);
			tf.setBorder(null);
			panel.add(tf);
			panel.addComponentListener(this);
			tf.setSize(width/nCols, LABEL_HEIGHT);

			add(panel);
		}
	}
}
