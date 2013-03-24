package edu.ucsf.rbvi.chemViz2.internal.model.descriptors;

import java.util.List;

import org.openscience.cdk.interfaces.IAtomContainer;

import edu.ucsf.rbvi.chemViz2.internal.model.Compound;
import edu.ucsf.rbvi.chemViz2.internal.model.Descriptor;

public class ImageDescriptor implements Descriptor <Compound> {

	public ImageDescriptor() { }

	public String toString() {return "2D Image"; }
	public String getShortName() {return "image";}
	public Class getClassType() {return Compound.class;}
	public List<String> getDescriptorList() {return null;}

	@Override
	public Compound getDescriptor(Compound c) {
		return c;
	}
}
