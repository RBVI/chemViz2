package edu.ucsf.rbvi.chemViz2.internal.model.descriptors;

import java.util.List;

import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.qsar.IMolecularDescriptor;
import org.openscience.cdk.qsar.result.DoubleResult;
import org.openscience.cdk.qsar.descriptors.molecular.TPSADescriptor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucsf.rbvi.chemViz2.internal.model.Compound;
import edu.ucsf.rbvi.chemViz2.internal.model.Descriptor;

public class TPSA implements Descriptor <Double> {
  private static Logger logger = LoggerFactory.getLogger(TPSA.class);

	public TPSA() { }

	public String toString() {return "Topological Polar Surface Area"; }
	public String getShortName() {return "polarsurface";}
	public Class getClassType() {return Double.class;}
	public List<String> getDescriptorList() {return null;}

	@Override
	public Double getDescriptor(Compound c) {
		IAtomContainer iMolecule = c.getMolecule();
		if (iMolecule == null) return null;
		IMolecularDescriptor descriptor = 
			new org.openscience.cdk.qsar.descriptors.molecular.TPSADescriptor();
		DoubleResult retval = (DoubleResult)(descriptor.calculate(iMolecule).getValue());
		return retval.doubleValue();
	}
}
