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

describe("Key Value Pair Component", () => {
  const m            = require("mithril");
  const styles       = require('../index.scss');
  const KeyValuePair = require("views/components/key_value_pair").KeyValuePair;

  let $root, root;

  beforeEach(() => {
    [$root, root] = window.createDomElementForTest();
  });

  afterEach(unmount);
  afterEach(window.destroyDomElementForTest);

  it("should render key value pairs", () => {
    mount();

    expect($root.find(`.${styles.keyValuePair}`).children()).toHaveLength(3);

    expect($root.find(`.${styles.keyValuePair} .${styles.key}`).get(0)).toContainText('First Name');
    expect($root.find(`.${styles.keyValuePair} .${styles.value}`).get(0)).toContainText('Jon');

    expect($root.find(`.${styles.keyValuePair} .${styles.key}`).get(1)).toContainText('Last Name');
    expect($root.find(`.${styles.keyValuePair} .${styles.value}`).get(1)).toContainText('Doe');

    expect($root.find(`.${styles.keyValuePair} .${styles.key}`).get(2)).toContainText('email');
    expect($root.find(`.${styles.keyValuePair} .${styles.value}`).get(2)).toContainText('jdoe@example.com');
  });

  function mount() {
    debugger
    m.mount(root, {
      view() {
        return m(KeyValuePair, {
          data: {
            'First Name': 'Jon',
            'Last Name':  'Doe',
            'email':      'jdoe@example.com',
          }
        });
      }
    });

    m.redraw(true);
  }

  function unmount() {
    m.mount(root, null);
    m.redraw();
  }
});
