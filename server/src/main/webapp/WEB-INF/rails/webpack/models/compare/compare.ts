/*
 * Copyright 2019 ThoughtWorks, Inc.
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

import {MaterialJSON} from "models/materials/serialization";
import {Material, MaterialAttributes} from "models/materials/types";
import {ChangeJSON, ComparisonJSON, DependencyRevisionJSON, MaterialRevisionJSON} from "./compare_json";
import {parseDate} from "./pipeline_instance";
import {dateOrUndefined} from "./pipeline_instance_json";

type Revisions = MaterialRevisions | DependencyRevisions;

export class Comparison {
  pipelineName: string;
  fromCounter: number;
  toCounter: number;
  isBisect: boolean;
  changes: Changes;

  constructor(pipelineName: string, fromCounter: number, toCounter: number, isBisect: boolean, changes: Changes) {
    this.pipelineName = pipelineName;
    this.fromCounter  = fromCounter;
    this.toCounter    = toCounter;
    this.isBisect     = isBisect;
    this.changes      = changes;
  }

  static fromJSON(data: ComparisonJSON): Comparison {
    return new Comparison(data.pipeline_name, data.from_counter, data.to_counter, data.is_bisect, Changes.fromJSON(data.changes));
  }
}

export class Change {
  material: Material;
  revision: Revisions;

  constructor(material: Material, revision: Revisions) {
    this.material = material;
    this.revision = revision;
  }

  static fromJSON(data: ChangeJSON): Change {
    return new Change(Change.parseMaterial(data.material), Revision.deserialize(data));
  }

  private static parseMaterial(material: MaterialJSON) {
    return new Material(material.type, MaterialAttributes.deserialize(material));
  }
}

class Changes extends Array<Change> {
  constructor(...vals: Change[]) {
    super(...vals);
    Object.setPrototypeOf(this, Object.create(Changes.prototype));
  }

  static fromJSON(data: ChangeJSON[]): Changes {
    return new Changes(...data.map((change) => Change.fromJSON(change)));
  }
}

class Revision {
  static deserialize(data: ChangeJSON) {
    switch (data.material.type) {
      case "dependency":
        return DependencyRevisions.fromJSON(data.revision as DependencyRevisionJSON[]);
      default:
        return MaterialRevisions.fromJSON(data.revision as MaterialRevisionJSON[]);
    }
  }
}

export class MaterialRevision {
  revisionSha: string;
  modifiedBy: string;
  modifiedAt: dateOrUndefined;
  commitMessage: string;

  constructor(revisionSha: string, modifiedBy: string, modifiedAt: string, commitMessage: string) {
    this.revisionSha   = revisionSha;
    this.modifiedBy    = modifiedBy;
    this.modifiedAt    = parseDate(modifiedAt);
    this.commitMessage = commitMessage;
  }

  static fromJSON(data: MaterialRevisionJSON): MaterialRevision {
    return new MaterialRevision(data.revision_sha, data.modified_by, data.modified_at, data.commit_message);
  }
}

export class MaterialRevisions extends Array<MaterialRevision> {
  constructor(...vals: MaterialRevision[]) {
    super(...vals);
    Object.setPrototypeOf(this, Object.create(MaterialRevisions.prototype));
  }

  static fromJSON(data: MaterialRevisionJSON[]): MaterialRevisions {
    return new MaterialRevisions(...data.map((rev) => MaterialRevision.fromJSON(rev)));
  }
}

export class DependencyRevision {
  revision: string;
  pipelineCounter: string;
  completedAt: dateOrUndefined;

  constructor(revision: string, pipelineCounter: string, completedAt: string) {
    this.revision        = revision;
    this.pipelineCounter = pipelineCounter;
    this.completedAt     = parseDate(completedAt);
  }

  static fromJSON(data: DependencyRevisionJSON): DependencyRevision {
    return new DependencyRevision(data.revision, data.pipeline_counter, data.completed_at);
  }
}

export class DependencyRevisions extends Array<DependencyRevision> {
  constructor(...vals: DependencyRevision[]) {
    super(...vals);
    Object.setPrototypeOf(this, Object.create(DependencyRevisions.prototype));
  }

  static fromJSON(data: DependencyRevisionJSON[]): DependencyRevisions {
    return new DependencyRevisions(...data.map((rev) => DependencyRevision.fromJSON(rev)));
  }
}
