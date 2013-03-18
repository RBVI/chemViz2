package edu.ucsf.rbvi.chemViz2.internal.model.descriptors;

import java.util.List;

import org.openscience.cdk.config.IsotopeFactory;
import org.openscience.cdk.interfaces.IIsotope;
import org.openscience.cdk.interfaces.IMolecule;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucsf.rbvi.chemViz2.internal.model.CDKUtils;
import edu.ucsf.rbvi.chemViz2.internal.model.Compound;
import edu.ucsf.rbvi.chemViz2.internal.model.Descriptor;

public class WeightDescriptor implements Descriptor <Double> {
  private static Logger logger = LoggerFactory.getLogger(WeightDescriptor.class);

	public WeightDescriptor() { }

	public String toString() {return "Molecular Weight"; }
	public String getShortName() {return "weight";}
	public Class getClassType() {return Double.class;}
	public List<String> getDescriptorList() {return null;}

	@Override
	public Double getDescriptor(Compound c) {
		IMolecule iMolecule = c.getMolecule();
		if (iMolecule == null) return null;
    IMolecularFormula mfa = MolecularFormulaManipulator.getMolecularFormula(CDKUtils.addh(iMolecule));
    double mass = 0.0;
    for (IIsotope isotope : mfa.isotopes()) {
    	try {
    		IIsotope isotope2 = IsotopeFactory.getInstance(mfa.getBuilder()).getMajorIsotope(isotope.getSymbol());
    		mass += isotope2.getMassNumber() * mfa.getIsotopeCount(isotope);
    	} catch (Exception e) {
				logger.warn("Unable to calculate weight: "+e.getMessage());
    	}
    }
    return new Double(mass);
	}
}
