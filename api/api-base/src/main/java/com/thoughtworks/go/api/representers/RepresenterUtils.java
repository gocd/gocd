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


import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RepresenterUtils {

    public static void addLinks(List<Link> links, Map jsonObject) {
        if (links.isEmpty()) {
            return;
        }
        Map<String, Object> linksMap = new LinkedHashMap<>();
        links.forEach(link -> linksMap.put(link.getName(), Collections.singletonMap("href", link.getHref())));
        jsonObject.put("_links", linksMap);
    }
}
