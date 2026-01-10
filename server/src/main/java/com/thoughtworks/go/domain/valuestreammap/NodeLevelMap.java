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
package com.thoughtworks.go.domain.valuestreammap;

import org.jetbrains.annotations.Nullable;

import java.util.*;

public class NodeLevelMap {

    private final Map<Integer, List<Node>> map = new HashMap<>();

    public void add(Node node) {
        map.computeIfAbsent(node.getLevel(), k -> new ArrayList<>()).add(node);
    }

    public @Nullable List<Node> get(int level) {
        return map.get(level);
    }

    public boolean contains(int level) {
        return map.containsKey(level);
    }

    public int lowestLevel() {
        return sortedLevelNumbers().first();
    }

    public int highestLevel() {
        return sortedLevelNumbers().last();
    }

    private SortedSet<Integer> sortedLevelNumbers() {
        return new TreeSet<>(map.keySet());
    }

    public List<List<Node>> nodesAtEachLevel() {
        List<List<Node>> nodesAtEachLevel = new ArrayList<>();
        SortedSet<Integer> sortedLevels = sortedLevelNumbers();
        for (Integer level : sortedLevels) {
            nodesAtEachLevel.add(map.get(level));
        }
        return nodesAtEachLevel;
    }
}
