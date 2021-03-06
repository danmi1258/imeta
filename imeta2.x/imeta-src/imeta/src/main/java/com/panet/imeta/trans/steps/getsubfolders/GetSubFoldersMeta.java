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

/* 
 * 
 * Created on 4-apr-2003
 * 
 */

package com.panet.imeta.trans.steps.getsubfolders;

import java.util.List;
import java.util.Map;

import org.w3c.dom.Node;

import com.panet.imeta.core.CheckResult;
import com.panet.imeta.core.CheckResultInterface;
import com.panet.imeta.core.Const;
import com.panet.imeta.core.Counter;
import com.panet.imeta.core.database.DatabaseMeta;
import com.panet.imeta.core.exception.KettleException;
import com.panet.imeta.core.exception.KettleStepException;
import com.panet.imeta.core.exception.KettleXMLException;
import com.panet.imeta.core.fileinput.FileInputList;
import com.panet.imeta.core.row.RowMetaInterface;
import com.panet.imeta.core.row.ValueMeta;
import com.panet.imeta.core.row.ValueMetaInterface;
import com.panet.imeta.core.variables.VariableSpace;
import com.panet.imeta.core.xml.XMLHandler;
import com.panet.imeta.repository.Repository;
import com.panet.imeta.shared.SharedObjectInterface;
import com.panet.imeta.trans.Trans;
import com.panet.imeta.trans.TransMeta;
import com.panet.imeta.trans.step.BaseStepMeta;
import com.panet.imeta.trans.step.StepDataInterface;
import com.panet.imeta.trans.step.StepInterface;
import com.panet.imeta.trans.step.StepMeta;
import com.panet.imeta.trans.step.StepMetaInterface;

/**
 * @author Samatar
 * @since 18-July-2008
 */

public class GetSubFoldersMeta extends BaseStepMeta implements StepMetaInterface
{
	public static final String[] RequiredFoldersDesc = new String[] { Messages.getString("System.Combo.No"), Messages.getString("System.Combo.Yes") };
	public static final String[] RequiredFoldersCode = new String[] {"N", "Y"};
	
	public static final String NO = "N";

	/** Array of filenames */
	private String             folderName[];
    
	/** Array of boolean values as string, indicating if a file is required. */
	private String             folderRequired[];
	
	/** Flag indicating that a row number field should be included in the output */
	private  boolean includeRowNumber;
	
	/** The name of the field in the output containing the row number*/
	private  String  rowNumberField;
	
	/** The name of the field in the output containing the foldername */
	private String dynamicFoldernameField;
	
	/** folder name from previous fields **/
	private boolean isFoldernameDynamic;
	
	/** The maximum number or lines to read */
	private  long  rowLimit;
	
	public GetSubFoldersMeta()
	{
		super(); // allocate BaseStepMeta
	}
    public String getRequiredFilesDesc(String tt)
    {
   	 if(Const.isEmpty(tt)) return RequiredFoldersDesc[0]; 
		if(tt.equalsIgnoreCase(RequiredFoldersCode[1]))
			return RequiredFoldersDesc[1];
		else
			return RequiredFoldersDesc[0]; 
    }

    /**
     * @return Returns the rowNumberField.
     */
    public String getRowNumberField()
    {
        return rowNumberField;
    }
    /**
     * @param dynamicFoldernameField The dynamic foldername field to set.
     */
    public void setDynamicFoldernameField(String dynamicFoldernameField)
    {
        this.dynamicFoldernameField = dynamicFoldernameField;
    }
    
    
    /**
     * @param rowNumberField The rowNumberField to set.
     */
    public void setRowNumberField(String rowNumberField)
    {
        this.rowNumberField = rowNumberField;
    }
    
    /**
     * @return Returns the dynamic folder field (from previous steps)
     */
    public String getDynamicFoldernameField()
    {
        return dynamicFoldernameField;
    }   
    
    
    /**
     * @return Returns the includeRowNumber.
     */
    public boolean includeRowNumber()
    {
        return includeRowNumber;
    }
    
    /**
     * @return Returns the dynamic foldername flag.
     */
    public boolean isFoldernameDynamic()
    {
        return isFoldernameDynamic;
    }
    /**
     * @param isFoldernameDynamic The isFoldernameDynamic to set.
     */
    public void setFolderField(boolean isFoldernameDynamic)
    {
        this.isFoldernameDynamic = isFoldernameDynamic;
    }
  
    /**
     * @param includeRowNumber The includeRowNumber to set.
     */
    public void setIncludeRowNumber(boolean includeRowNumber)
    {
        this.includeRowNumber = includeRowNumber;
    }
    
