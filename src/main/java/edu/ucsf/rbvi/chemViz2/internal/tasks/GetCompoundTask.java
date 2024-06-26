/*
  Copyright (c) 2006, 2007, 2008 The Cytoscape Consortium (www.cytoscape.org)

  The Cytoscape Consortium is:
  - Institute for Systems Biology
  - University of California San Diego
  - Memorial Sloan-Kettering Cancer Center
  - Institut Pasteur
  - Agilent Technologies

  This library is free software; you can redistribute it and/or modify it
  under the terms of the GNU Lesser General Public License as published
  by the Free Software Foundation; either version 2.1 of the License, or
  any later version.

  This library is distributed in the hope that it will be useful, but
  WITHOUT ANY WARRANTY, WITHOUT EVEN THE IMPLIED WARRANTY OF
  MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.  The software and
  documentation provided hereunder is on an "as is" basis, and the
  Institute for Systems Biology and the Whitehead Institute
  have no obligations to provide maintenance, support,
  updates, enhancements or modifications.  In no event shall the
  Institute for Systems Biology and the Whitehead Institute
  be liable to any party for direct, indirect, special,
  incidental or consequential damages, including lost profits, arising
  out of the use of this software and its documentation, even if the
  Institute for Systems Biology and the Whitehead Institute
  have been advised of the possibility of such damage.  See
  the GNU Lesser General Public License for more details.

  You should have received a copy of the GNU Lesser General Public License
  along with this library; if not, write to the Free Software Foundation,
  Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
 */

package edu.ucsf.rbvi.chemViz2.internal.tasks;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.cytoscape.model.CyIdentifiable;
import org.cytoscape.model.CyNetwork;

import edu.ucsf.rbvi.chemViz2.internal.model.ChemInfoSettings;
import edu.ucsf.rbvi.chemViz2.internal.model.Compound;
import edu.ucsf.rbvi.chemViz2.internal.model.Compound.AttriType;


public class GetCompoundTask implements Callable<Compound> {
	private CyIdentifiable go;
	private ChemInfoSettings settings;
	private CyNetwork network;
	private String attr;
	private String cstring;
	private AttriType type;
	private Compound result = null;
	static List<Compound> threadResultsList = null;

	static public List<Compound> runThreads(int maxThreads, List<GetCompoundTask> getList) {
		List<Compound> results = new ArrayList<Compound>();
		
		if (getList == null || getList.size() == 0) 
			return results;

		int nThreads = Runtime.getRuntime().availableProcessors()-1;
		if (maxThreads > 0)
			nThreads = maxThreads;

		// System.out.println("Getting "+getList.size()+" compounds using "+nThreads+" threads");

		ExecutorService threadPool = Executors.newFixedThreadPool(nThreads);

		try {
			List<Future<Compound>> futures = threadPool.invokeAll(getList);
			for (Future<Compound> future: futures)
				results.add(future.get());
		} catch (Exception e) {
			System.out.println("Execution exception: "+e);
			e.printStackTrace();
		}

		return results;
	}

	public GetCompoundTask(ChemInfoSettings settings, CyIdentifiable go, 
	                       CyNetwork network, String attr, 
	                       String cstring, AttriType type) {
		this.go = go;
		this.network = network;
		this.attr = attr;
		this.cstring = cstring;
		this.type = type;
		this.settings = settings;
		this.result = null;
	}

	public Compound call() {
		// System.out.println("Thread "+Thread.currentThread()+" fetching "+go+"["+attr+"]");
		try {
			result = new Compound(settings, go, network, attr, cstring, type);
		} catch (RuntimeException e) {
			result = null;
		}
		return result;
	}

	public Compound get() { 
		if (result == null) 
			return call();
		else
			return result; 
	}

}
