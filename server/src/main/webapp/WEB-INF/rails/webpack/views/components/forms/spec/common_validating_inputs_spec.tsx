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
import _ from "lodash";
import m from "mithril";
import Stream from "mithril/stream";
import {TestHelper} from "views/pages/spec/test_helper";
import {IdentifierInputField} from "../common_validating_inputs";
import css from "../forms.scss";

describe("Common Validating Input Fields:", () => {
  const helper = new TestHelper();
  const sel = asSelector(css);

  describe("IdentifierInputField", () => {
    const data: Stream<string> = Stream();
    const required: Stream<boolean> = Stream();

    beforeEach(() => {
      data("");
      required(false);
    });

    afterEach(() => helper.unmount());

    it("allows valid input", (done) => {
      helper.mount(() => <IdentifierInputField property={data} required={required()}/>);

      const input = helper.q("input") as HTMLInputElement;
      expect(input).toBeInDOM();

      input.addEventListener("invalid", () => done.fail("should be valid"));
      helper.oninput(input, "Good-Name");

      setTimeout(() => {
        expect(input.validationMessage).toBe("");
        expect(helper.q(sel.formErrorText)).not.toBeInDOM();
        done();
      }, 0);
    });

    it("allows empty input when required is false", (done) => {
      required(false);
      helper.mount(() => <IdentifierInputField property={data} required={required()}/>);

      const input = helper.q("input") as HTMLInputElement;
      expect(input).toBeInDOM();

      input.addEventListener("invalid", () => done.fail("should be valid"));
      helper.oninput(input, "");

      setTimeout(() => {
        expect(input.validationMessage).toBe("");
        expect(helper.q(sel.formErrorText)).not.toBeInDOM();
        done();
      }, 0);
    });

    it("refutes empty input when required is true", (done) => {
      required(true);
      helper.mount(() => <IdentifierInputField property={data} required={required()}/>);

      const input = helper.q("input") as HTMLInputElement;
      expect(input).toBeInDOM();

      input.addEventListener("invalid", () => {
        expect(input.validationMessage).toBe(IdentifierInputField.MISSING);

        setTimeout(() => {
          expect(helper.text(sel.formErrorText)).toBe(IdentifierInputField.MISSING);
          done();
        }, 0);
      });

      helper.oninput(input, "");
    });

    it("refutes values with invalid chars", (done) => {
      helper.mount(() => <IdentifierInputField property={data} required={required()}/>);

      const input = helper.q("input") as HTMLInputElement;
      expect(input).toBeInDOM();

      input.addEventListener("invalid", () => {
        expect(input.validationMessage).toBe(IdentifierInputField.INVALID_CHARS);

        setTimeout(() => {
          expect(helper.text(sel.formErrorText)).toBe(IdentifierInputField.INVALID_CHARS);
          done();
        }, 0);
      });

      helper.oninput(input, "bad name?");
    });

    it("refutes values when first character is a period", (done) => {
      helper.mount(() => <IdentifierInputField property={data} required={required()}/>);

      const input = helper.q("input") as HTMLInputElement;
      expect(input).toBeInDOM();

      input.addEventListener("invalid", () => {
        expect(input.validationMessage).toBe(IdentifierInputField.INVALID_START);

        setTimeout(() => {
          expect(helper.text(sel.formErrorText)).toBe(IdentifierInputField.INVALID_START);
          done();
        }, 0);
      });

      helper.oninput(input, ".why-me");
    });

    it("refutes values that are too long", (done) => {
      helper.mount(() => <IdentifierInputField property={data} required={required()}/>);

      const input = helper.q("input") as HTMLInputElement;
      expect(input).toBeInDOM();

      input.addEventListener("invalid", () => {
        expect(input.validationMessage).toBe(IdentifierInputField.TOO_LONG);

        setTimeout(() => {
          expect(helper.text(sel.formErrorText)).toBe(IdentifierInputField.TOO_LONG);
          done();
        }, 0);
      });

      helper.oninput(input, _.repeat("a", 256));
    });
  });
});
