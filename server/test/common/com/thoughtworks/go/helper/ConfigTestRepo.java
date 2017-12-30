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
package com.thoughtworks.go.helper;

import com.thoughtworks.go.config.MagicalGoConfigXmlWriter;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.materials.mercurial.HgMaterial;
import com.thoughtworks.go.config.parts.PartialConfigHelper;
import com.thoughtworks.go.domain.materials.Modification;

import java.io.File;
import java.util.List;

/**
 * An scm repository that is a configuration repository. Helper for tests.
 */
public class ConfigTestRepo {
    private final File baseDir;
    private final HgMaterial material;
    private  PartialConfigHelper partialConfigHelper;
    private HgTestRepo repo;

    public ConfigTestRepo(HgTestRepo repo,MagicalGoConfigXmlWriter xmlWriter)
    {
        this.repo = repo;
        baseDir = repo.prepareWorkDirectory();
        material = repo.updateTo(baseDir);

        partialConfigHelper = new PartialConfigHelper(xmlWriter,baseDir);
    }

    public List<Modification> addPipelineToRepositoryAndPush(String fileName, PipelineConfig pipelineConfig) throws Exception {
        File file = new File(baseDir, fileName);
        partialConfigHelper.addFileWithPipeline(fileName, pipelineConfig);

        return repo.addCommitPush(material, "added pipeline config", baseDir, file);
    }
    public List<Modification> addCodeToRepositoryAndPush(String fileName,String comment, String content) throws Exception {
        File file = new File(baseDir, fileName);
        partialConfigHelper.writeFileWithContent(fileName, content);

        return repo.addCommitPush(material, comment, baseDir, file);
    }

    public HgMaterial getMaterial() {
        return material;
    }
}
