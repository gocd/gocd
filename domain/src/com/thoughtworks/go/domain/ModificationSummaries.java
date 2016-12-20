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

package com.thoughtworks.go.domain;

import java.util.ArrayList;
import java.util.List;

import com.thoughtworks.go.domain.materials.Modification;
import org.apache.commons.lang.StringUtils;

public class ModificationSummaries extends ModificationVisitorAdapter {
    private final List<ModificationSummary> mods = new ArrayList<>();

    public ModificationSummaries() {

    }

    public ModificationSummaries(MaterialRevisions materialRevisions) {
        materialRevisions.accept(this);
    }

    private boolean containsRevision(String revision) {
        for (ModificationSummary mod : mods) {
            if (mod.getRevision().equals(revision)) {
                return true;
            }
        }
        return false;
    }

    public String latestRevision() {
        if (mods.isEmpty()) {
            return StringUtils.EMPTY;
        }
        return mods.get(0).getRevision();
    }

    public int getModificationCount() {
        return mods.size();
    }

    public ModificationSummary getModification(int index) {
        return mods.get(index);
    }

    public void visit(Modification modification) {
        if (!containsRevision(modification.getRevision())) {
            mods.add(new ModificationSummary(modification));
        }
    }

}
