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
package com.axway.ats.testexplorer.model.db;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.wicket.Session;

import com.axway.ats.testexplorer.model.TestExplorerSession;

public class MessageFilterDetails implements Serializable {

    private static final long        serialVersionUID   = 1L;

    private Set<String>              threads            = new TreeSet<String>();

    private Set<String>              machines           = new TreeSet<String>();

    private List<String>             levels             = new ArrayList<String>();

    public static final List<String> SKIPPED_LOG_LEVELS = new ArrayList<String>(Arrays.asList("trace",
                                                                                              "debug"));

    public Set<String> getThreads() {

        return threads;
    }

    public void setThreads(
                            Set<String> threads ) {

        this.threads = threads;
    }

    public Set<String> getMachines() {

        return machines;
    }

    public void setMachines(
                             Set<String> machines ) {

        this.machines = machines;
    }

    public List<String> getLevels() {

        return levels;
    }

    public List<String> getSelectedLevels() {

        List<String> selectedLevels = new ArrayList<String>(levels);
        String minMessageLevel = ((TestExplorerSession) Session.get()).getMinMessageLevel();
        if (SKIPPED_LOG_LEVELS.contains(minMessageLevel)) {
            selectedLevels.removeAll(SKIPPED_LOG_LEVELS.subList(0,
                                                                SKIPPED_LOG_LEVELS.indexOf(minMessageLevel)));
        } else {
            selectedLevels.removeAll(SKIPPED_LOG_LEVELS);
        }
        return selectedLevels;
    }

    public void setLevels(
                           List<String> levels ) {

        this.levels = levels;
    }

}
