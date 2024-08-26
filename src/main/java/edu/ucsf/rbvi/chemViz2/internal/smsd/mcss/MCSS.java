/*
 * Copyright (C) 2013 Syed Asad Rahman <asad at ebi.ac.uk>.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package edu.ucsf.rbvi.chemViz2.internal.smsd.mcss;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;

import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.tools.ILoggingTool;
import org.openscience.cdk.tools.LoggingToolFactory;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;

import edu.ucsf.rbvi.chemViz2.internal.smsd.tools.AtomContainerComparator;

/**
 * @cdk.module smsd
 * @cdk.githash
 * @author Syed Asad Rahman <asad@ebi.ac.uk>
 *
 */
final public class MCSS {

    private final static ILoggingTool logger
            = LoggingToolFactory.createLoggingTool(MCSS.class);
    private final Collection<IAtomContainer> calculateMCSS;
    private TaskUpdater updater = null;
    private double percentDone = 0.01;
    private final boolean matchBonds;
    private final boolean matchRings;
    private final boolean matchAtomType;

    /**
     *
     * @param jobList
     * @param jobType
     * @param updater
     * @param numberOfThreads
     */
    public MCSS(List<IAtomContainer> jobList, JobType jobType, TaskUpdater updater, int numberOfThreads) {
        this(jobList, jobType, updater, numberOfThreads, true, true, true);
    }

    /**
     *
     * @param jobList
     * @param jobType
     * @param updater
     * @param numberOfThreads
     * @param matchBonds
     * @param matchRings
     * @param matchAtomType
     */
    public MCSS(List<IAtomContainer> jobList, 
		            JobType jobType, TaskUpdater updater, 
		            int numberOfThreads, boolean matchBonds, 
		            boolean matchRings, boolean matchAtomType) {
        int threadsAvailable = Runtime.getRuntime().availableProcessors() - 1;
        this.updater = updater;

        logger.debug("Demand threads: " + numberOfThreads);
        logger.debug(", Available threads: " + threadsAvailable);
        // System.out.println("Demand threads: " + numberOfThreads);
        // System.out.println(", Available threads: " + threadsAvailable);
        if (numberOfThreads > 0 && threadsAvailable >= numberOfThreads) {
            threadsAvailable = numberOfThreads;
        } else if (threadsAvailable <= 0) {
            threadsAvailable = 1;
        }
        logger.debug(", Assigned threads: " + threadsAvailable + "\n");
        // System.out.println(", Assigned threads: " + threadsAvailable + "\n");
        /*
         * Remove hydrogen from the molecules
         **/
        List<IAtomContainer> selectedJobs = new ArrayList<IAtomContainer>(jobList.size());
        for (IAtomContainer ac : jobList) {
            selectedJobs.add(AtomContainerManipulator.removeHydrogens(ac));
        }
        if (updater != null) updater.updateStatus("Calculating MCSS using "+threadsAvailable+" threads");
        /*
         * Sort the molecules in the ascending order of atom size and atom type
         */
        Comparator<IAtomContainer> comparator = new AtomContainerComparator();
        Collections.sort(selectedJobs, comparator);
        this.matchBonds = matchBonds;
        this.matchRings = matchRings;
        this.matchAtomType = matchAtomType;
        /*
         * Call the MCS
         */
        calculateMCSS = calculateMCSS(selectedJobs, jobType, threadsAvailable);
        selectedJobs.clear();
    }

    private synchronized Collection<IAtomContainer> calculateMCSS(List<IAtomContainer> mcssList, JobType jobType, int nThreads) {
        if (updater != null) {
            updater.setTotalCount(mcssList.size());
            updater.incrementCount();
        }
        List<IAtomContainer> newMCSSList;
        if (nThreads == 1) {
            newMCSSList = new LinkedList<IAtomContainer>(submitSingleThreadedJob(mcssList, jobType, updater, nThreads));
        } else {
            /*
             * Calling recursive MCS
             */
            newMCSSList = new LinkedList<IAtomContainer>(submitMultiThreadedJob(mcssList, jobType, updater, nThreads));
            while (newMCSSList.size() > 1) {
                if (newMCSSList.size() > 2) {
                    newMCSSList = new LinkedList<IAtomContainer>(submitMultiThreadedJob(newMCSSList, jobType, updater, nThreads));
                } else {
                    newMCSSList = new LinkedList<IAtomContainer>(submitMultiThreadedJob(newMCSSList, jobType, updater, 1));
                }
            }
        }
        if (!mcssList.isEmpty() && !newMCSSList.isEmpty()) {
            IAtomContainer inTheList = mcssList.get(mcssList.size() - 1);
            if (inTheList == newMCSSList.iterator().next()) {
                return new LinkedBlockingQueue<IAtomContainer>();
            }
        }
        return newMCSSList;
    }

