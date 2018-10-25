/*
 * Copyright 2018 ThoughtWorks, Inc.
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

import {InputLabel} from "../input_label";
import {Stream} from "mithril/stream";

describe("Input Label Component", () => {
  const m             = require("mithril");
  const stream        = require("mithril/stream");
  const styles        = require('../index.scss');
  const simulateEvent = require("simulate-event");

  let $root: any, root: any;
  beforeEach(() => {
    // @ts-ignore
    [$root, root] = window.createDomElementForTest();
  });

  afterEach(unmount);
  // @ts-ignore
  afterEach(window.destroyDomElementForTest);

  it("should render input label component", () => {
    const label                    = "Enter magic word";
    const property: Stream<string> = stream("foo");
    const helpText                 = "Magic word is foo";

    mount(label, helpText, property);

    expect($root.find(`.${styles['form-group']}`)).toBeInDOM();

    expect($root.find(`.${styles['form-label']}`)).toContainText(label);
    expect($root.find(`.${styles['form-control']}`)).toHaveValue(property());
    expect($root.find(`.${styles['form-help']}`)).toContainText(helpText);
  });

  it("should update property model on input value change", () => {
    const label                    = "Enter magic word";
    const property: Stream<string> = stream("foo");
    const helpText                 = "Magic word is foo";

    mount(label, helpText, property);

    expect($root.find(`.${styles['form-group']}`)).toBeInDOM();

    expect($root.find(`.${styles['form-label']}`)).toContainText(label);
    expect($root.find(`.${styles['form-control']}`)).toHaveValue(property());
    expect($root.find(`.${styles['form-help']}`)).toContainText(helpText);

    const newValue: string = "bar";

    const searchField = $root.find(`.${styles['form-control']}`).get(0);
    $(searchField).val(newValue);
    simulateEvent.simulate(searchField, 'input');

    expect(property()).toEqual(newValue);
    expect($root.find(`.${styles['form-control']}`)).toHaveValue(newValue);
  });

  function mount(label: string, helpText: string, property: Stream<string>) {
    m.mount(root, {
      view() {
        return (<InputLabel label={label} helpText={helpText} property={property}/>)
      }
    });

    m.redraw();
  }

  function unmount() {
    m.mount(root, null);
    m.redraw();
  }
});
