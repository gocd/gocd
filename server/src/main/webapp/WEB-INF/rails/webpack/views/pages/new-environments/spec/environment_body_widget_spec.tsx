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
import {Environments, EnvironmentWithOrigin} from "models/new-environments/environments";
import data from "models/new-environments/spec/test_data";
import {EnvironmentBody} from "views/pages/new-environments/environment_body_widget";
import {TestHelper} from "views/pages/spec/test_helper";

describe("Environments Body Widget", () => {
  const helper = new TestHelper();

  let environment: EnvironmentWithOrigin;
  let environments: Environments;

  beforeEach(() => {
    environments = new Environments();
    environment  = EnvironmentWithOrigin.fromJSON(data.environment_json());
    environments.push(environment);

    helper.mount(() => <EnvironmentBody environment={environment} environments={environments}
                                        onSuccessfulSave={_.noop}/>);
  });

  afterEach(() => helper.unmount());

  it("should render pipelines section", () => {
    expect(helper.byTestId("pipelines-for-" + environment.name())).toBeInDOM();
  });

  it("should render agents section", () => {
    expect(helper.byTestId("agents-for-" + environment.name())).toBeInDOM();
  });

  it("should render environment variables section", () => {
    expect(helper.byTestId("environment-variables-for-" + environment.name())).toBeInDOM();
  });

  describe("Environment Pipelines", () => {
    it("should render pipeline header", () => {
      expect(helper.textByTestId("pipelines-header")).toContain("Pipelines");
    });

    it("should render pipeline names", () => {
      expect(helper.textByTestId("pipelines-content")).toContain(environment.pipelines()[0].name());
      expect(helper.textByTestId("pipelines-content")).toContain(environment.pipelines()[1].name());
    });
  });

  describe("Environment Agents", () => {
    it("should render agent header", () => {
      expect(helper.textByTestId("agents-header")).toContain("Agents");
    });

    it("should render agent uuids", () => {
      expect(helper.textByTestId("agents-content")).toContain(environment.agents()[0].uuid());
      expect(helper.textByTestId("agents-content")).toContain(environment.agents()[1].uuid());
    });
  });

  describe("Environment Environment Variables", () => {
    it("should render environment variable header", () => {
      expect(helper.textByTestId("environment-variables-header")).toContain("Environment Variables");
    });

    it("should render help message when no plain text environment variables are specified", () => {
      //remove all plain text variables;
      environment.environmentVariables().plainTextVariables().forEach((envVar) => {
        environment.environmentVariables().remove(envVar);
      });
      m.redraw.sync();

      const expectedMessage = "No Plain Text Environment Variables are defined.";
      expect(helper.text("#no-plain-text-env-var")).toContain(expectedMessage);
    });

    it("should render help message when no secure environment variables are specified", () => {
      //remove all plain text variables;
      environment.environmentVariables().secureVariables().forEach((envVar) => {
        environment.environmentVariables().remove(envVar);
      });
      m.redraw.sync();

      const expectedMessage = "No Secure Environment Variables are defined.";
      expect(helper.text("#no-secure-env-var")).toContain(expectedMessage);
    });

    it("should render inner environment variable header", () => {
      const plainTextEnvVarHeader = "Plain Text Environment Variables";
      const secureEnvVarHeader    = "Secure Environment Variables";
      expect(helper.textByTestId("environment-variables-content")).toContain(plainTextEnvVarHeader);
      expect(helper.textByTestId("environment-variables-content")).toContain(secureEnvVarHeader);
    });

    it("should render plain text environment variable", () => {
      const environmentVariables = environment.environmentVariables().plainTextVariables();
      const plainTextEnvVar1     = environmentVariables[0].name() + " = " + environmentVariables[0].value();
      const plainTextEnvVar2     = environmentVariables[1].name() + " = " + environmentVariables[1].value();

      expect(helper.textByTestId("environment-variables-content")).toContain(plainTextEnvVar1);
      expect(helper.textByTestId("environment-variables-content")).toContain(plainTextEnvVar2);
    });

    it("should render secure environment variable", () => {
      const secureEnvVar1 = environment.environmentVariables().secureVariables()[0].name() + " = ******";
      const secureEnvVar2 = environment.environmentVariables().secureVariables()[0].name() + " = ******";

      expect(helper.textByTestId("environment-variables-content")).toContain(secureEnvVar1);
      expect(helper.textByTestId("environment-variables-content")).toContain(secureEnvVar2);
    });
  });

});
