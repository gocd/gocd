/*
 * Copyright 2020 ThoughtWorks, Inc.
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
package com.thoughtworks.go.config;

import com.thoughtworks.go.domain.MaterialRevision;
import com.thoughtworks.go.domain.MaterialRevisions;
import com.thoughtworks.go.domain.ModificationVisitor;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.ModifiedFile;
import com.thoughtworks.go.domain.materials.Revision;
import org.apache.commons.lang3.StringUtils;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class MaterialRevisionsMatchers {
    static class ModifiedBy implements ModificationVisitor {
        private final String user;
        private final String file;
        private String currentName;
        private boolean contains;

        public ModifiedBy(String user, String file) {
            this.user = user;
            this.file = file;
        }

        @Override
        public void visit(MaterialRevision materialRevision) {
        }

        @Override
        public void visit(Material material, Revision revision) {
        }

        @Override
        public void visit(Modification modification) {
            this.currentName = modification.getUserName();
        }

        @Override
        public void visit(ModifiedFile file) {
            if (StringUtils.equals(file.getFileName(), this.file) && StringUtils.equals(currentName, this.user)) {
                contains = true;
            }
        }
    }

    static class ModifiedFileVisitor implements ModificationVisitor {
        private final String file;
        private String currentName;
        private boolean contains;

        public ModifiedFileVisitor(String file) {
            this.file = file;
        }

        @Override
        public void visit(MaterialRevision materialRevision) {
        }

        @Override
        public void visit(Material material, Revision revision) {
        }

        @Override
        public void visit(Modification modification) {
        }

        @Override
        public void visit(ModifiedFile file) {
            if (StringUtils.equals(file.getFileName(), this.file)) {
                contains = true;
            }
        }
    }

    public static Matcher<MaterialRevisions> containsModifiedBy(final String filename, final String user) {
        return new TypeSafeMatcher<MaterialRevisions>() {
            @Override
            public boolean matchesSafely(MaterialRevisions revisions) {
                ModifiedBy modifiedBy = new ModifiedBy(user, filename);
                revisions.accept(modifiedBy);
                return modifiedBy.contains;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("Does not contains file [" + filename + "] modified by user [" + user + "]");
            }
        };
    }


    public static Matcher<MaterialRevisions> containsModifiedFile(final String filename) {
        return new TypeSafeMatcher<MaterialRevisions>() {
            @Override
            public boolean matchesSafely(MaterialRevisions revisions) {
                ModifiedFileVisitor modifiedBy = new ModifiedFileVisitor(filename);
                revisions.accept(modifiedBy);
                return modifiedBy.contains;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("Does not contains file [" + filename + "]");
            }
        };
    }
}
