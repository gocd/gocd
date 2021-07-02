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
package com.thoughtworks.go.config;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class PathFromAncestorTest {
    @Test
    public void shouldReturnOnlyPath_excludingActualAncestorNode() {
        PathFromAncestor path = new PathFromAncestor(new CaseInsensitiveString("grand-parent/parent/child"));
        assertThat(path.pathToAncestor(), is(Arrays.asList(new CaseInsensitiveString("child"), new CaseInsensitiveString("parent"))));
    }

    @Test
    public void shouldReturnPath_includingActualAncestorNode() {
        PathFromAncestor path = new PathFromAncestor(new CaseInsensitiveString("grand-parent/parent/child"));
        assertThat(path.pathIncludingAncestor(), is(Arrays.asList(new CaseInsensitiveString("child"), new CaseInsensitiveString("parent"), new CaseInsensitiveString("grand-parent"))));
    }

    @Test
    public void shouldUnderstandAncestorName() {
        PathFromAncestor path = new PathFromAncestor(new CaseInsensitiveString("grand-parent/parent/child"));
        assertThat(path.getAncestorName(), is(new CaseInsensitiveString("grand-parent")));
    }

    @Test
    public void shouldUnderstandDirectParentName() {
        PathFromAncestor path = new PathFromAncestor(new CaseInsensitiveString("grand-parent/parent/child"));
        assertThat(path.getDirectParentName(), is(new CaseInsensitiveString("child")));
    }

    @Test
    public void shouldReturnPath_asToString() {//this is important because its used in xml fragment that is spit out on job-console
        assertThat(new PathFromAncestor(new CaseInsensitiveString("grand-parent/parent/child")).toString(), is("grand-parent/parent/child"));
    }
}
