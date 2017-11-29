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
package com.axway.ats.testexplorer.pages.model;

import java.io.Serializable;

public class SelectOption implements Serializable {

    private static final long serialVersionUID = 1L;

    private String            key;

    private String            value;

    private int               parameter;

    public SelectOption( String key,
                         String value ) {

        this( key, value, -1 );
    }

    public SelectOption( String key,
                         String value,
                         int parameter ) {

        this.key = key;
        this.value = value;
        this.parameter = parameter;
    }

    public String getKey() {

        return key;
    }

    public void setKey(
                        String key ) {

        this.key = key;
    }

    public String getValue() {

        return value;
    }

    public void setValue(
                          String value ) {

        this.value = value;
    }

    public int getParameter() {

        return parameter;
    }

    @Override
    public boolean equals(
                           Object obj ) {

        if( obj == this ) {
            return true;
        } else if( obj == null ) {
            return false;
        } else if( obj instanceof SelectOption ) {
            SelectOption other = ( SelectOption ) obj;
            if( this.key == null || other.key == null || this.value == null || other.value == null ) {
                return false;
            }
            return other.key.equals( this.key ) && other.value.equals( this.value );
        }
        return false;
    }

    @Override
    public int hashCode() {

        return Integer.valueOf( this.key ).hashCode();
    }

}
