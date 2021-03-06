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

package com.panet.imeta.job.entries.trans;

import static com.panet.imeta.job.entry.validator.AndValidator.putValidators;
import static com.panet.imeta.job.entry.validator.JobEntryValidatorUtils.andValidator;
import static com.panet.imeta.job.entry.validator.JobEntryValidatorUtils.notBlankValidator;
import static com.panet.imeta.job.entry.validator.JobEntryValidatorUtils.notNullValidator;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Node;

import com.panet.imeta.cluster.SlaveServer;
import com.panet.imeta.core.CheckResultInterface;
import com.panet.imeta.core.Const;
import com.panet.imeta.core.Result;
import com.panet.imeta.core.ResultFile;
import com.panet.imeta.core.RowMetaAndData;
import com.panet.imeta.core.SQLStatement;
import com.panet.imeta.core.database.Database;
import com.panet.imeta.core.database.DatabaseMeta;
import com.panet.imeta.core.exception.KettleDatabaseException;
import com.panet.imeta.core.exception.KettleException;
import com.panet.imeta.core.exception.KettleJobException;
import com.panet.imeta.core.exception.KettleXMLException;
import com.panet.imeta.core.logging.Log4jFileAppender;
import com.panet.imeta.core.logging.LogWriter;
import com.panet.imeta.core.parameters.NamedParams;
import com.panet.imeta.core.parameters.NamedParamsDefault;
import com.panet.imeta.core.variables.VariableSpace;
import com.panet.imeta.core.vfs.KettleVFS;
import com.panet.imeta.core.xml.XMLHandler;
import com.panet.imeta.job.Job;
import com.panet.imeta.job.JobEntryCategory;
import com.panet.imeta.job.JobEntryType;
import com.panet.imeta.job.JobMeta;
import com.panet.imeta.job.entry.JobEntryBase;
import com.panet.imeta.job.entry.JobEntryInterface;
import com.panet.imeta.repository.Repository;
import com.panet.imeta.resource.ResourceDefinition;
import com.panet.imeta.resource.ResourceEntry;
import com.panet.imeta.resource.ResourceNamingInterface;
import com.panet.imeta.resource.ResourceReference;
import com.panet.imeta.resource.ResourceEntry.ResourceType;
import com.panet.imeta.shared.SharedObjectInterface;
import com.panet.imeta.trans.Trans;
import com.panet.imeta.trans.TransExecutionConfiguration;
import com.panet.imeta.trans.TransMeta;
import com.panet.imeta.trans.cluster.TransSplitter;
import com.panet.imeta.www.SlaveServerTransStatus;

/**
 * This is the job entry that defines a transformation to be run.
 * 
 * @author Matt
 * @since 1-10-2003, rewritten on 18-06-2004
 * 
 */
