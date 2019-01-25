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
package com.axway.ats.testexplorer.pages.testcase.attachments;

import java.io.IOException;
import java.util.Properties;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.log4j.Logger;

import com.axway.ats.common.filesystem.FileSystemOperationException;
import com.axway.ats.core.filesystem.LocalFileSystemOperations;
import com.axway.ats.core.utils.IoUtils;
import com.axway.ats.core.utils.StringUtils;

public class ContextListener implements ServletContextListener {

    private static final Logger LOG                     = Logger.getLogger(ContextListener.class);

    // directory name, relative to the attached files parent, where all attached files will be stored
    private static final String ATTACHED_FILES_DIR      = "ats-attached-files";
    /** Parent directory where attached files directory will be created */
    private static final String ATTACHED_FILES_PARENT_DIR = "ats.attached.files.dir";

    public static String getAttachedFilesDir() {

        return ATTACHED_FILES_DIR;
    }

    @Override
    public void contextInitialized(
                                    ServletContextEvent sce ) {

        LocalFileSystemOperations operations = new LocalFileSystemOperations();

        String attachmentsDir = System.getProperty(ATTACHED_FILES_PARENT_DIR);
        if (StringUtils.isNullOrEmpty(attachmentsDir)) {
            attachmentsDir = System.getenv(ATTACHED_FILES_PARENT_DIR);
        }
        if (StringUtils.isNullOrEmpty(attachmentsDir)) {
            attachmentsDir = getProperties().getProperty(ATTACHED_FILES_PARENT_DIR);
        }
        if (StringUtils.isNullOrEmpty(attachmentsDir)) {
            attachmentsDir = System.getenv("CATALINA_BASE");
        }
        if (StringUtils.isNullOrEmpty(attachmentsDir)) {
            attachmentsDir = System.getenv("CATALINA_HOME");
        }

        if (StringUtils.isNullOrEmpty(attachmentsDir)) {
            LOG.error("Directory for attached files is not configured. "
                      + "You can set such directory in one of the following ways: " + "key '"
                      + ATTACHED_FILES_PARENT_DIR
                      + "' as a system variable, environment variable or property in the WEB-INF/classes/ats.config.properties configuration file in the Test Explorer war file. "
                      + "Last option is to set 'CATALINA_BASE' or 'CATALINA_HOME' when running on Tomcat.");
        } else {
            String atsAttachedFiles = IoUtils.normalizeFilePath(attachmentsDir + "/" + ATTACHED_FILES_DIR);
            if (!operations.doesFileExist(atsAttachedFiles)) {
                try {
                    operations.createDirectory(atsAttachedFiles);
                } catch (FileSystemOperationException fsoe) {
                    sce.getServletContext().log(
                                                "Could not create directory for storing ATS attached files at "
                                                + atsAttachedFiles, fsoe);
                    return;
                }
            }

            // setting folder path to the property
            sce.getServletContext().setAttribute(ATTACHED_FILES_DIR, atsAttachedFiles);

            sce.getServletContext()
               .log("ATS attached files directory is set to \"" + atsAttachedFiles + "\".");
        }
    }

    private Properties getProperties() {

        Properties configProperties = new Properties();

        try {
            configProperties.load(this.getClass()
                                      .getClassLoader()
                                      .getResourceAsStream("ats.config.properties"));
        } catch (IOException e) {
            LOG.error("Can't load ats.config.properties file", e);
        }

        return configProperties;
    }

    @Override
    public void contextDestroyed(
                                  ServletContextEvent sce ) {}
}
