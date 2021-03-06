/*
 * Copyright (c) 2015 John May <jwmay@users.sf.net>
 *
 * Contact: cdk-devel@lists.sourceforge.net
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version. All we ask is that proper credit is given
 * for our work, which includes - but is not limited to - adding the above
 * copyright notice to the beginning of your source code files, and to any
 * copyright notice that you may distribute with programs based on this work.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 U
 */

package edu.ucsf.rbvi.chemViz2.internal.depict;

import org.openscience.cdk.CDKConstants;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.exception.InvalidSmilesException;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.isomorphism.Pattern;
import org.openscience.cdk.isomorphism.matchers.IQueryAtom;
import org.openscience.cdk.isomorphism.matchers.IQueryAtomContainer;
import org.openscience.cdk.isomorphism.matchers.IQueryBond;
import org.openscience.cdk.isomorphism.matchers.QueryAtomContainer;
import org.openscience.cdk.isomorphism.matchers.smarts.AnyOrderQueryBond;
import org.openscience.cdk.isomorphism.matchers.smarts.AtomicNumberAtom;
import org.openscience.cdk.isomorphism.matchers.smarts.SmartsMatchers;
import org.openscience.cdk.isomorphism.matchers.smarts.TotalConnectionAtom;
import org.openscience.cdk.isomorphism.matchers.smarts.TotalHCountAtom;
import org.openscience.cdk.isomorphism.matchers.smarts.TotalValencyAtom;
import org.openscience.cdk.sgroup.Sgroup;
import org.openscience.cdk.sgroup.SgroupType;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesGenerator;
import org.openscience.cdk.smiles.SmilesParser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.openscience.cdk.isomorphism.matchers.smarts.LogicalOperatorAtom.and;

/**
 * Utility class for abbreviating (sub)structures. Using either self assigned structural
 * motifs or pre-loading a common set a structure depiction can be made more concise with
 * the use of abbreviations (sometimes called superatoms). <p/>
 *
 * Basic usage:
 * <pre>{@code
 * Abbreviations abrv = new Abbreviations();
 *
 * // add some abbreviations, when overlapping (e.g. Me,Et,tBu) first one wins
 * abrv.add("[Na+].[H-] NaH");
 * abrv.add("*c1ccccc1 Ph");
 * abrv.add("*C(C)(C)C tBu");
 * abrv.add("*CC Et");
 * abrv.add("*C Me");
 *
 * // maybe we don't want 'Me' in the depiction
 * abrv.setEnabled("Me", false);
 *
 * // assign abbreviations with some filters
 * int numAdded = abrv.apply(mol);
 *
 * // generate all but don't assign, need to be added manually
 * // set/update the CDKConstants.CTAB_SGROUPS property of mol
 * List<Sgroup> sgroups = abrv.generate(mol);
 * }</pre>
 *
 * Predefined sets of abbreviations can be loaded, the following are
 * on the classpath.
 *
 * <pre>{@code
 * // https://www.github.com/openbabel/superatoms
 * abrv.loadFromFile("obabel_superatoms.smi");
 * }</pre>
 *
 * @cdk.keyword abbreviate
 * @cdk.keyword depict
 * @cdk.keyword superatom
 * @see CDKConstants#CTAB_SGROUPS
 * @see Sgroup
 */
public class Abbreviations implements Iterable<String> {

    private final Map<Pattern, String> connectedAbbreviations    = new LinkedHashMap<>();
    private final Map<String, String>  disconnectedAbbreviations = new LinkedHashMap<>();
    private final Set<String>          labels                    = new LinkedHashSet<>();
    private final Set<String>          disabled                  = new HashSet<>();

    private final SmilesParser smipar = new SmilesParser(SilentChemObjectBuilder.getInstance());

    public Abbreviations() {
    }

    /**
     * Iterate over loaded abbreviations. Both enabled and disabled abbreviations are listed.
     *
     * @return the abbreviations labels (e.g. Ph, Et, Me, OAc, etc.)
     */
    @Override
    public Iterator<String> iterator() {
        return Collections.unmodifiableSet(labels).iterator();
    }

