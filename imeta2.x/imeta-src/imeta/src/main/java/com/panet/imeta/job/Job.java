/* Copyright (c) 2007 Pentaho Corporation.  All rights reserved. 
 * This software was developed by Pentaho Corporation and is provided under the terms 
 * of the GNU Lesser General Public License, Version 2.1. You may not use 
 * this file except in compliance with the license. If you need a copy of the license, 
 * please go to http://www.gnu.org/licenses/lgpl-2.1.txt. The Original Code is Pentaho 
 * Data Integration.  The Initial Developer is Pentaho Corporation.
 *
 * Software distributed under the GNU Lesser Public License is distributed on an "AS IS" 
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or  implied. Please refer to 
 * the license for the specific language governing your rights and limitations.*/

package com.panet.imeta.job;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.vfs.FileName;
import org.apache.commons.vfs.FileObject;

import com.panet.imeta.cluster.SlaveServer;
import com.panet.imeta.core.Const;
import com.panet.imeta.core.Result;
import com.panet.imeta.core.RowMetaAndData;
import com.panet.imeta.core.database.Database;
import com.panet.imeta.core.database.DatabaseMeta;
import com.panet.imeta.core.exception.KettleDatabaseException;
import com.panet.imeta.core.exception.KettleException;
import com.panet.imeta.core.exception.KettleJobException;
import com.panet.imeta.core.exception.KettleValueException;
import com.panet.imeta.core.gui.JobTracker;
import com.panet.imeta.core.gui.OverwritePrompter;
import com.panet.imeta.core.logging.Log4jStringAppender;
import com.panet.imeta.core.logging.LogWriter;
import com.panet.imeta.core.logging.LogWriterInterface;
import com.panet.imeta.core.logging.LogWriterProxy;
import com.panet.imeta.core.parameters.NamedParams;
import com.panet.imeta.core.parameters.NamedParamsDefault;
import com.panet.imeta.core.row.ValueMeta;
import com.panet.imeta.core.variables.VariableSpace;
import com.panet.imeta.core.variables.Variables;
import com.panet.imeta.core.vfs.KettleVFS;
import com.panet.imeta.job.entries.special.JobEntrySpecial;
import com.panet.imeta.job.entry.JobEntryCopy;
import com.panet.imeta.job.entry.JobEntryInterface;
import com.panet.imeta.repository.Repository;
import com.panet.imeta.trans.StepLoader;
import com.panet.imeta.trans.Trans;
import com.panet.imeta.www.AddJobServlet;
import com.panet.imeta.www.StartJobServlet;
import com.panet.imeta.www.WebResult;

/**
 * This class executes a JobInfo object.
 * 
 * @author Matt
 * @since 07-apr-2003
 * 
 */
public class Job extends Thread implements VariableSpace {
	private LogWriterInterface log;
	private JobMeta jobMeta;
	private Repository rep;
	private int errors = 0;
	private VariableSpace variables = new Variables();

	/**
	 * The job that's launching this (sub-) job. This gives us access to the
	 * whole chain, including the parent variables, etc.
	 */
	private Job parentJob;

	/**
	 * Keep a list of the job entries that were executed.
	 */
	private JobTracker jobTracker;

	private Date startDate, endDate, currentDate, logDate, depDate;

	private boolean active, stopped;

	private long batchId;

	/**
	 * This is the batch ID that is passed from job to job to transformation, if
	 * nothing is passed, it's the job's batch id
	 */
	private long passedBatchId;

	/**
	 * The rows that were passed onto this job by a previous transformation.
	 * These rows are passed onto the first job entry in this job (on the result
	 * object)
	 */
	private List<RowMetaAndData> sourceRows;

	/**
	 * The result of the job, after execution.
	 */
	private Result result;
	private boolean initialized;
	private Log4jStringAppender stringAppender;

	private List<JobListener> jobListeners;

	private boolean finished;

	/**
	 * Parameters of the job.
	 */
	private NamedParams namedParams = new NamedParamsDefault();

	public Job(LogWriterInterface lw, String name, String file, String args[]) {
		this();
		init(lw, name, file, args);
	}

	public void init(LogWriterInterface lw, String name, String file,
			String args[]) {
		this.log = lw;

		if (name != null)
			setName(name + " (" + super.getName() + ")");

		jobMeta = new JobMeta(log);
		jobMeta.setName(name);
		jobMeta.setFilename(file);
		jobMeta.setArguments(args);
		active = false;
		stopped = false;
		jobTracker = new JobTracker(jobMeta);
		initialized = false;
		batchId = -1;
		passedBatchId = -1;

		result = null;
	}

