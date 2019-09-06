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

import Stream from "mithril/stream";

export enum ArtifactType {
  Build = "Build",
  Test  = "Test"
}

export interface Artifact {
  type: () => ArtifactType;
}

export abstract class BuiltInArtifact implements Artifact {
  public readonly source: Stream<string | undefined>;
  public readonly destination: Stream<string | undefined>;

  constructor(source?: string, destination?: string) {
    this.source      = Stream(source);
    this.destination = Stream(destination);
  }

  abstract type(): ArtifactType;
}

export class BuildArtifact extends BuiltInArtifact {
  constructor(source?: string, destination?: string) {
    super(source, destination);
  }

  type() {
    return ArtifactType.Build;
  }
}

export class TestArtifact extends BuiltInArtifact {
  constructor(source?: string, destination?: string) {
    super(source, destination);
  }

  type() {
    return ArtifactType.Test;
  }
}
