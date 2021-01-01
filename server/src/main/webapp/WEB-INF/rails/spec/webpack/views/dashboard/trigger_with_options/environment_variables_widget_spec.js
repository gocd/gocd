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
import {TestHelper} from "views/pages/spec/test_helper";
import {EnvironmentVariables} from "models/dashboard/environment_variables";
import * as EnvironmentVariablesWidget from "views/dashboard/trigger_with_options/environment_variables_widget";
import m from "mithril";

describe("Dashboard Environment Variables Trigger Widget", () => {

  const helper = new TestHelper();

  describe("Plain Text Variables", () => {
    const json = [
      {
        "name":   "version",
        "secure": false,
        "value":  "1.0.0"
      },
      {
        "name":   "foobar",
        "secure": false,
        "value":  "asdf"

      }
    ];

    let variables = EnvironmentVariables.fromJSON(json);

    beforeEach(() => {
      variables = EnvironmentVariables.fromJSON(json);
      helper.mount(() => m(EnvironmentVariablesWidget.Plain, {
        variables
      }));
    });

    afterEach(helper.unmount.bind(helper));

    it("should render plain text variables", () => {
      expect(helper.qa('.environment-variables .name')).toHaveLength(2);
      expect(helper.qa('.environment-variables input')).toHaveLength(2);

      expect(helper.qa('.environment-variables .name')[0]).toContainText(json[0].name);
      expect(helper.qa('.environment-variables .name')[1]).toContainText(json[1].name);

      expect(helper.qa('.environment-variables input')[0]).toHaveValue(json[0].value);
      expect(helper.qa('.environment-variables input')[1]).toHaveValue(json[1].value);
    });

    it("it should display variable overriden message", () => {
      const valueInputField = helper.q('.environment-variables input');

      expect(valueInputField).toHaveValue(json[0].value);
      expect(helper.q('.overridden-message')).not.toExist();

      const newValue = "ldap";
      helper.oninput(valueInputField, newValue);

      expect(variables[0].value()).toBe(newValue);
      expect(valueInputField).toHaveValue(newValue);
      expect(helper.text('.overridden-message')).toContain(`The value is overridden. Default value :${json[0].value}`);
    });
  });

  describe("Secure Text Variables", () => {
    const json = [
      {
        "name":   "password",
        "secure": true,
      },
      {
        "name":   "ip_address",
        "secure": true,
      }
    ];

    const variables = EnvironmentVariables.fromJSON(json);

    beforeEach(() => {
      helper.mount(() => m(EnvironmentVariablesWidget.Secure, {
        variables
      }));
    });

    afterEach(helper.unmount.bind(helper));

    it("should render Secure text variables", () => {
      expect(helper.qa('.environment-variables .name')).toHaveLength(2);
      expect(helper.qa('.environment-variables input')).toHaveLength(2);

      expect(helper.qa('.environment-variables .name')[0]).toContainText(json[0].name);
      expect(helper.qa('.environment-variables .name')[1]).toContainText(json[1].name);

      expect(helper.qa('.environment-variables input')[0]).toHaveValue('*****');
      expect(helper.qa('.environment-variables input')[1]).toHaveValue('*****');
    });

    it("it should display variable overriden message", () => {
      const valueInputField = helper.q('.environment-variables input');
      expect(valueInputField).toBeDisabled();

      expect(valueInputField).toHaveValue('*****');
      expect(helper.q('.reset')).not.toExist();

      helper.click('.override');

      expect(valueInputField).not.toBeDisabled();
      expect(helper.q('.reset')).toBeInDOM();

      const newValue = "ldap";
      helper.oninput(valueInputField, newValue);

      expect(variables[0].value()).toBe(newValue);
      expect(valueInputField).toHaveValue(newValue);

      helper.click('.reset');

      expect(valueInputField).toBeDisabled();
      expect(helper.q('.reset')).not.toExist();
    });
  });
});
