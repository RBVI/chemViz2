package edu.ucsf.rbvi.chemViz2.internal.model.descriptors;

import java.util.Iterator;
import java.util.List;

import org.openscience.cdk.CDKConstants;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IAtomType.Hybridization;
import org.openscience.cdk.interfaces.IMolecule;
import org.openscience.cdk.interfaces.IRing;
import org.openscience.cdk.interfaces.IRingSet;
import org.openscience.cdk.ringsearch.SSSRFinder;

import edu.ucsf.rbvi.chemViz2.internal.model.Compound;
import edu.ucsf.rbvi.chemViz2.internal.model.Descriptor;

public class AromaticRingCountDescriptor implements Descriptor <Integer> {

	public AromaticRingCountDescriptor() { }

	public String toString() {return "Aromatic ring count"; }
	public String getShortName() {return "naromatic";}
	public Class getClassType() {return Integer.class;}
	public List<String> getDescriptorList() {return null;}

	@Override
	public Integer getDescriptor(Compound c) {
		IMolecule iMolecule = c.getMolecule();
		if (iMolecule == null) return null;

		SSSRFinder finder = new SSSRFinder(iMolecule);
		IRingSet ringSet = finder.findSSSR();

		// We have the number of rings, now we want to restrict
		// the ring set to aromatic rings only
		Iterator<IAtomContainer> i = ringSet.atomContainers().iterator();
		while (i.hasNext()) {
			IRing r = (IRing) i.next();
			if (r.getAtomCount() > 8) {
				i.remove();
			} else {
				for (IAtom a: r.atoms()) {
					Hybridization h = a.getHybridization();
					if (h == CDKConstants.UNSET
					    || !(h == Hybridization.SP2
					    || h == Hybridization.PLANAR3)) {
						i.remove();
						break;
					}
				}
			}
		}
		return new Integer(ringSet.getAtomContainerCount());
	}
}