	/**
	 * @return Returns the folderRequired.
	 */
	public String[] getFolderRequired() 
	{
		return folderRequired;
	}
    public String getRequiredFoldersCode(String tt)
    {
   	if(tt==null) return RequiredFoldersCode[0]; 
		if(tt.equals(RequiredFoldersDesc[1]))
			return RequiredFoldersCode[1];
		else
			return RequiredFoldersCode[0]; 
    }
    
	/**
	 * @param folderRequired The folderRequired to set.
	 */

	public void setFolderRequired(String[] folderRequiredin) {
		for (int i=0;i<folderRequiredin.length;i++)
		{
			this.folderRequired[i] = getRequiredFoldersCode(folderRequiredin[i]);
		}
	}

	/**
	 * @return Returns the folderName.
	 */
	public String[] getFolderName()
	{
		return folderName;
	}

	/**
	 * @param folderName The folderName to set.
	 */
	public void setFolderName(String[] folderName)
	{
		this.folderName = folderName;
	}
    
    /**
     * @return Returns the rowLimit.
     */
    public long getRowLimit()
    {
        return rowLimit;
    }
    
    /**
     * @param rowLimit The rowLimit to set.
     */
    public void setRowLimit(long rowLimit)
    {
        this.rowLimit = rowLimit;
    }
	
	public void loadXML(Node stepnode, List<DatabaseMeta> databases, Map<String, Counter> counters) throws KettleXMLException
	{
		readData(stepnode);
	}

	public Object clone()
	{
		GetSubFoldersMeta retval = (GetSubFoldersMeta) super.clone();

		int nrfiles = folderName.length;

		retval.allocate(nrfiles);
		
        for (int i = 0; i < nrfiles; i++)
        {
            retval.folderName[i] = folderName[i];
            retval.folderRequired[i] = folderRequired[i];
        }

		return retval;
	}

	public void allocate(int nrfiles)
	{
		folderName = new String[nrfiles];
		folderRequired = new String[nrfiles];
	}

	public void setDefault()
	{
		int nrfiles = 0;
		isFoldernameDynamic=false;
		includeRowNumber = false;
		rowNumberField   = "";
		dynamicFoldernameField ="";
		
		allocate(nrfiles);

		for (int i = 0; i < nrfiles; i++)
		{
			folderName[i] = "folderName" + (i + 1);
			folderRequired[i] = NO;
		}
	}

	public void getFields(RowMetaInterface row, String name, RowMetaInterface[] info, StepMeta nextStep, VariableSpace space) throws KettleStepException
	{
        
		// the folderName
		ValueMetaInterface folderName = new ValueMeta("folderName",ValueMeta.TYPE_STRING);
		folderName.setLength(500);
		folderName.setPrecision(-1);
		folderName.setOrigin(name);
		row.addValueMeta(folderName);

		// the short folderName
		ValueMetaInterface short_folderName = new ValueMeta("short_folderName",ValueMeta.TYPE_STRING);
		short_folderName.setLength(500);
		short_folderName.setPrecision(-1);
		short_folderName.setOrigin(name);
		row.addValueMeta(short_folderName);
		
		// the path
		ValueMetaInterface path = new ValueMeta("path",ValueMeta.TYPE_STRING);
		path.setLength(500);
		path.setPrecision(-1);
		path.setOrigin(name);
		row.addValueMeta(path);
		
		// the ishidden     
		ValueMetaInterface ishidden = new ValueMeta("ishidden",ValueMeta.TYPE_BOOLEAN);
		ishidden.setOrigin(name);
		row.addValueMeta(ishidden);
              
		// the isreadable     
		ValueMetaInterface isreadable = new ValueMeta("isreadable",ValueMeta.TYPE_BOOLEAN);
		isreadable.setOrigin(name);
		row.addValueMeta(isreadable);
        
		// the iswriteable     
		ValueMetaInterface iswriteable = new ValueMeta("iswriteable",ValueMeta.TYPE_BOOLEAN);
		iswriteable.setOrigin(name);
		row.addValueMeta(iswriteable);  
                
		// the lastmodifiedtime     
		ValueMetaInterface lastmodifiedtime = new ValueMeta("lastmodifiedtime",ValueMeta.TYPE_DATE);
		lastmodifiedtime.setOrigin(name);
		row.addValueMeta(lastmodifiedtime);  
		
		// the uri     
		ValueMetaInterface uri = new ValueMeta("uri", ValueMeta.TYPE_STRING);
		uri.setOrigin(name);
		row.addValueMeta(uri); 
        
        
		// the rooturi     
		ValueMetaInterface rooturi = new ValueMeta("rooturi", ValueMeta.TYPE_STRING);
		rooturi.setOrigin(name);
		row.addValueMeta(rooturi); 
        
		// childrens
		ValueMetaInterface childrens = new ValueMeta(space.environmentSubstitute("childrens"), ValueMeta.TYPE_INTEGER);
		childrens.setLength(ValueMetaInterface.DEFAULT_INTEGER_LENGTH, 0);
		childrens.setOrigin(name);
		row.addValueMeta(childrens);
		
		if (includeRowNumber)
		{
			ValueMetaInterface v = new ValueMeta(space.environmentSubstitute(rowNumberField), ValueMeta.TYPE_INTEGER);
			v.setLength(ValueMetaInterface.DEFAULT_INTEGER_LENGTH, 0);
			v.setOrigin(name);
			row.addValueMeta(v);
		}
 
	}

