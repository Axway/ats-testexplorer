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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * This is a Statistic Container which we create.
 * It is used to combined the values of similar statistics.
 */
public class CombinedStatisticContainer extends StatisticContainer {

    private static final long                  serialVersionUID = 1L;

    private Map<Integer, DbStatisticDescription> combinedStatDescriptions;

    public CombinedStatisticContainer( int containerIndexInDataPanel,
                                       String containerName ) {

        super( containerIndexInDataPanel, containerName );

        this.combinedStatDescriptions = new TreeMap<Integer, DbStatisticDescription>();
    }

    @Override
    public List<DbStatisticDescription> getStatDescriptions() {

        return new ArrayList<DbStatisticDescription>( combinedStatDescriptions.values() );
    }

    public DbStatisticDescription addCombinedStatisticDescription(
                                                                 DbStatisticDescription statDescription ) {

        DbStatisticDescription combinedStatDescription = null;
        for( DbStatisticDescription description : combinedStatDescriptions.values() ) {
            if( description.name.equals( statDescription.name ) ) {
                combinedStatDescription = description;
                break;
            }
        }

        if( combinedStatDescription == null ) {
            // we do not know this statistic
            combinedStatDescription = statDescription.newInstance();
            combinedStatDescription.setCombinedDisplayMode();
            combinedStatDescription.parentName = DataPanel.COMBINED_CONTAINER;

            int statisticDescriptionIndexInContainer = getNewStatisticDescriptionIndexInContainer();
            combinedStatDescription.setIndexInUI( statisticDescriptionIndexInContainer );

            combinedStatDescriptions.put( statisticDescriptionIndexInContainer, combinedStatDescription );
        } else {
            // we already know this statistic 

            // update the statistic IDs
            combinedStatDescription.addStatisticIds( statDescription.getStatisticIds() );

            // recalculate the numbers
            int nStatistics = combinedStatDescriptions.size();
            combinedStatDescription.minValue = ( combinedStatDescription.minValue * nStatistics + statDescription.minValue )
                                               / ( nStatistics + 1 );
            combinedStatDescription.avgValue = ( combinedStatDescription.avgValue * nStatistics + statDescription.minValue )
                                               / ( nStatistics + 1 );
            combinedStatDescription.maxValue = ( combinedStatDescription.maxValue * nStatistics + statDescription.minValue )
                                               / ( nStatistics + 1 );
        }

        return combinedStatDescription;
    }

    private int getNewStatisticDescriptionIndexInContainer() {

        return this.indexInDataPanel + combinedStatDescriptions.size() + 1;
    }
}
