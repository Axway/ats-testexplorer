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
package com.axway.ats.testexplorer.pages.testcase.attachments;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.imageio.ImageIO;
import javax.servlet.ServletContext;

import org.apache.log4j.Logger;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.link.DownloadLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.axway.ats.core.filesystem.LocalFileSystemOperations;
import com.axway.ats.core.utils.IoUtils;
import com.axway.ats.core.utils.StringUtils;
import com.axway.ats.log.autodb.exceptions.DatabaseAccessException;
import com.axway.ats.testexplorer.TestExplorerUtils;
import com.axway.ats.testexplorer.model.TestExplorerSession;
import com.axway.ats.testexplorer.model.db.PageNavigation;

public class AttachmentsPanel extends Panel {

    private static final long  serialVersionUID  = 1L;

    private static Logger      LOG               = Logger.getLogger(AttachmentsPanel.class);

    private Form<Object>       form;
    private MarkupContainer    buttonPanel;
    private MarkupContainer    noButtonPanel;

    private DownloadLink       downloadFile;
    private AjaxLink<?>        alink;
    private TextArea<String>   fileContentContainer;
    private WebMarkupContainer imageContainer;
    private Label              fileContentInfo;

    private List<String>       buttons;

    private String             noButtonPanelInfo = "No attached files";

