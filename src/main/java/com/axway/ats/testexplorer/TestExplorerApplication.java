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
package com.axway.ats.testexplorer;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.wicket.Component;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.AjaxRequestTarget.IJavaScriptResponse;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.request.Request;
import org.apache.wicket.request.Response;
import org.apache.wicket.util.visit.IVisit;
import org.apache.wicket.util.visit.IVisitor;

import com.axway.ats.testexplorer.model.TestExplorerSession;
import com.axway.ats.testexplorer.pages.WelcomePage;
import com.axway.ats.testexplorer.pages.errors.InternalErrorPage;
import com.axway.ats.testexplorer.pages.errors.PageExpiredErrorPage;
import com.axway.ats.testexplorer.pages.machines.MachinesPage;
import com.axway.ats.testexplorer.pages.model.TableColumn;
import com.axway.ats.testexplorer.pages.reports.compare.ComparePage;
import com.axway.ats.testexplorer.pages.reports.compare.CompareTestcaseSystemStatisticsPage;
import com.axway.ats.testexplorer.pages.reports.testcase.SelectTestcaseReportPage;
import com.axway.ats.testexplorer.pages.runCopy.RunCopyPage;
import com.axway.ats.testexplorer.pages.runs.RunMessagePage;
import com.axway.ats.testexplorer.pages.runs.RunsPage;
import com.axway.ats.testexplorer.pages.runsByTypeDashboard.home.RunsByTypeDashboardHomePage;
import com.axway.ats.testexplorer.pages.runsByTypeDashboard.run.RunsByTypeDashboardRunPage;
import com.axway.ats.testexplorer.pages.runsByTypeDashboard.suite.RunsByTypeDashboardSuitePage;
import com.axway.ats.testexplorer.pages.scenarios.ScenariosPage;
import com.axway.ats.testexplorer.pages.suites.SuiteMessagePage;
import com.axway.ats.testexplorer.pages.suites.SuitesPage;
import com.axway.ats.testexplorer.pages.testcase.TestcasePage;
import com.axway.ats.testexplorer.pages.testcases.TestcasesPage;
import com.axway.ats.testexplorer.pages.testcasesByGroups.TestcasesByGroupsPage;
import com.axway.ats.testexplorer.pages.testcasesCopy.TestcasesCopyPage;
import com.axway.ats.testexplorer.plugins.report.TestExplorerPluginsRepo;

/**
 * Application object for the Test Explorer web application
 */
public class TestExplorerApplication extends WebApplication {

    private static final Logger            LOG                  = Logger.getLogger( TestExplorerApplication.class );

    private Properties                     configProperties;

    //DB name and DB table column definitions
    private Map<String, List<TableColumn>> columnDefinitionsMap = new HashMap<String, List<TableColumn>>();

    @Override
    protected void init() {

        Locale.setDefault( Locale.US );

        getApplicationSettings().setPageExpiredErrorPage( PageExpiredErrorPage.class );
        getApplicationSettings().setInternalErrorPage( InternalErrorPage.class );
        // show internal error page rather than default developer page
        //TODO: use this line in PRODUCTION mode, by default in development mode is used IExceptionSettings.SHOW_EXCEPTION_PAGE
        //        getExceptionSettings().setUnexpectedExceptionDisplay( IExceptionSettings.SHOW_INTERNAL_ERROR_PAGE );

        mountPage( "/runs", RunsPage.class );
        mountPage( "/suites", SuitesPage.class );
        mountPage( "/scenarios", ScenariosPage.class );
        mountPage( "/testcases", TestcasesPage.class );
        mountPage( "/testcase", TestcasePage.class );

        mountPage( "/compare", ComparePage.class );
        mountPage( "/compareStatistics", CompareTestcaseSystemStatisticsPage.class );

        mountPage( "/runMessages", RunMessagePage.class );
        mountPage( "/suiteMessages", SuiteMessagePage.class );

        mountPage( "/machines", MachinesPage.class );
        mountPage( "/runCopy", RunCopyPage.class );
        mountPage( "/testcasesCopy", TestcasesCopyPage.class );

        mountPage( "/reportSelect", SelectTestcaseReportPage.class );

        mountPage( "/pageExpired", PageExpiredErrorPage.class );
        mountPage( "/error", InternalErrorPage.class );

        mountPage( "/dashboardhome", RunsByTypeDashboardHomePage.class );
        mountPage( "/dashboardrun", RunsByTypeDashboardRunPage.class );
        mountPage( "/dashboardsuite", RunsByTypeDashboardSuitePage.class );

        mountPage( "/groups", TestcasesByGroupsPage.class );

        try {
            configProperties = new Properties();
            configProperties.load( this.getClass()
                                       .getClassLoader()
                                       .getResourceAsStream( "ats.config.properties" ) );
        } catch( IOException e ) {
            LOG.error( "Can't load config.properties file", e );
        }

        getAjaxRequestTargetListeners().add( new AjaxRequestTarget.IListener() {

            @Override
            public void onBeforeRespond( Map<String, Component> map, final AjaxRequestTarget target ) {

                // if( !Session.get().getFeedbackMessages().isEmpty() ) {

                target.getPage().visitChildren( IFeedback.class, new IVisitor<Component, Void>() {
                    public void component( final Component component, final IVisit<Void> visit ) {

                        if( component.getOutputMarkupId() ) {
                            target.appendJavaScript( "$('#" + component.getMarkupId()
                                                     + "').effect('bounce', { times:5 }, 200);" );
                            target.add( component );
                            //visit.stop();
                        }
                        visit.dontGoDeeper();
                    }
                } );
            }

            @Override
            public void onAfterRespond( Map<String, Component> map, IJavaScriptResponse response ) {

                // Do nothing.
            }

            @Override
            public void updateAjaxAttributes( AbstractDefaultAjaxBehavior behavior,
                                              AjaxRequestAttributes attributes ) {

                // TODO Auto-generated method stub

            }
        } );

        // load any available Test Explorer plugins
        TestExplorerPluginsRepo.getInstance();
    }

    public Properties getConfigProperties() {

        return configProperties;
    }

    /**
     * @see org.apache.wicket.Application#getHomePage()
     */
    public Class<WelcomePage> getHomePage() {

        return WelcomePage.class;
    }

    @Override
    public final Session newSession( Request request, Response response ) {

        return new TestExplorerSession( request );
    }

    /**
     * Add columnDefinition list for dbname
     * 
     * @param dbName
     * @param columnDefinition
     */
    public void setColumnDefinition( String dbName, List<TableColumn> columnDefinition ) {

        columnDefinitionsMap.put( dbName, columnDefinition );
    }

    public List<TableColumn> getColumnDefinition( String dbName ) {

        return columnDefinitionsMap.get( dbName );
    }
}
