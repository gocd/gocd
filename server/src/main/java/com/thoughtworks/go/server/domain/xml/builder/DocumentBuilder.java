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
package com.thoughtworks.go.server.domain.xml.builder;

import org.apache.commons.lang3.StringUtils;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.dom.DOMDocument;
import org.dom4j.dom.DOMElement;

public class DocumentBuilder extends AbstractBuilder<DOMDocument, DocumentBuilder> {

    protected DocumentBuilder(DOMDocument document) {
        super(DocumentBuilder.class, document);
    }

    public static DocumentBuilder withRoot(String name) {
        return withRoot(name, null);
    }

    public static DocumentBuilder withRoot(String name, String xmlns) {
        DOMElement rootElement = new DOMElement(name);
        if (StringUtils.isNotBlank(xmlns)) {
            rootElement.setNamespace(new Namespace("", xmlns));
        }
        return new DocumentBuilder(new DOMDocument(rootElement));
    }

    @Override
    protected Element current() {
        return parent.getRootElement();
    }

    public DocumentBuilder additionalNamespace(String prefix, String uri) {
        current().addNamespace(prefix, uri);
        return this;
    }

    public DocumentBuilder encoding(String encoding) {
        parent.setXMLEncoding(encoding);
        return this;
    }

    public Document build() {
        return parent;
    }
}
