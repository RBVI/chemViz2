package edu.ucsf.rbvi.chemViz2.internal.model.descriptors;

import java.util.List;

import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IRingSet;
import org.openscience.cdk.ringsearch.SSSRFinder;

import edu.ucsf.rbvi.chemViz2.internal.model.Compound;
import edu.ucsf.rbvi.chemViz2.internal.model.Descriptor;

public class RingCountDescriptor implements Descriptor <Integer> {

	public RingCountDescriptor() { }

	public String toString() {return "Ring count"; }
	public String getShortName() {return "nrings";}
	public Class getClassType() {return Integer.class;}
	public List<String> getDescriptorList() {return null;}

	@Override
	public Integer getDescriptor(Compound c) {
		IAtomContainer iMolecule = c.getMolecule();
		if (iMolecule == null) return null;

		SSSRFinder finder = new SSSRFinder(iMolecule);
		IRingSet ringSet = finder.findSSSR();
		return new Integer(ringSet.getAtomContainerCount());
	}
}
