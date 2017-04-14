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

package com.thoughtworks.go.domain.materials.perforce;

import com.thoughtworks.go.config.materials.perforce.P4Material;
import com.thoughtworks.go.domain.BuildCommand;
import com.thoughtworks.go.domain.materials.RevisionContext;
import com.thoughtworks.go.util.GoConstants;

import java.io.File;

import static com.thoughtworks.go.domain.BuildCommand.*;
import static java.lang.Long.parseLong;
import static java.lang.String.format;

public class P4MaterialUpdater {
    private P4Material material;

    public P4MaterialUpdater(P4Material material) {
        this.material = material;
    }

    public BuildCommand updateTo(String baseDir, RevisionContext revisionContext) {
        String workingDir = material.workingdir(new File(baseDir)).getPath();
        String revision = revisionContext.getLatestRevision().getRevision();
        String clientName = material.clientName(new File(workingDir));

        return compose(
                secret(material.getPassword()),
                export("P4PORT", material.getServerAndPort(), false),
                export("P4USER", material.getUserName(), false),
                export("P4PASSWD", material.getPassword(), true),
                export("P4CLIENT", clientName, false),
                constructClient(workingDir, clientName),
                cleanWorkingDir(workingDir),
                echo(format("[%s] Start updating %s at revision %s from %s", GoConstants.PRODUCT_NAME, material.updatingTarget(), revision, material.getServerAndPort())),
                sync(workingDir, revision),
                echo(format("[%s] Done.\n", GoConstants.PRODUCT_NAME))

        );
    }

    private BuildCommand constructClient(String workingDir, String clientName) {
        String clientArgs = "Client: " + clientName + "\n\n"
                + "Root: " + new File(workingDir).getAbsolutePath() + "\n\n"
                + "Options: clobber rmdir\n\n"
                + "LineEnd: local\n\n"
                + "View:\n"
                + material.p4view(clientName);
        return exec("p4", "client", "-i").setExecInput(clientArgs);
    }

    private BuildCommand sync(String workingDir, String revision) {
        return exec("p4", "-d", workingDir, "sync", "@" + parseLong(revision));
    }

    private BuildCommand cleanWorkingDir(String workingDir) {
        return compose(
                echo(format("[%s] Cleaning up working directory", GoConstants.PRODUCT_NAME)),
                exec("p4", "-d", workingDir, "clean")
        );
    }
}
