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
(function () {
  "use strict";

  var PipelineDependencyNode = function PipelineDependencyNode(data) {
    this.id         = data.id;
    this.name       = data.name;
    this.parents    = data.parents || [];
    this.dependents = data.dependents || [];
    this.type       = data.type;
    this.instances  = data.instances;
  };

  PipelineDependencyNode.fromJSON = function (json) {
    return new PipelineDependencyNode({
      id:         json.id,
      name:       json.name,
      type:       json.node_type,
      parents:    json.parents ? json.parents.slice(0) : [],
      dependents: json.dependents ? json.dependents.slice(0) : [],
      instances:  json.instances.map(function (instance) {
        return PipelineDependencyNode.Instance.fromJSON(instance);
      }).filter(function (i) {
        return i;
      })
    });
  };

  PipelineDependencyNode.Instance = function (data) {
    this.counter = data.counter;
    this.label   = data.label;
    this.stages  = data.stages;
  };

  PipelineDependencyNode.Instance.fromJSON = function (json) {
    if (json.counter == 0) {
      return null;
    }

    return new PipelineDependencyNode.Instance({
      counter: json.counter,
      label:   json.label,
      stages:  json.stages.map(function (stage) {
        return PipelineDependencyNode.Instance.Stage.fromJSON(stage);
      })
    });
  };

  PipelineDependencyNode.Instance.Stage = function (data) {
    this.name     = data.name;
    this.status   = data.status;
    this.counter  = data.counter;
    this.duration = data.duration;
  };

  PipelineDependencyNode.Instance.Stage.fromJSON = function (json) {
    return new PipelineDependencyNode.Instance.Stage({
      name:     json.name,
      status:   json.status,
      counter:  parseInt(json.locator.split("/").last()),
      duration: json.duration
    });
  };

  var SCMDependencyNode = function SCMDependencyNode(data) {
    this.id                 = data.id;
    this.name               = data.name;
    this.parents            = data.parents;
    this.dependents         = data.dependents;
    this.type               = data.type;
    this.material_revisions = data.material_revisions;
  };

  SCMDependencyNode.fromJSON = function (json) {
    return new SCMDependencyNode({
      id:                 json.id,
      name:               json.name,
      type:               json.node_type,
      parents:            json.parents ? json.parents.slice(0) : [],
      dependents:         json.dependents ? json.dependents.slice(0) : [],
      material_revisions: json.material_revisions.map(function (mr) {
        return SCMDependencyNode.MaterialRevision.fromJSON(mr);
      })
    });
  };

  SCMDependencyNode.MaterialRevision = function (data) {
    this.modifications = data.modifications;
  };

  SCMDependencyNode.MaterialRevision.fromJSON = function (json) {
    return new SCMDependencyNode.MaterialRevision({
      modifications: json.modifications.map(function (m) {
        return new SCMDependencyNode.MaterialRevision.Modification.fromJSON(m);
      })
    });
  };

  SCMDependencyNode.MaterialRevision.Modification = function (data) {
    this.revision = data.revision;
  };

  SCMDependencyNode.MaterialRevision.Modification.fromJSON = function (json) {
    return new SCMDependencyNode.MaterialRevision.Modification({
      revision: json.revision
    });
  };

  var DummyNode = function DummyNode(data) {
    this.id         = data.id;
    this.parents    = data.parents;
    this.dependents = data.dependents;
    this.type       = data.type;
  };

  DummyNode.fromJSON = function (json) {
    return new SCMDependencyNode({
      id:         json.id,
      type:       json.node_type,
      parents:    json.parents ? json.parents.slice(0) : [],
      dependents: json.dependents ? json.dependents.slice(0) : []
    });
  };

  if ("undefined" !== typeof module) {
    module.exports = PipelineDependencyNode;
    module.exports = SCMDependencyNode;
    module.exports = DummyNode;
  } else {
    window.PipelineDependencyNode = PipelineDependencyNode;
    window.SCMDependencyNode = SCMDependencyNode;
    window.DummyNode = DummyNode;
  }
})();