/*************************GO-LICENSE-START*********************************
 * Copyright 2015 ThoughtWorks, Inc.
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

package com.thoughtworks.go.domain.cctray;

import com.thoughtworks.go.domain.MaterialRevision;
import com.thoughtworks.go.domain.MaterialRevisions;
import com.thoughtworks.go.domain.Stage;
import com.thoughtworks.go.domain.StageResult;
import com.thoughtworks.go.domain.feed.Author;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

/* Understands how to calculate "breakers" for a failed stage, for CCTray. */
@Component
public class CcTrayBreakersCalculator {
    private MaterialRepository materialRepository;

    @Autowired
    public CcTrayBreakersCalculator(MaterialRepository materialRepository) {
        this.materialRepository = materialRepository;
    }

    public Set<String> calculateFor(Stage stage) {
        Set<String> breakersForChangedMaterials = new HashSet<>();
        Set<String> breakersForMaterialsWithNoChange = new HashSet<>();

        if (stage.getResult() == StageResult.Failed) {
            MaterialRevisions materialRevisions = materialRepository.findMaterialRevisionsForPipeline(stage.getPipelineId());
            for (MaterialRevision materialRevision : materialRevisions) {
                if (materialRevision.isChanged()) {
                    addToBreakers(breakersForChangedMaterials, materialRevision);
                } else {
                    addToBreakers(breakersForMaterialsWithNoChange, materialRevision);
                }
            }
        }

        return breakersForChangedMaterials.isEmpty() ? breakersForMaterialsWithNoChange : breakersForChangedMaterials;
    }

    private void addToBreakers(Set<String> breakers, MaterialRevision materialRevision) {
        for (Modification modification : materialRevision.getModifications()) {
            Author authorInfo = Author.getAuthorInfo(materialRevision.getMaterial().getType(), modification);
            if (authorInfo != null) {
                breakers.add(authorInfo.getName());
            }
        }
    }
}
