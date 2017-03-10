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

import java.util.List;

import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.axway.ats.log.autodb.exceptions.DatabaseAccessException;
import com.axway.ats.testexplorer.model.TestExplorerSession;
import com.axway.ats.testexplorer.pages.BasePage;
import com.axway.ats.testexplorer.pages.WelcomePage;

public class RunsByTypeDashboardHomePage extends BasePage {

    private static final long serialVersionUID = 1L;

    private List<String>      jsonDatas;
    
    private Filter            filter;

    public RunsByTypeDashboardHomePage( PageParameters parameters ) {

        super( parameters );

        addNavigationLink( WelcomePage.class, new PageParameters(), "Home", null );

        filter = new Filter();

        add( filter );

        filter.performSearchOnPageLoad();

        AjaxLink<String> modalTooltip = new AjaxLink<String>( "modalTooltip" ) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(
                                 AjaxRequestTarget target ) {

            }
        };
        //        modalTooltip.
        modalTooltip.add( new WebMarkupContainer( "helpButton" ) );

        add( modalTooltip );

        TestExplorerSession session = ( TestExplorerSession ) Session.get();

        try {
            jsonDatas = new DashboardHomeUtils().initJsonData( filter.getSelectedProductAndVersionNames(),
                                                               session.getDbReadConnection()
                                                                      .getAllBuildTypes( "AND 1=1" ) );
        } catch( DatabaseAccessException e ) {
            error( "Unable to perform initial search" );
            LOG.error( "Unable to perform initial search", e );
        }

    }

    @Override
    public void renderHead(
                            IHeaderResponse response ) {

        new DashboardHomeUtils().callJavaScript( response, jsonDatas );

    }

    @Override
    public String getPageName() {

        return "Runs by type";
    }

}
