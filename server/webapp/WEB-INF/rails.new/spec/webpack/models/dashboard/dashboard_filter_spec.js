/*
 * Copyright 2018 ThoughtWorks, Inc.
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

const DashboardFilter = require("models/dashboard/dashboard_filter");

describe("Dashboard filter", () => {
  describe("list filtering", () => {
    let filter;
    describe("blacklist", () => {
      beforeEach(() => {
        filter = new DashboardFilter({
          name: "pink_diamond",
          pipelines: ["p1", "p2"]
        });
      });
      it("should return false if the pipeline is on the list", () => {
        expect(filter.byPipelineName("p1")).toBe(false);
        expect(filter.byPipelineName("p2")).toBe(false);
      });

      it("should return true if the pipeline is not on the list", () => {
        expect(filter.byPipelineName("p3")).toBe(true);
      });
    });

    describe("whitelist", () => {
      beforeEach(() => {
        filter = new DashboardFilter({
          name: "white_diamond",
          type: "whitelist",
          pipelines: ["p1", "p2"]
        });
      });
      it("should return true if the pipeline is on the list", () => {
        expect(filter.byPipelineName("p1")).toBe(true);
        expect(filter.byPipelineName("p2")).toBe(true);
      });

      it("should return false if the pipeline is not on the list", () => {
        expect(filter.byPipelineName("p3")).toBe(false);
      });
    });
  });

  describe("state filtering", () => {
    let stage, pipeline, filter;

    function Stage(status) {
      this.isFailed = () => status === "Failed";
      this.isBuilding = () => status === "Building";
    }

    beforeEach(() => {
      pipeline = {
        name: null,
        latestStage: () => stage
      };
      filter = new DashboardFilter({name: "blue_diamond"});
    });

    it("should properly filter when state=[failing]", () => {
      filter.state = ["failing"];

      stage = new Stage("Failed");
      expect(filter.byState(pipeline)).toBe(true);

      stage = new Stage("Passing");
      expect(filter.byState(pipeline)).toBe(false);
    });

    it("should properly filter when state=[building]", () => {
      filter.state = ["building"];

      stage = new Stage("Building");
      expect(filter.byState(pipeline)).toBe(true);

      stage = new Stage("Passing");
      expect(filter.byState(pipeline)).toBe(false);
    });

    it("should properly filter when state=[building, failing]", () => {
      filter.state = ["building", "failing"];

      stage = new Stage("Building");
      expect(filter.byState(pipeline)).toBe(true);

      stage = new Stage("Failed");
      expect(filter.byState(pipeline)).toBe(true);

      stage = new Stage("Passing");
      expect(filter.byState(pipeline)).toBe(false);
    });

    it("should return true if state=[]", () => {
      filter.state = [];
      stage = new Stage("Whatever");
      expect(filter.byState(pipeline)).toBe(true);
    });

    it("should return true if there's no latest stage or instance", () => {
      expect(filter.byState({latestStage: () => null})).toBe(true);
    });
  });
});
