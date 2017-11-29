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

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxCallListener;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.resource.PackageResourceReference;

import com.axway.ats.log.autodb.exceptions.DatabaseAccessException;
import com.axway.ats.testexplorer.TestExplorerApplication;
import com.axway.ats.testexplorer.model.TestExplorerSession;
import com.axway.ats.testexplorer.model.db.TestExplorerDbWriteAccessInterface;
import com.inmethod.grid.datagrid.DataGrid;

public class ColumnsDialog extends WebMarkupContainer {

    private static Logger     LOG              = Logger.getLogger( ColumnsDialog.class );

    private static final long serialVersionUID = 1L;

    private boolean           clickBkgToClose  = false;

    private List<TableColumn> dbColumnDefinitions;

    @SuppressWarnings({ "rawtypes" })
    public ColumnsDialog( String id,
                          final DataGrid grid,
                          List<TableColumn> columnDefinitions ) {

        super( id );
        setOutputMarkupId( true );

        this.dbColumnDefinitions = columnDefinitions;

        DataView<TableColumn> table = new DataView<TableColumn>( "headers",
                                                                 new ListDataProvider<TableColumn>( dbColumnDefinitions ),
                                                                 100 ) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(
                                         final Item<TableColumn> item ) {

                final TableColumn column = item.getModelObject();

                item.add( new CheckBox( "visible", new PropertyModel<Boolean>( column, "visible" ) ) );
                item.add( new Label( "columnName", new PropertyModel<String>( column, "columnName" ) ) );

                item.add( new AjaxEventBehavior( "click" ) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void onEvent(
                                            AjaxRequestTarget target ) {

                        TableColumn tableColumn = ( TableColumn ) this.getComponent().getDefaultModelObject();
                        tableColumn.setVisible( !tableColumn.isVisible() );

                        if( tableColumn.isVisible() ) {
                            item.add( AttributeModifier.replace( "class", "selected" ) );
                        } else {
                            item.add( AttributeModifier.replace( "class", "notSelected" ) );
                        }
                        grid.getColumnState().setColumnVisibility( tableColumn.getColumnId(),
                                                                   tableColumn.isVisible() );
                        target.add( grid );
                        target.add( this.getComponent() );

                        open( target );
                    }
                } );
                item.setOutputMarkupId( true );

                if( column.isVisible() ) {
                    item.add( AttributeModifier.replace( "class", "selected" ) );
                } else {
                    item.add( AttributeModifier.replace( "class", "notSelected" ) );
                }
            }
        };
        add( table );

        final Form<Void> columnDefinitionsForm = new Form<Void>( "columnDefinitionsForm" );
        add( columnDefinitionsForm );

        AjaxSubmitLink saveButton = new AjaxSubmitLink( "saveButton", columnDefinitionsForm ) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void updateAjaxAttributes(
                                                 AjaxRequestAttributes attributes ) {

                super.updateAjaxAttributes( attributes );
                AjaxCallListener ajaxCallListener = new AjaxCallListener();
                ajaxCallListener.onPrecondition( "getTableColumnDefinitions(); " );
                attributes.getAjaxCallListeners().add( ajaxCallListener );
            }

            @Override
            protected void onSubmit(
                                     AjaxRequestTarget target,
                                     Form<?> form ) {

                String columnDefinitionsString = form.getRequest()
                                                     .getPostParameters()
                                                     .getParameterValue( "columnDefinitions" )
                                                     .toString();

                List<TableColumn> jsColDefinitions = asList( columnDefinitionsString );
                orderTableColumns( dbColumnDefinitions, jsColDefinitions );

                try {
                    saveColumnDefinitionsToDb( jsColDefinitions );

                    modifyDBColumnDefinitionList( jsColDefinitions );

                } catch( DatabaseAccessException dae ) {
                    throw new RuntimeException( "Unable to save table Column definitions in db: "
                                                + ( ( TestExplorerSession ) Session.get() ).getDbName(), dae );
                } catch( SQLException sqle ) {
                    throw new RuntimeException( "Unable to save table Column definitions in db: "
                                                + ( ( TestExplorerSession ) Session.get() ).getDbName(), sqle );
                }

                close( target );
            }
        };
        add( AttributeModifier.append( "class", "runsTableColDialogDivWrapper" ) );
        columnDefinitionsForm.add( saveButton );

        add( new Behavior() {

            private static final long serialVersionUID = 1L;

            @Override
            public void renderHead(
                                    Component component,
                                    IHeaderResponse response ) {

                if( autoAddToHeader() ) {

                    String script = "jQuery.fn.center=function(){" + "this.css(\"position\",\"absolute\");"
                                    + "this.css(\"top\",(jQuery(window).height()-this.height())/2+jQuery(window).scrollTop()+\"px\");"
                                    + "this.css(\"left\",(jQuery(window).width()-this.width())/2+jQuery(window).scrollLeft()+\"px\");"
                                    + "return this};";

                    String css = "#settingsoverlay,.settingsoverlay,#settingsoverlay_high,"
                                 + ".settingsoverlay_high{filter:Alpha(Opacity=40);"
                                 + "-moz-opacity:.4;opacity:.4;background-color:#444;display:none;position:absolute;"
                                 + "left:0;top:0;width:100%;height:100%;text-align:center;z-index:5000;}"
                                 + "#settingsoverlay_high,.settingsoverlay_high{z-index:6000;}"
                                 + "#settingsoverlaycontent,#settingsoverlaycontent_high{display:none;z-index:5500;"
                                 + "text-align:center;}.settingsoverlaycontent,"
                                 + ".settingsoverlaycontent_high{display:none;z-index:5500;text-align:left;}"
                                 + "#settingsoverlaycontent_high,.settingsoverlaycontent_high{z-index:6500;}"
                                 + "#settingsoverlaycontent .modalborder,"
                                 + "#settingsoverlaycontent_high .modalborder{padding:15px;width:300px;"
                                 + "border:1px solid #444;background-color:white;"
                                 + "-webkit-box-shadow:0 0 10px rgba(0,0,0,0.8);-moz-box-shadow:0 0 10px rgba(0,0,0,0.8);"
                                 + "box-shadow:0 0 10px rgba(0,0,0,0.8);"
                                 + "filter:progid:DXImageTransform.Microsoft.dropshadow(OffX=5,OffY=5,Color='gray');"
                                 + "-ms-filter:\"progid:DXImageTransform.Microsoft.dropshadow(OffX=5,OffY=5,Color='gray')\";}";

                    response.render( JavaScriptHeaderItem.forScript( script, null ) );
                    response.render( CssHeaderItem.forCSS( css, null ) );
                    if( isSupportIE6() ) {
                        response.render( JavaScriptHeaderItem.forReference( new PackageResourceReference( getClass(),
                                                                                                          "jquery.bgiframe.js" ) ) );
                    }
                }

                response.render( OnDomReadyHeaderItem.forScript( getJS() ) );
            }

            private String getJS() {

                StringBuilder sb = new StringBuilder();
                sb.append( "if (jQuery('#" )
                  .append( getDivId() )
                  .append( "').length == 0) { jQuery(document.body).append('" )
                  .append( getDiv().replace( "'", "\\'" ) )
                  .append( "'); }" );
                return sb.toString();
            }

            private String getDivId() {

                return getMarkupId() + "_ovl";
            }

            private String getDiv() {

                if( isClickBkgToClose() ) {
                    return ( "<div id=\"" + getDivId() + "\" class=\"settingsoverlayCD\" onclick=\""
                             + getCloseString() + "\"></div>" );
                } else {
                    return ( "<div id=\"" + getDivId() + "\" class=\"settingsoverlayCD\"></div>" );
                }
            }
        } );

    }

    /**
     * Override and return false to suppress static JavaScript and CSS
     * contributions. (May be desired if you are concatenating / compressing
     * resources as part of build process)
     * 
     * @return
     */
    protected boolean autoAddToHeader() {

        return true;
    }

    @Override
    protected void onComponentTag(
                                   ComponentTag tag ) {

        super.onComponentTag( tag );
        tag.put( "class", "settingsoverlaycontent" );
    }

    /**
     * Open using the current Ajax context.
     * 
     * @param target
     */
    public void open(
                      AjaxRequestTarget target ) {

        target.appendJavaScript( getOpenString() );
    }

    /**
     * Close using the current Ajax context.
     * 
     * @param target
     */
    public void close(
                       AjaxRequestTarget target ) {

        target.appendJavaScript( getCloseString() );
    }

    /**
     * Return a Behavior which when applied to a Component will add an "onclick"
     * event handler that will open this Dialog.
     * 
     * @return
     */
    public Behavior getClickToOpenBehaviour() {

        return new Behavior() {

            private static final long serialVersionUID = 1L;

            @Override
            public void onComponentTag(
                                        Component component,
                                        ComponentTag tag ) {

                tag.put( "click", getOpenString() );
            }
        };
    }

    /**
     * Return a Behavior which when applied to a Component will add an "onclick"
     * event handler that will close this Dialog.
     * 
     * @return
     */
    public Behavior getClickToCloseBehaviour() {

        return new Behavior() {

            private static final long serialVersionUID = 1L;

            @Override
            public void onComponentTag(
                                        Component component,
                                        ComponentTag tag ) {

                tag.put( "click", getCloseString() );
            }
        };
    }

    /**
     * Returns the JavaScript required to open the dialog in the client browser.
     * Override to prefix or postfix with your own JavaScript code.
     * 
     * @return
     */
    protected String getOpenString() {

        if( !isEnabled() ) {
            return "";
        }
        StringBuilder result = new StringBuilder();
        result.append( "jQuery('#" ).append( getMarkupId() ).append( "_ovl').show();" );
        if( isSupportIE6() ) {
            result.append( "jQuery('#" ).append( getMarkupId() ).append( "_ovl').bgiframe();" );
        }
        result.append( "jQuery('#" )
              .append( getMarkupId() )
              .append( "_ovl').height(jQuery(document).height()); jQuery('#" )
              .append( getMarkupId() )
              .append( "').show(); " )
              .append( "jQuery('#" )
              .append( getMarkupId() )
              .append( "')[0].style.top = jQuery(document.getElementById('" )
              .append( getMarkupId() )
              .append( "').parentNode).offset().top + 30 + 'px';" );

        return result.toString();
    }

    /**
     * Returns the JavaScript required to close the dialog in the client
     * browser. Override to prefix or postfix with your own JavaScript code.
     * 
     * @return
     */
    protected String getCloseString() {

        StringBuilder result = new StringBuilder();
        result.append( "jQuery('#" ).append( getMarkupId() ).append( "').hide();" );
        result.append( "jQuery('#" ).append( getMarkupId() ).append( "_ovl').hide();" );
        return result.toString();
    }

    /**
     * Override to enable BGI frame IE6 support.
     * 
     * @return
     */
    public boolean isSupportIE6() {

        return false;
    }

    /**
     * True if clicking the background will close the dialog.
     * 
     * @return
     */
    public boolean isClickBkgToClose() {

        return clickBkgToClose;
    }

    /**
     * Set whether the dialog should close when the user clicks the background
     * (default: false);
     * 
     * @param clickBkgToClose
     */
    public void setClickBkgToClose(
                                    boolean clickBkgToClose ) {

        this.clickBkgToClose = clickBkgToClose;
    }

    /**
     * Return the visible columns into a List
     *
     * @param columnDefinitionsString
     *            column definitions String
     * @return {@link List} of {@link TableColumn}s
     */
    protected List<TableColumn> asList(
                                        String columnDefinitionsString ) {

        List<TableColumn> tableColumns = new ArrayList<TableColumn>();

        String[] cols = columnDefinitionsString.split( "," );
        int position = 1;
        for( int index = 0; index < cols.length; index++ ) {

            String[] colData = cols[index].split( ":" );

            if( colData[0].isEmpty() ) {

                continue;
            }
            TableColumn newColumn = new TableColumn();
            newColumn.setColumnPosition( position++ );
            if( "UserNote".equalsIgnoreCase( colData[0] ) ) {
                newColumn.setColumnName( "User Note" );
            } else {
                newColumn.setColumnName( colData[0] );
            }
            try {
                Double columnWidth = Double.parseDouble( colData[1] );
                newColumn.setInitialWidth( columnWidth.intValue() );
            } catch( NumberFormatException nfe ) {
                LOG.warn( "Non parsable double value for column width: " + colData[1] );
            }

            tableColumns.add( newColumn );
        }

        return tableColumns;
    }

    /**
     * Set column properties to each element from the jsColDefinitions list and
     * add the missing not visible columns
     *
     * @param dbColDefinitions
     * @param jsColDefinitions
     */
    public void orderTableColumns(
                                   List<TableColumn> dbColDefinitions,
                                   List<TableColumn> jsColDefinitions ) {

        for( TableColumn dbCol : dbColDefinitions ) {

            if( jsColDefinitions.contains( dbCol ) ) {

                // add column id
                jsColDefinitions.get( jsColDefinitions.indexOf( dbCol ) ).setColumnId( dbCol.getColumnId() );
                // add column type
                jsColDefinitions.get( jsColDefinitions.indexOf( dbCol ) )
                                .setParentTable( dbCol.getParentTable() );

            } else {

                // add column definition of the hidden columns
                String tooltip = null;
                if( dbCol.getTooltip() != null ) {
                    tooltip = dbCol.getTooltip().toString();
                }
                TableColumn column = new TableColumn( dbCol.getColumnId(),
                                                      dbCol.getColumnName(),
                                                      dbCol.getParentTable(),
                                                      dbCol.getSortProperty(),
                                                      dbCol.getPropertyExpression(),
                                                      tooltip,
                                                      dbCol.getHeaderCssClass(),
                                                      false,
                                                      dbCol.isEditable(),
                                                      dbCol.getInitialWidth() );
                jsColDefinitions.add( column );
                column.setColumnPosition( jsColDefinitions.indexOf( column ) + 1 );
            }
        }
    }

    /**
     * Send a list with updated column states to be saved to the database
     *
     * @param jsColDefinitions
     *            column definitions List from the Test Explorer
     * @throws DatabaseAccessException
     * @throws SQLException
     */
    public void saveColumnDefinitionsToDb(
                                           List<TableColumn> jsColDefinitions ) throws DatabaseAccessException,
                                                                                SQLException {

        TestExplorerDbWriteAccessInterface dbWriter = ( ( TestExplorerSession ) Session.get() ).getDbWriteConnection();
        dbWriter.updateDBColumnDefinitionTable( jsColDefinitions );
    }

    /**
     * Update the column list used by the Test Explorer
     *
     * @param oldDBList
     *            column definitions List with the old records
     * @param jsColDefinitions
     *            column definition List with the new records
     */
    private void modifyDBColumnDefinitionList(
                                               List<TableColumn> jsColDefinitions ) {

        String dbName = ( ( TestExplorerSession ) Session.get() ).getDbName();
        List<TableColumn> oldDBList = ( ( TestExplorerApplication ) getApplication() ).getColumnDefinition( dbName );

        for( TableColumn jsCol : jsColDefinitions ) {

            oldDBList.get( oldDBList.indexOf( jsCol ) ).setColumnPosition( jsCol.getColumnPosition() );
            oldDBList.get( oldDBList.indexOf( jsCol ) ).setVisible( jsCol.isVisible() );
            oldDBList.get( oldDBList.indexOf( jsCol ) ).setInitialWidth( jsCol.getInitialWidth() );
        }

        ( ( TestExplorerApplication ) getApplication() ).setColumnDefinition( dbName, oldDBList );

    }
}
