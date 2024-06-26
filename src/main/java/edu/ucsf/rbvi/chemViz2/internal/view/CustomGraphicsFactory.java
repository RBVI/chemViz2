package edu.ucsf.rbvi.chemViz2.internal.view;

import java.net.URL;

import org.cytoscape.model.CyIdentifiable;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.presentation.customgraphics.CyCustomGraphics;
import org.cytoscape.view.presentation.customgraphics.CyCustomGraphicsFactory;
import org.cytoscape.view.presentation.customgraphics.CustomGraphicLayer;
import org.cytoscape.view.presentation.customgraphics.PaintedShape;

import edu.ucsf.rbvi.chemViz2.internal.model.ChemInfoSettings;
import edu.ucsf.rbvi.chemViz2.internal.model.Compound;
import edu.ucsf.rbvi.chemViz2.internal.model.Compound.AttriType;
import edu.ucsf.rbvi.chemViz2.internal.model.CompoundManager;

public class CustomGraphicsFactory implements CyCustomGraphicsFactory<PaintedShape> {
	CompoundManager compoundManager;
	ChemInfoSettings settings;

	public CustomGraphicsFactory(ChemInfoSettings settings) {
		this.settings = settings;
		compoundManager = settings.getCompoundManager();
	}

	public CyCustomGraphics<PaintedShape> getInstance(String input) {
		if (input == null || input == "null")
			return null;

		System.out.println("input is not null");

		// Do we have a compound? (assumes input is a SMILES string...)
		Compound compound = compoundManager.getCompound(input);

		// No, create one
		if (compound == null) {
			System.out.println("compound is null");
			try {
				compound = new Compound(settings, null, null, null, input, AttriType.smiles);
			} catch (RuntimeException e) {
				return null;
			}
		}

		compound.layoutStructure();

		return new ChemVizCustomGraphics(compound, ((double)settings.getNodeStructureSize())/100.0);
	}

	public CyCustomGraphics<PaintedShape> getInstance(URL url) {
		return null; // not yet...
	}

	public String getPrefix() {
		return "chemviz";
	}

	public Class<? extends CyCustomGraphics> getSupportedClass() { return ChemVizCustomGraphics.class; }

	public CyCustomGraphics<PaintedShape> parseSerializableString(String string) { return null; }

	public boolean supportsMime(String mimeType) { return false; }
	
}
