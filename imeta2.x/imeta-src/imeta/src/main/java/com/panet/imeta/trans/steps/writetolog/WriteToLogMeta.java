 /* Copyright (c) 2007 Pentaho Corporation.  All rights reserved. 
 * This software was developed by Pentaho Corporation and is provided under the terms 
 * of the GNU Lesser General Public License, Version 2.1. You may not use 
 * this file except in compliance with the license. If you need a copy of the license, 
 * please go to http://www.gnu.org/licenses/lgpl-2.1.txt. The Original Code is Pentaho 
 * Data Integration.  The Initial Developer is Samatar HASSAN.
 *
 * Software distributed under the GNU Lesser Public License is distributed on an "AS IS" 
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or  implied. Please refer to 
 * the license for the specific language governing your rights and limitations.*/

package com.panet.imeta.trans.steps.writetolog;

import java.util.List;
import java.util.Map;

import org.w3c.dom.Node;

import com.panet.imeta.core.CheckResult;
import com.panet.imeta.core.CheckResultInterface;
import com.panet.imeta.core.Const;
import com.panet.imeta.core.Counter;
import com.panet.imeta.core.database.DatabaseMeta;
import com.panet.imeta.core.exception.KettleException;
import com.panet.imeta.core.exception.KettleXMLException;
import com.panet.imeta.core.row.RowMetaInterface;
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


/*
 * Created on 30-06-2008
 *
 */


public class WriteToLogMeta extends BaseStepMeta implements StepMetaInterface
{
	public static final String STEP_ATTRIBUTE_LOGLEVEL = "loglevel";
	public static final String STEP_ATTRIBUTE_DISPLAYHEADER = "displayHeader";
//	loglevel = rep.getStepAttributeString(id_step, "loglevel");
//	displayHeader = rep.getStepAttributeBoolean(id_step, "displayHeader");
    /** by which fields to display? */
    private String  fieldName[];
	
    public static String logLevelCodes[] = 
	{			
		"log_level_nothing",
		"log_level_error",
		"log_level_minimal",
		"log_level_basic",
		"log_level_detailed",
		"log_level_debug",
		"log_level_rowlevel"
	};
    
    private boolean displayHeader;
    
    private String loglevel;
    
	public WriteToLogMeta()
	{
		super(); // allocate BaseStepMeta
	}

	public void setLogLevel(int i)
	{
		loglevel=logLevelCodes[i];
	}

	public String getLogLevel(){
		return loglevel;
	}
	
	public int getLogLevelByDesc()
	{
		if(loglevel==null) return 3;
		int retval;
		if(loglevel.equals(logLevelCodes[0]))
			retval=0;
		else if(loglevel.equals(logLevelCodes[1]))
			retval=1;
		else if(loglevel.equals(logLevelCodes[2]))
			retval=2;
		else if(loglevel.equals(logLevelCodes[3]))
			retval=3;
		else if(loglevel.equals(logLevelCodes[4]))
			retval=4;
		else if(loglevel.equals(logLevelCodes[5]))
			retval=5;
		else
			retval=6;
		return retval;
	}
	public void loadXML(Node stepnode, List<DatabaseMeta> databases, Map<String,Counter> counters)
	throws KettleXMLException
	{
		readData(stepnode);
	}

	public Object clone()
	{	
        WriteToLogMeta retval = (WriteToLogMeta) super.clone();

        int nrfields = fieldName.length;

        retval.allocate(nrfields);

        for (int i = 0; i < nrfields; i++)
        {
            retval.fieldName[i] = fieldName[i];
        }
		return retval;
	}
	   public void allocate(int nrfields)
	    {
	        fieldName = new String[nrfields]; 
	    }
    /**
     * @return Returns the fieldName.
     */
    public String[] getFieldName()
    {
        return fieldName;
    }
    /**
     * @param fieldName The fieldName to set.
     */
    public void setFieldName(String[] fieldName)
    {
        this.fieldName = fieldName;
    }
    public boolean isdisplayHeader()
    {
    	return displayHeader;
    }
    public void setdisplayHeader(boolean displayheader)
    {
    	this.displayHeader=displayheader;
    }
    
	private void readData(Node stepnode)  throws KettleXMLException
	{
	  try
	    {
		  loglevel = XMLHandler.getTagValue(stepnode, "loglevel");
		  displayHeader = "Y".equalsIgnoreCase(XMLHandler.getTagValue(stepnode, "displayHeader"));
		  
		  Node fields = XMLHandler.getSubNode(stepnode, "fields");
          int nrfields = XMLHandler.countNodes(fields, "field");

          allocate(nrfields);

          for (int i = 0; i < nrfields; i++)
          {
              Node fnode = XMLHandler.getSubNodeByNr(fields, "field", i);
              fieldName[i] = XMLHandler.getTagValue(fnode, "name");
          }
	    }
      catch (Exception e)
      {
          throw new KettleXMLException("Unable to load step info from XML", e);
      }
	}
   public String getXML()
    {
        StringBuffer retval = new StringBuffer();
        retval.append("      " + XMLHandler.addTagValue("loglevel", loglevel));
        retval.append("      " + XMLHandler.addTagValue("displayHeader", displayHeader));
        
        retval.append("    <fields>" + Const.CR);
        for (int i = 0; i < fieldName.length; i++)
        {
            retval.append("      <field>" + Const.CR);
            retval.append("        " + XMLHandler.addTagValue("name", fieldName[i]));
            retval.append("        </field>" + Const.CR);
        }
        retval.append("      </fields>" + Const.CR);

        return retval.toString();
    }
	public void setDefault()
	{
		loglevel=logLevelCodes[3];
		displayHeader=true;
        int nrfields = 0;

        allocate(nrfields);

        for (int i = 0; i < nrfields; i++)
        {
            fieldName[i] = "field" + i;
        }
	}

