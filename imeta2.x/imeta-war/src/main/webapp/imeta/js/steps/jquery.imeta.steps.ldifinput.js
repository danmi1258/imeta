jQuery.imeta.steps.ldifinput = {
	btn : {
		getfields : function(e, v) {
			var r = [ {
				id : 'fieldId',
				type : 'number',
				text : ''
			}, {
				id : 'fieldName',
				type : 'input',
				text : 'fieldName'
			}, {
				id : 'fieldType',
				type : 'select',
				text : 'type'
			}, {
				id : 'fieldAttribute',
				type : 'input',
				text : ''
			}, {
				id : 'fieldFormat',
				type : 'input',
				text : ''
			}, {
				id : 'fieldLength',
				type : 'input',
				text : 'length'
			}, {
				id : 'fieldPrecision',
				type : 'input',
				text : 'precision'
			}, {
				id : 'fieldCurrency',
				type : 'input',
				text : ''
			}, {
				id : 'fieldDecimal',
				type : 'input',
				text : 'decimal'
			}, {
				id : 'fieldGroup',
				type : 'input',
				text : 'group'
			}, {
				id : 'fieldTrimType',
				type : 'select',
				text : ''
			}, {
				id : 'fieldRepeat',
				type : 'select',
				text : ''
			} ];
			jQuery.imeta.parameter.getfields(e, v, r);
		}
	},
	selectedfiles : {
		btn : {
			fieldAdd : function(c) {
				var rootId = c.getAttribute("rootId");
				var r = [ {
					id : rootId + '.fileId',
					type : 'number',
					text : ''
				}, {
					id : rootId + '.fileName',
					type : 'input',
					text : ''
				}, {
					id : rootId + '.fileMask',
					type : 'input',
					text : ''
				} ];
				jQuery.imetabar.createRowByHeader(r, rootId);
			}
		}
	},
	fields : {
		btn : {
			fieldAdd : function(c) {
				var rootId = c.getAttribute("rootId");
				var r = [ {
					id : rootId + '.fieldId',
					type : 'number',
					text : ''
				}, {
					id : rootId + '.fieldName',
					type : 'input',
					text : ''
				}, {
					id : rootId + '.fieldType',
					type : 'select',
					text : ''
				}, {
					id : rootId + '.fieldAttribute',
					type : 'input',
					text : ''
				}, {
					id : rootId + '.fieldFormat',
					type : 'input',
					text : ''
				}, {
					id : rootId + '.fieldLength',
					type : 'input',
					text : ''
				}, {
					id : rootId + '.fieldPrecision',
					type : 'input',
					text : ''
				}, {
					id : rootId + '.fieldCurrency',
					type : 'input',
					text : ''
				}, {
					id : rootId + '.fieldDecimal',
					type : 'input',
					text : ''
				}, {
					id : rootId + '.fieldGroup',
					type : 'input',
					text : ''
				}, {
					id : rootId + '.fieldTrimType',
					type : 'select',
					text : ''
				}, {
					id : rootId + '.fieldRepeat',
					type : 'select',
					text : ''
				} ];
				jQuery.imetabar.createRowByHeader(r, rootId);
			}
		}
	},
	listeners : {
		filefieldListeners : function(e, v) {
			var elId = e.target.id;
			var id = elId.split(".")[0];
			if (e.target.checked) {
				$("[id=" + id + ".dynamicFilenameField]").attr("disabled",
						false);
				$("[id=" + id + ".fileOrdirectory]").attr("disabled", true);
				// $("[id="+id+".btn.fileOrdirectoryAdd]").attr("disabled",true);
				// $("[id="+id+".btn.fileOrdirectorySelecter]").attr("disabled",true);
				$("[id=" + id + ".regularExpression]").attr("disabled", true);
				// $("[id="+id+".deletebtn]").attr("disabled",true);
				// $("[id="+id+".editbtn]").attr("disabled",true);
				// $("[id="+id+".showFilesName]").attr("disabled",true);
				$("[id=" + id + ".includeFilename]").attr("disabled", true);
				$("[id=" + id + ".filenameField]").attr("disabled", true);
				$("[id=" + id + ".rowLimit]").attr("disabled", true);
				$("[id=" + id + ".includeFilename]").attr("checked", false);

				// 点击后，表格屏蔽选中文件的按钮和包含文件的字段名
				$("[id=" + id + "_selectedFiles.btn.add]").attr("disabled",
						true);
				$("[id=" + id + "_selectedFiles.btn.delete]").attr("disabled",
						true);
				$("[id=" + id + "_selectedFiles.btn.add.root]").addClass(
						"x-item-disabled");
				$("[id=" + id + "_selectedFiles.btn.delete.root]").addClass(
						"x-item-disabled");
				$("#" + id + "_selectedFiles_gRoot input").attr("disabled",
						true);
			} else {
				$("[id=" + id + ".dynamicFilenameField]")
						.attr("disabled", true);
				$("[id=" + id + ".fileOrdirectory]").attr("disabled", false);
				// $("[id="+id+".btn.fileOrdirectoryAdd]").attr("disabled",false);
				// $("[id="+id+".btn.fileOrdirectorySelecter]").attr("disabled",false);
				$("[id=" + id + ".regularExpression]").attr("disabled", false);
				// $("[id="+id+".deletebtn]").attr("disabled",false);
				// $("[id="+id+".editbtn]").attr("disabled",false);
				// $("[id="+id+".showFilesName]").attr("disabled",false);
				$("[id=" + id + ".includeFilename]").attr("disabled", false);
				$("[id=" + id + ".rowLimit]").attr("disabled", false);

				// 点击不选中后，表格显示选中文件的按钮和包含文件的字段名
				$("[id=" + id + "_selectedFiles.btn.add]").attr("disabled",
						false);
				$("[id=" + id + "_selectedFiles.btn.delete]").attr("disabled",
						false);
				$("[id=" + id + "_selectedFiles.btn.add.root]").removeClass(
						"x-item-disabled");
				$("[id=" + id + "_selectedFiles.btn.delete.root]").removeClass(
						"x-item-disabled");
				$("#" + id + "_selectedFiles_gRoot input").attr("disabled",
						false);
			}
		},
		includeFilenameListeners : function(e, v) {
			var elId = e.target.id;
			var id = elId.split(".")[0];
			if (e.target.checked) {
				$("[id=" + id + ".filenameField]").attr("disabled", false);
			} else {
				$("[id=" + id + ".filenameField]").attr("disabled", true);
			}
		},
		includeRowNumberListeners : function(e, v) {
			var elId = e.target.id;
			var id = elId.split(".")[0];
			if (e.target.checked) {
				$("[id=" + id + ".rowNumberField]").attr("disabled", false);
			} else {
				$("[id=" + id + ".rowNumberField]").attr("disabled", true);
			}
		}
	}
};