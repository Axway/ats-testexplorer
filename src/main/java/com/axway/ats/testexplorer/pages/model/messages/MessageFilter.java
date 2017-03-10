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
package com.axway.ats.testexplorer.pages.model.messages;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.extensions.markup.html.form.select.IOptionRenderer;
import org.apache.wicket.extensions.markup.html.form.select.Select;
import org.apache.wicket.extensions.markup.html.form.select.SelectOptions;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.util.WildcardCollectionModel;

import com.axway.ats.core.utils.StringUtils;
import com.axway.ats.log.autodb.exceptions.DatabaseAccessException;
import com.axway.ats.testexplorer.TestExplorerUtils;
import com.axway.ats.testexplorer.model.TestExplorerSession;
import com.axway.ats.testexplorer.model.db.MessageFilterDetails;
import com.axway.ats.testexplorer.model.db.TestExplorerDbReadAccessInterface;
import com.axway.ats.testexplorer.pages.model.filtering.IFilter;
import com.inmethod.grid.datagrid.DataGrid;

@SuppressWarnings({ "rawtypes" })
public class MessageFilter extends Form<Object> implements IFilter{

    private static final long                    serialVersionUID = 1L;

    private static Logger                        LOG              = Logger.getLogger( MessageFilter.class );

    private Select                               threadChoices;

    private Select                               machineChoices;

    private Select                               levelChoices;

    private TextField<String>                    searchByMessage  = new TextField<String>( "search_by_message",
                                                                                           new Model<String>( "" ) );

    private String                               idColumnValue;

    private IModel<Collection<? extends String>> selectedThreads;

    private IModel<Collection<? extends String>> selectedMachines;

    private IModel<Collection<? extends String>> selectedLevels;

    private MessageFilterDetails                 messageFilterDetails;

    public MessageFilter( String wicketId,
                          final DataGrid dataGrid,
                          String idColumnName,
                          String idColumnValue ) {

        super( wicketId );

        this.idColumnValue = idColumnValue;
        searchByMessage.setOutputMarkupId( true );
        searchByMessage.setEscapeModelStrings( false );
        add( searchByMessage );

        TestExplorerDbReadAccessInterface dbAccess = ( ( TestExplorerSession ) Session.get() ).getDbReadConnection();
        String table = "";
        String whereClause = "";
        try {
            if( "runId".equals( idColumnName ) ) {
                table = "tRunMessages";
                whereClause = "WHERE runMessageId IN (SELECT runMessageId FROM tRunMessages WHERE runId="
                              + idColumnValue + ")";
            } else if( "suiteId".equals( idColumnName ) ) {
                table = "tSuiteMessages";
                whereClause = "WHERE suiteMessageId IN (SELECT suiteMessageId FROM tSuiteMessages WHERE suiteId="
                              + idColumnValue + ")";
            } else {
                table = "tMessages";
                whereClause = "WHERE testcaseId=" + idColumnValue;
            }
            String sqlQuery = "SELECT DISTINCT mt.messageTypeId, mt.name as levelName, m.threadName, "
                    + "CASE WHEN mach.machineAlias is NULL OR DATALENGTH(mach.machineAlias) = 0 THEN mach.machineName "
                    + " ELSE mach.machineAlias END as machineName "
                    + "FROM " + table + " AS m "
                    + "LEFT JOIN tMessageTypes mt ON m.messageTypeId = mt.messageTypeId "
                    + "JOIN tMachines mach ON m.machineId = mach.machineId "
                    + whereClause + " ORDER BY mt.messageTypeId";

            messageFilterDetails = dbAccess.getMessageFilterDetails( sqlQuery );
        } catch( DatabaseAccessException e ) {
            LOG.error( "Can't get message filter details", e );
        }
        add( getThreadChoices() );
        add( getMachineChoices() );
        add( getLevelChoices() );

        // search button
        AjaxButton searchButton = new AjaxButton( "search_button" ) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(
                                     AjaxRequestTarget target,
                                     Form<?> form ) {

                target.add( dataGrid );
            }
        };
        add( searchButton );