    /**
     * @return the calculateMCSS
     */
    public synchronized Collection<IAtomContainer> getCalculateMCSS() {
        return Collections.unmodifiableCollection(calculateMCSS);
    }

    private synchronized LinkedBlockingQueue<IAtomContainer> submitSingleThreadedJob(List<IAtomContainer> mcssList, JobType jobType, TaskUpdater updater, int nThreads) {
        LinkedBlockingQueue<IAtomContainer> solutions = new LinkedBlockingQueue<IAtomContainer>();
        MCSSThread task = new MCSSThread(mcssList, jobType, updater, 1);
        LinkedBlockingQueue<IAtomContainer> results = task.call();
        if (results != null) {
            solutions.addAll(results);
        }
        return solutions;
    }

    private synchronized LinkedBlockingQueue<IAtomContainer> submitMultiThreadedJob(List<IAtomContainer> mcssList, JobType jobType, TaskUpdater updater, int nThreads) {
        int taskNumber = 1;
        LinkedBlockingQueue<IAtomContainer> solutions = new LinkedBlockingQueue<IAtomContainer>();
        LinkedBlockingQueue<Callable<LinkedBlockingQueue<IAtomContainer>>> callablesQueue = 
					new LinkedBlockingQueue<Callable<LinkedBlockingQueue<IAtomContainer>>>();
        ExecutorService threadPool = Executors.newFixedThreadPool(nThreads);
        int step = (int) Math.ceil((double) mcssList.size() / (double) nThreads);
        if (step < 2) {
            step = 2; // Can't have a step size of less than 2
        }
        for (int i = 0; i < mcssList.size(); i += step) {
            int endPoint = i + step;
            if (endPoint > mcssList.size()) {
                endPoint = mcssList.size();
            }
            List<IAtomContainer> subList = new ArrayList<IAtomContainer>(mcssList.subList(i, endPoint));
            if (subList.size() > 1) {
                MCSSThread mcssJobThread = new MCSSThread(subList, jobType, updater, taskNumber, matchBonds, matchRings, matchAtomType);
                callablesQueue.add(mcssJobThread);
                taskNumber++;
            } else {
                solutions.add(subList.get(0));
            }
        }
        try {
            /*
             * Wait for all the threads to finish
             */
            List<Future<LinkedBlockingQueue<IAtomContainer>>> futureList = threadPool.invokeAll(callablesQueue);
            /*
             * Collect the results
             */
            // for (Iterator<Future<LinkedBlockingQueue<IAtomContainer>>> it = futureList.iterator(); it.hasNext();) {
            //     Future<LinkedBlockingQueue<IAtomContainer>> callable = it.next();
						for (Future<LinkedBlockingQueue<IAtomContainer>> callable: futureList) {
                LinkedBlockingQueue<IAtomContainer> mapping = callable.get();
                if (callable.isDone() && mapping != null) {
										if (mapping.size() > 0)
											solutions.addAll(mapping);
										else {
											solutions.clear();
											break;
										}
                } else {
                    logger.warn("WARNING: InComplete job in AtomMappingTool: ");
                }
            }
            threadPool.shutdown();
            // Wait until all threads are finish
            while (!threadPool.isTerminated()) {
            }
            System.gc();
        } catch (InterruptedException e) {
            logger.debug("ERROR: in AtomMappingTool: " + e.getMessage());
            logger.error(e);
				} catch (ExecutionException e) {
            logger.debug("ERROR: in AtomMappingTool: " + e.getMessage());
            logger.error(e);
        } finally {
            threadPool.shutdown();
        }

        return solutions;
    }

    public synchronized String getTitle() {
        return "Calculating Maximum Commmon Substrutures (MCSS) using SMSD";
    }
}