	public Job(LogWriterInterface lw, Repository rep, JobMeta ti) {
		this(lw, null, rep, ti);
	}

	public Job(LogWriterInterface lw, StepLoader steploader, Repository rep,
			JobMeta ti) {
		this();
		open(lw, steploader, rep, ti);
		if (ti.getName() != null)
			setName(ti.getName() + " (" + super.getName() + ")");
	}

	// Empty constructor, for Class.newInstance()
	public Job() {
		jobListeners = new ArrayList<JobListener>();
	}

	public void open(LogWriterInterface lw, StepLoader steploader,
			Repository rep, JobMeta ti) {
		this.log = lw;
		this.rep = rep;
		this.jobMeta = ti;

		if (ti.getName() != null)
			setName(ti.getName() + " (" + super.getName() + ")");

		active = false;
		stopped = false;
		jobTracker = new JobTracker(jobMeta);
	}

	public void open(Repository rep, String fname, String jobname,
			String dirname, OverwritePrompter prompter) throws KettleException {
		this.rep = rep;
		if (rep != null) {
			jobMeta = new JobMeta(log, rep, jobname, rep.getDirectoryTree()
					.findDirectory(dirname));
		} else {
			jobMeta = new JobMeta(log, fname, rep, prompter);
		}

		if (jobMeta.getName() != null)
			setName(jobMeta.getName() + " (" + super.getName() + ")");
	}

	public static final Job createJobWithNewClassLoader()
			throws KettleException {
		try {
			// Load the class.
			Class<?> jobClass = Const.createNewClassLoader().loadClass(
					Job.class.getName());

			// create the class
			// Try to instantiate this one...
			Job job = (Job) jobClass.newInstance();

			// Done!
			return job;
		} catch (Exception e) {
			String message = Messages.getString(
					"Job.Log.ErrorAllocatingNewJob", e.toString());
			LogWriter.getInstance().logError("Create Job in new ClassLoader",
					message);
			LogWriter.getInstance().logError("Create Job in new ClassLoader",
					Const.getStackTracker(e));
			throw new KettleException(message, e);
		}
	}

	public String getJobname() {
		if (jobMeta == null)
			return null;

		return jobMeta.getName();
	}

	public void setRepository(Repository rep) {
		this.rep = rep;
	}

	// Threads main loop: called by Thread.start();
	public void run() {
		try {
			finished = false;
			initialized = true;

			// Create a new variable name space as we want jobs to have their
			// own set of variables.
			// initialize from parentjob or null
			//
			variables.initializeVariablesFrom(parentJob);
			setInternalKettleVariables(variables);

			// Run the job, don't fire the job listeners at the end
			//
			result = execute(false);
		} catch (Throwable je) {
			log.logError(toString(), Messages.getString("Job.Log.ErrorExecJob",
					je.getMessage()));
			log.logError(toString(), Const.getStackTracker(je));
			//
			// we don't have result object because execute() threw a curve-ball.
			// So we create a new error object.
			//
			result = new Result();
			result.setNrErrors(1L);
			result.setResult(false);
			addErrors(1); // This can be before actual execution
			active = false;
			finished = true;
			stopped = true;
		} finally {
			// Try to write the result back at the end, even if it's an
			// unexpected error
			//
			try {
				endProcessing(Database.LOG_STATUS_END, result); // $NON-NLS-1$
			} catch (KettleJobException e) {
				log.logError(toString(), Messages.getString(
						"Job.Log.ErrorExecJob", e.getMessage()));
				log.logError(toString(), Const.getStackTracker(e));
			}

			fireJobListeners();
		}
	}

	/**
	 * Execute a job without previous results. This is a job entry point (not
	 * recursive)<br>
	 * Fire the job listeners at the end.<br>
	 * <br>
	 * 
	 * @return the result of the execution
	 * 
	 * @throws KettleException
	 */
	public Result execute() throws KettleException {
		return execute(true);
	}

