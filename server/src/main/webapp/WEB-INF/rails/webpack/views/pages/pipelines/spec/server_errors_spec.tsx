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

import {asSelector} from "helpers/css_proxies";
import m from "mithril";
import Stream from "mithril/stream";
import {Errors} from "models/mixins/errors";
import css from "views/pages/pipelines/server_errors.scss";
import {TestHelper} from "views/pages/spec/test_helper";
import {ServerErrors} from "../server_errors";

const sel = asSelector<typeof css>(css);
const helper = new TestHelper();

describe("AddPipeline: ServerErrors", () => {
  afterEach(() => helper.unmount());

  it("displays a message", () => {
    helper.mount(() => <ServerErrors message={Stream("you suck")} details={Stream()}/>);
    expect(helper.q(sel.errorResponse)).toBeInDOM();
    expect(helper.q(`${sel.errorResponse} ol`)).not.toBeInDOM();
    expect(helper.q(`${sel.errorResponse} li`)).not.toBeInDOM();
    expect(helper.text(sel.errorResponse)).toBe("you suck");
  });

  it("displays a message with details", () => {
    helper.mount(() => <ServerErrors message={Stream("you suck")} details={Stream(new Errors({
      a: ["one", "two"],
      b: ["three"],
    }))}/>);
    expect(helper.q(sel.errorResponse)).toBeInDOM();
    expect(helper.q(`${sel.errorResponse} li`)).toBeInDOM();
    expect(helper.text(`${sel.errorResponse} span`)).toBe("you suck:");
    expect(helper.textAll(`${sel.errorResponse} li`)).toEqual([
      "a: one. two.",
      "b: three."
    ]);
  });
});
