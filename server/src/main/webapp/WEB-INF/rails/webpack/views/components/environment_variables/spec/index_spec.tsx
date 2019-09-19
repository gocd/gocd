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

import m from "mithril";
import {EnvironmentVariable, EnvironmentVariables} from "models/environment_variables/types";
import {EnvironmentVariablesWidget} from "views/components/environment_variables";
import {TestHelper} from "views/pages/spec/test_helper";

describe("Environment Variables Widget", () => {
  const helper               = new TestHelper();
  const plainTextEnvVar      = new EnvironmentVariable("env1", "plain-text-variables-value");
  const secureEnvVar         = new EnvironmentVariable("env1", undefined, true, "encrypted-value");
  const environmentVariables = new EnvironmentVariables(plainTextEnvVar, secureEnvVar);

  beforeEach(() => {
    helper.mount(() => <EnvironmentVariablesWidget environmentVariables={environmentVariables}/>);
  });

  afterEach(helper.unmount.bind(helper));

  describe("Plain Text Variables", () => {
    it("should have a title", () => {
      expect(helper.byTestId("plain-text-variables-title")).toBeInDOM();
    });

    it("should have an add button", () => {
      expect(helper.byTestId("add-plain-text-variables-btn")).toBeInDOM();
    });

    it("should have plain text environment variable fields", () => {
      const plainTextValue = plainTextEnvVar.value();
      expect(helper.byTestId("env-var-name")).toHaveValue(plainTextEnvVar.name());
      expect(helper.byTestId("env-var-value")).toHaveValue(plainTextValue ? plainTextValue : "");
      expect(helper.byTestId("remove-env-var-btn")).toBeInDOM();
    });

    it("should have readonly fields if environment variable is non-editable", () => {
      expect(helper.byTestId("env-var-name")).not.toHaveAttr("readonly");
      expect(helper.byTestId("env-var-value")).not.toHaveAttr("readonly");

      plainTextEnvVar.editable = () => false;
      m.redraw.sync();

      expect(helper.byTestId("env-var-name")).toHaveAttr("readonly");
      expect(helper.byTestId("env-var-value")).toHaveAttr("readonly");
    });
    it("should display error if any", () => {
      plainTextEnvVar.errors().add("name", "some error");
      plainTextEnvVar.errors().add("value", "some error in value");

      m.redraw.sync();

      expect(helper.byTestId("env-var-name").parentElement).toHaveText("some error.");
      expect(helper.byTestId("env-var-value").parentElement).toHaveText("some error in value.");
    });
  });

  describe("Secure Variables", () => {
    it("should have a title", () => {
      expect(helper.byTestId("plain-text-variables-title")).toBeInDOM();
    });

    it("should have an add button", () => {
      expect(helper.byTestId("add-secure-variables-btn")).toBeInDOM();
    });

    it("should have secure environment variable fields", () => {
      expect(helper.allByTestId("env-var-name")[1]).toHaveValue(secureEnvVar.name());
      expect(helper.allByTestId("env-var-value")[1]).toBeInDOM();
      expect(helper.allByTestId("remove-env-var-btn")[1]).toBeInDOM();
    });
  });
});
