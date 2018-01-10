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

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.log4j.Logger;

import com.axway.ats.common.systemproperties.AtsSystemProperties;
import com.axway.ats.core.system.LocalSystemOperations;
import com.axway.ats.core.utils.IoUtils;

/**
 * Stores info about Test Explorer plugins.
 * This info is read from XML file placed into some jar containing the plugin classes. 
 */
public class TestExplorerPluginsRepo {

    private static Logger                    LOG               = Logger.getLogger(TestExplorerPluginsRepo.class);

    /**
     * If this file is found into a jar, then this jar is supposed to be a Test Explorer plugin jar
     */
    public static final String               PLUGIN_DESCRIPTOR = "META-INF/testexplorer_plugin_descriptor.xml";

    private static TestExplorerPluginsRepo   instance;

    /**
     * [plugin name, [parameters map] ]
     */
    private Map<String, Map<String, String>> pluginParametersMap;

    synchronized public static TestExplorerPluginsRepo getInstance() {

        if (instance == null) {
            instance = new TestExplorerPluginsRepo();
        }
        return instance;
    }

    private TestExplorerPluginsRepo() {

        // find the plugins folder
        String webappsHome = getTomcatWebappsFolder();

        // find all jars, they will be checked for plugins inside
        List<String> jarFiles = getJarFilesReference(webappsHome);

        LOG.debug("Searching for Test Explorer plugins in '" + webappsHome + "'");
        loadPlugins(jarFiles);
    }

    /**
     * Get all parameters of some plugin
     * 
     * @param type
     * @return
     */
    public List<PluginParameters> getPluginParameters( PluginConfigurationParser.PLUGIN_TYPE type ) {

        List<PluginParameters> pluginParameters = new ArrayList<>();
        for (String pluginName : getPluginNamesForType(type)) {
            pluginParameters.add(new PluginParameters(pluginParametersMap.get(pluginName)));
        }

        return pluginParameters;
    }

    /**
     * Get the name of all plugins of some particular type
     * 
     * @param type
     * @return
     */
    private List<String> getPluginNamesForType( PluginConfigurationParser.PLUGIN_TYPE type ) {

        List<String> pluginNames = new ArrayList<>();
        for (String name : pluginParametersMap.keySet()) {
            Map<String, String> pluginParameters = pluginParametersMap.get(name);
            if (pluginParameters.get(PluginConfigurationParser.NODE__TYPE)
                                .equalsIgnoreCase(type.toString())) {
                pluginNames.add(name);
            }
        }
        return pluginNames;
    }

    private String getTomcatWebappsFolder() {

        // get the path where this class is
        // we expect it deployed into a Tomcat webapps folder
        Class<?> klass = TestExplorerPluginsRepo.class;
        String webapps = klass.getClassLoader()
                              .getResource(klass.getName().replace(".", "/") + ".class")
                              .getPath();

        // make sure we always use same file separator character
        webapps = IoUtils.normalizeDirPath(webapps);

        // remove the path coming from Test Explorers war
        webapps = webapps.substring(0, webapps.indexOf("webapps") + "webapps".length() + 1);

        // if running on Windows and it starts with a file separator - remove it
        if (new LocalSystemOperations().getOperatingSystemType().isWindows()
            && webapps.startsWith(AtsSystemProperties.SYSTEM_FILE_SEPARATOR)) {
            webapps = webapps.substring(1);
        }

        // next replacement appears to be needed when loading files 
        // from folder with whitespace in the path
        return webapps.replaceAll("%20", " ");
    }

    /**
     * Browse a folder for jar files (not recursively)
     *
     * @param folder the folder to search into
     * @return a list with all found jars
     */
    private List<String> getJarFilesReference( String folder ) {

        List<String> jarsReference = new ArrayList<String>();

        try {
            File[] files = new File(folder).listFiles();
            for (File file : files) {
                if (file.isFile() && file.getName().endsWith(".jar")) {
                    jarsReference.add(folder + AtsSystemProperties.SYSTEM_FILE_SEPARATOR + file.getName());
                }
            }
        } catch (Exception e) {
            LOG.warn("Error searching for jar files into '" + folder + "' folder");
        }

        return jarsReference;
    }

    /**
     * Search for plugins in all provided jars and try to load them
     * 
     * @param jarFiles the jars to look through
     */
    private void loadPlugins( List<String> jarFiles ) {

        pluginParametersMap = new HashMap<>();

        for (String jarFilePath : jarFiles) {
            JarFile jarFile = null;
            try {
                jarFile = new JarFile(new File(jarFilePath));

                JarEntry entry = jarFile.getJarEntry(PLUGIN_DESCRIPTOR);
                if (entry != null) {
                    LOG.info("Found Test Explorer plugins description file in '" + jarFilePath
                             + "', starting registration process");

                    InputStream descriptorStream = jarFile.getInputStream(entry);

                    pluginParametersMap.putAll(new PluginConfigurationParser().parse(descriptorStream,
                                                                                     jarFilePath));
                }
            } catch (Exception e) {
                LOG.warn("Could not load Test Explorer plugin from '" + jarFilePath + "', skipping it.", e);
            } finally {
                IoUtils.closeStream(jarFile);
            }
        }
    }
}
