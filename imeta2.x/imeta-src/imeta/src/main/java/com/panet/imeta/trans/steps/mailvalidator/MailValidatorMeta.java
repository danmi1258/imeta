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

package com.panet.imeta.trans.steps.mailvalidator;

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

/*
 * Created on 03-Juin-2008
 * 
 */

public class MailValidatorMeta extends BaseStepMeta implements StepMetaInterface
{
	public static final String STEP_ATTRIBUTE_EMAILFIELD = "emailfield";
	public static final String STEP_ATTRIBUTE_RESULTFIELDNAME = "resultfieldname";
	public static final String STEP_ATTRIBUTE_RESULTASSTRING = "ResultAsString";
	public static final String STEP_ATTRIBUTE_SMTPCHECK = "smtpCheck";
	public static final String STEP_ATTRIBUTE_EMAILVALIDEMSG = "emailValideMsg";
	public static final String STEP_ATTRIBUTE_EMAILNOTVALIDEMSG = "emailNotValideMsg";
	public static final String STEP_ATTRIBUTE_ERRORSFIELDNAME = "errorsFieldName";
	public static final String STEP_ATTRIBUTE_TIEMOUT = "timeout";
	public static final String STEP_ATTRIBUTE_DEFAULTSMTP = "defaultSMTP";
	public static final String STEP_ATTRIBUTE_EMAILSENDER = "emailSender";
	public static final String STEP_ATTRIBUTE_DEFAULTSMTPFIELD = "defaultSMTPField";
	public static final String STEP_ATTRIBUTE_ISDYNAMICDEFAULTSMTP = "isdynamicDefaultSMTP";
//	emailfield = rep.getStepAttributeString(id_step, "emailfield"); //$NON-NLS-1$
//    resultfieldname = rep.getStepAttributeString(id_step, "resultfieldname"); //$NON-NLS-1$
//    ResultAsString  = rep.getStepAttributeBoolean(id_step, "ResultAsString");
//    smtpCheck  = rep.getStepAttributeBoolean(id_step, "smtpCheck");
//    
//    emailValideMsg = rep.getStepAttributeString(id_step, "emailValideMsg"); //$NON-NLS-1$
//    emailNotValideMsg = rep.getStepAttributeString(id_step, "emailNotValideMsg"); //$NON-NLS-1$
//    errorsFieldName = rep.getStepAttributeString(id_step, "errorsFieldName"); //$NON-NLS-1$
//    timeout = rep.getStepAttributeString(id_step, "timeout"); //$NON-NLS-1$ 
//    defaultSMTP = rep.getStepAttributeString(id_step, "defaultSMTP"); //$NON-NLS-1$ 
//    emailSender = rep.getStepAttributeString(id_step, "emailSender"); //$NON-NLS-1$ 
//    defaultSMTPField = rep.getStepAttributeString(id_step, "defaultSMTPField"); //$NON-NLS-1$ 
//    
//    isdynamicDefaultSMTP = rep.getStepAttributeBoolean(id_step, "isdynamicDefaultSMTP"); //$NON-NLS-1$ 
    
    /** dynamic email address */
    private String    emailfield;
    
    private boolean  resultAsString;
    
    private boolean smtpCheck;
    
    private String emailValideMsg;
    
    private String emailNotValideMsg;
    
    private String errorsFieldName;
    
    private String timeout;
    
    private String defaultSMTP;
    
    private String emailSender;
    
    private String defaultSMTPField;
    
    private boolean isdynamicDefaultSMTP;
    
    private String resultfieldname;
    
    public MailValidatorMeta()
    {
        super(); // allocate BaseStepMeta
    }

    /**
     * @return Returns the emailfield.
     */
    public String getEmailField()
    {
        return emailfield;
    }

    /**
     * @param emailfield The emailfield to set.
     */
    public void setEmailfield(String emailfield)
    {
        this.emailfield = emailfield;
    }

    /**
     * @return Returns the resultName.
     */
    public String getResultFieldName()
    {
        return resultfieldname;
    }

    /**
     * @param resultfieldname The resultfieldname to set.
     */
    public void setResultFieldName(String resultfieldname)
    {
        this.resultfieldname = resultfieldname;
    }
    
    /**
     * @param emailValideMsg The emailValideMsg to set.
     */
    public void setEmailValideMsg(String emailValideMsg)
    {
        this.emailValideMsg = emailValideMsg;
    }

