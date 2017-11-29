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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.wicket.Component;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import com.axway.ats.log.autodb.entities.Scenario;
import com.inmethod.grid.column.WicketColumnAdapter;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class ScenarioDescriptionColumn extends WicketColumnAdapter {

    private static final long    serialVersionUID       = 1L;

    private static final Pattern wikiUrlPattern         = Pattern.compile( "\\[([^\\|]+)\\|\\s*((?:http|https)\\:\\/\\/[^\\]]+)\\]" );
    private static final Pattern quotedUrlPattern       = Pattern.compile( "(?<!(?:href\\=))\\\"((?:http|https)\\:\\/\\/[^\\\"]+)\\\"" );
    private static final Pattern withoutSpaceUrlPattern = Pattern.compile( "(?<!(?:href\\=\\\"|>))((?:http|https)\\:\\/\\/[^\\s]+)" );

    private TableColumn          tableColumn;

    public ScenarioDescriptionColumn( TableColumn tableColumn ) {

        super( tableColumn.getColumnId(), new PropertyColumn( new PropertyModel<TableColumn>( tableColumn,
                                                                                              "columnName" ),
                                                              tableColumn.getSortProperty(),
                                                              tableColumn.getPropertyExpression() ) );
        this.tableColumn = tableColumn;
    }

    @Override
    public Component newCell(
                              WebMarkupContainer parent,
                              String componentId,
                              IModel rowModel ) {

        final Scenario scenario = ( Scenario ) rowModel.getObject();
        String description = scenario.description;
        if( description == null ) {
            description = "";
        }
        IModel<String> labelModel = new Model<String>( description );
        Label label = new Label( componentId, labelModel );
        if( description.length() > 0 && description.contains( "://" ) ) {

            description = description.replace( "<", "&lt;" ).replace( ">", "&gt;" );

            // search for links with wiki syntax eg. [Link text|http://some.rul.com/test.html]
            StringBuffer sb = new StringBuffer( description.length() );
            Matcher wikiUrlMatcher = wikiUrlPattern.matcher( description );
            while( wikiUrlMatcher.find() ) {
                wikiUrlMatcher.appendReplacement( sb, "<a href=\"$2\" class=\"linkInTable\">$1</a>" );
            }
            wikiUrlMatcher.appendTail( sb );
            description = sb.toString();

            // search for quoted links eg. "http://some.rul.com/read me/test spaces.html"
            sb = new StringBuffer( description.length() );
            Matcher quotedUrlMatcher = quotedUrlPattern.matcher( description );
            while( quotedUrlMatcher.find() ) {
                quotedUrlMatcher.appendReplacement( sb, "<a href=\"$1\" class=\"linkInTable\">$1</a>" );
            }
            quotedUrlMatcher.appendTail( sb );
            description = sb.toString();

            // search for regular links(without spaces) eg. http://some.rul.com/read_me.txt
            sb = new StringBuffer( description.length() );
            Matcher withoutSpaceMatcher = withoutSpaceUrlPattern.matcher( description );
            while( withoutSpaceMatcher.find() ) {
                withoutSpaceMatcher.appendReplacement( sb, "<a href=\"$1\" class=\"linkInTable\">$1</a>" );
            }
            withoutSpaceMatcher.appendTail( sb );

            labelModel.setObject( sb.toString() );
            label.setEscapeModelStrings( false );
        }

        return label;
    }

    @Override
    public String getHeaderCssClass() {

        return this.tableColumn.getHeaderCssClass();
    }

}
