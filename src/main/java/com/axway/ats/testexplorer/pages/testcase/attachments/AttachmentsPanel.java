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
import java.util.Arrays;
import java.util.List;

import javax.servlet.ServletContext;

import org.apache.log4j.Logger;
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

import com.axway.ats.core.filesystem.LocalFileSystemOperations;
import com.axway.ats.core.utils.StringUtils;
import com.axway.ats.log.autodb.exceptions.DatabaseAccessException;
import com.axway.ats.testexplorer.model.TestExplorerSession;
import com.axway.ats.testexplorer.model.db.PageNavigation;

public class AttachmentsPanel extends Panel {

    private static final long serialVersionUID = 1L;

    private static Logger     LOG               = Logger.getLogger( AttachmentsPanel.class );

    private Form<Object>      form;
    private MarkupContainer   buttonPanel;
    private MarkupContainer   noButtonPanel;
    private Label             startingDisplayingMessage;
    private Label             endingDisplayingMessage;
    private Label             downloadLabel;
    private DownloadLink      downloadFile;
    private AjaxLink<?>       alink;
    private TextArea<String>  fileContent;

    private List<String>      buttons;
    private String            fileInfo          = "";

    private String            noButtonPanelInfo = "No attached files";

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public AttachmentsPanel( String id,
                             String testcaseId ) {
        super( id );

        form = new Form<Object>( "form" );
        buttonPanel = new WebMarkupContainer( "buttonPanel" );
        noButtonPanel = new WebMarkupContainer( "noButtonPanel" );
        downloadFile = new DownloadLink( "download", new File( " " ), "" );
        downloadLabel = new Label( "downloadLabel", "" );
        //        downloadFile.setVisible( false );
        downloadFile.add( downloadLabel );
        fileContent = new TextArea<String>( "fileContent", new Model<String>( "" ) );
        buttons = getAllAttachedFiles( testcaseId );
        startingDisplayingMessage = new Label( "startingDisplayingMessage", "" );
        endingDisplayingMessage = new Label( "endingDisplayingMessage", "" );

        form.add( fileContent );
        form.add( downloadFile );
        form.add( buttonPanel );
        form.add( noButtonPanel );
        form.add( startingDisplayingMessage );
        form.add( endingDisplayingMessage );
        add( form );

        buttonPanel.setVisible(!( buttons == null));
        fileContent.setVisible(!( buttons == null));
        noButtonPanel.setVisible( buttons == null );
        
        noButtonPanel.add( new Label("description", noButtonPanelInfo ) );

        final ListView lv = new ListView( "buttons", buttons ) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(
                                         final ListItem item ) {

                final String name = getFileSimpleName( buttons.get( item.getIndex() ) );
                final Label buttonLabel = new Label( "name", name );
                alink = new AjaxLink( "alink", item.getModel() ) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(
                                         AjaxRequestTarget target ) {

                        String viewedFile = buttons.get( item.getIndex() );
                        fileContent.setModelObject( getFileContent( viewedFile ) );
                        startingDisplayingMessage.setDefaultModelObject( fileInfo );
                        downloadFile.setModelObject( new File( buttons.get( item.getIndex() ) ) );
                        downloadFile.setVisible( true );
                        downloadLabel.setDefaultModelObject( name );
                        endingDisplayingMessage.setDefaultModelObject( " with size "
                                                                       + getFileSize( viewedFile ) );

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
                        target.appendJavaScript( reverseButtonsState );
                        target.appendJavaScript( pressClickedButton );

                        target.add( form );
                    }
                };
                alink.add( buttonLabel );
                item.add( alink );
            }
        };
        buttonPanel.add( lv );
    }

    private String getFileSimpleName(
                                      String filePath ) {

        if( !StringUtils.isNullOrEmpty( filePath ) ) {
            String normalizedFilePath = filePath.replace( "\\", "/" );
            int lastDashIndex = normalizedFilePath.lastIndexOf( '/' );
            return normalizedFilePath.substring( lastDashIndex + 1, normalizedFilePath.length() );
        }
        return null;
    }

    private String getFileContent(
                                   String filePath ) {

        StringBuilder fileContent = new StringBuilder();
        LocalFileSystemOperations fo = new LocalFileSystemOperations();
        String[] fileContentArray = fo.getLastLinesFromFile( filePath, 1024 );

        for( String line : fileContentArray ) {
            fileContent.append( line );
            fileContent.append( "\n" );
        }

        if( fileContentArray.length >= 1024 ) {
            fileInfo = "Displaying content of last 1024 lines from file ";
        } else {
            fileInfo = "Displaying content of all " + fileContentArray.length + " lines from file ";
        }

        return fileContent.toString();
    }

    private String getFileSize(
                                String filePath ) {

        LocalFileSystemOperations fo = new LocalFileSystemOperations();
        double size = fo.getFileSize( filePath ) / 1024d; // calculating the file size in bytes

        StringBuilder fixedSize = new StringBuilder( String.format( "%.2f", size ) ); // round the value to the second digit after the comma
        int idx = fixedSize.length() - 4;

        // grouping number in 3 digits
        while( idx > 0 ) {
            fixedSize.insert( idx, " " );
            idx = idx - 4;
        }
        fixedSize.append( " KB" );

        return fixedSize.toString();
    }

    private List<String> getAllAttachedFiles(
                                              String testcaseId ) {

        ServletContext context = ( ( WebApplication ) getApplication() ).getServletContext();
        if( context.getAttribute( ContextListener.getAttachedFilesDir() ) == null ) {
            String errorMsg = "No attached files can be displayed. \nPossible reason could be Tomcat 'CATALINA_HOME' or 'CATALINA_BASE' is not set.";
            LOG.error( errorMsg );
            noButtonPanelInfo = errorMsg;

            return null;
        }

        String attachedfilesDir = context.getAttribute( "ats-attached-files" ).toString();

        try {
            PageNavigation navigation = ( ( TestExplorerSession ) Session.get() ).getDbReadConnection()
                                                                       .getNavigationForTestcase( testcaseId, getTESession().getTimeOffset() );
            String database = ( ( TestExplorerSession ) Session.get() ).getDbName();
            String runId = navigation.getRunId();
            String suiteId = navigation.getSuiteId();

            LocalFileSystemOperations fo = new LocalFileSystemOperations();
            // check if there is a directory for the current testcase and files attached to it
            String baseDir = attachedfilesDir + "\\" + database;
            if( fo.doesFileExist( baseDir + "\\" + runId + "\\" + suiteId + "\\" + testcaseId ) ) {
                return Arrays.asList( fo.findFiles( baseDir + "\\" + runId + "\\" + suiteId + "\\"
                                                    + testcaseId, ".*", true, false, false ) );
            }
        } catch( DatabaseAccessException e ) {
            LOG.error( "There was problem getting testcase parameters, files attached to the current testcase will not be shown!" );
        }
        return null;
    }
    
    public TestExplorerSession getTESession() {

        return ( TestExplorerSession ) Session.get();
    }
}
