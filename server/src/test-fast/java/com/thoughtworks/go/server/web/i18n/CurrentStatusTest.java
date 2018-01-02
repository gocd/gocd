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

package com.thoughtworks.go.server.web.i18n;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class CurrentStatusTest {
    @Test
    public void testShouldReturnInactiveAsTheDefaultStatus() {
        CurrentStatus building = CurrentStatus.getProjectBuildStatus(null);
        assertSame(CurrentStatus.DISCONTINUED, building);
    }

    @Test
    public void testShouldReturnBuildingObjectBaseOnValue() {
        CurrentStatus exptected = CurrentStatus.BUILDING;
        CurrentStatus building = CurrentStatus.getProjectBuildStatus(exptected.getCruiseStatus());
        assertSame(CurrentStatus.BUILDING, building);
        assertEquals("Building", building.getStatus());
    }

    @Test
    public void testShouldReturnBuildingObjectBaseOnValuePlusTimestamp() {
        String exptected = CurrentStatus.BUILDING.getCruiseStatus() + " since 20070420170000";
        CurrentStatus building = CurrentStatus.getProjectBuildStatus(exptected);
        assertSame(CurrentStatus.BUILDING, building);
        assertEquals("Building", building.getStatus());
    }

    @Test
    public void testShouldReturnStatusBootStrappingObjectBaseOnValue() {
        String exptected = CurrentStatus.BOOTSTRAPPING.getCruiseStatus();
        CurrentStatus bootstrapping = CurrentStatus.getProjectBuildStatus(exptected);
        assertSame(CurrentStatus.BOOTSTRAPPING, bootstrapping);
        assertEquals("Bootstrapping", bootstrapping.getStatus());
    }

    @Test
    public void testShouldReturnStatusModificationSetObjectBaseOnValue() {
        String exptected = CurrentStatus.MODIFICATIONSET.getCruiseStatus();
        CurrentStatus modificationset = CurrentStatus.getProjectBuildStatus(exptected);
        assertSame(CurrentStatus.MODIFICATIONSET, modificationset);
        assertEquals("ModificationSet", modificationset.getStatus());
    }

    @Test
    public void testShouldReturnWaitingObjectBaseOnValue() {
        String exptected = CurrentStatus.WAITING.getCruiseStatus();
        CurrentStatus waiting = CurrentStatus.getProjectBuildStatus(exptected);
        assertSame(CurrentStatus.WAITING, waiting);
        assertEquals("Waiting", waiting.getStatus());
    }
}
