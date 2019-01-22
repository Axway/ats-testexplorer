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
package com.axway.ats.testexplorer.pages.testcasesByGroups;

import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnLoadHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.axway.ats.log.autodb.exceptions.DatabaseAccessException;
import com.axway.ats.testexplorer.model.TestExplorerSession;
import com.axway.ats.testexplorer.pages.BasePage;
import com.axway.ats.testexplorer.pages.WelcomePage;

public class TestcasesByGroupsPage extends BasePage {

    private static final long                serialVersionUID        = 1L;

    private String                           treemapData;

    private String                           testcasesIdsMap;

    private String                           filterData;

    private boolean                          initialSearchSuccessful = false;

    private transient TestcasesByGroupFilter filter;

    public TestcasesByGroupsPage( PageParameters parameters ) {

        super(parameters);

        addNavigationLink(WelcomePage.class, new PageParameters(), "Home", null);

        AjaxLink<String> modalTooltip = new AjaxLink<String>("modalTooltip") {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(
                                 AjaxRequestTarget target ) {
                
            }
        };
        //modalTooltip.
        modalTooltip.add(new WebMarkupContainer("helpButton"));

        add(modalTooltip);

        filter = new TestcasesByGroupFilter("searchForm");

        add(filter);

        initialSearchSuccessful = filter.performSearchOnPageLoad();

        if (!initialSearchSuccessful) {
            return;
        }

        filterData = filter.getFilterData();

        TestExplorerSession session = (TestExplorerSession) Session.get();
        TestcaseInfoPerGroupStorage perGroupStorage = null;
        try {
            perGroupStorage = session.getDbReadConnection()
                                     .getTestcaseInfoPerGroupStorage(filter.getSelectedProductName(),
                                                                     filter.getSelectedVersionNames(),
                                                                     filter.getSelectedGroupNames(),
                                                                     filter.getSearchByAfterDate(),
                                                                     filter.getSearchByBeforeDate(),
                                                                     filter.getSearchByGroupContains());

        } catch (DatabaseAccessException e) {
            LOG.error("Unable to get Testcases and Groups data", e);
            error("Unable to get Testcases and Groups data");
        }

        if (perGroupStorage != null) {
            treemapData = perGroupStorage.generateTreemapData();
            testcasesIdsMap = perGroupStorage.generateTestcasesIdsMap();
        }

    }

    @Override
    public void renderHead(
                            IHeaderResponse response ) {

        super.renderHead(response);

        if (!initialSearchSuccessful) {
            return;
        }

        TestExplorerSession session = (TestExplorerSession) Session.get();

        String initScript = ";setHiddenValue(\"groups\");drawTreemap(" + treemapData + ","
                            + TestcaseInfoPerGroupStorage.TREEMAP_OPTIONS + ");populateFilterDataPanel("
                            + filterData + ");setDbName(\"" + (session.getDbName())
                            + "\");setTestcasesIdsMap(" + testcasesIdsMap + ");";

        response.render(OnLoadHeaderItem.forScript(initScript));

    }

    @Override
    public String getPageName() {

        return "Testcases by Groups";
    }

}
