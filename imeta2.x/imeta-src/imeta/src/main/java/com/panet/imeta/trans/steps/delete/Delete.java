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
package com.panet.imeta.trans.steps.delete;

import java.sql.SQLException;

import com.panet.imeta.core.Const;
import com.panet.imeta.core.database.Database;
import com.panet.imeta.core.database.DatabaseMeta;
import com.panet.imeta.core.exception.KettleDatabaseException;
import com.panet.imeta.core.exception.KettleException;
import com.panet.imeta.core.exception.KettleStepException;
import com.panet.imeta.core.row.RowMeta;
import com.panet.imeta.core.row.RowMetaInterface;
import com.panet.imeta.trans.Trans;
import com.panet.imeta.trans.TransMeta;
import com.panet.imeta.trans.step.BaseStep;
import com.panet.imeta.trans.step.StepDataInterface;
import com.panet.imeta.trans.step.StepInterface;
import com.panet.imeta.trans.step.StepMeta;
import com.panet.imeta.trans.step.StepMetaInterface;

/**
 * Delete data in a database table.
 * 
 * @author Tom
 * @since 28-March-2006
 */
public class Delete extends BaseStep implements StepInterface {
	private DeleteMeta meta;
	private DeleteData data;

	public Delete(StepMeta stepMeta, StepDataInterface stepDataInterface,
			int copyNr, TransMeta transMeta, Trans trans) {
		super(stepMeta, stepDataInterface, copyNr, transMeta, trans);
	}

	private synchronized void deleteValues(RowMetaInterface rowMeta,
			Object[] row) throws KettleException {
		// OK, now do the lookup.
		// We need the lookupvalues for that.
		Object[] deleteRow = new Object[data.deleteParameterRowMeta.size()];
		int deleteIndex = 0;

		for (int i = 0; i < meta.getKeyStream().length; i++) {
			if (data.keynrs[i] >= 0) {
				deleteRow[deleteIndex] = row[data.keynrs[i]];
				deleteIndex++;
			}
			if (data.keynrs2[i] >= 0) {
				deleteRow[deleteIndex] = row[data.keynrs2[i]];
				deleteIndex++;
			}
		}

		data.db.setValues(data.deleteParameterRowMeta, deleteRow,
				data.prepStatementDelete);

		if (log.isDebug())
			logDebug(Messages
					.getString(
							"Delete.Log.SetValuesForDelete", data.deleteParameterRowMeta.getString(deleteRow), rowMeta.getString(row))); //$NON-NLS-1$

		data.db.insertRow(data.prepStatementDelete);
		incrementLinesUpdated();
	}

	public boolean processRow(StepMetaInterface smi, StepDataInterface sdi)
			throws KettleException {
		meta = (DeleteMeta) smi;
		data = (DeleteData) sdi;

		boolean sendToErrorRow = false;
		String errorMessage = null;

		Object[] r = getRow(); // Get row from input rowset & set row busy!
		if (r == null) // no more input to be expected...
		{
			setOutputDone();
			return false;
		}

		if (first) {
			first = false;

			// What's the output Row format?
			data.outputRowMeta = getInputRowMeta().clone();
			meta.getFields(data.outputRowMeta, getStepname(), null, null, this);

			data.schemaTable = meta.getDatabaseMeta()
					.getQuotedSchemaTableCombination(
							environmentSubstitute(meta.getSchemaName()),
							environmentSubstitute(meta.getTableName()));

			// lookup the values!
			if (log.isDetailed())
				logDetailed(Messages.getString("Delete.Log.CheckingRow") + getInputRowMeta().getString(r)); //$NON-NLS-1$

			data.keynrs = new int[meta.getKeyStream().length];
			data.keynrs2 = new int[meta.getKeyStream().length];
			for (int i = 0; i < meta.getKeyStream().length; i++) {
				data.keynrs[i] = getInputRowMeta().indexOfValue(
						meta.getKeyStream()[i]);
				if ((data.keynrs[i] < 0 && !isConstance(meta.getKeyStream()[i]))
						&& // couldn't find field!
						!"IS NULL".equalsIgnoreCase(meta.getKeyCondition()[i]) && // No field needed! //$NON-NLS-1$
						!"IS NOT NULL".equalsIgnoreCase(meta.getKeyCondition()[i]) // No field needed! //$NON-NLS-1$
				) {
					throw new KettleStepException(
							Messages
									.getString(
											"Delete.Exception.FieldRequired", meta.getKeyStream()[i])); //$NON-NLS-1$ //$NON-NLS-2$
				}
				data.keynrs2[i] = getInputRowMeta().indexOfValue(
						meta.getKeyStream2()[i]);
				if ((data.keynrs2[i] < 0 && !isConstance(meta.getKeyStream2()[i]))
						&& // couldn't find field!
						"BETWEEN".equalsIgnoreCase(meta.getKeyCondition()[i]) // 2 fields needed! //$NON-NLS-1$
				) {
					throw new KettleStepException(
							Messages
									.getString(
											"Delete.Exception.FieldRequired", meta.getKeyStream2()[i])); //$NON-NLS-1$ //$NON-NLS-2$
				}

				if (log.isDebug())
					logDebug(Messages.getString(
							"Delete.Log.FieldInfo", meta.getKeyStream()[i]) + data.keynrs[i]); //$NON-NLS-1$ //$NON-NLS-2$
			}

			prepareDelete(getInputRowMeta());
		}

		try {
			deleteValues(getInputRowMeta(), r); // add new values to the row in
			// rowset[0].
			putRow(data.outputRowMeta, r); // output the same rows of data, but
			// with a copy of the metadata

			if (checkFeedback(getLinesRead())) {
				if (log.isBasic())
					logBasic(Messages.getString("Delete.Log.LineNumber") + getLinesRead()); //$NON-NLS-1$
			}
		} catch (KettleException e) {

			if (getStepMeta().isDoingErrorHandling()) {
				sendToErrorRow = true;
				errorMessage = e.toString();
			} else {

				logError(Messages.getString("Delete.Log.ErrorInStep") + e.getMessage()); //$NON-NLS-1$
				setErrors(1);
				stopAll();
				setOutputDone(); // signal end to receiver(s)
				return false;
			}

			if (sendToErrorRow) {
				// Simply add this row to the error row
				putError(getInputRowMeta(), r, 1, errorMessage, null, "DEL001");
			}
		}

		return true;
	}

