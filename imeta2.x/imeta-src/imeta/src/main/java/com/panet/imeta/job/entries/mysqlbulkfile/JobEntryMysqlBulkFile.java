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

package com.panet.imeta.job.entries.mysqlbulkfile;

import static com.panet.imeta.job.entry.validator.AndValidator.putValidators;
import static com.panet.imeta.job.entry.validator.JobEntryValidatorUtils.andValidator;
import static com.panet.imeta.job.entry.validator.JobEntryValidatorUtils.notBlankValidator;

import java.io.File;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Node;

import com.panet.imeta.cluster.SlaveServer;
import com.panet.imeta.core.CheckResultInterface;
import com.panet.imeta.core.Const;
import com.panet.imeta.core.Result;
import com.panet.imeta.core.ResultFile;
import com.panet.imeta.core.database.Database;
import com.panet.imeta.core.database.DatabaseMeta;
import com.panet.imeta.core.exception.KettleDatabaseException;
import com.panet.imeta.core.exception.KettleException;
import com.panet.imeta.core.exception.KettleXMLException;
import com.panet.imeta.core.logging.LogWriter;
import com.panet.imeta.core.util.StringUtil;
import com.panet.imeta.core.vfs.KettleVFS;
import com.panet.imeta.core.xml.XMLHandler;
import com.panet.imeta.job.Job;
import com.panet.imeta.job.JobEntryType;
import com.panet.imeta.job.JobMeta;
import com.panet.imeta.job.entries.waitforsql.Messages;
import com.panet.imeta.job.entry.JobEntryBase;
import com.panet.imeta.job.entry.JobEntryInterface;
import com.panet.imeta.repository.Repository;
import com.panet.imeta.resource.ResourceEntry;
import com.panet.imeta.resource.ResourceReference;
import com.panet.imeta.resource.ResourceEntry.ResourceType;
import com.panet.imeta.shared.SharedObjectInterface;


/**
 * This defines an MYSQL Bulk file job entry.
 *
 * @author Samatar
 * @since 05-03-2006
 *
 */
public class JobEntryMysqlBulkFile extends JobEntryBase implements Cloneable, JobEntryInterface
{
	
	public static final String STEP_ATTRIBUTE_SCHEMANAME ="schemaname";
	public static final String STEP_ATTRIBUTE_TABLENAME="tablename";
	public static final String STEP_ATTRIBUTE_FILENAME ="filename";
	public static final String STEP_ATTRIBUTE_SEPARATOR="separator";
	public static final String STEP_ATTRIBUTE_ENCLOSED ="enclosed";
	public static final String STEP_ATTRIBUTE_LINETERMINATED ="lineterminated";
	public static final String STEP_ATTRIBUTE_LIMITLINES ="limitlines";
	public static final String STEP_ATTRIBUTE_LISTCOLUMN ="listcolumn";
	public static final String STEP_ATTRIBUTE_HIGHPRIORITY ="highpriority";
	public static final String STEP_ATTRIBUTE_OPTIONENCLOSED ="optionenclosed";
	public static final String STEP_ATTRIBUTE_OUTDUMPVALUE ="outdumpvalue";
	public static final String STEP_ATTRIBUTE_IFFILEEXISTS ="iffileexists";
	public static final String STEP_ATTRIBUTE_ADDFILETORESULT="addfiletoresult";
	
	private String tablename;
	private String schemaname;
	private String filename;
	private String separator;
	private String enclosed;
	private String lineterminated;
	private String limitlines;
	private String listcolumn;
	private boolean highpriority;
	private boolean optionenclosed;
	public int outdumpvalue;
	public int iffileexists;
	private boolean addfiletoresult ;
	
	private DatabaseMeta connection;

	public JobEntryMysqlBulkFile(String n)
	{
		super(n, "");
		tablename=null;
		schemaname=null;
		filename=null;
		separator=null;
		enclosed=null;
		limitlines = "0";
		listcolumn=null;
		lineterminated=null;
		highpriority=true;
		optionenclosed=false;
		iffileexists=2;
		connection=null;
		addfiletoresult = false;
		setID(-1L);
		setJobEntryType(JobEntryType.MYSQL_BULK_FILE);
	}

	public JobEntryMysqlBulkFile()
	{
		this("");
	}

