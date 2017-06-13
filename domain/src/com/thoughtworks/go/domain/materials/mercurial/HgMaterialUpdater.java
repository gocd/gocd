/*
 * Copyright 2017 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.thoughtworks.go.domain.materials.mercurial;

import com.thoughtworks.go.config.materials.mercurial.HgMaterial;
import com.thoughtworks.go.domain.BuildCommand;
import com.thoughtworks.go.domain.materials.Revision;
import com.thoughtworks.go.domain.materials.RevisionContext;
import com.thoughtworks.go.domain.materials.svn.MaterialUrl;
import com.thoughtworks.go.util.command.HgUrlArgument;
import com.thoughtworks.go.util.command.UrlArgument;

import java.io.File;

import static com.thoughtworks.go.domain.BuildCommand.*;

public class HgMaterialUpdater {
    private HgMaterial material;

    public HgMaterialUpdater(HgMaterial material) {
        this.material = material;
    }

    public BuildCommand updateTo(String baseDir, RevisionContext revisionContext) {
        Revision revision = revisionContext.getLatestRevision();
        String workingDir = material.workingdir(new File(baseDir)).getPath();
        UrlArgument url = material.getUrlArgument();
        return compose(
                secret(url.forCommandline(), url.forDisplay()),
                echoWithPrefix("Start updating %s at revision %s from %s", material.updatingTarget(), revision.getRevision(), url.forDisplay()),
                cloneIfNeeded(workingDir),
                pull(workingDir),
                update(workingDir, revision),
                echoWithPrefix("Done.\n"));
    }

    private BuildCommand pull(String workingDir) {
        return exec("hg", "pull", "-b", material.getBranch(), "--config", String.format("paths.default=%s", material.getUrl())).
                setWorkingDirectory(workingDir);
    }

    private BuildCommand update(String workingDir, Revision revision) {
        return exec("hg", "update", "--clean", "-r", revision.getRevision()).setWorkingDirectory(workingDir);
    }

    private BuildCommand cloneIfNeeded(String workDir) {
        return compose(
                mkdirs(workDir).setTest(test("-nd", workDir)),
                cleanWorkingDir(workDir),
                cmdClone(workDir).setTest(isNotRepository(workDir))
        );
    }

    private BuildCommand isRepoUrlChanged(String workDir) {
        return test("-neq",
                new MaterialUrl(material.getHgUrlArgument().defaultRemoteUrl()).getUrl(),
                exec("hg", "showconfig", "paths.default").setWorkingDirectory(workDir));
    }

    private BuildCommand isBranchChanged(String workDir) {
        return test("-neq",
                material.getBranch(),
                exec("hg", "branch").setWorkingDirectory(workDir));
    }

    private BuildCommand isNotRepository(String workDir) {
        return test("-nd", new File(workDir, ".hg").getPath());
    }

    private BuildCommand cleanWorkingDir(String workDir) {
        return compose(
                cleandir(workDir).setTest(isNotRepository(workDir)),
                cleandir(workDir).setTest(isRepoUrlChanged(workDir)),
                cleandir(workDir).setTest(isBranchChanged(workDir))

        ).setTest(test("-d", workDir));
    }

    private BuildCommand cmdClone(String workingDir) {
        return exec("hg", "clone", "-b", this.material.getBranch(), material.getUrl(), workingDir);
    }
}
