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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.datetime.markup.html.form.DateTextField;
import org.apache.wicket.extensions.yui.calendar.DatePicker;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.ListMultipleChoice;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.validation.validator.DateValidator;

import com.axway.ats.core.utils.StringUtils;
import com.axway.ats.log.autodb.exceptions.DatabaseAccessException;
import com.axway.ats.testexplorer.model.TestExplorerSession;
import com.axway.ats.testexplorer.pages.model.filtering.IFilter;

public class TestcasesByGroupFilter extends Form<String> implements IFilter {

    private static final long          serialVersionUID                = 1L;

    private static final String        TOO_MANY_TESTCASES_WARN_MESSAGE = "There are too many testcases. "
                                                                         + "Please increase the heap of the web application before requesting any report on that page. Currently the heap size is around %s MB";

    private DropDownChoice<String>     searchByProduct;
    private List<String>               productNames;
    private String                     selectedProductName;

    private ListMultipleChoice<String> searchByVersion;
    private List<String>               selectedVersionNames            = new ArrayList<String>();
    private List<String>               versionNames                    = new ArrayList<String>();

    private ListMultipleChoice<String> searchByAllGroups;
    private List<String>               selectedGroupNames;
    private List<String>               groupNames;

    private DateTextField              searchByAfterDate               = DateTextField.forDatePattern("search_by_after_date",
                                                                                                      new Model<Date>(),
                                                                                                      "dd.MM.yyyy");
    private DateTextField              searchByBeforeDate              = DateTextField.forDatePattern("search_by_before_date",
                                                                                                      new Model<Date>(),
                                                                                                      "dd.MM.yyyy");

    private TextField<String>          searchByGroupContains;

    private static Logger              LOG                             = Logger.getLogger(TestcasesByGroupFilter.class);

    private boolean                    hasTooManyTestcases             = false;

