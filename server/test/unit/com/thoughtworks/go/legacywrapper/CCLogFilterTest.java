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

package com.thoughtworks.go.legacywrapper;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CCLogFilterTest {
    @Test
    public void testShouldAcceptFailedLogFile() {
        assertTrue(new CCLogFilter().accept(null, "log19990505080808.xml"));
    }

    @Test
    public void testShouldAcceptPassedLogFile() {
        assertTrue(new CCLogFilter().accept(null, "log19990505080808Lbuild.123.xml"));
    }

    @Test
    public void testShouldNotAcceptFileIfDoesnotEndswithXml() {
        assertFalse(new CCLogFilter().accept(null, "file.notxml"));
    }

    @Test
    public void testShouldNotAcceptFileIfDoesnotStartwithLog() {
        assertFalse(new CCLogFilter().accept(null, "filelog.xml"));
    }

    @Test
    public void testShouldNotAcceptFileIfLengthShorterThan21() {
        assertFalse(new CCLogFilter().accept(null, "log1234.xml"));
        assertFalse(new CCLogFilter().accept(null, "log.xml"));
    }

    @Test
    public void testShouldAcceptZippedLogFile() throws Exception {
        assertTrue(new CCLogFilter().accept(null, "log19990505080808.xml.gz"));
        assertTrue(new CCLogFilter().accept(null, "log19990505080808Lbuild.123.xml.gz"));
    }
}
