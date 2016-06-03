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

import com.sun.org.apache.xalan.internal.XalanConstants;

import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;

public class XSLTTransformerRegistry {
    private final Map<String, Templates> transformerMap;

    public XSLTTransformerRegistry() {
        transformerMap = new HashMap<>();
        try {
            register("cruise/pipeline-graph-grddl.xsl");
            register("cruise/stage-graph-grddl.xsl");
            register("cruise/job-grddl.xsl");
            register("xunit/ant-junit-grddl.xsl");
            register("xunit/nunit-to-junit.xsl");
        } catch (IOException e) {
            throw bomb(e);
        }
    }

    private void register(String xsltPath) throws IOException {
        try (InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream(xsltPath)) {
            transformerMap.put(xsltPath, transformerForXSLStream(resourceAsStream));
        }
    }

    public Transformer getTransformer(String xsltPath) {
        try {
            return transformerMap.get(xsltPath).newTransformer();
        } catch (TransformerConfigurationException e) {
            throw bomb(e);
        }
    }

    private Templates transformerForXSLStream(InputStream xsl) {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        transformerFactory.setAttribute(XalanConstants.JDK_EXTENSION_CLASSLOADER, this.getClass().getClassLoader());
        try {
            return transformerFactory.newTemplates(new StreamSource(xsl));
        } catch (TransformerConfigurationException e) {
            throw new InvalidGrddlTransformationException(e);
        }
    }

}
