/*
 * Copyright 2021 ThoughtWorks, Inc.
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
package com.thoughtworks.go.tfssdk;

import java.io.File;
import java.util.List;

import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.Revision;
import com.thoughtworks.go.domain.materials.tfs.TfsCommand;
import com.thoughtworks.go.tfssdk14.TfsSDKCommand;
import com.thoughtworks.go.util.command.CommandArgument;

/**
 * Adapter class that sets the thread context classloader to appropriate value
 * before delegation to the actual implementation workhorse.
 */
public class TfsSDKCommandTCLAdapter implements TfsCommand {
    private final TfsSDKCommand sdkCommandDelegate;

    public TfsSDKCommandTCLAdapter(String materialFingerprint, CommandArgument url, String domain, String userName, String password, String workspace, String projectPath) {
        sdkCommandDelegate = new TfsSDKCommand(materialFingerprint, url, domain, userName, password, workspace, projectPath);
    }

    @Override public void checkout(File workingDir, Revision revision) {
        ClassLoader tccl = changeClassLoader();
        try {
            sdkCommandDelegate.init();
            sdkCommandDelegate.checkout(workingDir, revision);
        } finally {
            sdkCommandDelegate.destroy();
            resetClassLoader(tccl);
        }
    }

    @Override
    public void checkConnection() {
        ClassLoader tccl = changeClassLoader();
        try {
            sdkCommandDelegate.init();
            sdkCommandDelegate.checkConnection();
        } finally {
            sdkCommandDelegate.destroy();
            resetClassLoader(tccl);
        }
    }

    @Override public List<Modification> latestModification(File workDir) {
        ClassLoader tccl = changeClassLoader();
        try {
            sdkCommandDelegate.init();
            return sdkCommandDelegate.latestModification(workDir);
        } finally {
            sdkCommandDelegate.destroy();
            resetClassLoader(tccl);
        }
    }

    @Override public List<Modification> modificationsSince(File workDir, Revision revision) {
        ClassLoader tccl = changeClassLoader();
        try {
            sdkCommandDelegate.init();
            return sdkCommandDelegate.modificationsSince(workDir, revision);
        } finally {
            sdkCommandDelegate.destroy();
            resetClassLoader(tccl);
        }
    }


    private void resetClassLoader(ClassLoader tccl) {
        Thread.currentThread().setContextClassLoader(tccl);
    }

    private ClassLoader changeClassLoader() {
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        resetClassLoader(this.getClass().getClassLoader());
        return tccl;
    }
}
