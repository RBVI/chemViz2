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

import edu.ucsf.rbvi.chemViz2.internal.smsd.algorithm.matchers.BondMatcher;
import edu.ucsf.rbvi.chemViz2.internal.smsd.algorithm.vflib.interfaces.IEdge;
import edu.ucsf.rbvi.chemViz2.internal.smsd.algorithm.vflib.interfaces.INode;

/**
 * Class for building/storing edges (bonds) in the graph with bond
 * query capabilities.
 * @cdk.module smsd
 * @cdk.githash
 * @author Syed Asad Rahman <asad@ebi.ac.uk>
 */
public class EdgeBuilder implements IEdge {

    private NodeBuilder source;
    private NodeBuilder target;
    private BondMatcher matcher;

    /**
     * 
     * @param source
     * @param target
     * @param matcher
     */
    protected EdgeBuilder(NodeBuilder source, NodeBuilder target, BondMatcher matcher) {
        this.source = source;
        this.target = target;
        this.matcher = matcher;
    }

    /** {@inheritDoc}
     */
    @Override
    public INode getSource() {
        return source;
    }

    /** {@inheritDoc}
     */
    @Override
    public INode getTarget() {
        return target;
    }

    /** {@inheritDoc}
     */
    @Override
    public BondMatcher getBondMatcher() {
        return matcher;
    }
}
