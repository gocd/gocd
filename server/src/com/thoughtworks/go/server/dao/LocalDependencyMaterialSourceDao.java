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

package com.thoughtworks.go.server.dao;

import java.util.ArrayList;
import java.util.List;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterial;
import com.thoughtworks.go.domain.Stage;
import com.thoughtworks.go.domain.StageAsDMR;
import com.thoughtworks.go.domain.StageIdentifier;
import com.thoughtworks.go.domain.Stages;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.server.util.Pagination;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @understands fetching modifications from local stages
 */
@Component
public class LocalDependencyMaterialSourceDao implements DependencyMaterialSourceDao {
    private final StageDao stageDao;

    @Autowired
    public LocalDependencyMaterialSourceDao(StageDao stageDao) {
        this.stageDao = stageDao;
    }

    public List<Modification> getPassedStagesByName(DependencyMaterial material, Pagination pagination) {
        Stages stages = stageDao.getPassedStagesByName(CaseInsensitiveString.str(material.getPipelineName()), CaseInsensitiveString.str(material.getStageName()), pagination.getPageSize(), pagination.getOffset());
        List<Modification> mods = new ArrayList<>();
        for (Stage stage : stages) {
            StageIdentifier stageIdentifier = stage.getIdentifier();
            Modification modification = new Modification(stage.completedDate(), stageIdentifier.stageLocator(), stageIdentifier.getPipelineLabel(), stage.getPipelineId());
            mods.add(modification);
        }
        return mods;
    }

    public List<Modification> getPassedStagesAfter(final String lastRevision, DependencyMaterial material, Pagination pagination) {
        StageIdentifier identifier = new StageIdentifier(lastRevision);
        List<StageAsDMR> passedStagesAfter = stageDao.getPassedStagesAfter(identifier, pagination.getPageSize(), pagination.getOffset());
        List<Modification> mods = new ArrayList<>();
        for (StageAsDMR stage : passedStagesAfter) {
            StageIdentifier stageIdentifier = stage.getIdentifier();
            Modification modification = new Modification(stage.getCompletedDate(), stageIdentifier.stageLocator(), stageIdentifier.getPipelineLabel(), stage.getPipelineId());
            mods.add(modification);
        }
        return mods;
    }
}
