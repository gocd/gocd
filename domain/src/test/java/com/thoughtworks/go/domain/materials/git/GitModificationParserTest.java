/*
 * Copyright Thoughtworks, Inc.
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

import com.thoughtworks.go.util.Dates;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class GitModificationParserTest {

    GitModificationParser parser = new GitModificationParser();

    private void simulateOneComment() {
        parser.processLine("commit 4e55d27dc7aad26dadb02a33db0518cb5ec54888");
        parser.processLine("Author: Cruise Developer <cruise@cruise-sf3.(none)>");
        parser.processLine("Date:   2009-08-11T13:08:51-07:00");
    }

    @Test
    public void shouldCreateModificationForEachCommit() {
        simulateOneComment();
        assertThat(parser.getModifications().getFirst().getRevision()).isEqualTo("4e55d27dc7aad26dadb02a33db0518cb5ec54888");
    }

    @Test
    public void shouldHaveCommitterAsAuthor() {
        simulateOneComment();
        assertThat(parser.getModifications().getFirst().getUserDisplayName()).isEqualTo("Cruise Developer <cruise@cruise-sf3.(none)>");
    }

    @Test
    public void shouldHaveCommitDate() {
        simulateOneComment();
        assertThat(
                parser.getModifications().getFirst().getModifiedTime()).isEqualTo(Dates.parseIso8601StrictOffset("2009-08-11T13:08:51-07:00"));
    }

    @Test
    public void shouldHaveComment() {
        simulateOneComment();
        parser.processLine("");
        parser.processLine("    My Comment");
        parser.processLine("");
        assertThat(
                parser.getModifications().getFirst().getComment()).isEqualTo("My Comment");
    }

    @Test
    public void shouldSupportMultipleLineComments() {
        simulateOneComment();
        parser.processLine("");
        parser.processLine("    My Comment");
        parser.processLine("    line 2");
        parser.processLine("");
        assertThat(
                parser.getModifications().getFirst().getComment()).isEqualTo("My Comment\nline 2");
    }

     @Test
    public void shouldSupportMultipleLineCommentsWithEmptyLines() {
        simulateOneComment();
        parser.processLine("");
        parser.processLine("    My Comment");
         parser.processLine("    ");
        parser.processLine("    line 2");
        parser.processLine("");
        assertThat(
                parser.getModifications().getFirst().getComment()).isEqualTo("My Comment\n\nline 2");
    }

    @Test
    public void shouldSupportMultipleModifications() {
        simulateOneComment();
        parser.processLine("");
        parser.processLine("    My Comment 1");
        simulateOneComment();
        parser.processLine("");
        parser.processLine("    My Comment 2");
         parser.processLine("");
        assertThat(
                parser.getModifications().getFirst().getComment()).isEqualTo("My Comment 1");
        assertThat(
                parser.getModifications().getLast().getComment()).isEqualTo("My Comment 2");
    }
}
