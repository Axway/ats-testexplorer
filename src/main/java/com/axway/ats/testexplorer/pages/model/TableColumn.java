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

import java.io.Serializable;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

public class TableColumn implements Serializable {

    private static final long serialVersionUID = 1L;

    private String            columnId;

    private String            columnName;

    private String            parentTable;

    private int               columnPosition;

    private String            propertyExpression;

    private IModel<String>    tooltipModel;

    private String            sortProperty;

    private boolean           isEditable       = false;

    private boolean           visible          = true;

    private String            headerCssClass;

    private int               initialWidth     = -1;

    public TableColumn() {

    }

    public TableColumn( String columnName,
                        String parentTable,
                        int columnPosition,
                        int initialWidth,
                        boolean isVisible ) {

        this.columnName = columnName;
        this.columnPosition = columnPosition;
        this.parentTable = parentTable;
        this.initialWidth = initialWidth;
        this.visible = isVisible;
    }

    public TableColumn( String columnId,
                        String columnName,
                        String parentTable,
                        String sortProperty,
                        String propertyExpression,
                        boolean visible ) {

        this.columnId = columnId;
        this.columnName = columnName;
        this.parentTable = parentTable;
        this.sortProperty = sortProperty;
        this.propertyExpression = propertyExpression;
        this.visible = visible;
    }

    public TableColumn( String columnId,
                        String columnName,
                        String parentTable,
                        String sortProperty,
                        String propertyExpression,
                        boolean visible,
                        boolean isEditable ) {

        this(columnId, columnName, parentTable, sortProperty, propertyExpression, visible);
        this.isEditable = isEditable;
    }

    public TableColumn( String columnId,
                        String columnName,
                        String parentTable,
                        String sortProperty,
                        String propertyExpression,
                        String headerCssClass,
                        boolean visible,
                        boolean isEditable,
                        int initialWidth ) {

        this(columnId, columnName, parentTable, sortProperty, propertyExpression, visible, isEditable);
        this.headerCssClass = headerCssClass;
        this.initialWidth = initialWidth;
    }

    public TableColumn( String columnId,
                        String columnName,
                        String parentTable,
                        String sortProperty,
                        String propertyExpression,
                        String tooltip,
                        String headerCssClass,
                        boolean visible,
                        boolean isEditable,
                        int initialWidth ) {

        this(columnId, columnName, parentTable, sortProperty, propertyExpression, visible, isEditable);
        this.headerCssClass = headerCssClass;
        this.initialWidth = initialWidth;

        this.tooltipModel = new Model<String>(tooltip);
    }

    public String getParentTable() {

        return parentTable;
    }

    public void setParentTable(
                                String parentTable ) {

        this.parentTable = parentTable;
    }

    public int getColumnPosition() {

        return columnPosition;
    }

    public void setColumnPosition(
                                   int columnPosition ) {

        this.columnPosition = columnPosition;
    }

    public boolean isVisible() {

        return visible;
    }

    public void setVisible(
                            boolean visible ) {

        this.visible = visible;
    }

    public String getColumnId() {

        return columnId;
    }

    public void setColumnId(
                             String columnId ) {

        this.columnId = columnId;
    }

    public String getColumnName() {

        return columnName;
    }

    public void setColumnName(
                               String columnName ) {

        this.columnName = columnName;
    }

    public boolean isEditable() {

        return isEditable;
    }

    public void setEditable(
                             boolean isEditable ) {

        this.isEditable = isEditable;
    }

    public String getPropertyExpression() {

        return propertyExpression;
    }

    public void setPropertyExpression(
                                       String propertyExpression ) {

        this.propertyExpression = propertyExpression;
    }

    public String getSortProperty() {

        return sortProperty;
    }

    public void setSortProperty(
                                 String sortProperty ) {

        this.sortProperty = sortProperty;
    }

    public String getHeaderCssClass() {

        return headerCssClass;
    }

    public void setHeaderCssClass(
                                   String headerCssClass ) {

        this.headerCssClass = headerCssClass;
    }

    public int getInitialWidth() {

        return initialWidth;
    }

    public void setInitialWidth(
                                 int initialWidth ) {

        this.initialWidth = initialWidth;
    }

    @Override
    public String toString() {

        return "[" + this.columnName + ", " + this.visible + "]";
    }

    @Override
    public boolean equals(
                           Object obj ) {

        if (! (obj instanceof TableColumn)) {
            return false;
        }
        TableColumn columnObj = (TableColumn) obj;
        if (this.columnName == null || columnObj.columnName == null) {

            return false;
        } else if (this.parentTable == null || columnObj.parentTable == null) {

            return this.columnName.equals(columnObj.columnName);
        }
        return this.columnName.equals(columnObj.columnName)
               && this.parentTable.equals(columnObj.parentTable);
    }

    public IModel<String> getTooltip() {

        return tooltipModel;
    }

}
