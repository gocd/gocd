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

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MessageLevelTest {
    @Test
    public void testShouldMapDisplayNameToType() {
        assertEquals(MessageLevel.DEBUG, MessageLevel.getLevelForPriority("debug"));
        assertEquals(MessageLevel.INFO, MessageLevel.getLevelForPriority("info"));
        assertEquals(MessageLevel.WARN, MessageLevel.getLevelForPriority("warn"));
        assertEquals(MessageLevel.ERROR, MessageLevel.getLevelForPriority("error"));
    }

    @Test
    public void testShouldMapDisplayNameToTypeIgnoringCase() {
        assertEquals(MessageLevel.DEBUG, MessageLevel.getLevelForPriority("Debug"));
        assertEquals(MessageLevel.INFO, MessageLevel.getLevelForPriority("Info"));
        assertEquals(MessageLevel.WARN, MessageLevel.getLevelForPriority("Warn"));
        assertEquals(MessageLevel.ERROR, MessageLevel.getLevelForPriority("Error"));
    }

    @Test
    public void testShouldGetDisplayNameForType() {
        assertEquals("debug", MessageLevel.DEBUG.getDisplayName());
        assertEquals("info", MessageLevel.INFO.getDisplayName());
        assertEquals("warn", MessageLevel.WARN.getDisplayName());
        assertEquals("error", MessageLevel.ERROR.getDisplayName());
    }

    @Test
    public void testShouldShowDisplayNameAsStringRepresentation() {
        assertEquals("debug", MessageLevel.DEBUG.toString());
        assertEquals("info", MessageLevel.INFO.toString());
        assertEquals("warn", MessageLevel.WARN.toString());
        assertEquals("error", MessageLevel.ERROR.toString());
    }
}
