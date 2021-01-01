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

import {asSelector} from "helpers/css_proxies";
import m from "mithril";
import Stream from "mithril/stream";
import {TestHelper} from "views/pages/spec/test_helper";
import css from "../forms.scss";
import {LiveValidatingInputField} from "../live_validating_input";

describe("LiveValidatingInputField", () => {
  const data = Stream("");
  const sel = asSelector(css);
  const helper = new TestHelper();

  function onlyNumbers(value: string) {
    if (value && /\D/.test(value)) {
      return "Only enter numbers";
    }
  }

  beforeEach(() => {
    data("");
    helper.mount(() => <LiveValidatingInputField label="type here pls." property={data} validator={onlyNumbers}/>);
  });

  afterEach(() => helper.unmount());

  it("should fire `invalid` event and display an error message when the value fails the validator function", (done) => {
    helper.q("input[type]").addEventListener("invalid", (e) => {
      expect((e.target as HTMLInputElement).validationMessage).toBe("Only enter numbers");

      setTimeout(() => {
        expect(helper.text(sel.formErrorText)).toBe("Only enter numbers");
        done();
      }, 0);
    });

    helper.oninput("input[type]", "hi");
  });

  it("allows valid input", (done) => {
    const input = helper.q("input");
    expect(input).toBeInDOM();

    input.addEventListener("invalid", () => done.fail("should be valid"));
    helper.oninput(input, "8675309");

    setTimeout(() => {
      expect(helper.q(sel.formErrorText)).not.toBeInDOM();
      done();
    }, 0);
  });

});