	public JobEntryMysqlBulkFile(JobEntryBase jeb)
	{
		super(jeb);
	}

	public Object clone()
	{
		JobEntryMysqlBulkFile je = (JobEntryMysqlBulkFile) super.clone();
		return je;
	}

	public String getXML()
	{
		StringBuffer retval = new StringBuffer(200);

		retval.append(super.getXML());
		retval.append("      ").append(XMLHandler.addTagValue("schemaname",  schemaname));
		retval.append("      ").append(XMLHandler.addTagValue("tablename",  tablename));
		retval.append("      ").append(XMLHandler.addTagValue("filename",  filename));
		retval.append("      ").append(XMLHandler.addTagValue("separator",  separator));
		retval.append("      ").append(XMLHandler.addTagValue("enclosed",  enclosed));
		retval.append("      ").append(XMLHandler.addTagValue("optionenclosed",  optionenclosed));
		retval.append("      ").append(XMLHandler.addTagValue("lineterminated",  lineterminated));
		retval.append("      ").append(XMLHandler.addTagValue("limitlines",  limitlines));
		retval.append("      ").append(XMLHandler.addTagValue("listcolumn",  listcolumn));
		retval.append("      ").append(XMLHandler.addTagValue("highpriority",  highpriority));
		retval.append("      ").append(XMLHandler.addTagValue("outdumpvalue",  outdumpvalue));
		retval.append("      ").append(XMLHandler.addTagValue("iffileexists",  iffileexists));
		retval.append("      ").append(XMLHandler.addTagValue("addfiletoresult",  addfiletoresult));
		retval.append("      ").append(XMLHandler.addTagValue("connection", connection==null?null:connection.getName()));

		return retval.toString();
	}

	public void loadXML(Node entrynode, List<DatabaseMeta> databases, List<SlaveServer> slaveServers, Repository rep) throws KettleXMLException
	{
		try
		{
			super.loadXML(entrynode, databases, slaveServers);
			schemaname     = XMLHandler.getTagValue(entrynode, "schemaname");
			tablename     = XMLHandler.getTagValue(entrynode, "tablename");
			filename     = XMLHandler.getTagValue(entrynode, "filename");
			separator     = XMLHandler.getTagValue(entrynode, "separator");
			enclosed     = XMLHandler.getTagValue(entrynode, "enclosed");
			lineterminated     = XMLHandler.getTagValue(entrynode, "lineterminated");
			limitlines     = XMLHandler.getTagValue(entrynode, "limitlines");
			listcolumn     = XMLHandler.getTagValue(entrynode, "listcolumn");
			highpriority = "Y".equalsIgnoreCase(XMLHandler.getTagValue(entrynode, "highpriority"));
			optionenclosed = "Y".equalsIgnoreCase(XMLHandler.getTagValue(entrynode, "optionenclosed"));
			outdumpvalue     = Const.toInt(XMLHandler.getTagValue(entrynode, "outdumpvalue"), -1);
			iffileexists = Const.toInt(XMLHandler.getTagValue(entrynode, "iffileexists"), -1);
			String dbname = XMLHandler.getTagValue(entrynode, "connection");
			connection    = DatabaseMeta.findDatabase(databases, dbname);
			addfiletoresult = "Y".equalsIgnoreCase(XMLHandler.getTagValue(entrynode, "addfiletoresult"));
		}
		catch(KettleException e)
		{
			throw new KettleXMLException("Unable to load job entry of type 'table exists' from XML node", e);
		}
	}
	
