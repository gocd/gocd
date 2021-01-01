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
package com.thoughtworks.go.domain.materials.svn;

import com.thoughtworks.go.domain.materials.Revision;
import com.thoughtworks.go.domain.materials.mercurial.StringRevision;
import org.apache.commons.lang3.StringUtils;

public class SubversionRevision extends StringRevision {

    public static final SubversionRevision HEAD = new SubversionRevision("head") { };

    public SubversionRevision(long revision) {
        this(Long.toString(revision));
    }

    public SubversionRevision(String revision) {
        super(revision);
    }

    public SubversionRevision(Revision revision) {
        super(revision.getRevision());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SubversionRevision)) {
            return false;
        }

        SubversionRevision that = (SubversionRevision) o;
        return StringUtils.equalsIgnoreCase(revision, that.revision);
    }

    @Override
    public int hashCode() {
        return revision.hashCode();
    }

    @Override
    public String toString() {
        return "SubversionRevsion[" + revision + "]";
    }
}
