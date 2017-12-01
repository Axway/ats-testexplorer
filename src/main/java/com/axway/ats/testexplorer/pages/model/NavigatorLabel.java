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

import org.apache.wicket.util.io.IClusterable;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;

import com.inmethod.grid.datagrid.DataGrid;

/**
 * Label that provides Showing x to y of z message given for a DataGrid. The message can be
 * overridden using the <code>NavigatorLabel</code> property key, the default message is used is
 * of the format <code>Showing ${from} to ${to} of ${of}</code>. The message can also be
 * configured pragmatically by setting it as the model object of the label.
 *
 */
@SuppressWarnings( { "rawtypes", "unchecked" })
public class NavigatorLabel extends Label {

    private static final long serialVersionUID = 1L;

    /**
     * @param id
     *            component id
     * @param table
     *            pageable view
     */
    public NavigatorLabel( final String id,
                           final DataGrid table,
                           String whatIsShowing ) {

        super(id);
        if (whatIsShowing == null) {
            whatIsShowing = "";
        }
        StringResourceModel stringResourceMode = new StringResourceModel("NavigatorLabel",
                                                                         this,
                                                                         new Model(new LabelModelObject(table)));
        stringResourceMode.setParameters(whatIsShowing + " from ${from} to ${to} (of ${of})");

        setDefaultModel(stringResourceMode);
    }

    @SuppressWarnings( "unused")
    private class LabelModelObject implements IClusterable {

        private static final long serialVersionUID = 1L;

        private final DataGrid    table;

        /**
         * Construct.
         *
         * @param table
         */
        public LabelModelObject( DataGrid table ) {

            this.table = table;
        }

        /**
         * @return "z" in "Showing x to y of z"
         */
        public String getOf() {

            int total = (int) table.getTotalRowCount();
            return total != -1
                               ? "" + total
                               : getString("unknown", null, "unknown");
        }

        /**
         * @return "x" in "Showing x to y of z"
         */
        public int getFrom() {

            if (table.getTotalRowCount() == 0) {
                return 0;
            }
            return (int) ( (table.getCurrentPage() * table.getRowsPerPage()) + 1);
        }

        /**
         * @return "y" in "Showing x to y of z"
         */
        public int getTo() {

            if (table.getTotalRowCount() == 0) {
                return 0;
            } else {
                return (int) (getFrom() + table.getCurrentPageItemCount() - 1);
            }
        }

    }
}
