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
package com.thoughtworks.go.domain.materials.git;

import de.skuzzle.semantic.Version;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class GitVersionTest {

    @Test
    public void shouldParseGitVersions() {
        assertThat(GitVersion.parse("git version 1.6.0.2").getVersion())
                .isEqualTo(Version.create(1, 6, 0));

        assertThat(GitVersion.parse("git version 1.5.4.3").getVersion())
                .isEqualTo(Version.create(1, 5, 4));

        assertThat(GitVersion.parse("git version 1.6.0.2.1172.ga5ed0").getVersion())
                .isEqualTo(Version.create(1, 6, 0));

        assertThat(GitVersion.parse("git version 1.6.1 (ubuntu 18.04.1)").getVersion())
                .isEqualTo(Version.create(1, 6, 1));
    }

    @Test
    public void shouldThowExceptionWhenGitVersionCannotBeParsed() {
        String invalidGitVersion = "git version ga5ed0asdasd.ga5ed0";
        assertThatExceptionOfType(RuntimeException.class)
                .isThrownBy(() -> GitVersion.parse(invalidGitVersion))
                .withMessage("cannot parse git version : " + invalidGitVersion);
    }

    @Test
    void shouldReturnTrueIfVersionIsGreaterThanOrEqualToOneDotNine() {
        GitVersion version = GitVersion.parse("git version 1.9.0");
        assertThat(version.isMinimumSupportedVersionOrHigher()).isTrue();

        version = GitVersion.parse("git version 1.10.0");
        assertThat(version.isMinimumSupportedVersionOrHigher()).isTrue();
    }

    @Test
    void shouldReturnFalseIfVersionLowerThan1Dot9() {
        GitVersion version = GitVersion.parse("git version 1.5.0.1");
        assertThat(version.isMinimumSupportedVersionOrHigher()).isFalse();
    }

    @Test
    void shouldReturnTrueIfVersionRequiresSubmoduleCommandFix() {
        GitVersion version = GitVersion.parse("git version 2.22.0");
        assertThat(version.requiresSubmoduleCommandFix()).isTrue();
    }
}
