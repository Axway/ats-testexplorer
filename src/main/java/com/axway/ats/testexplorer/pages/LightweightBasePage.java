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

import org.apache.log4j.Logger;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.IAjaxIndicatorAware;
import org.apache.wicket.markup.html.TransparentWebMarkupContainer;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.request.http.WebResponse;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.StringValue;

import com.axway.ats.testexplorer.TestExplorerUtils;
import com.axway.ats.testexplorer.model.TestExplorerSession;

public abstract class LightweightBasePage extends WebPage implements IAjaxIndicatorAware {

    private static final long serialVersionUID = 1L;

    protected static Logger   LOG;

    private String            currentRunId;

    public LightweightBasePage( PageParameters parameters ) {

        super( parameters );

        currentRunId = TestExplorerUtils.extractPageParameter( parameters, "runId" );

        LOG = Logger.getLogger( this.getClass() );

        addHeader();

        // add child page
        TransparentWebMarkupContainer pageWrapper = new TransparentWebMarkupContainer( "page_wrapper" );
        add( pageWrapper );
    }

    public TestExplorerSession getTESession() {

        return ( TestExplorerSession ) Session.get();
    }

    private void addHeader() {

        add( new Label( "page_title", "Axway ATS Test Explorer - " + getPageName() ) );

        // check DB connection and sets the current DB Name
        getTESession().getDbReadConnection();

        if( getPageHeaderText() == null ) {
            add( new Label( "headerText", "" ) );
        } else {
            add( new Label( "headerText", " - " + getPageHeaderText() ) );
        }

        WebMarkupContainer topRightContent = new WebMarkupContainer( "topRightContent" );
        add( topRightContent );

        String dbName = getTESession().getDbName();
        if( dbName == null || "".equals( dbName ) ) {
            topRightContent.add( new Label( "dbName", "" ).setVisible( false ) );
        } else {
            String dbNameAndVersion = dbName;
            String dbVersion = getTESession().getDbVersion();
            if( dbVersion != null ) {
                dbNameAndVersion = dbNameAndVersion + ", v" + dbVersion;
            }
            topRightContent.add( new Label( "dbName",
                                            "<div class=\"dbName\"><span style=\"color:#C8D5DF;\">Exploring database:</span>&nbsp; "
                                                    + dbNameAndVersion + "</div>" ).setEscapeModelStrings( false ) );
        }

        FeedbackPanel feedbackPanel = new FeedbackPanel( "feedback" );
        feedbackPanel.setOutputMarkupId( true );
        add( feedbackPanel );
    }

    public abstract String getPageName();

    public String extractParameter(
                                    PageParameters parameters,
                                    String paramName ) {

        String paramValue = null;
        Object paramObject = parameters.get( paramName );
        if( paramObject == null ) {
            return null;
        } else if( paramObject instanceof StringValue ) {
            paramValue = paramObject.toString();
        } else {
            paramValue = ( ( StringValue[] ) paramObject )[0].toString();
        }
        return paramValue;
    }

    public String getCurrentRunId() {

        return currentRunId;
    }

    abstract public String getPageHeaderText();

    @Override
    public String getAjaxIndicatorMarkupId() {

        return "ajaxLoader";
    }

    @Override
    protected void configureResponse(
                                      WebResponse response ) {

        response.disableCaching();
        super.configureResponse( response );
    }

}