    /**
     * Check whether an abbreviation is enabled.
     *
     * @param label is enabled
     * @return the label is enabled
     */
    public boolean isEnabled(final String label) {
        return labels.contains(label) && !disabled.contains(label);
    }

    /**
     * Set whether an abbreviation is enabled or disabled.
     *
     * @param label   the label (e.g. Ph, Et, Me, OAc, etc.)
     * @param enabled flag the label as enabled or disabled
     * @return the label state was modified
     */
    public boolean setEnabled(String label, boolean enabled) {
        return enabled ? labels.contains(label) && disabled.remove(label)
                       : labels.contains(label) && disabled.add(label);
    }

    /**
     * Find all enabled abbreviations in the provided molecule. They are not
     * added to the existing Sgroups and may need filtering.
     *
     * @param mol molecule
     * @return list of new abbreviation Sgroups
     */
    public List<Sgroup> generate(final IAtomContainer mol) {

        // mark which atoms have already been abbreviated or are
        // part of an existing Sgroup
        Set<IAtom> usedAtoms = new HashSet<>();
        List<Sgroup> sgroups = mol.getProperty(CDKConstants.CTAB_SGROUPS);
        if (sgroups != null) {
            for (Sgroup sgroup : sgroups)
                usedAtoms.addAll(sgroup.getAtoms());
        }

        // disconnected abbreviations, salts, common reagents, large compounds
        if (usedAtoms.isEmpty()) {
            try {
                String cansmi = SmilesGenerator.unique().create(mol);
                String label = disconnectedAbbreviations.get(cansmi);
                if (label != null && !disabled.contains(label)) {
                    Sgroup sgroup = new Sgroup();
                    sgroup.setType(SgroupType.CtabAbbreviation);
                    sgroup.setSubscript(label);
                    for (IAtom atom : mol.atoms())
                        sgroup.addAtom(atom);
                    return Collections.singletonList(sgroup);
                }
            } catch (CDKException ignored) {
            }
        }


        // attached abbreviations
        SmartsMatchers.prepare(mol, false);

        final List<Sgroup> newSgroups = new ArrayList<>();
        for (Map.Entry<Pattern, String> e : connectedAbbreviations.entrySet()) {
            if (disabled.contains(e.getValue()))
                continue;
            for (Map<IAtom, IAtom> atoms : e.getKey()
                                            .matchAll(mol)
                                            .uniqueAtoms()
                                            .toAtomMap()) {
                boolean overlap = false;
                final Set<IAtom> atomset = new HashSet<>(atoms.values());
                for (IAtom atom : atomset) {
                    if (usedAtoms.contains(atom)) {
                        overlap = true;
                        break;
                    }
                }

                // this hit overlaps with an existing one
                if (overlap)
                    continue;

                // create new abbreviation SGroup
                Sgroup sgroup = new Sgroup();
                sgroup.setType(SgroupType.CtabAbbreviation);
                sgroup.setSubscript(e.getValue());
                for (IAtom atom : atomset)
                    sgroup.addAtom(atom);

                // find crossing bonds
                boolean skip = false;
                for (IBond bond : mol.bonds()) {
                    IAtom beg = bond.getAtom(0);
                    IAtom end = bond.getAtom(1);
                    if (atomset.contains(beg) && !atomset.contains(end)) {
                        if (end.getAtomicNumber() == 1) {
                            sgroup.addAtom(end);
                            if (mol.getConnectedBondsCount(end) > 1)
                                skip = true;
                        } else {
                            sgroup.addBond(bond);
                        }
                    } else if (!atomset.contains(beg) && atomset.contains(end)) {
                        if (end.getAtomicNumber() == 1) {
                            sgroup.addAtom(end);
                            if (mol.getConnectedBondsCount(end) > 1)
                                skip = true;
                        } else {
                            sgroup.addBond(bond);
                        }
                    }
                }

                if (!skip || !sgroup.getBonds().isEmpty()) {
                    usedAtoms.addAll(sgroup.getAtoms());
                    newSgroups.add(sgroup);
                }
            }
        }

        return newSgroups;
    }