        AjaxButton hiddenSearchButton = new AjaxButton( "hiddenSearchButton" ) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(
                                     AjaxRequestTarget target,
                                     Form<?> form ) {

                target.add( dataGrid );
            }
        };
        add( hiddenSearchButton );

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
                searchByMessage.setModelObject( "" );
                target.add( searchByMessage );

                selectedThreads.setObject( new TreeSet<String>( messageFilterDetails.getThreads() ) );
                target.add( threadChoices );

                selectedMachines.setObject( new TreeSet<String>( messageFilterDetails.getMachines() ) );
                target.add( machineChoices );

                selectedLevels.setObject( messageFilterDetails.getSelectedLevels() );
                target.add( levelChoices );

                // automatically trigger a new search
                target.add( dataGrid );
            }
        } );

        // if there are error log messages, show only them at page load
        if( messageFilterDetails.getSelectedLevels().contains( "error" ) ) {
            List<String> errorListObject = new ArrayList<String>();
            errorListObject.add( "error" );
            selectedLevels.setObject( errorListObject );
        }
    }
    
    @Override
    public boolean hasSelectedFields() {

        if( !StringUtils.isNullOrEmpty( searchByMessage.getModelObject() ) ) {
            return true;
        }
        if( selectedThreads.getObject().size() > 0 ) {
            return true;
        }
        if( selectedMachines.getObject().size() > 0 ) {
            return true;
        }
        if( selectedLevels.getObject().size() > 0 ) {
            return true;
        }

        return false;
    }

    @Override
    public void renderHead(
                            IHeaderResponse response ) {

        super.renderHead( response );
        if( hasSelectedFields() ) {
            response.render( OnDomReadyHeaderItem.forScript( "$('.filterHeader').click()" ) );
        }
   }

    private Select getLevelChoices() {

        levelChoices = new Select( "levelChoices" );
        selectedLevels = new WildcardCollectionModel<String>( messageFilterDetails.getSelectedLevels() );
        levelChoices.setDefaultModel( selectedLevels );
        IOptionRenderer<String> renderer = new IOptionRenderer<String>() {

            private static final long serialVersionUID = 1L;

            public String getDisplayValue(
                                           String object ) {

                return object;
            }

            public IModel<String> getModel(
                                            String value ) {

                return new Model<String>( value );
            }

        };
        IModel<Collection<? extends String>> optionsModel = new WildcardCollectionModel<String>( new ArrayList<String>( messageFilterDetails.getLevels() ) );
        levelChoices.add( new SelectOptions<String>( "levelOptions", optionsModel, renderer ) );
        levelChoices.setOutputMarkupId( true );
        return levelChoices;
    }

    private Select getMachineChoices() {

        machineChoices = new Select( "machineChoices" );
        Set<String> machines = new TreeSet<String>( messageFilterDetails.getMachines() );
        selectedMachines = new WildcardCollectionModel<String>( machines );
        machineChoices.setDefaultModel( selectedMachines );
        IOptionRenderer<String> renderer = new IOptionRenderer<String>() {

            private static final long serialVersionUID = 1L;

            public String getDisplayValue(
                                           String object ) {

                return object;
            }

            public IModel<String> getModel(
                                            String value ) {

                return new Model<String>( value );
            }

        };
        IModel<Collection<? extends String>> optionsModel = new WildcardCollectionModel<String>( machines );
        machineChoices.add( new SelectOptions<String>( "machineOptions", optionsModel, renderer ) );
        machineChoices.setOutputMarkupId( true );
        return machineChoices;
    }

    private Select getThreadChoices() {

        threadChoices = new Select( "threadChoices" );
        Set<String> threads = new TreeSet<String>( messageFilterDetails.getThreads() );
        selectedThreads = new WildcardCollectionModel<String>( threads );
        threadChoices.setDefaultModel( selectedThreads );
        IOptionRenderer<String> renderer = new IOptionRenderer<String>() {

            private static final long serialVersionUID = 1L;

            public String getDisplayValue(
                                           String object ) {

                return object;
            }

            public IModel<String> getModel(
                                            String value ) {

                return new Model<String>( value );
            }

        };
        IModel<Collection<? extends String>> optionsModel = new WildcardCollectionModel<String>( threads );
        threadChoices.add( new SelectOptions<String>( "threadOptions", optionsModel, renderer ) );
        threadChoices.setOutputMarkupId( true );
        return threadChoices;
    }

    public String getWhereClause(
                                  String id ) {

        StringBuilder where = new StringBuilder();

        String searchValue = searchByMessage.getValue();
        if( searchValue != null && searchValue.length() > 0 ) {

            where.append( " AND message LIKE '%" + TestExplorerUtils.escapeSqlSearchValue( searchValue )
                          + "%' escape '\\'" );
        }
        if( selectedThreads != null && selectedThreads.getObject().size() > 0
            && selectedThreads.getObject().size() < messageFilterDetails.getThreads().size() ) {

            where.append( " AND threadName in ("
                          + getQueryString( selectedThreads.getObject().toArray( new String[0] ) ) + ")" );
        }
        if( selectedMachines != null && selectedMachines.getObject().size() > 0
            && selectedMachines.getObject().size() < messageFilterDetails.getMachines().size() ) {

            /*
             * The function:   COALESCE ( expression [ ,...n ] )
             * is a shortcut for the CASE expression:
             * CASE
             *    WHEN (expression1 IS NOT NULL) THEN expression1
             *    WHEN (expression2 IS NOT NULL) THEN expression2
             *    ...
             *    ELSE expressionN
             * END
             */
            where.append( " AND COALESCE(machineAlias,machineName) in ("
                          + getQueryString( selectedMachines.getObject().toArray( new String[0] ) ) + ")" );
        }

        setMinMessageLevel();
        if( selectedLevels != null && selectedLevels.getObject().size() > 0
            && selectedLevels.getObject().size() < messageFilterDetails.getLevels().size() ) {

            where.append( " AND name in ("
                          + getQueryString( selectedLevels.getObject().toArray( new String[0] ) ) + ")" );
        }
        where.append( " AND " + id + "=" + idColumnValue );

        if( where.length() > 0 ) {
            where.delete( 0, " AND".length() );
            where.insert( 0, "WHERE" );
        }
        return where.toString();
    }

    private void setMinMessageLevel() {

        if( selectedLevels != null && selectedLevels.getObject().size() > 0 ) {
            int minLevelIndex = 10;
            for( String selectedLevel : selectedLevels.getObject() ) {
                int idx = MessageFilterDetails.SKIPPED_LOG_LEVELS.indexOf( selectedLevel );
                if( idx > -1 && minLevelIndex > idx ) {
                    minLevelIndex = idx;
                }
            }
            if( minLevelIndex < 10 ) {
                ( ( TestExplorerSession ) Session.get() ).setMinMessageLevel( MessageFilterDetails.SKIPPED_LOG_LEVELS.get( minLevelIndex ) );
            } else {
                // check if there is unchecked skipped level
                boolean hasOneOfTheSkippedLevels = false;
                for( String skippedLevel : MessageFilterDetails.SKIPPED_LOG_LEVELS ) {
                    if( messageFilterDetails.getLevels().contains( skippedLevel ) ) {
                        hasOneOfTheSkippedLevels = true;
                        break;
                    }
                }
                if( hasOneOfTheSkippedLevels ) {
                    ( ( TestExplorerSession ) Session.get() ).setMinMessageLevel( "info" );
                }
            }
        }
    }

    private String getQueryString(
                                   String[] array ) {

        StringBuilder sb = new StringBuilder();
        for( int i = 0; i < array.length; i++ ) {
            sb.append( "'" ).append( array[i] ).append( "'" );
            if( i < array.length - 1 ) {
                sb.append( ", " );
            }
        }
        return sb.toString();
    }
}
