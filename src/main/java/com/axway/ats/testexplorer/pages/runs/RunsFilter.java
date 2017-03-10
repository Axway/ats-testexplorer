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
package com.axway.ats.testexplorer.pages.runs;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.log4j.Logger;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.datetime.markup.html.form.DateTextField;
import org.apache.wicket.extensions.yui.calendar.DatePicker;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.handler.resource.ResourceReferenceRequestHandler;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.util.string.StringValue;
import org.apache.wicket.validation.validator.DateValidator;

import com.axway.ats.core.utils.StringUtils;
import com.axway.ats.testexplorer.TestExplorerUtils;
import com.axway.ats.testexplorer.pages.model.filtering.IFilter;
import com.inmethod.grid.datagrid.DataGrid;

public class RunsFilter extends Form<Object> implements IFilter {
    
    private Logger LOG = Logger.getLogger( getClass() );

    private static final long serialVersionUID   = 1L;
    
    // these values are added to the URL, when filtered runs are saved
    private String runValue = "run";
    private String productValue = "product";
    private String versionValue = "version";
    private String buildValue = "build";
    private String osValue = "os";
    private String userNoteValue = "userNote";
    private String beforeDateValue = "beforeDate";
    private String afterDateValue =  "afterDate";
    

    private TextField<String> searchByRun        = new TextField<String>( "search_by_run",
                                                                          new Model<String>( "" ) );
    private TextField<String> searchByProduct    = new TextField<String>( "search_by_product",
                                                                          new Model<String>( "" ) );
    private TextField<String> searchByVersion    = new TextField<String>( "search_by_version",
                                                                          new Model<String>( "" ) );
    private TextField<String> searchByBuild      = new TextField<String>( "search_by_build",
                                                                          new Model<String>( "" ) );
    private TextField<String> searchByOs         = new TextField<String>( "search_by_os",
                                                                          new Model<String>( "" ) );

    private TextField<String> searchByUserNote   = new TextField<String>( "search_by_user_note",
                                                                          new Model<String>( "" ) );

    private DateTextField     searchByAfterDate  = DateTextField.forDatePattern( "search_by_after_date",
                                                                                 new Model<Date>(),
                                                                                 "dd.MM.yyyy" );
    private DateTextField     searchByBeforeDate = DateTextField.forDatePattern( "search_by_before_date",
                                                                                 new Model<Date>(),
                                                                                 "dd.MM.yyyy" );
    private String urlParameters = "";

    private boolean showFilter;
    
    @SuppressWarnings({ "rawtypes" })
    public RunsFilter( String id,
                       final DataGrid dataGrid,
                       final PageParameters parameters) {

        super( id );

        // filter fields
        searchByRun.setEscapeModelStrings( false );
        searchByRun.setOutputMarkupId( true );
        searchByProduct.setEscapeModelStrings( false );
        searchByProduct.setOutputMarkupId( true );
        searchByVersion.setEscapeModelStrings( false );
        searchByVersion.setOutputMarkupId( true );
        searchByBuild.setEscapeModelStrings( false );
        searchByBuild.setOutputMarkupId( true );
        searchByOs.setEscapeModelStrings( false );
        searchByOs.setOutputMarkupId( true );
        searchByUserNote.setOutputMarkupId( true );
        searchByUserNote.setEscapeModelStrings( false );
        searchByAfterDate.setOutputMarkupId( true );
        searchByBeforeDate.setOutputMarkupId( true );

        searchByAfterDate.add( DateValidator.maximum( new Date(), "dd.MM.yyyy" ) );

        add( searchByRun );
        add( searchByProduct );
        add( searchByVersion );
        add( searchByBuild );
        add( searchByOs );
        add( searchByUserNote );
        add( searchByAfterDate );
        add( searchByBeforeDate );

        // attach a Date Picker component
        searchByAfterDate.add( new TEDatePicker().setShowOnFieldClick( true ).setAutoHide( true ) );
        searchByBeforeDate.add( new TEDatePicker().setShowOnFieldClick( true ).setAutoHide( true ) );

        // search button
        AjaxButton searchButton = new AjaxButton( "search_button" ) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(
                                     AjaxRequestTarget target,
                                     Form<?> form ) {
                clearPageParameters(parameters);

                target.add( dataGrid );
            }

            @Override
            protected void onError(
                                    AjaxRequestTarget target,
                                    Form<?> form ) {

                super.onError( target, form );
            }
        };
        add( searchButton );
        // search button is the button to trigger when user hit the enter key
        this.setDefaultButton( searchButton );

