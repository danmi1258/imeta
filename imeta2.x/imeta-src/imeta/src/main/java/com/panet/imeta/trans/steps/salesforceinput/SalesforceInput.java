/*************************************************************************************** 
 * Copyright (C) 2007 Samatar.  All rights reserved. 
 * This software was developed by Samatar and is provided under the terms 
 * of the GNU Lesser General Public License, Version 2.1. You may not use 
 * this file except in compliance with the license. A copy of the license, 
 * is included with the binaries and source code. The Original Code is Samatar.  
 * The Initial Developer is Samatar.
 *
 * Software distributed under the GNU Lesser Public License is distributed on an 
 * "AS IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. 
 * Please refer to the license for the specific language governing your rights 
 * and limitations.
 ***************************************************************************************/
 
package com.panet.imeta.trans.steps.salesforceinput;

import com.panet.imeta.core.Const;
import com.panet.imeta.core.exception.KettleException;
import com.panet.imeta.core.row.RowDataUtil;
import com.panet.imeta.core.row.RowMeta;
import com.panet.imeta.core.row.RowMetaInterface;
import com.panet.imeta.core.row.ValueMetaInterface;
import com.panet.imeta.trans.Trans;
import com.panet.imeta.trans.TransMeta;
import com.panet.imeta.trans.step.BaseStep;
import com.panet.imeta.trans.step.StepDataInterface;
import com.panet.imeta.trans.step.StepInterface;
import com.panet.imeta.trans.step.StepMeta;
import com.panet.imeta.trans.step.StepMetaInterface;
import com.sforce.soap.partner.DescribeSObjectResult;
import com.sforce.soap.partner.GetUserInfoResult;
import com.sforce.soap.partner.LoginResult;
import com.sforce.soap.partner.SessionHeader;
import com.sforce.soap.partner.SforceServiceLocator;
import com.sforce.soap.partner.SoapBindingStub;
import com.sforce.soap.partner.sobject.SObject;


/**
 * Read data from Salesforce module, convert them to rows and writes these to one or more output streams.
 * 
 * @author Samatar
 * @since 10-06-2007
 */
public class SalesforceInput extends BaseStep implements StepInterface
{
	private SalesforceInputMeta meta;
	private SalesforceInputData data;
	
	public SalesforceInput(StepMeta stepMeta, StepDataInterface stepDataInterface, int copyNr, TransMeta transMeta, Trans trans)
	{
		super(stepMeta, stepDataInterface, copyNr, transMeta, trans);
	}
	
