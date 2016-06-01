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

package com.thoughtworks.go.domain.valuestreammap;

import com.thoughtworks.go.i18n.Localizable;
import com.thoughtworks.go.i18n.Localizer;
import com.thoughtworks.go.server.util.CollectionUtil;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public abstract class Node implements Comparable<Node>{
    protected final String id;
    protected final String nodeName;
    private final List<Node> parents = new ArrayList<>();
    private final List<Node> children = new ArrayList<>();
    protected int level = 0;
    protected Integer depth = 0;
    protected DependencyNodeType type;
    private VSMViewType viewType;

    public Node(DependencyNodeType dependencyNodeType, String nodeId, String nodeName) {
        this.id = nodeId;
        this.type= dependencyNodeType;
        this.nodeName = nodeName;
    }

    public String getName() {
        return nodeName;
    }

    public String getId() {
        return id;
    }

    public List<Node> getParents() {
        return parents;
    }

    public List<Node> getChildren() {
        return children;
    }

    public int getLevel() {
        return level;
    }

    public DependencyNodeType getType() {
        return type;
    }

    public void addParentIfAbsent(Node parentNode) {
        if (parentNode != null && !parents.contains(parentNode)) {
            parents.add(parentNode);
        }
    }

    public void addChildIfAbsent(Node childNode) {
        if (childNode != null && !children.contains(childNode)) {
            children.add(childNode);
        }
    }

    private void addParent(int index, Node parentNode) {
        if (parentNode != null && !parents.contains(parentNode)) {
            parents.add(index, parentNode);
        }
    }

    private void addChild(int index, Node childNode) {
        if (childNode != null && !children.contains(childNode)) {
            children.add(index, childNode);
        }
    }

    private int removeParent(Node parentNode) {
        int index = parents.indexOf(parentNode);
        parents.remove(parentNode);
        return index;
    }

    private int removeChild(Node childNode) {
        int index = children.indexOf(childNode);
        children.remove(childNode);
        return index;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Node that = (Node) o;

        if (id != null ? !id.equals(that.id) : that.id != null) {
            return false;
        }

        return true;
    }


    @Override
    public int compareTo(Node other) {
        return this.depth.compareTo(other.depth);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        List<String> childIds = CollectionUtil.map(children, new CollectionUtil.MapFn<Node, String>() {
            @Override
            public String map(Node node) {
                return node.getId();
            }
        });
        return String.format("id='%s' name='%s' level='%d' depth='%d' revisions='%s' children='%s'", id, nodeName, level, depth, revisions(), StringUtils.join(childIds, ','));
    }

    public void replaceParentWith(Node currentParentNode, Node newParentNode) {
        int parentIndex = removeParent(currentParentNode);
        addParent(parentIndex, newParentNode);
    }

    public void replaceChildWith(Node currentChildNode, Node newChildNode) {
        int childIndex = removeChild(currentChildNode);
        addChild(childIndex, newChildNode);
    }

    public void addEdge(Node dependentNode) {
        this.addChildIfAbsent(dependentNode);
        dependentNode.addParentIfAbsent(this);
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }

    public int getDepth() {
        return depth;
    }

    public String getMessageString(Localizer localizer) {
        return null;
    }

    public void setMessage(Localizable message) {
    }

    public VSMViewType getViewType() {
        return viewType;
    }

    public void setViewType(VSMViewType viewType) {
        this.viewType = viewType;
    }

    public abstract void addRevision(Revision revision);

    public abstract List<Revision> revisions();

    public boolean hasCycleInSubGraph(Set<String> nodesInPath, Set<Node> verifiedNodes) {
        if (nodesInPath.contains(getId())) {
            return true;
        }
        nodesInPath.add(getId());
        for (Node child : getChildren()) {
            if (!verifiedNodes.contains(child)) {
                if(child.hasCycleInSubGraph(nodesInPath, verifiedNodes)) {
                    return true;
                }
            }
        }
        nodesInPath.remove(getId());
        verifiedNodes.add(this);
        return false;
    }

    public abstract void addRevisions(List<Revision> revisions);
}
