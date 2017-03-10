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

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.wicket.Session;
import org.apache.wicket.model.IModel;

import com.axway.ats.log.autodb.entities.Suite;
import com.axway.ats.log.autodb.exceptions.DatabaseAccessException;
import com.axway.ats.testexplorer.model.TestExplorerSession;
import com.inmethod.grid.IDataSource;
import com.inmethod.grid.IGridSortState;
import com.inmethod.grid.IGridSortState.ISortStateColumn;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class SuitesDataSource implements IDataSource {

    private static final long   serialVersionUID = 1L;

    private static final Logger LOG              = Logger.getLogger( SuitesDataSource.class );

    private String              runId;
    private String              suiteId;

    public SuitesDataSource( String runId,
                             String suiteId ) {

        this.runId = runId;
        this.suiteId = suiteId;
    }

    public SuitesDataSource( String runId ) {

        this.runId = runId;
    }

    @Override
    public IModel<Suite> model(
                                final Object object ) {

        return new SuiteLoadableDetachableModel( ( Suite ) object );
    }

    @Override
    public void query(
                       IQuery query,
                       IQueryResult result ) {

        String sortProperty = "suiteId";
        boolean sortAsc = true;
        // is there any sorting
        if( query.getSortState().getColumns().size() > 0 ) {
            // get the most relevant column
            ISortStateColumn state = query.getSortState().getColumns().get( 0 );
            // get the column sort properties
            sortProperty = ( String ) state.getPropertyName();
            sortAsc = state.getDirection() == IGridSortState.Direction.ASC;
        }

        List<Suite> resultList;
        try {
            TestExplorerDbReadAccessInterface dbAccess = ( ( TestExplorerSession ) Session.get() ).getDbReadConnection();
            String whereClause;
            if( suiteId != null ) {
                whereClause = "WHERE runId=" + this.runId + " and suiteId=" + this.suiteId;
            } else {
                whereClause = "WHERE runId=" + this.runId;
            }
            result.setTotalCount( dbAccess.getSuitesCount( whereClause ) );

            resultList = dbAccess.getSuites( ( int ) ( query.getFrom() + 1 ),
                                             ( int ) ( query.getFrom() + query.getCount() + 1 ),
                                             whereClause,
                                             sortProperty,
                                             sortAsc,
                                             true );

            String[] packageNames = new String[resultList.size()];

            for( int i = 0; i < resultList.size(); i++ ) {

                packageNames[i] = resultList.get( i ).packageName;

            }
            String[] parsedPackageNames = parsePackages( packageNames );

            for( int i = 0; i < parsedPackageNames.length; i++ ) {

                resultList.get( i ).packageName = parsedPackageNames[i];

            }

            result.setItems( resultList.iterator() );
        } catch( DatabaseAccessException e ) {
            LOG.error( "Can't get suites", e );
        }
    }

    @Override
    public void detach() {

    }

    /**
     * Receives full java package names and returns them back as short as possible 
     * by removing the common leading tokens in the package names
     * 
     * This method has package scope for easy unit testing in {@link Test_SuitesDataSource} 
     * 
     * @param packages  List with all full package names 
     * @return  List with all parsed package names
     */
    static String[] parsePackages(
                                   String[] packages ) {

        String[][] packageTokens = new String[packages.length][];

        // break packages into package tokens
        int sizeShortestPackage = Integer.MAX_VALUE;
        for( int i = 0; i < packages.length; i++ ) {
            packageTokens[i] = packages[i].split( "\\." );

            if( sizeShortestPackage > packageTokens[i].length ) {
                sizeShortestPackage = packageTokens[i].length;
            }
        }

        // find the index of the first different token
        int iDifferentTokens = -1;
        for( int iToken = 0; iToken < sizeShortestPackage; iToken++ ) {
            String currentToken = null;
            for( int iPackage = 0; iPackage < packageTokens.length; iPackage++ ) {
                String token = packageTokens[iPackage][iToken];
                if( currentToken == null ) {
                    // remember this token
                    currentToken = token;
                } else if( !currentToken.equals( token ) ) {
                    // we have found a different token, stop cycling
                    iDifferentTokens = iToken;
                    break;
                }
                // else -> token is the same, go to next package at this index
            }

            if( iDifferentTokens != -1 ) {
                // we have found a different token, stop cycling
                break;
            }
            // else -> all tokens are the same at this index, go to next index
        }

        // define the token where we start copying packages from
        int startToken;
        if( iDifferentTokens != -1 ) {
            // different packages are found
            startToken = iDifferentTokens;
        } else {
            // Different packages are not found.
            startToken = sizeShortestPackage - 1;
        }

        // generate the result packages
        List<String> resultPackages = new ArrayList<String>();
        for( int iPackage = 0; iPackage < packageTokens.length; iPackage++ ) {
            StringBuilder resultPackage = new StringBuilder();
            for( int iToken = startToken; iToken < packageTokens[iPackage].length; iToken++ ) {
                resultPackage.append(packageTokens[iPackage][iToken] + ".");
            }
            resultPackages.add( resultPackage.substring( 0, resultPackage.length() - 1 ) );
        }
        return resultPackages.toArray( new String[resultPackages.size()] );
    }

    public String getRunId() {

        return runId;
    }

    public void setRunId(
                          String runId ) {

        this.runId = runId;
    }

}
