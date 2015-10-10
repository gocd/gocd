/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.server.domain;

import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.thoughtworks.go.helper.PipelineMaterialModificationMother.modification;
import com.thoughtworks.go.domain.PipelineTimelineEntry;
import static org.hamcrest.core.Is.is;
import org.joda.time.DateTime;
import static org.junit.Assert.assertThat;
import org.junit.Test;

public class PipelineMaterialModificationTest {

    @Test public void shouldThrowNPEIfNullIsPassedIn() throws Exception {
        try {
            modification(new ArrayList<String>(), 1, "123").compareTo(null);
            fail("Should throw NPE. That is the Comparable's contract.");
        } catch (NullPointerException expected) {
            assertThat(expected.getMessage(), is("Cannot compare this object with null"));
        }
    }

    @Test public void shouldReturn0IfComparedToItself() throws Exception {
        DateTime now = new DateTime();
        PipelineTimelineEntry self = modification(Arrays.asList("flyweight"), 1, "123", now);
        assertThat(self.compareTo(self), is(0));

        PipelineTimelineEntry another = modification(Arrays.asList("flyweight"), 1, "123", now);
        assertThat(self.compareTo(another), is(0));
        assertThat(another.compareTo(self), is(0));
    }

    @Test public void shouldThrowExceptionIfIfComparedToADifferentClassObject() throws Exception {
        try {
            modification(Arrays.asList("flyweight"), 1, "123").compareTo(new Object());
            fail("Should throw up.");
        } catch (RuntimeException expected) {
        }
    }

    @Test public void shouldCompareWhenThisModificationOccuredBeforeTheOtherModification() throws Exception {
        PipelineTimelineEntry modification = modification(Arrays.asList("flyweight"), 1, "123", new DateTime());
        PipelineTimelineEntry that = modification(2, Arrays.asList("flyweight"), Arrays.asList(new DateTime().plusMinutes(1)), 1, "123");

        assertThat(modification.compareTo(that), is(-1));
        assertThat(that.compareTo(modification), is(1));
    }

    @Test public void shouldCompareModsWithMultipleMaterials() throws Exception {
        List<String> materials = Arrays.asList("flyweight", "another");
        DateTime base = new DateTime();

        PipelineTimelineEntry modification = modification(1, materials, Arrays.asList(base.plusMinutes(1), base.plusMinutes(3)), 1, "123");
        PipelineTimelineEntry that = modification(2, materials, Arrays.asList(base.plusMinutes(4), base.plusMinutes(2)), 1, "123");

        assertThat(modification.compareTo(that), is(-1));
        assertThat(that.compareTo(modification), is(1));
    }

    @Test public void shouldCompareModsWithMultipleMaterialsWithOneMaterialNotChanged() throws Exception {
        List<String> materials = Arrays.asList("flyweight", "another");
        DateTime base = new DateTime();

        PipelineTimelineEntry modification = modification(1, materials, Arrays.asList(base, base.plusMinutes(3)), 1, "123");
        PipelineTimelineEntry that = modification(2, materials, Arrays.asList(base, base.plusMinutes(2)), 1, "123");

        assertThat(modification.compareTo(that), is(1));
        assertThat(that.compareTo(modification), is(-1));
    }

    @Test public void shouldCompareModsWithNoMaterialsChanged() throws Exception {
        List<String> materials = Arrays.asList("flyweight", "another");
        DateTime base = new DateTime();

        PipelineTimelineEntry modification = modification(1, materials, Arrays.asList(base, base.plusMinutes(3)), 1, "123", "pipeline");
        PipelineTimelineEntry that = modification(2, materials, Arrays.asList(base, base.plusMinutes(3)), 2, "123", "pipeline");

        assertThat(modification.compareTo(that), is(-1));
        assertThat(that.compareTo(modification), is(1));
    }

    @Test public void shouldBreakTieOnMinimumUsingPipelineCounter() throws Exception {
        List<String> materials = Arrays.asList("first", "second", "third", "fourth");
        DateTime base = new DateTime();

        //Because there is a tie on the lowest value i.e. date 2, use the counter to order
        PipelineTimelineEntry modification = modification(1, materials, Arrays.asList(base, base.plusMinutes(3), base.plusMinutes(2), base.plusMinutes(4)), 1, "123", "pipeline");
        PipelineTimelineEntry that = modification(2, materials, Arrays.asList(base, base.plusMinutes(2), base.plusMinutes(3), base.plusMinutes(2)), 2, "123", "pipeline");

        assertThat(modification.compareTo(that), is(-1));
        assertThat(that.compareTo(modification), is(1));
    }

    @Test public void shouldCompareModsWith4MaterialsWithOneMaterialNotChanged() throws Exception {
        List<String> materials = Arrays.asList("first", "second", "third", "fourth");
        DateTime base = new DateTime();

        PipelineTimelineEntry modification = modification(1, materials, Arrays.asList(base, base.plusMinutes(3), base.plusMinutes(2), base.plusMinutes(4)), 1, "123", "pipeline");
        PipelineTimelineEntry that = modification(2, materials, Arrays.asList(base, base.plusMinutes(2), base.plusMinutes(3), base.plusMinutes(1)), 2, "123", "pipeline");

        assertThat(modification.compareTo(that), is(1));
        assertThat(that.compareTo(modification), is(-1));
    }

    @Test public void shouldCompareModsUsingCounterToBreakTies() throws Exception {
        List<String> materials = Arrays.asList("first", "second", "third");
        DateTime base = new DateTime();

        PipelineTimelineEntry modification = modification(1, materials, Arrays.asList(base, base.plusMinutes(3), base.plusMinutes(2)), 1, "123", "pipeline");
        PipelineTimelineEntry that = modification(2, materials, Arrays.asList(base, base.plusMinutes(2), base.plusMinutes(3)), 2, "123", "pipeline");

        assertThat(modification.compareTo(that), is(-1));
        assertThat(that.compareTo(modification), is(1));
    }

    @Test public void shouldIgnoreExtraMaterialForComparison() throws Exception {
        DateTime base = new DateTime();

        //Ignore the extra material
        PipelineTimelineEntry modification = modification(1, Arrays.asList("first", "second", "third"), Arrays.asList(base, base.plusMinutes(3), base.plusMinutes(2)), 1, "123", "pipeline");
        PipelineTimelineEntry that = modification(2, Arrays.asList("first", "second"), Arrays.asList(base, base.plusMinutes(2)), 2, "123", "pipeline");

        assertThat(modification.compareTo(that), is(1));
        assertThat(that.compareTo(modification), is(-1));

        //Now break the tie using counter and ignore the extra third material
        modification = modification(1, Arrays.asList("first", "second", "third"), Arrays.asList(base, base.plusMinutes(3), base.plusMinutes(2)), 1, "123", "pipeline");
        that = modification(2, Arrays.asList("first", "second"), Arrays.asList(base, base.plusMinutes(3)), 2, "123", "pipeline");

        assertThat(modification.compareTo(that), is(-1));
        assertThat(that.compareTo(modification), is(1));
    }
}
