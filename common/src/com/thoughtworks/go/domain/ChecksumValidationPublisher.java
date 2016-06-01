/*************************GO-LICENSE-START*********************************
 * Copyright 2016 ThoughtWorks, Inc.
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

import com.thoughtworks.go.work.GoPublisher;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

public class ChecksumValidationPublisher implements com.thoughtworks.go.agent.ChecksumValidationPublisher, Serializable {
    private Set<String> md5NotFoundPaths = new HashSet<>();
    private Set<String> md5MismatchPaths = new HashSet<>();
    private boolean md5ChecksumFileWasNotFound;

    public void md5Match(String filePath) {
    }

    public void md5Mismatch(String filePath) {
        md5MismatchPaths.add(filePath);
    }

    public void md5NotFoundFor(String filePath) {
        md5NotFoundPaths.add(filePath);
    }

    public void md5ChecksumFileNotFound() {
        md5ChecksumFileWasNotFound = true;
    }

    public void publish(int httpCode, File artifact, GoPublisher goPublisher) {
        if (!this.md5MismatchPaths.isEmpty()) {
            String mismatchedFilePath = md5MismatchPaths.iterator().next();
            goPublisher.consumeLineWithPrefix(
                    String.format("[ERROR] Verification of the integrity of the artifact [%s] failed. The artifact file on the server may have changed since its original upload.", mismatchedFilePath));
            throw new RuntimeException(String.format("Artifact download failed for [%s]", mismatchedFilePath));
        }
        for (String md5NotFoundPath : md5NotFoundPaths) {
            goPublisher.consumeLineWithPrefix(String.format("[WARN] The md5checksum value of the artifact [%s] was not found on the server. Hence, Go could not verify the integrity of its contents.", md5NotFoundPath));
        }

        if (httpCode == HttpServletResponse.SC_NOT_MODIFIED) {
            goPublisher.consumeLineWithPrefix("Artifact is not modified, skipped fetching it");
        }

        if (httpCode == HttpServletResponse.SC_OK) {
            if (md5NotFoundPaths.size() > 0 || md5ChecksumFileWasNotFound) {
                goPublisher.consumeLineWithPrefix(String.format("Saved artifact to [%s] without verifying the integrity of its contents.", artifact));
            } else {
                goPublisher.consumeLineWithPrefix(String.format("Saved artifact to [%s] after verifying the integrity of its contents.", artifact));
            }
        }
    }

}
