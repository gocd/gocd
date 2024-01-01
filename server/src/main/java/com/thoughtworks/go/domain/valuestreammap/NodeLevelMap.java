/*
 * Copyright 2024 Thoughtworks, Inc.
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

import java.util.*;

public class NodeLevelMap {

    private Map<Integer,List<Node>> map = new HashMap<>();

    public void add(Node node) {
        int level = node.getLevel();
        map.computeIfAbsent(level, k -> new ArrayList<>()).add(node);
    }


    public List<Node> get(int level) {
        return map.get(level);
    }

    public int lowestLevel() {
        return sortedLevelNumbers().first();
    }

    public int highestLevel() {
        return sortedLevelNumbers().last();
    }

    private TreeSet<Integer> sortedLevelNumbers() {
        return new TreeSet<>(map.keySet());
    }

    public List<List<Node>> nodesAtEachLevel() {
        List<List<Node>> nodesAtEachLevel = new ArrayList<>();
        TreeSet<Integer> sortedLevels = sortedLevelNumbers();
        for (Integer level : sortedLevels) {
            nodesAtEachLevel.add(map.get(level));
        }
        return nodesAtEachLevel;
    }
}
