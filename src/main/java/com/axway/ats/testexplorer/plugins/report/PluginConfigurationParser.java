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

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.axway.ats.core.utils.StringUtils;

/**
 * XML parser class which reads all data about a Test Explorer plugin
 */
public class PluginConfigurationParser {

    private static final Logger LOG               = Logger.getLogger( PluginConfigurationParser.class );

    private static final String NODE__PLUGINS     = "plugins";
    private static final String NODE__PLUGIN      = "plugin";
    public static final String  NODE__TYPE        = "type";
    private static final String NODE__NAME        = "name";
    public static final String  NODE__PAGE_CLASS  = "pageClass";
    public static final String  NODE__BUTTON_NAME = "buttonName";

    public enum PLUGIN_TYPE {
        SINGLE_TESTCASE_REPORT, COMPARE_TESTCASES_REPORT, COMPARE_RUNS_REPORT
    }

    /**
     * Initializer method of the parser. Initializes the document instance.
     * 
     * @param inputStream - configuration file input stream to be parsed
     * @param jarFilePath the jar containing the XMl we will parse
     */
    private Document inititalizeParser( InputStream inputStream,
                                        String jarFilePath ) throws IOException, SAXException,
                                                             ParserConfigurationException {

        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setIgnoringElementContentWhitespace( true );
        documentBuilderFactory.setNamespaceAware( true );
        documentBuilderFactory.setValidating( false );
        documentBuilderFactory.setIgnoringComments( true );

        try {
            // skip DTD validation
            documentBuilderFactory.setFeature( "http://apache.org/xml/features/nonvalidating/load-dtd-grammar",
                                               false );
            documentBuilderFactory.setFeature( "http://apache.org/xml/features/nonvalidating/load-external-dtd",
                                               false );

            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            return documentBuilder.parse( inputStream, jarFilePath );
        } catch( ParserConfigurationException | IOException | SAXException e ) {
            LOG.error( e.getMessage() );
            throw e;
        }
    }

    /**
     * Parses the input stream from the CTF configuration file
     * and fills <code>registeredClasses</code> and <code>registeredListeners</code>
     * with classes found.
     * @param inputStream - the input stream
     * @param jarFilePath - the jar containing the XMl we will parse
     * 
     * @return [plugin name , [parameters map] ]
     * @throws ParserConfigurationException 
     * @throws SAXException 
     * @throws IOException 
     * @throws PluginLoadException 
     */
    public Map<String, Map<String, String>> parse( InputStream inputStream,
                                                   String jarFilePath ) throws IOException, SAXException,
                                                                        ParserConfigurationException,
                                                                        PluginLoadException {

        Map<String, Map<String, String>> pluginsInfo = new HashMap<>();

        Document mDocument = inititalizeParser( inputStream, jarFilePath );

        NodeList nodes = mDocument.getChildNodes();
        Node pluginNode = nodes.item( 0 );
        if( pluginNode.getNodeType() != Node.ELEMENT_NODE
            || !pluginNode.getNodeName().equals( NODE__PLUGINS ) ) {
            throw new PluginLoadException( "The expected top level " + NODE__PLUGINS + " node not found in "
                                           + jarFilePath );
        }

        nodes = pluginNode.getChildNodes();
        for( int i = 0; i < nodes.getLength(); i++ ) {
            Node childNode = nodes.item( i );
            if( childNode.getNodeType() == Node.ELEMENT_NODE
                && childNode.getNodeName().equals( NODE__PLUGIN ) ) {

                Map<String, String> parameters = new HashMap<>();
                String name = null;
                NamedNodeMap attributes = childNode.getAttributes();
                for( int iAtt = 0; iAtt < attributes.getLength(); iAtt++ ) {
                    Node attributeNode = attributes.item( iAtt );
                    if( NODE__NAME.equalsIgnoreCase( attributeNode.getNodeName() ) ) {
                        name = attributeNode.getNodeValue();
                    } else {
                        parameters.put( attributeNode.getNodeName(), attributeNode.getNodeValue() );
                    }
                }

                // check for missing data
                String missingData = null;
                if( StringUtils.isNullOrEmpty( name ) ) {
                    missingData = NODE__NAME;
                }
                if( !parameters.containsKey( NODE__TYPE ) ) {
                    missingData = NODE__TYPE;
                }
                if( !parameters.containsKey( NODE__PAGE_CLASS ) ) {
                    missingData = NODE__PAGE_CLASS;
                }
                if( !parameters.containsKey( NODE__BUTTON_NAME ) ) {
                    missingData = NODE__BUTTON_NAME;
                }
                if( missingData != null ) {
                    throw new PluginLoadException( "Missing plugin '" + missingData + "' node in "
                                                   + TestExplorerPluginsRepo.PLUGIN_DESCRIPTOR + " in "
                                                   + jarFilePath );
                }

                // we do not allow more than one plugin with same name
                if( pluginsInfo.containsKey( name ) ) {
                    throw new PluginLoadException( "There is already a plugin with name '" + name + "'" );
                }

                // make a quick check the plugin class name can be loaded runtime
                new PluginParameters( parameters );

                // accept the plugin info
                pluginsInfo.put( name, parameters );
                LOG.info( "Loaded Test Explorer plugin pluginName '" + name + "'" );
            }
        }

        return pluginsInfo;
    }
}
