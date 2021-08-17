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
package com.thoughtworks.go.plugin.access.common.models;

import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ImageDeserializerTest {

    @Test
    public void shouldDeserializeFromJSON() throws Exception {
        com.thoughtworks.go.plugin.domain.common.Image image = new ImageDeserializer().fromJSON("{\"content_type\":\"image/png\",\"data\":\"Zm9vYmEK\"}");
        assertThat(image.getContentType(), is("image/png"));
        assertThat(image.getData(), is("Zm9vYmEK"));
        assertThat(image.getHash(), is("a67bb2a01e8bbf37082c1288c7bfcc0e33bcc6ae65861df19b94e36600baa5a0"));
    }
}