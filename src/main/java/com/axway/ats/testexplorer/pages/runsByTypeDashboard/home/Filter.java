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
package com.axway.ats.testexplorer.pages.runsByTypeDashboard.home;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.ListMultipleChoice;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.util.ListModel;

import com.axway.ats.core.utils.StringUtils;
import com.axway.ats.log.autodb.exceptions.DatabaseAccessException;
import com.axway.ats.testexplorer.model.TestExplorerSession;
import com.axway.ats.testexplorer.pages.model.filtering.IFilter;

public class Filter extends Form<String> implements IFilter {

    private static final long          serialVersionUID = 1L;

    private DropDownChoice<String>     searchByProduct;
    private List<String>               productNames;
    private String                     selectedProductName;

    private ListMultipleChoice<String> searchByVersion;
    private List<String>               versionNames;
    private List<String>               selectedVersionNames;

    private static transient Logger    LOG              = Logger.getLogger( Filter.class );

    public Filter() {

        super( "filter" );

        TestExplorerSession session = ( TestExplorerSession ) Session.get();

        try {

            productNames = ( ArrayList<String> ) session.getDbReadConnection()
                                                        .getAllProductNames( "WHERE 1=1" );

            selectedProductName = null;

            searchByProduct = new DropDownChoice<String>( "search_by_product",
                                                          new PropertyModel<String>( this,
                                                                                     "selectedProductName" ),
                                                          productNames );

            searchByProduct.setNullValid( false );

            searchByProduct.setEscapeModelStrings( false );
            searchByProduct.setOutputMarkupId( true );

            searchByProduct.add( new OnChangeAjaxBehavior() {

                private static final long serialVersionUID = 1L;

                @Override
                protected void onUpdate( AjaxRequestTarget target ) {

                    TestExplorerSession session = ( TestExplorerSession ) Session.get();
                    try {

                        versionNames = session.getDbReadConnection()
                                              .getAllVersionNames( "WHERE productName = '"
                                                                   + selectedProductName + "'" );

                        selectedVersionNames = new ArrayList<String>( versionNames );

                        searchByVersion.getModel().setObject( selectedVersionNames );

                        searchByVersion.setChoices( versionNames );

                        target.add( searchByVersion );

                    } catch( DatabaseAccessException e ) {
                        error( "Unable to get version names" );
                        LOG.error( e );
                    }

                }

            } );

        } catch( DatabaseAccessException e ) {
            error( e.getMessage() );
            LOG.error( e );
        }

        versionNames = new ArrayList<String>( 1 );
        selectedVersionNames = new ArrayList<String>( 1 );

        searchByVersion = new ListMultipleChoice<String>( "search_by_version",
                                                          new ListModel<String>( selectedVersionNames ),
                                                          versionNames );

        searchByVersion.setEscapeModelStrings( false );
        searchByVersion.setOutputMarkupId( true );

        add( searchByProduct );
        add( searchByVersion );

        AjaxButton searchButton = new AjaxButton( "submit" ) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit( AjaxRequestTarget target, Form<?> form ) {

                List<String[]> productAndVersionNames = new ArrayList<String[]>( 1 );

                for( String versionName : selectedVersionNames ) {
                    productAndVersionNames.add( new String[]{ selectedProductName, versionName } );
                }

                TestExplorerSession session = ( TestExplorerSession ) Session.get();

                try {
                    new DashboardHomeUtils().callJavaScript( target,
                                                             new DashboardHomeUtils().initJsonData( productAndVersionNames,
                                                                                                    session.getDbReadConnection()
                                                                                                           .getAllBuildTypes( "AND 1=1" ) ) );
                } catch( DatabaseAccessException e ) {
                    error( "Unable to get runs data" );
                    LOG.error( "Unable to get runs data", e );
                }

            }

            @Override
            protected void onError( AjaxRequestTarget target, Form<?> form ) {

                super.onError( target, form );
            }

        };
        add( searchButton );
        // search button is the button to trigger when user hit the enter key
        this.setDefaultButton( searchButton );

        add( new AjaxButton( "clear" ) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit( AjaxRequestTarget target, Form<?> form ) {

                selectedProductName = null;

                selectedVersionNames = new ArrayList<String>( 1 );
                versionNames = new ArrayList<String>( 1 );

                searchByProduct.setModelObject( selectedProductName );

                searchByVersion.setModelObject( selectedVersionNames );
                searchByVersion.setChoices( new ListModel<String>( versionNames ) );

                target.add( searchByProduct );
                target.add( searchByVersion );

                target.appendJavaScript( ";$('#container').empty();" );

            }

        } );

    }

    public List<String[]> getSelectedProductAndVersionNames() {

        List<String[]> productAndVersionNames = new ArrayList<String[]>( 1 );

        for( String versionName : selectedVersionNames ) {
            productAndVersionNames.add( new String[]{ selectedProductName, versionName } );
        }

        return productAndVersionNames;
    }

    public void performSearchOnPageLoad() {

        if( productNames.size() == 0 ) {
            return;
        }

        selectedProductName = productNames.get( productNames.size() - 1 );
        searchByProduct.setModelObject( selectedProductName );

        TestExplorerSession session = ( TestExplorerSession ) Session.get();
        try {
            versionNames = session.getDbReadConnection()
                                  .getAllVersionNames( "WHERE productName = '" + selectedProductName + "'" );
            selectedVersionNames = new ArrayList<String>( versionNames );
            searchByVersion.getModel().setObject( selectedVersionNames );
            searchByVersion.setChoices( versionNames );

        } catch( DatabaseAccessException e ) {
            error( "Unable to perform initial search" );
            LOG.error( "Unable to perform initial search", e );
        }
    }
    
    @Override
    public boolean hasSelectedFields() {

        if( !StringUtils.isNullOrEmpty( selectedProductName ) ) {
            return true;
        }
        if( selectedVersionNames.size() > 0 ) {
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
    
}
