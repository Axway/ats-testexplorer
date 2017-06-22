/*
 * Copyright 2017 Axway Software
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.axway.ats.testexplorer.pages.testcase.statistics;

import java.io.Serializable;

import org.apache.wicket.model.Model;

public class StatisticsTableCell implements Serializable {

	private static final long serialVersionUID = 1L;

	public boolean isCheckbox;
	public boolean isInputText;
	public boolean isCheckboxLabel;
	public Model<Boolean> checkboxModel = new Model<Boolean>(Boolean.FALSE);
	private Model<String> machineLabelModel;
	public String labelText;
	public String cssClass;
	public String title;

	public StatisticsTableCell(boolean isCheckbox) {

		this.isCheckbox = isCheckbox;
	}

	public StatisticsTableCell(String labelText, boolean isCheckboxLabel) {

		this.labelText = labelText;
		this.isCheckboxLabel = isCheckboxLabel;
	}

	public StatisticsTableCell(String labelText, boolean isCheckboxLabel, String cssClass) {

		this(labelText, isCheckboxLabel);
		this.cssClass = cssClass;
	}

	public StatisticsTableCell(Model<Boolean> checkboxModel) {

		this.isCheckbox = true;
		this.checkboxModel = checkboxModel;
	}

	public StatisticsTableCell(boolean isInputText, Model<String> machineLabelModel) {

		this.isInputText = isInputText;
		this.machineLabelModel = machineLabelModel;
	}

	public Model<String> getMachineLabelModel() {

		return machineLabelModel;
	}

	public String getMachineLabel() {

		if (machineLabelModel != null) {
			return machineLabelModel.getObject();
		}
		return "";
	}
}
