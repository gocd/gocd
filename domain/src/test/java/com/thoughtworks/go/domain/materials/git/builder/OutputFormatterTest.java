/*
 * Copyright 2019 ThoughtWorks, Inc.
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

package com.thoughtworks.go.domain.materials.git.builder;

import com.thoughtworks.go.junit5.FileSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;

import static org.assertj.core.api.Assertions.assertThat;

class OutputFormatterTest {
    @Test
    void shouldAddOutputTypeInfo() {
        String format = OutputFormatter
                .newFormatter(DummyOutputType.class)
                .format();

        assertThat(format).isEqualTo("- !!com.thoughtworks.go.domain.materials.git.builder.DummyOutputType");
    }

    @ParameterizedTest
    @FileSource(files = "/git/formatter/with-commit-hash.txt")
    void shouldSelectCommitHash(String expected) {
        String format = OutputFormatter
                .newFormatter(DummyOutputType.class)
                .withCommitHash()
                .format();

        assertThat(format).isEqualTo(expected.trim());
    }

    @ParameterizedTest
    @FileSource(files = "/git/formatter/with-author-name-and-email.txt")
    void shouldSelectAuthorName(String expected) {
        String format = OutputFormatter
                .newFormatter(DummyOutputType.class)
                .withAuthorName()
                .withAuthorEmail()
                .format();

        assertThat(format).isEqualTo(expected.trim());
    }

    @ParameterizedTest
    @FileSource(files = "/git/formatter/with-commit-date.txt")
    void shouldSelectCommitDate(String expected) {
        String format = OutputFormatter
                .newFormatter(DummyOutputType.class)
                .withDate()
                .format();

        assertThat(format).isEqualTo(expected.trim());
    }

    @ParameterizedTest
    @FileSource(files = "/git/formatter/with-commit-subject-and-body.txt")
    void shouldSelectSubjectAndRawBody(String expected) {
        String format = OutputFormatter
                .newFormatter(DummyOutputType.class)
                .withSubject()
                .withRawBody()
                .format();

        assertThat(format).isEqualTo(expected.trim());
    }

    @ParameterizedTest
    @FileSource(files = "/git/formatter/with-additional-info.txt")
    void shouldSelectAdditionalInformation(String expected) {
        String format = OutputFormatter
                .newFormatter(DummyOutputType.class)
                .withAdditionalInfo()
                .format();

        assertThat(format).isEqualTo(expected.trim());
    }
}

class DummyOutputType {

}