    /**
     * @return Returns the emailValideMsg.
     */
    public String getEMailValideMsg()
    {
        return emailValideMsg;
    }
    /**
     * @return Returns the emailNotValideMsg.
     */
    public String getEMailNotValideMsg()
    {
        return emailNotValideMsg;
    }

    /**
     * @return Returns the errorsFieldName.
     */
    public String getErrorsField()
    {
        return errorsFieldName;
    }
 
    /**
     * @param errorsFieldName The errorsFieldName to set.
     */
    public void setErrorsField(String errorsFieldName)
    {
        this.errorsFieldName = errorsFieldName;
    }
    
    /**
     * @return Returns the timeout.
     */
    public String getTimeOut()
    {
        return timeout;
    }

    /**
     * @param timeout The timeout to set.
     */
    public void setTimeOut(String timeout)
    {
        this.timeout = timeout;
    } 
    
    /**
     * @return Returns the defaultSMTP.
     */
    public String getDefaultSMTP()
    {
        return defaultSMTP;
    }
    /**
     * @param defaultSMTP The defaultSMTP to set.
     */
    public void setDefaultSMTP(String defaultSMTP)
    {
        this.defaultSMTP = defaultSMTP;
    } 
    /**
     * @return Returns the emailSender.
     */
    public String geteMailSender()
    {
        return emailSender;
    }
    /**
     * @param emailSender The emailSender to set.
     */
    public void seteMailSender(String emailSender)
    {
        this.emailSender = emailSender;
    } 
    /**
     * @return Returns the defaultSMTPField.
     */
    public String getDefaultSMTPField()
    {
        return defaultSMTPField;
    }
    /**
     * @param defaultSMTPField The defaultSMTPField to set.
     */
    public void setDefaultSMTPField(String defaultSMTPField)
    {
        this.defaultSMTPField = defaultSMTPField;
    } 
    
    /**
     * @return Returns the isdynamicDefaultSMTP.
     */
    public boolean isdynamicDefaultSMTP()
    {
        return isdynamicDefaultSMTP;
    }
    /**
     * @param isdynamicDefaultSMTP The isdynamicDefaultSMTP to set.
     */
    public void setdynamicDefaultSMTP(boolean isdynamicDefaultSMTP)
    {
        this.isdynamicDefaultSMTP = isdynamicDefaultSMTP;
    } 
    
    /**
     * @param emailNotValideMsg The emailNotValideMsg to set.
     */
    public void setEmailNotValideMsg(String emailNotValideMsg)
    {
        this.emailNotValideMsg = emailNotValideMsg;
    }
    public boolean isResultAsString()
    {
    	return resultAsString;
    }
    
    public void setResultAsString(boolean ResultAsString)
    {
    	this.resultAsString=ResultAsString;
    }
    public void setSMTPCheck(boolean smtpcheck)
    {
    	this.smtpCheck=smtpcheck;
    }
    public boolean isSMTPCheck()
    {
    	return smtpCheck;
    } 
    
    public void loadXML(Node stepnode, List<DatabaseMeta> databases, Map<String, Counter> counters)
    throws KettleXMLException
    {
		readData(stepnode);
	}
 

    public Object clone()
    {
        MailValidatorMeta retval = (MailValidatorMeta) super.clone();
       
        return retval;
    }

    public void setDefault()
    {
        resultfieldname = "result"; //$NON-NLS-1$
        emailValideMsg="email address is valid";
        emailNotValideMsg="email address is not valid";
        resultAsString=false;
        errorsFieldName="Error message";
        timeout="0";
        defaultSMTP=null;
        emailSender="noreply@domain.com";
        smtpCheck=false;
        isdynamicDefaultSMTP=false;
        defaultSMTPField=null;
    }
    public void getFields(RowMetaInterface r, String name, RowMetaInterface info[], 
    		StepMeta nextStep, VariableSpace space) throws KettleStepException
	{	
      
        String realResultFieldName=space.environmentSubstitute(resultfieldname);
        if (resultAsString) {  
			ValueMetaInterface v = new ValueMeta(realResultFieldName, ValueMeta.TYPE_STRING);
			v.setLength(100, -1);
			v.setOrigin(name);
			r.addValueMeta(v);
    		
	     }else {
			 ValueMetaInterface v = new ValueMeta(realResultFieldName, ValueMeta.TYPE_BOOLEAN);
			 v.setOrigin(name);
			 r.addValueMeta(v);
	     }
        
        String realErrorsFieldName=space.environmentSubstitute(errorsFieldName);
        if(!Const.isEmpty(realErrorsFieldName)){		     
				ValueMetaInterface v = new ValueMeta(realErrorsFieldName, ValueMeta.TYPE_STRING);
				v.setLength(100, -1);
				v.setOrigin(name);
				r.addValueMeta(v);
        }
    }
	

