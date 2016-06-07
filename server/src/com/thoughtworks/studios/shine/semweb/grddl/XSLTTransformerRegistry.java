/*
 * Copyright 2016 ThoughtWorks, Inc.
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

package com.thoughtworks.studios.shine.semweb.grddl;

import com.thoughtworks.studios.shine.XSLTTransformerExecutor;

import javax.xml.transform.*;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;

public class XSLTTransformerRegistry {
    public static final String CRUISE_PIPELINE_GRAPH_GRDDL_XSL = "cruise/pipeline-graph-grddl.xsl";
    public static final String CRUISE_STAGE_GRAPH_GRDDL_XSL = "cruise/stage-graph-grddl.xsl";
    public static final String CRUISE_JOB_GRDDL_XSL = "cruise/job-grddl.xsl";
    public static final String XUNIT_ANT_JUNIT_GRDDL_XSL = "xunit/ant-junit-grddl.xsl";
    public static final String XUNIT_NUNIT_TO_JUNIT_XSL = "xunit/nunit-to-junit.xsl";
    protected final Map<String, Templates> transformerMap;

    public XSLTTransformerRegistry() {
        transformerMap = new HashMap<>();
        try {
            register(CRUISE_PIPELINE_GRAPH_GRDDL_XSL);
            register(CRUISE_STAGE_GRAPH_GRDDL_XSL);
            register(CRUISE_JOB_GRDDL_XSL);
            register(XUNIT_ANT_JUNIT_GRDDL_XSL);
            register(XUNIT_NUNIT_TO_JUNIT_XSL);
        } catch (IOException e) {
            throw bomb(e);
        }
    }

    private void register(String xsltPath) throws IOException {
        try (InputStream resourceAsStream = getClass().getClassLoader().getResourceAsStream(xsltPath)) {
            transformerMap.put(xsltPath, transformerForXSLStream(resourceAsStream));
        }
    }

    private Templates transformerForXSLStream(InputStream xsl) {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        try {
            return transformerFactory.newTemplates(new StreamSource(xsl));
        } catch (TransformerConfigurationException e) {
            throw new InvalidGrddlTransformationException(e);
        }
    }

    public <T> T transformWithCorrectClassLoader(String key, XSLTTransformerExecutor<T> executor) throws TransformerException, GrddlTransformException {
        ClassLoader orig = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
            return executor.execute(getTransformer(key));
        } finally {
            Thread.currentThread().setContextClassLoader(orig);
        }
    }

    private Transformer getTransformer(String xsltPath) {
        try {
            return transformerMap.get(xsltPath).newTransformer();
        } catch (TransformerConfigurationException e) {
            throw bomb(e);
        }
    }

}
