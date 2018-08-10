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
const Stream          = require('mithril/stream');

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
        expect(filter.byPipelines("p1")).toBe(false);
        expect(filter.byPipelines("p2")).toBe(false);
      });

      it("should return true if the pipeline is not on the list", () => {
        expect(filter.byPipelines("p3")).toBe(true);
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
        expect(filter.byPipelines("p1")).toBe(true);
        expect(filter.byPipelines("p2")).toBe(true);
      });

      it("should return false if the pipeline is not on the list", () => {
        expect(filter.byPipelines("p3")).toBe(false);
      });
    });
  });

  describe("state filtering", () => {
    let stage, pipeline, filter;
    beforeEach(() => {
      stage = {
        isFailed: Stream(),
        isBuilding: Stream()
      };
      pipeline = {
        name: null,
        latestStage: () => { return stage; }
      };
      filter = new DashboardFilter({name: "blue_diamond"});
    });

    it("should properly filter when state=[failing]", () => {
      filter.state = ["failing"];

      stage.isFailed(true);
      expect(filter.byState(pipeline)).toBe(true);

      stage.isFailed(false);
      expect(filter.byState(pipeline)).toBe(false);
    });

    it("should properly filter when state=[building]", () => {
      filter.state = ["building"];

      stage.isBuilding(true);
      expect(filter.byState(pipeline)).toBe(true);

      stage.isBuilding(false);
      expect(filter.byState(pipeline)).toBe(false);
    });

    it("should properly filter when state=[building, failing]", () => {
      filter.state = ["building", "failing"];

      stage.isBuilding(true);
      stage.isFailed(true);
      expect(filter.byState(pipeline)).toBe(true);

      stage.isBuilding(false);
      expect(filter.byState(pipeline)).toBe(true);

      stage.isFailed(false);
      expect(filter.byState(pipeline)).toBe(false);

      stage.isBuilding(true);
      expect(filter.byState(pipeline)).toBe(true);
    });

    it("should return true if state=[]", () => {
      filter.state = [];
      expect(filter.byState(pipeline)).toBe(true);
    });

    it("should return true if there's no latest stage or instance", () => {
      expect(filter.byState({latestStage: () => { return;}})).toBe(true);
    });
  });
});
