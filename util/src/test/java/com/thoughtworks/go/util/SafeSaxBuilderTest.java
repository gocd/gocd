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

package com.thoughtworks.go.util;

import org.jdom2.input.JDOMParseException;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SafeSaxBuilderTest {
    @Test
    public void shouldDisableDocTypeDeclarationsWhenValidatingXmlDocuments() throws Exception {
        try (InputStream content = xxeFileContent()) {
            assertThatThrownBy(() -> new SafeSaxBuilder().build(content))
                .isInstanceOf(JDOMParseException.class)
                .hasMessageContaining("DOCTYPE is disallowed when the feature \"http://apache.org/xml/features/disallow-doctype-decl\" set to true");
        }
    }

    private InputStream xxeFileContent() {
        return Objects.requireNonNull(this.getClass().getResourceAsStream("/data/xml-with-xxe.xml"));
    }
}