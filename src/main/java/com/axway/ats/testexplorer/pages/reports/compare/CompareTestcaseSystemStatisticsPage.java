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
package com.axway.ats.testexplorer.pages.reports.compare;

import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.axway.ats.testexplorer.pages.LightweightBasePage;
import com.axway.ats.testexplorer.pages.testcase.charts.ChartsPanel;

public class CompareTestcaseSystemStatisticsPage extends LightweightBasePage {

    private static final long serialVersionUID = 1L;

    public CompareTestcaseSystemStatisticsPage( PageParameters parameters ) {

        super( parameters );

        String testcaseIds = extractParameter( parameters, "testcaseIds" );

        add( new ChartsPanel( "chartsPanel", testcaseIds, true ) );
    }

    @Override
    public String getPageName() {

        return "Compare System Statistics";
    }

    @Override
    public String getPageHeaderText() {

        return "Compare Testcase System Statistics";
    }
}
