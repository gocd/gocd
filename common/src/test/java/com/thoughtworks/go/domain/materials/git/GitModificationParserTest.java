/*
 * Copyright 2020 ThoughtWorks, Inc.
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

import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.junit5.FileSource;
import com.thoughtworks.go.util.DateUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


class GitModificationParserTest {

    @Test
    void shouldReturnEmptyListPassedANull() {
        List<Modification> list = new GitModificationParser().parse(null);

        assertThat(list).isEmpty();
    }

    @Test
    void shouldReturnEmptyListPassedAnEmptyString() {
        List<Modification> list = new GitModificationParser().parse("");

        assertThat(list).isEmpty();
    }

    @ParameterizedTest
    @FileSource(files = "/git/log.yaml")
    void shouldParseTheYaml(String inputYaml) {
        List<Modification> modifications = new GitModificationParser().parse(inputYaml);

        assertThat(modifications).hasSize(2);

        Modification firstModification = modifications.get(0);
        assertThat(firstModification.getRevision()).isEqualTo("9fef97af1cd3a8920fefe5f656cb5795d690ee1b");
        assertThat(firstModification.getUserName()).isEqualTo("Chris Turner");
        assertThat(firstModification.getEmailAddress()).isEqualTo("cturner@thoughtworks.com");
        assertThat(firstModification.getComment()).isEqualTo("Added remote file");
        assertThat(firstModification.getModifiedTime()).isEqualTo(DateUtils.parseISO8601("2009-02-11 17:26:36 -0800"));
        assertThat(firstModification.getAdditionalDataMap())
                .hasSize(8)
                .containsEntry("subject", "Added remote file")
                .containsEntry("signed", "N")
                .containsEntry("signerName", null)
                .containsEntry("signingKey", null)
                .containsEntry("signingMessage", "This is signing message with\n" +
                        "multiple lines")
                .containsEntry("committerName", "Chris Turner")
                .containsEntry("committerEmail", "cturner@thoughtworks.com")
                .containsEntry("commitDate", "2009-02-11 17:26:36 -0800");

        Modification secondModification = modifications.get(1);
        assertThat(secondModification.getRevision()).isEqualTo("ewehsjf232349fef97af1cd3a8920fefe5f656cb57");
        assertThat(secondModification.getUserName()).isEqualTo("Bob Ford");
        assertThat(secondModification.getEmailAddress()).isEqualTo("bford@thoughtworks.com");
        assertThat(secondModification.getComment()).isEqualTo("Initial commit\n" +
                "\n" +
                "  - Added remote file\n" +
                "  - Added .gitignore");

        assertThat(secondModification.getModifiedTime()).isEqualTo(DateUtils.parseISO8601("2009-02-11 17:26:36 -0800"));
        assertThat(secondModification.getAdditionalDataMap())
                .hasSize(8)
                .containsEntry("subject", "Initial commit")
                .containsEntry("signed", "N")
                .containsEntry("signerName", null)
                .containsEntry("signingKey", null)
                .containsEntry("signingMessage", null)
                .containsEntry("committerName", "Chris Turner")
                .containsEntry("committerEmail", "cturner@thoughtworks.com")
                .containsEntry("commitDate", "2009-02-11 17:26:36 -0800");
    }
}
