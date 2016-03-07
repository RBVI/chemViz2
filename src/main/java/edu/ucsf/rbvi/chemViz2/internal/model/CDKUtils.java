package edu.ucsf.rbvi.chemViz2.internal.model;

import javax.vecmath.Point2d;
import org.openscience.cdk.AtomContainerSet;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.geometry.GeometryTools;
import org.openscience.cdk.graph.ConnectivityChecker;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IAtomContainerSet;
import org.openscience.cdk.interfaces.IReaction;
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

	static public IReaction layoutReaction(IReaction reaction) throws CDKException {
		int offset = -10;
		// Layout agents
		AtomContainerSet agents = new AtomContainerSet();
		for (IAtomContainer agent: reaction.getAgents().atomContainers()) {
			IAtomContainer newAgent = layoutMolecule(agent);
			agents.addAtomContainer(newAgent);
		}
		// ??? no setAgents method ???
		reaction.getAgents().removeAllAtomContainers();
		for (IAtomContainer agent: agents.atomContainers()) {
			reaction.addAgent(agent);
		}

		// Layout reactants
		AtomContainerSet reactants = new AtomContainerSet();
		for (IAtomContainer reactant: reaction.getReactants().atomContainers()) {
			IAtomContainer newReactant = layoutMolecule(reactant);
			GeometryTools.translate2DCenterTo(newReactant, new Point2d(offset, 0));
			offset += 2;
			reactants.addAtomContainer(newReactant);
		}
		reaction.setReactants(reactants);
		// Layout Products
		offset += 6;
		AtomContainerSet products = new AtomContainerSet();
		for (IAtomContainer product: reaction.getProducts().atomContainers()) {
			IAtomContainer newProduct = layoutMolecule(product);
			GeometryTools.translate2DCenterTo(newProduct, new Point2d(offset, 0));
			offset += 2;
			products.addAtomContainer(newProduct);
		}
		reaction.setProducts(products);
		return reaction;
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
