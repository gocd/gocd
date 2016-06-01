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

import java.util.ArrayList;
import java.util.List;

import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamImplicit;

@XStreamAlias("history")
public class History {
    @XStreamImplicit
    private List<Changeset> changesets = new ArrayList<>();

    public List<Modification> getModifications() {
        List<Modification> mods = new ArrayList<>();
        if (changesets != null) {
            for (Changeset changeset : changesets) {
                mods.add(changeset.getModification());
            }
        }
        return mods;
    }

    @Override public String toString() {
        return "History{" +
                "changesets=" + changesets +
                '}';
    }

    public void add(Changeset changeset) {
        changesets.add(changeset);
    }

    public Changeset lastChangeSet() {
        return changesets.get(changesets.size() - 1);
    }
}