	public void setInfo(Map<String, String[]> p, String id,
			List<? extends SharedObjectInterface> databases) {
		// TODO Auto-generated method stub
		
		connection = DatabaseMeta.findDatabase((List<DatabaseMeta>) databases, JobEntryBase
				.parameterToLong(p.get(id + ".connection")));
		
		schemaname = JobEntryBase.parameterToString(p.get(id + ".schemaname"));
		tablename = JobEntryBase.parameterToString(p.get(id + ".tablename"));
		filename = JobEntryBase.parameterToString(p.get(id + ".filename"));
		separator = JobEntryBase.parameterToString(p.get(id + ".separator"));
		enclosed = JobEntryBase.parameterToString(p.get(id + ".enclosed"));
		lineterminated = JobEntryBase.parameterToString(p.get(id + ".lineterminated"));
		limitlines = JobEntryBase.parameterToString(p.get(id + ".limitlines"));
		listcolumn = JobEntryBase.parameterToString(p.get(id + ".listcolumn"));
		highpriority =JobEntryBase.parameterToBoolean(p.get(id + ".highpriority"));
		optionenclosed = JobEntryBase.parameterToBoolean(p.get(id + ".optionenclosed"));
		outdumpvalue = JobEntryBase.parameterToInt(p.get(id + ".outdumpvalue"));
		iffileexists =  JobEntryBase.parameterToInt(p.get(id + ".iffileexists"));	
		addfiletoresult = JobEntryBase.parameterToBoolean(p.get(id + ".addfiletoresult"));

	}
	
	public void loadRep(Repository rep, long id_jobentry, List<DatabaseMeta> databases, List<SlaveServer> slaveServers)
		throws KettleException
	{
		try
		{
			super.loadRep(rep, id_jobentry, databases, slaveServers);
			
			schemaname = rep.getJobEntryAttributeString(id_jobentry, STEP_ATTRIBUTE_SCHEMANAME );
			tablename = rep.getJobEntryAttributeString(id_jobentry,STEP_ATTRIBUTE_TABLENAME);
			filename = rep.getJobEntryAttributeString(id_jobentry, STEP_ATTRIBUTE_FILENAME );
			separator = rep.getJobEntryAttributeString(id_jobentry,STEP_ATTRIBUTE_SEPARATOR);
			enclosed = rep.getJobEntryAttributeString(id_jobentry, STEP_ATTRIBUTE_ENCLOSED );
			lineterminated = rep.getJobEntryAttributeString(id_jobentry, STEP_ATTRIBUTE_LINETERMINATED );
			limitlines = rep.getJobEntryAttributeString(id_jobentry, STEP_ATTRIBUTE_LIMITLINES );
			listcolumn = rep.getJobEntryAttributeString(id_jobentry, STEP_ATTRIBUTE_LISTCOLUMN );
			highpriority =rep.getJobEntryAttributeBoolean(id_jobentry, STEP_ATTRIBUTE_HIGHPRIORITY );
			optionenclosed =rep.getJobEntryAttributeBoolean(id_jobentry, STEP_ATTRIBUTE_OPTIONENCLOSED );
			outdumpvalue =(int) rep.getJobEntryAttributeInteger(id_jobentry, STEP_ATTRIBUTE_OUTDUMPVALUE );
			iffileexists =(int) rep.getJobEntryAttributeInteger(id_jobentry, STEP_ATTRIBUTE_IFFILEEXISTS );
			addfiletoresult =rep.getJobEntryAttributeBoolean(id_jobentry,STEP_ATTRIBUTE_ADDFILETORESULT);

			long id_db = rep.getJobEntryAttributeInteger(id_jobentry, "id_database");
			if (id_db>0)
			{
				connection = DatabaseMeta.findDatabase(databases, id_db);
			}
			else
			{
				// This is were we end up in normally, the previous lines are for backward compatibility.
				connection = DatabaseMeta.findDatabase(databases, rep.getJobEntryAttributeString(id_jobentry, "connection"));
			}

		}
		catch(KettleDatabaseException dbe)
		{
			throw new KettleException("Unable to load job entry of type 'table exists' from the repository for id_jobentry="+id_jobentry, dbe);
		}
	}

