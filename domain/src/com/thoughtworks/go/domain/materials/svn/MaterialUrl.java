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

package com.thoughtworks.go.domain.materials.svn;

import java.net.URLDecoder;

public class MaterialUrl {
    private final String url;

    public MaterialUrl(String url) {
        url = url.trim();
        url = url.endsWith("/") ? (url.substring(0, url.length() - "/".length())) : url;
        url = URLDecoder.decode(url);
        url = url.toLowerCase().startsWith("file://") ? (url.substring("file://".length(), url.length())) : url;
        this.url = url;
    }

    public static boolean sameUrl(String firstUrl, String secondUrl) {
        return new MaterialUrl(firstUrl).equals(new MaterialUrl(secondUrl));
    }

    public String toString() {
        return url;
    }

    public String getUrl() {
        return url;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MaterialUrl svnUrl = (MaterialUrl) o;
        return url.equals(svnUrl.url);
    }

    public int hashCode() {
        return url.hashCode();
    }
}
