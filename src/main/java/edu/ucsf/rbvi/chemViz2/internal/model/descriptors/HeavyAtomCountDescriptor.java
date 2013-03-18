package edu.ucsf.rbvi.chemViz2.internal.model.descriptors;

import java.util.List;

import org.openscience.cdk.interfaces.IMolecule;

import edu.ucsf.rbvi.chemViz2.internal.model.Compound;
import edu.ucsf.rbvi.chemViz2.internal.model.Descriptor;

public class HeavyAtomCountDescriptor implements Descriptor <Integer> {

	public HeavyAtomCountDescriptor() { }

	public String toString() {return "Heavy atom count"; }
	public String getShortName() {return "nheavy";}
	public Class getClassType() {return Integer.class;}
	public List<String> getDescriptorList() {return null;}

	@Override
	public Integer getDescriptor(Compound c) {
		IMolecule iMolecule = c.getMolecule();
		if (iMolecule == null) return null;
			int heavyAtomCount = 0;
			for (int i = 0; i < iMolecule.getAtomCount(); i++) {
				if (!(iMolecule.getAtom(i).getSymbol()).equals("H"))
					heavyAtomCount++;
			}
			return new Integer(heavyAtomCount);
	}
}
