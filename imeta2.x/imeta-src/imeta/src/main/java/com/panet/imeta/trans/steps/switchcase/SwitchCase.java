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

package com.panet.imeta.trans.steps.switchcase;

import java.util.Hashtable;

import com.panet.imeta.core.Const;
import com.panet.imeta.core.RowSet;
import com.panet.imeta.core.exception.KettleException;
import com.panet.imeta.core.row.ValueMeta;
import com.panet.imeta.core.row.ValueMetaInterface;
import com.panet.imeta.trans.Trans;
import com.panet.imeta.trans.TransMeta;
import com.panet.imeta.trans.step.BaseStep;
import com.panet.imeta.trans.step.StepDataInterface;
import com.panet.imeta.trans.step.StepInterface;
import com.panet.imeta.trans.step.StepMeta;
import com.panet.imeta.trans.step.StepMetaInterface;

/**
 * Filters input rows base on conditions.
 * 
 * @author Matt
 * @since 16-apr-2003, 07-nov-2004 (rewrite)
 */
public class SwitchCase extends BaseStep implements StepInterface
{
	private SwitchCaseMeta meta;
	private SwitchCaseData data;
	
	public SwitchCase(StepMeta stepMeta, StepDataInterface stepDataInterface, int copyNr, TransMeta transMeta, Trans trans)
	{
		super(stepMeta, stepDataInterface, copyNr, transMeta, trans);
	}
	
	public boolean processRow(StepMetaInterface smi, StepDataInterface sdi) throws KettleException
	{
		meta=(SwitchCaseMeta)smi;
		data=(SwitchCaseData)sdi;

		Object[] r=getRow();       // Get next usable row from input rowset(s)!
		if (r==null)  // no more input to be expected...
		{
			setOutputDone();
			return false;
		}
        
        if (first)
        {
        	first = false;
        	
            data.outputRowMeta = getInputRowMeta().clone();
            meta.getFields(getInputRowMeta(), getStepname(), null, null, this);

            data.fieldIndex = getInputRowMeta().indexOfValue(meta.getFieldname());
            if (data.fieldIndex<0) {
            	throw new KettleException(Messages.getString("SwitchCase.Exception.UnableToFindFieldName", meta.getFieldname())); //$NON-NLS-1$
            }

            data.inputValueMeta = getInputRowMeta().getValueMeta(data.fieldIndex); 
        }

        // We already know the target values, but we need to make sure that the input data type is the same as the specified one.
        // Perhaps there is some conversion needed.
        //
        Object lookupData = data.valueMeta.convertData(data.inputValueMeta, r[data.fieldIndex]);
        
        // Determine the output rowset to use...
        //
        RowSet rowSet = null;
        if (data.inputValueMeta.isNull(lookupData)) {
        	rowSet = data.nullRowSet;
        } else {
        	rowSet = data.outputMap.get(lookupData);
        }
        
        // If the rowset is still not found (unspecified key value, we drop down to the default option
        // For now: send it to the default step...
        //
        if (rowSet==null) {
        	if (data.defaultRowSet!=null) {
        		putRowTo(data.outputRowMeta, r, data.defaultRowSet);
        	}
        } else {
        	putRowTo(data.outputRowMeta, r, rowSet);
        }
        
        if (checkFeedback(getLinesRead())) 
        {
        	if (log.isBasic()) logBasic(Messages.getString("SwitchCase.Log.LineNumber")+getLinesRead()); //$NON-NLS-1$
        }
			
		return true;
	}

	/**
     * @see StepInterface#init( com.panet.imeta.trans.step.StepMetaInterface , com.panet.imeta.trans.step.StepDataInterface)
     */
    public boolean init(StepMetaInterface smi, StepDataInterface sdi)
    {
		meta=(SwitchCaseMeta)smi;
		data=(SwitchCaseData)sdi;

        if (super.init(smi, sdi))
        {
        	data.outputMap = new Hashtable<Object,RowSet>();
        	
        	if (Const.isEmpty(meta.getFieldname())) {
        		logError(Messages.getString("SwitchCase.Log.NoFieldSpecifiedToSwitchWith")); //$NON-NLS-1$
        		return false;
        	}
        	
        	data.valueMeta = new ValueMeta(meta.getFieldname(), meta.getCaseValueType());
        	data.valueMeta.setConversionMask(meta.getCaseValueFormat());
        	data.valueMeta.setGroupingSymbol(meta.getCaseValueGroup());
        	data.valueMeta.setDecimalSymbol(meta.getCaseValueDecimal());
        	
        	data.stringValueMeta = data.valueMeta.clone();
        	data.stringValueMeta.setType(ValueMetaInterface.TYPE_STRING);
        	
        	boolean ok=true;
        	for (int i=0;i<meta.getCaseTargetSteps().length;i++) {
        		if (meta.getCaseTargetSteps()[i]==null) {
        			logError(Messages.getString("SwitchCase.Log.NoTargetStepSpecifiedForValue", meta.getCaseValues()[i])); //$NON-NLS-1$
        			ok=false;
        		} else {
        			RowSet rowSet = findOutputRowSet(meta.getCaseTargetSteps()[i].getName());
        			if (rowSet!=null) {
	            		try {
	            			Object value = data.valueMeta.convertDataFromString(meta.getCaseValues()[i], data.stringValueMeta, null, null, ValueMeta.TRIM_TYPE_NONE);
	            			
	            			// If we have a value and a rowset, we can store the combination in the map
	            			//
	            			if (data.valueMeta.isNull(value)) {
	            				data.nullRowSet = rowSet;
	            			} else {
	            				data.outputMap.put(value, rowSet);
	            			}
	            			
	            		}
	            		catch(Exception e) {
	            			logError(Messages.getString("SwitchCase.Log.UnableToConvertValue", meta.getCaseValues()[i]), e); //$NON-NLS-1$
	            			ok=false;
	            		}
        			} else {
            			logError(Messages.getString("SwitchCase.Log.UnableToFindTargetRowSetForStep", meta.getCaseTargetSteps()[i].getName())); //$NON-NLS-1$
            			ok=false;
        			}
        		}
        	}
        	
        	if (meta.getDefaultTargetStep()!=null) {
        		data.defaultRowSet = findOutputRowSet(meta.getDefaultTargetStep().getName());
        	} else {
        		data.defaultRowSet = null;
        	}
        	
            return ok;
        }
        return false;
    }
	
	//
	// Run is were the action happens!
	public void run()
	{
    	BaseStep.runStepThread(this, meta, data);
	}
}
