/*************************GO-LICENSE-START*********************************
 * Copyright 2016 ThoughtWorks, Inc.
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

package com.thoughtworks.go.util;

import org.jdom.input.SAXBuilder;

import java.net.URISyntaxException;
import java.net.URL;

class ValidatingSaxBuilder extends SAXBuilder {
    public ValidatingSaxBuilder() {
        this.setFeature("http://apache.org/xml/features/validation/schema", true);
        this.setValidation(true);
        this.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    }

    public ValidatingSaxBuilder(URL resource) throws URISyntaxException {
        this();
        this.setProperty("http://apache.org/xml/properties/schema/external-noNamespaceSchemaLocation", resource.toURI().toString());
    }

    public ValidatingSaxBuilder(URL resource, String xsds) throws URISyntaxException {
        this(resource);
        this.setProperty("http://apache.org/xml/properties/schema/external-schemaLocation", xsds);
    }

}
