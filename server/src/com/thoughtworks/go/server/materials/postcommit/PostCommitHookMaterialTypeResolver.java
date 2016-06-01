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

package com.thoughtworks.go.server.materials.postcommit;

import java.util.ArrayList;
import java.util.List;

import com.thoughtworks.go.server.materials.postcommit.git.GitPostCommitHookImplementer;
import com.thoughtworks.go.server.materials.postcommit.mercurial.MercurialPostCommitHookImplementer;
import com.thoughtworks.go.server.materials.postcommit.pluggablescm.PluggableSCMPostCommitHookImplementer;
import com.thoughtworks.go.server.materials.postcommit.svn.SvnPostCommitHookImplementer;
import org.springframework.stereotype.Component;

/**
 * @understands: All Materials that can use the post commit hook feature
 */
@Component
public class PostCommitHookMaterialTypeResolver {
    private List<PostCommitHookMaterialType> allKnownMaterialTypes = new ArrayList<>();

    public PostCommitHookMaterialTypeResolver() {
        allKnownMaterialTypes.add(new UnknownPostCommitHookMaterialType());
        allKnownMaterialTypes.add(new SvnPostCommitHookMaterialType());
        allKnownMaterialTypes.add(new GitPostCommitHookMaterialType());
        allKnownMaterialTypes.add(new MercurialPostCommitHookMaterialType());
        allKnownMaterialTypes.add(new PluggableSCMPostCommitHookMaterialType());
    }

    public PostCommitHookMaterialType toType(String type) {
        for (PostCommitHookMaterialType materialType : allKnownMaterialTypes) {
            if (materialType.isValid(type)) {
                return materialType;
            }
        }
        return new UnknownPostCommitHookMaterialType();
    }

    final class UnknownPostCommitHookMaterialType implements PostCommitHookMaterialType {
        @Override public boolean isKnown() {
            return false;
        }

        @Override public boolean isValid(String type) {
            return false;
        }

        @Override public PostCommitHookImplementer getImplementer() {
            throw new UnsupportedOperationException();
        }
    }

    final class SvnPostCommitHookMaterialType implements PostCommitHookMaterialType {
        private final String TYPE = "svn";

        @Override public boolean isKnown() {
            return true;
        }

        @Override public boolean isValid(String type) {
            return TYPE.equalsIgnoreCase(type);
        }

        @Override public PostCommitHookImplementer getImplementer() {
            return new SvnPostCommitHookImplementer();
        }
    }

    final class GitPostCommitHookMaterialType implements PostCommitHookMaterialType {
        private static final String TYPE = "git";

        @Override
        public boolean isKnown() {
            return true;
        }

        @Override
        public boolean isValid(String type) {
            return TYPE.equalsIgnoreCase(type);
        }

        @Override
        public PostCommitHookImplementer getImplementer() {
            return new GitPostCommitHookImplementer();
        }
    }

    final class MercurialPostCommitHookMaterialType implements PostCommitHookMaterialType {
        private static final String TYPE = "hg";

        @Override
        public boolean isKnown() {
            return true;
        }

        @Override
        public boolean isValid(String type) {
            return TYPE.equalsIgnoreCase(type);
        }

        @Override
        public PostCommitHookImplementer getImplementer() {
            return new MercurialPostCommitHookImplementer();
        }
    }

    final class PluggableSCMPostCommitHookMaterialType implements PostCommitHookMaterialType {
        private static final String TYPE = "scm";

        @Override
        public boolean isKnown() {
            return true;
        }

        @Override
        public boolean isValid(String type) {
            return TYPE.equalsIgnoreCase(type);
        }

        @Override
        public PostCommitHookImplementer getImplementer() {
            return new PluggableSCMPostCommitHookImplementer();
        }
    }
}
