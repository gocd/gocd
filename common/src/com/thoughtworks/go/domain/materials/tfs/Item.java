/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.domain.materials.tfs;

import com.thoughtworks.go.domain.materials.ModifiedAction;
import com.thoughtworks.go.domain.materials.ModifiedFile;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

@XStreamAlias("item")
public class Item {
    @XStreamAsAttribute
    @XStreamAlias("change-type")
    private String changeType;

    @XStreamAsAttribute
    @XStreamAlias("server-item")
    private String serverItem;

    public Item(String changeType, String serverItem) {
        this.changeType = changeType;
        this.serverItem = serverItem;
    }

    enum ChangeType {
        add(ModifiedAction.added), delete(ModifiedAction.deleted), edit(ModifiedAction.modified);
        public final ModifiedAction action;

        ChangeType(ModifiedAction action) {
            this.action = action;
        }
    }

    public ModifiedFile getModifiedFile() {
        return new ModifiedFile(serverItem, null, getModifiedAction());
    }

    private ModifiedAction getModifiedAction() {
        ChangeType[] values = ChangeType.values();
        for (ChangeType value : values) {
            if (value.toString().equals(changeType)) {
                return value.action;
            }
        }
        return ModifiedAction.unknown;
    }

    @Override public String toString() {
        return "Item{" +
                "changeType='" + changeType + '\'' +
                ", serverItem='" + serverItem + '\'' +
                '}';
    }
}
