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
import javax.swing.JSplitPane;
import javax.swing.JTextField;

import org.cytoscape.model.CyIdentifiable;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTableUtil;
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

public class CompoundResultPanel extends JPanel implements ComponentListener {
	private CyNetwork network;
	private Compound compound;
	private final ChemInfoSettings settings;
	private JPanel outerPanel;
	private JPanel structurePanel;
	private JLabel imageLabel;
	private JSplitPane splitPane;
	private OpenBrowser openBrowser;

	enum LabelType {ATTRIBUTE, TEXT};

	public CompoundResultPanel(ChemInfoSettings settings, CyNetwork network, Compound compound) {
		this.compound = compound;
		this.network = network;
		this.settings = settings;
		openBrowser = (OpenBrowser)settings.getServiceRegistrar().getService(OpenBrowser.class);

    setLayout(new BorderLayout());

		outerPanel = new JPanel();
		outerPanel.setSize(200, 400);

    add(BorderLayout.CENTER, outerPanel);

		createPanel();

	}

	@Override
	public void componentHidden(ComponentEvent e) {}

	@Override
	public void componentMoved(ComponentEvent e) {}

	@Override
	public void componentShown(ComponentEvent e) {}

	@Override
	public void componentResized(ComponentEvent e) {
		if (!(e.getComponent() instanceof JPanel)) 
			return;

		JPanel panel = (JPanel)e.getComponent();
		Component[] components = panel.getComponents();

		// Get our new width
		int width = panel.getWidth();
		int height = panel.getHeight();

		if (height < width)
			width = height;

		// Resize
		Image img = compound.getImage(width, width, Color.WHITE);
		imageLabel.setIcon(new ImageIcon(img));
		structurePanel.setSize(width, width);
		this.revalidate();
	}

	private void createPanel() {
		int width = outerPanel.getWidth();
		outerPanel.removeAll();

		addInfoPanel(width, compound); // Make the initial width a bit small

		this.revalidate();
		this.repaint();
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
			structurePanel = new JPanel();
			structurePanel.setBackground(Color.WHITE);
			structurePanel.setOpaque(true);
			BorderLayout bl2 = new BorderLayout();
			structurePanel.setLayout(bl2);

			// Now add our image
			Image img = compound.getImage(width, width, Color.WHITE);
			imageLabel = new JLabel(new ImageIcon(img));
			structurePanel.add(imageLabel, BorderLayout.CENTER);
			structurePanel.setMinimumSize(new Dimension(100, 100));
			structurePanel.setSize(width, width);
			structurePanel.addComponentListener(this);
			splitPane.setTopComponent(structurePanel);
		}

		// Finally, add the table
		final OpenBrowser ob = openBrowser;
		JEditorPane textArea = new JEditorPane("text/html", null);
		textArea.setEditable(false);
		textArea.addHyperlinkListener(new MyHyperlinkListener());

		String message = "<h2 style=\"margin-left: 5px;margin-bottom: 0px;\">CrossLinks</h2>";
		message += "<table style=\"margin-left: 10px;margin-top: 0px;\"><tr><td><a href=\""+PUBCHEM+compound.getMoleculeString()+"\">PubChem</a></td>";
		message += "<td><a href=\""+CHEMSPIDER+compound.getMoleculeString()+"\">ChemSpider</a> </td>";
		/*
		String chemblID = getChEMBLID(compound);
		if (chemblID != null)
			message += "<td><a href=\""+CHEMBL+chemblID+"\">ChEMBL</a> </td></tr></table>";
		else
			message += "</tr></table>";
		*/
		message += "</tr></table>";
		
		message += "<h2 style=\"margin-left: 5px;margin-bottom: 0px;\">Chemical Descriptor</h2>";
		message = addDescriptors(message, compound, "htmlformula", "mass", "weight", "roff", "acceptors", "donors", "alogp2", "smiles");
		message += "<br/>";
		textArea.setText(message);
    JScrollPane scrollPane = new JScrollPane(textArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                             JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		splitPane.setBottomComponent(scrollPane);
		outerPanel.add(BorderLayout.CENTER, splitPane);
	}

	private String addDescriptors(String message, Compound compound, String... descriptors) {
		DescriptorManager manager = settings.getDescriptorManager();
		message += "<table style=\"margin-left: 10px;\">";
		for (String descriptor: descriptors) {
			Descriptor d;
			if (descriptor.equals("htmlformula"))
				d = new HTMLFormulaDescriptor();
			else if (descriptor.equals("smiles"))
				//d = new SmilesDescriptor();
				continue;
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

	/**
	 * Get the chembl id from pubchem
	 **/
	private String getChEMBLID(Compound compound) {
		try {
			// Get the SMILES string
			String smiles = compound.getMoleculeString();
			// Construct the query
			URL query = new URL("https://pubchem.ncbi.nlm.nih.gov/rest/pug/compound/smiles/"+URLEncoder.encode(smiles, "UTF-8")+"/xrefs/RegistryID/JSON");
			// System.out.println("Query = "+query.toString());

			URLConnection connection = query.openConnection();
			// Get the XML back
			BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
	
			String line;
			StringBuilder json = new StringBuilder();
			while ((line = in.readLine()) != null) {
				if (line.strip().contains("CHEMBL")) {
					line = line.strip();
					String chembl = line.split("\"")[1];
					in.close();
					return chembl;
				}
			}
			in.close();
			return null;

		} catch (Exception e) { 
			// e.printStackTrace();
			return null;
		}
	}
			
}