	public void setInfo(Map<String, String[]> p, String id,
			List<? extends SharedObjectInterface> databases) {
		loglevel = BaseStepMeta.parameterToString(p.get(id + ".loglevel"));
		displayHeader = BaseStepMeta.parameterToBoolean(p.get(id + ".displayHeader"));
		String[] fieldName = p.get(id + "_fields.fieldName");

		this.fieldName = (fieldName != null) ? fieldName : (new String[0]);
	}
	
	public void readRep(Repository rep, long id_step, List<DatabaseMeta> databases, Map<String,Counter> counters)
	throws KettleException
	{
	        try
	        {
	        	loglevel = rep.getStepAttributeString(id_step, "loglevel");
	        	displayHeader = rep.getStepAttributeBoolean(id_step, "displayHeader");
	        	
	            int nrfields = rep.countNrStepAttributes(id_step, "field_name");

	            allocate(nrfields);

	            for (int i = 0; i < nrfields; i++)
	            {
	                fieldName[i] = rep.getStepAttributeString(id_step, i, "field_name");
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
	        	rep.saveStepAttribute(id_transformation, id_step, "loglevel", loglevel);
	        	rep.saveStepAttribute(id_transformation, id_step, "displayHeader", displayHeader);
	        	
	            for (int i = 0; i < fieldName.length; i++)
	            {
	                rep.saveStepAttribute(id_transformation, id_step, i, "field_name", fieldName[i]);
	            }
	        }
	        catch (Exception e)
	        {
	            throw new KettleException("Unable to save step information to the repository for id_step=" + id_step, e);
	        }
	    }
	
	public void check(List<CheckResultInterface> remarks, TransMeta transmeta, StepMeta stepinfo, RowMetaInterface prev, String input[], String output[], RowMetaInterface info)
	{
		CheckResult cr;
		if (prev==null || prev.size()==0)
		{
			cr = new CheckResult(CheckResult.TYPE_RESULT_WARNING, Messages.getString("WriteToLogMeta.CheckResult.NotReceivingFields"), stepinfo); //$NON-NLS-1$
			remarks.add(cr);
		}
		else
		{
			cr = new CheckResult(CheckResult.TYPE_RESULT_OK, Messages.getString("WriteToLogMeta.CheckResult.StepRecevingData",prev.size()+""), stepinfo); //$NON-NLS-1$ //$NON-NLS-2$
			remarks.add(cr);
			
			String error_message = "";
	        boolean error_found = false;

            // Starting from selected fields in ...
            for (int i = 0; i < fieldName.length; i++)
            {
                int idx = prev.indexOfValue(fieldName[i]);
                if (idx < 0)
                {
                    error_message += "\t\t" + fieldName[i] + Const.CR;
                    error_found = true;
                }
            }
            if (error_found)
            {
                error_message = Messages.getString("WriteToLogMeta.CheckResult.FieldsFound", error_message);

                cr = new CheckResult(CheckResult.TYPE_RESULT_ERROR, error_message, stepinfo);
                remarks.add(cr);
            }
            else
            {
                if (fieldName.length > 0)
                {
                    cr = new CheckResult(CheckResult.TYPE_RESULT_OK, Messages.getString("WriteToLogMeta.CheckResult.AllFieldsFound"), stepinfo);
                    remarks.add(cr);
                }
                else
                {
                    cr = new CheckResult(CheckResult.TYPE_RESULT_WARNING, Messages.getString("WriteToLogMeta.CheckResult.NoFieldsEntered"), stepinfo);
                    remarks.add(cr);
                }
            }

		}
		
		// See if we have input streams leading to this step!
		if (input.length>0)
		{
			cr = new CheckResult(CheckResult.TYPE_RESULT_OK, Messages.getString("WriteToLogMeta.CheckResult.StepRecevingData2"), stepinfo); //$NON-NLS-1$
			remarks.add(cr);
		}
		else
		{
			cr = new CheckResult(CheckResult.TYPE_RESULT_ERROR, Messages.getString("WriteToLogMeta.CheckResult.NoInputReceivedFromOtherSteps"), stepinfo); //$NON-NLS-1$
			remarks.add(cr);
		}
	}
	public StepInterface getStep(StepMeta stepMeta, StepDataInterface stepDataInterface, int cnr, TransMeta tr, Trans trans)
	{
		return new WriteToLog(stepMeta, stepDataInterface, cnr, tr, trans);
	}
	
	public StepDataInterface getStepData()
	{
		return new WriteToLogData();
	}


}