	private boolean isConstance(String s) {
		if (Const.isEmpty(s))
			return false;
		if (s.startsWith("#"))
			return true;
		return false;
	}

	/**
	 * 
	 * 
	 * @return
	 */
	private String getContstance(String id, RowMetaInterface rowMeta) {
		if (Const.isEmpty(id))
			return null;

		if (id.startsWith("#")) {
			return id.substring(1);
		}

		if (rowMeta.searchValueMeta(id) == null) {
			return id;
		}
		return null;
	}

	// Lookup certain fields in a table
	public void prepareDelete(RowMetaInterface rowMeta)
			throws KettleDatabaseException {
		DatabaseMeta databaseMeta = meta.getDatabaseMeta();
		data.deleteParameterRowMeta = new RowMeta();

		String sql = "DELETE FROM " + data.schemaTable + Const.CR;

		// streams1 streams2 constanse
		String c1, c2;

		for (int i = 0; i < meta.getKeyLookup().length; i++) {
			if (i != 0)
				sql += "AND   ";
			else
				sql += "WHERE ";

			c1 = getContstance(meta.getKeyStream()[i], rowMeta);
			c2 = getContstance(meta.getKeyStream2()[i], rowMeta);

			sql += databaseMeta.quoteField(meta.getKeyLookup()[i]);
			if ("BETWEEN".equalsIgnoreCase(meta.getKeyCondition()[i])) {
				sql += " BETWEEN ";

				if (Const.isEmpty(c1)) {
					sql += "? ";
					data.deleteParameterRowMeta.addValueMeta(rowMeta
							.searchValueMeta(meta.getKeyStream()[i]));
				} else {
					sql += (c1 + " ");
				}
				if (Const.isEmpty(c2)) {
					sql += " AND ? ";
					data.deleteParameterRowMeta.addValueMeta(rowMeta
							.searchValueMeta(meta.getKeyStream2()[i]));
				} else {
					sql += (" AND " + c2 + " ");
				}

			} else if ("IS NULL".equalsIgnoreCase(meta.getKeyCondition()[i])
					|| "IS NOT NULL"
							.equalsIgnoreCase(meta.getKeyCondition()[i])) {
				sql += " " + meta.getKeyCondition()[i] + " ";
			} else if (!Const.isEmpty(c1)) {
				sql += " " + meta.getKeyCondition()[i] + " " + c1 + " ";
			} else {
				sql += " " + meta.getKeyCondition()[i] + " ? ";
				data.deleteParameterRowMeta.addValueMeta(rowMeta
						.searchValueMeta(meta.getKeyStream()[i]));
			}
		}

		System.out.println(sql);

		try {
			if (log.isDetailed())
				log.logDetailed(toString(),
						"Setting delete preparedStatement to [" + sql + "]");
			data.prepStatementDelete = data.db.getConnection()
					.prepareStatement(databaseMeta.stripCR(sql));
		} catch (SQLException ex) {
			throw new KettleDatabaseException(
					"Unable to prepare statement for SQL statement [" + sql
							+ "]", ex);
		}
	}

	public boolean init(StepMetaInterface smi, StepDataInterface sdi) {
		meta = (DeleteMeta) smi;
		data = (DeleteData) sdi;

		if (super.init(smi, sdi)) {
			data.db = new Database(meta.getDatabaseMeta());
			data.db.shareVariablesWith(this);
			try {
				if (getTransMeta().isUsingUniqueConnections()) {
					synchronized (getTrans()) {
						data.db.connect(getTrans().getThreadName(),
								getPartitionID());
					}
				} else {
					data.db.connect(getPartitionID());
				}

				if (log.isDetailed())
					logDetailed(Messages.getString("Delete.Log.ConnectedToDB")); //$NON-NLS-1$

				data.db.setCommit(meta.getCommitSize());

				return true;
			} catch (KettleException ke) {
				logError(Messages.getString("Delete.Log.ErrorOccurred") + ke.getMessage()); //$NON-NLS-1$
				setErrors(1);
				stopAll();
			}
		}
		return false;
	}

	public void dispose(StepMetaInterface smi, StepDataInterface sdi) {
		meta = (DeleteMeta) smi;
		data = (DeleteData) sdi;

		try {
			if (!data.db.isAutoCommit()) {
				if (getErrors() == 0) {
					data.db.commit();
				} else {
					data.db.rollback();
				}
			}
			data.db.closeUpdate();
		} catch (KettleDatabaseException e) {
			log
					.logError(
							toString(),
							Messages
									.getString("Delete.Log.UnableToCommitUpdateConnection") + data.db + "] :" + e.toString()); //$NON-NLS-1$ //$NON-NLS-2$
			setErrors(1);
		} finally {
			if (data.db != null) {
				data.db.disconnect();
			}
		}

		super.dispose(smi, sdi);
	}

	public String toString() {
		return this.getClass().getName();
	}

	//
	// Run is were the action happens!
	public void run() {
		BaseStep.runStepThread(this, meta, data);
	}
}
