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
import static com.thoughtworks.go.util.command.ConsoleLogTags.PREP;
import static com.thoughtworks.go.util.command.ConsoleLogTags.PREP_TAGS;
import static java.lang.String.format;

public class GitMaterialUpdater {
    private GitMaterial material;

    public GitMaterialUpdater(GitMaterial material) {
        this.material = material;
    }

    private static BuildCommand git(String... args) {
        return exec("git", PREP_TAGS, args);
    }

    public BuildCommand updateTo(String baseDir, RevisionContext revisionContext) {
        Revision revision = revisionContext.getLatestRevision();
        String workingDir = material.workingdir(new File(baseDir)).getPath();
        UrlArgument url = material.getUrlArgument();
        return compose(
                echoWithPrefix(PREP, "Start updating %s at revision %s from %s", material.updatingTarget(), revision.getRevision(), url.forDisplay()),
                secret(url.forCommandline(), url.forDisplay()),
                cloneIfNeeded(workingDir, revisionContext.numberOfModifications() + 1),
                fetchRemote(workingDir),
                unshallowIfNeeded(workingDir, revision, new Integer[]{GitMaterial.UNSHALLOW_TRYOUT_STEP, Integer.MAX_VALUE}),
                resetWorkingCopy(workingDir, revision),
                echoWithPrefix(PREP, "Done.\n"));
    }

    private BuildCommand fetchRemote(String workingDir) {
        return compose(
                echo(PREP, "[GIT] Fetching changes"),
                git("fetch", "origin", "--prune").setWorkingDirectory(workingDir),
                git("gc", "--auto").setWorkingDirectory(workingDir));
    }

    private BuildCommand resetWorkingCopy(String workingDir, Revision revision) {
        return compose(
                echo(PREP, "[GIT] Reset working directory %s", workingDir),
                echo(PREP, "[GIT] Updating working copy to revision %s", revision.getRevision()),
                cleanupUnversionedFiles(workingDir),
                resetHard(workingDir, revision),
                updateSubmodules(workingDir),
                cleanupUnversionedFiles(workingDir));
    }

    private BuildCommand updateSubmodules(String workingDir) {
        return compose(
                echo(PREP, "[GIT] Removing modified files in submodules"),
                git("submodule", "foreach", "--recursive", "git", "checkout", "."),
                echo(PREP, "[GIT] Updating git sub-modules"),
                git("submodule", "init"),
                git("submodule", "sync"),
                git("submodule", "foreach", "--recursive", "git", "submodule", "sync"),
                git("submodule", "update"),
                echo(PREP, "[GIT] Git sub-module status"),
                git("submodule", "status"))
                .setTest(hasSubmodules(workingDir))
                .setWorkingDirectoryRecursively(workingDir);
    }

    private BuildCommand resetHard(String workingDir, Revision revision) {
        return git("reset", "--hard", revision.getRevision())
                .setWorkingDirectory(workingDir);
    }

    private BuildCommand cleanupUnversionedFiles(String workingDir) {
        return compose(
                echo(PREP, "[GIT] Cleaning all unversioned files in working copy"),
                git("submodule", "foreach", "--recursive", "git", "clean", "-fdd")
                        .setTest(hasSubmodules(workingDir)),
                git("clean", "-dff"))
                .setWorkingDirectoryRecursively(workingDir);
    }

    private BuildCommand hasSubmodules(String workingDir) {
        return test("-f", new File(workingDir, ".gitmodules").getPath());
    }

    private BuildCommand unshallowIfNeeded(String workingDir, Revision revision, Integer[] steps) {
        if (steps.length == 0) {
            return noop();
        }
        int depth = steps[0];
        return compose(
                compose(
                        echo(PREP, "[GIT] Unshallowing repository with depth %d", depth),
                        git("fetch", "origin", format("--depth=%d", depth)).setWorkingDirectory(workingDir),
                        unshallowIfNeeded(workingDir, revision, Arrays.copyOfRange(steps, 1, steps.length))
                ).setTest(revisionNotExists(workingDir, revision))
        ).setTest(isShallow(workingDir));
    }

    private BuildCommand revisionNotExists(String workingDir, Revision revision) {
        return test("-neq", "commit",
                git("cat-file", "-t", revision.getRevision()).setWorkingDirectory(workingDir));
    }

    private BuildCommand isShallow(String workingDir) {
        return test("-f", new File(workingDir, ".git/shallow").getPath());
    }

    private BuildCommand cloneIfNeeded(String workDir, int cloneDepth) {
        return compose(
                mkdirs(workDir).setTest(test("-nd", workDir)),
                cleanWorkingDir(workDir),
                cmdClone(workDir, cloneDepth));
    }

    private BuildCommand cleanWorkingDir(String workDir) {
        return compose(
                cleandir(workDir).setTest(isNotRepository(workDir)),
                cleandir(workDir).setTest(isRepoUrlChanged(workDir)),
                cleandir(workDir).setTest(isBranchChanged(workDir)),
                material.isShallowClone() ? noop() : cleandir(workDir).setTest(isShallow(workDir))
        ).setTest(test("-d", workDir));
    }

    private BuildCommand isRepoUrlChanged(String workDir) {
        return test("-neq",
                material.getUrlArgument().forCommandline(),
                git("config", "remote.origin.url").setWorkingDirectory(workDir));
    }

    private BuildCommand isBranchChanged(String workDir) {
        return test("-neq",
                material.branchWithDefault(),
                git("rev-parse", "--abbrev-ref", "HEAD").setWorkingDirectory(workDir));
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
        return git(cloneArgs.toArray(new String[cloneArgs.size()]))
                .setTest(isNotRepository(workDir));
    }

    private BuildCommand isNotRepository(String workDir) {
        return test("-nd", new File(workDir, ".git").getPath());
    }
}
