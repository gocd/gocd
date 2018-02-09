/*
 * Copyright 2018 ThoughtWorks, Inc.
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

package com.thoughtworks.go.api.representers;

import com.thoughtworks.go.spark.Link;
import com.thoughtworks.go.spark.RequestContext;

import java.util.*;

public class JsonWriter {

    private final Map<String, Object> properties;
    private final RequestContext requestContext;
    private final List<Link> links;
    private final Map<String, Object> embedded;

    public JsonWriter(RequestContext requestContext) {
        this.requestContext = requestContext;
        this.properties = new LinkedHashMap<>();
        this.links = new ArrayList<>();
        this.embedded = new HashMap<>();
    }

    public JsonWriter add(String propertyName, Object value) {
        properties.put(propertyName, value);
        return this;
    }

    public JsonWriter addEmbedded(String propertyName, List<Map> objects) {
        embedded.put(propertyName, objects);
        return this;
    }

    public JsonWriter addIfNotNull(String propertyName, Object value) {
        if (value != null) {
            properties.put(propertyName, value);
        }
        return this;
    }

    public JsonWriter addOptional(String propertyName, Optional<?> value) {
        value.ifPresent(val -> properties.put(propertyName, val));
        return this;
    }

    public JsonWriter addLink(String name, String href) {
        links.add(requestContext.build(name, href));
        return this;
    }

    public Map<String, Object> getAsMap() {
        Map<String, Object> json = new LinkedHashMap<>();
        if (!links.isEmpty()) {
            json.put("_links", getLinksAsMap());
        }
        json.putAll(properties);
        if (!embedded.isEmpty()) {
            json.put("_embedded", embedded);
        }
        return json;
    }

    private Map<String, Object> getLinksAsMap() {
        Map<String, Object> linksMap = new LinkedHashMap<>();
        links.forEach(link -> linksMap.put(link.getName(), Collections.singletonMap("href", link.getHref())));
        return linksMap;
    }

    public JsonWriter addDocLink(String absoluteUrl) {
        links.add(new Link("doc", absoluteUrl));
        return this;
    }
}
