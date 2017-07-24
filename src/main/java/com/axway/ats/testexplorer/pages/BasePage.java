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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.wicket.Component;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.IAjaxIndicatorAware;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnLoadHeaderItem;
import org.apache.wicket.markup.html.TransparentWebMarkupContainer;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.HiddenField;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.http.WebResponse;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.axway.ats.log.autodb.entities.Scenario;
import com.axway.ats.log.autodb.entities.Suite;
import com.axway.ats.log.autodb.entities.Testcase;
import com.axway.ats.testexplorer.TestExplorerUtils;
import com.axway.ats.testexplorer.model.TestExplorerSession;
import com.axway.ats.testexplorer.model.db.RunsDataSource;
import com.axway.ats.testexplorer.model.db.ScenariosDataSource;
import com.axway.ats.testexplorer.model.db.SuitesDataSource;
import com.axway.ats.testexplorer.model.db.utilities.TestcasesCopyUtility.ENTITY_TYPES;
import com.axway.ats.testexplorer.pages.model.MainDataGrid;
import com.axway.ats.testexplorer.pages.model.TableColumn;
import com.axway.ats.testexplorer.pages.runs.RunsPage;
import com.axway.ats.testexplorer.pages.runs.RunsPanel;
import com.axway.ats.testexplorer.pages.scenarios.ScenariosPage;
import com.axway.ats.testexplorer.pages.scenarios.ScenariosPanel;
import com.axway.ats.testexplorer.pages.suites.SuitesPage;
import com.axway.ats.testexplorer.pages.suites.SuitesPanel;
import com.axway.ats.testexplorer.pages.testcases.TestcasesPage;
import com.axway.ats.testexplorer.pages.testcasesCopy.TestcasesCopyPage;
import com.inmethod.grid.IDataSource;
import com.inmethod.grid.IGridColumn;
import com.inmethod.grid.common.ColumnsState;
import com.inmethod.grid.datagrid.DataGrid;

@SuppressWarnings({ "unchecked", "rawtypes" })
public abstract class BasePage extends WebPage implements IAjaxIndicatorAware {

    private static final long         serialVersionUID                = 1L;

    protected static transient Logger LOG;

    private String                    pageSuffix;

    private Label                     itemsCountLabel;

    private DataGrid                  runGrid;
    private DataGrid                  suiteGrid;
    private DataGrid                  scenarioGrid;
    private Class<?>                  currentClass;

    private List<PagePojo>            navigationList                  = new ArrayList<PagePojo>();

    protected Map<String, String>     singleTestIds                   = new HashMap<String, String>();

    private IModel<String>            runCopyLinkModel                = new Model<String>( "" );
    private IModel<String>            testcasesCopyLinkModel          = new Model<String>( "" );
    private IModel<String>            representationLinkModel         = new Model<String>( "" );

    public boolean                    showTestcaseStatusChangeButtons = false;

    // we remember the current grid we work with
    private MainDataGrid              mainGrid;

    // preserves the timeOffset for the current session
    private HiddenField<String>       timeOffsetField                 = new HiddenField<>( "timeOffset",
                                                                                           new Model<String>( "" ) );
    // preserves the time offset (from UTC) for the current session
    private HiddenField<String>       currentTimestampField           = new HiddenField<>( "currentTimestamp",
                                                                                           new Model<String>( "" ) );
    
    // preserves whether when calculating all time stamps, requested by the current session, into consideration must be taken day-light saving
    private HiddenField<String>       dayLightSavingOnField           = new HiddenField<>( "dayLightSavingOn",
                                                                                           new Model<String>( "" ) );

