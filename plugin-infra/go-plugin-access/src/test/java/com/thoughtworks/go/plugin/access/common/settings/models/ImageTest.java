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
package com.thoughtworks.go.plugin.access.common.settings.models;

import com.thoughtworks.go.plugin.access.common.models.Image;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ImageTest {
    @Test
    public void convertsToDataUri() throws Exception {
        String encodedString = Base64.getEncoder().encodeToString("asdf".getBytes(StandardCharsets.UTF_8));
        String dataURI = new Image("foo", encodedString).toDataURI();
        assertThat(dataURI, is("data:foo;base64," + encodedString));
    }

    @Test
    public void convertsToByteData() throws Exception {
        Image image = new Image("foo", Base64.getEncoder().encodeToString("asdf".getBytes(StandardCharsets.UTF_8)));
        assertThat(image.getDataAsBytes(), is("asdf".getBytes(StandardCharsets.UTF_8)));
    }

}