    /**
     * Generates and assigns abbreviations to a molecule. Abbrevations are first
     * generated with {@link #generate} and the filtered based on
     * the coverage. Currently only abbreviations that cover 100%, or < 40% of the
     * atoms are assigned.
     *
     * @param mol molecule
     * @return number of new abbreviations
     * @see #generate(IAtomContainer)
     */
    public int apply(final IAtomContainer mol) {
        List<Sgroup> newSgroups = generate(mol);
        List<Sgroup> sgroups    = mol.getProperty(CDKConstants.CTAB_SGROUPS);

        if (sgroups == null)
            sgroups = new ArrayList<>();
        else
            sgroups = new ArrayList<>(sgroups);

        int prev = sgroups.size();
        for (Sgroup sgroup : newSgroups) {
            double coverage = sgroup.getAtoms().size() / (double) mol.getAtomCount();
            // update javadoc if changed!
            if (sgroup.getBonds().isEmpty() || coverage < 0.4d)
                sgroups.add(sgroup);
        }
        mol.setProperty(CDKConstants.CTAB_SGROUPS, Collections.unmodifiableList(sgroups));
        return sgroups.size() - prev;
    }

    /**
     * Make a query atom that matches atomic number, h count, valence, and
     * connectivity. This effectively provides an exact match for that atom
     * type.
     *
     * @param mol  molecule
     * @param atom atom of molecule
     * @return the query atom (null if attachment point)
     */
    private IQueryAtom matchExact(final IAtomContainer mol, final IAtom atom) {
        final IChemObjectBuilder bldr = atom.getBuilder();

        int elem = atom.getAtomicNumber();

        // attach atom skipped
        if (elem == 0)
            return null;

        int hcnt = atom.getImplicitHydrogenCount();
        int val = hcnt;
        int con = hcnt;

        for (IBond bond : mol.getConnectedBondsList(atom)) {
            val += bond.getOrder().numeric();
            con++;
            if (bond.getConnectedAtom(atom).getAtomicNumber() == 1)
                hcnt++;
        }

        return and(and(new AtomicNumberAtom(elem, bldr),
                       new TotalConnectionAtom(con, bldr)),
                   and(new TotalHCountAtom(hcnt, bldr),
                       new TotalValencyAtom(val, bldr)));
    }

    /**
     * Internal - create a query atom container that exactly matches the molecule provided.
     * Similar to {@link org.openscience.cdk.isomorphism.matchers.QueryAtomContainerCreator}
     * but we can't access SMARTS query classes from that module (cdk-isomorphism).
     *
     * @param mol molecule
     * @return query container
     * @see org.openscience.cdk.isomorphism.matchers.QueryAtomContainerCreator
     */
    private IQueryAtomContainer matchExact(IAtomContainer mol) {
        final IChemObjectBuilder bldr = mol.getBuilder();
        final IQueryAtomContainer qry = new QueryAtomContainer(mol.getBuilder());
        final Map<IAtom, IAtom> atmmap = new HashMap<>();

        for (IAtom atom : mol.atoms()) {
            IAtom qatom = matchExact(mol, atom);
            if (qatom != null) {
                atmmap.put(atom, qatom);
                qry.addAtom(qatom);
            }
        }

        for (IBond bond : mol.bonds()) {
            final IAtom beg = atmmap.get(bond.getAtom(0));
            final IAtom end = atmmap.get(bond.getAtom(1));

            // attach bond skipped
            if (beg == null || end == null)
                continue;

            IQueryBond qbond = new AnyOrderQueryBond(bldr);
            qbond.setAtom(beg, 0);
            qbond.setAtom(end, 1);
            qry.addBond(qbond);
        }

        return qry;
    }

