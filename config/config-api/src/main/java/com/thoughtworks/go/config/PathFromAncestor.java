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

package com.thoughtworks.go.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@ConfigAttributeValue(fieldName = "path", createForNull = false)
public class PathFromAncestor {
    public static final String DELIMITER = "/";
    private final CaseInsensitiveString path;

    private List<CaseInsensitiveString> cachedPathToAncestor;
    private List<CaseInsensitiveString> cachedPathIncludingAncestor;
    private CaseInsensitiveString cachedAncestorName;

    public PathFromAncestor(String path) {
        this(new CaseInsensitiveString(path));
    }

    public PathFromAncestor(CaseInsensitiveString path) {
        this.path = path;
    }

    private String path() {
        return CaseInsensitiveString.str(path);
    }

    public CaseInsensitiveString getPath() {
        return path;
    }

    public List<CaseInsensitiveString> pathToAncestor() {
        if (cachedPathToAncestor == null) {
            cachedPathToAncestor = Collections.unmodifiableList(pathToAncestor(0));
        }
        return cachedPathToAncestor;
    }

    public List<CaseInsensitiveString> pathIncludingAncestor() {
        if (cachedPathIncludingAncestor == null) {
            cachedPathIncludingAncestor = Collections.unmodifiableList(pathToAncestor(-1));
        }
        return cachedPathIncludingAncestor;
    }

    public CaseInsensitiveString getAncestorName() {
        if (cachedAncestorName == null) {
            String stringPath = path();
            if (stringPath == null) {
                return null;
            }
            int index = stringPath.indexOf(DELIMITER);
            cachedAncestorName = index == -1 ? path : new CaseInsensitiveString(stringPath.substring(0, index));
        }
        return cachedAncestorName;
    }

    // cache parent name if required (like ancestor). currently used only for UI.
    public CaseInsensitiveString getDirectParentName() {
        String stringPath = path();
        if (stringPath == null) {
            return null;
        }
        int index = stringPath.lastIndexOf(DELIMITER);
        return index == -1 ? path : new CaseInsensitiveString(stringPath.substring(index + 1, stringPath.length()));
    }

    private List<CaseInsensitiveString> pathToAncestor(int upTill) {
        List<CaseInsensitiveString> fragments = new ArrayList<>();
        String[] allFragments = path().split(DELIMITER);
        for (int i = allFragments.length - 1; i > upTill; i--) {
            fragments.add(new CaseInsensitiveString(allFragments[i]));
        }
        return fragments;
    }

    public boolean isAncestor() {
        return path().contains("/");
    }

    @Override public String toString() {
        return path();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        PathFromAncestor that = (PathFromAncestor) o;

        if (path != null ? !path.equals(that.path) : that.path != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return path != null ? path.hashCode() : 0;
    }
}
