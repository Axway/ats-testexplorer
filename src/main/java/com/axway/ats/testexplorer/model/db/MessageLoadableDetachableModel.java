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

import com.axway.ats.log.autodb.entities.Message;

public class MessageLoadableDetachableModel extends LoadableDetachableModel<Message> {

    private static final long serialVersionUID = 1L;

    private Message           message;

    public MessageLoadableDetachableModel( Message message ) {

        this.message = message;
    }

    @Override
    protected Message load() {

        return this.message;
    }

    @Override
    public boolean equals(
                           Object obj ) {

        if( obj == this ) {
            return true;
        } else if( obj == null ) {
            return false;
        } else if( obj instanceof MessageLoadableDetachableModel ) {
            MessageLoadableDetachableModel other = ( MessageLoadableDetachableModel ) obj;
            return other.message.messageId == this.message.messageId;
        }
        return false;
    }

    @Override
    public int hashCode() {

        return Long.valueOf( this.message.messageId ).hashCode();
    }

}
