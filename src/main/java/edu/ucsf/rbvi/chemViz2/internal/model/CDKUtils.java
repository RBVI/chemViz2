package edu.ucsf.rbvi.chemViz2.internal.model;

import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.graph.ConnectivityChecker;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IAtomContainerSet;
import org.openscience.cdk.layout.StructureDiagramGenerator;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;

public class CDKUtils {
	static public IAtomContainer addh(IAtomContainer mol) {
		IAtomContainer molH;
		try {
			molH = (IAtomContainer)mol.clone();
		} catch (Exception e) {
			return mol;
		}

		if (molH == null) return mol;

		// Make sure we handle hydrogens
		AtomContainerManipulator.convertImplicitToExplicitHydrogens(molH);
		return molH;
	}

	static public IAtomContainer layoutMolecule(IAtomContainer mol) throws CDKException {
		// Is the structure connected?
		if (!ConnectivityChecker.isConnected(mol)) {
			// No, for now, find the largest component and use that exclusively
			IAtomContainerSet molSet = ConnectivityChecker.partitionIntoMolecules(mol);
			IAtomContainer largest = molSet.getAtomContainer(0);
			for (int i = 0; i < molSet.getAtomContainerCount(); i++) {
				if (molSet.getAtomContainer(i).getAtomCount() > largest.getAtomCount())
					largest = molSet.getAtomContainer(i);
			}
			mol = largest;
		}
		StructureDiagramGenerator sdg = new StructureDiagramGenerator();
		sdg.setUseTemplates(false);
		sdg.setMolecule(mol);
		sdg.generateCoordinates();
		return sdg.getMolecule();
	}
}
