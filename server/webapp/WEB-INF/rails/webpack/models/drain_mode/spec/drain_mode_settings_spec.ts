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

import {DrainModeSettings} from "models/drain_mode/drain_mode_settings";

const TimeFormatter = require("helpers/time_formatter");

describe("Drain mode settings", () => {
  it("should serialize to json", () => {
    const drainModeSettings = new DrainModeSettings(true, "bob", "2018-12-04T06:35:56Z");

    expect(drainModeSettings.toSnakeCaseJSON()).toEqual({
                                                       drain: true,
                                                       updated_by: "bob",
                                                       updated_on: "04 Dec 2018"
                                                     });
  });

  it("should deserialize json to DrainModeSettings", () => {
    const drainModeSettings = DrainModeSettings.fromJSON(drainModeJSON());

    expect(drainModeSettings.drain()).toEqual(false);
    expect(drainModeSettings.updatedBy).toEqual("bob");
    expect(drainModeSettings.updatedOn).toEqual(TimeFormatter.formatInDate("2018-12-04T06:35:56Z"));
  });
});

function drainModeJSON() {
  return {
    _links: {
      self: {
        href: "http://localhost:8083/go/api/drain_mode/settings"
      },
      doc: {
        href: "https://api.gocd.org/current/#server-drain-mode"
      }
    },
    _embedded: {
      drain: false,
      updated_by: "bob",
      updated_on: "2018-12-04T06:35:56Z"
    }
  };
}
