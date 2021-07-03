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
package com.thoughtworks.go.plugin.domain.common;


import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class ImageTest {
    @Test
    void convertsToDataUri() {
        String encodedString = Base64.getEncoder().encodeToString("asdf".getBytes(StandardCharsets.UTF_8));
        String dataURI = new Image("image/png", encodedString, "sha-256").toDataURI();
        assertThat(dataURI, is("data:image/png;base64," + encodedString));
    }

    @Test
    void convertsToByteData() {
        String encodedString = Base64.getEncoder().encodeToString("asdf".getBytes(StandardCharsets.UTF_8));
        Image image = new Image("image/png", encodedString, "sha-256");
        assertThat(image.getDataAsBytes(), is("asdf".getBytes(StandardCharsets.UTF_8)));
    }
}
