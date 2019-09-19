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

import {asSelector} from "helpers/css_proxies";
import m from "mithril";
import Stream from "mithril/stream";
import {Parameter} from "models/new_pipeline_configs/parameter";
import {ParametersEditor} from "views/pages/pipeline_configs/parameters_editor";
import {TestHelper} from "views/pages/spec/test_helper";
import css from "../environment_variables_editor.scss";

describe("ParametersEditor", () => {
  const helper = new TestHelper();
  const sel = asSelector<typeof css>(css);
  const variables: Stream<Parameter[]> = Stream();

  beforeEach(() => {
    variables([]);
    helper.mount(() => {
      return <ParametersEditor parameters={variables}/>;
    });
  });

  afterEach(helper.unmount.bind(helper));

  it("adds variable", () => {
    addVariable("name", "value");
    addVariable("one", "two");
    assertModel(
      new Parameter("name", "value"),
      new Parameter("one", "two"),
    );
  });

  it("removes variable", () => {
    addVariable("blah", "value");
    removeVariable(addVariable("eek", "two"));
    removeVariable(addVariable("eek", "two"));
    assertModel(
      new Parameter("blah", "value"),
    );
  });

  function addVariable(name: string, value: string): Element {
    const context = helper.q(sel.envVars);
    helper.click("button", context);
    const variable = Array.from(helper.allByTestId("table-row", context)).pop()!;
    const fields = helper.qa("input", variable);
    helper.oninput(fields.item(0), name);
    helper.oninput(fields.item(1), value);
    return variable;
  }

  function removeVariable(variable: Element) {
    helper.click("button", variable);
  }

  function plainObj(obj: any): any {
    return JSON.parse(JSON.stringify(obj));
  }

  function assertModel(...expected: Parameter[]) {
    return expect(plainObj(variables())).toEqual(plainObj(expected));
  }
});
