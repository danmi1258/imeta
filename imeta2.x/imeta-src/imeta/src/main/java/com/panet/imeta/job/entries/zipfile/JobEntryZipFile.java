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

package com.panet.imeta.job.entries.zipfile;

import static com.panet.imeta.job.entry.validator.AbstractFileValidator.putVariableSpace;
import static com.panet.imeta.job.entry.validator.AndValidator.putValidators;
import static com.panet.imeta.job.entry.validator.FileDoesNotExistValidator.putFailIfExists;
import static com.panet.imeta.job.entry.validator.JobEntryValidatorUtils.andValidator;
import static com.panet.imeta.job.entry.validator.JobEntryValidatorUtils.fileDoesNotExistValidator;
import static com.panet.imeta.job.entry.validator.JobEntryValidatorUtils.notBlankValidator;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.Deflater;
import java.util.zip.ZipInputStream;

import org.apache.commons.vfs.FileObject;
import org.apache.tools.zip.ZipEntry;
import org.w3c.dom.Node;

import com.panet.imeta.cluster.SlaveServer;
import com.panet.imeta.core.CheckResultInterface;
import com.panet.imeta.core.Const;
import com.panet.imeta.core.Result;
import com.panet.imeta.core.ResultFile;
import com.panet.imeta.core.RowMetaAndData;
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
import com.panet.imeta.job.entry.JobEntryBase;
import com.panet.imeta.job.entry.JobEntryInterface;
import com.panet.imeta.job.entry.validator.ValidatorContext;
import com.panet.imeta.repository.Repository;
import com.panet.imeta.shared.SharedObjectInterface;
import com.panet.imeta.trans.step.BaseStepMeta;

/**
 * This defines a 'zip file' job entry. Its main use would be to zip files in a
 * directory and process zipped files (deleted or move).
 * 
 * @author Samatar Hassan
 * @since 27-02-2007
 * 
 */
