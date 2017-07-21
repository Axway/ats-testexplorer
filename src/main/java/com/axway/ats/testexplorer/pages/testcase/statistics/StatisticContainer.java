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
package com.axway.ats.testexplorer.pages.testcase.statistics;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * this is:
 *  - the parent process when working with system processes
 *  - the action queue name when working with agent actions
 *
 * it contains statistic descriptions
 */
/**
 * See the javadoc for ChartsPanel class to see how this class stays in the hierarchy 
 * 
 * This class is
 *  - the parent process when working with system processes
 *  - the action queue name when working with agent actions
 *
 * It contains statistic descriptions
 */
public class StatisticContainer implements Serializable {

    private static final long                  serialVersionUID = 1L;

    protected int                              indexInDataPanel;
    protected String                           name;

    // all statistics are ordered
    private Map<Integer, DbStatisticDescription> statDescriptions;

    public StatisticContainer( int containerIndexInDataPanel,
                               String containerName ) {

        this.indexInDataPanel = containerIndexInDataPanel;
        this.name = containerName;
        this.statDescriptions = new TreeMap<Integer, DbStatisticDescription>();
    }

    public int getContainerIndexInDataPanel() {

        return indexInDataPanel;
    }

    public String getName() {

        return name;
    }

    public List<DbStatisticDescription> getStatDescriptions() {

        return new ArrayList<DbStatisticDescription>( this.statDescriptions.values() );
    }

    public void addStatisticDescription(
                                         DbStatisticDescription statDescription ) {

        int statisticDescriptionIndexInContainer = getNewStatisticDescriptionIndexInContainer();
        statDescription.setIndexInUI( statisticDescriptionIndexInContainer );
        statDescriptions.put( statisticDescriptionIndexInContainer, statDescription );
    }

    public boolean isStatisticAvailableForThisContainer(
                                                         DbStatisticDescription newStatDescription ) {

        for( DbStatisticDescription statDescription : statDescriptions.values() ) {
            // statistic containers keep one entry of a statistic description for all testcases
            if( statDescription.getUid().equals( newStatDescription.getUid() ) ) {
                return true;
            }
        }
        return false;
    }

    private int getNewStatisticDescriptionIndexInContainer() {

        return this.indexInDataPanel + statDescriptions.size() + 1;
    }
}
