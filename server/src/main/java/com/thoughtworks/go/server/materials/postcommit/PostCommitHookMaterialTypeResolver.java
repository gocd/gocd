/*
 * Copyright Thoughtworks, Inc.
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
package com.thoughtworks.go.server.materials.postcommit;

import com.thoughtworks.go.server.materials.postcommit.git.GitPostCommitHookImplementer;
import com.thoughtworks.go.server.materials.postcommit.mercurial.MercurialPostCommitHookImplementer;
import com.thoughtworks.go.server.materials.postcommit.pluggablescm.PluggableSCMPostCommitHookImplementer;
import com.thoughtworks.go.server.materials.postcommit.svn.SvnPostCommitHookImplementer;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toMap;

/**
 * Understands: All Materials that can use the post commit hook feature
 */
@Component
public class PostCommitHookMaterialTypeResolver {
    private final Map<String, PostCommitHookMaterialType> allKnownMaterialTypes = Stream.of(
        new UnknownPostCommitHookMaterialType(),
        new SvnPostCommitHookMaterialType(),
        new GitPostCommitHookMaterialType(),
        new MercurialPostCommitHookMaterialType(),
        new PluggableSCMPostCommitHookMaterialType()
    ).collect(toMap(PostCommitHookMaterialType::type, v -> v));

    public PostCommitHookMaterialType toType(String type) {
        return allKnownMaterialTypes.getOrDefault(type.toLowerCase(), new UnknownPostCommitHookMaterialType());
    }

    static final class UnknownPostCommitHookMaterialType implements PostCommitHookMaterialType {
        @Override
        public boolean isKnown() {
            return false;
        }

        @Override
        public boolean isValid(String type) {
            return false;
        }

        @Override
        public PostCommitHookImplementer getImplementer() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String type() {
            return "???";
        }
    }

    static final class SvnPostCommitHookMaterialType implements PostCommitHookMaterialType {
        private static final String TYPE = "svn";

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
            return new SvnPostCommitHookImplementer();
        }

        @Override
        public String type() {
            return TYPE;
        }
    }

    static final class GitPostCommitHookMaterialType implements PostCommitHookMaterialType {
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

        @Override
        public String type() {
            return TYPE;
        }
    }

    static final class MercurialPostCommitHookMaterialType implements PostCommitHookMaterialType {
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

        @Override
        public String type() {
            return TYPE;
        }
    }

    static final class PluggableSCMPostCommitHookMaterialType implements PostCommitHookMaterialType {
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

        @Override
        public String type() {
            return TYPE;
        }
    }
}