    public TestcasesByGroupFilter( String id ) {

        super(id);

        searchByProduct = createSearchByProductComponent();
        searchByVersion = createSearchByVersionComponent();
        searchByAllGroups = createSearchByAllGroupsComponent();
        searchByGroupContains = new TextField<String>("search_by_group_contains", new Model<String>(""));
        searchByGroupContains.add(new OnChangeAjaxBehavior() {

            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate( AjaxRequestTarget target ) {

                if (StringUtils.isNullOrEmpty(searchByGroupContains.getModel().getObject())) {
                    TestExplorerSession session = (TestExplorerSession) Session.get();
                    try {
                        groupNames = session.getDbReadConnection().getAllGroupNames(selectedProductName,
                                                                                    selectedVersionNames);
                    } catch (DatabaseAccessException e) {
                        LOG.error("Unable to get all group names", e);
                        error("Unable to get all group names");
                    }
                } else {
                    groupNames = new ArrayList<String>();
                }

                selectedGroupNames = groupNames;
                searchByAllGroups.getModel().setObject(selectedGroupNames);
                searchByAllGroups.setChoices(groupNames);
                target.add(searchByAllGroups);
            }
        });

        searchByAfterDate.setOutputMarkupId(true);
        searchByAfterDate.add(DateValidator.maximum(new Date(), "dd.MM.yyyy"));

        searchByBeforeDate.setOutputMarkupId(true);

        searchByGroupContains.setEscapeModelStrings(false);
        searchByGroupContains.setOutputMarkupId(true);

        add(searchByProduct);
        add(searchByVersion);
        add(searchByAllGroups);
        add(searchByAfterDate);
        add(searchByBeforeDate);
        add(searchByGroupContains);

        searchByAfterDate.add(new DatePicker().setShowOnFieldClick(true).setAutoHide(true));
        searchByBeforeDate.add(new DatePicker().setShowOnFieldClick(true).setAutoHide(true));

        AjaxButton searchButton = new AjaxButton("submit") {

            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit( AjaxRequestTarget target, Form<?> form ) {

                if (StringUtils.isNullOrEmpty(selectedProductName) && selectedVersionNames.size() == 0
                    && selectedGroupNames.size() == 0
                    && StringUtils.isNullOrEmpty(searchByGroupContains.getModel().getObject())
                    && StringUtils.isNullOrEmpty(searchByAfterDate.getInput())
                    && StringUtils.isNullOrEmpty(searchByBeforeDate.getInput())) {
                    return;
                }

                TestExplorerSession session = (TestExplorerSession) Session.get();
                TestcaseInfoPerGroupStorage perGroupStorage = null;
                try {
                    perGroupStorage = session.getDbReadConnection()
                                             .getTestcaseInfoPerGroupStorage(selectedProductName,
                                                                             selectedVersionNames,
                                                                             selectedGroupNames,
                                                                             searchByAfterDate.getValue(),
                                                                             searchByBeforeDate.getValue(),
                                                                             searchByGroupContains.getModel()
                                                                                                  .getObject());
                } catch (DatabaseAccessException e) {
                    LOG.error("Unable to get Testcases and groups data", e);
                    error("Unable to get Testcases and groups data");
                }

                if (perGroupStorage != null) {
                    String treemapData = perGroupStorage.generateTreemapData();

                    String testcasesIdsMap = perGroupStorage.generateTestcasesIdsMap();

                    String script = ";setHiddenValue(\"groups\");drawTreemap(" + treemapData + ","
                                    + TestcaseInfoPerGroupStorage.TREEMAP_OPTIONS
                                    + ");$('.filterHeader').click();populateFilterDataPanel("
                                    + getFilterData() + ");setTestcasesIdsMap(" + testcasesIdsMap + ");";

                    target.appendJavaScript(script);
                }
            }

            @Override
            protected void onError( AjaxRequestTarget target, Form<?> form ) {

                super.onError(target, form);
            }

        };
        add(searchButton);
        // search button is the button to trigger when user hit the enter key
        this.setDefaultButton(searchButton);

        add(new AjaxButton("clear") {

            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit( AjaxRequestTarget target, Form<?> form ) {

                selectedProductName = null;

                selectedVersionNames = new ArrayList<String>(1);
                versionNames = new ArrayList<String>(1);

                selectedGroupNames = new ArrayList<String>(1);
                groupNames = new ArrayList<String>(1);

                searchByProduct.getModel().setObject(selectedProductName);

                searchByVersion.getModel().setObject(selectedVersionNames);
                searchByVersion.setChoices(new ListModel<String>(versionNames));

                searchByAllGroups.getModel().setObject(selectedGroupNames);
                searchByAllGroups.setChoices(new ListModel<String>(groupNames));

                searchByAfterDate.getModel().setObject(null);
                searchByAfterDate.clearInput();

                searchByBeforeDate.clearInput();
                searchByBeforeDate.getModel().setObject(null);

                searchByGroupContains.getModel().setObject(null);

                target.add(searchByProduct);
                target.add(searchByVersion);
                target.add(searchByAllGroups);
                target.add(searchByAfterDate);
                target.add(searchByBeforeDate);
                target.add(searchByGroupContains);

                target.appendJavaScript(";$('#chart_div').empty();populateFilterDataPanel(" + getFilterData()
                                        + ");");
            }
        });
    }

    public String getSelectedProductName() {

        return selectedProductName;
    }

    public List<String> getSelectedVersionNames() {

        return selectedVersionNames;
    }

    public List<String> getSelectedGroupNames() {

        return selectedGroupNames;
    }

    public String getSearchByAfterDate() {

        return searchByAfterDate.getValue();
    }

    public String getSearchByBeforeDate() {

        return searchByBeforeDate.getValue();
    }

    public String getSearchByGroupContains() {

        return searchByGroupContains.getModel().getObject();
    }

