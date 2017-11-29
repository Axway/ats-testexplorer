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
package com.axway.ats.testexplorer.plugins.report;

import java.io.Serializable;
import java.util.Map;

import org.apache.wicket.markup.html.WebPage;

/**
 * Parameters describing a report plugin
 */
public class PluginParameters implements Serializable {

    private static final long        serialVersionUID = 1L;

    /**
     * the button name as the user will see it
     */
    private String                   buttonName;

    /**
     * The Web page class that will be triggered upon pressing the plugin button
     */
    private Class<? extends WebPage> klass;

    @SuppressWarnings("unchecked")
    public PluginParameters( Map<String, String> parameters ) {

        buttonName = parameters.get( PluginConfigurationParser.NODE__BUTTON_NAME );

        try {
            klass = ( Class<? extends WebPage> ) Class.forName( parameters.get( PluginConfigurationParser.NODE__PAGE_CLASS ) );
        } catch( ClassNotFoundException e ) {
            throw new PluginLoadException( "Error loading plugin class "
                                           + parameters.get( PluginConfigurationParser.NODE__PAGE_CLASS ),
                                           e );
        }
    }

    public String getButtonName() {

        return buttonName;
    }

    public Class<? extends WebPage> getPluginClass() {

        return klass;
    }
}