	public void saveRep(Repository rep, long id_job)
		throws KettleException
	{
		try
		{
			super.saveRep(rep, id_job);	
			rep.saveJobEntryAttribute(id_job, getID(),STEP_ATTRIBUTE_SCHEMANAME , schemaname);
			rep.saveJobEntryAttribute(id_job, getID(),STEP_ATTRIBUTE_TABLENAME,tablename);
			rep.saveJobEntryAttribute(id_job, getID(),STEP_ATTRIBUTE_FILENAME , filename);
			rep.saveJobEntryAttribute(id_job, getID(),STEP_ATTRIBUTE_SEPARATOR,separator);
			rep.saveJobEntryAttribute(id_job, getID(),STEP_ATTRIBUTE_ENCLOSED , enclosed);
			rep.saveJobEntryAttribute(id_job, getID(),STEP_ATTRIBUTE_LINETERMINATED , lineterminated);
			rep.saveJobEntryAttribute(id_job, getID(),STEP_ATTRIBUTE_LIMITLINES , limitlines);
			rep.saveJobEntryAttribute(id_job, getID(),STEP_ATTRIBUTE_LISTCOLUMN , listcolumn);
			rep.saveJobEntryAttribute(id_job, getID(),STEP_ATTRIBUTE_HIGHPRIORITY , highpriority);
			rep.saveJobEntryAttribute(id_job, getID(),STEP_ATTRIBUTE_OPTIONENCLOSED , optionenclosed);
			rep.saveJobEntryAttribute(id_job, getID(),STEP_ATTRIBUTE_OUTDUMPVALUE , outdumpvalue);
			rep.saveJobEntryAttribute(id_job, getID(),STEP_ATTRIBUTE_IFFILEEXISTS , iffileexists);			
			rep.saveJobEntryAttribute(id_job, getID(),STEP_ATTRIBUTE_ADDFILETORESULT,addfiletoresult);

			if (connection!=null) rep.saveJobEntryAttribute(id_job, getID(), "connection", connection.getName());
		}
		catch(KettleDatabaseException dbe)
		{
			throw new KettleException("Unable to load job entry of type 'Mysql Bulk Load' to the repository for id_job="+id_job, dbe);
		}
	}


	public void setTablename(String tablename)
	{
		this.tablename = tablename;
	}
	public void setSchemaname(String schemaname)
	{
		this.schemaname = schemaname;
	}

	public String getTablename()
	{
		return tablename;
	}
	public String getSchemaname()
	{
		return schemaname;
	}

	public void setDatabase(DatabaseMeta database)
	{
		this.connection = database;
	}

	public DatabaseMeta getDatabase()
	{
		return connection;
	}

	public boolean evaluates()
	{
		return true;
	}

	public boolean isUnconditional()
	{
		return false;
	}

