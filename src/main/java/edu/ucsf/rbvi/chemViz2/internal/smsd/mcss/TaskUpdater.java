/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.ucsf.rbvi.chemViz2.internal.smsd.mcss;

import java.util.logging.Level;

/**
 *
 * @author Scooter
 */
public interface TaskUpdater {

    /**
     * Set the number of iterations.
     *
     * @param nIterations
     **/
    public void setTotalCount(int nIterations);

    /**
     * Update the count
     **/
    public void incrementCount();

    /**
     * Update the status message
     **/
    public void updateStatus(String status);

    /**
     * This logs an exception and provides a mechanism
     * for tools to integrate with the caller's logging mechanism
     *
     * @param className the class name of the reporter
     * @param level the level of the error
     * @param message the log message
     * @param exception the exception itself
     */
    public void logException(String className, Level level, String message, Exception exception);
}