    private boolean addDisconnectedAbbreviation(IAtomContainer mol, String label) {
        try {
            String cansmi = SmilesGenerator.unique().create(mol);
            disconnectedAbbreviations.put(cansmi, label);
            labels.add(label);
            return true;
        } catch (CDKException e) {
            return false;
        }
    }

    private boolean addConnectedAbbreviation(IAtomContainer mol, String label) {
        connectedAbbreviations.put(Pattern.findSubstructure(matchExact(mol)),
                                   label);
        labels.add(label);
        return true;
    }

    /**
     * Convenience method to add an abbreviation from a SMILES string.
     *
     * @param line the smiles to add with a title (the label)
     * @return the abbreviation was added, will be false if no title supplied
     * @throws InvalidSmilesException the SMILES was not valid
     */
    public boolean add(String line) throws InvalidSmilesException {
        return add(smipar.parseSmiles(line), getSmilesSuffix(line));
    }

    /**
     * Add an abbreviation to the factory. Abbreviations can be of various flavour based
     * on the number of attachments:
     *
     * <p/>
     * <b>Detached</b> - zero attachments, the abbreviation covers the whole structure (e.g. THF) <p/>
     * <b>Terminal</b> - one attachment, covers substituents (e.g. Ph for Phenyl)<p/>
     * <b>Linker</b> - [NOT SUPPORTED YET] two attachments, covers long repeated chains (e.g. PEG4) <p/>
     *
     * Attachment points (if present) must be specified with zero element atoms. <p/>
     * <pre>
     * *c1ccccc1 Ph
     * *OC(=O)C OAc
     * </pre>
     *
     * @param mol   the fragment to abbreviate
     * @param label the label of the fragment
     * @return the abbreviation was added
     */
    public boolean add(IAtomContainer mol, String label) {

        if (label == null || label.isEmpty())
            return false;

        // required invariants and check for number of attachment points
        int numAttach = 0;
        for (IAtom atom : mol.atoms()) {
            if (atom.getImplicitHydrogenCount() == null || atom.getAtomicNumber() == null)
                throw new IllegalArgumentException("Implicit hydrogen count or atomic number is null");
            if (atom.getAtomicNumber() == 0)
                numAttach++;
        }

        switch (numAttach) {
            case 0:
                return addDisconnectedAbbreviation(mol, label);
            case 1:
                return addConnectedAbbreviation(mol, label);
            default:
                // not-supported yet - update JavaDoc if added
                return false;
        }
    }

    private static String getSmilesSuffix(String line) {
        final int last = line.length() - 1;
        for (int i = 0; i < last; i++)
            if (line.charAt(i) == ' ' || line.charAt(i) == '\t')
                return line.substring(i + 1).trim();
        return "";
    }

    private int loadSmiles(final InputStream in) throws IOException {
        int count = 0;
        try (BufferedReader brdr = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = brdr.readLine()) != null) {
                if (line.isEmpty() || line.charAt(0) == '#')
                    continue;
                try {
                    if (add(line))
                        count++;
                } catch (InvalidSmilesException e) {
                    e.printStackTrace();
                }
            }
        }
        return count;
    }

    /**
     * Load a set of abbreviations from a classpath resource or file.
     * <pre>
     * *c1ccccc1 Ph
     * *c1ccccc1 OAc
     * </pre>
     *
     * Available:
     * <pre>
     * obabel_superatoms.smi - https://www.github.com/openbabel/superatoms
     * </code>
     *
     * @param path classpath or filesystem path to a SMILES file
     * @return the number of loaded abbreviation
     * @throws IOException
     */
    public int loadFromFile(final String path) throws IOException {
        InputStream in = null;
        try {
            in = getClass().getResourceAsStream(path);
            if (in != null)
                return loadSmiles(in);
            File file = new File(path);
            if (file.exists() && file.canRead())
                return loadSmiles(new FileInputStream(file));
        } finally {
            if (in != null)
                in.close();
        }
        return 0;
    }
}
