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

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.AjaxSelfUpdatingTimerBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.time.Duration;

import com.axway.ats.testexplorer.pages.LightweightBasePage;

public abstract class BaseCopyPage extends LightweightBasePage {

    private static final long             serialVersionUID        = 1L;

    public static List<CopyJobThread>     copyJobThreads          = new ArrayList<CopyJobThread>();

    // Max console lines. If the limit is reached the new lines are added and the oldest are deleted
    private static final int              MAX_CONSOLE_LINES       = 200;
    // Console update interval in seconds
    protected static final int            CONSOLE_UPDATE_INTERVAL = 3;

    protected WebMarkupContainer          consoleContainer;
    private AjaxSelfUpdatingTimerBehavior consoleUpdateTimer;
    protected List<String>                webConsole              = new ArrayList<String>();

    protected IModel<String>              sourceHostModel         = new Model<String>();
    protected IModel<String>              sourceDbNameModel       = new Model<String>();
    protected IModel<String>              destinationHostModel    = new Model<String>();
    protected IModel<String>              destinationDbNameModel  = new Model<String>();

    protected Form<Object>                form;

    transient private CopyJobThread       copyThread;

    public BaseCopyPage( PageParameters parameters ) {

        super( parameters );

        this.form = new Form<Object>( "form" );
        addCopyButton( form );
        addCopyHiddenButton( form );
        add( form );

        IModel<List<String>> consoleModel = new LoadableDetachableModel<List<String>>() {

            private static final long serialVersionUID = 1L;

            protected List<String> load() {

                int size = webConsole.size();
                if( size > MAX_CONSOLE_LINES ) {
                    webConsole.subList( 0, size - MAX_CONSOLE_LINES ).clear();
                }
                return webConsole;
            }
        };

        ListView<String> messages = new ListView<String>( "messages", consoleModel ) {

            private static final long serialVersionUID = 1L;

            protected void populateItem(
                                         ListItem<String> item ) {

                Label label = new Label( "message", item.getModelObject() );
                label.setEscapeModelStrings( false );
                item.add( label );
            }
        };
        messages.setOutputMarkupId( true );

        consoleContainer = new WebMarkupContainer( "webConsole" );
        consoleContainer.setOutputMarkupId( true );
        consoleContainer.add( messages );

        add( consoleContainer );
    }

    protected abstract String generateThreadIdentifier();

    protected abstract CopyJobThread getNewCopyThread(
                                                       String threadIdentifier );

    protected abstract boolean isInputValid();

    protected abstract void addCopyDetailsComponents();

    private void addCopyButton(
                                Form<Object> form ) {

        AjaxButton button = new AjaxButton( "copyButton", form ) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(
                                     AjaxRequestTarget target,
                                     Form<?> form ) {

                if( consoleUpdateTimer == null || consoleUpdateTimer.isStopped() ) {

                    // clear current console logs
                    webConsole.clear();

                    // do some input validation
                    if( isInputValid() ) {

                        // get the thread identifier for this copy process
                        String threadIdentifier = generateThreadIdentifier();

                        // if we are already running this copy process - we will not start a new copy
                        // process, but we will simply attach to the current copy console
                        if( !checkForThreadCopingTheSameRun( threadIdentifier ) ) {

                            // create a new copy process
                            copyThread = getNewCopyThread( threadIdentifier );

                            if( copyThread.isInitSuccessful() ) {

                                if( copyThread.areDbVersionsDifferent() ) {

                                    // ask user for confirmation
                                    target.appendJavaScript( "if ( confirm('The two databases has different versions!"
                                                             + "\\nSource database version is "
                                                             + copyThread.getSrcDbVersion()
                                                             + " but the destinanation is "
                                                             + copyThread.getDstDbVersion()
                                                             + "\\nAre you sure you want to continue?') ) "
                                                             + "document.getElementById('copyHiddenButtonSpan').getElementsByTagName('input')[0].click();" );
                                } else {
                                    startCurrentCopyThread();
                                }
                            }
                        }
                    }
                    target.add( consoleContainer );
                }
            }
        };

        form.add( button );
    }

    private void addCopyHiddenButton(
                                      Form<Object> form ) {

        AjaxButton button = new AjaxButton( "copyHiddenButton" ) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(
                                     AjaxRequestTarget target,
                                     Form<?> form ) {

                startCurrentCopyThread();
                target.add( consoleContainer );
            }
        };

        form.add( button );
    }

    private void startCurrentCopyThread() {

        // start the timer
        consoleUpdateTimer = new AjaxSelfUpdatingTimerBehavior( Duration.seconds( CONSOLE_UPDATE_INTERVAL ) );
        consoleContainer.add( consoleUpdateTimer );
        // adding console update timer to thread and when the coping is done the timer will be stopped
        copyThread.addConsoleUpdateTimer( consoleUpdateTimer );

        copyThread.start();

        synchronized( copyJobThreads ) {

            copyJobThreads.add( copyThread );
        }

    }

    private boolean checkForThreadCopingTheSameRun(
                                                    String threadIdentifier ) {

        synchronized( copyJobThreads ) {

            for( CopyJobThread th : copyJobThreads ) {
                if( th.isAlive() && th.getThreadIdentifier().equals( threadIdentifier ) ) {

                    webConsole = th.getWebConsole();
                    // start the timer and attach it to the coping thread
                    AjaxSelfUpdatingTimerBehavior consoleUpdateTimer = new AjaxSelfUpdatingTimerBehavior( Duration.seconds( CONSOLE_UPDATE_INTERVAL ) );
                    th.addConsoleUpdateTimer( consoleUpdateTimer );
                    consoleContainer.add( consoleUpdateTimer );
                    return true;
                }
            }
        }
        return false;
    }
}
