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

describe("Pipeline Config - Artifact Model", () => {
  describe("Build", () => {

    it("should create build artifact with no source and destination", () => {
      const artifact = new BuildArtifact();
      expect(artifact.source()).toBe(undefined);
      expect(artifact.destination()).toBe(undefined);
    });

    it("should allow specifying source", () => {
      const artifact = new BuildArtifact();
      expect(artifact.source()).toBe(undefined);
      artifact.source("build/foo.txt");
      expect(artifact.source()).toBe("build/foo.txt");
    });

    it("should allow specifying destination", () => {
      const artifact = new BuildArtifact();
      expect(artifact.destination()).toBe(undefined);
      artifact.destination("build/foo.txt");
      expect(artifact.destination()).toBe("build/foo.txt");
    });

    it("should return type of artifact", () => {
      const artifact = new BuildArtifact();
      expect(artifact.type()).toBe(ArtifactType.Build);
    });
  });

  describe("Test", () => {
    it("should create test artifact with no source and destination", () => {
      const artifact = new TestArtifact();
      expect(artifact.source()).toBe(undefined);
      expect(artifact.destination()).toBe(undefined);
    });

    it("should allow specifying source", () => {
      const artifact = new TestArtifact();
      expect(artifact.source()).toBe(undefined);
      artifact.source("build/foo.txt");
      expect(artifact.source()).toBe("build/foo.txt");
    });

    it("should allow specifying destination", () => {
      const artifact = new TestArtifact();
      expect(artifact.destination()).toBe(undefined);
      artifact.destination("build/foo.txt");
      expect(artifact.destination()).toBe("build/foo.txt");
    });

    it("should return type of artifact", () => {
      const artifact = new TestArtifact();
      expect(artifact.type()).toBe(ArtifactType.Test);
    });
  });

});
