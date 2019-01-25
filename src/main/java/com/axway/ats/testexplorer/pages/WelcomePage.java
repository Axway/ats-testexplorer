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
package com.axway.ats.testexplorer.pages;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.axway.ats.common.systemproperties.AtsSystemProperties;
import com.axway.ats.core.utils.IoUtils;
import com.axway.ats.log.autodb.exceptions.DatabaseAccessException;
import com.axway.ats.testexplorer.pages.runs.RunsPage;

public class WelcomePage extends BasePage {

    private static final long      serialVersionUID     = 1L;

    private TextField<String>      newDatabaseTextField = new TextField<String>("new_database",
                                                                                new Model<String>(""));

    private DropDownChoice<Object> databaseChoices      = new DropDownChoice<Object>("databases",
                                                                                     new PropertyModel<Object>(this,
                                                                                                               ""),
                                                                                     loadDatabasesList());

    public WelcomePage() {

        add(new SelectDatabaseForm("databases_form"));
    }

    @Override
    public String getPageName() {

        return "Home";
    }

    class SelectDatabaseForm extends Form<String> {
        private static final long serialVersionUID = 1L;

        public SelectDatabaseForm( String id ) {

            super(id);

            // text field for specifying a new database
            add(newDatabaseTextField);

            // the form submit button
            AjaxButton exploreDatabaseButton = new AjaxButton("explore_database") {
                private static final long serialVersionUID = 1L;

                @Override
                protected void onSubmit(
                                         AjaxRequestTarget target,
                                         Form<?> form ) {

                    // FIXME poor validation, use the Wicket's built-in approach
                    String actualDb = null;

                    String newDbSelected = newDatabaseTextField.getModelObject();
                    if (newDbSelected != null && newDbSelected.trim().length() > 0) {
                        actualDb = newDbSelected;
                    } else {
                        Object knownDbSelected = databaseChoices.getModelObject();
                        if (knownDbSelected != null && knownDbSelected instanceof String
                            && ((String) knownDbSelected).length() > 0) {

                            actualDb = (String) knownDbSelected;
                        }
                    }

                    if (actualDb != null) {
                        actualDb = actualDb.trim();
                        if (initializeDatabaseConnection(actualDb)) {
                            updateDatabasesList(actualDb);

                            PageParameters parameters = new PageParameters();
                            // pass database name
                            parameters.add("dbname", actualDb);
                            setResponsePage(RunsPage.class, parameters);
                        }
                    } else {
                        error("Database name field is empty");
                    }
                }
            };

            add(exploreDatabaseButton);
            // search button is the button to trigger when user hit the enter
            // key
            this.setDefaultButton(exploreDatabaseButton);

            // list with already known databases
            add(new ListView<String>("dbLinksList", loadDatabasesList()) {
                private static final long serialVersionUID = 1L;

                @Override
                protected void populateItem(
                                             ListItem<String> item ) {

                    final String dbName = item.getModelObject();

                    item.add(new Link<RunsPage>("dbLink") {
                        private static final long serialVersionUID = 1L;

                        @Override
                        public void onClick() {

                            if (initializeDatabaseConnection(dbName)) {
                                updateDatabasesList(dbName);

                                PageParameters parameters = new PageParameters();
                                // pass database name
                                parameters.add("dbname", dbName);
                                setResponsePage(RunsPage.class, parameters);
                            }
                        }

                    }.add(new Label("dbLinkText", humanReadableDbName(dbName)))
                     .add(AttributeModifier.replace("title", dbName)));
                }
            });
        }
    }

    private List<String> loadDatabasesList() {

        List<String> databases = new ArrayList<String>();
        BufferedReader buff = null;
        try {
            File dbListFile = findDatabaseListFile();
            if (dbListFile != null) {
                FileInputStream fis = new FileInputStream(dbListFile);

                DataInputStream dins = new DataInputStream(fis);
                buff = new BufferedReader(new InputStreamReader(dins));
                String line;
                while ( (line = buff.readLine()) != null) {
                    line = line.trim();
                    if (line.length() > 0) {
                        databases.add(line);
                    }
                }
                IoUtils.closeStream(dins);
            } else {
                LOG.error("Could not find databases.txt file");
            }
        } catch (Exception e) {
            LOG.error("Error loading databases list from databases.txt file", e);
        } finally {
            try {
                buff.close();
            } catch (IOException e) {
                LOG.error("Unable to close stream used for loading of known dababases.");
            }
        }

        return databases;
    }