public class JobEntryZipFile extends JobEntryBase implements Cloneable,
		JobEntryInterface {
	private String zipFilename;
	public int compressionrate;
	public int ifzipfileexists;
	public int afterzip;
	private String wildcard;
	private String wildcardexclude;
	private String sourcedirectory;
	private String movetodirectory;
	private boolean addfiletoresult;
	private boolean isfromprevious;
	private boolean createparentfolder;
	private boolean adddate;
	private boolean addtime;
	private boolean SpecifyFormat;
	private String date_time_format;

	/**
	 * Default constructor.
	 */
	public JobEntryZipFile(String n) {
		super(n, "");
		date_time_format = null;
		zipFilename = null;
		ifzipfileexists = 2;
		afterzip = 0;
		compressionrate = 1;
		wildcard = null;
		wildcardexclude = null;
		sourcedirectory = null;
		movetodirectory = null;
		addfiletoresult = false;
		isfromprevious = false;
		createparentfolder = false;
		adddate = false;
		addtime = false;
		SpecifyFormat = false;
		setID(-1L);
		setJobEntryType(JobEntryType.ZIP_FILE);
	}

	public JobEntryZipFile() {
		this("");
	}

	public JobEntryZipFile(JobEntryBase jeb) {
		super(jeb);
	}

	public Object clone() {
		JobEntryZipFile je = (JobEntryZipFile) super.clone();
		return je;
	}

	public String getXML() {
		StringBuffer retval = new StringBuffer(500);

		retval.append(super.getXML());
		retval.append("      ").append(
				XMLHandler.addTagValue("zipfilename", zipFilename));
		retval.append("      ").append(
				XMLHandler.addTagValue("compressionrate", compressionrate));
		retval.append("      ").append(
				XMLHandler.addTagValue("ifzipfileexists", ifzipfileexists));
		retval.append("      ").append(
				XMLHandler.addTagValue("wildcard", wildcard));
		retval.append("      ").append(
				XMLHandler.addTagValue("wildcardexclude", wildcardexclude));
		retval.append("      ").append(
				XMLHandler.addTagValue("sourcedirectory", sourcedirectory));
		retval.append("      ").append(
				XMLHandler.addTagValue("movetodirectory", movetodirectory));
		retval.append("      ").append(
				XMLHandler.addTagValue("afterzip", afterzip));
		retval.append("      ").append(
				XMLHandler.addTagValue("addfiletoresult", addfiletoresult));
		retval.append("      ").append(
				XMLHandler.addTagValue("isfromprevious", isfromprevious));
		retval.append("      ").append(
				XMLHandler
						.addTagValue("createparentfolder", createparentfolder));
		retval.append("      ").append(
				XMLHandler.addTagValue("adddate", adddate));
		retval.append("      ").append(
				XMLHandler.addTagValue("addtime", addtime));
		retval.append("      ").append(
				XMLHandler.addTagValue("SpecifyFormat", SpecifyFormat));
		retval.append("      ").append(
				XMLHandler.addTagValue("date_time_format", date_time_format));

		return retval.toString();
	}

	public void loadXML(Node entrynode, List<DatabaseMeta> databases,
			List<SlaveServer> slaveServers, Repository rep)
			throws KettleXMLException {
		try {
			super.loadXML(entrynode, databases, slaveServers);
			zipFilename = XMLHandler.getTagValue(entrynode, "zipfilename");
			compressionrate = Const.toInt(XMLHandler.getTagValue(entrynode,
					"compressionrate"), -1);
			ifzipfileexists = Const.toInt(XMLHandler.getTagValue(entrynode,
					"ifzipfileexists"), -1);
			afterzip = Const.toInt(XMLHandler
					.getTagValue(entrynode, "afterzip"), -1);
			wildcard = XMLHandler.getTagValue(entrynode, "wildcard");
			wildcardexclude = XMLHandler.getTagValue(entrynode,
					"wildcardexclude");
			sourcedirectory = XMLHandler.getTagValue(entrynode,
					"sourcedirectory");
			movetodirectory = XMLHandler.getTagValue(entrynode,
					"movetodirectory");
			addfiletoresult = "Y".equalsIgnoreCase(XMLHandler.getTagValue(
					entrynode, "addfiletoresult"));
			isfromprevious = "Y".equalsIgnoreCase(XMLHandler.getTagValue(
					entrynode, "isfromprevious"));
			createparentfolder = "Y".equalsIgnoreCase(XMLHandler.getTagValue(
					entrynode, "createparentfolder"));
			adddate = "Y".equalsIgnoreCase(XMLHandler.getTagValue(entrynode,
					"adddate"));
			addtime = "Y".equalsIgnoreCase(XMLHandler.getTagValue(entrynode,
					"addtime"));
			SpecifyFormat = "Y".equalsIgnoreCase(XMLHandler.getTagValue(
					entrynode, "SpecifyFormat"));
			date_time_format = XMLHandler.getTagValue(entrynode,
					"date_time_format");
		} catch (KettleXMLException xe) {
			throw new KettleXMLException(
					"Unable to load job entry of type 'zipfile' from XML node",
					xe);
		}
	}

	public void loadRep(Repository rep, long id_jobentry,
			List<DatabaseMeta> databases, List<SlaveServer> slaveServers)
			throws KettleException {

		try {
			super.loadRep(rep, id_jobentry, databases, slaveServers);
			zipFilename = rep.getJobEntryAttributeString(id_jobentry,
					"zipfilename");
			compressionrate = (int) rep.getJobEntryAttributeInteger(
					id_jobentry, "compressionrate");
			ifzipfileexists = (int) rep.getJobEntryAttributeInteger(
					id_jobentry, "ifzipfileexists");
			afterzip = (int) rep.getJobEntryAttributeInteger(id_jobentry,
					"afterzip");
			wildcard = rep.getJobEntryAttributeString(id_jobentry, "wildcard");
			wildcardexclude = rep.getJobEntryAttributeString(id_jobentry,
					"wildcardexclude");
			sourcedirectory = rep.getJobEntryAttributeString(id_jobentry,
					"sourcedirectory");
			movetodirectory = rep.getJobEntryAttributeString(id_jobentry,
					"movetodirectory");
			addfiletoresult = rep.getJobEntryAttributeBoolean(id_jobentry,
					"addfiletoresult");
			isfromprevious = rep.getJobEntryAttributeBoolean(id_jobentry,
					"isfromprevious");
			createparentfolder = rep.getJobEntryAttributeBoolean(id_jobentry,
					"createparentfolder");
			adddate = rep.getJobEntryAttributeBoolean(id_jobentry, "adddate");
			addtime = rep.getJobEntryAttributeBoolean(id_jobentry, "adddate");
			SpecifyFormat = rep.getJobEntryAttributeBoolean(id_jobentry,
					"SpecifyFormat");
			date_time_format = rep.getJobEntryAttributeString(id_jobentry,
					"date_time_format");

		} catch (KettleException dbe) {
			throw new KettleException(
					"Unable to load job entry of type 'zipfile' from the repository for id_jobentry="
							+ id_jobentry, dbe);
		}
	}

	public void saveRep(Repository rep, long id_job) throws KettleException {
		try {
			super.saveRep(rep, id_job);

			rep.saveJobEntryAttribute(id_job, getID(), "zipfilename",
					zipFilename);
			rep.saveJobEntryAttribute(id_job, getID(), "compressionrate",
					compressionrate);
			rep.saveJobEntryAttribute(id_job, getID(), "ifzipfileexists",
					ifzipfileexists);
			rep.saveJobEntryAttribute(id_job, getID(), "afterzip", afterzip);

			rep.saveJobEntryAttribute(id_job, getID(), "wildcard", wildcard);
			rep.saveJobEntryAttribute(id_job, getID(), "wildcardexclude",
					wildcardexclude);
			rep.saveJobEntryAttribute(id_job, getID(), "sourcedirectory",
					sourcedirectory);
			rep.saveJobEntryAttribute(id_job, getID(), "movetodirectory",
					movetodirectory);
			rep.saveJobEntryAttribute(id_job, getID(), "addfiletoresult",
					addfiletoresult);
			rep.saveJobEntryAttribute(id_job, getID(), "isfromprevious",
					isfromprevious);
			rep.saveJobEntryAttribute(id_job, getID(), "createparentfolder",
					createparentfolder);
			rep.saveJobEntryAttribute(id_job, getID(), "addtime", addtime);
			rep.saveJobEntryAttribute(id_job, getID(), "adddate", adddate);
			rep.saveJobEntryAttribute(id_job, getID(), "SpecifyFormat",
					SpecifyFormat);
			rep.saveJobEntryAttribute(id_job, getID(), "date_time_format",
					date_time_format);

		} catch (KettleDatabaseException dbe) {
			throw new KettleException(
					"Unable to save job entry of type 'zipfile' to the repository for id_job="
							+ id_job, dbe);
		}
	}

	private boolean createParentFolder(String filename) {
		// Check for parent folder
		FileObject parentfolder = null;

		boolean result = false;
		LogWriter log = LogWriter.getInstance();
		try {
			// Get parent folder
			parentfolder = KettleVFS.getFileObject(filename).getParent();

			if (!parentfolder.exists()) {
				if (log.isDetailed())
					log.logDetailed(toString(), "Folder"
							+ parentfolder.getName() + " does not exist !");
				parentfolder.createFolder();
				if (log.isDetailed())
					log.logDetailed(toString(), "Folder was created.");
			} else {
				if (log.isDetailed())
					log.logDetailed(toString(), "Folder"
							+ parentfolder.getName() + " exist !");
			}
			result = true;
		} catch (Exception e) {
			log.logError(toString(), "Couldn't created folder "
					+ parentfolder.getName());
		} finally {
			if (parentfolder != null) {
				try {
					parentfolder.close();
				} catch (Exception ex) {
				}
				;
			}
		}

		return result;
	}

	public boolean processRowFile(Job parentJob, Result result,
			String realZipfilename, String realWildcard,
			String realWildcardExclude, String realTargetdirectory,
			String realMovetodirectory, boolean createparentfolder) {
		LogWriter log = LogWriter.getInstance();
		boolean Fileexists = false;
		File tempFile = null;
		File fileZip = null;
		boolean resultat = false;
		boolean renameOk = false;
		boolean orginexist = false;

		// Check if target file/folder exists!
		FileObject OriginFile = null;
		ZipInputStream zin = null;
		byte[] buffer = null;
		FileOutputStream dest = null;
		BufferedOutputStream buff = null;
		org.apache.tools.zip.ZipOutputStream out = null;
		org.apache.tools.zip.ZipEntry entry = null;

		try {
			OriginFile = KettleVFS.getFileObject(realTargetdirectory);
			orginexist = OriginFile.exists();
		} catch (Exception e) {
		} finally {
			if (OriginFile != null) {
				try {
					OriginFile.close();
				} catch (IOException ex) {
				}
				;
			}
		}

		if (realZipfilename != null && orginexist) {

			FileObject fileObject = null;
			try {
				fileObject = KettleVFS.getFileObject(realZipfilename);

				// Check if Zip File exists
				if (fileObject.exists()) {
					Fileexists = true;
					if (log.isDebug())
						log
								.logDebug(
										toString(),
										Messages
												.getString("JobZipFiles.Zip_FileExists1.Label")
												+ realZipfilename
												+ Messages
														.getString("JobZipFiles.Zip_FileExists2.Label"));
				}
				// Let's see if we need to create parent folder of destination
				// zip filename
				if (createparentfolder) {
					createParentFolder(realZipfilename);
				}

				// Let's start the process now
				if (ifzipfileexists == 3 && Fileexists) {
					// the zip file exists and user want to Fail
					resultat = false;
				} else if (ifzipfileexists == 2 && Fileexists) {
					// the zip file exists and user want to do nothing
					if (addfiletoresult) {
						// Add file to result files name
						ResultFile resultFile = new ResultFile(
								ResultFile.FILE_TYPE_GENERAL, KettleVFS
										.getFileObject(realZipfilename),
								parentJob.getJobname(), toString());
						result.getResultFiles().put(
								resultFile.getFile().toString(), resultFile);
					}
					resultat = true;
				} else if (afterzip == 2 && realMovetodirectory == null) {
					// After Zip, Move files..User must give a destination
					// Folder
					resultat = false;
					log
							.logError(
									toString(),
									Messages
											.getString("JobZipFiles.AfterZip_No_DestinationFolder_Defined.Label"));
				} else
				// After Zip, Move files..User must give a destination Folder
				{
					// Let's see if we deal with file or folder
					String[] filelist = null;

					File f = new File(realTargetdirectory);
					if (f.isDirectory()) {
						// Target is a directory
						// Get all the files in the directory...
						filelist = f.list();
					} else {
						// Target is a file
						filelist = new String[1];
						filelist[0] = f.getName();
					}
					if (filelist.length == 0) {
						resultat = false;
						log.logError(toString(), Messages.getString(
								"JobZipFiles.Log.FolderIsEmpty",
								realTargetdirectory));
					} else if (!checkContainsFile(realTargetdirectory, filelist)) {
						resultat = false;
						log.logError(toString(), Messages.getString(
								"JobZipFiles.Log.NoFilesInFolder",
								realTargetdirectory));
					} else {
						if (ifzipfileexists == 0 && Fileexists) {
							// the zip file exists and user want to create new
							// one with unique name
							// Format Date

							// do we have already a .zip at the end?
							if (realZipfilename.toLowerCase().endsWith(".zip")) {
								// strip this off
								realZipfilename = realZipfilename.substring(0,
										realZipfilename.length() - 4);
							}

							realZipfilename = realZipfilename + "_"
									+ StringUtil.getFormattedDateTimeNow(true)
									+ ".zip";
							if (log.isDebug())
								log
										.logDebug(
												toString(),
												Messages
														.getString("JobZipFiles.Zip_FileNameChange1.Label")
														+ realZipfilename
														+ Messages
																.getString("JobZipFiles.Zip_FileNameChange1.Label"));
						} else if (ifzipfileexists == 1 && Fileexists) {
							// the zip file exists and user want to append
							// get a temp file
							fileZip = new File(realZipfilename);
							tempFile = File.createTempFile(fileZip.getName(),
									null);

							// delete it, otherwise we cannot rename existing
							// zip to it.
							tempFile.delete();

							renameOk = fileZip.renameTo(tempFile);

							if (!renameOk) {
								log
										.logError(
												toString(),
												Messages
														.getString("JobZipFiles.Cant_Rename_Temp1.Label")
														+ fileZip
																.getAbsolutePath()
														+ Messages
																.getString("JobZipFiles.Cant_Rename_Temp2.Label")
														+ tempFile
																.getAbsolutePath()
														+ Messages
																.getString("JobZipFiles.Cant_Rename_Temp3.Label"));
							}
							if (log.isDebug())
								log
										.logDebug(
												toString(),
												Messages
														.getString("JobZipFiles.Zip_FileAppend1.Label")
														+ realZipfilename
														+ Messages
																.getString("JobZipFiles.Zip_FileAppend2.Label"));
						}

						if (log.isDetailed())
							log
									.logDetailed(
											toString(),
											Messages
													.getString("JobZipFiles.Files_Found1.Label")
													+ filelist.length
													+ Messages
															.getString("JobZipFiles.Files_Found2.Label")
													+ realTargetdirectory
													+ Messages
															.getString("JobZipFiles.Files_Found3.Label"));

						Pattern pattern = null;
						Pattern patternexclude = null;
						// Let's prepare pattern..only if target is a folder !
						if (f.isDirectory()) {
							if (!Const.isEmpty(realWildcard)) {
								pattern = Pattern.compile(realWildcard);
							}

							if (!Const.isEmpty(realWildcardExclude)) {
								patternexclude = Pattern
										.compile(realWildcardExclude);
							}
						}

						// Prepare Zip File
						buffer = new byte[18024];
						dest = new FileOutputStream(realZipfilename);
						buff = new BufferedOutputStream(dest);
						out = new org.apache.tools.zip.ZipOutputStream(buff);
						HashSet<String> fileSet = new HashSet<String>();

						if (renameOk) {
							// User want to append files to existing Zip file
							// The idea is to rename the existing zip file to a
							// temporary file
							// and then adds all entries in the existing zip
							// along with the new files,
							// excluding the zip entries that have the same name
							// as one of the new files.

							zin = new ZipInputStream(new FileInputStream(
									tempFile));
							entry = (ZipEntry) zin.getNextEntry();

							while (entry != null) {
								String name = entry.getName();

								if (!fileSet.contains(name)) {

									// Add ZIP entry to output stream.
									out.putNextEntry(new ZipEntry(name));
									// Transfer bytes from the ZIP file to the
									// output file
									int len;
									while ((len = zin.read(buffer)) > 0) {
										out.write(buffer, 0, len);
									}

									fileSet.add(name);
								}
								entry = (ZipEntry) zin.getNextEntry();
							}
							// Close the streams
							zin.close();
						}

						// Set the method
						out
								.setMethod(org.apache.tools.zip.ZipOutputStream.DEFLATED);
						// Set the compression level
						if (compressionrate == 0) {
							out.setLevel(Deflater.NO_COMPRESSION);
						} else if (compressionrate == 1) {
							out.setLevel(Deflater.DEFAULT_COMPRESSION);
						}
						if (compressionrate == 2) {
							out.setLevel(Deflater.BEST_COMPRESSION);
						}
						if (compressionrate == 3) {
							out.setLevel(Deflater.BEST_SPEED);
						}
						// Specify Zipped files (After that we will move,delete
						// them...)
						String[] ZippedFiles = new String[filelist.length];
						int FileNum = 0;

						// Get the files in the list...
						for (int i = 0; i < filelist.length
								&& !parentJob.isStopped(); i++) {
							boolean getIt = true;
							boolean getItexclude = false;

							// First see if the file matches the regular
							// expression!
							// ..only if target is a folder !
							if (f.isDirectory()) {
								if (pattern != null) {
									Matcher matcher = pattern
											.matcher(filelist[i]);
									getIt = matcher.matches();
								}

								if (patternexclude != null) {
									Matcher matcherexclude = patternexclude
											.matcher(filelist[i]);
									getItexclude = matcherexclude.matches();
								}
							}
							// Get processing File
							String targetFilename = realTargetdirectory
									+ Const.FILE_SEPARATOR + filelist[i];
							if (f.isFile())
								targetFilename = realTargetdirectory;

							File file = new File(targetFilename);

							if (getIt && !getItexclude && !file.isDirectory()
									&& !fileSet.contains(filelist[i])) {

								// We can add the file to the Zip Archive
								if (log.isDebug())
									log
											.logDebug(
													toString(),
													Messages
															.getString("JobZipFiles.Add_FilesToZip1.Label")
															+ filelist[i]
															+ Messages
																	.getString("JobZipFiles.Add_FilesToZip2.Label")
															+ realTargetdirectory
															+ Messages
																	.getString("JobZipFiles.Add_FilesToZip3.Label"));

								// Associate a file input stream for the current
								// file
								FileInputStream in = new FileInputStream(
										targetFilename);
								// Add ZIP entry to output stream.
								out.putNextEntry(new ZipEntry(filelist[i]));

								int len;
								while ((len = in.read(buffer)) > 0) {
									out.write(buffer, 0, len);
								}
								out.flush();
								out.closeEntry();

								// Close the current file input stream
								in.close();

								// Get Zipped File
								ZippedFiles[FileNum] = filelist[i];
								FileNum = FileNum + 1;
							}
						}
						// Close the ZipOutPutStream
						out.close();
						buff.close();
						dest.close();

						if (log.isBasic())
							log.logBasic(toString(), Messages.getString(
									"JobZipFiles.Log.TotalZippedFiles", ""
											+ ZippedFiles.length));
						// Delete Temp File
						if (tempFile != null) {
							tempFile.delete();
						}

						// -----Get the list of Zipped Files and Move or Delete
						// Them
						if (afterzip == 1 || afterzip == 2) {
							// iterate through the array of Zipped files
							for (int i = 0; i < ZippedFiles.length; i++) {
								if (ZippedFiles[i] != null) {
									// Delete File
									FileObject fileObjectd = KettleVFS
											.getFileObject(realTargetdirectory
													+ Const.FILE_SEPARATOR
													+ ZippedFiles[i]);
									if (f.isFile())
										fileObjectd = KettleVFS
												.getFileObject(realTargetdirectory);

									// Here gc() is explicitly called if e.g.
									// createfile is used in the same
									// job for the same file. The problem is
									// that after creating the file the
									// file object is not properly garbaged
									// collected and thus the file cannot
									// be deleted anymore. This is a known
									// problem in the JVM.

									System.gc();

									// Here we can move, delete files
									if (afterzip == 1) {

										// Delete File
										boolean deleted = fileObjectd.delete();
										if (!deleted) {
											resultat = false;
											log
													.logError(
															toString(),
															Messages
																	.getString("JobZipFiles.Cant_Delete_File1.Label")
																	+ realTargetdirectory
																	+ Const.FILE_SEPARATOR
																	+ ZippedFiles[i]
																	+ Messages
																			.getString("JobZipFiles.Cant_Delete_File2.Label"));

										}
										// File deleted
										if (log.isDebug())
											log
													.logDebug(
															toString(),
															Messages
																	.getString("JobZipFiles.File_Deleted1.Label")
																	+ realTargetdirectory
																	+ Const.FILE_SEPARATOR
																	+ ZippedFiles[i]
																	+ Messages
																			.getString("JobZipFiles.File_Deleted2.Label"));
									} else if (afterzip == 2) {
										// Move File
										try {
											FileObject fileObjectm = KettleVFS
													.getFileObject(realMovetodirectory
															+ Const.FILE_SEPARATOR
															+ ZippedFiles[i]);
											fileObjectd.moveTo(fileObjectm);
										} catch (IOException e) {
											log
													.logError(
															toString(),
															Messages
																	.getString("JobZipFiles.Cant_Move_File1.Label")
																	+ ZippedFiles[i]
																	+ Messages
																			.getString("JobZipFiles.Cant_Move_File2.Label")
																	+ e
																			.getMessage());
											resultat = false;
										}
										// File moved
										if (log.isDebug())
											log
													.logDebug(
															toString(),
															Messages
																	.getString("JobZipFiles.File_Moved1.Label")
																	+ ZippedFiles[i]
																	+ Messages
																			.getString("JobZipFiles.File_Moved2.Label"));
									}
								}
							}
						}

						if (addfiletoresult) {
							// Add file to result files name
							ResultFile resultFile = new ResultFile(
									ResultFile.FILE_TYPE_GENERAL, KettleVFS
											.getFileObject(realZipfilename),
									parentJob.getJobname(), toString());
							result.getResultFiles()
									.put(resultFile.getFile().toString(),
											resultFile);
						}

						resultat = true;
					}
				}
			} catch (Exception e) {
				log
						.logError(
								toString(),
								Messages
										.getString("JobZipFiles.Cant_CreateZipFile1.Label")
										+ realZipfilename
										+ Messages
												.getString("JobZipFiles.Cant_CreateZipFile2.Label")
										+ e.getMessage());
				// result.setResult( false );
				// result.setNrErrors(1);
				resultat = false;
			} finally {
				if (fileObject != null) {
					try {
						fileObject.close();
					} catch (IOException ex) {
					}
					;
				}
				// Close the ZipOutPutStream
				try {
					if (out != null)
						out.close();
					if (buff != null)
						buff.close();
					if (dest != null)
						dest.close();
					if (zin != null)
						zin.close();
					if (entry != null)
						entry = null;

				} catch (IOException ex) {
				}
				;
			}
		} else {
			resultat = true;
			if (realZipfilename == null)
				log.logError(toString(), Messages
						.getString("JobZipFiles.No_ZipFile_Defined.Label"));
			if (!orginexist)
				log.logError(toString(), Messages.getString(
						"JobZipFiles.No_FolderCible_Defined.Label",
						realTargetdirectory));
		}
		// return a verifier
		return resultat;
	}

	private boolean checkContainsFile(String realTragetDirectory,
			String[] filelist) {
		boolean retval = false;
		for (int i = 0; i < filelist.length; i++) {
			File file = new File(realTragetDirectory + Const.FILE_SEPARATOR
					+ filelist[i]);
			if ((file.exists() && file.isFile()))
				retval = true;
		}
		return retval;
	}

	public Result execute(Result previousResult, int nr, Repository rep,
			Job parentJob) {
		LogWriter log = LogWriter.getInstance();
		Result result = previousResult;
		List<RowMetaAndData> rows = result.getRows();

		// reset values
		String realZipfilename = null;
		String realWildcard = null;
		String realWildcardExclude = null;
		String realTargetdirectory = null;
		String realMovetodirectory = environmentSubstitute(movetodirectory);

		// arguments from previous

		if (isfromprevious) {
			if (log.isDetailed())
				log.logDetailed(toString(), Messages.getString(
						"JobZipFiles.ArgFromPrevious.Found",
						(rows != null ? rows.size() : 0) + ""));
		}
		if (isfromprevious && rows != null) {
			try {
				for (int iteration = 0; iteration < rows.size()
						&& !parentJob.isStopped(); iteration++) {

					// get arguments from previous job entry
					RowMetaAndData resultRow = rows.get(iteration);
					// get target directory
					realTargetdirectory = resultRow.getString(0, null);
					if (!Const.isEmpty(realTargetdirectory)) {
						// get wildcard to include
						if (!Const.isEmpty(resultRow.getString(1, null)))
							realWildcard = resultRow.getString(1, null);
						// get wildcard to exclude
						if (!Const.isEmpty(resultRow.getString(2, null)))
							realWildcardExclude = resultRow.getString(2, null);

						// get destination zip file
						realZipfilename = resultRow.getString(3, null);
						if (!Const.isEmpty(realZipfilename)) {
							if (!processRowFile(parentJob, result,
									realZipfilename, realWildcard,
									realWildcardExclude, realTargetdirectory,
									realMovetodirectory, createparentfolder)) {
								result.setResult(false);
								return result;
							}
						} else {
							log
									.logError(toString(),
											"destination zip filename is empty! Ignoring row...");
						}

					} else {
						log.logError(toString(),
								"Target directory is empty! Ignoring row...");
					}

				}
			} catch (Exception e) {
				log.logError(toString(), "Erreur during process!");
			}
		} else if (!isfromprevious) {
			if (!Const.isEmpty(sourcedirectory)) {
				// get values from job entry
				realZipfilename = getFullFilename(
						environmentSubstitute(zipFilename), adddate, addtime,
						SpecifyFormat, date_time_format);
				realWildcard = environmentSubstitute(wildcard);
				realWildcardExclude = environmentSubstitute(wildcardexclude);
				realTargetdirectory = environmentSubstitute(sourcedirectory);

				result.setResult(processRowFile(parentJob, result,
						realZipfilename, realWildcard, realWildcardExclude,
						realTargetdirectory, realMovetodirectory,
						createparentfolder));
			} else {
				log.logError(toString(),
						"Source folder/file is empty! Ignoring row...");
			}
		}

		// End
		return result;
	}

	public String getFullFilename(String filename, boolean add_date,
			boolean add_time, boolean specify_format, String datetime_folder) {
		String retval = "";
		if (Const.isEmpty(filename))
			return null;

		// Replace possible environment variables...
		String realfilename = environmentSubstitute(filename);
		int lenstring = realfilename.length();
		int lastindexOfDot = realfilename.lastIndexOf('.');
		if (lastindexOfDot == -1)
			lastindexOfDot = lenstring;

		retval = realfilename.substring(0, lastindexOfDot);

		SimpleDateFormat daf = new SimpleDateFormat();
		Date now = new Date();

		if (specify_format && !Const.isEmpty(datetime_folder)) {
			daf.applyPattern(datetime_folder);
			String dt = daf.format(now);
			retval += dt;
		} else {
			if (add_date) {
				daf.applyPattern("yyyyMMdd");
				String d = daf.format(now);
				retval += "_" + d;
			}
			if (add_time) {
				daf.applyPattern("HHmmssSSS");
				String t = daf.format(now);
				retval += "_" + t;
			}
		}
		retval += realfilename.substring(lastindexOfDot, lenstring);
		return retval;

	}

	public boolean evaluates() {
		return true;
	}

	public void setZipFilename(String zipFilename) {
		this.zipFilename = zipFilename;
	}

	public void setWildcard(String wildcard) {
		this.wildcard = wildcard;
	}

	public void setWildcardExclude(String wildcardexclude) {
		this.wildcardexclude = wildcardexclude;
	}

	public void setSourceDirectory(String sourcedirectory) {
		this.sourcedirectory = sourcedirectory;
	}

	public void setMoveToDirectory(String movetodirectory) {
		this.movetodirectory = movetodirectory;
	}

	public String getSourceDirectory() {
		return sourcedirectory;
	}

	public String getMoveToDirectory() {
		return movetodirectory;
	}

	public String getZipFilename() {
		return zipFilename;
	}

	public String getWildcard() {
		return wildcard;
	}

	public String getWildcardExclude() {
		return wildcardexclude;
	}

	public void setAddFileToResult(boolean addfiletoresultin) {
		this.addfiletoresult = addfiletoresultin;
	}

	public boolean isAddFileToResult() {
		return addfiletoresult;
	}

	public void setcreateparentfolder(boolean createparentfolder) {
		this.createparentfolder = createparentfolder;
	}

	public void setDateInFilename(boolean adddate) {
		this.adddate = adddate;
	}

	public boolean isDateInFilename() {
		return adddate;
	}

	public void setTimeInFilename(boolean addtime) {
		this.addtime = addtime;
	}

	public boolean isTimeInFilename() {
		return addtime;
	}

	public boolean isSpecifyFormat() {
		return SpecifyFormat;
	}

	public void setSpecifyFormat(boolean SpecifyFormat) {
		this.SpecifyFormat = SpecifyFormat;
	}

	public String getDateTimeFormat() {
		return date_time_format;
	}

	public void setDateTimeFormat(String date_time_format) {
		this.date_time_format = date_time_format;
	}

	public boolean getcreateparentfolder() {
		return createparentfolder;
	}

	public void setDatafromprevious(boolean isfromprevious) {
		this.isfromprevious = isfromprevious;
	}

	public boolean getDatafromprevious() {
		return isfromprevious;
	}

	@Override
	public void check(List<CheckResultInterface> remarks, JobMeta jobMeta) {
		ValidatorContext ctx1 = new ValidatorContext();
		putVariableSpace(ctx1, getVariables());
		putValidators(ctx1, notBlankValidator(), fileDoesNotExistValidator());
		if (3 == ifzipfileexists) {
			// execute method fails if the file already exists; we should too
			putFailIfExists(ctx1, true);
		}
		andValidator().validate(this, "zipFilename", remarks, ctx1);//$NON-NLS-1$

		if (2 == afterzip) {
			// setting says to move
			andValidator()
					.validate(
							this,
							"moveToDirectory", remarks, putValidators(notBlankValidator())); //$NON-NLS-1$
		}

		andValidator().validate(this,
				"sourceDirectory", remarks, putValidators(notBlankValidator())); //$NON-NLS-1$

	}

	@Override
	public void setInfo(Map<String, String[]> p, String id,
			List<? extends SharedObjectInterface> databases) {
		// TODO Auto-generated method stub
		this.date_time_format = BaseStepMeta.parameterToString(p.get(id
				+ ".date_time_format"));
		this.SpecifyFormat = BaseStepMeta.parameterToBoolean(p.get(id
				+ ".SpecifyFormat"));
		this.adddate = BaseStepMeta.parameterToBoolean(p.get(id + ".adddate"));
		this.addfiletoresult = BaseStepMeta.parameterToBoolean(p.get(id
				+ ".addfiletoresult"));
		this.addtime = BaseStepMeta.parameterToBoolean(p.get(id + ".addtime"));
		this.createparentfolder = BaseStepMeta.parameterToBoolean(p.get(id
				+ ".createparentfolder"));
		this.isfromprevious = BaseStepMeta.parameterToBoolean(p.get(id
				+ ".isfromprevious"));
		this.sourcedirectory = BaseStepMeta.parameterToString(p.get(id
				+ ".sourcedirectory"));
		this.wildcard = BaseStepMeta.parameterToString(p.get(id + ".wildcard"));
		this.wildcardexclude = BaseStepMeta.parameterToString(p.get(id
				+ ".wildcardexclude"));
		this.zipFilename = BaseStepMeta.parameterToString(p.get(id
				+ ".zipFilename"));
//		this.zipFilename += ".zip";
		this.date_time_format = BaseStepMeta.parameterToString(p.get(id
				+ ".date_time_format"));
		this.date_time_format = BaseStepMeta.parameterToString(p.get(id
				+ ".date_time_format"));

		this.compressionrate = Integer.parseInt(BaseStepMeta
				.parameterToString(p.get(id + ".compressionrate")));
		this.afterzip = Integer.parseInt(BaseStepMeta.parameterToString(p
				.get(id + ".afterzip")));
		this.ifzipfileexists = Integer.parseInt(BaseStepMeta
				.parameterToString(p.get(id + ".ifzipfileexists")));
		this.movetodirectory = BaseStepMeta.parameterToString(p.get(id
				+ ".movetodirectory"));
	}
}