	/**
	 * Execute a job without previous results. This is a job entry point (not
	 * recursive)<br>
	 * <br>
	 * 
	 * @param fireJobListeners
	 *            true if the job listeners need to be fired off
	 * @return the result of the execution
	 * 
	 * @throws KettleException
	 */
	private Result execute(boolean fireJobListeners) throws KettleException {
		finished = false;

		// Start the tracking...
		JobEntryResult jerStart = new JobEntryResult(null, Messages
				.getString("Job.Comment.JobStarted"), Messages
				.getString("Job.Reason.Started"), null);
		jobTracker.addJobTracker(new JobTracker(jobMeta, jerStart));

		active = true;

		// Where do we start?
		JobEntryCopy startpoint;
		beginProcessing();
		startpoint = jobMeta.findJobEntry(JobMeta.STRING_SPECIAL, 0, false);
		if (startpoint == null) {
			throw new KettleJobException(Messages
					.getString("Job.Log.CounldNotFindStartingPoint"));
		}
		JobEntrySpecial jes = (JobEntrySpecial) startpoint.getEntry();
		Result res = null;
		boolean isFirst = true;
		while ((jes.isRepeat() || isFirst) && !isStopped()) {
			isFirst = false;
			res = execute(0, null, startpoint, null, Messages
					.getString("Job.Reason.Started"));
		}
		// Save this result...
		JobEntryResult jerEnd = new JobEntryResult(res, Messages
				.getString("Job.Comment.JobFinished"), Messages
				.getString("Job.Reason.Finished"), null);
		jobTracker.addJobTracker(new JobTracker(jobMeta, jerEnd));

		active = false;

		// Tell the world that we've finished processing this job...
		//
		if (fireJobListeners) {
			fireJobListeners();
		}

		return res;
	}

	/**
	 * Execute a job with previous results passed in.<br>
	 * <br>
	 * Execute called by JobEntryJob: don't clear the jobEntryResults.
	 * 
	 * @param nr
	 *            The job entry number
	 * @param result
	 *            the result of the previous execution
	 * @return Result of the job execution
	 * @throws KettleJobException
	 */
	public Result execute(int nr, Result result) throws KettleException {
		finished = false;

		// Where do we start?
		JobEntryCopy startpoint;

		// Perhaps there is already a list of input rows available?
		if (getSourceRows() != null) {
			result.setRows(getSourceRows());
		}

		startpoint = jobMeta.findJobEntry(JobMeta.STRING_SPECIAL_START, 0,
				false);
		if (startpoint == null) {
			throw new KettleJobException(Messages
					.getString("Job.Log.CounldNotFindStartingPoint"));
		}

		Result res = execute(nr, result, startpoint, null, Messages
				.getString("Job.Reason.StartOfJobentry"));

		// Tell the world that we've finished processing this job...
		//
		fireJobListeners();

		return res;
	}

	/**
	 * Sets the finished flag.<b> Then launch all the job listeners and call the
	 * jobFinished method for each.<br>
	 * 
	 * @see JobListener#jobFinished(Job)
	 */
	private void fireJobListeners() {
		finished = true;
		for (JobListener jobListener : jobListeners) {
			jobListener.jobFinished(this);
		}
	}

