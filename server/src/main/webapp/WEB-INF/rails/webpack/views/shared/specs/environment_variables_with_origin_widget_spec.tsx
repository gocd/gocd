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
import {SparkRoutes} from "helpers/spark_routes";
import m from "mithril";
import {EnvironmentVariables, EnvironmentVariableWithOrigin} from "models/environment_variables/types";
import {Origin, OriginType} from "models/origin";
import {TestHelper} from "views/pages/spec/test_helper";
import {EnvironmentVariablesWithOriginWidget} from "../environment_variables_with_origin_widget";

describe("Environment Variables Widget", () => {
  const helper               = new TestHelper();
  const plainTextEnvVar      = new EnvironmentVariableWithOrigin("env1", new Origin(OriginType.GoCD), "plain-text-variables-value");
  const secureEnvVar         = new EnvironmentVariableWithOrigin("env1", new Origin(OriginType.GoCD), undefined, true, "encrypted-value");
  const dummyVariable        = new EnvironmentVariableWithOrigin("dummy", new Origin(OriginType.ConfigRepo, "config-repo"));
  const environmentVariables = new EnvironmentVariables(plainTextEnvVar, secureEnvVar, dummyVariable);

  beforeEach(() => {
    helper.mount(() => <EnvironmentVariablesWithOriginWidget environmentVariables={environmentVariables}/>);
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
      const wrapper = helper.allByTestId("environment-variable-wrapper")[0];
      expect(helper.byTestId("env-var-name", wrapper)).toHaveValue(plainTextEnvVar.name());
      expect(helper.byTestId("env-var-value", wrapper)).toHaveValue(plainTextEnvVar.value()!);
      expect(helper.byTestId("remove-env-var-btn", wrapper)).toBeInDOM();
    });

    it("should have readonly fields if environment variable is non-editable", () => {
      const wrapper = helper.allByTestId("environment-variable-wrapper")[1];
      expect(helper.byTestId("env-var-name", wrapper)).toHaveAttr("readonly");
      expect(helper.byTestId("env-var-value", wrapper)).toHaveAttr("readonly");
      expect(helper.byTestId("remove-env-var-btn", wrapper)).not.toBeInDOM();
      expect(helper.byTestId("info-tooltip-wrapper", wrapper)).toBeInDOM();
      expect(helper.byTestId("info-tooltip-wrapper", wrapper)).toContainText(dummyVariable.reasonForNonEditable());
      const configRepoLink = helper.q("a", helper.byTestId("info-tooltip-wrapper", wrapper));
      expect(configRepoLink).toHaveAttr("href", SparkRoutes.ConfigRepoViewPath("config-repo"));
    });

    it("should display error if any", () => {
      plainTextEnvVar.errors().add("name", "some error");
      plainTextEnvVar.errors().add("value", "some error in value");

      m.redraw.sync();
      const wrapper = helper.allByTestId("environment-variable-wrapper")[0];
      expect(helper.byTestId("env-var-name", wrapper).parentElement).toHaveText("some error.");
      expect(helper.byTestId("env-var-value", wrapper).parentElement).toHaveText("some error in value.");
    });
  });

  describe("Secure Variables", () => {
    it("should have a title", () => {
      expect(helper.byTestId("secure-variables-title")).toBeInDOM();
    });

    it("should have an add button", () => {
      expect(helper.byTestId("add-secure-variables-btn")).toBeInDOM();
    });

    it("should have secure environment variable fields", () => {
      const wrapper = helper.allByTestId("environment-variable-wrapper")[2];
      expect(helper.byTestId("env-var-name", wrapper)).toHaveValue(secureEnvVar.name());
      expect(helper.byTestId("env-var-value", wrapper)).toBeInDOM();
      expect(helper.byTestId("remove-env-var-btn", wrapper)).toBeInDOM();
    });
  });
});