    public String getXML()
    {
        StringBuffer retval = new StringBuffer();

        retval.append("    " + XMLHandler.addTagValue("emailfield", emailfield)); //$NON-NLS-1$ //$NON-NLS-2$
        retval.append("    " + XMLHandler.addTagValue("resultfieldname", resultfieldname)); //$NON-NLS-1$ //$NON-NLS-2$
        retval.append("    ").append(XMLHandler.addTagValue("ResultAsString",       resultAsString));
        retval.append("    ").append(XMLHandler.addTagValue("smtpCheck",       smtpCheck));
        
        retval.append("    " + XMLHandler.addTagValue("emailValideMsg", emailValideMsg));
        retval.append("    " + XMLHandler.addTagValue("emailNotValideMsg", emailNotValideMsg));
        retval.append("    " + XMLHandler.addTagValue("errorsFieldName", errorsFieldName));
        retval.append("    " + XMLHandler.addTagValue("timeout", timeout));
        retval.append("    " + XMLHandler.addTagValue("defaultSMTP", defaultSMTP));
        retval.append("    " + XMLHandler.addTagValue("emailSender", emailSender));
        retval.append("    " + XMLHandler.addTagValue("defaultSMTPField", defaultSMTPField));
        
        retval.append("    " + XMLHandler.addTagValue("isdynamicDefaultSMTP", isdynamicDefaultSMTP));
        
        
        return retval.toString();
    }

    private void readData(Node stepnode) throws KettleXMLException
    {
	try
	{
		emailfield = XMLHandler.getTagValue(stepnode, "emailfield"); //$NON-NLS-1$
            resultfieldname = XMLHandler.getTagValue(stepnode, "resultfieldname");
            resultAsString  = "Y".equalsIgnoreCase(XMLHandler.getTagValue(stepnode, "ResultAsString"));
            smtpCheck  = "Y".equalsIgnoreCase(XMLHandler.getTagValue(stepnode, "smtpCheck"));
            
            emailValideMsg = XMLHandler.getTagValue(stepnode, "emailValideMsg");
            emailNotValideMsg = XMLHandler.getTagValue(stepnode, "emailNotValideMsg");
            errorsFieldName = XMLHandler.getTagValue(stepnode, "errorsFieldName");
            timeout = XMLHandler.getTagValue(stepnode, "timeout");
            defaultSMTP = XMLHandler.getTagValue(stepnode, "defaultSMTP");
            emailSender = XMLHandler.getTagValue(stepnode, "emailSender");
            defaultSMTPField = XMLHandler.getTagValue(stepnode, "defaultSMTPField");
            
            isdynamicDefaultSMTP = "Y".equalsIgnoreCase(XMLHandler.getTagValue(stepnode, "isdynamicDefaultSMTP"));
            
            
        }
        catch (Exception e)
        {
            throw new KettleXMLException(Messages.getString("MailValidatorMeta.Exception.UnableToReadStepInfo"), e); //$NON-NLS-1$
        }
    }

    public void readRep(Repository rep, long id_step, List<DatabaseMeta> databases, Map<String, Counter> counters)
    throws KettleException
   {
        try
        {
        	emailfield = rep.getStepAttributeString(id_step, "emailfield"); //$NON-NLS-1$
            resultfieldname = rep.getStepAttributeString(id_step, "resultfieldname"); //$NON-NLS-1$
            resultAsString  = rep.getStepAttributeBoolean(id_step, "ResultAsString");
            smtpCheck  = rep.getStepAttributeBoolean(id_step, "smtpCheck");
            
            emailValideMsg = rep.getStepAttributeString(id_step, "emailValideMsg"); //$NON-NLS-1$
            emailNotValideMsg = rep.getStepAttributeString(id_step, "emailNotValideMsg"); //$NON-NLS-1$
            errorsFieldName = rep.getStepAttributeString(id_step, "errorsFieldName"); //$NON-NLS-1$
            timeout = rep.getStepAttributeString(id_step, "timeout"); //$NON-NLS-1$ 
            defaultSMTP = rep.getStepAttributeString(id_step, "defaultSMTP"); //$NON-NLS-1$ 
            emailSender = rep.getStepAttributeString(id_step, "emailSender"); //$NON-NLS-1$ 
            defaultSMTPField = rep.getStepAttributeString(id_step, "defaultSMTPField"); //$NON-NLS-1$ 
            
            isdynamicDefaultSMTP = rep.getStepAttributeBoolean(id_step, "isdynamicDefaultSMTP"); //$NON-NLS-1$ 
            
            
        }
        catch (Exception e)
        {
            throw new KettleException(Messages.getString("MailValidatorMeta.Exception.UnexpectedErrorReadingStepInfo"), e); //$NON-NLS-1$
        }
    }

