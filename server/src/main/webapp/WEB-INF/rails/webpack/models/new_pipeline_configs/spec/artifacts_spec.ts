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

import {ArtifactType, BuildArtifact, TestArtifact} from "models/new_pipeline_configs/artifact";
import {Artifacts} from "models/new_pipeline_configs/artifacts";

describe("Pipeline Config - Artifacts Model", () => {

  const buildArtifact = new BuildArtifact("foo.txt", "src/foo.txt");
  const testArtifact  = new TestArtifact("bar.txt", "src/bar.txt");

  it("should list artifacts", () => {
    expect(new Artifacts().list()).toEqual([]);
    expect(new Artifacts(buildArtifact, testArtifact).list()).toEqual([buildArtifact, testArtifact]);
  });

  it("should count artifacts", () => {
    expect(new Artifacts().count()).toEqual(0);
    expect(new Artifacts(buildArtifact, testArtifact).count()).toEqual(2);
  });

  it("should allow adding an empty artifact of type build", () => {
    const artifacts = new Artifacts();
    expect(artifacts.count()).toEqual(0);

    artifacts.addEmptyOfType(ArtifactType.Build);

    expect(artifacts.count()).toEqual(1);
    expect((artifacts.list()[0] as BuildArtifact).source()).toEqual(undefined);
    expect((artifacts.list()[0] as BuildArtifact).destination()).toEqual(undefined);
  });

  it("should allow adding an empty artifact of type test", () => {
    const artifacts = new Artifacts();
    expect(artifacts.count()).toEqual(0);

    artifacts.addEmptyOfType(ArtifactType.Test);

    expect(artifacts.count()).toEqual(1);
    expect((artifacts.list()[0] as TestArtifact).source()).toEqual(undefined);
    expect((artifacts.list()[0] as TestArtifact).destination()).toEqual(undefined);
  });

  it("should allow removing an artifact", () => {
    const artifacts = new Artifacts(buildArtifact, testArtifact);

    expect(artifacts.count()).toEqual(2);
    expect(artifacts.list()).toEqual([buildArtifact, testArtifact]);

    artifacts.remove(buildArtifact);

    expect(artifacts.count()).toEqual(1);
    expect(artifacts.list()).toEqual([testArtifact]);

    artifacts.remove(testArtifact);

    expect(artifacts.count()).toEqual(0);
    expect(artifacts.list()).toEqual([]);
  });
});
