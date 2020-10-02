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
"use strict";

(function () {
  "use strict";

  var VSMGraph = function VSMGraph(data) {
    this.current_pipeline = data.current_pipeline;
    this.current_material = data.current_material;
    this.levels           = data.levels;

    var dummyNodeMap = {};

    data.levels.forEach(function (level) {
      level.nodes.forEach(function (node) {
        if (node.type === "DUMMY") dummyNodeMap[node.id] = node;
      });
    });

    var dummyNodeIds = Object.keys(dummyNodeMap);

    var resolveDummyDependents = function resolveDummyDependents(levels) {
      levels.forEach(function (level) {
        level.nodes.forEach(function (node) {
          dummyDependentsFor(node).forEach(function (dummyDependent) {
            node.dependents[node.dependents.indexOf(dummyDependent)] = actualDependentNodeFor(dummyDependent);
          });
        });
      });
    };

    var resolveDummyParents = function resolveDummyParents(levels) {
      levels.forEach(function (level) {
        level.nodes.forEach(function (node) {
          dummyParentsFor(node).forEach(function (dep) {
            node.parents[node.parents.indexOf(dep)] = actualParentNodeFor(dep);
          });
        });
      });
    };

    var dummyDependentsFor = function dummyDependentsFor(node) {
      return node.dependents.filter(function (dependentNodeId) {
        return dummyNodeIds.indexOf(dependentNodeId) >= 0;
      });
    };

    var dummyParentsFor = function dummyParentsFor(node) {
      return node.parents.filter(function (parentNodeId) {
        return dummyNodeIds.indexOf(parentNodeId) >= 0;
      });
    };

    var actualDependentNodeFor = function actualDependentNodeFor(dep) {
      var node = dummyNodeMap[dep];
      if (dummyNodeIds.indexOf(node.dependents[0]) === -1) {
        return node.dependents[0];
      }
      return actualDependentNodeFor(node.dependents[0]);
    };

    var actualParentNodeFor = function actualParentNodeFor(dep) {
      var node = dummyNodeMap[dep];
      if (dummyNodeIds.indexOf(node.parents[0]) === -1) {
        return node.parents[0];
      }
      return actualParentNodeFor(node.parents[0]);
    };

    var removeDummyNodes = function removeDummyNodes(levels) {
      levels.forEach(function (level) {
        level.nodes = level.nodes.filter(function (node) {
          return node.type !== "DUMMY";
        });
      });
    };

    resolveDummyDependents(this.levels);
    resolveDummyParents(this.levels);
    removeDummyNodes(this.levels);
  };

  VSMGraph.fromJSON = function (json) {
    return new VSMGraph({
      current_pipeline: json.current_pipeline,
      current_material: json.current_material,
      levels:           json.levels.map(function (level) {
        return VSMGraph.Level.fromJSON(level);
      })
    });
  };

  VSMGraph.Level = function (data) {
    this.nodes = data.nodes;
  };

  VSMGraph.Level.fromJSON = function (json) {
    return new VSMGraph.Level({
      nodes: json.nodes.map(function (node) {
        if (node.node_type === "PIPELINE") return PipelineDependencyNode.fromJSON(node);
        if (node.node_type === "DUMMY") return DummyNode.fromJSON(node);
        return SCMDependencyNode.fromJSON(node);
      })
    });
  };

  if ("undefined" !== typeof module) {
    module.exports = VSMGraph;
  } else {
    window.VSMGraph = VSMGraph;
  }
})();