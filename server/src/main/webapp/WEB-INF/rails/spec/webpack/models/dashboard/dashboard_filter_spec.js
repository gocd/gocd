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
import {DashboardFilter} from "models/dashboard/dashboard_filter";

describe("DashboardFilter", () => {
  describe("pipeline status filtering", () => {
    let stage, pipeline, filter;

    function Stage(status) {
      this.isFailed = () => status === "Failed";
      this.isBuilding = () => status === "Building";
      this.isFailing = () => status === "Failing";
    }

    beforeEach(() => {
      pipeline = {
        name: null,
        latestStage: () => stage
      };
    });

    it("should not show pipelines that have never run when state is set", () => {
      stage = null;

      filter = new DashboardFilter({state: ["failing"]});
      expect(filter.acceptsStatusOf(pipeline)).toBe(false);

      filter = new DashboardFilter({state: ["builing"]});
      expect(filter.acceptsStatusOf(pipeline)).toBe(false);

      filter = new DashboardFilter({state: []});
      expect(filter.acceptsStatusOf(pipeline)).toBe(true);
    });

    it("should filter when state=[failing]", () => {
      filter = new DashboardFilter({state: ["failing"]});

      stage = new Stage("Failed");
      expect(filter.acceptsStatusOf(pipeline)).toBe(true);

      stage = new Stage("Passing");
      expect(filter.acceptsStatusOf(pipeline)).toBe(false);
    });

    it("should filter when state=[building]", () => {
      filter = new DashboardFilter({state: ["building"]});

      stage = new Stage("Building");
      expect(filter.acceptsStatusOf(pipeline)).toBe(true);

      stage = new Stage("Failing");
      expect(filter.acceptsStatusOf(pipeline)).toBe(true);

      stage = new Stage("Failed");
      expect(filter.acceptsStatusOf(pipeline)).toBe(false);

      stage = new Stage("Passing");
      expect(filter.acceptsStatusOf(pipeline)).toBe(false);
    });

    it("should filter when state=[building, failing]", () => {
      filter = new DashboardFilter({state: ["building", "failing"]});

      stage = new Stage("Building");
      expect(filter.acceptsStatusOf(pipeline)).toBe(true);

      stage = new Stage("Failed");
      expect(filter.acceptsStatusOf(pipeline)).toBe(true);

      stage = new Stage("Passing");
      expect(filter.acceptsStatusOf(pipeline)).toBe(false);
    });

    it("should return true if state=[]", () => {
      filter = new DashboardFilter({state: []});

      stage = new Stage("Whatever");
      expect(filter.acceptsStatusOf(pipeline)).toBe(true);
    });
  });
});