    public void saveRep(Repository rep, long id_transformation, long id_step) throws KettleException
    {
        try
        {
            rep.saveStepAttribute(id_transformation, id_step, "emailfield", emailfield); //$NON-NLS-1$
            rep.saveStepAttribute(id_transformation, id_step, "resultfieldname", resultfieldname); //$NON-NLS-1$
			rep.saveStepAttribute(id_transformation, id_step, "ResultAsString",          resultAsString);
			rep.saveStepAttribute(id_transformation, id_step, "smtpCheck",          smtpCheck);
			
			rep.saveStepAttribute(id_transformation, id_step, "emailValideMsg", emailValideMsg);
			rep.saveStepAttribute(id_transformation, id_step, "emailNotValideMsg", emailNotValideMsg);
			rep.saveStepAttribute(id_transformation, id_step, "errorsFieldName", errorsFieldName);
			rep.saveStepAttribute(id_transformation, id_step, "timeout", timeout);
			rep.saveStepAttribute(id_transformation, id_step, "defaultSMTP", defaultSMTP);
			rep.saveStepAttribute(id_transformation, id_step, "emailSender", emailSender);
			rep.saveStepAttribute(id_transformation, id_step, "defaultSMTPField", defaultSMTPField);
			
			rep.saveStepAttribute(id_transformation, id_step, "isdynamicDefaultSMTP", isdynamicDefaultSMTP);
			
			
        }
        catch (Exception e)
        {
            throw new KettleException(Messages.getString("MailValidatorMeta.Exception.UnableToSaveStepInfo") + id_step, e); //$NON-NLS-1$
        }
    }

