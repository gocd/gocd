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

import com.thoughtworks.go.util.DateUtils;
import org.junit.jupiter.api.Test;

import java.text.SimpleDateFormat;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

public class GitModificationParserTest {

    GitModificationParser parser = new GitModificationParser();

    private void simulateOneComment() {
        parser.processLine("commit 4e55d27dc7aad26dadb02a33db0518cb5ec54888");
        parser.processLine("Author: Cruise Developer <cruise@cruise-sf3.(none)>");
        parser.processLine("Date:   2009-08-11 13:08:51 -0700");
    }

    private void simulateOneCommentWithUnparseableDate(String format) {
        parser.processLine("commit 4e55d27dc7aad26dadb02a33db0518cb5ec54888");
        parser.processLine("Author: Cruise Developer <cruise@cruise-sf3.(none)>");
        if (format == "RFC822") {
            parser.processLine("Date:   Sun Feb 7 06:29:14 2106 -12296855");
        } else {
            parser.processLine("Date:   2106-02-07 06:28:51 -111309508");
        }
    }

    @Test
    public void shouldCreateModificationForEachCommit() {
        simulateOneComment();
        assertThat(parser.getModifications().get(0).getRevision()).isEqualTo("4e55d27dc7aad26dadb02a33db0518cb5ec54888");
    }

    @Test
    public void shouldHaveCommitterAsAuthor() {
        simulateOneComment();
        assertThat(parser.getModifications().get(0).getUserDisplayName()).isEqualTo("Cruise Developer <cruise@cruise-sf3.(none)>");
    }

    @Test
    public void shouldHaveCommitDate() {
        simulateOneComment();
        assertThat(
                parser.getModifications().get(0).getModifiedTime()).isEqualTo(DateUtils.parseISO8601("2009-08-11 13:08:51 -0700"));
    }

    @Test
    public void shouldSupportUnparseableCommitDateISO8601() {
        simulateOneCommentWithUnparseableDate("ISO8601");

        Date expectedDate = parser.getModifications().get(0).getModifiedTime();
        Date parsedDate = DateUtils.parseISO8601("2106-02-07 06:28:51 -111309508");

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");

        assertThat(formatter.format(expectedDate)).isEqualTo(formatter.format(parsedDate));
    }

    @Test
    public void shouldSupportUnparseableCommitDateRFC822() {
        simulateOneCommentWithUnparseableDate("RFC822");

        Date expectedDate = parser.getModifications().get(0).getModifiedTime();
        Date parsedDate = DateUtils.parseRFC822("Sub Feb 7 06:29:14 2106 -12296855");

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");

        assertThat(formatter.format(expectedDate)).isEqualTo(formatter.format(parsedDate));
    }

    @Test
    public void shouldSupportMultipleLineComments() {
        simulateOneComment();
        parser.processLine("");
        parser.processLine("    My Comment");
        parser.processLine("    line 2");
        parser.processLine("");
        assertThat(
                parser.getModifications().get(0).getComment()).isEqualTo("My Comment\nline 2");
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
                parser.getModifications().get(0).getComment()).isEqualTo("My Comment\n\nline 2");
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
                parser.getModifications().get(0).getComment()).isEqualTo("My Comment 1");
        assertThat(
                parser.getModifications().get(1).getComment()).isEqualTo("My Comment 2");
    }
}