	/**
	 * Execute a job entry recursively and move to the next job entry
	 * automatically.<br>
	 * Uses a back-tracking algorithm.<br>
	 * 
	 * @param nr
	 * @param prev_result
	 * @param startpoint
	 * @param previous
	 * @param reason
	 * @return
	 * @throws KettleException
	 */
	private Result execute(final int nr, Result prev_result,
			final JobEntryCopy startpoint, JobEntryCopy previous, String reason)
			throws KettleException {
		Result res = null;

		if (stopped) {
			res = new Result(nr);
			res.stopped = true;
			return res;
		}

		if (log.isDetailed())
			log.logDetailed(toString(), "exec(" + nr + ", "
					+ (prev_result != null ? prev_result.getNrErrors() : 0)
					+ ", "
					+ (startpoint != null ? startpoint.toString() : "null")
					+ ")");

		// What entry is next?
		JobEntryInterface jei = startpoint.getEntry();

		// Track the fact that we are going to launch the next job entry...
		JobEntryResult jerBefore = new JobEntryResult(null, Messages
				.getString("Job.Comment.JobStarted"), reason, startpoint);
		jobTracker.addJobTracker(new JobTracker(jobMeta, jerBefore));

		Result prevResult = null;
		if (prev_result != null) {
			prevResult = (Result) prev_result.clone();
		} else {
			prevResult = new Result();
		}

		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(
				jei.getClass().getClassLoader());
		// Execute this entry...
		JobEntryInterface cloneJei = (JobEntryInterface) jei.clone();
		((VariableSpace) cloneJei).copyVariablesFrom(this);
		final Result result = cloneJei.execute(prevResult, nr, rep, this);

		Thread.currentThread().setContextClassLoader(cl);
		addErrors((int) result.getNrErrors());

		// Save this result as well...
		JobEntryResult jerAfter = new JobEntryResult(result, Messages
				.getString("Job.Comment.JobFinished"), null, startpoint);
		jobTracker.addJobTracker(new JobTracker(jobMeta, jerAfter));

		// Try all next job entries.
		//
		// Keep track of all the threads we fired in case of parallel
		// execution...
		// Keep track of the results of these executions too.
		//
		final List<Thread> threads = new ArrayList<Thread>();
		final List<Result> threadResults = new ArrayList<Result>();
		final List<KettleException> threadExceptions = new ArrayList<KettleException>();
		final List<JobEntryCopy> threadEntries = new ArrayList<JobEntryCopy>();

		// Launch only those where the hop indicates true or false
		//
		int nrNext = jobMeta.findNrNextJobEntries(startpoint);
		for (int i = 0; i < nrNext && !isStopped(); i++) {
			// The next entry is...
			final JobEntryCopy nextEntry = jobMeta.findNextJobEntry(startpoint,
					i);

			// See if we need to execute this...
			final JobHopMeta hi = jobMeta.findJobHop(startpoint, nextEntry);

			// The next comment...
			final String nextComment;
			if (hi.isUnconditional()) {
				nextComment = Messages
						.getString("Job.Comment.FollowedUnconditional");
			} else {
				if (result.getResult()) {
					nextComment = Messages
							.getString("Job.Comment.FollowedSuccess");
				} else {
					nextComment = Messages
							.getString("Job.Comment.FollowedFailure");
				}
			}

			// 
			// If the link is unconditional, execute the next job entry
			// (entries).
			// If the start point was an evaluation and the link color is
			// correct: green or red, execute the next job entry...
			//
			if (hi.isUnconditional()
					|| (startpoint.evaluates() && (!(hi.getEvaluation() ^ result
							.getResult())))) {
				// Start this next step!
				if (log.isBasic())
					log.logBasic(jobMeta.toString(), Messages.getString(
							"Job.Log.StartingEntry", nextEntry.getName()));

				// Pass along the previous result, perhaps the next job can use
				// it...
				// However, set the number of errors back to 0 (if it should be
				// reset)
				// When an evaluation is executed the errors e.g. should not be
				// reset.
				if (nextEntry.resetErrorsBeforeExecution()) {
					result.setNrErrors(0);
				}

				// Now execute!
				// 
				// if (we launch in parallel, fire the execution off in a new
				// thread...
				//
				if (startpoint.isLaunchingInParallel()) {
					threadEntries.add(nextEntry);

					Runnable runnable = new Runnable() {
						public void run() {
							try {
								Result threadResult = execute(nr + 1, result,
										nextEntry, startpoint, nextComment);
								threadResults.add(threadResult);
							} catch (Throwable e) {
								log.logError(toString(), Const
										.getStackTracker(e));
								threadExceptions.add(new KettleException(
										Messages.getString(
												"Job.Log.UnexpectedError",
												nextEntry.toString()), e));
								Result threadResult = new Result();
								threadResult.setResult(false);
								threadResult.setNrErrors(1L);
								threadResults.add(threadResult);
							}
						}
					};
					Thread thread = new Thread(runnable);
					threads.add(thread);
					thread.start();
					if (log.isBasic())
						log.logBasic(jobMeta.toString(), Messages.getString(
								"Job.Log.LaunchedJobEntryInParallel", nextEntry
										.getName()));
				} else {
					try {
						// Same as before: blocks until it's done
						//
						res = execute(nr + 1, result, nextEntry, startpoint,
								nextComment);
					} catch (Throwable e) {
						log.logError(toString(), Const.getStackTracker(e));
						throw new KettleException(Messages
								.getString("Job.Log.UnexpectedError", nextEntry
										.toString()), e);
					}
					if (log.isBasic())
						log.logBasic(jobMeta.toString(), Messages.getString(
								"Job.Log.FinishedJobEntry",
								nextEntry.getName(), res.getResult() + ""));
				}
			}
		}

		// OK, if we run in parallel, we need to wait for all the job entries to
		// finish...
		//
		if (startpoint.isLaunchingInParallel()) {
			for (int i = 0; i < threads.size(); i++) {
				Thread thread = threads.get(i);
				JobEntryCopy nextEntry = threadEntries.get(i);

				try {
					thread.join();
				} catch (InterruptedException e) {
					log.logError(jobMeta.toString(), Messages.getString(
							"Job.Log.UnexpectedErrorWhileWaitingForJobEntry",
							nextEntry.getName()));
					threadExceptions
							.add(new KettleException(
									Messages
											.getString(
													"Job.Log.UnexpectedErrorWhileWaitingForJobEntry",
													nextEntry.getName()), e));
				}
			}
			if (log.isBasic())
				log.logBasic(jobMeta.toString(), Messages.getString(
						"Job.Log.FinishedJobEntry", startpoint.getName()));
		}

		// Perhaps we don't have next steps??
		// In this case, return the previous result.
		if (res == null) {
			res = prevResult;
		}

		// See if there where any errors in the parallel execution
		//
		if (threadExceptions.size() > 0) {
			res.setResult(false);
			res.setNrErrors(threadExceptions.size());

			for (KettleException e : threadExceptions) {
				log.logError(jobMeta.toString(), e.getMessage(), e);
			}

			// Now throw the first Exception for good measure...
			//
			throw threadExceptions.get(0);
		}

		// In parallel execution, we aggregate all the results, simply add them
		// to the previous result...
		//
		for (Result threadResult : threadResults) {
			res.add(threadResult);
		}

		// If there have been errors, logically, we need to set the result to
		// "false"...
		//
		if (res.getNrErrors() > 0) {
			res.setResult(false);
		}

		return res;
	}

