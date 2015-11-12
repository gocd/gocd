/*
 * Copyright 2015 ThoughtWorks, Inc.
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
 */

package com.thoughtworks.go.util;

import java.util.List;
import com.thoughtworks.go.config.CaseInsensitiveString;

import static java.util.Arrays.asList;

public class Node {
    private final List<DependencyNode> dependencies;

    public Node(DependencyNode... deps) {
        this(asList(deps));
    }

    public Node(List<DependencyNode> deps) {
        this.dependencies = deps;
    }

    public List<DependencyNode> getDependencies() {
        return dependencies;
    }

    public int hashCode() {
        return dependencies.hashCode();
    }

    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (that == null) {
            return false;
        }
        if (this.getClass() != that.getClass()) {
            return false;
        }
        return equals((Node) that);
    }

    private boolean equals(Node that) {
        return dependencies.equals(that.dependencies);
    }

    public String toString() {
        return "<Node: " + dependencies.toString() + ">";
    }

    public boolean hasDependency(final CaseInsensitiveString pipelineName) {
        return ListUtil.find(dependencies, new ListUtil.Condition() {
            @Override
            public <T> boolean isMet(T item) {
                DependencyNode dependencyNode = (DependencyNode) item;
                return dependencyNode.getPipelineName().equals(pipelineName);
            }
        }) != null;
    }

    public static class DependencyNode {
        private CaseInsensitiveString pipelineName;
        private CaseInsensitiveString stageName;

        public DependencyNode(CaseInsensitiveString pipelineName, CaseInsensitiveString stageName) {
            this.pipelineName = pipelineName;
            this.stageName = stageName;
        }

        public CaseInsensitiveString getPipelineName() {
            return pipelineName;
        }

        public CaseInsensitiveString getStageName() {
            return stageName;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            DependencyNode that = (DependencyNode) o;

            if (!pipelineName.equals(that.pipelineName)) return false;
            return stageName.equals(that.stageName);
        }

        @Override
        public int hashCode() {
            int result = pipelineName.hashCode();
            result = 31 * result + stageName.hashCode();
            return result;
        }
    }
}
