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
package com.axway.ats.testexplorer.pages.model;

import java.util.List;

import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.extensions.markup.html.tabs.TabbedPanel;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.request.mapper.parameter.PageParameters;

public class BookmarkableTabbedPanel extends TabbedPanel<ITab> {

    private static final long serialVersionUID = 1L;

    private PageParameters    pageParameters;

    private String            tabParameterName = "tab";

    private int               defaultTabIndex  = 0;

    /**
    * Using this constructor the following defaults take effect:
    * <ul>
    * <li>tabParameterName = "tab"</li>
    * <li>defaultTabIndex = 0</li>
    * </ul>
    * @param id component id
    * @param tabs list of ITab objects used to represent tabs
    * @param pageParameters Container for parameters to a requested page. A
    *     parameter for the selected tab will be inserted.
    */
    public BookmarkableTabbedPanel( String id,
                                    List<ITab> tabs,
                                    PageParameters pageParameters ) {

        super(id, tabs);
        this.pageParameters = pageParameters;

        if (pageParameters.getNamedKeys().contains(tabParameterName)) {

            int tab = pageParameters.get(tabParameterName).toInt();
            setSelectedTab(tab);
        } else {
            setSelectedTab(defaultTabIndex);
        }
    }

    /**
    * @param id component id
    * @param tabs list of ITab objects used to represent tabs
    * @param defaultTabIndex Set the tab to by displayed by default. The url
    * for this tab will not contain any tab specific information. If you want to
    * display the first tab by default, you can use the constructor without this
    * parameter.
    * @param tabParameterName name
    * @param pageParameters Container for parameters to a requested page. A
    * parameter for the selected tab will be inserted.
    */
    public BookmarkableTabbedPanel( String id,
                                    List<ITab> tabs,
                                    int defaultTabIndex,
                                    String tabParameterName,
                                    PageParameters pageParameters ) {

        this(id, tabs, pageParameters);
        this.defaultTabIndex = defaultTabIndex;
        setSelectedTab(defaultTabIndex);
        this.tabParameterName = tabParameterName;
    }

    @Override
    protected WebMarkupContainer newLink(
                                          String linkId,
                                          int index ) {

        PageParameters newPageParameters = new PageParameters(pageParameters);
        if (index == defaultTabIndex) {
            newPageParameters.remove(tabParameterName);
        } else {
            newPageParameters.set(tabParameterName, index);
        }

        WebMarkupContainer link = new BookmarkablePageLink<Object>(linkId,
                                                                   getPage().getClass(),
                                                                   newPageParameters);
        return link;
    }

}
