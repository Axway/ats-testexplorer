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
package com.axway.ats.testexplorer.pages.machines;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.axway.ats.log.autodb.entities.Machine;
import com.axway.ats.log.autodb.exceptions.DatabaseAccessException;
import com.axway.ats.testexplorer.pages.LightweightBasePage;

public class MachinesPage extends LightweightBasePage {

    private static final long            serialVersionUID        = 1L;

    private List<Machine>                machines;

    private Map<Integer, IModel<String>> machineModels           = new HashMap<Integer, IModel<String>>();

    private WebMarkupContainer           machineInfoDialog;
    private TextArea<String>             machineInfoText;
    private Label                        machineInfoDialogTitle;
    private Form<Object>                 machineInfoDialogForm;
    private Machine                      machineForEdit;

    private static final int             MAX_MACHINE_INFO_LENGTH = 10000;

    public MachinesPage( PageParameters parameters ) {

        super(parameters);

        machines = getMachines();

        Label noMachinesLabel = new Label("noMachinesLabel", "There are no machines in database '"
                                                             + getTESession().getDbName() + "'");
        noMachinesLabel.setOutputMarkupId(true);
        Form<Object> machinesForm = getMachinesForm(noMachinesLabel);
        add(machinesForm);

        machineInfoDialog = new WebMarkupContainer("machineInfoDialog");
        machineInfoDialog.setVisible(false);
        machineInfoDialogTitle = new Label("machineInfoDialogTitle", "");
        machineInfoDialog.add(machineInfoDialogTitle);
        machineInfoText = new TextArea<String>("machineInformationText", new Model<String>(""));
        machineInfoDialog.add(machineInfoText);
        machineInfoDialog.add(getMachineInfoDialogSaveButton());
        machineInfoDialog.add(getMachineInfoDialogCancelButton());

        machineInfoDialogForm = new Form<Object>("machineInfoDialogForm");
        machineInfoDialogForm.setOutputMarkupId(true);
        machineInfoDialogForm.add(machineInfoDialog);

        add(machineInfoDialogForm);
    }

    private Component getMachineInfoDialogSaveButton() {

        return new AjaxButton("machineInfoDialogSave") {

            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(
                                     AjaxRequestTarget target,
                                     Form<?> form ) {

                String information = machineInfoText.getModel().getObject();
                if (information == null) {
                    information = "";
                }
                if (information.length() > MAX_MACHINE_INFO_LENGTH) {
                    information = information.substring(0, MAX_MACHINE_INFO_LENGTH) + "...";
                }
                updateMachineInformation(machineForEdit, information);

                machineInfoDialog.setVisible(false);
                target.add(machineInfoDialogForm);
            }
        };
    }

    private Component getMachineInfoDialogCancelButton() {

        return new AjaxButton("machineInfoDialogCancel") {

            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(
                                     AjaxRequestTarget target,
                                     Form<?> form ) {

                machineInfoDialog.setVisible(false);
                target.add(machineInfoDialogForm);
            }
        };
    }

    private List<Machine> getMachines() {

        try {
            if (getTESession().getDbReadConnection() != null) {

                return getTESession().getDbReadConnection().getMachines();
            }
        } catch (DatabaseAccessException e) {
            LOG.error("Can't get machines from database '" + getTESession().getDbName() + "'", e);
        }
        return new ArrayList<Machine>();
    }

    private String getMachineInformation(
                                          Machine machine ) {

        try {
            if (getTESession().getDbReadConnection() != null) {

                return getTESession().getDbReadConnection().getMachineInformation(machine.machineId);
            }
        } catch (DatabaseAccessException e) {
            LOG.error("Can't get machine information for machine '" + machine.name + "' in database '"
                      + getTESession().getDbName() + "'", e);
        }
        return "";
    }

    private void updateMachineInformation(
                                           Machine machine,
                                           String information ) {

        try {
            if (getTESession().getDbWriteConnection() != null) {

                getTESession().getDbWriteConnection().updateMachineInformation(machine.machineId,
                                                                               information);
            }
        } catch (DatabaseAccessException e) {
            LOG.error("Can't update machine information for machine '" + machine.name + "' in database '"
                      + getTESession().getDbName() + "'", e);
        }
    }

    private Form<Object> getMachinesForm(
                                          final Label noMachinesLabel ) {

        final Form<Object> machinesForm = new Form<Object>("machinesForm");
        machinesForm.setOutputMarkupId(true);

        machineModels = new HashMap<Integer, IModel<String>>();

        ListView<Machine> machinesTable = new ListView<Machine>("machine", machines) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(
                                         final ListItem<Machine> item ) {

                if (item.getIndex() % 2 != 0) {
                    item.add(AttributeModifier.replace("class", "oddRow"));
                }
                IModel<String> aliasModel = new Model<String>(item.getModelObject().alias);
                machineModels.put(item.getModelObject().machineId, aliasModel);
                item.add(new TextField<String>("machineAlias", aliasModel));

                item.add(new Label("machineName", item.getModelObject().name).setEscapeModelStrings(false));

                final Machine machine = item.getModelObject();
                item.add(new AjaxButton("machineInfo") {

                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void onSubmit(
                                             AjaxRequestTarget target,
                                             Form<?> form ) {

                        if (machine.alias == null || machine.alias.trim().length() == 0) {
                            machineInfoDialogTitle.setDefaultModelObject(machine.name);
                        } else {
                            machineInfoDialogTitle.setDefaultModelObject(machine.alias + " (" + machine.name
                                                                         + ")");
                        }

                        machineInfoDialog.setVisible(true);
                        machineForEdit = machine;
                        machineInfoText.setModelObject(getMachineInformation(machine));

                        target.add(machineInfoDialogForm);
                    }
                });
            }
        };
        machinesForm.add(machinesTable);

        AjaxButton saveMachineAliasesButton = new AjaxButton("saveMachineAliasesButton") {

            private static final long serialVersionUID = 1L;

            @Override
            public void onSubmit(
                                  AjaxRequestTarget target,
                                  Form<?> form ) {

                if (!form.isSubmitted()) {
                    return;
                }

                for (Machine machine : machines) {

                    String newMachineAlias = machineModels.get(machine.machineId).getObject();
                    if (newMachineAlias != null) {
                        newMachineAlias = newMachineAlias.trim();
                    }
                    if ( (newMachineAlias == null && machine.alias != null)
                         || (newMachineAlias != null && !newMachineAlias.equals(machine.alias))) {

                        machine.alias = newMachineAlias;
                        try {
                            getTESession().getDbWriteConnection().updateMachineAlias(machine);
                        } catch (DatabaseAccessException e) {
                            LOG.error("Can't update alias of machine '" + machine.name + "'", e);
                            target.appendJavaScript("alert('There was an error while updating the machine aliases!');");
                            return;
                        }
                    }
                }
                target.appendJavaScript("alert('The machine aliases were successfully updated.');");
            }
        };

        boolean hasMachines = machines.size() > 0;

        machinesTable.setVisible(hasMachines);
        saveMachineAliasesButton.setVisible(hasMachines);
        noMachinesLabel.setVisible(!hasMachines);

        machinesForm.add(saveMachineAliasesButton);
        machinesForm.add(noMachinesLabel);

        return machinesForm;
    }

    @Override
    public String getPageHeaderText() {

        return "Machines management page";
    }

    @Override
    public String getPageName() {

        return "Machines";
    }

}
