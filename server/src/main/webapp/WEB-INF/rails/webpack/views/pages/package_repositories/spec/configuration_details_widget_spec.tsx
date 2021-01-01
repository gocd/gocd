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

import m from "mithril";
import {TestHelper} from "views/pages/spec/test_helper";
import {ConfigurationDetailsWidget} from "../configuration_details_widget";
import styles from "../index.scss";

describe('ConfigurationDetailsWidgetSpec', () => {
  const helper = new TestHelper();

  afterEach((done) => helper.unmount(done));

  function mount(header: m.Children = "Header", data: Map<string, m.Children> = new Map()) {
    helper.mount(() => <ConfigurationDetailsWidget header={header} data={data}/>);
  }

  it('should render the header and configurations', () => {
    const map = new Map();
    map.set("Key1", "Value1");
    map.set("Key2", "Value2");
    mount("Configuration Header", map);

    expect(helper.byTestId('configuration-details-widget')).toBeInDOM();
    expect(helper.textByTestId('configuration-details-header')).toBe('Configuration Header');

    expect(helper.byTestId('configuration-details')).toBeInDOM();
    const configs = helper.qa('li', helper.byTestId('configuration-details'));

    expect(configs.length).toBe(2);
    expect(configs[0].textContent).toBe('Key1Value1');
    expect(configs[1].textContent).toBe('Key2Value2');
  });

  it('should expand on click of header', () => {
    const map = new Map();
    map.set("Key1", "Value1");
    map.set("Key2", "Value2");
    mount("Configuration Header", map);

    expect(helper.byTestId('configuration-details-header')).not.toHaveClass(styles.expanded);
    expect(helper.byTestId('configuration-details')).not.toHaveClass(styles.expanded);

    helper.clickByTestId('configuration-details-header');

    expect(helper.byTestId('configuration-details-header')).toHaveClass(styles.expanded);
    expect(helper.byTestId('configuration-details')).toHaveClass(styles.expanded);
  });
});