    @SuppressWarnings( { "unchecked", "rawtypes" })
    public AttachmentsPanel( String id, final String testcaseId, final PageParameters parameters ) {
        super(id);

        form = new Form<Object>("form");
        buttonPanel = new WebMarkupContainer("buttonPanel");
        noButtonPanel = new WebMarkupContainer("noButtonPanel");
        fileContentContainer = new TextArea<String>("textFile", new Model<String>(""));
        imageContainer = new WebMarkupContainer("imageFile");
        fileContentInfo = new Label("fileContentInfo", new Model<String>(""));
        buttons = getAllAttachedFiles(testcaseId);

        form.add(fileContentContainer);
        form.add(imageContainer);
        form.add(fileContentInfo);
        form.add(buttonPanel);

        add(noButtonPanel);
        add(form);

        buttonPanel.setVisible(! (buttons == null));
        fileContentContainer.setVisible(false);
        imageContainer.setVisible(false);
        fileContentInfo.setVisible(false);
        noButtonPanel.setVisible(buttons == null);

        // if noButtonPanel is visible, do not show form and vice versa
        form.setVisible(!noButtonPanel.isVisible());

        noButtonPanel.add(new Label("description", noButtonPanelInfo));

        final ListView lv = new ListView("buttons", buttons) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem( final ListItem item ) {

                if (item.getIndex() % 2 != 0) {
                    item.add(AttributeModifier.replace("class", "oddRow"));
                }

                final String viewedFile = buttons.get(item.getIndex());

                final String name = getFileSimpleName(buttons.get(item.getIndex()));
                final Label buttonLabel = new Label("name", name);
                buttonLabel.add(AttributeModifier.append("title", name));

                Label fileSize = new Label("fileSize", getFileSize(viewedFile));

                downloadFile = new DownloadLink("download", new File(" "), "");
                downloadFile.setModelObject(new File(viewedFile));
                downloadFile.setVisible(true);

                alink = new AjaxLink("alink", item.getModel()) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick( AjaxRequestTarget target ) {

                        fileContentInfo.setVisible(true);
                        String fileContent = new String();
                        if (!isImage(viewedFile)) {
                            fileContentContainer.setVisible(true);
                            imageContainer.setVisible(false);
                            fileContent = getFileContent(viewedFile, name);
                            fileContentContainer.setModelObject(fileContent);
                        } else {

                            PageNavigation navigation = null;
                            try {
                                navigation = ((TestExplorerSession) Session.get()).getDbReadConnection()
                                                                                  .getNavigationForTestcase(testcaseId,
                                                                                                            getTESession().getTimeOffset());
                            } catch (DatabaseAccessException e) {
                                LOG.error("Can't get runId, suiteId and dbname for testcase with ID = "
                                          + testcaseId, e);
                            }

                            String runId = navigation.getRunId();
                            String suiteId = navigation.getSuiteId();
                            String dbname = TestExplorerUtils.extractPageParameter(parameters, "dbname");

                            fileContentInfo.setDefaultModelObject("Previewing '" + name + "' image");

                            final String url = "AttachmentsServlet?&runId=" + runId + "&suiteId=" + suiteId
                                               + "&testcaseId=" + testcaseId + "&dbname=" + dbname
                                               + "&fileName=" + name;
                            imageContainer.add(new AttributeModifier("src", new Model<String>(url)));
                            imageContainer.setVisible(true);
                            fileContentContainer.setVisible(false);
                        }

                        // first setting all buttons with the same state
                        String reverseButtonsState = "var cusid_ele = document.getElementsByClassName('attachedButtons'); "
                                                     + "for (var i = 0; i < cusid_ele.length; ++i) { "
                                                     + "var item = cusid_ele[i];  "
                                                     + "item.style.color= \"#000000\";" + "}";
                        // setting CSS style to the pressed button and its label
                        String pressClickedButton = "var span = document.evaluate(\"//a[@class='button attachedButtons']/span[text()='"
                                                    + name + "']\", "
                                                    + "document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue;"
                                                    + "span.style.backgroundPosition=\"left bottom\";"
                                                    + "span.style.padding=\"6px 0 4px 18px\";"
                                                    + "var button = document.evaluate(\"//a[@class='button attachedButtons']/span[text()='"
                                                    + name + "']/..\", "
                                                    + "document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue;"
                                                    + "button.style.backgroundPosition=\"right bottom\";"
                                                    + "button.style.color=\"#000000\";"
                                                    + "button.style.outline=\"medium none\";";

                        // I could not figure out how it works with wicket, so i did it with JS
                        target.appendJavaScript(reverseButtonsState);
                        target.appendJavaScript(pressClickedButton);

                        target.add(form);
                    }
                };

                alink.add(buttonLabel);
                item.add(alink);
                item.add(downloadFile);
                item.add(fileSize);
            }
        };
        buttonPanel.add(lv);
    }

    private boolean isImage( String filePath ) {

        try (FileInputStream fileStream = new FileInputStream(filePath)) {
            if (ImageIO.read(fileStream) == null) {
                return false;
            }
        } catch (IOException e) {
            LOG.error("File '" + filePath + "' cannot be read.");
        }
        return true;
    }

    private String getFileSimpleName( String filePath ) {

        if (!StringUtils.isNullOrEmpty(filePath)) {
            String normalizedFilePath = filePath.replace("\\", "/");
            int lastDashIndex = normalizedFilePath.lastIndexOf('/');
            return normalizedFilePath.substring(lastDashIndex + 1, normalizedFilePath.length());
        }
        return null;
    }

    private String getFileContent( String filePath, String name ) {

        int maxLines = 1024;
        StringBuilder fileContent = new StringBuilder();
        LocalFileSystemOperations fo = new LocalFileSystemOperations();
        String[] fileContentArray = fo.getLastLinesFromFile(filePath, maxLines);

        for (String line : fileContentArray) {
            fileContent.append(line);
            fileContent.append("\n");
        }

        if (fileContent.length() < maxLines) {
            fileContentInfo.setDefaultModelObject("Showing the content of '" + name + "' in "
                                                  + fileContentArray.length + " lines");
        } else {
            fileContentInfo.setDefaultModelObject("Showing the last " + maxLines + " lines of '" + name
                                                  + "'");
        }

        return fileContent.toString();
    }

    private String getFileSize( String filePath ) {

        LocalFileSystemOperations fo = new LocalFileSystemOperations();
        double size = fo.getFileSize(filePath) / 1024d; // calculating the file size in bytes

        StringBuilder fixedSize = new StringBuilder(String.format("%.2f", size)); // round the value to the second digit after the comma
        int idx = fixedSize.length() - 4;

        // grouping number in 3 digits
        while (idx > 0) {
            fixedSize.insert(idx, " ");
            idx = idx - 4;
        }
        fixedSize.append(" KB");

        return fixedSize.toString();
    }

    private List<String> getAllAttachedFiles( String testcaseId ) {

        ServletContext context = ((WebApplication) getApplication()).getServletContext();
        String attachedFilesDir = (String) context.getAttribute(ContextListener.getAttachedFilesDirAttribute());
        if ( attachedFilesDir == null) {
            String errorMsg = "No attached files can be displayed. \nPossible reason could be Tomcat 'CATALINA_HOME' or 'CATALINA_BASE' is not set.";
            LOG.error(errorMsg);
            noButtonPanelInfo = errorMsg;
            return null;
        }

        if (StringUtils.isNullOrEmpty(testcaseId)) {
            String errorMsg = "Testcase ID is not provided.";
            LOG.error(errorMsg);
            noButtonPanelInfo = errorMsg;
            return null;
        }

        try {
            PageNavigation navigation = ((TestExplorerSession) Session.get()).getDbReadConnection()
                                                                             .getNavigationForTestcase(testcaseId,
                                                                                                       getTESession().getTimeOffset());
            String database = ((TestExplorerSession) Session.get()).getDbName();
            String runId = navigation.getRunId();
            String suiteId = navigation.getSuiteId();

            LocalFileSystemOperations fo = new LocalFileSystemOperations();
            // check if there is a directory for the current testcase and files attached to it
            String baseDir = attachedFilesDir + "\\" + database;
            String fullFilePath = baseDir + "\\" + runId + "\\" + suiteId + "\\" + testcaseId;
            fullFilePath = IoUtils.normalizeFilePath(fullFilePath);
            if (fo.doesFileExist(fullFilePath)) {
                String[] attachedFiles = fo.findFiles(fullFilePath, ".*", true, false, false);
                if (attachedFiles.length > 0) {
                    return Arrays.asList(attachedFiles);
                }
                return null;
            }
        } catch (DatabaseAccessException e) {
            LOG.error("There was problem getting testcase parameters, files attached to the current testcase will not be shown!", e);
        }
        return null;
    }

    public TestExplorerSession getTESession() {

        return (TestExplorerSession) Session.get();
    }
}
