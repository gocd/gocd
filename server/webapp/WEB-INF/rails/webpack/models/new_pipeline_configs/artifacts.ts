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

import _ from "lodash";
import Stream from "mithril/stream";
import {Artifact, ArtifactType, BuildArtifact, TestArtifact} from "models/new_pipeline_configs/artifact";

export class Artifacts {
  private readonly artifacts: Stream<Artifact[]>;

  constructor(...artifacts: Artifact[]) {
    this.artifacts = Stream(artifacts);
  }

  list(): Artifact[] {
    return this.artifacts();
  }

  count(): number {
    return this.list().length;
  }

  remove(artifact: Artifact) {
    _.remove(this.artifacts(), (a) => a === artifact);
  }

  addEmptyOfType(type: ArtifactType) {
    let artifact: Artifact;
    switch (type) {
      case ArtifactType.Build:
        artifact = new BuildArtifact();
        break;
      case ArtifactType.Test:
        artifact = new TestArtifact();
    }

    this.artifacts().push(artifact!);
  }
}
