package edu.ucsf.rbvi.chemViz2.internal.model.descriptors;

import java.util.List;

import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

import edu.ucsf.rbvi.chemViz2.internal.model.Compound;
import edu.ucsf.rbvi.chemViz2.internal.model.Descriptor;

public class FormulaDescriptor implements Descriptor <String> {

	public FormulaDescriptor() { }

	public String toString() {return "Molecular Formula"; }
	public String getShortName() {return "formula";}
	public Class getClassType() {return String.class;}
	public List<String> getDescriptorList() {return null;}

	@Override
	public String getDescriptor(Compound c) {
		IMolecularFormula formula = MolecularFormulaManipulator.getMolecularFormula(c.getMolecule());
		if (formula == null) return "";
		return MolecularFormulaManipulator.getString(formula);
	}
}