	/**
	 * Wait until this job has finished.
	 */
	public void waitUntilFinished() {
		waitUntilFinished(-1L);
	}

	/**
	 * Wait until this job has finished.
	 * 
	 * @param maxMiliseconds
	 *            the maximum number of ms to wait
	 */
	public void waitUntilFinished(long maxMiliseconds) {
		try {
			while (isAlive()) {
				Thread.sleep(0, 1);
			}
		} catch (InterruptedException e) {

		}
		/*
		 * long time = 0L; while (isAlive() && (time<maxMiliseconds ||
		 * maxMiliseconds<0)) { try { Thread.sleep(1); time+=1; }
		 * catch(InterruptedException e) {} }
		 */
	}

	/**
	 * Get the number of errors that happened in the job.
	 * 
	 * @return nr of error that have occurred during execution. During execution
	 *         of a job the number can change.
	 */
	public int getErrors() {
		return errors;
	}

	/**
	 * Set the number of occured errors to 0.
	 */
	public void resetErrors() {
		errors = 0;
	}

	/**
	 * Add a number of errors to the total number of erros that occured during
	 * execution.
	 * 
	 * @param nrToAdd
	 *            nr of errors to add.
	 */
	public void addErrors(int nrToAdd) {
		if (nrToAdd > 0) {
			errors += nrToAdd;
		}
	}

	//
	// Handle logging at start
	public boolean beginProcessing() throws KettleJobException {
		currentDate = new Date();
		logDate = new Date();
		startDate = Const.MIN_DATE;
		endDate = currentDate;

		resetErrors();
		DatabaseMeta logcon = jobMeta.getLogConnection();
		if (logcon != null && !Const.isEmpty(jobMeta.getLogTable())) {
			Database ldb = new Database(logcon);
			ldb.shareVariablesWith(this);
			try {
				boolean lockedTable = false;
				ldb.connect();

				// Enable transactions to make table locking possible
				//
				ldb.setCommit(100);

				// See if we have to add a batch id...
				Long id_batch = new Long(1);
				if (jobMeta.isBatchIdUsed()) {
					// Make sure we lock that table to avoid concurrency issues
					//
					ldb.lockTables(new String[] { jobMeta.getLogTable(), });
					lockedTable = true;

					// Now insert value -1 to create a real write lock blocking
					// the other requests.. FCFS
					//
					String sql = "INSERT INTO "
							+ logcon.quoteField(jobMeta.getLogTable()) + "("
							+ logcon.quoteField("ID_JOB") + ") values (-1)";
					ldb.execStatement(sql);

					// Now this next lookup will stall on the other connections
					//
					id_batch = ldb.getNextValue(null, jobMeta.getLogTable(),
							"ID_JOB");

					setBatchId(id_batch.longValue());
					if (getPassedBatchId() <= 0) {
						setPassedBatchId(id_batch.longValue());
					}
				}

				Object[] lastr = ldb.getLastLogDate(jobMeta.getLogTable(),
						jobMeta.getName(), true, Database.LOG_STATUS_END); // $NON-NLS-1$
				if (!Const.isEmpty(lastr)) {
					Date last;
					try {
						last = ldb.getReturnRowMeta().getDate(lastr, 0);
					} catch (KettleValueException e) {
						throw new KettleJobException(Messages.getString(
								"Job.Log.ConversionError", ""
										+ jobMeta.getLogTable()), e);
					}
					if (last != null) {
						startDate = last;
					}
				}

				depDate = currentDate;

				ldb.writeLogRecord(jobMeta.getLogTable(), jobMeta
						.isBatchIdUsed(), getBatchId(), true,
						jobMeta.getName(),
						Database.LOG_STATUS_START, // $NON-NLS-1$
						0L, 0L, 0L, 0L, 0L, 0L, startDate, endDate, logDate,
						depDate, currentDate, ((LogWriterProxy) log)
								.getOperateUser(), ((LogWriterProxy) log)
								.getIvokeType(), null);
				if (lockedTable) {

					// Remove the -1 record again...
					//
					String sql = "DELETE FROM "
							+ logcon.quoteField(jobMeta.getLogTable())
							+ " WHERE " + logcon.quoteField("ID_JOB") + "= -1";
					ldb.execStatement(sql);

					ldb.unlockTables(new String[] { jobMeta.getLogTable(), });
				}
				ldb.disconnect();
			} catch (KettleDatabaseException dbe) {
				addErrors(1); // This is even before actual execution
				throw new KettleJobException(Messages.getString(
						"Job.Log.UnableToProcessLoggingStart", ""
								+ jobMeta.getLogTable()), dbe);
			} finally {
				ldb.disconnect();
			}
		}

		if (jobMeta.isLogfieldUsed()) {
			stringAppender = LogWriter.createStringAppender();
			log.addAppender(stringAppender);
			stringAppender.setBuffer(new StringBuffer("START" + Const.CR));
		}

		return true;
	}