    public String getFilterData() {

        StringBuilder sb = new StringBuilder();

        String productName = selectedProductName;
        String versionNames = null;
        String groupNames = null;
        String groupContains = (searchByGroupContains.getModelObject() == null)
                                                                                ? null
                                                                                : searchByGroupContains.getModel()
                                                                                                       .getObject();

        String startedAfterDate = (searchByAfterDate.getModelObject() == null)
                                                                               ? null
                                                                               : (searchByAfterDate.getInput() == null)
                                                                                                                        ? searchByAfterDate.getModelObject()
                                                                                                                                           .toString()
                                                                                                                        : searchByAfterDate.getInput();
        String startedBeforeDate = (searchByBeforeDate.getModelObject() == null)
                                                                                 ? null
                                                                                 : searchByBeforeDate.getInput();

        if (selectedVersionNames.size() > 0) {
            StringBuilder versionNameStringBuilder = new StringBuilder();

            for (int i = 0; i < selectedVersionNames.size(); i++) {
                versionNameStringBuilder.append(selectedVersionNames.get(i));
                if (i < selectedVersionNames.size() - 1) {
                    versionNameStringBuilder.append(", ");
                }
            }
            versionNames = versionNameStringBuilder.toString();
        }

        if (selectedGroupNames.size() > 0) {
            StringBuilder groupNameStringBuilder = new StringBuilder();

            for (int i = 0; i < selectedGroupNames.size(); i++) {
                groupNameStringBuilder.append(selectedGroupNames.get(i));
                if (i < selectedGroupNames.size() - 1) {
                    groupNameStringBuilder.append(", ");
                }
            }
            groupNames = groupNameStringBuilder.toString();
        }

        if (!StringUtils.isNullOrEmpty(groupContains)) {
            sb.append("{")
              .append("'ProductName':'" + productName + "',")
              .append("'VersionNames':'" + versionNames + "',")
              .append("'GroupNames':'" + null + "',")
              .append("'GroupContains':'" + groupContains + "',")
              .append("'StartedAfter':'" + startedAfterDate + "',")
              .append("'StartedBefore':'" + startedBeforeDate + "'")
              .append("}");
        } else {
            sb.append("{")
              .append("'ProductName':'" + productName + "',")
              .append("'VersionNames':'" + versionNames + "',")
              .append("'GroupNames':'" + groupNames + "',")
              .append("'GroupContains':'" + null + "',")
              .append("'StartedAfter':'" + startedAfterDate + "',")
              .append("'StartedBefore':'" + startedBeforeDate + "'")
              .append("}");
        }

        return sb.toString();
    }

    private ListMultipleChoice<String> createSearchByVersionComponent() {

        versionNames = new ArrayList<String>(1);
        selectedVersionNames = new ArrayList<String>(1);

        searchByVersion = new ListMultipleChoice<String>("search_by_version",
                                                         new ListModel<String>(selectedVersionNames),
                                                         versionNames);
        searchByVersion.setEscapeModelStrings(false);
        searchByVersion.setOutputMarkupId(true);
        searchByVersion.add(new OnChangeAjaxBehavior() {

            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate( AjaxRequestTarget target ) {

                TestExplorerSession session = (TestExplorerSession) Session.get();
                try {

                    groupNames = session.getDbReadConnection().getAllGroupNames(selectedProductName,
                                                                                selectedVersionNames);

                    selectedGroupNames = new ArrayList<String>(groupNames);
                    searchByAllGroups.setChoices(groupNames);
                    searchByAllGroups.getModel().setObject(selectedGroupNames);
                    target.add(searchByAllGroups);
                } catch (DatabaseAccessException e) {
                    error("Unable to get group names");
                }
            }
        });

        return searchByVersion;
    }