@com.panet.imeta.core.annotations.Job(image = "TRN.png", id = "TRANS", type = JobEntryType.TRANS, tooltip = "JobEntry.Trans.Tooltip", category = JobEntryCategory.CATEGORY_GENERAL)
public class JobEntryTrans extends JobEntryBase implements Cloneable,
		JobEntryInterface {
	private String transname;
	private String filename;
	private String directory;

	public String arguments[];
	public boolean argFromPrevious;
	public boolean paramsFromPrevious;
	public boolean execPerRow;

	public String parameters[];
	public String parameterFieldNames[];
	public String parameterValues[];

	public boolean clearResultRows;
	public boolean clearResultFiles;

	public boolean setLogfile;
	public boolean setAppendLogfile;
	public String logfile, logext;
	public boolean addDate, addTime;
	public int loglevel;

	private String directoryPath;

	private boolean clustering;

	public boolean waitingToFinish = true;
	public boolean followingAbortRemotely;

	private String remoteSlaveServerName;
	public boolean passingAllParameters = true;

	public JobEntryTrans(String name) {
		super(name, "");
		setJobEntryType(JobEntryType.TRANS);
	}

	public JobEntryTrans() {
		this("");
		clear();
	}

	public Object clone() {
		JobEntryTrans je = (JobEntryTrans) super.clone();
		return je;
	}

	public JobEntryTrans(JobEntryBase jeb) {
		super(jeb);
	}

	public void setFileName(String n) {
		filename = n;
	}

	/**
	 * @deprecated use getFilename() instead
	 * @return the filename
	 */
	public String getFileName() {
		return filename;
	}

	public String getFilename() {
		return filename;
	}

	public String getRealFilename() {
		return environmentSubstitute(getFilename());
	}

	public void setTransname(String transname) {
		this.transname = transname;
	}

	public String getTransname() {
		return transname;
	}

	public String getDirectory() {
		return directory;
	}

	public void setDirectory(String directory) {
		this.directory = directory;
	}

	public String getLogFilename() {
		String retval = "";
		if (setLogfile) {
			retval += logfile;
			Calendar cal = Calendar.getInstance();
			if (addDate) {
				SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
				retval += "_" + sdf.format(cal.getTime());
			}
			if (addTime) {
				SimpleDateFormat sdf = new SimpleDateFormat("HHmmss");
				retval += "_" + sdf.format(cal.getTime());
			}
			if (logext != null && logext.length() > 0) {
				retval += "." + logext;
			}
		}
		return retval;
	}

	public String getXML() {
		StringBuffer retval = new StringBuffer(300);

		retval.append(super.getXML());

		retval.append("      ").append(
				XMLHandler.addTagValue("filename", filename));
		retval.append("      ").append(
				XMLHandler.addTagValue("transname", transname));
		if (directory != null) {
			retval.append("      ").append(
					XMLHandler.addTagValue("directory", directory));
		} else if (directoryPath != null) {
			retval.append("      ").append(
					XMLHandler.addTagValue("directory", directoryPath)); // don't
			// loose
			// this
			// info
			// (backup/recovery)
		}
		retval.append("      ").append(
				XMLHandler.addTagValue("arg_from_previous", argFromPrevious));
		retval.append("      ").append(
				XMLHandler.addTagValue("params_from_previous",
						paramsFromPrevious));
		retval.append("      ").append(
				XMLHandler.addTagValue("exec_per_row", execPerRow));
		retval.append("      ").append(
				XMLHandler.addTagValue("clear_rows", clearResultRows));
		retval.append("      ").append(
				XMLHandler.addTagValue("clear_files", clearResultFiles));
		retval.append("      ").append(
				XMLHandler.addTagValue("set_logfile", setLogfile));
		retval.append("      ").append(
				XMLHandler.addTagValue("logfile", logfile));
		retval.append("      ")
				.append(XMLHandler.addTagValue("logext", logext));
		retval.append("      ").append(
				XMLHandler.addTagValue("add_date", addDate));
		retval.append("      ").append(
				XMLHandler.addTagValue("add_time", addTime));
		retval.append("      ").append(
				XMLHandler.addTagValue("loglevel", LogWriter
						.getLogLevelDesc(loglevel)));
		retval.append("      ").append(
				XMLHandler.addTagValue("cluster", clustering));
		retval.append("      ").append(
				XMLHandler.addTagValue("slave_server_name",
						remoteSlaveServerName));
		retval.append("      ").append(
				XMLHandler.addTagValue("set_append_logfile", setAppendLogfile));
		retval.append("      ").append(
				XMLHandler.addTagValue("wait_until_finished", waitingToFinish));
		retval.append("      ").append(
				XMLHandler.addTagValue("follow_abort_remote",
						followingAbortRemotely));

		if (arguments != null)
			for (int i = 0; i < arguments.length; i++) {
				retval.append("      ").append(
						XMLHandler.addTagValue("argument" + i, arguments[i]));
			}

		if (parameters != null) {
			retval.append("      ").append(XMLHandler.openTag("parameters"));

			retval.append("        ").append(
					XMLHandler.addTagValue("pass_all_parameters",
							passingAllParameters));

			for (int i = 0; i < parameters.length; i++) {
				// This is a better way of making the XML file than the
				// arguments.
				retval.append("            ").append(
						XMLHandler.openTag("parameter"));

				retval.append("            ").append(
						XMLHandler.addTagValue("name", parameters[i]));
				retval.append("            ").append(
						XMLHandler.addTagValue("stream_name",
								parameterFieldNames[i]));
				retval.append("            ").append(
						XMLHandler.addTagValue("value", parameterValues[i]));

				retval.append("            ").append(
						XMLHandler.closeTag("parameter"));
			}
			retval.append("      ").append(XMLHandler.closeTag("parameters"));
		}

		return retval.toString();
	}

	public void loadXML(Node entrynode, List<DatabaseMeta> databases,
			List<SlaveServer> slaveServers, Repository rep)
			throws KettleXMLException {
		try {
			super.loadXML(entrynode, databases, slaveServers);

			filename = XMLHandler.getTagValue(entrynode, "filename");
			transname = XMLHandler.getTagValue(entrynode, "transname");

			directory = XMLHandler.getTagValue(entrynode, "directory");

			argFromPrevious = "Y".equalsIgnoreCase(XMLHandler.getTagValue(
					entrynode, "arg_from_previous"));
			paramsFromPrevious = "Y".equalsIgnoreCase(XMLHandler.getTagValue(
					entrynode, "params_from_previous"));
			execPerRow = "Y".equalsIgnoreCase(XMLHandler.getTagValue(entrynode,
					"exec_per_row"));
			clearResultRows = "Y".equalsIgnoreCase(XMLHandler.getTagValue(
					entrynode, "clear_rows"));
			clearResultFiles = "Y".equalsIgnoreCase(XMLHandler.getTagValue(
					entrynode, "clear_files"));
			setLogfile = "Y".equalsIgnoreCase(XMLHandler.getTagValue(entrynode,
					"set_logfile"));
			addDate = "Y".equalsIgnoreCase(XMLHandler.getTagValue(entrynode,
					"add_date"));
			addTime = "Y".equalsIgnoreCase(XMLHandler.getTagValue(entrynode,
					"add_time"));
			logfile = XMLHandler.getTagValue(entrynode, "logfile");
			logext = XMLHandler.getTagValue(entrynode, "logext");
			loglevel = LogWriter.getLogLevel(XMLHandler.getTagValue(entrynode,
					"loglevel"));
			clustering = "Y".equalsIgnoreCase(XMLHandler.getTagValue(entrynode,
					"cluster"));

			remoteSlaveServerName = XMLHandler.getTagValue(entrynode,
					"slave_server_name");

			setAppendLogfile = "Y".equalsIgnoreCase(XMLHandler.getTagValue(
					entrynode, "set_append_logfile"));
			String wait = XMLHandler.getTagValue(entrynode,
					"wait_until_finished");
			if (Const.isEmpty(wait))
				waitingToFinish = true;
			else
				waitingToFinish = "Y".equalsIgnoreCase(wait);

			followingAbortRemotely = "Y".equalsIgnoreCase(XMLHandler
					.getTagValue(entrynode, "follow_abort_remote"));

			// How many arguments?
			int argnr = 0;
			while (XMLHandler.getTagValue(entrynode, "argument" + argnr) != null)
				argnr++;
			arguments = new String[argnr];

			// Read them all...
			for (int a = 0; a < argnr; a++) {
				arguments[a] = XMLHandler
						.getTagValue(entrynode, "argument" + a);
			}

			Node parametersNode = XMLHandler
					.getSubNode(entrynode, "parameters"); //$NON-NLS-1$

			String passAll = XMLHandler.getTagValue(parametersNode,
					"pass_all_parameters");
			passingAllParameters = Const.isEmpty(passAll)
					|| "Y".equalsIgnoreCase(passAll);

			int nrParameters = XMLHandler.countNodes(parametersNode,
					"parameter"); //$NON-NLS-1$

			parameters = new String[nrParameters];
			parameterFieldNames = new String[nrParameters];
			parameterValues = new String[nrParameters];

			for (int i = 0; i < nrParameters; i++) {
				Node knode = XMLHandler.getSubNodeByNr(parametersNode,
						"parameter", i); //$NON-NLS-1$

				parameters[i] = XMLHandler.getTagValue(knode, "name"); //$NON-NLS-1$
				parameterFieldNames[i] = XMLHandler.getTagValue(knode,
						"stream_name"); //$NON-NLS-1$
				parameterValues[i] = XMLHandler.getTagValue(knode, "value"); //$NON-NLS-1$
			}
		} catch (KettleException e) {
			throw new KettleXMLException(
					"Unable to load job entry of type 'trans' from XML node", e);
		}
	}

	@Override
	public void setInfo(Map<String, String[]> p, String id,
			List<? extends SharedObjectInterface> databases) {
		transname = JobEntryBase.parameterToString(p.get(id + ".transname"));
		directory = JobEntryBase.parameterToString(p.get(id + ".directory"));
		filename = JobEntryBase.parameterToString(p.get(id + ".filename"));
		argFromPrevious = JobEntryBase.parameterToBoolean(p.get(id
				+ ".argFromPrevious"));
		paramsFromPrevious = JobEntryBase.parameterToBoolean(p.get(id
				+ ".paramsFromPrevious"));
		execPerRow = JobEntryBase.parameterToBoolean(p.get(id + ".execPerRow"));
		clearResultRows = JobEntryBase.parameterToBoolean(p.get(id
				+ ".clearResultRows"));
		clearResultFiles = JobEntryBase.parameterToBoolean(p.get(id
				+ ".clearResultFiles"));
		setLogfile = JobEntryBase.parameterToBoolean(p.get(id + ".setLogfile"));
		addDate = JobEntryBase.parameterToBoolean(p.get(id + ".addDate"));
		addTime = JobEntryBase.parameterToBoolean(p.get(id + ".addTime"));
		logfile = JobEntryBase.parameterToString(p.get(id + ".logfile"));
		logext = JobEntryBase.parameterToString(p.get(id + ".logext"));
		loglevel = JobEntryBase.parameterToInt(p.get(id + ".loglevel"));
		clustering = JobEntryBase.parameterToBoolean(p.get(id + ".clustering"));
		remoteSlaveServerName = JobEntryBase.parameterToString(p.get(id
				+ ".remoteSlaveServerName"));
		setAppendLogfile = JobEntryBase.parameterToBoolean(p.get(id
				+ ".setAppendLogfile"));
		waitingToFinish = JobEntryBase.parameterToBoolean(p.get(id
				+ ".waitingToFinish"));
		followingAbortRemotely = JobEntryBase.parameterToBoolean(p.get(id
				+ ".followingAbortRemotely"));

		String[] arguments = p.get(id + "_fields.arguments");

		this.arguments = (arguments != null) ? arguments : (new String[0]);

		String[] parameters = p.get(id + "_parameters.parameterNames");
		String[] parameterFieldNames = p.get(id
				+ "_parameters.valuesFromResult");
		String[] parameterValues = p.get(id + "_parameters.values");

		this.parameters = (parameters != null) ? parameters : (new String[0]);
		this.parameterFieldNames = (parameterFieldNames != null) ? parameterFieldNames
				: (new String[0]);
		this.parameterValues = (parameterValues != null) ? parameterValues
				: (new String[0]);
	}

	// Load the jobentry from repository
	public void loadRep(Repository rep, long id_jobentry,
			List<DatabaseMeta> databases, List<SlaveServer> slaveServers)
			throws KettleException {
		try {
			super.loadRep(rep, id_jobentry, databases, slaveServers);

			transname = rep.getJobEntryAttributeString(id_jobentry, "name");
			directory = rep.getJobEntryAttributeString(id_jobentry, "dir_path");
			filename = rep.getJobEntryAttributeString(id_jobentry, "file_name");
			argFromPrevious = rep.getJobEntryAttributeBoolean(id_jobentry,
					"arg_from_previous");
			paramsFromPrevious = rep.getJobEntryAttributeBoolean(id_jobentry,
					"params_from_previous");
			execPerRow = rep.getJobEntryAttributeBoolean(id_jobentry,
					"exec_per_row");
			clearResultRows = rep.getJobEntryAttributeBoolean(id_jobentry,
					"clear_rows", true);
			clearResultFiles = rep.getJobEntryAttributeBoolean(id_jobentry,
					"clear_files", true);
			setLogfile = rep.getJobEntryAttributeBoolean(id_jobentry,
					"set_logfile");
			addDate = rep.getJobEntryAttributeBoolean(id_jobentry, "add_date");
			addTime = rep.getJobEntryAttributeBoolean(id_jobentry, "add_time");
			logfile = rep.getJobEntryAttributeString(id_jobentry, "logfile");
			logext = rep.getJobEntryAttributeString(id_jobentry, "logext");
			loglevel = LogWriter.getLogLevel(rep.getJobEntryAttributeString(
					id_jobentry, "loglevel"));
			clustering = rep
					.getJobEntryAttributeBoolean(id_jobentry, "cluster");

			remoteSlaveServerName = rep.getJobEntryAttributeString(id_jobentry,
					"slave_server_name");
			setAppendLogfile = rep.getJobEntryAttributeBoolean(id_jobentry,
					"set_append_logfile");
			waitingToFinish = rep.getJobEntryAttributeBoolean(id_jobentry,
					"wait_until_finished", true);
			followingAbortRemotely = rep.getJobEntryAttributeBoolean(
					id_jobentry, "follow_abort_remote");

			// How many arguments?
			int argnr = rep.countNrJobEntryAttributes(id_jobentry, "argument");
			arguments = new String[argnr];

			// Read them all...
			for (int a = 0; a < argnr; a++) {
				arguments[a] = rep.getJobEntryAttributeString(id_jobentry, a,
						"argument");
			}

			// How many arguments?
			int parameternr = rep.countNrJobEntryAttributes(id_jobentry,
					"parameter_name");
			parameters = new String[parameternr];
			parameterFieldNames = new String[parameternr];
			parameterValues = new String[parameternr];

			// Read all parameters ...
			for (int a = 0; a < parameternr; a++) {
				parameters[a] = rep.getJobEntryAttributeString(id_jobentry, a,
						"parameter_name");
				parameterFieldNames[a] = rep.getJobEntryAttributeString(
						id_jobentry, a, "parameter_stream_name");
				parameterValues[a] = rep.getJobEntryAttributeString(
						id_jobentry, a, "parameter_value");
			}

			passingAllParameters = rep.getJobEntryAttributeBoolean(id_jobentry,
					"pass_all_parameters", true);

		} catch (KettleDatabaseException dbe) {
			throw new KettleException(
					"Unable to load job entry of type 'trans' from the repository for id_jobentry="
							+ id_jobentry, dbe);
		}
	}

	// Save the attributes of this job entry
	//
	public void saveRep(Repository rep, long id_job) throws KettleException {
		try {
			super.saveRep(rep, id_job);

			// Removed id_transformation as we do not know what it is if we are
			// using variables in the path
			// long id_transformation = rep.getTransformationID(transname,
			// directory.getID());
			// rep.saveJobEntryAttribute(id_job, getID(), "id_transformation",
			// id_transformation);
			rep.saveJobEntryAttribute(id_job, getID(), "name", getTransname());
			rep.saveJobEntryAttribute(id_job, getID(), "dir_path",
					getDirectory() != null ? getDirectory() : "");
			rep.saveJobEntryAttribute(id_job, getID(), "file_name", filename);
			rep.saveJobEntryAttribute(id_job, getID(), "arg_from_previous",
					argFromPrevious);
			rep.saveJobEntryAttribute(id_job, getID(), "params_from_previous",
					paramsFromPrevious);
			rep.saveJobEntryAttribute(id_job, getID(), "exec_per_row",
					execPerRow);
			rep.saveJobEntryAttribute(id_job, getID(), "clear_rows",
					clearResultRows);
			rep.saveJobEntryAttribute(id_job, getID(), "clear_files",
					clearResultFiles);
			rep.saveJobEntryAttribute(id_job, getID(), "set_logfile",
					setLogfile);
			rep.saveJobEntryAttribute(id_job, getID(), "add_date", addDate);
			rep.saveJobEntryAttribute(id_job, getID(), "add_time", addTime);
			rep.saveJobEntryAttribute(id_job, getID(), "logfile", logfile);
			rep.saveJobEntryAttribute(id_job, getID(), "logext", logext);
			rep.saveJobEntryAttribute(id_job, getID(), "loglevel", LogWriter
					.getLogLevelDesc(loglevel));
			rep.saveJobEntryAttribute(id_job, getID(), "cluster", clustering);
			rep.saveJobEntryAttribute(id_job, getID(), "slave_server_name",
					remoteSlaveServerName);
			rep.saveJobEntryAttribute(id_job, getID(), "set_append_logfile",
					setAppendLogfile);
			rep.saveJobEntryAttribute(id_job, getID(), "wait_until_finished",
					waitingToFinish);
			rep.saveJobEntryAttribute(id_job, getID(), "follow_abort_remote",
					followingAbortRemotely);

			// save the arguments...
			if (arguments != null) {
				for (int i = 0; i < arguments.length; i++) {
					rep.saveJobEntryAttribute(id_job, getID(), i, "argument",
							arguments[i]);
				}
			}

			// Save the parameters...
			if (parameters != null) {
				for (int i = 0; i < parameters.length; i++) {
					rep.saveJobEntryAttribute(id_job, getID(), i,
							"parameter_name", parameters[i]);
					rep.saveJobEntryAttribute(id_job, getID(), i,
							"parameter_stream_name", Const.NVL(
									parameterFieldNames[i], ""));
					rep.saveJobEntryAttribute(id_job, getID(), i,
							"parameter_value", Const
									.NVL(parameterValues[i], ""));
				}
			}

			rep.saveJobEntryAttribute(id_job, getID(), "pass_all_parameters",
					passingAllParameters);

		} catch (KettleDatabaseException dbe) {
			throw new KettleException(
					"Unable to save job entry of type 'trans' to the repository for id_job="
							+ id_job, dbe);
		}
	}

	public void clear() {
		super.clear();

		transname = null;
		filename = null;
		directory = null;
		arguments = null;
		parameters = null;
		argFromPrevious = false;
		paramsFromPrevious = false;
		execPerRow = false;
		addDate = false;
		addTime = false;
		logfile = null;
		logext = null;
		setLogfile = false;
		clearResultRows = false;
		clearResultFiles = false;
		remoteSlaveServerName = null;
		setAppendLogfile = false;
		waitingToFinish = true;
		followingAbortRemotely = false; // backward compatibility reasons
	}

	/**
	 * Execute this job entry and return the result. In this case it means, just
	 * set the result boolean in the Result class.
	 * 
	 * @param result
	 *            The result of the previous execution
	 * @param nr
	 *            the job entry number
	 * @param rep
	 *            the repository connection to use
	 * @param parentJob
	 *            the parent job
	 * @return The Result of the execution.
	 */
	public Result execute(Result result, int nr, Repository rep, Job parentJob)
			throws KettleException {
		LogWriter log = LogWriter.getInstance();
		result.setEntryNr(nr);

		Log4jFileAppender appender = null;
		int backupLogLevel = log.getLogLevel();
		if (setLogfile) {
			try {
				appender = LogWriter.createFileAppender(
						environmentSubstitute(getLogFilename()), true,
						setAppendLogfile);
			} catch (KettleException e) {
				log.logError(toString(), Messages.getString(
						"JobTrans.Error.UnableOpenAppender", getLogFilename(),
						e.toString()));

				log.logError(toString(), Const.getStackTracker(e));
				result.setNrErrors(1);
				result.setResult(false);
				return result;
			}
			log.addAppender(appender);
			log.setLogLevel(loglevel);
		}

		// Figure out the remote slave server...
		//
		SlaveServer remoteSlaveServer = null;
		if (!Const.isEmpty(remoteSlaveServerName)) {
			String realRemoteSlaveServerName = environmentSubstitute(remoteSlaveServerName);
			remoteSlaveServer = parentJob.getJobMeta().findSlaveServer(
					realRemoteSlaveServerName);
			if (remoteSlaveServer == null) {
				throw new KettleException(Messages.getString(
						"JobTrans.Exception.UnableToFindRemoteSlaveServer",
						realRemoteSlaveServerName));
			}
		}

		// Open the transformation...
		// Default directory for now...
		// XXX: This seems a bit odd here. These three log messages all work off
		// of getFilename(). Why are there three?
		if (log.isDetailed())
			log.logDetailed(toString(), Messages.getString(
					"JobTrans.Log.OpeningFile",
					environmentSubstitute(getFilename())));
		if (!Const.isEmpty(getFilename())) {
			if (log.isDetailed())
				log.logDetailed(toString(), Messages.getString(
						"JobTrans.Log.OpeningTrans",
						environmentSubstitute(getFilename())));
		} else {
			if (log.isDetailed())
				log.logDetailed(toString(), Messages.getString(
						"JobTrans.Log.OpeningTransInDirec",
						environmentSubstitute(getFilename()),
						environmentSubstitute(directory)));
		}

		// Load the transformation only once for the complete loop!
		// Throws an exception if it was not possible to load the
		// transformation. For example, the XML file doesn't exist or the
		// repository is down.
		// Log the stack trace and return an error condition from this
		//
		TransMeta transMeta = getTransMeta(rep);

		int iteration = 0;
		String args1[] = arguments;
		if (args1 == null || args1.length == 0) // No arguments set, look at the
		// parent job.
		{
			args1 = parentJob.getJobMeta().getArguments();
		}
		// initializeVariablesFrom(parentJob);

		//
		// For the moment only do variable translation at the start of a job,
		// not
		// for every input row (if that would be switched on). This is for
		// safety,
		// the real argument setting is later on.
		//
		String args[] = null;
		if (args1 != null) {
			args = new String[args1.length];
			for (int idx = 0; idx < args1.length; idx++) {
				args[idx] = environmentSubstitute(args1[idx]);
			}
		}

		RowMetaAndData resultRow = null;
		boolean first = true;
		List<RowMetaAndData> rows = new ArrayList<RowMetaAndData>(result
				.getRows());

		while ((first && !execPerRow)
				|| (execPerRow && rows != null && iteration < rows.size() && result
						.getNrErrors() == 0) && !parentJob.isStopped()) {
			if (execPerRow) {
				result.getRows().clear(); // Otherwise we double the amount of
				// rows every iteration in the
				// simple cases.
			}

			if (rows != null && execPerRow) {
				resultRow = rows.get(iteration);
			} else {
				resultRow = null;
			}

			NamedParams namedParam = new NamedParamsDefault();
			if (parameters != null && parameters.length > 0) {
				for (int idx = 0; idx < parameters.length; idx++) {
					if (!Const.isEmpty(parameters[idx])) {
						// We have a parameter
						namedParam.addParameterDefinition(parameters[idx],
								"Job entry runtime");
						if (Const.isEmpty(Const.trim(parameterFieldNames[idx]))) {
							// There is no field name specified.
							String value = Const
									.NVL(
											environmentSubstitute(parameterValues[idx]),
											"");
							namedParam
									.setParameterValue(parameters[idx], value);
						} else {
							// something filled in, in the field column...
							//
							String value = "";
							if (resultRow != null) {
								value = resultRow.getString(
										parameterFieldNames[idx], "");
							}
							namedParam
									.setParameterValue(parameters[idx], value);
						}
					}
				}
			}

			first = false;

			try {
				if (log.isDetailed())
					log.logDetailed(toString(), Messages.getString(
							"JobTrans.StartingTrans", getFilename(), getName(),
							getDescription()));

				// Set the result rows for the next one...
				transMeta.setPreviousResult(result);

				if (clearResultRows) {
					transMeta.getPreviousResult().setRows(
							new ArrayList<RowMetaAndData>());
				}

				if (clearResultFiles) {
					transMeta.getPreviousResult().getResultFiles().clear();
				}

				/*
				 * Set one or more "result" rows on the transformation...
				 */
				if (execPerRow) // Execute for each input row
				{
					if (argFromPrevious) // Copy the input row to the (command
					// line) arguments
					{
						args = null;
						if (resultRow != null) {
							args = new String[resultRow.size()];
							for (int i = 0; i < resultRow.size(); i++) {
								args[i] = resultRow.getString(i, null);
							}
						}
					} else {
						// Just pass a single row
						List<RowMetaAndData> newList = new ArrayList<RowMetaAndData>();
						newList.add(resultRow);

						// This previous result rows list can be either empty or
						// not.
						// Depending on the checkbox "clear result rows"
						// In this case, it would execute the transformation
						// with one extra row each time
						// Can't figure out a real use-case for it, but hey, who
						// am I to decide that, right?
						// :-)
						//
						transMeta.getPreviousResult().getRows().addAll(newList);
					}

					if (paramsFromPrevious) { // Copy the input the parameters

						if (parameters != null) {
							for (int idx = 0; idx < parameters.length; idx++) {
								if (!Const.isEmpty(parameters[idx])) {
									// We have a parameter
									if (Const.isEmpty(Const
											.trim(parameterFieldNames[idx]))) {
										namedParam
												.setParameterValue(
														parameters[idx],
														Const
																.NVL(
																		environmentSubstitute(parameterValues[idx]),
																		""));
									} else {
										String fieldValue = "";

										if (resultRow != null) {
											fieldValue = resultRow.getString(
													parameterFieldNames[idx],
													"");
										}
										// Get the value from the input stream
										namedParam.setParameterValue(
												parameters[idx], Const.NVL(
														fieldValue, ""));
									}
								}
							}
						}
					}
				} else {
					if (argFromPrevious) {
						// Only put the first Row on the arguments
						args = null;
						if (resultRow != null) {
							args = new String[resultRow.size()];
							for (int i = 0; i < resultRow.size(); i++) {
								args[i] = resultRow.getString(i, null);
							}
						}
					} else {
						// do nothing
					}

					if (paramsFromPrevious) { // Copy the input the parameters
						if (parameters != null) {
							for (int idx = 0; idx < parameters.length; idx++) {
								if (!Const.isEmpty(parameters[idx])) {
									// We have a parameter
									if (Const.isEmpty(Const
											.trim(parameterFieldNames[idx]))) {
										namedParam
												.setParameterValue(
														parameters[idx],
														Const
																.NVL(
																		environmentSubstitute(parameterValues[idx]),
																		""));
									} else {
										String fieldValue = "";

										if (resultRow != null) {
											fieldValue = resultRow.getString(
													parameterFieldNames[idx],
													"");
										}
										// Get the value from the input stream
										namedParam.setParameterValue(
												parameters[idx], Const.NVL(
														fieldValue, ""));
									}
								}
							}
						}
					}
				}

				// Handle the parameters...
				//
				String[] parameterNames = transMeta.listParameters();
				for (int idx = 0; idx < parameterNames.length; idx++) {
					// Grab the parameter value set in the Trans job entry
					//
					String thisValue = namedParam
							.getParameterValue(parameterNames[idx]);
					if (!Const.isEmpty(thisValue)) {
						// Set the value as specified by the user in the job
						// entry
						//
						transMeta.setParameterValue(parameterNames[idx],
								thisValue);
					} else {
						// See if the parameter had a value set in the parent
						// job...
						// This value should pass down to the transformation if
						// that's what we opted to do.
						//
						if (isPassingAllParameters()) {
							String parentValue = parentJob
									.getParameterValue(parameterNames[idx]);
							if (!Const.isEmpty(parentValue)) {
								transMeta.setParameterValue(
										parameterNames[idx], parentValue);
							}
						}
					}
				}
				transMeta.activateParameters();

				// Execute this transformation across a cluster of servers
				//
				if (clustering) {
					TransExecutionConfiguration executionConfiguration = new TransExecutionConfiguration();
					executionConfiguration.setClusterPosting(true);
					executionConfiguration.setClusterPreparing(true);
					executionConfiguration.setClusterStarting(true);
					executionConfiguration
							.setClusterShowingTransformation(false);
					executionConfiguration.setSafeModeEnabled(false);
					executionConfiguration.setRepository(rep);

					// Also pass the variables from the transformation into the
					// execution configuration
					// That way it can go over the HTTP connection to the slave
					// server.
					//
					executionConfiguration.setVariables(transMeta);

					// Also set the arguments...
					//
					executionConfiguration.setArgumentStrings(args);

					TransSplitter transSplitter = Trans.executeClustered(
							transMeta, executionConfiguration);

					// Monitor the running transformations, wait until they are
					// done.
					// Also kill them all if anything goes bad
					// Also clean up afterwards...
					//
					long errors = Trans.monitorClusteredTransformation(log,
							toString(), transSplitter, parentJob);

					Result clusterResult = Trans
							.getClusteredTransformationResult(log, toString(),
									transSplitter, parentJob);
					result.clear();
					result.add(clusterResult);

					result.setNrErrors(result.getNrErrors() + errors);

				}
				// Execute this transformation remotely
				//
				else if (remoteSlaveServer != null) {
					// Remote execution...
					//
					TransExecutionConfiguration transExecutionConfiguration = new TransExecutionConfiguration();
					transExecutionConfiguration.setPreviousResult(transMeta
							.getPreviousResult().clone());
					transExecutionConfiguration.setArgumentStrings(args);
					transExecutionConfiguration.setVariables(this);
					transExecutionConfiguration
							.setRemoteServer(remoteSlaveServer);
					transExecutionConfiguration.setLogLevel(log.getLogLevel());

					// Send the XML over to the slave server
					// Also start the transformation over there...
					//
					Trans.sendXMLToSlaveServer(transMeta,
							transExecutionConfiguration);

					// Now start the monitoring...
					//
					SlaveServerTransStatus transStatus = null;
					while (!parentJob.isStopped() && waitingToFinish) {
						try {
							transStatus = remoteSlaveServer
									.getTransStatus(transMeta.getName());
							if (!transStatus.isRunning()) {
								// The transformation is finished, get the
								// result...
								//
								Result remoteResult = transStatus.getResult();
								result.clear();
								result.add(remoteResult);

								// In case you manually stop the remote trans
								// (browser etc), make sure it's marked as an
								// error
								//
								if (remoteResult.isStopped()) {
									result
											.setNrErrors(result.getNrErrors() + 1); //
								}

								// Make sure to clean up : write a log record
								// etc, close any left-over sockets etc.
								//
								remoteSlaveServer
										.cleanupTransformation(transMeta
												.getName());

								break;
							}
						} catch (Exception e1) {

							log
									.logError(
											toString(),
											Messages
													.getString(
															"JobTrans.Error.UnableContactSlaveServer",
															""
																	+ remoteSlaveServer,
															transMeta.getName()));
							result.setNrErrors(result.getNrErrors() + 1L);
							break; // Stop looking too, chances are too low the
							// server will come back on-line
						}

						try {
							Thread.sleep(2000);
						} catch (InterruptedException e) {
						}
						; // sleep for 2 seconds
					}

					if (parentJob.isStopped()) {
						// See if we have a status and if we need to stop the
						// remote execution here...
						// 
						if (transStatus == null || transStatus.isRunning()) {
							// Try a remote abort ...
							//
							remoteSlaveServer.stopTransformation(transMeta
									.getName());

							// And a cleanup...
							//
							remoteSlaveServer.cleanupTransformation(transMeta
									.getName());

							// Set an error state!
							//
							result.setNrErrors(result.getNrErrors() + 1L);
						}
					}
				}
				// Execute this transformation on the local machine
				//
				else // Local execution...
				{
					// Create the transformation from meta-data
					Trans trans = new Trans(transMeta);

					if (parentJob.getJobMeta().isBatchIdPassed()) {
						trans.setPassedBatchId(parentJob.getPassedBatchId());
					}

					// set the parent job on the transformation, variables are
					// taken from here...
					trans.setParentJob(parentJob);
					trans.setParentVariableSpace(parentJob);

					// First get the root job
					//
					Job rootJob = parentJob;
					while (rootJob.getParentJob() != null)
						rootJob = rootJob.getParentJob();

					// Get the start and end-date from the root job...
					//
					trans.setJobStartDate(rootJob.getStartDate());
					trans.setJobEndDate(rootJob.getEndDate());

					try {
						// Start execution...
						//
						trans.execute(args);

						// Wait until we're done with it...
						//
						while (!trans.isFinished() && !parentJob.isStopped()
								&& trans.getErrors() == 0) {
							try {
								Thread.sleep(0, 500);
							} catch (InterruptedException e) {
							}
						}

						if (parentJob.isStopped() || trans.getErrors() != 0) {
							trans.stopAll();
							trans.waitUntilFinished();
							trans.endProcessing(Database.LOG_STATUS_STOP);
							result.setNrErrors(1);
						} else {
							trans.endProcessing(Database.LOG_STATUS_END);
						}
						Result newResult = trans.getResult();

						result.clear(); // clear only the numbers, NOT the files
						// or rows.
						result.add(newResult);

						// Set the result rows too...
						result.setRows(newResult.getRows());

						if (setLogfile) {
							ResultFile resultFile = new ResultFile(
									ResultFile.FILE_TYPE_LOG, KettleVFS
											.getFileObject(getLogFilename()),
									parentJob.getJobname(), toString());
							result.getResultFiles()
									.put(resultFile.getFile().toString(),
											resultFile);
						}
					} catch (KettleException e) {

						log.logError(toString(), Messages
								.getString("JobTrans.Error.UnablePrepareExec"),
								e);
						result.setNrErrors(1);
					}
				}
			} catch (Exception e) {

				log.logError(toString(), Messages.getString(
						"JobTrans.ErrorUnableOpenTrans", e.getMessage()));
				log.logError(toString(), Const.getStackTracker(e));
				result.setNrErrors(1);
			}
			iteration++;
		}

		if (setLogfile) {
			if (appender != null) {
				log.removeAppender(appender);
				appender.close();

				ResultFile resultFile = new ResultFile(
						ResultFile.FILE_TYPE_LOG, appender.getFile(), parentJob
								.getJobname(), getName());
				result.getResultFiles().put(resultFile.getFile().toString(),
						resultFile);
			}
			log.setLogLevel(backupLogLevel);
		}

		if (result.getNrErrors() == 0) {
			result.setResult(true);
		} else {
			result.setResult(false);
		}

		// Hint the VM to release handles.
		System.gc();

		return result;
	}

	private TransMeta getTransMeta(Repository rep) throws KettleException {
		try {
			LogWriter log = LogWriter.getInstance();

			TransMeta transMeta = null;
			if (!Const.isEmpty(getFilename())) // Load from an XML file
			{
				String filename = environmentSubstitute(getFilename());
				log.logBasic(toString(),
						"Loading transformation from XML file [" + filename
								+ "]");
				transMeta = new TransMeta(filename, null, true, this);
				transMeta.copyVariablesFrom(this);
			} else if (!Const.isEmpty(getTransname()) && getDirectory() != null) // Load
			// from
			// the
			// repository
			{
				String filename = environmentSubstitute(getTransname());

				log.logBasic(toString(), Messages.getString(
						"JobTrans.Log.LoadingTransRepDirec", filename, ""
								+ getDirectory()));

				if (rep != null) {
					//
					// It only makes sense to try to load from the repository
					// when the repository is also filled in.
					//
					transMeta = new TransMeta(rep, filename, rep
							.getDirectoryTree().findDirectory(
									environmentSubstitute(getDirectory())));
					transMeta.copyVariablesFrom(this);
				} else {

					throw new KettleException(Messages
							.getString("JobTrans.Exception.NoRepDefined"));
				}
			} else {
				throw new KettleJobException(Messages
						.getString("JobTrans.Exception.TransNotSpecified"));
			}

			// Set the arguments...
			transMeta.setArguments(arguments);

			return transMeta;
		} catch (Exception e) {

			throw new KettleException(Messages
					.getString("JobTrans.Exception.MetaDataLoad"), e);
		}
	}

	public boolean evaluates() {
		return true;
	}

	public boolean isUnconditional() {
		return true;
	}

	public List<SQLStatement> getSQLStatements(Repository repository)
			throws KettleException {
		TransMeta transMeta = getTransMeta(repository);

		return transMeta.getSQLStatements();
	}

	/**
	 * @return Returns the directoryPath.
	 */
	public String getDirectoryPath() {
		return directoryPath;
	}

	/**
	 * @param directoryPath
	 *            The directoryPath to set.
	 */
	public void setDirectoryPath(String directoryPath) {
		this.directoryPath = directoryPath;
	}

	/**
	 * @return the clustering
	 */
	public boolean isClustering() {
		return clustering;
	}

	/**
	 * @param clustering
	 *            the clustering to set
	 */
	public void setClustering(boolean clustering) {
		this.clustering = clustering;
	}

	public void check(List<CheckResultInterface> remarks, JobMeta jobMeta) {
		if (setLogfile) {
			andValidator().validate(this,
					"logfile", remarks, putValidators(notBlankValidator())); //$NON-NLS-1$
		}
		if (!Const.isEmpty(filename)) {
			andValidator().validate(this,
					"filename", remarks, putValidators(notBlankValidator())); //$NON-NLS-1$
		} else {
			andValidator().validate(this,
					"transname", remarks, putValidators(notBlankValidator())); //$NON-NLS-1$
			andValidator().validate(this,
					"directory", remarks, putValidators(notNullValidator())); //$NON-NLS-1$
		}
	}

	public List<ResourceReference> getResourceDependencies(JobMeta jobMeta) {
		List<ResourceReference> references = super
				.getResourceDependencies(jobMeta);
		if (!Const.isEmpty(filename)) {
			// During this phase, the variable space hasn't been initialized yet
			// - it seems
			// to happen during the execute. As such, we need to use the job
			// meta's resolution
			// of the variables.
			String realFileName = jobMeta.environmentSubstitute(filename);
			ResourceReference reference = new ResourceReference(this);
			reference.getEntries().add(
					new ResourceEntry(realFileName, ResourceType.ACTIONFILE));
			references.add(reference);
		}
		return references;
	}

	/**
	 * We're going to load the transformation meta data referenced here. Then
	 * we're going to give it a new filename, modify that filename in this
	 * entries. The parent caller will have made a copy of it, so it should be
	 * OK to do so.
	 */
	public String exportResources(VariableSpace space,
			Map<String, ResourceDefinition> definitions,
			ResourceNamingInterface namingInterface) throws KettleException {
		// Try to load the transformation from repository or file.
		// Modify this recursively too...
		//
		if (!Const.isEmpty(filename)) {
			// AGAIN: there is no need to clone this job entry because the
			// caller is responsible for this.
			//
			// First load the transformation metadata...
			//
			// Had to add this to support variable replacement here as well.
			// asilva 9/07/07
			copyVariablesFrom(space);
			TransMeta transMeta = getTransMeta(null);

			// Also go down into the transformation and export the files there.
			// (mapping recursively down)
			//
			String newFilename = transMeta.exportResources(transMeta,
					definitions, namingInterface);

			// Set the correct filename inside the XML.
			// Replace if BEFORE XML generation occurs.
			transMeta.setFilename(newFilename);

			// change it in the job entry
			//
			filename = newFilename;

			//
			// Don't save it, that has already been done a few lines above, in
			// transMeta.exportResources()
			//
			// String xml = transMeta.getXML();
			// definitions.put(newFilename, new ResourceDefinition(newFilename,
			// xml));
			//

			return newFilename;
		} else {
			return null;
		}
	}

	protected String getLogfile() {
		return logfile;
	}

	/**
	 * @return the remote slave server name
	 */
	public String getRemoteSlaveServerName() {
		return remoteSlaveServerName;
	}

	/**
	 * @param remoteSlaveServerName
	 *            the remote slave server name to set
	 */
	public void setRemoteSlaveServerName(String remoteSlaveServerName) {
		this.remoteSlaveServerName = remoteSlaveServerName;
	}

	/**
	 * @return the waitingToFinish
	 */
	public boolean isWaitingToFinish() {
		return waitingToFinish;
	}

	/**
	 * @param waitingToFinish
	 *            the waitingToFinish to set
	 */
	public void setWaitingToFinish(boolean waitingToFinish) {
		this.waitingToFinish = waitingToFinish;
	}

	/**
	 * @return the followingAbortRemotely
	 */
	public boolean isFollowingAbortRemotely() {
		return followingAbortRemotely;
	}

	/**
	 * @param followingAbortRemotely
	 *            the followingAbortRemotely to set
	 */
	public void setFollowingAbortRemotely(boolean followingAbortRemotely) {
		this.followingAbortRemotely = followingAbortRemotely;
	}

	public boolean isPassingAllParameters() {
		return passingAllParameters;
	}

	public void setPassingAllParameters(boolean passingAllParameters) {
		this.passingAllParameters = passingAllParameters;
	}

}
