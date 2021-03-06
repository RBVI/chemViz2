/* Copyright (C) 2009-2013  Syed Asad Rahman <asad@ebi.ac.uk>
 *
 * Contact: cdk-devel@lists.sourceforge.net
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 * All we ask is that proper credit is given for our work, which includes
 * - but is not limited to - adding the above copyright notice to the beginning
 * of your source code files, and to any copyright notice that you may distribute
 * with programs based on this work.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package edu.ucsf.rbvi.chemViz2.internal.smsd.algorithm.vflib.builder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


// import java.util.Objects;
import edu.ucsf.rbvi.chemViz2.internal.smsd.algorithm.matchers.AtomMatcher;
import edu.ucsf.rbvi.chemViz2.internal.smsd.algorithm.vflib.interfaces.IEdge;
import edu.ucsf.rbvi.chemViz2.internal.smsd.algorithm.vflib.interfaces.INode;

/**
 * Class for building/storing nodes (atoms) in the graph with atom query capabilities.
 *
 * @cdk.module smsd
 * @cdk.githash
 * @author Syed Asad Rahman <asad@ebi.ac.uk>
 */
public class NodeBuilder implements INode {

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 17 * hash + hashCode(this.matcher);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final NodeBuilder other = (NodeBuilder) obj;
        if (!equals(this.matcher, other.matcher)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "NodeBuilder{" + "matcher=" + matcher.getQueryAtom().getID()+ '}';
    }
    private List<INode> neighborsList;
    private List<IEdge> edgesList;
    private AtomMatcher matcher;

    /**
     * Construct a node for a query atom
     *
     * @param matcher
     */
    protected NodeBuilder(AtomMatcher matcher) {
        edgesList = new ArrayList<IEdge>();
        neighborsList = new ArrayList<INode>();
        this.matcher = matcher;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int countNeighbors() {
        return neighborsList.size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterable<INode> neighbors() {
        return Collections.unmodifiableList(neighborsList);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AtomMatcher getAtomMatcher() {
        return matcher;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<IEdge> getEdges() {
        return Collections.unmodifiableList(edgesList);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addEdge(EdgeBuilder edge) {
        edgesList.add(edge);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addNeighbor(NodeBuilder node) {
        neighborsList.add(node);
    }

		private int hashCode(Object obj) {
			if (obj == null) return 0;
			return obj.hashCode();
		}

		private boolean equals(Object a, Object b) {
			if (a == null && b == null) return true;
			if (a == null || b == null) return false;
			return (a.equals(b));
		}
}