    private ListMultipleChoice<String> createSearchByAllGroupsComponent() {

        groupNames = new ArrayList<String>(1);
        selectedGroupNames = new ArrayList<String>(1);

        searchByAllGroups = new ListMultipleChoice<String>("search_by_all_groups",
                                                           new ListModel<String>(selectedGroupNames),
                                                           groupNames);

        searchByAllGroups.setEscapeModelStrings(false);
        searchByAllGroups.setOutputMarkupId(true);
        searchByAllGroups.add(new OnChangeAjaxBehavior() {

            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate( AjaxRequestTarget target ) {

                TestExplorerSession session = (TestExplorerSession) Session.get();
                try {

                    groupNames = session.getDbReadConnection().getAllGroupNames(selectedProductName,
                                                                                selectedVersionNames);

                    //selectedGroupNames = new ArrayList<String>( groupNames );
                    searchByAllGroups.setChoices(groupNames);
                    searchByAllGroups.getModel().setObject(selectedGroupNames);
                    target.add(searchByAllGroups);
                } catch (DatabaseAccessException e) {
                    error("Unable to get group names");
                }

                searchByGroupContains.setModelObject(null);
                target.add(searchByGroupContains);
            }
        });

        return searchByAllGroups;
    }

    private DropDownChoice<String> createSearchByProductComponent() {

        TestExplorerSession session = (TestExplorerSession) Session.get();

        try {
            productNames = (ArrayList<String>) session.getDbReadConnection()
                                                      .getAllProductNames("WHERE 1=1");

            selectedProductName = null;

            searchByProduct = new DropDownChoice<String>("search_by_product",
                                                         new PropertyModel<String>(this,
                                                                                   "selectedProductName"),
                                                         productNames);
            searchByProduct.setNullValid(false);
            searchByProduct.setEscapeModelStrings(false);
            searchByProduct.setOutputMarkupId(true);
            searchByProduct.add(new OnChangeAjaxBehavior() {

                private static final long serialVersionUID = 1L;

                @Override
                protected void onUpdate( AjaxRequestTarget target ) {

                    TestExplorerSession session = (TestExplorerSession) Session.get();
                    try {
                        versionNames = session.getDbReadConnection()
                                              .getAllVersionNames("WHERE productName = '"
                                                                  + selectedProductName + "'");

                        selectedVersionNames = new ArrayList<String>(1);
                        searchByVersion.getModel().setObject(selectedVersionNames);

                        searchByVersion.setChoices(versionNames);
                        target.add(searchByVersion);

                        groupNames = new ArrayList<String>(1);
                        selectedGroupNames = new ArrayList<String>(1);
                        searchByAllGroups.getModel().setObject(selectedGroupNames);
                        searchByAllGroups.setChoices(groupNames);
                        target.add(searchByAllGroups);
                    } catch (DatabaseAccessException e) {
                        error("Unable to get version names");
                    }
                }
            });
        } catch (DatabaseAccessException e) {
            error(e.getMessage());
        }
        return searchByProduct;
    }

