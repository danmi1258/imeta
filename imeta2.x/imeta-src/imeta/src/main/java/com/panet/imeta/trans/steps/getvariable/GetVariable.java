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
 
package com.panet.imeta.trans.steps.getvariable;

import java.util.List;

import com.panet.imeta.core.exception.KettleException;
import com.panet.imeta.core.row.RowDataUtil;
import com.panet.imeta.core.row.RowMeta;
import com.panet.imeta.trans.Trans;
import com.panet.imeta.trans.TransMeta;
import com.panet.imeta.trans.step.BaseStep;
import com.panet.imeta.trans.step.StepDataInterface;
import com.panet.imeta.trans.step.StepInterface;
import com.panet.imeta.trans.step.StepMeta;
import com.panet.imeta.trans.step.StepMetaInterface;


/**
 * Get information from the System or the supervising transformation.
 * 
 * @author Matt 
 * @since 4-aug-2003
 */
public class GetVariable extends BaseStep implements StepInterface
{
	private GetVariableMeta meta;
	private GetVariableData data;
    
	public GetVariable(StepMeta stepMeta, StepDataInterface stepDataInterface, int copyNr, TransMeta transMeta, Trans trans)
	{
		super(stepMeta, stepDataInterface, copyNr, transMeta, trans);
        setName(stepMeta.getName());
	}
	
	
	public boolean processRow(StepMetaInterface smi, StepDataInterface sdi) throws KettleException
	{
		Object[] rowData;
		
		if (data.readsRows)
		{
			rowData = getRow();
			if (rowData==null)
			{
				setOutputDone();
				return false;
			}
		}
		else
		{
			rowData=RowDataUtil.allocateRowData(0);
			incrementLinesRead();
		}
		
		// initialize 
		if (first && rowData!=null)
		{
			first=false;
			
            
            // Make output meta data
			//
			if (data.readsRows) {
				data.inputRowMeta = getInputRowMeta();
			}
			else {
				data.inputRowMeta = new RowMeta();
			}
            data.outputRowMeta = data.inputRowMeta.clone();
            meta.getFields(data.outputRowMeta, getStepname(), null, null, this);
		}
		
        // Add the variables to the row...
		//
		// TODO: Maybe keep the Object[] for speed, although this step will always
		//       be used in "small" amounts.
        // 
		Object extraData[] = new Object[meta.getFieldName().length];
        for (int i=0;i<meta.getFieldName().length;i++)
        {
            String newValue = environmentSubstitute(meta.getVariableString()[i]);
            if ( log.isDetailed() )
                logDetailed("field ["+meta.getFieldName()[i]+"] has value ["+newValue+"]");
            extraData[i] = newValue;
        }
        
        rowData = RowDataUtil.addRowData(rowData, data.inputRowMeta.size(), extraData);
        
        putRow(data.outputRowMeta, rowData);		     
					
        if (!data.readsRows) // Just one row and then stop!
        {
            setOutputDone();
            return false;
        }
        
		return true;
	}
	
	public boolean init(StepMetaInterface smi, StepDataInterface sdi)
	{
		meta=(GetVariableMeta)smi;
		data=(GetVariableData)sdi;
		
		if (super.init(smi, sdi))
		{
		    // Add init code here.
			data.readsRows = false;
	        List<StepMeta> previous = getTransMeta().findPreviousSteps(getStepMeta());
			if (previous!=null && previous.size()>0)
			{
				data.readsRows = true;
			}
			
		    return true;
		}
		return false;
	}
	
	public void dispose(StepMetaInterface smi, StepDataInterface sdi)
	{
		super.dispose(smi, sdi);
	}

	//
	// Run is were the action happens!
	public void run()
	{
    	BaseStep.runStepThread(this, meta, data);
	}
}