	public SoapBindingStub getBinding(String Url,String username, String password, String module, 
			String condition,String timeout) throws KettleException
	{
		SoapBindingStub binding=null;
		LoginResult loginResult = null;
		GetUserInfoResult userInfo = null;
		
		
		try{
			binding = (SoapBindingStub) new SforceServiceLocator().getSoap();
			if (log.isDetailed()) logDetailed(Messages.getString("SalesforceInput.Log.LoginURL") + " : " + binding._getProperty(SoapBindingStub.ENDPOINT_ADDRESS_PROPERTY));
		      
	        //  Set timeout
			int timeOut=Const.toInt(timeout, 0);
	      	if(timeOut>0) binding.setTimeout(timeOut);
	        
	      	// Set URL
	        binding._setProperty(SoapBindingStub.ENDPOINT_ADDRESS_PROPERTY, Url);
	        
	        // Attempt the login giving the user feedback
		      
	        if (log.isDetailed())
	        {
	        	logDetailed(Messages.getString("SalesforceInput.Log.LoginNow"));
	        	logDetailed("----------------------------------------->");
	        	logDetailed(Messages.getString("SalesforceInput.Log.LoginURL",Url));
	        	logDetailed(Messages.getString("SalesforceInput.Log.LoginUsername",username));
	        	logDetailed(Messages.getString("SalesforceInput.Log.LoginModule",module));
	        	if(!Const.isEmpty(condition)) logDetailed(Messages.getString("SalesforceInput.Log.LoginCondition",condition));
	        	logDetailed("<-----------------------------------------");
	        }
	        
	        // Login
	        loginResult = binding.login(username, password);
	        
	        if (log.isDebug())
	        {
	        	logDebug(Messages.getString("SalesforceInput.Log.SessionId") + " : " + loginResult.getSessionId());
	        	logDebug(Messages.getString("SalesforceInput.Log.NewServerURL") + " : " + loginResult.getServerUrl());
	        }
	        
	        // set the session header for subsequent call authentication
	        binding._setProperty(SoapBindingStub.ENDPOINT_ADDRESS_PROPERTY,loginResult.getServerUrl());
	
	        // Create a new session header object and set the session id to that
	        // returned by the login
	        SessionHeader sh = new SessionHeader();
	        sh.setSessionId(loginResult.getSessionId());
	        binding.setHeader(new SforceServiceLocator().getServiceName().getNamespaceURI(), "SessionHeader", sh);
	        
	        // Return the user Infos
	        userInfo = binding.getUserInfo();
	        if (log.isDebug()) 
	        {
	        	logDebug(Messages.getString("SalesforceInput.Log.UserInfos") + " : " + userInfo.getUserFullName());
	        	logDebug("----------------------------------------->");
	        	logDebug(Messages.getString("SalesforceInput.Log.UserName") + " : " + userInfo.getUserFullName());
	        	logDebug(Messages.getString("SalesforceInput.Log.UserEmail") + " : " + userInfo.getUserEmail());
	        	logDebug(Messages.getString("SalesforceInput.Log.UserLanguage") + " : " + userInfo.getUserLanguage());
	        	logDebug(Messages.getString("SalesforceInput.Log.UserOrganization") + " : " + userInfo.getOrganizationName());    
			    logDebug("<-----------------------------------------");
	        }	
		}catch(Exception e)
		{
			throw new KettleException(e);
		}
		return binding;
	}

	 public void connectSalesforce() throws KettleException {
		 
		String username=environmentSubstitute(meta.getUserName());
		String password = environmentSubstitute(meta.getPassword());
		String module = environmentSubstitute(meta.getModule());
		String condition = environmentSubstitute(meta.getCondition()); 
		String timeout= environmentSubstitute(meta.getTimeOut()); 
		
		// connect and return binding
		SoapBindingStub binding=getBinding(data.URL,username, password, module, condition,timeout);
		
		if(binding==null)  throw new KettleException(Messages.getString("SalesforceInput.Exception.CanNotGetBiding"));
		

	    try{
	    	
			// check if Object is queryable
			
		    DescribeSObjectResult describeSObjectResult = binding.describeSObject(module);
		        
		    if (describeSObjectResult == null) throw new KettleException(Messages.getString("SalesforceInput.ErrorGettingObject"));
		        
		    if(!describeSObjectResult.isQueryable()) throw new KettleException(Messages.getString("SalesforceInputDialog.ObjectNotQueryable",module));
			
		    // Built SQL statement
		    String SQLString=BuiltSQl();
		        
		    if (log.isDetailed()) logDetailed(Messages.getString("SalesforceInput.Log.SQLString") + " : " +  SQLString);        
		    
		    if(meta.includeSQL()) data.SQL=SQLString;
	    	if(meta.includeTimestamp()) data.Timestamp= binding.getServerTimestamp().toString();
	 		if(log.isDebug()) Messages.getString("SalesforceInput.Log.ServerTimestamp",""+binding.getServerTimestamp());
			
	    	// return query result
	        data.qr = binding.query(SQLString);
	        
	        if(data.qr==null) data.limitReached = true;	
	        else{
	        	if(data.qr.getRecords()==null) data.limitReached = true;
	        	else {
	        		data.recordcount=data.qr.getRecords().length;
	        		data.limitReached = false;	
	        	}
	        }
	        
	        if (log.isDetailed()) logDetailed(Messages.getString("SalesforceInput.Log.RecordCount") + " : " +  data.recordcount);      
	        
		}catch(Exception e)
		{
			throw new KettleException(e);
		}
	 }
	