    /**
     * Performs initial search via the default search query
     * @return true if the search is successful, false otherwise
     * */
    public boolean performSearchOnPageLoad() {

        if (productNames == null || productNames.size() == 0) {
            return false;
        }

        // Since ATS needs to execute queries over all of the database's data
        // this can result in OOM exceptions.
        // So check how much test data is in the DB and log a warning if it is too much
        // What means too much, it depends, but ATS says testcases count >= 1k
        try {
            if (!isFreeHeapSpaceEnough()) {
                hasTooManyTestcases = true;
                LOG.warn(getLowHeapMemoryMessage());
                return false;
            }
        } catch (Exception e) {
            error("Unable to get testcases count");
            LOG.error("Unable to get testcases count", e);
            return false;
        }

        selectedProductName = productNames.get(productNames.size() - 1);
        searchByProduct.getModel().setObject(selectedProductName);

        TestExplorerSession session = (TestExplorerSession) Session.get();
        try {

            versionNames = session.getDbReadConnection()
                                  .getAllVersionNames("WHERE productName = '" + selectedProductName + "'");
            selectedVersionNames = Arrays.asList(versionNames.get(versionNames.size() - 1));
            searchByVersion.getModel().setObject(selectedVersionNames);
            searchByVersion.setChoices(versionNames);

            groupNames = session.getDbReadConnection().getAllGroupNames(selectedProductName, selectedVersionNames);
            selectedGroupNames = groupNames;

            searchByAllGroups.getModel().setObject(selectedGroupNames);
            searchByAllGroups.setChoices(new ListModel<String>(groupNames));

            String[] lastRunDateStart = session.getDbReadConnection()
                                               .getRuns(0, 1, "WHERE 1=1", "dateStart", false,
                                                        ((TestExplorerSession) Session.get()).getTimeOffset())
                                               .get(0)
                                               .getDateStartLong()
                                               .split(" ")[0].split("-");

            searchByAfterDate.getModel()
                             .setObject(new SimpleDateFormat("dd.MM.yyyy").parse(lastRunDateStart[2] + "."
                                                                                 + lastRunDateStart[1]
                                                                                 + "."
                                                                                 + lastRunDateStart[0]));
        } catch (DatabaseAccessException e) {
            error("Unable to perform initial search");
            LOG.error("Unable to perform initial search", e);
            return false;
        } catch (ParseException e) {
            error("Unable to parse date start for last run");
            LOG.error("Unable to parse date start for last run", e);
            return false;
        }

        return true;
    }

    private String getLowHeapMemoryMessage() {

        long maxHeapSize = Runtime.getRuntime().maxMemory(); // return the max heap size in bytes
        if (maxHeapSize / (1000 * 1000) <= 0) {
            maxHeapSize = 1; // The heap is around 1MB and the rounding errors will show it like it is 0 (zero). So set it to 1 (MB) instead
        } else {
            maxHeapSize /= (1000 * 1000); // convert it to MB
        }

        return String.format(TOO_MANY_TESTCASES_WARN_MESSAGE, maxHeapSize);
    }

    /**
     * Checks if the remaining heap size is enough to serve the upcoming request.</br>
     * Approximately one testcase treemap data consumes around 750 bytes of data.
     * 
     * @return true if the free heap space is enough, false other wise
     * @throws DatabaseAccessException 
     * */
    private boolean isFreeHeapSpaceEnough() throws DatabaseAccessException {

        long heapFreeSize = Runtime.getRuntime().freeMemory();

        TestExplorerSession session = (TestExplorerSession) Session.get();
        int count = session.getDbReadConnection().getTestcasesCount("WHERE 1=1");

        long neededSize = count * 750;

        return heapFreeSize > neededSize;

    }

    @Override
    public boolean hasSelectedFields() {

        if (!StringUtils.isNullOrEmpty(selectedProductName)) {
            return true;
        }
        if (selectedVersionNames.size() > 0) {
            return true;
        }
        if (selectedGroupNames.size() > 0) {
            return true;
        }
        if (!StringUtils.isNullOrEmpty(searchByGroupContains.getModelObject())) {
            return true;
        }
        if (searchByAfterDate.getModelObject() != null) {
            return true;
        }
        if (searchByBeforeDate.getModelObject() != null) {
            return true;
        }

        return false;
    }

    @Override
    public void renderHead( IHeaderResponse response ) {

        super.renderHead(response);
        StringBuilder jsScript = new StringBuilder();
        // we want to always show the filter expanded
        jsScript.append("$('.filterHeader').click()");
        if (hasTooManyTestcases) {
            long maxHeapSize = Runtime.getRuntime().maxMemory();
            if (maxHeapSize / (1000 * 1000) >= 0) {
                maxHeapSize = 1; // The heap is around 1MB
            } else {
                maxHeapSize /= (1000 * 1000);
            }
            jsScript.append(";alert('" + getLowHeapMemoryMessage() + "')");
            hasTooManyTestcases = false; // clear the flag so we pop the alert only once
        }
        response.render(OnDomReadyHeaderItem.forScript(jsScript.toString()));

    }

}
