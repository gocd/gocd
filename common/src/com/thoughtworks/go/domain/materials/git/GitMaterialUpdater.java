/*************************** GO-LICENSE-START*********************************
 * Copyright 2016 ThoughtWorks, Inc.
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
 * ************************GO-LICENSE-END***********************************/
package com.thoughtworks.go.domain.materials.git;

import com.thoughtworks.go.config.materials.git.GitMaterial;
import com.thoughtworks.go.domain.BuildCommand;
import com.thoughtworks.go.domain.materials.Revision;
import com.thoughtworks.go.domain.materials.RevisionContext;
import com.thoughtworks.go.util.command.UrlArgument;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import static com.thoughtworks.go.domain.BuildCommand.*;
import static java.lang.String.format;

public class GitMaterialUpdater {
    private GitMaterial material;

    public GitMaterialUpdater(GitMaterial material) {
        this.material = material;
    }

    public BuildCommand updateTo(String baseDir, RevisionContext revisionContext) {
        Revision revision = revisionContext.getLatestRevision();
        String workingDir = material.workingdir(new File(baseDir)).getPath();
        UrlArgument url = material.getUrlArgument();
        return compose(
                echoWithPrefix("Start updating %s at revision %s from %s", material.updatingTarget(), revision.getRevision(), url.forDisplay()),
                secret(url.forCommandline(), url.forDisplay()),
                cloneIfNeeded(workingDir, revisionContext.numberOfModifications() + 1),
                fetchRemote(workingDir),
                unshallowIfNeeded(workingDir, revision, new Integer[]{GitMaterial.UNSHALLOW_TRYOUT_STEP, Integer.MAX_VALUE}),
                resetWorkingCopy(workingDir, revision),
                echoWithPrefix("Done.\n"));
    }

    private BuildCommand fetchRemote(String workingDir) {
        return compose(
                echo("[GIT] Fetching changes"),
                git(workingDir, "fetch", "origin"),
                git(workingDir, "gc", "--auto"));
    }

    private BuildCommand resetWorkingCopy(String workingDir, Revision revision) {
        return compose(
                echo("[GIT] Reset working directory %s", workingDir),
                echo("[GIT] Updating working copy to revision %s", revision.getRevision()),
                cleanupUnversionedFiles(workingDir),
                resetHard(workingDir, revision),
                updateSubmodules(workingDir),
                cleanupUnversionedFiles(workingDir));
    }

    private BuildCommand updateSubmodules(String workingDir) {
        return cond(hasSubmodules(workingDir),
                compose(
                        echo("[GIT] Removing modified files in submodules"),
                        git(workingDir, "submodule", "foreach", "--recursive", "git", "checkout", "."),
                        echo("[GIT] Updating git sub-modules"),
                        git(workingDir, "submodule", "init"),
                        git(workingDir, "submodule", "sync"),
                        git(workingDir, "submodule", "foreach", "--recursive", "git", "submodule", "sync"),
                        git(workingDir, "submodule", "update"),
                        echo("[GIT] Git sub-module status"),
                        git(workingDir, "submodule", "status")));
    }

    private BuildCommand resetHard(String workingDir, Revision revision) {
        return git(workingDir, "reset", "--hard", revision.getRevision());
    }

    private BuildCommand cleanupUnversionedFiles(String workingDir) {
        return compose(
                echo("[GIT] Cleaning all unversioned files in working copy"),
                cond(hasSubmodules(workingDir),
                        git(workingDir, "submodule", "foreach", "--recursive", "git", "clean", "-fdd")),
                git(workingDir, "clean", "-dff"));
    }

    private BuildCommand hasSubmodules(String workingDir) {
        return test("-f", new File(workingDir, ".gitmodules").getPath());
    }

    private BuildCommand unshallowIfNeeded(String workingDir, Revision revision, Integer[] steps) {
        if (steps.length == 0) {
            return noop();
        }
        int depth = steps[0];
        return cond(and(isShallow(workingDir), revisionNotExists(workingDir, revision)),
                compose(echo("[GIT] Unshallowing repository with depth %d", depth),
                        git(workingDir, "fetch", "origin", format("--depth=%d", depth)),
                        unshallowIfNeeded(workingDir, revision, Arrays.copyOfRange(steps, 1, steps.length))));
    }

    private BuildCommand revisionNotExists(String workingDir, Revision revision) {
        return test("-neq", "commit",
                git(workingDir, "cat-file", "-t", revision.getRevision()));
    }

    private BuildCommand isShallow(String workingDir) {
        return test("-f", new File(workingDir, ".git/shallow").getPath());
    }

    private BuildCommand cloneIfNeeded(String workDir, int cloneDepth) {
        return compose(
                cond(test("-nd", workDir),
                        mkdirs(workDir)),
                cond(shouldCleanWorkingDir(workDir),
                        cleandir(workDir)),
                cond(isNotRepository(workDir),
                        cmdClone(workDir, cloneDepth)));
    }

    private BuildCommand shouldCleanWorkingDir(String workDir) {
        return or(isNotRepository(workDir),
                isBranchChanged(workDir),
                isRepoUrlChanged(workDir),
                material.isShallowClone() ? fail() : isShallow(workDir));
    }

    private BuildCommand isRepoUrlChanged(String workDir) {
        return test("-neq",
                material.getUrlArgument().forCommandline(),
                git(workDir, "config", "remote.origin.url"));
    }

    private BuildCommand isBranchChanged(String workDir) {
        return test("-neq",
                material.branchWithDefault(),
                git(workDir, "rev-parse", "--abbrev-ref", "HEAD"));
    }


    private BuildCommand cmdClone(String workDir, int cloneDepth) {
        ArrayList<String> cloneArgs = new ArrayList<>();
        cloneArgs.add("clone");
        cloneArgs.add("--no-checkout");
        cloneArgs.add(format("--branch=%s", material.branchWithDefault()));
        if (material.isShallowClone()) {
            cloneArgs.add(format("--depth=%s", String.valueOf(cloneDepth)));
        }
        cloneArgs.add(material.getUrlArgument().forCommandline());
        cloneArgs.add(workDir);
        return git(null, cloneArgs.toArray(new String[cloneArgs.size()]));
    }

    private BuildCommand isNotRepository(String workDir) {
        return test("-nd", new File(workDir, ".git").getPath());
    }

    private BuildCommand git(String workingDir, String... args) {
        return exec("git", args).setWorkingDirectory(workingDir);
    }
}