	public boolean processRow(StepMetaInterface smi, StepDataInterface sdi) throws KettleException
	{
		if(first)
		{
			first=false;
			// Check if module is specified 
			 if (Const.isEmpty(meta.getModule()))
			 {
				 throw new KettleException(Messages.getString("SalesforceInputDialog.ModuleMissing.DialogMessage"));
			 }
			 
			  // Check if username is specified 
			 if (Const.isEmpty(meta.getUserName()))
			 {
				 throw new KettleException(Messages.getString("SalesforceInputDialog.UsernameMissing.DialogMessage"));
			 }
			 
			data.recordcount=0;
			data.rownr = 0;	
			data.limit=Const.toLong(environmentSubstitute(meta.getRowLimit()),0);
			data.URL=environmentSubstitute(meta.getTargetURL());
			data.Module=environmentSubstitute(meta.getModule());
			
			// get total fields in the grid
			data.nrfields = meta.getInputFields().length;
			
			 // Check if field list is filled 
			 if (data.nrfields==0)
			 {
				 throw new KettleException(Messages.getString("SalesforceInputDialog.FieldsMissing.DialogMessage"));
			 }
			 
			// Create the output row meta-data
            data.outputRowMeta = new RowMeta();

			meta.getFields(data.outputRowMeta, getStepname(), null, null, this);

            // For String to <type> conversions, we allocate a conversion meta data row as well...
			//
			data.convertRowMeta = data.outputRowMeta.clone();
			for (int i=0;i<data.convertRowMeta.size();i++) {
				data.convertRowMeta.getValueMeta(i).setType(ValueMetaInterface.TYPE_STRING);            
			}
			
			// connect to Salesforce
			connectSalesforce();
		}
		
		if(log.isDebug()) logDebug(Messages.getString("SalesforceInput.Log.Connected"));	
		
		Object[] outputRowData=null;
        boolean sendToErrorRow=false;
		String errorMessage = null;
		try 
		{	
			// get one row ...
			outputRowData =getOneRow();
			
			if(outputRowData==null)
			{
				setOutputDone();
				return false;
			}
		
			putRow(data.outputRowMeta, outputRowData);  // copy row to output rowset(s);
		    
		    if (checkFeedback(getLinesInput()))
		    {
		    	if(log.isDetailed()) logDetailed(Messages.getString("SalesforceInput.log.LineRow",""+ getLinesInput()));
		    }
		    
		    return true; 
		} 
		catch(Exception e)
		{
			if (getStepMeta().isDoingErrorHandling())
			{
		         sendToErrorRow = true;
		         errorMessage = e.toString();
			}
			else
			{
				logError(Messages.getString("SalesforceInput.log.Exception", e.getMessage()));
				setErrors(1);
				stopAll();
				setOutputDone();  // signal end to receiver(s)
				return false;				
			}
			if (sendToErrorRow)
			{
			   // Simply add this row to the error row
			   putError(getInputRowMeta(), outputRowData, 1, errorMessage, null, "SalesforceInput001");
			}
		} 
		return true;
	}		
	private Object[] getOneRow()  throws KettleException
	{
		if (data.limitReached || data.rownr>=data.recordcount)
		{
	      return null;
		} 
		
		// Build an empty row based on the meta-data		  
		Object[] outputRowData=buildEmptyRow();

		try{
			SObject con = data.qr.getRecords()[(int)data.rownr];
			if(con==null) return null;
			for (int i=0;i<data.nrfields;i++)
			{
					
				String value=null;
				if(con.get_any()[i]!=null) value=con.get_any()[i].getValue();
				
				// DO Trimming!
				switch (meta.getInputFields()[i].getTrimType())
				{
				case SalesforceInputField.TYPE_TRIM_LEFT:
					value = Const.ltrim(value);
					break;
				case SalesforceInputField.TYPE_TRIM_RIGHT:
					value = Const.rtrim(value);
					break;
				case SalesforceInputField.TYPE_TRIM_BOTH:
					value = Const.trim(value);
					break;
				default:
					break;
				}
					      
				// DO CONVERSIONS...
				//
			    ValueMetaInterface targetValueMeta = data.outputRowMeta.getValueMeta(i);
				ValueMetaInterface sourceValueMeta = data.convertRowMeta.getValueMeta(i);
				outputRowData[i] = targetValueMeta.convertData(sourceValueMeta, value);
	    
	        
				// Do we need to repeat this field if it is null?
				if (meta.getInputFields()[i].isRepeated())
				{
					if (data.previousRow!=null && Const.isEmpty(value))
					{
						outputRowData[i] = data.previousRow[i];
					}
				}
					
			}  // End of loop over fields...
			
			int rowIndex = data.nrfields;
			
			// See if we need to add the url to the row...  
			if (meta.includeTargetURL() && !Const.isEmpty(meta.getTargetURLField()))
			{
				outputRowData[rowIndex++]= data.URL;
			}
			
			// See if we need to add the module to the row...  
			if (meta.includeModule() && !Const.isEmpty(meta.getModuleField()))
			{
				outputRowData[rowIndex++]=data.Module;
			}
	        
			// See if we need to add the generated SQL to the row...  
			if (meta.includeSQL() && !Const.isEmpty(meta.getSQLField()))
			{
				outputRowData[rowIndex++]=data.SQL;
			}
	        
			// See if we need to add the server timestamp to the row...  
			if (meta.includeTimestamp() && !Const.isEmpty(meta.getTimestampField()))
			{
				outputRowData[rowIndex++]=data.Timestamp;
			}
			
			// See if we need to add the row number to the row...  
	        if (meta.includeRowNumber() && !Const.isEmpty(meta.getRowNumberField()))
	        {
	            outputRowData[rowIndex++] = new Long(data.rownr);
	        }
	        
			RowMetaInterface irow = getInputRowMeta();
			
			data.previousRow = irow==null?outputRowData:(Object[])irow.cloneRow(outputRowData); // copy it to make
            
            // check for limit rows
            if (data.limit>0 && data.rownr>=data.limit-1)  data.limitReached = true;
            
            data.rownr++;
            
		 }
		 catch (Exception e)
		 {
			throw new KettleException(Messages.getString("SalesforceInput.Exception.CanNotReadFromSalesforce"), e);
		 }
		return outputRowData;
	}
 /* build the SQL statement to send to 
  * Salesforce
  */ 
 private String BuiltSQl()
 {
	String sql="SELECT ";
	
	SalesforceInputField fields[] = meta.getInputFields();
	
	for (int i=0;i<data.nrfields;i++){
		SalesforceInputField field = fields[i];    
		sql = sql + environmentSubstitute(field.getField());
		if(i<data.nrfields-1) sql+= ",";
	}
	
	sql = sql + " FROM " + environmentSubstitute(meta.getModule());
	
	if (!Const.isEmpty(environmentSubstitute(meta.getCondition()))){
		sql = sql + " WHERE " + environmentSubstitute(meta.getCondition().replace("\n\r", "").replace("\n", ""));
	}
	
	return sql;
 }
 
	/**
	 * Build an empty row based on the meta-data.
	 * 
	 * @return
	 */
	private Object[] buildEmptyRow()
	{
       Object[] rowData = RowDataUtil.allocateRowData(data.outputRowMeta.size());
	    return rowData;
	}
	public boolean init(StepMetaInterface smi, StepDataInterface sdi)
	{
		meta=(SalesforceInputMeta)smi;
		data=(SalesforceInputData)sdi;
		
		if (super.init(smi, sdi))
		{
			return true;
		}
		return false;
	}
	
	public void dispose(StepMetaInterface smi, StepDataInterface sdi)
	{
		meta=(SalesforceInputMeta)smi;
		data=(SalesforceInputData)sdi;
		try{
		if(!data.qr.isDone()) data.qr.setDone(true);
		}catch(Exception e){};
		super.dispose(smi, sdi);
	}
	
	//
	// Run is were the action happens!	
	public void run()
	{
    	BaseStep.runStepThread(this, meta, data);
	}
}