    public BasePage( PageParameters parameters ) {

        super( parameters );

        LOG = Logger.getLogger( this.getClass() );

        add( new Label( "page_title", "Axway ATS Test Explorer - " + getPageName() ) );

        // check DB connection and sets the current DB Name
        getTESession().getDbReadConnection();

        WebMarkupContainer topRightContent = new WebMarkupContainer( "topRightContent" );
        add( topRightContent );

        String dbName = getTESession().getDbName();
        if( dbName == null || "".equals( dbName ) ) {
            topRightContent.add( new Label( "dbName", "" ).setVisible( false ) );
            topRightContent.add( new Label( "machinesLink", "" ).setVisible( false ) );
            topRightContent.add( new Label( "runCopyLink", runCopyLinkModel ).setVisible( false ) );
            topRightContent.add( new Label( "testcasesCopyLink",
                                            testcasesCopyLinkModel ).setVisible( false ) );
            topRightContent.add( new Label( "representationLink",
                                            representationLinkModel ).setVisible( false ) );
        } else {
            String dbNameAndVersion = dbName;
            String dbVersion = getTESession().getDbVersion();
            if( dbVersion != null ) {
                dbNameAndVersion = dbNameAndVersion + ", v" + dbVersion;
            }
            topRightContent.add( new Label( "dbName",
                                            "<div class=\"dbName\"><span style=\"color:#C8D5DF;\">Exploring database:</span>&nbsp; "
                                                      + dbNameAndVersion
                                                      + "</div>" ).setEscapeModelStrings( false ) );
            topRightContent.add( new Label( "machinesLink", "<a href=\"machines?dbname=" + dbName
                                                            + "\" class=\"machinesLink\" target=\"_blank\"></a>" ).setEscapeModelStrings( false ) );
            runCopyLinkModel.setObject( "<a href=\"runCopy?dbname=" + dbName
                                        + "\" class=\"runCopyLink\" target=\"_blank\"></a>" );
            topRightContent.add( new Label( "runCopyLink",
                                            runCopyLinkModel ).setEscapeModelStrings( false ) );

            testcasesCopyLinkModel.setObject( "<a href=\"testcasesCopy?dbname=" + dbName
                                              + "\" class=\"testcasesCopyLink\" target=\"_blank\"></a>" );
            topRightContent.add( getTestcasesCopyButton() );

            representationLinkModel.setObject( createRepresentationLinkModelObject() );

            topRightContent.add( new Label( "representationLink",
                                            representationLinkModel ).setEscapeModelStrings( false ) );

        }

        itemsCountLabel = new Label( "itemsCount", new Model<Integer>() {

            private static final long serialVersionUID = 1L;

            @Override
            public Integer getObject() {

                return getTESession().getCompareContainer().size();
            }
        } );
        itemsCountLabel.setOutputMarkupId( true );
        topRightContent.setVisible( ! ( this instanceof WelcomePage ) );
        topRightContent.add( itemsCountLabel );

        FeedbackPanel feedbackPanel = new FeedbackPanel( "feedback" );
        feedbackPanel.setOutputMarkupId( true );
        add( feedbackPanel );

        // add navigation panel
        add( new ListView<PagePojo>( "navigation_links", navigationList ) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem( ListItem<PagePojo> item ) {

                final PagePojo pp = item.getModelObject();

                if( pp.pageSuffix != null && !pp.pageName.endsWith( "</span>" ) ) {
                    pp.pageName = pp.pageName + " <span class=\"locationName\">[" + pp.pageSuffix
                                  + "]</span>";
                }

                item.add( new Link<Object>( "navigation_link" ) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    protected CharSequence getURL() {

                        // generate Bookmarkable link url
                        return urlFor( pp.pageClass, pp.parameters );
                    }

                    @Override
                    public void onClick() {

                        // This link acts like Bookmarkable link and don't have a click handler.
                    }
                }.add( new Label( "navigation_link_name", pp.pageName ).setEscapeModelStrings( false ) ) );
            }
        } );
        add( new Label( "navigation_current_page_name", getPageName() ) );
        add( getNavigationSuffixComponent() );
        add( getTestcaseNavigationButtons() );

        currentTestDetails();

        // add child page
        TransparentWebMarkupContainer pageWrapper = new TransparentWebMarkupContainer( "page_wrapper" );
        add( pageWrapper );

        if( TestExplorerUtils.extractPageParameter( parameters, "hacks" ) != null ) {
            showTestcaseStatusChangeButtons = true;
        }

        add( timeOffsetField );
        add( currentTimestampField );
        add( dayLightSavingOnField );

        // AJAX handler for obtaining browser's time offset from UTC and current browser timestamp
        add( new AbstractDefaultAjaxBehavior() {

            private static final long serialVersionUID = 1L;

            @Override
            protected void respond( AjaxRequestTarget target ) {

                IRequestParameters request = RequestCycle.get().getRequest().getRequestParameters();
                int timeOffset = request.getParameterValue( "timeOffset" ).toInt();
                TestExplorerSession teSession = ( TestExplorerSession ) Session.get();
                teSession.setTimeOffset( timeOffset );
                teSession.setCurrentTimestamp( request.getParameterValue( "currentTimestamp" ).toLong() );
                teSession.setDayLightSavingOn( request.getParameterValue( "dayLightSavingOn" ).toBoolean() );
            }

            @Override
            protected void updateAjaxAttributes( AjaxRequestAttributes attributes ) {

                super.updateAjaxAttributes( attributes );
                attributes.getDynamicExtraParameters()
                          .add( "return {'timeOffset': $('#timeOffset').val(), "
                                + "'currentTimestamp': $('#currentTimestamp').val(),"
                                + "'dayLightSavingOn': $('#dayLightSavingOn').val() }" );
            }

            @Override
            public void renderHead( Component component, IHeaderResponse response ) {

                // Date.prototype.getTimezoneOffset() returns negative value if the local time is ahead of UTC,
                // so we invert the result, before sending it to Wicket
                String getTimeOffsetScript = ";var timeOffset = $('#timeOffset');timeOffset.val(new Date().getTimezoneOffset()*60*1000*-1);"
                                             + ";var currentTimestamp = $('#currentTimestamp');currentTimestamp.val(new Date().getTime());"
                                             + ";var dayLightSavingOn = $('#dayLightSavingOn');dayLightSavingOn.val(isDayLightSavingOn());";
                response.render( OnLoadHeaderItem.forScript( getCallbackScript().toString() ) );
                response.render( OnLoadHeaderItem.forScript( getTimeOffsetScript ) );
            }

        } );

    }

    private String createRepresentationLinkModelObject() {

        String tableLinkStyle = "style=background-color:#transparent;";
        String dashboardLinkStyle = "style=background-color:#transparent;";
        String groupsLinkStyle = "style=background-color:#transparent;";

        if( "Runs".equals( this.getPageName() ) ) {
            tableLinkStyle = "style=background-color:#598196;color:#FFFFFF";
        }

        if( "Groups".equals( this.getPageName() ) ) {
            groupsLinkStyle = "style=background-color:#598196;color:#FFFFFF";
        }

        if( "Dashboard Home".equals( this.getPageName() ) ) {
            dashboardLinkStyle = "style=background-color:#598196;color:#FFFFFF";
        }

        StringBuilder sb = new StringBuilder();
        sb.append( "<a href='javascript:showOrHideRepresentationDropdownList()' class=\"representationLink\"></a>" )
          .append( "<div class=\"dropdown\">" )
          .append( "<div id=\"representationDropdown\" class=\"dropdown-content\">" )
          .append( "<a title='Show runs by execution time' id='tableViewLink' " + tableLinkStyle
                   + " href=\"./runs\">Runs by execution time</a>" )
          .append( "<a title='Show runs by type' id='dashboardViewLink' " + dashboardLinkStyle
                   + " href=\"./dashboardhome\">Runs by type</a>" )
          .append( "<a title='Show test cases by groups' id='groupsViewLink ' " + groupsLinkStyle
                   + " href=\"./groups\">Test cases by groups</a>" )
          .append( "</div>" )
          .append( "</div>" );
        return sb.toString();
    }

    private void currentTestDetails() {

        currentClass = this.getClass();
        final WebMarkupContainer testDetails = new WebMarkupContainer( "testDetails" );
        testDetails.setOutputMarkupId( true );
        add( testDetails );

        // here we will create empty datagrids, and later we will replace them with the full ones
        runGrid = new DataGrid( "singleRun", new SuitesDataSource( "0" ), new ArrayList<IGridColumn>() );
        runGrid.setOutputMarkupId( true );
        runGrid.setVisible( false );
        testDetails.add( runGrid );

        suiteGrid = new DataGrid( "singleSuite", new SuitesDataSource( "0" ), new ArrayList<IGridColumn>() );
        suiteGrid.setOutputMarkupId( true );
        suiteGrid.setVisible( false );
        testDetails.add( suiteGrid );

        scenarioGrid = new DataGrid( "singleScenario", new SuitesDataSource( "0" ),
                                     new ArrayList<IGridColumn>() );
        scenarioGrid.setOutputMarkupId( true );
        scenarioGrid.setVisible( false );
        testDetails.add( scenarioGrid );

        AjaxLink<Object> testDetailslink = new AjaxLink<Object>( "testDetailsButton" ) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick( AjaxRequestTarget target ) {

                boolean isRunVisible = runGrid.isVisible();
                String runId = singleTestIds.get( "runId" );
                String suiteId = singleTestIds.get( "suiteId" );
                String scenarioId = singleTestIds.get( "scenarioId" );

                if( !isRunVisible ) {
                    RunsPanel runs = new RunsPanel( runId );

                    createSingleGrid( testDetails, runGrid, "singleRun", new RunsDataSource( runId ),
                                      runs.getColumns( null ), runs.getTableColumnDefinitions() );
                }

                if( currentClass != SuitesPage.class ) {
                    if( !isRunVisible ) {
                        SuitesPanel suites = new SuitesPanel( suiteId );

                        createSingleGrid( testDetails, suiteGrid, "singleSuite",
                                          new SuitesDataSource( runId, suiteId ), suites.getColumns(),
                                          suites.getTableColumnDefinitions() );
                    }
                    suiteGrid.setVisible( !isRunVisible );
                    target.add( suiteGrid );

                    if( currentClass != ScenariosPage.class ) {
                        if( !isRunVisible ) {
                            ScenariosPanel scenarios = new ScenariosPanel( scenarioId );

                            createSingleGrid( testDetails, scenarioGrid, "singleScenario",
                                              new ScenariosDataSource( suiteId, scenarioId ),
                                              scenarios.getColumns(), scenarios.getTableColumnDefinitions() );
                        }
                        scenarioGrid.setVisible( !isRunVisible );
                        target.add( scenarioGrid );
                    }
                }
                // here we will call JS function to show the navigation test details
                target.appendJavaScript( "showOrHideTestDetails(" + !isRunVisible + ")" );

                runGrid.setVisible( !isRunVisible );
                target.add( runGrid );
                target.add( testDetails );
            }
        };
        testDetailslink.setEnabled( currentClass != WelcomePage.class && currentClass != BasePage.class
                                    && currentClass != RunsPage.class );
        add( testDetailslink );

    }

    private void createSingleGrid( WebMarkupContainer testDetails, DataGrid grid, String instance,
                                   IDataSource dataSource, List<IGridColumn> columns,
                                   List<TableColumn> columnDetails ) {

        List<IGridColumn> cc = new ArrayList<IGridColumn>( columns.subList( 1, columns.size() ) );

        grid = new DataGrid( instance, dataSource, cc );
        grid.setOutputMarkupId( true );
        testDetails.addOrReplace( grid );

        // we have to add the table to the grid, so we could change column settings
        ColumnsState cs = grid.getColumnState();

        for( TableColumn col : columnDetails ) {
            cs.setColumnWidth( col.getColumnId(), col.getInitialWidth() );
            cs.setColumnVisibility( col.getColumnId(), col.isVisible() );
        }

        grid.setColumnState( cs );
    }

    public void setMainGrid( MainDataGrid mainGrid ) {

        this.mainGrid = mainGrid;
    }

    /**
     *
     * @return Testcase navigation buttons component
     */
    protected Component getTestcaseNavigationButtons() {

        WebMarkupContainer testcaseNavigationButtons = new WebMarkupContainer( "testcaseNavigationButtons" );
        testcaseNavigationButtons.setVisible( false );

        testcaseNavigationButtons.add( new ExternalLink( "goToPrevTestcase", "#" ) );
        testcaseNavigationButtons.add( new ExternalLink( "goToNextTestcase", "#" ) );
        testcaseNavigationButtons.add( new ExternalLink( "goToLastTestcase", "#" ) );

        return testcaseNavigationButtons;
    }

    private Component getTestcasesCopyButton() {

        AjaxLink<Object> testcasesCopyLink = new AjaxLink<Object>( "testcasesCopyLink" ) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick( AjaxRequestTarget target ) {

                if( mainGrid.getSelectedItems().size() == 0 ) {
                    target.appendJavaScript( "alert('Please select one or more items to copy');" );
                } else {
                    // serialize all the important DB entities in a string
                    StringBuilder copyEntities = new StringBuilder();
                    PageParameters parameters = new PageParameters();

                    ENTITY_TYPES copyEntityTypes = null;
                    for( IModel<?> model : ( Collection<IModel<?>> ) mainGrid.getSelectedItems() ) {

                        Object obj = model.getObject();
                        if( obj instanceof Suite ) {
                            copyEntityTypes = ENTITY_TYPES.SUITES;
                            copyEntities.append( ( ( Suite ) obj ).suiteId );
                        } else if( obj instanceof Scenario ) {
                            copyEntityTypes = ENTITY_TYPES.SCENARIOS;
                            copyEntities.append( ( ( Scenario ) obj ).scenarioId );
                            parameters.add( "suiteId", ( ( Scenario ) obj ).suiteId );
                        } else {
                            copyEntityTypes = ENTITY_TYPES.TESTCASES;
                            copyEntities.append( ( ( Testcase ) obj ).testcaseId );
                        }

                        copyEntities.append( "_" );
                    }

                    String copyEntitiesString = copyEntities.toString();
                    copyEntitiesString = copyEntitiesString.substring( 0, copyEntitiesString.length() - 1 );

                    parameters.add( "dbname", ( ( TestExplorerSession ) Session.get() ).getDbName() );
                    parameters.add( "copyEntities", copyEntitiesString );
                    parameters.add( "copyEntitiesType", copyEntityTypes.toString() );
                    setResponsePage( TestcasesCopyPage.class, parameters );
                }
            }
        };
        testcasesCopyLink.setVisible( this instanceof SuitesPage || this instanceof ScenariosPage
                                      || this instanceof TestcasesPage );

        return testcasesCopyLink;
    }

    public BasePage() {

        this( null );
    }

    public abstract String getPageName();

    public Component getNavigationSuffixComponent() {

        return new Label( "navigation_suffix" ).setVisible( false );
    }

    public String getPageSuffix() {

        return this.pageSuffix;
    }

    public void setPageSuffix( String pageSuffix ) {

        this.pageSuffix = pageSuffix;
    }

    public Label getItemsCountLabel() {

        itemsCountLabel.setDefaultModelObject( getTESession().getCompareContainer().size() );
        return itemsCountLabel;
    }

    public TestExplorerSession getTESession() {

        return ( TestExplorerSession ) Session.get();
    }

    @Override
    public String getAjaxIndicatorMarkupId() {

        return "ajaxLoader";
    }

    @Override
    protected void configureResponse( WebResponse response ) {

        response.disableCaching();
        super.configureResponse( response );
    }

    public void addNavigationLink( Class<? extends BasePage> pageClass, PageParameters parameters,
                                   String pageName, String pageSuffix ) {

        navigationList.add( new PagePojo( pageClass, parameters, pageName, pageSuffix ) );
    }

    public String getCurrentPath() {

        StringBuilder sb = new StringBuilder();
        for( int i = 1; i < navigationList.size(); i++ ) {
            PagePojo page = navigationList.get( i );
            sb.append( page.pageSuffix.replace( "/", "\\" ) );
            if( i < navigationList.size() - 1 ) {
                sb.append( "/" );
            }
        }
        return sb.toString();
    }

    public void setRunIdToRunCopyLink( String runId ) {

        if( !runCopyLinkModel.getObject().isEmpty() ) {
            runCopyLinkModel.setObject( runCopyLinkModel.getObject()
                                                        .replace( "?dbname=",
                                                                  "?runId=" + runId + "&dbname=" ) );
        }
    }
}

class PagePojo implements Serializable {

    private static final long        serialVersionUID = 1L;
    public Class<? extends BasePage> pageClass;
    public PageParameters            parameters;
    public String                    pageName;
    public String                    pageSuffix;

    public PagePojo( Class<? extends BasePage> pageClass, PageParameters parameters, String pageName,
                     String pageSuffix ) {

        this.pageClass = pageClass;
        this.parameters = parameters;
        this.pageName = pageName;
        this.pageSuffix = pageSuffix;
    }
}