	public String getXML()
	{
		StringBuffer retval = new StringBuffer(300);
		
		retval.append("    ").append(XMLHandler.addTagValue("rownum",          includeRowNumber));
	    retval.append("    ").append(XMLHandler.addTagValue("foldername_dynamic",       isFoldernameDynamic));
	    retval.append("    ").append(XMLHandler.addTagValue("rownum_field",    rowNumberField));
        retval.append("    ").append(XMLHandler.addTagValue("foldername_field",  dynamicFoldernameField));
        retval.append("    ").append(XMLHandler.addTagValue("limit", rowLimit));
		retval.append("    <file>").append(Const.CR);
		
		for (int i = 0; i < folderName.length; i++)
		{
			retval.append("      ").append(XMLHandler.addTagValue("name", folderName[i]));
			retval.append("      ").append(XMLHandler.addTagValue("file_required", folderRequired[i]));
		}
		retval.append("    </file>").append(Const.CR);

		return retval.toString();
	}

	private void readData(Node stepnode) throws KettleXMLException
	{
		try
		{
			includeRowNumber  = "Y".equalsIgnoreCase(XMLHandler.getTagValue(stepnode, "rownum"));
			isFoldernameDynamic  = "Y".equalsIgnoreCase(XMLHandler.getTagValue(stepnode, "foldername_dynamic"));
			rowNumberField    = XMLHandler.getTagValue(stepnode, "rownum_field");
			dynamicFoldernameField    = XMLHandler.getTagValue(stepnode, "foldername_Field");
			
			// Is there a limit on the number of rows we process?
			rowLimit = Const.toLong(XMLHandler.getTagValue(stepnode, "limit"), 0L);
			
			Node filenode = XMLHandler.getSubNode(stepnode, "file");
			int nrfiles   = XMLHandler.countNodes(filenode, "name");
				
			allocate(nrfiles);

			for (int i = 0; i < nrfiles; i++)
			{
				Node folderNamenode     = XMLHandler.getSubNodeByNr(filenode, "name", i);
				Node folderRequirednode = XMLHandler.getSubNodeByNr(filenode, "file_required", i);
				folderName[i]           = XMLHandler.getNodeValue(folderNamenode);
				folderRequired[i]       = XMLHandler.getNodeValue(folderRequirednode);
			}
		}
		catch (Exception e)
		{
			throw new KettleXMLException("Unable to load step info from XML", e);
		}
	}

	@Override
	public void setInfo(Map<String, String[]> p, String id,
			List<? extends SharedObjectInterface> databases) {
		dynamicFoldernameField = BaseStepMeta.parameterToString(p.get(id + ".dynamicFoldernameField"));
		includeRowNumber = BaseStepMeta.parameterToBoolean(p.get(id + ".includeRowNumber"));
		isFoldernameDynamic = BaseStepMeta.parameterToBoolean(p.get(id + ".isFoldernameDynamic"));
		rowNumberField = BaseStepMeta.parameterToString(p.get(id + ".rowNumberField"));
		rowLimit = BaseStepMeta.parameterToLong(p.get(id + ".rowLimit"));
		
		String[] folderName = p.get(id + "_files.folderName");
		String[] folderRequired = p.get(id + "_files.folderRequired");

		this.folderName = (folderName != null) ? folderName : (new String[0]);
		this.folderRequired = (folderRequired != null) ? folderRequired : (new String[0]);
	}	
	