	//
	// Handle logging at end
	public boolean endProcessing(String status, Result res)
			throws KettleJobException {
		try {
			long read = res.getNrErrors();
			long written = res.getNrLinesWritten();
			long updated = res.getNrLinesUpdated();
			long errors = res.getNrErrors();
			long input = res.getNrLinesInput();
			long output = res.getNrLinesOutput();

			if (errors == 0 && !res.getResult())
				errors = 1;

			logDate = new Date();

			// Change the logging back to stream...
			String log_string = null;

			if (jobMeta.isLogfieldUsed()) {
				log_string = stringAppender.getBuffer().append(
						Const.CR + "END" + Const.CR).toString();
				log.removeAppender(stringAppender);
			}

			/*
			 * Sums errors, read, written, etc.
			 */

			DatabaseMeta logcon = jobMeta.getLogConnection();
			if (logcon != null) {
				Database ldb = new Database(logcon);
				ldb.shareVariablesWith(this);
				try {
					ldb.connect();
					ldb.writeLogRecord(jobMeta.getLogTable(), jobMeta
							.isBatchIdUsed(), getBatchId(), true, jobMeta
							.getName(), status, read, written, updated, input,
							output, errors, startDate, endDate, logDate,
							depDate, currentDate, ((LogWriterProxy) log)
									.getOperateUser(), ((LogWriterProxy) log)
									.getIvokeType(), log_string);
				} catch (KettleDatabaseException dbe) {
					addErrors(1);
					throw new KettleJobException(
							"Unable to end processing by writing log record to table "
									+ jobMeta.getLogTable(), dbe);
				} finally {
					ldb.disconnect();
				}
			}

			return true;
		} catch (Exception e) {
			throw new KettleJobException(e); // In case something else goes
			// wrong.
		}
	}

	public boolean isActive() {
		return active;
	}

	// Stop all activity!
	public void stopAll() {
		stopped = true;
	}

	public void setStopped(boolean stopped) {
		this.stopped = stopped;
	}

	/**
	 * @return Returns the stopped status of this Job...
	 */
	public boolean isStopped() {
		return stopped;
	}

	/**
	 * @return Returns the startDate.
	 */
	public Date getStartDate() {
		return startDate;
	}

	/**
	 * @return Returns the endDate.
	 */
	public Date getEndDate() {
		return endDate;
	}

	/**
	 * @return Returns the currentDate.
	 */
	public Date getCurrentDate() {
		return currentDate;
	}

	/**
	 * @return Returns the depDate.
	 */
	public Date getDepDate() {
		return depDate;
	}

	/**
	 * @return Returns the logDate.
	 */
	public Date getLogDate() {
		return logDate;
	}

	/**
	 * @return Returns the jobinfo.
	 */
	public JobMeta getJobMeta() {
		return jobMeta;
	}

	/**
	 * @return Returns the log.
	 */
	public LogWriterInterface getLog() {
		return log;
	}

	/**
	 * @return Returns the rep.
	 */
	public Repository getRep() {
		return rep;
	}

	public Thread getThread() {
		return (Thread) this;
	}

	/**
	 * @return Returns the jobTracker.
	 */
	public JobTracker getJobTracker() {
		return jobTracker;
	}

	/**
	 * @param jobTracker
	 *            The jobTracker to set.
	 */
	public void setJobTracker(JobTracker jobTracker) {
		this.jobTracker = jobTracker;
	}

