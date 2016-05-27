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

package com.thoughtworks.go.validation;

import java.io.IOException;

import com.thoughtworks.go.agent.ChecksumValidationPublisher;
import com.thoughtworks.go.domain.ArtifactMd5Checksums;
import com.thoughtworks.go.util.StringUtil;

public class ChecksumValidator {

    private final ArtifactMd5Checksums artifactMd5Checksums;

    public ChecksumValidator(ArtifactMd5Checksums artifactMd5Checksums) {
        this.artifactMd5Checksums = artifactMd5Checksums;
    }

    public void validate(String effectivePath, String artifactMD5, ChecksumValidationPublisher checksumValidationPublisher) throws IOException {
        if (artifactMd5Checksums == null) {
            checksumValidationPublisher.md5ChecksumFileNotFound();
            return;
        }
        String expectedMd5 = artifactMd5Checksums.md5For(effectivePath);
        if (StringUtil.isBlank(expectedMd5)) {
            checksumValidationPublisher.md5NotFoundFor(effectivePath);
            return;
        }
        if (expectedMd5.equals(artifactMD5)) {
            checksumValidationPublisher.md5Match(effectivePath);
        } else {
            checksumValidationPublisher.md5Mismatch(effectivePath);
        }
    }
}
