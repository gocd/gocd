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

import * as m from "mithril";
import {KeyValuePair} from "views/components/key_value_pair/index";
import * as styles from "../index.scss";

describe("KeyValuePair", () => {
  let $root: any, root: HTMLElement;

  beforeEach(() => {
    // @ts-ignore
    [$root, root] = window.createDomElementForTest();
  });

  afterEach(unmount);
  // @ts-ignore
  afterEach(window.destroyDomElementForTest);

  it("should render key value pairs", () => {
    mount();

    expect($root.find(`.${styles.keyValuePair}`).children()).toHaveLength(data().size);

    expect($root.find(`.${styles.keyValuePair} .${styles.key}`).get(0)).toHaveText("First Name");
    expect($root.find(`.${styles.keyValuePair} .${styles.value}`).get(0)).toHaveText("Jon");

    expect($root.find(`.${styles.keyValuePair} .${styles.key}`).get(1)).toHaveText("Last Name");
    expect($root.find(`.${styles.keyValuePair} .${styles.value}`).get(1)).toHaveText("Doe");

    expect($root.find(`.${styles.keyValuePair} .${styles.key}`).get(2)).toHaveText("email");
    expect($root.find(`.${styles.keyValuePair} .${styles.value}`).get(2)).toHaveText("jdoe@example.com");

    expect($root.find(`.${styles.keyValuePair} .${styles.value}`).get(3)).toHaveText("true");
    expect($root.find(`.${styles.keyValuePair} .${styles.value}`).get(4)).toHaveText("false");

    expect($root.find(`.${styles.keyValuePair} .${styles.value}`).get(5)).toHaveHtml("<em>(Not specified)</em>");
    expect($root.find(`.${styles.keyValuePair} .${styles.value}`).get(6)).toHaveHtml("<em>(Not specified)</em>");
    expect($root.find(`.${styles.keyValuePair} .${styles.value}`).get(7)).toHaveHtml("<em>(Not specified)</em>");
    expect($root.find(`.${styles.keyValuePair} .${styles.value}`).get(8)).toHaveHtml("<em>(Not specified)</em>");

    expect($root.find(`.${styles.keyValuePair} .${styles.value}`).get(9)).toHaveHtml("<strong>grrr!</strong>");
debugger;
    expect($root.find(`.${styles.keyValuePair} .${styles.key}`).get(10)).toHaveText("Integer");
    expect($root.find(`.${styles.keyValuePair} .${styles.value}`).get(10)).toHaveText("1");

    expect($root.find(`.${styles.keyValuePair} .${styles.key}`).get(11)).toHaveText("Float");
    expect($root.find(`.${styles.keyValuePair} .${styles.value}`).get(11)).toHaveText("3.14");
  });

  function data() {
    return new Map<string, m.Children>([
                                         // strings
                                         ["First Name", "Jon"],
                                         ["Last Name", "Doe"],
                                         ["email", "jdoe@example.com"],
                                         // booleans
                                         ["This should be true", true],
                                         ["This should be false", false],
                                         // null "emptyish"
                                         ["This should be unset", null],
                                         ["This should also be unset", undefined],
                                         ["This empty string should also be unset", "  \n\n \t\t  "],
                                         ["This empty array should also be unset", []],
                                         // html
                                         ["This should be bold", (<strong>grrr!</strong>)],
                                         //numbers
                                         ["Integer", 1],
                                         ["Float", 3.14],
                                       ]);
  }

  function mount() {
    m.mount(root, {
      view() {
        return <KeyValuePair data={data()}/>;
      }
    });

    m.redraw();
  }

  function unmount() {
    m.mount(root, null);
    m.redraw();
  }
});