	public void setSourceRows(List<RowMetaAndData> sourceRows) {
		this.sourceRows = sourceRows;
	}

	public List<RowMetaAndData> getSourceRows() {
		return sourceRows;
	}

	/**
	 * @return Returns the parentJob.
	 */
	public Job getParentJob() {
		return parentJob;
	}

	/**
	 * @param parentJob
	 *            The parentJob to set.
	 */
	public void setParentJob(Job parentJob) {
		this.parentJob = parentJob;
	}

	public Result getResult() {
		return result;
	}

	public void setResult(Result result) {
		this.result = result;
	}

	/**
	 * @return Returns the initialized.
	 */
	public boolean isInitialized() {
		return initialized;
	}

	/**
	 * @return Returns the batchId.
	 */
	public long getBatchId() {
		return batchId;
	}

	/**
	 * @param batchId
	 *            The batchId to set.
	 */
	public void setBatchId(long batchId) {
		this.batchId = batchId;
	}

	/**
	 * @return the jobBatchId
	 */
	public long getPassedBatchId() {
		return passedBatchId;
	}

	/**
	 * @param jobBatchId
	 *            the jobBatchId to set
	 */
	public void setPassedBatchId(long jobBatchId) {
		this.passedBatchId = jobBatchId;
	}

	public void setInternalKettleVariables(VariableSpace var) {
		if (jobMeta != null && jobMeta.getFilename() != null) // we have a
		// finename
		// that's
		// defined.
		{
			try {
				FileObject fileObject = KettleVFS.getFileObject(jobMeta
						.getFilename());
				FileName fileName = fileObject.getName();

				// The filename of the transformation
				var.setVariable(Const.INTERNAL_VARIABLE_JOB_FILENAME_NAME,
						fileName.getBaseName());

				// The directory of the transformation
				FileName fileDir = fileName.getParent();
				var.setVariable(Const.INTERNAL_VARIABLE_JOB_FILENAME_DIRECTORY,
						fileDir.getURI());
			} catch (IOException e) {
				var.setVariable(Const.INTERNAL_VARIABLE_JOB_FILENAME_DIRECTORY,
						"");
				var.setVariable(Const.INTERNAL_VARIABLE_JOB_FILENAME_NAME, "");
			}
		} else {
			var.setVariable(Const.INTERNAL_VARIABLE_JOB_FILENAME_DIRECTORY, ""); //$NON-NLS-1$
			var.setVariable(Const.INTERNAL_VARIABLE_JOB_FILENAME_NAME, ""); //$NON-NLS-1$
		}

		// The name of the job
		var.setVariable(Const.INTERNAL_VARIABLE_JOB_NAME, Const.NVL(jobMeta
				.getName(), "")); //$NON-NLS-1$

		// The name of the directory in the repository
		var.setVariable(Const.INTERNAL_VARIABLE_JOB_REPOSITORY_DIRECTORY,
				jobMeta.getDirectory() != null ? jobMeta.getDirectory()
						.getPath() : ""); //$NON-NLS-1$

		// Undefine the transformation specific variables
		var
				.setVariable(
						Const.INTERNAL_VARIABLE_TRANSFORMATION_FILENAME_DIRECTORY,
						null);
		var.setVariable(Const.INTERNAL_VARIABLE_TRANSFORMATION_FILENAME_NAME,
				null);
		var
				.setVariable(
						Const.INTERNAL_VARIABLE_TRANSFORMATION_FILENAME_DIRECTORY,
						null);
		var.setVariable(Const.INTERNAL_VARIABLE_TRANSFORMATION_FILENAME_NAME,
				null);
		var.setVariable(Const.INTERNAL_VARIABLE_TRANSFORMATION_NAME, null);
		var.setVariable(
				Const.INTERNAL_VARIABLE_TRANSFORMATION_REPOSITORY_DIRECTORY,
				null);
	}

	public void copyVariablesFrom(VariableSpace space) {
		variables.copyVariablesFrom(space);
	}

	public String environmentSubstitute(String aString) {
		return variables.environmentSubstitute(aString);
	}

	public String[] environmentSubstitute(String aString[]) {
		return variables.environmentSubstitute(aString);
	}

	public VariableSpace getParentVariableSpace() {
		return variables.getParentVariableSpace();
	}

	public void setParentVariableSpace(VariableSpace parent) {
		variables.setParentVariableSpace(parent);
	}

	public String getVariable(String variableName, String defaultValue) {
		return variables.getVariable(variableName, defaultValue);
	}

	public String getVariable(String variableName) {
		return variables.getVariable(variableName);
	}