	public void readRep(Repository rep, long id_step, List<DatabaseMeta> databases, Map<String, Counter> counters) throws KettleException
	{
		try
		{
			int nrfiles = rep.countNrStepAttributes(id_step, "file_name");
			
			dynamicFoldernameField  = rep.getStepAttributeString(id_step, "foldername_Field");
			
			includeRowNumber  = rep.getStepAttributeBoolean(id_step, "rownum");
			isFoldernameDynamic  = rep.getStepAttributeBoolean(id_step, "foldername_dynamic");
			rowNumberField    = rep.getStepAttributeString (id_step, "rownum_field");
			rowLimit          = rep.getStepAttributeInteger(id_step, "limit");
						
			allocate(nrfiles);

			for (int i = 0; i < nrfiles; i++)
			{
				folderName[i] = rep.getStepAttributeString(id_step, i, "file_name");
				folderRequired[i] = rep.getStepAttributeString(id_step, i, "file_required");
			}
		}
		catch (Exception e)
		{
			throw new KettleException("Unexpected error reading step information from the repository", e);
		}
	}

	public void saveRep(Repository rep, long id_transformation, long id_step) throws KettleException
	{
		try
		{			
			
			rep.saveStepAttribute(id_transformation, id_step, "rownum",          includeRowNumber);
			rep.saveStepAttribute(id_transformation, id_step, "foldername_dynamic", isFoldernameDynamic);
			rep.saveStepAttribute(id_transformation, id_step, "foldername_field",    dynamicFoldernameField);
			
			rep.saveStepAttribute(id_transformation, id_step, "rownum_field",    rowNumberField);
			rep.saveStepAttribute(id_transformation, id_step, "limit",           rowLimit);
			
			for (int i = 0; i < folderName.length; i++)
			{
				rep.saveStepAttribute(id_transformation, id_step, i, "file_name", folderName[i]);
				rep.saveStepAttribute(id_transformation, id_step, i, "file_required", folderRequired[i]);
			}
		}
		catch (Exception e)
		{
			throw new KettleException("Unable to save step information to the repository for id_step=" + id_step, e);
		}
	}

	public FileInputList getFolderList(VariableSpace space)
	{
		return FileInputList.createFolderList(space, folderName, folderRequired);
	}

	public FileInputList getDynamicFolderList(VariableSpace space, String[] folderName,String[] folderRequired)
	{
		return FileInputList.createFolderList(space, folderName, folderRequired);
	}

	public void check(List<CheckResultInterface> remarks, TransMeta transMeta, StepMeta stepinfo, RowMetaInterface prev, String input[], String output[], RowMetaInterface info)
	{
		CheckResult cr;

		// See if we get input...
		if(isFoldernameDynamic)
		{
			if (input.length > 0)
				cr = new CheckResult(CheckResultInterface.TYPE_RESULT_OK, Messages.getString("GetSubFoldersMeta.CheckResult.InputOk"), stepinfo);
			else
				cr = new CheckResult(CheckResultInterface.TYPE_RESULT_ERROR, Messages.getString("GetSubFoldersMeta.CheckResult.InputErrorKo"), stepinfo);
			remarks.add(cr);
			
			if(Const.isEmpty(dynamicFoldernameField))
				cr = new CheckResult(CheckResultInterface.TYPE_RESULT_ERROR, Messages.getString("GetSubFoldersMeta.CheckResult.FolderFieldnameMissing"), stepinfo);
			else
				cr = new CheckResult(CheckResultInterface.TYPE_RESULT_OK, Messages.getString("GetSubFoldersMeta.CheckResult.FolderFieldnameOk"), stepinfo);
			
			remarks.add(cr);
		}else
		{
			if (input.length > 0)
				cr = new CheckResult(CheckResultInterface.TYPE_RESULT_ERROR, Messages.getString("GetSubFoldersMeta.CheckResult.NoInputError"), stepinfo);
			else
				cr = new CheckResult(CheckResultInterface.TYPE_RESULT_OK, Messages.getString("GetSubFoldersMeta.CheckResult.NoInputOk"), stepinfo);
			remarks.add(cr);
			// check specified folder names
			FileInputList fileList = getFolderList(transMeta);
			if (fileList.nrOfFiles() == 0)
			{
				cr = new CheckResult(CheckResultInterface.TYPE_RESULT_ERROR, Messages.getString("GetSubFoldersMeta.CheckResult.ExpectedFoldersError"), stepinfo);
				remarks.add(cr);
			}
			else
			{
				cr = new CheckResult(CheckResultInterface.TYPE_RESULT_OK, Messages.getString("GetSubFoldersMeta.CheckResult.ExpectedFilesOk", ""+fileList.nrOfFiles()), stepinfo);
				remarks.add(cr);
			}
		}

	}
  
	public StepInterface getStep(StepMeta stepMeta, StepDataInterface stepDataInterface, int cnr, TransMeta transMeta, Trans trans)
	{
		return new GetSubFolders(stepMeta, stepDataInterface, cnr, transMeta, trans);
	}

	public StepDataInterface getStepData()
	{
		return new GetSubFoldersData();
	}
	
}
