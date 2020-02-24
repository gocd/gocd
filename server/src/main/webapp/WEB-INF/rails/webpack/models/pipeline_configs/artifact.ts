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

import Stream = require("mithril/stream");
import {Configurations, PropertyJSON} from "models/shared/configuration";

export enum ArtifactType {
  test = "test", build = "build", external = "external"
}

export interface ArtifactJSON {
  type: ArtifactType;

  // for `test` or `build` type
  source?: string;
  destination?: string;

  // for `external`
  artifact_id?: string;
  store_id?: string;
  configuration?: PropertyJSON[];
}

export class Artifacts extends Array<Artifact> {
  constructor(...artifacts: Artifact[]) {
    super(...artifacts);
    Object.setPrototypeOf(this, Object.create(Artifacts.prototype));
  }

  static fromJSON(json: ArtifactJSON[]) {
    return new Artifacts(...json.map(Artifact.fromJSON));
  }
}

export abstract class Artifact {
  readonly type = Stream<ArtifactType>();

  protected constructor(type: ArtifactType) {
    this.type(type);
  }

  static fromJSON(json: ArtifactJSON) {
    switch (json.type) {
      case ArtifactType.build:
        return new GoCDArtifact(json.type, json.source!, json.destination!);
      case ArtifactType.test:
        return new GoCDArtifact(json.type, json.source!, json.destination!);
      case ArtifactType.external:
        const configurations = json.configuration ? Configurations.fromJSON(json.configuration) : new Configurations([]);
        return new ExternalArtifact(json.artifact_id!, json.store_id!, configurations);
      default:
        throw Error("Invalid artifact type");
    }
  }
}

export class GoCDArtifact extends Artifact {
  readonly source      = Stream<string>();
  readonly destination = Stream<string>();

  constructor(type: ArtifactType.test | ArtifactType.build, source: string, destination: string) {
    super(type);
    this.source(source);
    this.destination(destination);
  }
}

export class ExternalArtifact extends Artifact {
  readonly artifactId    = Stream<string>();
  readonly storeId       = Stream<string>();
  readonly configuration = Stream<Configurations>();

  constructor(artifactId: string, storeId: string, configurations = new Configurations([])) {
    super(ArtifactType.external);
    this.artifactId(artifactId);
    this.storeId(storeId);
    this.configuration(configurations);
  }
}