	public boolean getBooleanValueOfVariable(String variableName,
			boolean defaultValue) {
		if (!Const.isEmpty(variableName)) {
			String value = environmentSubstitute(variableName);
			if (!Const.isEmpty(value)) {
				return ValueMeta.convertStringToBoolean(value);
			}
		}
		return defaultValue;
	}

	public void initializeVariablesFrom(VariableSpace parent) {
		variables.initializeVariablesFrom(parent);
	}

	public String[] listVariables() {
		return variables.listVariables();
	}

	public void setVariable(String variableName, String variableValue) {
		variables.setVariable(variableName, variableValue);
	}

	public void shareVariablesWith(VariableSpace space) {
		variables = space;
	}

	public void injectVariables(Map<String, String> prop) {
		variables.injectVariables(prop);
	}

	public String getStatus() {
		String message;

		if (!initialized) {
			message = Trans.STRING_WAITING;
		} else {
			if (active) {
				if (stopped) {
					message = Trans.STRING_STOPPED;
				} else {
					message = Trans.STRING_RUNNING;
				}
			} else {
				message = Trans.STRING_FINISHED;
				if (result != null && result.getNrErrors() > 0) {
					message += " (with errors)";
				}
			}
		}

		return message;
	}

	public static void sendXMLToSlaveServer(JobMeta jobMeta,
			JobExecutionConfiguration executionConfiguration)
			throws KettleException {
		SlaveServer slaveServer = executionConfiguration.getRemoteServer();

		if (slaveServer == null)
			throw new KettleException(Messages
					.getString("Job.Log.NoSlaveServerSpecified"));
		if (Const.isEmpty(jobMeta.getName()))
			throw new KettleException(Messages
					.getString("Job.Log.UniqueJobName"));

		try {
			// Inject certain internal variables to make it more intuitive.
			// 
			for (String var : Const.INTERNAL_TRANS_VARIABLES)
				executionConfiguration.getVariables().put(var,
						jobMeta.getVariable(var));
			for (String var : Const.INTERNAL_JOB_VARIABLES)
				executionConfiguration.getVariables().put(var,
						jobMeta.getVariable(var));

			String xml = new JobConfiguration(jobMeta, executionConfiguration)
					.getXML();

			String reply = slaveServer.sendXML(xml, AddJobServlet.CONTEXT_PATH
					+ "/?xml=Y");
			WebResult webResult = WebResult.fromXMLString(reply);
			if (!webResult.getResult().equalsIgnoreCase(WebResult.STRING_OK)) {
				throw new KettleException(
						"There was an error posting the job on the remote server: "
								+ Const.CR + webResult.getMessage());
			}

			reply = slaveServer
					.getContentFromServer(StartJobServlet.CONTEXT_PATH
							+ "/?name="
							+ URLEncoder.encode(jobMeta.getName(), "UTF-8")
							+ "&xml=Y");
			webResult = WebResult.fromXMLString(reply);
			if (!webResult.getResult().equalsIgnoreCase(WebResult.STRING_OK)) {
				throw new KettleException(
						"There was an error starting the job on the remote server: "
								+ Const.CR + webResult.getMessage());
			}
		} catch (Exception e) {
			throw new KettleException(e);
		}
	}

	/**
	 * @return the jobListeners
	 */
	public List<JobListener> getJobListeners() {
		return jobListeners;
	}

	/**
	 * @param jobListeners
	 *            the jobListeners to set
	 */
	public void setJobListeners(List<JobListener> jobListeners) {
		this.jobListeners = jobListeners;
	}

	/**
	 * Add a job listener to the job
	 * 
	 * @param jobListener
	 *            the job listener to add
	 */
	public void addJobListener(JobListener jobListener) {
		jobListeners.add(jobListener);
	}

	/**
	 * @return the finished
	 */
	public boolean isFinished() {
		return finished;
	}

	/**
	 * @param finished
	 *            the finished to set
	 */
	public void setFinished(boolean finished) {
		this.finished = finished;
		if (finished) {
			System.out
					.println("--------------------- Finished ---------------------------");
		}
	}

	public void addParameterDefinition(String key, String description) {
		namedParams.addParameterDefinition(key, description);
	}

	public String getParameterDescription(String key) {
		return namedParams.getParameterDescription(key);
	}

	public String getParameterValue(String key) {
		return namedParams.getParameterValue(key);
	}

	public String[] listParameters() {
		return namedParams.listParameters();
	}

	public void setParameterValue(String key, String value) {
		namedParams.setParameterValue(key, value);
	}

	public void clearValues() {
		namedParams.clearValues();
	}

}
