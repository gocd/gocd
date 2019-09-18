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
import _ from "lodash";
import m from "mithril";
import {EnvironmentVariable, EnvironmentVariables} from "models/environment_variables/types";
import {EnvironmentVariablesWidget} from "views/components/environment_variables";
import {TestHelper} from "views/pages/spec/test_helper";

describe("Environment Variables Widget", () => {
  const helper               = new TestHelper();
  const plainTextEnvVar      = new EnvironmentVariable("env1", "plain-text-value");
  const secureEnvVar         = new EnvironmentVariable("env1", undefined, true, "encrypted-value");
  const environmentVariables = new EnvironmentVariables(plainTextEnvVar, secureEnvVar);

  beforeEach(() => {
    helper.mount(() => <EnvironmentVariablesWidget environmentVariables={environmentVariables}
                                                   onAdd={_.noop}
                                                   onRemove={_.noop}/>);
  });

  afterEach(helper.unmount.bind(helper));

  describe("Plain Text Variables", () => {
    it("should have a title", () => {
      expect(helper.findByDataTestId("plain-text-env-var-title")).toBeInDOM();
    });

    it("should have an add button", () => {
      expect(helper.findByDataTestId("add-plain-text-env-var-btn")).toBeInDOM();
    });

    it("should have plain text environment variable fields", () => {
      const plainTextValue = plainTextEnvVar.value();
      expect(helper.findByDataTestId("env-var-name")[0]).toHaveValue(plainTextEnvVar.name());
      expect(helper.findByDataTestId("env-var-plain-text-value")[0]).toHaveValue(plainTextValue ? plainTextValue : "");
      expect(helper.findByDataTestId("remove-env-var-btn")[0]).toBeInDOM();
    });

    it("should have readonly fields if environment variable is non-editable", () => {
      expect(helper.findByDataTestId("env-var-name")[0] as HTMLElement).not.toHaveAttr("readonly");
      expect(helper.findByDataTestId("env-var-plain-text-value")[0] as HTMLElement).not.toHaveAttr("readonly");

      plainTextEnvVar.editable = () => false;
      m.redraw.sync();

      expect(helper.findByDataTestId("env-var-name")[0] as HTMLElement).toHaveAttr("readonly");
      expect(helper.findByDataTestId("env-var-plain-text-value")[0] as HTMLElement).toHaveAttr("readonly");
    });
  });

  describe("Secure Variables", () => {
    it("should have a title", () => {
      expect(helper.findByDataTestId("plain-text-env-var-title")).toBeInDOM();
    });

    it("should have an add button", () => {
      expect(helper.findByDataTestId("add-secure-env-var-btn")).toBeInDOM();
    });

    it("should have secure environment variable fields", () => {
      expect(helper.findByDataTestId("env-var-name")[1]).toHaveValue(secureEnvVar.name());
      expect(helper.findByDataTestId("env-var-secure-value")[0]).toBeInDOM();
      expect(helper.findByDataTestId("remove-env-var-btn")[1]).toBeInDOM();
    });
  });
});
