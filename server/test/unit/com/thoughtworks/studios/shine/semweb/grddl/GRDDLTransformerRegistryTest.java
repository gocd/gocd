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

package com.thoughtworks.studios.shine.semweb.grddl;

import javax.xml.transform.Transformer;

import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import org.junit.Test;

public class GRDDLTransformerRegistryTest {

    @Test
    public void canGetATransformerBasedOnAGrddlResource() {
        XSLTTransformerRegistry registry = new XSLTTransformerRegistry();

        Transformer transformer1 = registry.getTransformer("xunit/ant-junit-grddl.xsl");
        Transformer transformer2 = registry.getTransformer("xunit/ant-junit-grddl.xsl");
        Transformer transformer3 = registry.getTransformer("cruise/job-grddl.xsl");

        assertSame(transformer1, transformer2);
        assertNotSame(transformer1, transformer3);
    }

}
