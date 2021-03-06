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

package com.panet.imeta.trans.steps.textfileinput;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
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
import com.panet.imeta.resource.ResourceEntry;
import com.panet.imeta.resource.ResourceReference;
import com.panet.imeta.resource.ResourceEntry.ResourceType;
import com.panet.imeta.shared.SharedObjectInterface;
import com.panet.imeta.trans.Trans;
import com.panet.imeta.trans.TransMeta;
import com.panet.imeta.trans.step.BaseStepMeta;
import com.panet.imeta.trans.step.StepDataInterface;
import com.panet.imeta.trans.step.StepInterface;
import com.panet.imeta.trans.step.StepMeta;
import com.panet.imeta.trans.step.StepMetaInterface;

public class TextFileInputMeta extends BaseStepMeta implements
		StepMetaInterface, InputFileMetaInterface {
	public static final String STEP_ATTRIBUTE_ACCEPT_FILENAMES = "accept_filenames";
	public static final String STEP_ATTRIBUTE_PASSING_THROUGH_FIELDS = "passing_through_fields";
	public static final String STEP_ATTRIBUTE_ACCEPT_FIELD = "accept_field";
	public static final String STEP_ATTRIBUTE_ACCEPT_STEPNAME = "accept_stepname";
	public static final String STEP_ATTRIBUTE_SEPARATOR = "separator";
	public static final String STEP_ATTRIBUTE_ENCLOSURE = "enclosure";
	public static final String STEP_ATTRIBUTE_ENCLOSURE_BREAKS = "enclosure_breaks";
	public static final String STEP_ATTRIBUTE_ESCAPECHAR = "escapechar";
	public static final String STEP_ATTRIBUTE_HEADER = "header";
	public static final String STEP_ATTRIBUTE_NR_HEADERLINES = "nr_headerlines";
	public static final String STEP_ATTRIBUTE_FOOTER = "footer";
	public static final String STEP_ATTRIBUTE_NR_FOOTERLINES = "nr_footerlines";
	public static final String STEP_ATTRIBUTE_LINE_WRAPPED = "line_wrapped";
	public static final String STEP_ATTRIBUTE_NR_WRAPS = "nr_wraps";
	public static final String STEP_ATTRIBUTE_LAYOUT_PAGED = "layout_paged";
	public static final String STEP_ATTRIBUTE_NR_LINES_PER_PAGE = "nr_lines_per_page";
	public static final String STEP_ATTRIBUTE_NR_LINES_DOC_HEADER = "nr_lines_doc_header";
	public static final String STEP_ATTRIBUTE_NOEMPTY = "noempty";
	public static final String STEP_ATTRIBUTE_INCLUDE = "include";
	public static final String STEP_ATTRIBUTE_INCLUDE_FIELD = "include_field";
	public static final String STEP_ATTRIBUTE_ROWNUM = "rownum";
	public static final String STEP_ATTRIBUTE_ROWNUMBYFILE = "rownumByFile";
	public static final String STEP_ATTRIBUTE_ROWNUM_FIELD = "rownum_field";
	public static final String STEP_ATTRIBUTE_FORMAT = "format";
	public static final String STEP_ATTRIBUTE_ENCODING = "encoding";
	public static final String STEP_ATTRIBUTE_ADD_TO_RESULT_FILENAMES = "add_to_result_filenames";
	public static final String STEP_ATTRIBUTE_LIMIT = "limit";
	public static final String STEP_ATTRIBUTE_FILE_NAME = "file_name";
	public static final String STEP_ATTRIBUTE_FIELD_NAME = "field_name";
	public static final String STEP_ATTRIBUTE_FILTER_STRING = "filter_string";
	// public static final String STEP_ATTRIBUTE_FILE_NAME = "file_name";
	public static final String STEP_ATTRIBUTE_FILE_MASK = "file_mask";
	public static final String STEP_ATTRIBUTE_FILE_REQUIRED = "file_required";
	public static final String STEP_ATTRIBUTE_FILE_TYPE = "file_type";
	public static final String STEP_ATTRIBUTE_COMPRESSION = "compression";
	public static final String STEP_ATTRIBUTE_FILE_ZIPPED = "file_zipped";
	public static final String STEP_ATTRIBUTE_FILTER_POSITION = "filter_position";
	// public static final String STEP_ATTRIBUTE_FILTER_STRING =
	// "filter_string";
	public static final String STEP_ATTRIBUTE_FILTER_IS_LAST_LINE = "filter_is_last_line";
	// public static final String STEP_ATTRIBUTE_FIELD_NAME = "field_name";
	public static final String STEP_ATTRIBUTE_FIELD_TYPE = "field_type";
	public static final String STEP_ATTRIBUTE_FIELD_FORMAT = "field_format";
	public static final String STEP_ATTRIBUTE_FIELD_CURRENCY = "field_currency";
	public static final String STEP_ATTRIBUTE_FIELD_DECIMAL = "field_decimal";
	public static final String STEP_ATTRIBUTE_FIELD_GROUP = "field_group";
	public static final String STEP_ATTRIBUTE_FIELD_NULLIF = "field_nullif";
	public static final String STEP_ATTRIBUTE_FIELD_IFNULL = "field_ifnull";
	public static final String STEP_ATTRIBUTE_FIELD_POSITION = "field_position";
	public static final String STEP_ATTRIBUTE_FIELD_LENGTH = "field_length";
	public static final String STEP_ATTRIBUTE_FIELD_PRECISION = "field_precision";
	public static final String STEP_ATTRIBUTE_FIELD_TRIM_TYPE = "field_trim_type";
	public static final String STEP_ATTRIBUTE_FIELD_REPEAT = "field_repeat";
	public static final String STEP_ATTRIBUTE_ERROR_IGNORED = "error_ignored";
	public static final String STEP_ATTRIBUTE_ERROR_LINE_SKIPPED = "error_line_skipped";
	public static final String STEP_ATTRIBUTE_ERROR_COUNT_FIELD = "error_count_field";
	public static final String STEP_ATTRIBUTE_ERROR_FIELDS_FIELD = "error_fields_field";
	public static final String STEP_ATTRIBUTE_ERROR_TEXT_FIELD = "error_text_field";
	public static final String STEP_ATTRIBUTE_BAD_LINE_FILES_DEST_DIR = "bad_line_files_dest_dir";
	public static final String STEP_ATTRIBUTE_BAD_LINE_FILES_EXT = "bad_line_files_ext";
	public static final String STEP_ATTRIBUTE_ERROR_LINE_FILES_DEST_DIR = "error_line_files_dest_dir";
	public static final String STEP_ATTRIBUTE_ERROR_LINE_FILES_EXT = "error_line_files_ext";
	public static final String STEP_ATTRIBUTE_LINE_NUMBER_FILES_DEST_DIR = "line_number_files_dest_dir";
	public static final String STEP_ATTRIBUTE_LINE_NUMBER_FILES_EXT = "line_number_files_ext";
	public static final String STEP_ATTRIBUTE_DATE_FORMAT_LENIENT = "date_format_lenient";
	public static final String STEP_ATTRIBUTE_DATE_FORMAT_LOCALE = "date_format_locale";

	private static final String NO = "N";

	private static final String YES = "Y";

	private static final String STRING_BASE64_PREFIX = "Base64: ";

	public static final int FILE_FORMAT_DOS = 0;
	public static final int FILE_FORMAT_UNIX = 1;
	public static final int FILE_FORMAT_MIXED = 2;

	public static final int FILE_TYPE_CSV = 0;
	public static final int FILE_TYPE_FIXED = 1;
	
	public static final String FILE_TYPE_CSV_TEXT = "CSV";
	public static final String FILE_TYPE_FIXED_TEXT = "Fixed";

	/** Array of filenames */
	private String fileName[];

	/** Wildcard or filemask (regular expression) */
	private String fileMask[];

	/** Array of boolean values as string, indicating if a file is required. */
	private String fileRequired[];

	/** Type of file: CSV or fixed */
	private String fileType;

	/** String used to separated field (;) */
	private String separator;

	/** String used to enclose separated fields (") */
	private String enclosure;

	/** Escape character used to escape the enclosure String (\) */
	private String escapeCharacter;

	/** Switch to allow breaks (CR/LF) in Enclosures */
	private boolean breakInEnclosureAllowed;

	/**
	 * Flag indicating that the file contains one header line that should be
	 * skipped.
	 */
	private boolean header;

	/** The number of header lines, defaults to 1 */
	private int nrHeaderLines;

	/**
	 * Flag indicating that the file contains one footer line that should be
	 * skipped.
	 */
	private boolean footer;

	/** The number of footer lines, defaults to 1 */
	private int nrFooterLines;

	/**
	 * Flag indicating that a single line is wrapped onto one or more lines in
	 * the text file.
	 */
	private boolean lineWrapped;

	/** The number of times the line wrapped */
	private int nrWraps;

	/** Flag indicating that the text-file has a paged layout. */
	private boolean layoutPaged;

	/** The number of lines in the document header */
	private int nrLinesDocHeader;

	/** The number of lines to read per page */
	private int nrLinesPerPage;

	/** Type of compression being used */
	private String fileCompression;

	/** Flag indicating that we should skip all empty lines */
	private boolean noEmptyLines;

	/** Flag indicating that we should include the filename in the output */
	private boolean includeFilename;

	/** The name of the field in the output containing the filename */
	private String filenameField;

	/** Flag indicating that a row number field should be included in the output */
	private boolean includeRowNumber;

	/** Flag indicating row number is per file */
	private boolean rowNumberByFile;

	/** The name of the field in the output containing the row number */
	private String rowNumberField;

	/** The file format: DOS or UNIX or mixed */
	private String fileFormat;

	/** The maximum number or lines to read */
	private long rowLimit;

	/** The fields to import... */
	private TextFileInputField inputFields[];

	/** The filters to use... */
	private TextFileFilter filter[];

	/**
	 * The encoding to use for reading: null or empty string means system
	 * default encoding
	 */
	private String encoding;

	/** Ignore error : turn into warnings */
	private boolean errorIgnored;

	/** The name of the field that will contain the number of errors in the row */
	private String errorCountField;

	/**
	 * The name of the field that will contain the names of the fields that
	 * generated errors, separated by ,
	 */
	private String errorFieldsField;

	/** The name of the field that will contain the error texts, separated by CR */
	private String errorTextField;

	/** The directory that will contain warning files */
	private String warningFilesDestinationDirectory;

	/** The extension of warning files */
	private String warningFilesExtension;

	/** The directory that will contain error files */
	private String errorFilesDestinationDirectory;

	/** The extension of error files */
	private String errorFilesExtension;

	/** The directory that will contain line number files */
	private String lineNumberFilesDestinationDirectory;

	/** The extension of line number files */
	private String lineNumberFilesExtension;

	/**
	 * Indicate whether or not we want to date fields strictly according to the
	 * format or lenient
	 */
	private boolean dateFormatLenient;

	/** Specifies the Locale of the Date format, null means the default */
	private Locale dateFormatLocale;

	/** If error line are skipped, you can replay without introducing doubles. */
	private boolean errorLineSkipped;

	/** Are we accepting filenames in input rows? */
	private boolean acceptingFilenames;

	/** If receiving input rows, should we pass through existing fields? */
	private boolean passingThruFields;

	/** The field in which the filename is placed */
	private String acceptingField;

	/** The stepname to accept filenames from */
	private String acceptingStepName;

	/** The step to accept filenames from */
	private StepMeta acceptingStep;

	/** The add filenames to result filenames flag */
	private boolean isaddresult;

	/**
	 * @return Returns the encoding.
	 */
	public String getEncoding() {
		return encoding;
	}

	/**
	 * @param encoding
	 *            The encoding to set.
	 */
	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}

	public TextFileInputMeta() {
		super(); // allocate BaseStepMeta
	}

	/**
	 * @return Returns the input fields.
	 */
	public TextFileInputField[] getInputFields() {
		return inputFields;
	}

	/**
	 * @param inputFields
	 *            The input fields to set.
	 */
	public void setInputFields(TextFileInputField[] inputFields) {
		this.inputFields = inputFields;
	}

	/**
	 * @return Returns the enclosure.
	 */
	public String getEnclosure() {
		return enclosure;
	}

	/**
	 * @param enclosure
	 *            The enclosure to set.
	 */
	public void setEnclosure(String enclosure) {
		this.enclosure = enclosure;
	}

	/**
	 * @return Returns the breakInEnclosureAllowed.
	 */
	public boolean isBreakInEnclosureAllowed() {
		return breakInEnclosureAllowed;
	}

	/**
	 * @param breakInEnclosureAllowed
	 *            The breakInEnclosureAllowed to set.
	 */
	public void setBreakInEnclosureAllowed(boolean breakInEnclosureAllowed) {
		this.breakInEnclosureAllowed = breakInEnclosureAllowed;
	}

	/**
	 * @return Returns the fileFormat.
	 */
	public String getFileFormat() {
		return fileFormat;
	}

	/**
	 * @param fileFormat
	 *            The fileFormat to set.
	 */
	public void setFileFormat(String fileFormat) {
		this.fileFormat = fileFormat;
	}

	/**
	 * @return Returns the fileMask.
	 */
	public String[] getFileMask() {
		return fileMask;
	}

	/**
	 * @return Returns the fileRequired.
	 */
	public String[] getFileRequired() {
		return fileRequired;
	}

	/**
	 * @param fileMask
	 *            The fileMask to set.
	 */
	public void setFileMask(String[] fileMask) {
		this.fileMask = fileMask;
	}

	/**
	 * @param fileRequired
	 *            The fileRequired to set.
	 */
	public void setFileRequired(String[] fileRequired) {
		this.fileRequired = fileRequired;
	}

	/**
	 * @return Returns the fileName.
	 */
	public String[] getFileName() {
		return fileName;
	}

	/**
	 * @param fileName
	 *            The fileName to set.
	 */
	public void setFileName(String[] fileName) {
		this.fileName = fileName;
	}

	/**
	 * @return Returns the filenameField.
	 */
	public String getFilenameField() {
		return filenameField;
	}

	/**
	 * @param filenameField
	 *            The filenameField to set.
	 */
	public void setFilenameField(String filenameField) {
		this.filenameField = filenameField;
	}

	/**
	 * @return Returns the fileType.
	 */
	public String getFileType() {
		return fileType;
	}

	/**
	 * @param fileType
	 *            The fileType to set.
	 */
	public void setFileType(String fileType) {
		this.fileType = fileType;
	}

	/**
	 * @return The array of filters for the metadata of this text file input
	 *         step.
	 */
	public TextFileFilter[] getFilter() {
		return filter;
	}

	/**
	 * @param filter
	 *            The array of filters to use
	 */
	public void setFilter(TextFileFilter[] filter) {
		this.filter = filter;
	}

	/**
	 * @return Returns the footer.
	 */
	public boolean hasFooter() {
		return footer;
	}

	/**
	 * @param footer
	 *            The footer to set.
	 */
	public void setFooter(boolean footer) {
		this.footer = footer;
	}

	/**
	 * @return Returns the header.
	 */
	public boolean hasHeader() {
		return header;
	}

	/**
	 * @param header
	 *            The header to set.
	 */
	public void setHeader(boolean header) {
		this.header = header;
	}

	/**
	 * @return Returns the includeFilename.
	 */
	public boolean includeFilename() {
		return includeFilename;
	}

	/**
	 * @param includeFilename
	 *            The includeFilename to set.
	 */
	public void setIncludeFilename(boolean includeFilename) {
		this.includeFilename = includeFilename;
	}

	/**
	 * @return Returns the includeRowNumber.
	 */
	public boolean includeRowNumber() {
		return includeRowNumber;
	}

	/**
	 * @param includeRowNumber
	 *            The includeRowNumber to set.
	 */
	public void setIncludeRowNumber(boolean includeRowNumber) {
		this.includeRowNumber = includeRowNumber;
	}

	/**
	 * true if row number reset for each file
	 * 
	 * @return rowNumberByFile
	 */
	public boolean isRowNumberByFile() {
		return rowNumberByFile;
	}

	/**
	 * @param rowNumberByFile
	 *            True if row number field is reset for each file
	 */
	public void setRowNumberByFile(boolean rowNumberByFile) {
		this.rowNumberByFile = rowNumberByFile;
	}

	/**
	 * @return Returns the noEmptyLines.
	 */
	public boolean noEmptyLines() {
		return noEmptyLines;
	}

	/**
	 * @param noEmptyLines
	 *            The noEmptyLines to set.
	 */
	public void setNoEmptyLines(boolean noEmptyLines) {
		this.noEmptyLines = noEmptyLines;
	}

	/**
	 * @return Returns the rowLimit.
	 */
	public long getRowLimit() {
		return rowLimit;
	}

	/**
	 * @param rowLimit
	 *            The rowLimit to set.
	 */
	public void setRowLimit(long rowLimit) {
		this.rowLimit = rowLimit;
	}

	/**
	 * @return Returns the rowNumberField.
	 */
	public String getRowNumberField() {
		return rowNumberField;
	}

	/**
	 * @param rowNumberField
	 *            The rowNumberField to set.
	 */
	public void setRowNumberField(String rowNumberField) {
		this.rowNumberField = rowNumberField;
	}

	/**
	 * @return Returns the separator.
	 */
	public String getSeparator() {
		return separator;
	}

	/**
	 * @param separator
	 *            The separator to set.
	 */
	public void setSeparator(String separator) {
		this.separator = separator;
	}

	/**
	 * @return Returns the type of compression used
	 */
	public String getFileCompression() {
		return fileCompression;
	}

	/**
	 * @param fileCompression
	 *            Sets the compression type
	 */
	public void setFileCompression(String fileCompression) {
		this.fileCompression = fileCompression;
	}

	public void loadXML(Node stepnode, List<DatabaseMeta> databases,
			Map<String, Counter> counters) throws KettleXMLException {
		readData(stepnode);
	}

	public Object clone() {
		TextFileInputMeta retval = (TextFileInputMeta) super.clone();

		int nrfiles = fileName.length;
		int nrfields = inputFields.length;
		int nrfilters = filter.length;

		retval.allocate(nrfiles, nrfields, nrfilters);

		for (int i = 0; i < nrfiles; i++) {
			retval.fileName[i] = fileName[i];
			retval.fileMask[i] = fileMask[i];
			retval.fileRequired[i] = fileRequired[i];
		}

		for (int i = 0; i < nrfields; i++) {
			retval.inputFields[i] = (TextFileInputField) inputFields[i].clone();
		}

		for (int i = 0; i < nrfilters; i++) {
			retval.filter[i] = (TextFileFilter) filter[i].clone();
		}

		retval.dateFormatLocale = (Locale) dateFormatLocale.clone();

		return retval;
	}

	public void allocate(int nrfiles, int nrfields, int nrfilters) {
		fileName = new String[nrfiles];
		fileMask = new String[nrfiles];
		fileRequired = new String[nrfiles];

		inputFields = new TextFileInputField[nrfields];

		filter = new TextFileFilter[nrfilters];
	}

	public void setDefault() {
		isaddresult = true;
		separator = ";";
		enclosure = "\"";
		breakInEnclosureAllowed = false;
		header = true;
		nrHeaderLines = 1;
		footer = false;
		nrFooterLines = 1;
		lineWrapped = false;
		nrWraps = 1;
		layoutPaged = false;
		nrLinesPerPage = 80;
		nrLinesDocHeader = 0;
		fileCompression = "None";
		noEmptyLines = true;
		fileFormat = "DOS";
		fileType = "CSV";
		includeFilename = false;
		filenameField = "";
		includeRowNumber = false;
		rowNumberField = "";
		errorIgnored = false;
		errorLineSkipped = false;
		warningFilesDestinationDirectory = null;
		warningFilesExtension = "warning";
		errorFilesDestinationDirectory = null;
		errorFilesExtension = "error";
		lineNumberFilesDestinationDirectory = null;
		lineNumberFilesExtension = "line";
		dateFormatLenient = true;
		rowNumberByFile = false;

		int nrfiles = 0;
		int nrfields = 0;
		int nrfilters = 0;

		allocate(nrfiles, nrfields, nrfilters);

		for (int i = 0; i < nrfiles; i++) {
			fileName[i] = "filename" + (i + 1);
			fileMask[i] = "";
			fileRequired[i] = NO;
		}

		for (int i = 0; i < nrfields; i++) {
			inputFields[i] = new TextFileInputField("field" + (i + 1), 1, -1);
		}

		dateFormatLocale = Locale.getDefault();

		rowLimit = 0L;
	}

	public void getFields(RowMetaInterface row, String name,
			RowMetaInterface[] info, StepMeta nextStep, VariableSpace space)
			throws KettleStepException {
		if (info != null && isPassingThruFields()) {
			boolean found = false;
			for (int i = 0; i < info.length && !found; i++) {
				if (info[i] != null) {
					row.mergeRowMeta(info[i]);
					found = true;
				}
			}
		}

		for (int i = 0; i < inputFields.length; i++) {
			TextFileInputField field = inputFields[i];

			int type = field.getType();
			if (type == ValueMetaInterface.TYPE_NONE)
				type = ValueMetaInterface.TYPE_STRING;

			ValueMetaInterface v = new ValueMeta(field.getName(), type);
			v.setLength(field.getLength());
			v.setPrecision(field.getPrecision());
			v.setOrigin(name);
			v.setConversionMask(field.getFormat());
			v.setDecimalSymbol(field.getDecimalSymbol());
			v.setGroupingSymbol(field.getGroupSymbol());
			v.setCurrencySymbol(field.getCurrencySymbol());
			v.setDateFormatLenient(dateFormatLenient);
			v.setDateFormatLocale(dateFormatLocale);
			v.setTrimType(field.getTrimType());

			row.addValueMeta(v);
		}
		if (errorIgnored) {
			if (errorCountField != null && errorCountField.length() > 0) {
				ValueMetaInterface v = new ValueMeta(errorCountField,
						ValueMetaInterface.TYPE_INTEGER);
				v.setLength(ValueMetaInterface.DEFAULT_INTEGER_LENGTH, 0);
				v.setOrigin(name);
				row.addValueMeta(v);
			}
			if (errorFieldsField != null && errorFieldsField.length() > 0) {
				ValueMetaInterface v = new ValueMeta(errorFieldsField,
						ValueMetaInterface.TYPE_STRING);
				v.setOrigin(name);
				row.addValueMeta(v);
			}
			if (errorTextField != null && errorTextField.length() > 0) {
				ValueMetaInterface v = new ValueMeta(errorTextField,
						ValueMetaInterface.TYPE_STRING);
				v.setOrigin(name);
				row.addValueMeta(v);
			}
		}
		if (includeFilename) {
			ValueMetaInterface v = new ValueMeta(filenameField,
					ValueMetaInterface.TYPE_STRING);
			v.setLength(100);
			v.setOrigin(name);
			row.addValueMeta(v);
		}
		if (includeRowNumber) {
			ValueMetaInterface v = new ValueMeta(rowNumberField,
					ValueMetaInterface.TYPE_INTEGER);
			v.setLength(ValueMetaInterface.DEFAULT_INTEGER_LENGTH, 0);
			v.setOrigin(name);
			row.addValueMeta(v);
		}
	}

	public String getXML() {
		StringBuffer retval = new StringBuffer(1500);

		retval.append("    ").append(
				XMLHandler.addTagValue("accept_filenames", acceptingFilenames));
		retval.append("    ").append(
				XMLHandler.addTagValue("passing_through_fields",
						passingThruFields));
		retval.append("    ").append(
				XMLHandler.addTagValue("accept_field", acceptingField));
		retval.append("    ")
				.append(
						XMLHandler.addTagValue("accept_stepname",
								(acceptingStep != null ? acceptingStep
										.getName() : "")));

		retval.append("    ").append(
				XMLHandler.addTagValue("separator", separator));
		retval.append("    ").append(
				XMLHandler.addTagValue("enclosure", enclosure));
		retval.append("    ").append(
				XMLHandler.addTagValue("enclosure_breaks",
						breakInEnclosureAllowed));
		retval.append("    ").append(
				XMLHandler.addTagValue("escapechar", escapeCharacter));
		retval.append("    ").append(XMLHandler.addTagValue("header", header));
		retval.append("    ").append(
				XMLHandler.addTagValue("nr_headerlines", nrHeaderLines));
		retval.append("    ").append(XMLHandler.addTagValue("footer", footer));
		retval.append("    ").append(
				XMLHandler.addTagValue("nr_footerlines", nrFooterLines));
		retval.append("    ").append(
				XMLHandler.addTagValue("line_wrapped", lineWrapped));
		retval.append("    ").append(
				XMLHandler.addTagValue("nr_wraps", nrWraps));
		retval.append("    ").append(
				XMLHandler.addTagValue("layout_paged", layoutPaged));
		retval.append("    ").append(
				XMLHandler.addTagValue("nr_lines_per_page", nrLinesPerPage));
		retval.append("    ")
				.append(
						XMLHandler.addTagValue("nr_lines_doc_header",
								nrLinesDocHeader));
		retval.append("    ").append(
				XMLHandler.addTagValue("noempty", noEmptyLines));
		retval.append("    ").append(
				XMLHandler.addTagValue("include", includeFilename));
		retval.append("    ").append(
				XMLHandler.addTagValue("include_field", filenameField));
		retval.append("    ").append(
				XMLHandler.addTagValue("rownum", includeRowNumber));
		retval.append("    ").append(
				XMLHandler.addTagValue("rownumByFile", rowNumberByFile));
		retval.append("    ").append(
				XMLHandler.addTagValue("rownum_field", rowNumberField));
		retval.append("    ").append(
				XMLHandler.addTagValue("format", fileFormat));
		retval.append("    ").append(
				XMLHandler.addTagValue("encoding", encoding));
		retval.append("    "
				+ XMLHandler
						.addTagValue("add_to_result_filenames", isaddresult));

		retval.append("    <file>").append(Const.CR);
		for (int i = 0; i < fileName.length; i++) {
			retval.append("      ").append(
					XMLHandler.addTagValue("name", fileName[i]));
			retval.append("      ").append(
					XMLHandler.addTagValue("filemask", fileMask[i]));
			retval.append("      ").append(
					XMLHandler.addTagValue("file_required", fileRequired[i]));
		}
		retval.append("      ")
				.append(XMLHandler.addTagValue("type", fileType));
		retval.append("      ").append(
				XMLHandler.addTagValue("compression", fileCompression));
		retval.append("    </file>").append(Const.CR);

		retval.append("    <filters>").append(Const.CR);
		for (int i = 0; i < filter.length; i++) {
			String filterString = filter[i].getFilterString();
			byte[] filterBytes = new byte[] {};
			String filterPrefix = "";
			if (filterString != null) {
				filterBytes = filterString.getBytes();
				filterPrefix = STRING_BASE64_PREFIX;
			}
			String filterEncoded = filterPrefix
					+ new String(Base64.encodeBase64(filterBytes));

			retval.append("      <filter>").append(Const.CR);
			retval.append("        ").append(
					XMLHandler.addTagValue("filter_string", filterEncoded,
							false));
			retval.append("        ").append(
					XMLHandler.addTagValue("filter_position", filter[i]
							.getFilterPosition(), false));
			retval.append("        ").append(
					XMLHandler.addTagValue("filter_is_last_line", filter[i]
							.isFilterLastLine(), false));
			retval.append("      </filter>").append(Const.CR);
		}
		retval.append("    </filters>").append(Const.CR);

		retval.append("    <fields>").append(Const.CR);
		for (int i = 0; i < inputFields.length; i++) {
			TextFileInputField field = inputFields[i];

			retval.append("      <field>").append(Const.CR);
			retval.append("        ").append(
					XMLHandler.addTagValue("name", field.getName()));
			retval.append("        ").append(
					XMLHandler.addTagValue("type", field.getTypeDesc()));
			retval.append("        ").append(
					XMLHandler.addTagValue("format", field.getFormat()));
			retval.append("        ").append(
					XMLHandler.addTagValue("currency", field
							.getCurrencySymbol()));
			retval.append("        ")
					.append(
							XMLHandler.addTagValue("decimal", field
									.getDecimalSymbol()));
			retval.append("        ").append(
					XMLHandler.addTagValue("group", field.getGroupSymbol()));
			retval.append("        ").append(
					XMLHandler.addTagValue("nullif", field.getNullString()));
			retval.append("        ").append(
					XMLHandler.addTagValue("ifnull", field.getIfNullValue()));
			retval.append("        ").append(
					XMLHandler.addTagValue("position", field.getPosition()));
			retval.append("        ").append(
					XMLHandler.addTagValue("length", field.getLength()));
			retval.append("        ").append(
					XMLHandler.addTagValue("precision", field.getPrecision()));
			retval.append("        ").append(
					XMLHandler
							.addTagValue("trim_type", field.getTrimTypeCode()));
			retval.append("        ").append(
					XMLHandler.addTagValue("repeat", field.isRepeated()));
			retval.append("      </field>").append(Const.CR);
		}
		retval.append("    </fields>").append(Const.CR);
		retval.append("    ").append(XMLHandler.addTagValue("limit", rowLimit));

		// ERROR HANDLING
		retval.append("    ").append(
				XMLHandler.addTagValue("error_ignored", errorIgnored));
		retval.append("    ").append(
				XMLHandler.addTagValue("error_line_skipped", errorLineSkipped));
		retval.append("    ").append(
				XMLHandler.addTagValue("error_count_field", errorCountField));
		retval.append("    ").append(
				XMLHandler.addTagValue("error_fields_field", errorFieldsField));
		retval.append("    ").append(
				XMLHandler.addTagValue("error_text_field", errorTextField));

		retval.append("    ").append(
				XMLHandler.addTagValue("bad_line_files_destination_directory",
						warningFilesDestinationDirectory));
		retval.append("    ").append(
				XMLHandler.addTagValue("bad_line_files_extension",
						warningFilesExtension));
		retval.append("    ").append(
				XMLHandler.addTagValue(
						"error_line_files_destination_directory",
						errorFilesDestinationDirectory));
		retval.append("    ").append(
				XMLHandler.addTagValue("error_line_files_extension",
						errorFilesExtension));
		retval.append("    ").append(
				XMLHandler.addTagValue(
						"line_number_files_destination_directory",
						lineNumberFilesDestinationDirectory));
		retval.append("    ").append(
				XMLHandler.addTagValue("line_number_files_extension",
						lineNumberFilesExtension));

		retval.append("    ").append(
				XMLHandler
						.addTagValue("date_format_lenient", dateFormatLenient));
		retval.append("    ").append(
				XMLHandler.addTagValue("date_format_locale", dateFormatLocale
						.toString()));

		return retval.toString();
	}

	public void setInfo(Map<String, String[]> p, String id,
			List<? extends SharedObjectInterface> databases) {
		acceptingFilenames = BaseStepMeta.parameterToBoolean(p.get(id
				+ ".acceptingFilenames"));
		passingThruFields = BaseStepMeta.parameterToBoolean(p.get(id
				+ ".passingThruFields"));
		acceptingField = BaseStepMeta.parameterToString(p.get(id
				+ ".acceptingField"));
		acceptingStepName = BaseStepMeta.parameterToString(p.get(id
				+ ".acceptingStepName"));
		separator = BaseStepMeta.parameterToString(p.get(id + ".separator"));
		enclosure = BaseStepMeta.parameterToString(p.get(id + ".enclosure"));
		breakInEnclosureAllowed = BaseStepMeta.parameterToBoolean(p.get(id
				+ ".breakInEnclosureAllowed"));
		escapeCharacter = BaseStepMeta.parameterToString(p.get(id
				+ ".escapeCharacter"));
		header = BaseStepMeta.parameterToBoolean(p.get(id + ".header"));
		nrHeaderLines = BaseStepMeta.parameterToInt(p
				.get(id + ".nrHeaderLines"));
		footer = BaseStepMeta.parameterToBoolean(p.get(id + ".footer"));
		nrFooterLines = BaseStepMeta.parameterToInt(p
				.get(id + ".nrFooterLines"));
		lineWrapped = BaseStepMeta.parameterToBoolean(p
				.get(id + ".lineWrapped"));
		nrWraps = BaseStepMeta.parameterToInt(p.get(id + ".nrWraps"));
		layoutPaged = BaseStepMeta.parameterToBoolean(p
				.get(id + ".layoutPaged"));
		nrLinesPerPage = BaseStepMeta.parameterToInt(p.get(id
				+ ".nrLinesPerPage"));
		nrLinesDocHeader = BaseStepMeta.parameterToInt(p.get(id
				+ ".nrLinesDocHeader"));
		isaddresult = BaseStepMeta.parameterToBoolean(p
				.get(id + ".isaddresult"));
		noEmptyLines = BaseStepMeta.parameterToBoolean(p.get(id
				+ ".noEmptyLines"));
		includeFilename = BaseStepMeta.parameterToBoolean(p.get(id
				+ ".includeFilename"));
		filenameField = BaseStepMeta.parameterToString(p.get(id
				+ ".filenameField"));
		includeRowNumber = BaseStepMeta.parameterToBoolean(p.get(id
				+ ".includeRowNumber"));
		rowNumberByFile = BaseStepMeta.parameterToBoolean(p.get(id
				+ ".rowNumberByFile"));
		rowNumberField = BaseStepMeta.parameterToString(p.get(id
				+ ".rowNumberField"));
		fileFormat = BaseStepMeta.parameterToString(p.get(id + ".fileFormat"));
		encoding = BaseStepMeta.parameterToString(p.get(id + ".encoding"));
		fileType = BaseStepMeta.parameterToString(p.get(id + ".fileType"));
		fileCompression = BaseStepMeta.parameterToString(p.get(id
				+ ".fileCompression"));
		rowLimit = BaseStepMeta.parameterToLong(p.get(id + ".rowLimit"));
		errorIgnored = BaseStepMeta.parameterToBoolean(p.get(id
				+ ".errorIgnored"));
		errorLineSkipped = BaseStepMeta.parameterToBoolean(p.get(id
				+ ".errorLineSkipped"));
		errorCountField = BaseStepMeta.parameterToString(p.get(id
				+ ".errorCountField"));
		errorFieldsField = BaseStepMeta.parameterToString(p.get(id
				+ ".errorFieldsField"));
		errorTextField = BaseStepMeta.parameterToString(p.get(id
				+ ".errorTextField"));
		warningFilesDestinationDirectory = BaseStepMeta.parameterToString(p
				.get(id + ".warningFilesDestinationDirectory"));
		warningFilesExtension = BaseStepMeta.parameterToString(p.get(id
				+ ".warningFilesExtension"));
		errorFilesDestinationDirectory = BaseStepMeta.parameterToString(p
				.get(id + ".errorFilesDestinationDirectory"));
		errorFilesExtension = BaseStepMeta.parameterToString(p.get(id
				+ ".errorFilesExtension"));
		lineNumberFilesDestinationDirectory = BaseStepMeta.parameterToString(p
				.get(id + ".lineNumberFilesDestinationDirectory"));
		lineNumberFilesExtension = BaseStepMeta.parameterToString(p.get(id
				+ ".lineNumberFilesExtension"));
		dateFormatLenient = BaseStepMeta.parameterToBoolean(p.get(id
				+ ".dateFormatLenient"));

		String[] fileName = p.get(id + "_pitch.fileName");
		String[] fileMask = p.get(id + "_pitch.fileMask");
		String[] fileRequired = p.get(id + "_pitch.fileRequired");
		this.fileName = (fileName != null) ? fileName : (new String[0]);
		this.fileMask = (fileMask != null) ? fileMask : (new String[0]);
		this.fileRequired = (fileRequired != null) ? fileRequired
				: (new String[0]);

		String[] filterString = p.get(id + "_sift.filterString");
		String[] filterPosition = p.get(id + "_sift.locationFilter");
		String[] filterLastLine = p.get(id + "_sift.stopFilter");

		if (filterString != null && filterString.length > 0) {
			TextFileFilter[] textFileFilter = new TextFileFilter[filterString.length];
			for (int i = 0; i < filterString.length; i++) {
				textFileFilter[i] = new TextFileFilter();
				textFileFilter[i].setFilterString(filterString[i]);
				textFileFilter[i].setFilterPosition(Const.toInt(
						filterPosition[i], 0));
				textFileFilter[i]
						.setFilterLastLine(parameterToBoolean(filterLastLine[i]));
			}
			this.filter = textFileFilter;
		}

		String[] name = p.get(id + "_fields.fieldName");
		String[] position = p.get(id + "_fields.fieldPosition");
		String[] type = p.get(id + "_fields.fieldType");
		String[] format = p.get(id + "_fields.fieldFormat");
		String[] length = p.get(id + "_fields.fieldLength");
		String[] precision = p.get(id + "_fields.fieldPrecision");
		String[] currency = p.get(id + "_fields.fieldCurrency");
		String[] decimalSymbol = p.get(id + "_fields.fieldDecimal");
		String[] groupingSymbol = p.get(id + "_fields.fieldGroup");
		String[] nullIf = p.get(id + "_fields.nullIf");
		String[] ifNull = p.get(id + "_fields.ifNull");
		String[] trimtype = p.get(id + "_fields.fieldTrimType");
		String[] repeat = p.get(id + "_fields.fieldRepeat");

		if (name != null && name.length > 0) {
			this.inputFields = new TextFileInputField[name.length];
			for (int i = 0; i < name.length; i++) {
				this.inputFields[i] = new TextFileInputField();
				this.inputFields[i].setName(name[i]);
				this.inputFields[i].setType(Const.toInt(type[i],0));
				this.inputFields[i].setFormat(format[i]);
				this.inputFields[i].setPosition(Const.toInt(position[i],0));
				this.inputFields[i].setLength(Const.toInt(length[i],-1));
				this.inputFields[i]
						.setPrecision(Const.toInt(precision[i],-1));
				this.inputFields[i].setCurrencySymbol(currency[i]);
				this.inputFields[i].setDecimalSymbol(decimalSymbol[i]);
				this.inputFields[i].setGroupSymbol(groupingSymbol[i]);
				this.inputFields[i].setNullString(nullIf[i]);
				this.inputFields[i].setIfNullValue(ifNull[i]);
				this.inputFields[i].setTrimType(Const.toInt(trimtype[i],0));
				this.inputFields[i].setRepeated(parameterToBoolean(repeat[i]));
			}
		}
	}

	private void readData(Node stepnode) throws KettleXMLException {
		try {
			acceptingFilenames = YES.equalsIgnoreCase(XMLHandler.getTagValue(
					stepnode, "accept_filenames"));
			passingThruFields = YES.equalsIgnoreCase(XMLHandler.getTagValue(
					stepnode, "passing_through_fields"));
			acceptingField = XMLHandler.getTagValue(stepnode, "accept_field");
			acceptingStepName = XMLHandler.getTagValue(stepnode,
					"accept_stepname");

			separator = XMLHandler.getTagValue(stepnode, "separator");
			enclosure = XMLHandler.getTagValue(stepnode, "enclosure");
			breakInEnclosureAllowed = YES.equalsIgnoreCase(XMLHandler
					.getTagValue(stepnode, "enclosure_breaks"));
			escapeCharacter = XMLHandler.getTagValue(stepnode, "escapechar");
			header = YES.equalsIgnoreCase(XMLHandler.getTagValue(stepnode,
					"header"));
			nrHeaderLines = Const.toInt(XMLHandler.getTagValue(stepnode,
					"nr_headerlines"), 1);
			footer = YES.equalsIgnoreCase(XMLHandler.getTagValue(stepnode,
					"footer"));
			nrFooterLines = Const.toInt(XMLHandler.getTagValue(stepnode,
					"nr_footerlines"), 1);
			lineWrapped = YES.equalsIgnoreCase(XMLHandler.getTagValue(stepnode,
					"line_wrapped"));
			nrWraps = Const.toInt(XMLHandler.getTagValue(stepnode, "nr_wraps"),
					1);
			layoutPaged = YES.equalsIgnoreCase(XMLHandler.getTagValue(stepnode,
					"layout_paged"));
			nrLinesPerPage = Const.toInt(XMLHandler.getTagValue(stepnode,
					"nr_lines_per_page"), 1);
			nrLinesDocHeader = Const.toInt(XMLHandler.getTagValue(stepnode,
					"nr_lines_doc_header"), 1);
			String addToResult = XMLHandler.getTagValue(stepnode,
					"add_to_result_filenames");
			if (Const.isEmpty(addToResult))
				isaddresult = true;
			else
				isaddresult = "Y".equalsIgnoreCase(addToResult);

			String nempty = XMLHandler.getTagValue(stepnode, "noempty");
			noEmptyLines = YES.equalsIgnoreCase(nempty) || nempty == null;
			includeFilename = YES.equalsIgnoreCase(XMLHandler.getTagValue(
					stepnode, "include"));
			filenameField = XMLHandler.getTagValue(stepnode, "include_field");
			includeRowNumber = YES.equalsIgnoreCase(XMLHandler.getTagValue(
					stepnode, "rownum"));
			rowNumberByFile = YES.equalsIgnoreCase(XMLHandler.getTagValue(
					stepnode, "rownumByFile"));
			rowNumberField = XMLHandler.getTagValue(stepnode, "rownum_field");
			fileFormat = XMLHandler.getTagValue(stepnode, "format");
			encoding = XMLHandler.getTagValue(stepnode, "encoding");

			Node filenode = XMLHandler.getSubNode(stepnode, "file");
			Node fields = XMLHandler.getSubNode(stepnode, "fields");
			Node filtersNode = XMLHandler.getSubNode(stepnode, "filters");
			int nrfiles = XMLHandler.countNodes(filenode, "name");
			int nrfields = XMLHandler.countNodes(fields, "field");
			int nrfilters = XMLHandler.countNodes(filtersNode, "filter");

			allocate(nrfiles, nrfields, nrfilters);

			for (int i = 0; i < nrfiles; i++) {
				Node filenamenode = XMLHandler.getSubNodeByNr(filenode, "name",
						i);
				Node filemasknode = XMLHandler.getSubNodeByNr(filenode,
						"filemask", i);
				Node fileRequirednode = XMLHandler.getSubNodeByNr(filenode,
						"file_required", i);
				fileName[i] = XMLHandler.getNodeValue(filenamenode);
				fileMask[i] = XMLHandler.getNodeValue(filemasknode);
				fileRequired[i] = XMLHandler.getNodeValue(fileRequirednode);
			}

			fileType = XMLHandler.getTagValue(stepnode, "file", "type");
			fileCompression = XMLHandler.getTagValue(stepnode, "file",
					"compression");
			if (fileCompression == null) {
				if (YES.equalsIgnoreCase(XMLHandler.getTagValue(stepnode,
						"file", "zipped"))) {
					fileCompression = "Zip";
				}
			}

			// Backward compatibility : just one filter
			if (XMLHandler.getTagValue(stepnode, "filter") != null) {
				filter = new TextFileFilter[1];
				filter[0] = new TextFileFilter();

				filter[0].setFilterPosition(Const.toInt(XMLHandler.getTagValue(
						stepnode, "filter_position"), -1));
				filter[0].setFilterString(XMLHandler.getTagValue(stepnode,
						"filter_string"));
				filter[0].setFilterLastLine(YES.equalsIgnoreCase(XMLHandler
						.getTagValue(stepnode, "filter_is_last_line")));
			} else {
				for (int i = 0; i < nrfilters; i++) {
					Node fnode = XMLHandler.getSubNodeByNr(filtersNode,
							"filter", i);
					filter[i] = new TextFileFilter();

					filter[i].setFilterPosition(Const.toInt(XMLHandler
							.getTagValue(fnode, "filter_position"), -1));

					String filterString = XMLHandler.getTagValue(fnode,
							"filter_string");
					if (filterString != null
							&& filterString.startsWith(STRING_BASE64_PREFIX)) {
						filter[i].setFilterString(new String(Base64
								.decodeBase64(filterString.substring(
										STRING_BASE64_PREFIX.length())
										.getBytes())));
					} else {
						filter[i].setFilterString(filterString);
					}

					filter[i].setFilterLastLine(YES.equalsIgnoreCase(XMLHandler
							.getTagValue(fnode, "filter_is_last_line")));
				}
			}

			for (int i = 0; i < nrfields; i++) {
				Node fnode = XMLHandler.getSubNodeByNr(fields, "field", i);
				TextFileInputField field = new TextFileInputField();

				field.setName(XMLHandler.getTagValue(fnode, "name"));
				field.setType(ValueMeta.getType(XMLHandler.getTagValue(fnode,
						"type")));
				field.setFormat(XMLHandler.getTagValue(fnode, "format"));
				field.setCurrencySymbol(XMLHandler.getTagValue(fnode,
						"currency"));
				field
						.setDecimalSymbol(XMLHandler.getTagValue(fnode,
								"decimal"));
				field.setGroupSymbol(XMLHandler.getTagValue(fnode, "group"));
				field.setNullString(XMLHandler.getTagValue(fnode, "nullif"));
				field.setIfNullValue(XMLHandler.getTagValue(fnode, "ifnull"));
				field.setPosition(Const.toInt(XMLHandler.getTagValue(fnode,
						"position"), -1));
				field.setLength(Const.toInt(XMLHandler.getTagValue(fnode,
						"length"), -1));
				field.setPrecision(Const.toInt(XMLHandler.getTagValue(fnode,
						"precision"), -1));
				field.setTrimType(ValueMeta.getTrimTypeByCode(XMLHandler
						.getTagValue(fnode, "trim_type")));
				field.setRepeated(YES.equalsIgnoreCase(XMLHandler.getTagValue(
						fnode, "repeat")));

				inputFields[i] = field;
			}

			// Is there a limit on the number of rows we process?
			rowLimit = Const.toLong(XMLHandler.getTagValue(stepnode, "limit"),
					0L);

			errorIgnored = YES.equalsIgnoreCase(XMLHandler.getTagValue(
					stepnode, "error_ignored"));
			errorLineSkipped = YES.equalsIgnoreCase(XMLHandler.getTagValue(
					stepnode, "error_line_skipped"));
			errorCountField = XMLHandler.getTagValue(stepnode,
					"error_count_field");
			errorFieldsField = XMLHandler.getTagValue(stepnode,
					"error_fields_field");
			errorTextField = XMLHandler.getTagValue(stepnode,
					"error_text_field");
			warningFilesDestinationDirectory = XMLHandler.getTagValue(stepnode,
					"bad_line_files_destination_directory");
			warningFilesExtension = XMLHandler.getTagValue(stepnode,
					"bad_line_files_extension");
			errorFilesDestinationDirectory = XMLHandler.getTagValue(stepnode,
					"error_line_files_destination_directory");
			errorFilesExtension = XMLHandler.getTagValue(stepnode,
					"error_line_files_extension");
			lineNumberFilesDestinationDirectory = XMLHandler.getTagValue(
					stepnode, "line_number_files_destination_directory");
			lineNumberFilesExtension = XMLHandler.getTagValue(stepnode,
					"line_number_files_extension");
			// Backward compatible

			dateFormatLenient = !NO.equalsIgnoreCase(XMLHandler.getTagValue(
					stepnode, "date_format_lenient"));
			String dateLocale = XMLHandler.getTagValue(stepnode,
					"date_format_locale");
			if (dateLocale != null) {
				dateFormatLocale = new Locale(dateLocale);
			} else {
				dateFormatLocale = Locale.getDefault();
			}
		} catch (Exception e) {
			throw new KettleXMLException("Unable to load step info from XML", e);
		}
	}

	public String getLookupStepname() {
		if (acceptingFilenames && acceptingStep != null
				&& !Const.isEmpty(acceptingStep.getName()))
			return acceptingStep.getName();
		return null;
	}

	/**
	 * @param steps
	 *            optionally search the info step in a list of steps
	 */
	public void searchInfoAndTargetSteps(List<StepMeta> steps) {
		acceptingStep = StepMeta.findStep(steps, acceptingStepName);
	}

	public String[] getInfoSteps() {
		if (acceptingFilenames && acceptingStep != null) {
			return new String[] { acceptingStep.getName() };
		}
		return super.getInfoSteps();
	}

	public void readRep(Repository rep, long id_step,
			List<DatabaseMeta> databases, Map<String, Counter> counters)
			throws KettleException {
		try {
			acceptingFilenames = rep.getStepAttributeBoolean(id_step,
					STEP_ATTRIBUTE_ACCEPT_FILENAMES);
			passingThruFields = rep.getStepAttributeBoolean(id_step,
					STEP_ATTRIBUTE_PASSING_THROUGH_FIELDS);
			acceptingField = rep.getStepAttributeString(id_step,
					STEP_ATTRIBUTE_ACCEPT_FIELD);
			acceptingStepName = rep.getStepAttributeString(id_step,
					STEP_ATTRIBUTE_ACCEPT_STEPNAME);

			separator = rep.getStepAttributeString(id_step,
					STEP_ATTRIBUTE_SEPARATOR);
			enclosure = rep.getStepAttributeString(id_step,
					STEP_ATTRIBUTE_ENCLOSURE);
			breakInEnclosureAllowed = rep.getStepAttributeBoolean(id_step,
					STEP_ATTRIBUTE_ENCLOSURE_BREAKS);
			escapeCharacter = rep.getStepAttributeString(id_step,
					STEP_ATTRIBUTE_ESCAPECHAR);
			header = rep
					.getStepAttributeBoolean(id_step, STEP_ATTRIBUTE_HEADER);
			nrHeaderLines = (int) rep.getStepAttributeInteger(id_step,
					STEP_ATTRIBUTE_NR_HEADERLINES);
			footer = rep
					.getStepAttributeBoolean(id_step, STEP_ATTRIBUTE_FOOTER);
			nrFooterLines = (int) rep.getStepAttributeInteger(id_step,
					STEP_ATTRIBUTE_NR_FOOTERLINES);
			lineWrapped = rep.getStepAttributeBoolean(id_step,
					STEP_ATTRIBUTE_LINE_WRAPPED);
			nrWraps = (int) rep.getStepAttributeInteger(id_step,
					STEP_ATTRIBUTE_NR_WRAPS);
			layoutPaged = rep.getStepAttributeBoolean(id_step,
					STEP_ATTRIBUTE_LAYOUT_PAGED);
			nrLinesPerPage = (int) rep.getStepAttributeInteger(id_step,
					STEP_ATTRIBUTE_NR_LINES_PER_PAGE);
			nrLinesDocHeader = (int) rep.getStepAttributeInteger(id_step,
					STEP_ATTRIBUTE_NR_LINES_DOC_HEADER);
			noEmptyLines = rep.getStepAttributeBoolean(id_step,
					STEP_ATTRIBUTE_NOEMPTY);

			includeFilename = rep.getStepAttributeBoolean(id_step,
					STEP_ATTRIBUTE_INCLUDE);
			filenameField = rep.getStepAttributeString(id_step,
					STEP_ATTRIBUTE_INCLUDE_FIELD);
			includeRowNumber = rep.getStepAttributeBoolean(id_step,
					STEP_ATTRIBUTE_ROWNUM);
			rowNumberByFile = rep.getStepAttributeBoolean(id_step,
					STEP_ATTRIBUTE_ROWNUMBYFILE);
			rowNumberField = rep.getStepAttributeString(id_step,
					STEP_ATTRIBUTE_ROWNUM_FIELD);

			fileFormat = rep.getStepAttributeString(id_step,
					STEP_ATTRIBUTE_FORMAT);
			encoding = rep.getStepAttributeString(id_step,
					STEP_ATTRIBUTE_ENCODING);
			String addToResult = rep.getStepAttributeString(id_step,
					STEP_ATTRIBUTE_ADD_TO_RESULT_FILENAMES);
			if (Const.isEmpty(addToResult))
				isaddresult = true;
			else
				isaddresult = rep.getStepAttributeBoolean(id_step,
						STEP_ATTRIBUTE_ADD_TO_RESULT_FILENAMES);

			rowLimit = (int) rep.getStepAttributeInteger(id_step,
					STEP_ATTRIBUTE_LIMIT);

			int nrfiles = rep.countNrStepAttributes(id_step,
					STEP_ATTRIBUTE_FILE_NAME);
			int nrfields = rep.countNrStepAttributes(id_step,
					STEP_ATTRIBUTE_FIELD_NAME);
			int nrfilters = rep.countNrStepAttributes(id_step,
					STEP_ATTRIBUTE_FILTER_STRING);

			allocate(nrfiles, nrfields, nrfilters);

			for (int i = 0; i < nrfiles; i++) {
				fileName[i] = rep.getStepAttributeString(id_step, i,
						STEP_ATTRIBUTE_FILE_NAME);
				fileMask[i] = rep.getStepAttributeString(id_step, i,
						STEP_ATTRIBUTE_FILE_MASK);
				fileRequired[i] = rep.getStepAttributeString(id_step, i,
						STEP_ATTRIBUTE_FILE_REQUIRED);
				if (!YES.equalsIgnoreCase(fileRequired[i]))
					fileRequired[i] = NO;
			}
			fileType = rep.getStepAttributeString(id_step,
					STEP_ATTRIBUTE_FILE_TYPE);
			fileCompression = rep.getStepAttributeString(id_step,
					STEP_ATTRIBUTE_COMPRESSION);
			if (fileCompression == null) {
				if (rep.getStepAttributeBoolean(id_step,
						STEP_ATTRIBUTE_FILE_ZIPPED))
					fileCompression = "Zip";
			}

			for (int i = 0; i < nrfilters; i++) {
				filter[i] = new TextFileFilter();
				filter[i].setFilterPosition((int) rep.getStepAttributeInteger(
						id_step, i, STEP_ATTRIBUTE_FILTER_POSITION));
				filter[i].setFilterString(rep.getStepAttributeString(id_step,
						i, STEP_ATTRIBUTE_FILTER_STRING));
				filter[i].setFilterLastLine(rep.getStepAttributeBoolean(
						id_step, i, STEP_ATTRIBUTE_FILTER_IS_LAST_LINE));
			}

			for (int i = 0; i < nrfields; i++) {
				TextFileInputField field = new TextFileInputField();

				field.setName(rep.getStepAttributeString(id_step, i,
						STEP_ATTRIBUTE_FIELD_NAME));
				field.setType(ValueMeta.getType(rep.getStepAttributeString(
						id_step, i, STEP_ATTRIBUTE_FIELD_TYPE)));
				field.setFormat(rep.getStepAttributeString(id_step, i,
						STEP_ATTRIBUTE_FIELD_FORMAT));
				field.setCurrencySymbol(rep.getStepAttributeString(id_step, i,
						STEP_ATTRIBUTE_FIELD_CURRENCY));
				field.setDecimalSymbol(rep.getStepAttributeString(id_step, i,
						STEP_ATTRIBUTE_FIELD_DECIMAL));
				field.setGroupSymbol(rep.getStepAttributeString(id_step, i,
						STEP_ATTRIBUTE_FIELD_GROUP));
				field.setNullString(rep.getStepAttributeString(id_step, i,
						STEP_ATTRIBUTE_FIELD_NULLIF));
				field.setIfNullValue(rep.getStepAttributeString(id_step, i,
						STEP_ATTRIBUTE_FIELD_IFNULL));
				field.setPosition((int) rep.getStepAttributeInteger(id_step, i,
						STEP_ATTRIBUTE_FIELD_POSITION));
				field.setLength((int) rep.getStepAttributeInteger(id_step, i,
						STEP_ATTRIBUTE_FIELD_LENGTH));
				field.setPrecision((int) rep.getStepAttributeInteger(id_step,
						i, STEP_ATTRIBUTE_FIELD_PRECISION));
				field.setTrimType(ValueMeta.getTrimTypeByCode(rep
						.getStepAttributeString(id_step, i,
								STEP_ATTRIBUTE_FIELD_TRIM_TYPE)));
				field.setRepeated(rep.getStepAttributeBoolean(id_step, i,
						STEP_ATTRIBUTE_FIELD_REPEAT));

				inputFields[i] = field;
			}

			errorIgnored = rep.getStepAttributeBoolean(id_step,
					STEP_ATTRIBUTE_ERROR_IGNORED);
			errorLineSkipped = rep.getStepAttributeBoolean(id_step,
					STEP_ATTRIBUTE_ERROR_LINE_SKIPPED);
			errorCountField = rep.getStepAttributeString(id_step,
					STEP_ATTRIBUTE_ERROR_COUNT_FIELD);
			errorFieldsField = rep.getStepAttributeString(id_step,
					STEP_ATTRIBUTE_ERROR_FIELDS_FIELD);
			errorTextField = rep.getStepAttributeString(id_step,
					STEP_ATTRIBUTE_ERROR_TEXT_FIELD);

			warningFilesDestinationDirectory = rep.getStepAttributeString(
					id_step, STEP_ATTRIBUTE_BAD_LINE_FILES_DEST_DIR);
			warningFilesExtension = rep.getStepAttributeString(id_step,
					STEP_ATTRIBUTE_BAD_LINE_FILES_EXT);
			errorFilesDestinationDirectory = rep.getStepAttributeString(
					id_step, STEP_ATTRIBUTE_ERROR_LINE_FILES_DEST_DIR);
			errorFilesExtension = rep.getStepAttributeString(id_step,
					STEP_ATTRIBUTE_ERROR_LINE_FILES_EXT);
			lineNumberFilesDestinationDirectory = rep.getStepAttributeString(
					id_step, STEP_ATTRIBUTE_LINE_NUMBER_FILES_DEST_DIR);
			lineNumberFilesExtension = rep.getStepAttributeString(id_step,
					STEP_ATTRIBUTE_LINE_NUMBER_FILES_EXT);

			dateFormatLenient = rep.getStepAttributeBoolean(id_step, 0,
					STEP_ATTRIBUTE_DATE_FORMAT_LENIENT, true);

			String dateLocale = rep.getStepAttributeString(id_step, 0,
					STEP_ATTRIBUTE_DATE_FORMAT_LOCALE);
			if (dateLocale != null) {
				dateFormatLocale = new Locale(dateLocale);
			} else {
				dateFormatLocale = Locale.getDefault();
			}
		} catch (Exception e) {
			throw new KettleException(
					"Unexpected error reading step information from the repository",
					e);
		}
	}

	public void saveRep(Repository rep, long id_transformation, long id_step)
			throws KettleException {
		try {
			rep.saveStepAttribute(id_transformation, id_step,
					"accept_filenames", acceptingFilenames);
			rep.saveStepAttribute(id_transformation, id_step,
					"passing_through_fields", passingThruFields);
			rep.saveStepAttribute(id_transformation, id_step, "accept_field",
					acceptingField);
			rep.saveStepAttribute(id_transformation, id_step,
					"accept_stepname", (acceptingStep != null ? acceptingStep
							.getName() : ""));

			rep.saveStepAttribute(id_transformation, id_step, "separator",
					separator);
			rep.saveStepAttribute(id_transformation, id_step, "enclosure",
					enclosure);
			rep.saveStepAttribute(id_transformation, id_step,
					"enclosure_breaks", breakInEnclosureAllowed);
			rep.saveStepAttribute(id_transformation, id_step, "escapechar",
					escapeCharacter);
			rep.saveStepAttribute(id_transformation, id_step, "header", header);
			rep.saveStepAttribute(id_transformation, id_step, "nr_headerlines",
					nrHeaderLines);
			rep.saveStepAttribute(id_transformation, id_step, "footer", footer);
			rep.saveStepAttribute(id_transformation, id_step, "nr_footerlines",
					nrFooterLines);
			rep.saveStepAttribute(id_transformation, id_step, "line_wrapped",
					lineWrapped);
			rep.saveStepAttribute(id_transformation, id_step, "nr_wraps",
					nrWraps);
			rep.saveStepAttribute(id_transformation, id_step, "layout_paged",
					layoutPaged);
			rep.saveStepAttribute(id_transformation, id_step,
					"nr_lines_per_page", nrLinesPerPage);
			rep.saveStepAttribute(id_transformation, id_step,
					"nr_lines_doc_header", nrLinesDocHeader);

			rep.saveStepAttribute(id_transformation, id_step, "noempty",
					noEmptyLines);

			rep.saveStepAttribute(id_transformation, id_step, "include",
					includeFilename);
			rep.saveStepAttribute(id_transformation, id_step, "include_field",
					filenameField);
			rep.saveStepAttribute(id_transformation, id_step, "rownum",
					includeRowNumber);
			rep.saveStepAttribute(id_transformation, id_step, "rownumByFile",
					rowNumberByFile);
			rep.saveStepAttribute(id_transformation, id_step, "rownum_field",
					rowNumberField);

			rep.saveStepAttribute(id_transformation, id_step, "format",
					fileFormat);
			rep.saveStepAttribute(id_transformation, id_step, "encoding",
					encoding);
			rep.saveStepAttribute(id_transformation, id_step,
					"add_to_result_filenames", isaddresult);

			rep
					.saveStepAttribute(id_transformation, id_step, "limit",
							rowLimit);

			for (int i = 0; i < fileName.length; i++) {
				rep.saveStepAttribute(id_transformation, id_step, i,
						"file_name", fileName[i]);
				rep.saveStepAttribute(id_transformation, id_step, i,
						"file_mask", fileMask[i]);
				rep.saveStepAttribute(id_transformation, id_step, i,
						"file_required", fileRequired[i]);
			}
			rep.saveStepAttribute(id_transformation, id_step, "file_type",
					fileType);
			rep.saveStepAttribute(id_transformation, id_step, "compression",
					fileCompression);

			for (int i = 0; i < filter.length; i++) {
				rep.saveStepAttribute(id_transformation, id_step, i,
						"filter_position", filter[i].getFilterPosition());
				rep.saveStepAttribute(id_transformation, id_step, i,
						"filter_string", filter[i].getFilterString());
				rep.saveStepAttribute(id_transformation, id_step, i,
						"filter_is_last_line", filter[i].isFilterLastLine());
			}

			for (int i = 0; i < inputFields.length; i++) {
				TextFileInputField field = inputFields[i];

				rep.saveStepAttribute(id_transformation, id_step, i,
						"field_name", field.getName());
				rep.saveStepAttribute(id_transformation, id_step, i,
						"field_type", field.getTypeDesc());
				rep.saveStepAttribute(id_transformation, id_step, i,
						"field_format", field.getFormat());
				rep.saveStepAttribute(id_transformation, id_step, i,
						"field_currency", field.getCurrencySymbol());
				rep.saveStepAttribute(id_transformation, id_step, i,
						"field_decimal", field.getDecimalSymbol());
				rep.saveStepAttribute(id_transformation, id_step, i,
						"field_group", field.getGroupSymbol());
				rep.saveStepAttribute(id_transformation, id_step, i,
						"field_nullif", field.getNullString());
				rep.saveStepAttribute(id_transformation, id_step, i,
						"field_ifnull", field.getIfNullValue());
				rep.saveStepAttribute(id_transformation, id_step, i,
						"field_position", field.getPosition());
				rep.saveStepAttribute(id_transformation, id_step, i,
						"field_length", field.getLength());
				rep.saveStepAttribute(id_transformation, id_step, i,
						"field_precision", field.getPrecision());
				rep.saveStepAttribute(id_transformation, id_step, i,
						"field_trim_type", field.getTrimTypeCode());
				rep.saveStepAttribute(id_transformation, id_step, i,
						"field_repeat", field.isRepeated());
			}

			rep.saveStepAttribute(id_transformation, id_step, "error_ignored",
					errorIgnored);
			rep.saveStepAttribute(id_transformation, id_step,
					"error_line_skipped", errorLineSkipped);
			rep.saveStepAttribute(id_transformation, id_step,
					"error_count_field", errorCountField);
			rep.saveStepAttribute(id_transformation, id_step,
					"error_fields_field", errorFieldsField);
			rep.saveStepAttribute(id_transformation, id_step,
					"error_text_field", errorTextField);

			rep
					.saveStepAttribute(id_transformation, id_step,
							"bad_line_files_dest_dir",
							warningFilesDestinationDirectory);
			rep.saveStepAttribute(id_transformation, id_step,
					"bad_line_files_ext", warningFilesExtension);
			rep
					.saveStepAttribute(id_transformation, id_step,
							"error_line_files_dest_dir",
							errorFilesDestinationDirectory);
			rep.saveStepAttribute(id_transformation, id_step,
					"error_line_files_ext", errorFilesExtension);
			rep.saveStepAttribute(id_transformation, id_step,
					"line_number_files_dest_dir",
					lineNumberFilesDestinationDirectory);
			rep.saveStepAttribute(id_transformation, id_step,
					"line_number_files_ext", lineNumberFilesExtension);

			rep.saveStepAttribute(id_transformation, id_step,
					"date_format_lenient", dateFormatLenient);
			rep.saveStepAttribute(id_transformation, id_step,
					"date_format_locale", dateFormatLocale.toString());
		} catch (Exception e) {
			throw new KettleException(
					"Unable to save step information to the repository for id_step="
							+ id_step, e);
		}
	}

	public String[] getFilePaths(VariableSpace space) {
		return FileInputList.createFilePathList(space, fileName, fileMask,
				fileRequired);
	}

	public FileInputList getTextFileList(VariableSpace space) {
		return FileInputList.createFileList(space, fileName, fileMask,
				fileRequired);
	}

	public void check(List<CheckResultInterface> remarks, TransMeta transMeta,
			StepMeta stepinfo, RowMetaInterface prev, String input[],
			String output[], RowMetaInterface info) {
		CheckResult cr;

		// See if we get input...
		if (input.length > 0) {
			if (!isAcceptingFilenames()) {
				cr = new CheckResult(
						CheckResultInterface.TYPE_RESULT_ERROR,
						Messages
								.getString("TextFileInputMeta.CheckResult.NoInputError"),
						stepinfo);
				remarks.add(cr);
			} else {
				cr = new CheckResult(
						CheckResultInterface.TYPE_RESULT_OK,
						Messages
								.getString("TextFileInputMeta.CheckResult.AcceptFilenamesOk"),
						stepinfo);
				remarks.add(cr);
			}
		} else {
			cr = new CheckResult(CheckResultInterface.TYPE_RESULT_OK, Messages
					.getString("TextFileInputMeta.CheckResult.NoInputOk"),
					stepinfo);
			remarks.add(cr);
		}

		FileInputList textFileList = getTextFileList(transMeta);
		if (textFileList.nrOfFiles() == 0) {
			if (!isAcceptingFilenames()) {
				cr = new CheckResult(
						CheckResultInterface.TYPE_RESULT_ERROR,
						Messages
								.getString("TextFileInputMeta.CheckResult.ExpectedFilesError"),
						stepinfo);
				remarks.add(cr);
			}
		} else {
			cr = new CheckResult(CheckResultInterface.TYPE_RESULT_OK, Messages
					.getString("TextFileInputMeta.CheckResult.ExpectedFilesOk",
							"" + textFileList.nrOfFiles()), stepinfo);
			remarks.add(cr);
		}
	}

	public StepInterface getStep(StepMeta stepMeta,
			StepDataInterface stepDataInterface, int cnr, TransMeta transMeta,
			Trans trans) {
		return new TextFileInput(stepMeta, stepDataInterface, cnr, transMeta,
				trans);
	}

	public StepDataInterface getStepData() {
		return new TextFileInputData();
	}

	/**
	 * @return Returns the escapeCharacter.
	 */
	public String getEscapeCharacter() {
		return escapeCharacter;
	}

	/**
	 * @param escapeCharacter
	 *            The escapeCharacter to set.
	 */
	public void setEscapeCharacter(String escapeCharacter) {
		this.escapeCharacter = escapeCharacter;
	}

	public String getErrorCountField() {
		return errorCountField;
	}

	public void setErrorCountField(String errorCountField) {
		this.errorCountField = errorCountField;
	}

	public String getErrorFieldsField() {
		return errorFieldsField;
	}

	public void setErrorFieldsField(String errorFieldsField) {
		this.errorFieldsField = errorFieldsField;
	}

	public boolean isErrorIgnored() {
		return errorIgnored;
	}

	public void setErrorIgnored(boolean errorIgnored) {
		this.errorIgnored = errorIgnored;
	}

	public String getErrorTextField() {
		return errorTextField;
	}

	public void setErrorTextField(String errorTextField) {
		this.errorTextField = errorTextField;
	}

	/**
	 * @return Returns the lineWrapped.
	 */
	public boolean isLineWrapped() {
		return lineWrapped;
	}

	/**
	 * @param lineWrapped
	 *            The lineWrapped to set.
	 */
	public void setLineWrapped(boolean lineWrapped) {
		this.lineWrapped = lineWrapped;
	}

	/**
	 * @return Returns the nrFooterLines.
	 */
	public int getNrFooterLines() {
		return nrFooterLines;
	}

	/**
	 * @param nrFooterLines
	 *            The nrFooterLines to set.
	 */
	public void setNrFooterLines(int nrFooterLines) {
		this.nrFooterLines = nrFooterLines;
	}

	/**
	 * @return Returns the nrHeaderLines.
	 */
	public int getNrHeaderLines() {
		return nrHeaderLines;
	}

	/**
	 * @param nrHeaderLines
	 *            The nrHeaderLines to set.
	 */
	public void setNrHeaderLines(int nrHeaderLines) {
		this.nrHeaderLines = nrHeaderLines;
	}

	/**
	 * @return Returns the nrWraps.
	 */
	public int getNrWraps() {
		return nrWraps;
	}

	/**
	 * @param nrWraps
	 *            The nrWraps to set.
	 */
	public void setNrWraps(int nrWraps) {
		this.nrWraps = nrWraps;
	}

	/**
	 * @return Returns the layoutPaged.
	 */
	public boolean isLayoutPaged() {
		return layoutPaged;
	}

	/**
	 * @param layoutPaged
	 *            The layoutPaged to set.
	 */
	public void setLayoutPaged(boolean layoutPaged) {
		this.layoutPaged = layoutPaged;
	}

	/**
	 * @return Returns the nrLinesPerPage.
	 */
	public int getNrLinesPerPage() {
		return nrLinesPerPage;
	}

	/**
	 * @param nrLinesPerPage
	 *            The nrLinesPerPage to set.
	 */
	public void setNrLinesPerPage(int nrLinesPerPage) {
		this.nrLinesPerPage = nrLinesPerPage;
	}

	/**
	 * @return Returns the nrLinesDocHeader.
	 */
	public int getNrLinesDocHeader() {
		return nrLinesDocHeader;
	}

	/**
	 * @param nrLinesDocHeader
	 *            The nrLinesDocHeader to set.
	 */
	public void setNrLinesDocHeader(int nrLinesDocHeader) {
		this.nrLinesDocHeader = nrLinesDocHeader;
	}

	public String getWarningFilesDestinationDirectory() {
		return warningFilesDestinationDirectory;
	}

	public void setWarningFilesDestinationDirectory(
			String warningFilesDestinationDirectory) {
		this.warningFilesDestinationDirectory = warningFilesDestinationDirectory;
	}

	public String getWarningFilesExtension() {
		return warningFilesExtension;
	}

	public void setWarningFilesExtension(String warningFilesExtension) {
		this.warningFilesExtension = warningFilesExtension;
	}

	public String getLineNumberFilesDestinationDirectory() {
		return lineNumberFilesDestinationDirectory;
	}

	public void setLineNumberFilesDestinationDirectory(
			String lineNumberFilesDestinationDirectory) {
		this.lineNumberFilesDestinationDirectory = lineNumberFilesDestinationDirectory;
	}

	public String getLineNumberFilesExtension() {
		return lineNumberFilesExtension;
	}

	public void setLineNumberFilesExtension(String lineNumberFilesExtension) {
		this.lineNumberFilesExtension = lineNumberFilesExtension;
	}

	public String getErrorFilesDestinationDirectory() {
		return errorFilesDestinationDirectory;
	}

	public void setErrorFilesDestinationDirectory(
			String errorFilesDestinationDirectory) {
		this.errorFilesDestinationDirectory = errorFilesDestinationDirectory;
	}

	public String getErrorLineFilesExtension() {
		return errorFilesExtension;
	}

	public void setErrorLineFilesExtension(String errorLineFilesExtension) {
		this.errorFilesExtension = errorLineFilesExtension;
	}

	public boolean isDateFormatLenient() {
		return dateFormatLenient;
	}

	public void setDateFormatLenient(boolean dateFormatLenient) {
		this.dateFormatLenient = dateFormatLenient;
	}

	/**
	 * @param isaddresult
	 *            The isaddresult to set.
	 */
	public void setAddResultFile(boolean isaddresult) {
		this.isaddresult = isaddresult;
	}

	/**
	 * @return Returns isaddresult.
	 */
	public boolean isAddResultFile() {
		return isaddresult;
	}

	public boolean isErrorLineSkipped() {
		return errorLineSkipped;
	}

	public void setErrorLineSkipped(boolean errorLineSkipped) {
		this.errorLineSkipped = errorLineSkipped;
	}

	/**
	 * @return Returns the dateFormatLocale.
	 */
	public Locale getDateFormatLocale() {
		return dateFormatLocale;
	}

	/**
	 * @param dateFormatLocale
	 *            The dateFormatLocale to set.
	 */
	public void setDateFormatLocale(Locale dateFormatLocale) {
		this.dateFormatLocale = dateFormatLocale;
	}

	public boolean isAcceptingFilenames() {
		return acceptingFilenames;
	}

	public void setAcceptingFilenames(boolean getFileFromJob) {
		this.acceptingFilenames = getFileFromJob;
	}

	public boolean isPassingThruFields() {
		return passingThruFields;
	}

	public void setPassingThruFields(boolean passingThruFields) {
		this.passingThruFields = passingThruFields;
	}

	/**
	 * @return Returns the fileNameField.
	 */
	public String getAcceptingField() {
		return acceptingField;
	}

	/**
	 * @param fileNameField
	 *            The fileNameField to set.
	 */
	public void setAcceptingField(String fileNameField) {
		this.acceptingField = fileNameField;
	}

	/**
	 * @return Returns the acceptingStep.
	 */
	public String getAcceptingStepName() {
		return acceptingStepName;
	}

	/**
	 * @param acceptingStep
	 *            The acceptingStep to set.
	 */
	public void setAcceptingStepName(String acceptingStep) {
		this.acceptingStepName = acceptingStep;
	}

	/**
	 * @return Returns the acceptingStep.
	 */
	public StepMeta getAcceptingStep() {
		return acceptingStep;
	}

	/**
	 * @param acceptingStep
	 *            The acceptingStep to set.
	 */
	public void setAcceptingStep(StepMeta acceptingStep) {
		this.acceptingStep = acceptingStep;
	}

	public int getFileFormatTypeNr() {
		// calculate the file format type in advance so we can use a switch
		if (getFileFormat().equalsIgnoreCase("DOS")) {
			return FILE_FORMAT_DOS;
		} else if (getFileFormat().equalsIgnoreCase("unix")) {
			return TextFileInputMeta.FILE_FORMAT_UNIX;
		} else {
			return TextFileInputMeta.FILE_FORMAT_MIXED;
		}
	}

	public int getFileTypeNr() {
		// calculate the file type in advance CSV or Fixed?
		if (getFileType().equalsIgnoreCase("CSV")) {
			return TextFileInputMeta.FILE_TYPE_CSV;
		} else {
			return TextFileInputMeta.FILE_TYPE_FIXED;
		}
	}

	@Override
	public List<ResourceReference> getResourceDependencies(TransMeta transMeta,
			StepMeta stepInfo) {
		List<ResourceReference> references = new ArrayList<ResourceReference>(5);
		ResourceReference reference = new ResourceReference(stepInfo);
		references.add(reference);

		String[] textFiles = getFilePaths(transMeta);
		if (textFiles != null) {
			for (int i = 0; i < textFiles.length; i++) {
				reference.getEntries().add(
						new ResourceEntry(textFiles[i], ResourceType.FILE));
			}
		}
		return references;
	}
}
