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

import org.apache.wicket.model.LoadableDetachableModel;

import com.axway.ats.log.autodb.entities.Run;

public class RunLoadableDetachableModel extends LoadableDetachableModel<Run> {

    private static final long serialVersionUID = 1L;

    private Run               run;

    /**
     *
     * @param run
     */
    public RunLoadableDetachableModel( Run run ) {

        this.run = run;
    }

    @Override
    protected Run load() {

        return this.run;
    }

    @Override
    public boolean equals( Object obj ) {

        if( obj == this ) {
            return true;
        } else if( obj == null ) {
            return false;
        } else if( obj instanceof RunLoadableDetachableModel ) {
            RunLoadableDetachableModel other = ( RunLoadableDetachableModel ) obj;

            /*
             *  The following code is a HACK, that helps us to keep model object/Run synced with the database.
             *  What we do here is to update only the dynamically changed fields of the cached Run.
             *
             *  Why we need this?
             *  If we return "false" when the same Run (equal IDs) has different duration time
             *  (no dateEnd, not finished yet), the cached object will be replaced in the model
             *  and if it was checked/selected before, we will not be able to Edit/Delete/... it,
             *  because the Selected object doesn't exist any more in the model/cached list.
             */
            if( other.run.runId.equals( this.run.runId )
                && other.run.getDuration( 0 ) != this.run.getDuration( 0 ) ) {

                // copy dynamic details from the most recent run version to the other
                if( other.run.getDuration( 0 ) > this.run.getDuration( 0 ) ) {
                    updateRunDynamicDetails( other.run, this.run );
                } else {
                    updateRunDynamicDetails( this.run, other.run );
                }
            }

            return other.run.runId.equals( this.run.runId );
        }
        return false;
    }

    @Override
    public int hashCode() {

        return this.run.runId.hashCode();
    }

    private void updateRunDynamicDetails( Run from, Run to ) {

        if( from.getEndTimestamp() != -1 ) {
            to.setEndTimestamp( from.getEndTimestamp() );
        }
        to.failed = from.failed;
        to.scenariosFailed = from.scenariosFailed;
        to.scenariosSkipped = from.scenariosSkipped;
        to.scenariosTotal = from.scenariosTotal;
        to.testcaseIsRunning = from.testcaseIsRunning;
        to.testcasesPassedPercent = from.testcasesPassedPercent;
        to.testcasesFailed = from.testcasesFailed;
        to.testcasesSkipped = from.testcasesSkipped;
        to.testcasesTotal = from.testcasesTotal;
        to.total = from.total;
    }

}