	public Result execute(Result previousResult, int nr, Repository rep, Job parentJob)
	{

		String LimitNbrLignes="";
		String ListOfColumn="*";
		String strHighPriority="";
		String OutDumpText="";
		String OptionEnclosed="";
		String FieldSeparator="";
		String LinesTerminated="";

		LogWriter log = LogWriter.getInstance();

		Result result = previousResult;
		result.setResult(false);

		// Let's check  the filename ...
		if (filename!=null)
		{
			// User has specified a file, We can continue ...
			String realFilename = getRealFilename();
			File file = new File(realFilename);

			if (file.exists() && iffileexists==2)
			{
				// the file exists and user want to Fail
				result.setResult( false );
				result.setNrErrors(1);
				log.logError(toString(),Messages.getString("JobMysqlBulkFile.FileExists1.Label") +
					realFilename + Messages.getString("JobMysqlBulkFile.FileExists2.Label"));

			}
			else if (file.exists() && iffileexists==1)
			{
				// the file exists and user want to do nothing
				result.setResult( true );
				if(log.isDetailed())	
					log.logDetailed(toString(),Messages.getString("JobMysqlBulkFile.FileExists1.Label") +
						realFilename + Messages.getString("JobMysqlBulkFile.FileExists2.Label"));

			}
			else
			{

				if (file.exists() && iffileexists==0)
				{
					// File exists and user want to renamme it with unique name

					//Format Date

					// Try to clean filename (without wildcard)
					String wildcard = realFilename.substring(realFilename.length()-4,realFilename.length());
					if(wildcard.substring(0,1).equals("."))
					{
						// Find wildcard
						realFilename=realFilename.substring(0,realFilename.length()-4) +
							"_" + StringUtil.getFormattedDateTimeNow(true) + wildcard;
					}
					else
					{
						// did not find wildcard
						realFilename=realFilename + "_" + StringUtil.getFormattedDateTimeNow(true);
					}

					log.logDebug(toString(), Messages.getString("JobMysqlBulkFile.FileNameChange1.Label") + realFilename +
						Messages.getString("JobMysqlBulkFile.FileNameChange1.Label"));



				}

				// User has specified an existing file, We can continue ...
				if(log.isDetailed())	
					log.logDetailed(toString(), Messages.getString("JobMysqlBulkFile.FileExists1.Label") +
									realFilename + Messages.getString("JobMysqlBulkFile.FileExists2.Label"));


				if (connection!=null)
				{
					// User has specified a connection, We can continue ...
					Database db = new Database(connection);
					db.shareVariablesWith(this);
					try
					{
						db.connect();
						// Get schemaname
						String realSchemaname = environmentSubstitute(schemaname);
						// Get tablename
						String realTablename = environmentSubstitute(tablename);

						if (db.checkTableExists(realTablename))
						{
							// The table existe, We can continue ...
							if(log.isDetailed())	
								log.logDetailed(toString(), Messages.getString("JobMysqlBulkFile.TableExists1.Label")+realTablename+
									Messages.getString("JobMysqlBulkFile.TableExists2.Label"));


							// Add schemaname (Most the time Schemaname.Tablename)
							if (schemaname !=null)
							{
								realTablename= realSchemaname + "." + realTablename;
							}

							// Set the Limit lines
							if (Const.toInt(getRealLimitlines(),0)>0)
							{
								LimitNbrLignes = "LIMIT " + getRealLimitlines() ;
							}

							// Set list of Column, if null get all columns (*)
							if (getRealListColumn()!= null )
							{
								ListOfColumn= MysqlString(getRealListColumn()) ;
							}


							// Fields separator
							if (getRealSeparator()!= null && outdumpvalue == 0)
							{
								FieldSeparator="FIELDS TERMINATED BY '" + Const.replace(getRealSeparator(), "'", "''") + "'";

							}

							// Lines Terminated by
							if (getRealLineterminated()!= null && outdumpvalue == 0)
							{
								LinesTerminated="LINES TERMINATED BY '" + Const.replace(getRealLineterminated(), "'", "''") + "'";

							}



							// High Priority ?
							if (isHighPriority())
							{
								strHighPriority = "HIGH_PRIORITY";
							}

							if (getRealEnclosed()!= null && outdumpvalue == 0)
							{
								if (isOptionEnclosed())
								{
									OptionEnclosed="OPTIONALLY ";
								}
								OptionEnclosed=OptionEnclosed + "ENCLOSED BY '" + Const.replace(getRealEnclosed(), "'", "''") + "'";

							}

							// OutFile or Dumpfile
							if (outdumpvalue == 0)
							{
								OutDumpText ="INTO OUTFILE";
							}
							else
							{
								OutDumpText = "INTO DUMPFILE";
							}


							String FILEBulkFile = "SELECT " + strHighPriority + " " + ListOfColumn + " " +
									OutDumpText + " '" + realFilename	+ "' " + FieldSeparator + " " +
									OptionEnclosed + " " + LinesTerminated  + " FROM " +
									realTablename + " " + LimitNbrLignes + 	" LOCK IN SHARE MODE";

							try
							{
								if(log.isDetailed())	
									log.logDetailed(toString(), FILEBulkFile);
								// Run the SQL
								PreparedStatement ps= db.prepareSQL(FILEBulkFile);
								ps.execute();

								// Everything is OK...we can disconnect now
								db.disconnect();
								
								if (isAddFileToResult())
								{
									// Add filename to output files
				                	ResultFile resultFile = new ResultFile(ResultFile.FILE_TYPE_GENERAL, KettleVFS.getFileObject(realFilename), parentJob.getJobname(), toString());
				                    result.getResultFiles().put(resultFile.getFile().toString(), resultFile);
								}
								
								result.setResult(true);


							}
							catch(SQLException je)
							{
								db.disconnect();
								result.setNrErrors(1);
								log.logError(toString(), Messages.getString("JobMysqlBulkFile.Error.Label") + " "+je.getMessage());
							}
							catch (IOException e)
							{
				       			log.logError(toString(), Messages.getString("JobMysqlBulkFile.Error.Label") + e.getMessage());
								result.setNrErrors(1);
							}


						}
						else
						{
							// Of course, the table should have been created already before the bulk load operation
							db.disconnect();
							result.setNrErrors(1);
							if(log.isDetailed())	
								log.logDetailed(toString(), Messages.getString("JobMysqlBulkFile.TableNotExists1.Label") +realTablename+
									Messages.getString("JobMysqlBulkFile.TableNotExists2.Label"));
						}


					}
					catch(KettleDatabaseException dbe)
					{
						db.disconnect();
						result.setNrErrors(1);
						log.logError(toString(), Messages.getString("JobMysqlBulkFile.Error.Label")  + " " + dbe.getMessage());
					}



				}

				else
				{
					// No database connection is defined
					result.setNrErrors(1);
					log.logError(toString(),  Messages.getString("JobMysqlBulkFile.Nodatabase.Label"));
				}


			}


		}
		else
		{
			// No file was specified
			result.setNrErrors(1);
			log.logError(toString(), Messages.getString("JobMysqlBulkFile.Nofilename.Label"));
		}

		return result;

	}

