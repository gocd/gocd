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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.io.InputStream;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.stream.StreamSource;

/**
 * IMPORTANT: This is completely thread unsafe
 */
public class XSLTTransformerRegistry {
    private final Map<String, Transformer> transformerMap;

    public XSLTTransformerRegistry() {
        transformerMap = new HashMap<String, Transformer>();
        register("cruise/pipeline-graph-grddl.xsl");
        register("cruise/stage-graph-grddl.xsl");
        register("cruise/job-grddl.xsl");
        register("xunit/ant-junit-grddl.xsl");
        register("xunit/nunit-to-junit.xsl");
    }

    private void register(String xsltPath) {
        transformerMap.put(xsltPath, transformerForXSLStream(getClass().getClassLoader().getResourceAsStream(xsltPath)));
    }

    public Transformer getTransformer(String xsltPath) {
        return transformerMap.get(xsltPath);
    }

    private Transformer transformerForXSLStream(InputStream xsl) {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        try {
            return transformerFactory.newTransformer(new StreamSource(xsl));
        } catch (TransformerConfigurationException e) {
            throw new InvalidGrddlTransformationException(e);
        }
    }

    public void reset() {
        for (Transformer transformer : transformerMap.values()) {
            transformer.reset();
        }
    }
}
