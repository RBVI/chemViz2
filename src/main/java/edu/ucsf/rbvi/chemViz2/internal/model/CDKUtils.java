package edu.ucsf.rbvi.chemViz2.internal.model;

import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.graph.ConnectivityChecker;
import org.openscience.cdk.interfaces.IMolecule;
import org.openscience.cdk.interfaces.IMoleculeSet;
import org.openscience.cdk.layout.StructureDiagramGenerator;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;

public class CDKUtils {
	static public IMolecule addh(IMolecule mol) {
		IMolecule molH;
		try {
			molH = (IMolecule)mol.clone();
		} catch (Exception e) {
			return mol;
		}

		if (molH == null) return mol;

		// Make sure we handle hydrogens
		AtomContainerManipulator.convertImplicitToExplicitHydrogens(molH);
		return molH;
	}

	static public IMolecule layoutMolecule(IMolecule mol) throws CDKException {
		// Is the structure connected?
		if (!ConnectivityChecker.isConnected(mol)) {
			// No, for now, find the largest component and use that exclusively
			IMoleculeSet molSet = ConnectivityChecker.partitionIntoMolecules(mol);
			IMolecule largest = molSet.getMolecule(0);
			for (int i = 0; i < molSet.getMoleculeCount(); i++) {
				if (molSet.getMolecule(i).getAtomCount() > largest.getAtomCount())
					largest = molSet.getMolecule(i);
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