        // reset button
        add( new AjaxButton( "reset_button" ) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(
                                     AjaxRequestTarget target,
                                     Form<?> form ) {

                // reset the filter
                searchByRun.setModelObject( "" );
                searchByProduct.setModelObject( "" );
                searchByVersion.setModelObject( "" );
                searchByBuild.setModelObject( "" );
                searchByOs.setModelObject( "" );
                searchByUserNote.setModelObject( "" );
                searchByAfterDate.setModelObject( null );
                searchByBeforeDate.setModelObject( null );

                target.add( searchByRun );
                target.add( searchByProduct );
                target.add( searchByVersion );
                target.add( searchByBuild );
                target.add( searchByOs );
                target.add( searchByUserNote );
                target.add( searchByAfterDate );
                target.add( searchByBeforeDate );

                // automatically trigger a new search
                target.add( dataGrid );
            }
        } );
        
        // copy URL button
        add( new AjaxButton( "copy_url_button" ) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(
                                     AjaxRequestTarget target,
                                     Form<?> form ) {

                String jsQuery = "window.prompt(\"Press 'Ctrl+C' to copy the following URL containing all Runs filter data\", window.location";
                if( StringUtils.isNullOrEmpty( urlParameters ) ) {
                    jsQuery = jsQuery + ")";
                } else {
                    jsQuery = jsQuery + "+'&" + urlParameters + "')";
                }
                target.appendJavaScript( jsQuery );
                target.add( dataGrid );
            }
        } );
        
        setSearchValuesOnLoad(parameters);
        
    }
    
    @Override
    public boolean hasSelectedFields() {

        if( !StringUtils.isNullOrEmpty( searchByRun.getModelObject() ) ) {
            return true;
        }
        if( !StringUtils.isNullOrEmpty( searchByProduct.getModelObject() ) ) {
            return true;
        }
        if( !StringUtils.isNullOrEmpty( searchByVersion.getModelObject() ) ) {
            return true;
        }
        if( !StringUtils.isNullOrEmpty( searchByBuild.getModelObject() ) ) {
            return true;
        }
        if( !StringUtils.isNullOrEmpty( searchByOs.getModelObject() ) ) {
            return true;
        }
        if( !StringUtils.isNullOrEmpty( searchByUserNote.getModelObject() ) ) {
            return true;
        }
        if( searchByAfterDate.getModelObject() != null ) {
            return true;
        }
        if( searchByBeforeDate.getModelObject() != null ) {
            return true;
        }

        return false;
   }
    
    @Override
    public void renderHead(
                            IHeaderResponse response ) {

        super.renderHead( response );
        if(showFilter || hasSelectedFields())
            response.render( OnDomReadyHeaderItem.forScript( "$('.filterHeader').click()" ) );
    }
    
    
    private void setSearchValuesOnLoad(
                                        PageParameters parameters ) {

        searchByRun.setModelObject( getParameterValue( parameters.get( runValue ) ) );

        searchByProduct.setModelObject( getParameterValue( parameters.get( productValue ) ) );

        searchByVersion.setModelObject( getParameterValue( parameters.get( versionValue ) ) );

        searchByBuild.setModelObject( getParameterValue( parameters.get( buildValue ) ) );

        searchByOs.setModelObject( getParameterValue( parameters.get( osValue ) ) );

        searchByUserNote.setModelObject( getParameterValue( parameters.get( userNoteValue ) ) );

        try {
            String afterDate = parameters.get( afterDateValue ).toString();
            if( !StringUtils.isNullOrEmpty( afterDate ) )
                searchByAfterDate.setModelObject( new SimpleDateFormat( "dd.MM.yyyy" ).parse( afterDate ) );

            String beforeDate = parameters.get( beforeDateValue ).toString();
            if( !StringUtils.isNullOrEmpty( beforeDate ) )
                searchByBeforeDate.setModelObject( new SimpleDateFormat( "dd.MM.yyyy" ).parse( beforeDate ) );
        } catch( ParseException e ) {
            LOG.debug( "Unable to parse date !", e );
        }
    }
    
    private String getParameterValue(
                                      StringValue value ) {

        if( !StringUtils.isNullOrEmpty( value.toString() ) ){
            // the RUNS FILTER will be opened on load, because filtered runs will be shown
            showFilter = true;
            return value.toString();
        }

        return "";
    }
    
    private void clearPageParameters(PageParameters parameters){
        
        parameters.remove( "run" );
        parameters.remove( "product" );
        parameters.remove( "version" );
        parameters.remove( "build" );
        parameters.remove( "os" );
        parameters.remove( "userNote" );
        parameters.remove( "afterDate" );
        parameters.remove( "beforeDate" );
    }
    
    public String getWhereClause() {

        StringBuilder where = new StringBuilder();
        urlParameters = "";

        String searchByRunValue = searchByRun.getValue();
        if( searchByRunValue != null && searchByRunValue.length() > 0 ) {
            where.append( " AND runName LIKE '%" + TestExplorerUtils.escapeSqlSearchValue( searchByRunValue )
                          + "%' escape '\\'" );
            urlParameters = urlParameters + "&run=" + searchByRunValue;
        }

        String searchByProductValue = searchByProduct.getValue();
        if( searchByProductValue != null && searchByProductValue.length() > 0 ) {
            where.append( " AND productName LIKE '%" + TestExplorerUtils.escapeSqlSearchValue( searchByProductValue )
                          + "%' escape '\\'" );
            urlParameters = urlParameters + "&product=" + searchByProductValue;
        }

        String searchByVersionValue = searchByVersion.getValue();
        if( searchByVersionValue != null && searchByVersionValue.length() > 0 ) {
            where.append( " AND versionName LIKE '%" + TestExplorerUtils.escapeSqlSearchValue( searchByVersionValue )
                          + "%' escape '\\'" );
            urlParameters = urlParameters + "&version=" + searchByVersionValue;
        }

        String searchByBuildValue = searchByBuild.getValue();
        if( searchByBuildValue != null && searchByBuildValue.length() > 0 ) {
            where.append( " AND buildName LIKE '%" + TestExplorerUtils.escapeSqlSearchValue( searchByBuildValue )
                          + "%' escape '\\'" );
            urlParameters = urlParameters + "&build=" + searchByBuildValue;
        }

        String searchByOsValue = searchByOs.getValue();
        if( searchByOsValue != null && searchByOsValue.length() > 0 ) {
            where.append( " AND OS LIKE '%" + TestExplorerUtils.escapeSqlSearchValue( searchByOsValue )
                          + "%' escape '\\'" );
            urlParameters = urlParameters + "&os=" + searchByOsValue;
        }

        String searchByUserNoteValue = searchByUserNote.getValue();
        if( searchByUserNoteValue != null && searchByUserNoteValue.length() > 0 ) {
            where.append( " AND userNote LIKE '%" + TestExplorerUtils.escapeSqlSearchValue( searchByUserNoteValue )
                          + "%' escape '\\'" );
            urlParameters = urlParameters + "&userNote=" + searchByUserNoteValue;
        }

        //to compare dates if it is valid SimpleDateFormat
        String afterDate = searchByAfterDate.getValue();
        String beforeDate = searchByBeforeDate.getValue();

        // check whether start date is before end date
        if( !StringUtils.isNullOrEmpty( afterDate ) && !StringUtils.isNullOrEmpty( beforeDate ) ) {

            SimpleDateFormat dates = new SimpleDateFormat( "dd.MM.yyyy" );

            try {
                Date dateStartParse = dates.parse( afterDate );
                Date dateEndParse = dates.parse( beforeDate );
                if( dateStartParse.after( dateEndParse ) ) {

                    error( "The provided value for 'Started before'(" + beforeDate
                           + ") is before the value for 'Started after'(" + afterDate + ")" );
                }
            } catch( ParseException e ) {
                // already catched by the DateValidator
            }
        }

        // add start/end dates to the where clause
        if( !StringUtils.isNullOrEmpty( afterDate ) ) {

            String[] tokens = afterDate.split( "\\." );
            where.append( " AND dateStart >= CONVERT(DATETIME,'" + tokens[2] + "-" + tokens[1] + "-"
                          + tokens[0] + " 00:00:00',20)" );
            urlParameters = urlParameters + "&afterDate=" + afterDate;
        }
        if( !StringUtils.isNullOrEmpty( beforeDate ) ) {

            String[] tokens = beforeDate.split( "\\." );
            where.append( " AND dateStart <= CONVERT(DATETIME,'" + tokens[2] + "-" + tokens[1] + "-"
                          + tokens[0] + " 23:59:59',20)" );
            urlParameters = urlParameters + "&beforeDate=" + beforeDate;
        }

        if( where.length() > 0 ) {
            where.delete( 0, " AND".length() );
            where.insert( 0, "WHERE" );
        }
        return where.toString();
    }

    /**
     * A date picker with icon different than the default one
     */
    private class TEDatePicker extends DatePicker {

        private static final long serialVersionUID = 1L;

        @Override
        protected CharSequence getIconUrl() {

            return RequestCycle.get()
                               .urlFor( new ResourceReferenceRequestHandler( new PackageResourceReference( DatePicker.class,
                                                                                                           "icon2.gif" ) ) );
        }
    }
}
