package edu.ucsf.rbvi.chemViz2.internal.model.descriptors;

import java.util.List;

import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

import edu.ucsf.rbvi.chemViz2.internal.model.CDKUtils;
import edu.ucsf.rbvi.chemViz2.internal.model.Compound;
import edu.ucsf.rbvi.chemViz2.internal.model.Descriptor;

public class MassDescriptor implements Descriptor <Double> {

	public MassDescriptor() { }

	public String toString() {return "Exact Mass"; }
	public String getShortName() {return "mass";}
	public Class getClassType() {return Double.class;}
	public List<String> getDescriptorList() {return null;}

	@Override
	public Double getDescriptor(Compound c) {
		IAtomContainer iMolecule = c.getMolecule();
		if (iMolecule == null) return null;
		IMolecularFormula mfa = MolecularFormulaManipulator.getMolecularFormula(CDKUtils.addh(iMolecule));
		return MolecularFormulaManipulator.getTotalExactMass(mfa);

	}
}