	public DatabaseMeta[] getUsedDatabaseConnections()
	{
		return new DatabaseMeta[] { connection, };
	}




	public void setHighPriority(boolean highpriority)
	{
		this.highpriority = highpriority;
	}
	public void setOptionEnclosed(boolean optionenclosed)
	{
		this.optionenclosed = optionenclosed;
	}


	public boolean isHighPriority()
	{
		return highpriority;
	}
	public boolean isOptionEnclosed()
	{
		return optionenclosed;
	}


	public void setFilename(String filename)
	{
		this.filename = filename;
	}

	public String getFilename()
	{
		return filename;
	}

	public String getRealFilename()
	{
		String RealFile= environmentSubstitute(getFilename());
		return RealFile.replace('\\','/');
	}
	public void setSeparator(String separator)
	{
		this.separator = separator;
	}

	public void setEnclosed(String enclosed)
	{
		this.enclosed = enclosed;
	}
	public void setLineterminated(String lineterminated)
	{
		this.lineterminated = lineterminated;
	}

	public String getLineterminated()
	{
		return lineterminated;
	}

	public String getRealLineterminated()
	{
		return environmentSubstitute(getLineterminated());
	}

	public String getSeparator()
	{
		return separator;
	}
	public String getEnclosed()
	{
		return enclosed;
	}

	public String getRealSeparator()
	{
		return environmentSubstitute(getSeparator());
	}

	public String getRealEnclosed()
	{
		return environmentSubstitute(getEnclosed());
	}

	public void setLimitlines(String limitlines)
	{
		this.limitlines = limitlines;
	}

	public String getLimitlines()
	{
		return limitlines;
	}

	public String getRealLimitlines()
	{
		return environmentSubstitute(getLimitlines());
	}

	public void setListColumn(String listcolumn)
	{
		this.listcolumn = listcolumn;
	}
	public String getListColumn()
	{
		return listcolumn;
	}

	public String getRealListColumn()
	{
		return environmentSubstitute(getListColumn());
	}

	public void setAddFileToResult(boolean addfiletoresultin)
	{
		this.addfiletoresult = addfiletoresultin;
	}

	public boolean isAddFileToResult()
	{
		return addfiletoresult;
	}
	
	private String MysqlString(String listcolumns)
	{
		/* handle forbiden char like '
		 */
		String ReturnString="";
		String[] split = listcolumns.split(",");

		for (int i=0;i<split.length;i++)
		{
			if(ReturnString.equals(""))
				ReturnString =  "`" + Const.trim(split[i]) + "`";
			else
				ReturnString = ReturnString +  ", `" + Const.trim(split[i]) + "`";

		}

		return ReturnString;


	}

  public List<ResourceReference> getResourceDependencies(JobMeta jobMeta) {
    List<ResourceReference> references = super.getResourceDependencies(jobMeta);
    if (connection != null) {
      ResourceReference reference = new ResourceReference(this);
      reference.getEntries().add( new ResourceEntry(connection.getHostname(), ResourceType.SERVER));
      reference.getEntries().add( new ResourceEntry(connection.getDatabaseName(), ResourceType.DATABASENAME));
      references.add(reference);
    }
    return references;
  }

  @Override
  public void check(List<CheckResultInterface> remarks, JobMeta jobMeta)
  {
    andValidator().validate(this, "filename", remarks, putValidators(notBlankValidator())); //$NON-NLS-1$
    andValidator().validate(this, "tablename", remarks, putValidators(notBlankValidator())); //$NON-NLS-1$
  }

}
