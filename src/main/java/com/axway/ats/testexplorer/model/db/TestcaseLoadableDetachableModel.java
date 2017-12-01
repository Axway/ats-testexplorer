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

import com.axway.ats.log.autodb.entities.Testcase;
import com.axway.ats.testexplorer.pages.model.MainDataGrid;

public class TestcaseLoadableDetachableModel extends LoadableDetachableModel<Testcase> {

    private static final long serialVersionUID = 1L;

    private Testcase          testcase;

    private MainDataGrid      grid;

    /**
     *
     * @param testcase
     * @param grid Used for edit mode detection. If in edit mode then the
     * duration(state,result) is not checked and the model is not changed.
     * Otherwise if the model was changed in edit mode and the testcase is still
     * in progress then editable cell will disappear
     */
    public TestcaseLoadableDetachableModel( Testcase testcase, MainDataGrid grid ) {

        this.testcase = testcase;
        this.grid = grid;
    }

    @Override
    protected Testcase load() {

        return this.testcase;
    }

    @Override
    public boolean equals( Object obj ) {

        if (obj == this) {
            return true;
        } else if (obj == null) {
            return false;
        } else if (obj instanceof TestcaseLoadableDetachableModel) {
            TestcaseLoadableDetachableModel other = (TestcaseLoadableDetachableModel) obj;

            if (grid != null && grid.isEditMode()) {
                return other.testcase.testcaseId.equals(this.testcase.testcaseId);
            }
            return other.testcase.testcaseId.equals(this.testcase.testcaseId)
                   && other.testcase.state.equals(this.testcase.state)
                   && other.testcase.getDurationAsString(0).equals(this.testcase.getDurationAsString(0))
                   && other.testcase.result == this.testcase.result;
        }
        return false;
    }

    @Override
    public int hashCode() {

        return this.testcase.testcaseId.hashCode();
    }

}
