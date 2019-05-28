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
package com.thoughtworks.go.server.ui;

import com.thoughtworks.go.config.MingleConfig;
import org.junit.Test;

import static org.junit.Assert.assertThat;
import static org.hamcrest.Matchers.is;

public class MingleCardTest {
    @Test
    public void shouldBeEqualToAnotherMingleCardWithSameConfigAndCardNumber(){
        MingleCard card1 = new MingleCard(new MingleConfig("mingle-url", "project-name", null), "#1234");
        MingleCard card2 = new MingleCard(new MingleConfig("mingle-url", "project-name", null), "#1234");
        assertThat(card1.equals(card2), is(true));
    }

    @Test
    public void shouldNotBeEqualToAnotherMingleCardWithSameConfigButDifferentCardNumber(){
        MingleCard card1 = new MingleCard(new MingleConfig("mingle-url", "project-name", null), "#1234");
        MingleCard card2 = new MingleCard(new MingleConfig("mingle-url", "project-name", null), "#5678");
        assertThat(card1.equals(card2), is(false));
    }

    @Test
    public void shouldNotBeEqualToAnotherMingleCardWithDifferentConfigButSameCardNumber(){
        MingleCard card1 = new MingleCard(new MingleConfig("mingle-url", "project-name", null), "#1234");
        MingleCard card2 = new MingleCard(new MingleConfig("mingle-url", "another-project-name", null), "#1234");
        assertThat(card1.equals(card2), is(false));
    }

    @Test
    public void shouldNotBeEqualToNull(){
        assertThat(new MingleCard(new MingleConfig("mingle-url", "project-name", null), "#1234").equals(null), is(false));
    }
}