	public void check(List<CheckResultInterface> remarks, TransMeta transMeta, StepMeta stepMeta, RowMetaInterface prev, String input[], String output[], RowMetaInterface info)
	{
        CheckResult cr;
      
        if (Const.isEmpty(resultfieldname))
            cr = new CheckResult(CheckResult.TYPE_RESULT_ERROR,  Messages.getString("MailValidatorMeta.CheckResult.ResultFieldMissing"), stepMeta);
        else
            cr = new CheckResult(CheckResult.TYPE_RESULT_OK, Messages.getString("MailValidatorMeta.CheckResult.ResultFieldOk"), stepMeta);
        remarks.add(cr);
        
        if(this.resultAsString){
        	if(Const.isEmpty(emailValideMsg))
        		cr = new CheckResult(CheckResult.TYPE_RESULT_ERROR,  Messages.getString("MailValidatorMeta.CheckResult.EmailValidMsgMissing"), stepMeta);
        	else
        		cr = new CheckResult(CheckResult.TYPE_RESULT_OK, Messages.getString("MailValidatorMeta.CheckResult.EmailValidMsgOk"), stepMeta);
        	 remarks.add(cr);
        	 
         	if(Const.isEmpty(emailNotValideMsg))
        		cr = new CheckResult(CheckResult.TYPE_RESULT_ERROR,  Messages.getString("MailValidatorMeta.CheckResult.EmailNotValidMsgMissing"), stepMeta);
        	else
        		cr = new CheckResult(CheckResult.TYPE_RESULT_OK, Messages.getString("MailValidatorMeta.CheckResult.EmailNotValidMsgOk"), stepMeta);
        	 remarks.add(cr);
        }
        
        
        if (Const.isEmpty(emailfield))
            cr = new CheckResult(CheckResult.TYPE_RESULT_ERROR, Messages.getString("MailValidatorMeta.CheckResult.eMailFieldMissing"), stepMeta);
        else 
            cr = new CheckResult(CheckResult.TYPE_RESULT_OK, Messages.getString("MailValidatorMeta.CheckResult.eMailFieldOK"), stepMeta);
        remarks.add(cr);
        
        // See if we have input streams leading to this step!
        if (input.length > 0){
            cr = new CheckResult(CheckResult.TYPE_RESULT_OK, Messages.getString("MailValidatorMeta.CheckResult.ReceivingInfoFromOtherSteps"), stepMeta); //$NON-NLS-1$
        }
        else{
            cr = new CheckResult(CheckResult.TYPE_RESULT_ERROR, Messages.getString("MailValidatorMeta.CheckResult.NoInpuReceived"), stepMeta); //$NON-NLS-1$  
        }
        remarks.add(cr);
        if(resultAsString){
        	if(Const.isEmpty(emailValideMsg))
                cr = new CheckResult(CheckResult.TYPE_RESULT_ERROR, Messages.getString("MailValidatorMeta.CheckResult.eMailValidMsgMissing"), stepMeta); //$NON-NLS-1$
            else
                cr = new CheckResult(CheckResult.TYPE_RESULT_OK, Messages.getString("MailValidatorMeta.CheckResult.eMailValidMsgOk"), stepMeta); //$NON-NLS-1$
        	remarks.add(cr);
        	
        	if(Const.isEmpty(emailNotValideMsg))
                cr = new CheckResult(CheckResult.TYPE_RESULT_ERROR, Messages.getString("MailValidatorMeta.CheckResult.eMailNotValidMsgMissing"), stepMeta); //$NON-NLS-1$	
        	else
                cr = new CheckResult(CheckResult.TYPE_RESULT_OK, Messages.getString("MailValidatorMeta.CheckResult.eMailNotValidMsgOk"), stepMeta); //$NON-NLS-1$
            remarks.add(cr);   
        }
        // SMTP check
        if(smtpCheck){
        	// sender
        	if(Const.isEmpty(emailSender))
        		  cr = new CheckResult(CheckResult.TYPE_RESULT_ERROR, Messages.getString("MailValidatorMeta.CheckResult.eMailSenderMissing"), stepMeta);
        	else
        		  cr = new CheckResult(CheckResult.TYPE_RESULT_OK, Messages.getString("MailValidatorMeta.CheckResult.eMailSenderOk"), stepMeta); //$NON-NLS-1$
        	remarks.add(cr); 
        	
        	//dynamic default SMTP
        	if(isdynamicDefaultSMTP){
        		if(Const.isEmpty(defaultSMTPField))
        			cr = new CheckResult(CheckResult.TYPE_RESULT_ERROR, Messages.getString("MailValidatorMeta.CheckResult.dynamicDefaultSMTPFieldMissing"), stepMeta);
        		else
        			cr = new CheckResult(CheckResult.TYPE_RESULT_OK, Messages.getString("MailValidatorMeta.CheckResult.dynamicDefaultSMTPFieldOk"), stepMeta);
        		remarks.add(cr); 
        	}
        }
    
    }

    public StepInterface getStep(StepMeta stepMeta, StepDataInterface stepDataInterface, int cnr, TransMeta transMeta, Trans trans)
    {
        return new MailValidator(stepMeta, stepDataInterface, cnr, transMeta, trans);
    }

    public StepDataInterface getStepData()
    {
        return new MailValidatorData();
    }

    public boolean supportsErrorHandling()
    {
        return true;
    }

	@Override
	public void setInfo(Map<String, String[]> p, String id,
			List<? extends SharedObjectInterface> databases) {
		// TODO Auto-generated method stub
		emailfield = BaseStepMeta.parameterToString(p.get(id + ".emailfield"));
		resultAsString = BaseStepMeta.parameterToBoolean(p.get(id + ".resultAsString"));
		smtpCheck = BaseStepMeta.parameterToBoolean(p.get(id + ".smtpCheck"));
		emailValideMsg= BaseStepMeta.parameterToString(p.get(id + ".emailValideMsg"));
		emailNotValideMsg= BaseStepMeta.parameterToString(p.get(id + ".emailNotValideMsg"));
		errorsFieldName= BaseStepMeta.parameterToString(p.get(id + ".errorsFieldName"));
		timeout= BaseStepMeta.parameterToString(p.get(id + ".timeout"));
		defaultSMTP= BaseStepMeta.parameterToString(p.get(id + ".defaultSMTP"));
		emailSender= BaseStepMeta.parameterToString(p.get(id + ".emailSender"));
		defaultSMTPField= BaseStepMeta.parameterToString(p.get(id + ".defaultSMTPField"));
		isdynamicDefaultSMTP= BaseStepMeta.parameterToBoolean(p.get(id + ".isdynamicDefaultSMTP"));
		resultfieldname= BaseStepMeta.parameterToString(p.get(id + ".resultfieldname"));
		
	}
 
}
