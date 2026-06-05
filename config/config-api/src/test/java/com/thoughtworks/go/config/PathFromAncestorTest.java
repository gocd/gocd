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
package com.thoughtworks.go.config;

import org.junit.jupiter.api.Test;

import java.util.List;

import static com.thoughtworks.go.config.CaseInsensitiveString.cis;
import static org.assertj.core.api.Assertions.assertThat;

public class PathFromAncestorTest {
    @Test
    public void shouldReturnOnlyPath_excludingActualAncestorNode() {
        PathFromAncestor path = new PathFromAncestor(cis("grand-parent/parent/child"));
        assertThat(path.pathToAncestor()).isEqualTo(List.of(cis("child"), cis("parent")));
    }

    @Test
    public void shouldReturnPath_includingActualAncestorNode() {
        PathFromAncestor path = new PathFromAncestor(cis("grand-parent/parent/child"));
        assertThat(path.pathIncludingAncestor()).isEqualTo(List.of(cis("child"), cis("parent"), cis("grand-parent")));
    }

    @Test
    public void shouldUnderstandAncestorName() {
        PathFromAncestor path = new PathFromAncestor(cis("grand-parent/parent/child"));
        assertThat(path.getAncestorName()).isEqualTo(cis("grand-parent"));
    }

    @Test
    public void shouldUnderstandDirectParentName() {
        PathFromAncestor path = new PathFromAncestor(cis("grand-parent/parent/child"));
        assertThat(path.getDirectParentName()).isEqualTo(cis("child"));
    }

    @Test
    public void shouldReturnPath_asToString() {//this is important because its used in xml fragment that is spit out on job-console
        assertThat(new PathFromAncestor(cis("grand-parent/parent/child")).toString()).isEqualTo("grand-parent/parent/child");
    }
}