    private boolean initializeDatabaseConnection(
                                                  String dbName ) {

        try {
            getTESession().initializeDbReadConnection(dbName);
            return true;
        } catch (DatabaseAccessException e) {
            String errorMessage = "Unable to connect to database '" + dbName + "' at " + getTESession().getDbHost()
                                  + ":"
                                  + getTESession().getDbPort() + " as user '" + getTESession().getDbUser() + "'";
            error(errorMessage);
            LOG.error(errorMessage, e);
            return false;
        }
    }

    private void updateDatabasesList(
                                      String newDatabase ) {

        List<? extends Object> databases = databaseChoices.getChoices();

        boolean isDatabaseAlreadyPresent = false;
        StringBuilder sb = new StringBuilder();
        for (Object db : databases) {
            if (newDatabase.endsWith(db.toString())) {
                isDatabaseAlreadyPresent = true;
            }
            sb.append(db.toString()).append(AtsSystemProperties.SYSTEM_LINE_SEPARATOR);
        }

        // add the new DB
        if (isDatabaseAlreadyPresent) {
            return;
        } else {
            sb.append(newDatabase).append(AtsSystemProperties.SYSTEM_LINE_SEPARATOR);
        }

        // update the file
        try {
            File dbListFile = findDatabaseListFile();
            if (dbListFile != null) {

                OutputStream fos = new FileOutputStream(dbListFile);
                fos.write(sb.toString().getBytes());
                IoUtils.closeStream(fos);
            } else {
                LOG.error("Could not find databases.txt file");
            }
        } catch (Exception e) {
            LOG.error("Error updating databases list in databases.txt file", e);
        }
    }

    private static File findDatabaseListFile() {

        URL url = WelcomePage.class.getResource("/databases.txt");
        try {
            return new File(url.toURI());
        } catch (URISyntaxException e) {
            LOG.error("Unable to construct URL for resource with filepath '/databases.txt'", e);
            return null;
        }
    }

    /**
     * The Welcome page shows a list with database links. In cases when the
     * database name is too long these links overlap. So we insert some white
     * spaces when needed in order to wrap the names into more lines.
     *
     * @param dbName
     *            database name
     * @return readable database name
     */
    private String humanReadableDbName(
                                        String dbName ) {

        dbName = dbName.replace('_', ' ');
        try {

            // Make lines by:
            // 1. Insert white space before a capital letter preceded by lower
            // cased letter ("Ats Db")
            // 2. Insert white space before a capital letter preceded by another
            // capital letter and followed by lower cased letter ("ATS
            // Functional")
            // 3. Insert white space if the line is getting too long

            StringBuilder newDbName = new StringBuilder();
            int lineLength = 0;
            int lastLowCaseChar = 0;
            char prevCh = ' ';
            int i = 0;
            for (i = 0; i < dbName.length(); i++) {
                char ch = dbName.charAt(i);
                // Upper after Lower case letter
                if ( (Character.isUpperCase(ch) && Character.isLowerCase(prevCh))
                     || (lineLength > 10) /* the line is too long */ ) {

                    newDbName.append(' ').append(ch);
                    lineLength = 1;

                    /*
                     * Upper,Upper,Lower case letters - add space after the last
                     * Upper followed by Lower - e.g. "ATS Functional"
                     */
                } else {
                    if (Character.isLowerCase(ch) && Character.isUpperCase(prevCh)
                        && newDbName.length() > 1
                        // check if 2 symbols back, the symbol is uppercase, so if we should add whitespace
                        && Character.isUpperCase(newDbName.charAt(newDbName.length() - 2))) {
                        if (lastLowCaseChar != 0)
                            newDbName.insert(lastLowCaseChar + 1, ' ');

                        newDbName.append(ch);
                        lineLength = newDbName.length() - lastLowCaseChar + 1;
                    } else if (newDbName.length() > 3 && Character.isDigit(ch)
                    // check if the last two symbols are letters, so we have to add whitespace
                               && Character.isLetter(newDbName.charAt(newDbName.length() - 2))
                               && Character.isLetter(prevCh)) {
                        newDbName.append(" " + ch);
                        lineLength = 1;
                    } else {
                        newDbName.append(ch);
                        lineLength++;
                        if ( (ch == ' ') || (ch == '-')) {
                            lineLength = 0;
                        }
                    }
                }

                // save the index of the last lowercase char
                if (Character.isLowerCase(ch))
                    lastLowCaseChar = i;

                prevCh = ch;
            }
            return newDbName.toString().trim();
        } catch (Exception e) {

            LOG.error("Unable to format db name: \"" + dbName + "\"");
        }

        return dbName;
    }

}
