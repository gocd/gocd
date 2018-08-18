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

const PersonalizeEditorVM = require("views/dashboard/models/personalize_editor_vm");

describe("Personalization Editor View Model", () => {
  it("blank config yields a wildcard filter", () => {
    const model = new PersonalizeEditorVM({}, {});

    expect(model.asFilter().type).toBe("blacklist");
    expect(model.asFilter().pipelines).toEqual([]);
  });

  it("checking the includeNewPipelines() sets the filter type", () => {
    const model = new PersonalizeEditorVM({}, {});

    check(model.includeNewPipelines);
    expect(model.asFilter().type).toBe("blacklist");

    uncheck(model.includeNewPipelines);
    expect(model.asFilter().type).toBe("whitelist");
  });

  it("includeNewPipelines() represents filter type as a checkbox state", () => {
    expect(new PersonalizeEditorVM({type: "whitelist"}, {}).includeNewPipelines()).toBe(false);
    expect(new PersonalizeEditorVM({type: "blacklist"}, {}).includeNewPipelines()).toBe(true);
  });

  it("maps pipeline state checkboxes to states", () => {
    const model = new PersonalizeEditorVM({}, {});

    expect(model.asFilter().state).toEqual([]);

    check(model.building);
    expect(model.asFilter().state).toEqual(["building"]);

    check(model.failing);
    expect(model.asFilter().state).toEqual(["building", "failing"]);

    uncheck(model.failing);
    expect(model.asFilter().state).toEqual(["building"]);
  });

  it("pipeline state checkboxes represent configured pipeline state filters", () => {
    const a = new PersonalizeEditorVM({state: []}, {});
    expect(a.building()).toBe(false);
    expect(a.failing()).toBe(false);

    const b = new PersonalizeEditorVM({state: ["building"]}, {});
    expect(b.building()).toBe(true);
    expect(b.failing()).toBe(false);

    const c = new PersonalizeEditorVM({state: ["failing"]}, {});
    expect(c.building()).toBe(false);
    expect(c.failing()).toBe(true);

    const d = new PersonalizeEditorVM({state: ["building", "failing"]}, {});
    expect(d.building()).toBe(true);
    expect(d.failing()).toBe(true);
  });
});

function check(stream) {
  stream(true);
}

function uncheck(stream) {
  stream(false);
}
