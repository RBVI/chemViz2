package edu.ucsf.rbvi.chemViz2.internal.model.descriptors;

import java.util.List;

import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IMolecule;

import edu.ucsf.rbvi.chemViz2.internal.model.CDKUtils;
import edu.ucsf.rbvi.chemViz2.internal.model.Compound;
import edu.ucsf.rbvi.chemViz2.internal.model.Descriptor;

public class AtomicCompositionDescriptor implements Descriptor <Double> {

	public AtomicCompositionDescriptor() { }

	public String toString() {return "Atomic composition"; }
	public String getShortName() {return "atomiccomp";}
	public Class getClassType() {return Double.class;}
	public List<String> getDescriptorList() {return null;}

	@Override
	public Double getDescriptor(Compound c) {
		IMolecule iMolecule = c.getMolecule();
		if (iMolecule == null) return null;

		int totalAtoms = 0;
		int polarAtoms = 0;
		for (IAtom a: iMolecule.atoms()) {
			String symbol = a.getSymbol();
			if (symbol.equals("N")) {
				polarAtoms++;
			} else if (symbol.equals("C")) {
				totalAtoms++;
			} else if (symbol.equals("O")) {
				polarAtoms++;
			} else if (symbol.equals("P")) {
				polarAtoms++;
			} else if (symbol.equals("S")) {
				polarAtoms++;
			}
		}
		return new Double((double)polarAtoms/(double)(polarAtoms+totalAtoms));
	}
}
