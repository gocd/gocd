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
import {KeyValuePair, KeyValueTitle} from "views/components/key_value_pair/index";
import {TestHelper} from "views/pages/spec/test_helper";
import styles from "../index.scss";

describe("KeyValuePair", () => {
  const helper = new TestHelper();
  afterEach(helper.unmount.bind(helper));

  it("renders empty message if configured and there is no data", () => {
    helper.mount(() => <KeyValuePair data={new Map()} whenEmpty={<em>Ain't nothing there!</em>}/>);
    expect(helper.qa(`.${styles.keyValuePair} .${styles.key}`).length).toBe(0);
    expect(helper.textAll(`*`)).toContain("Ain't nothing there!");
  });

  it("should render key value pairs", () => {
    helper.mount(() => <KeyValuePair data={data()}/>);

    expect(helper.q(`.${styles.keyValuePair}`).children).toHaveLength(data().size);

    expect(helper.q(`.${styles.keyValuePair} .${styles.key}`)).toHaveText("First Name");
    expect(helper.q(`.${styles.keyValuePair} .${styles.value}`)).toHaveText("Jon");

    expect(helper.qa(`.${styles.keyValuePair} .${styles.key}`).item(1)).toHaveText("Last Name");
    expect(helper.qa(`.${styles.keyValuePair} .${styles.value}`).item(1)).toHaveText("Doe");

    expect(helper.qa(`.${styles.keyValuePair} .${styles.key}`).item(2)).toHaveText("email");
    expect(helper.qa(`.${styles.keyValuePair} .${styles.value}`).item(2)).toHaveText("jdoe@example.com");

    expect(helper.qa(`.${styles.keyValuePair} .${styles.value}`).item(3)).toHaveText("true");
    expect(helper.qa(`.${styles.keyValuePair} .${styles.value}`).item(4)).toHaveText("false");

    expect(helper.qa(`.${styles.keyValuePair} .${styles.value}`).item(5)).toHaveHtml("<em>(Not specified)</em>");
    expect(helper.qa(`.${styles.keyValuePair} .${styles.value}`).item(6)).toHaveHtml("<em>(Not specified)</em>");
    expect(helper.qa(`.${styles.keyValuePair} .${styles.value}`).item(7)).toHaveHtml("<em>(Not specified)</em>");
    expect(helper.qa(`.${styles.keyValuePair} .${styles.value}`).item(8)).toHaveHtml("<em>(Not specified)</em>");

    expect(helper.qa(`.${styles.keyValuePair} .${styles.value}`).item(9)).toHaveHtml("<strong>grrr!</strong>");

    expect(helper.qa(`.${styles.keyValuePair} .${styles.key}`).item(10)).toHaveText("Integer");
    expect(helper.qa(`.${styles.keyValuePair} .${styles.value}`).item(10)).toHaveText("1");

    expect(helper.qa(`.${styles.keyValuePair} .${styles.key}`).item(11)).toHaveText("Float");
    expect(helper.qa(`.${styles.keyValuePair} .${styles.value}`).item(11)).toHaveText("3.14");
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

});

describe("KeyValueTitle", () => {
  const helper = new TestHelper();
  afterEach(helper.unmount.bind(helper));

  it("should render key value title", () => {
    helper.mount(() => <KeyValueTitle {...data()} inline={false}/>);

    expect(helper.q(`.${styles.title}`)).toContainText("A Long Descriptive Title");
    expect(helper.q(`.${styles.title}`)).not.toHaveClass(styles.titleInline);
    expect(helper.byTestId("data-test-icon-span")).toContainText("Icon Goes Here");
  });

  it("should render key value title inline", () => {
    helper.mount(() => <KeyValueTitle {...data()} inline={true}/>);

    expect(helper.q(`.${styles.title}`)).toContainText("A Long Descriptive Title");
    expect(helper.q(`.${styles.title}`)).toHaveClass(styles.titleInline);
    expect(helper.byTestId("data-test-icon-span")).toContainText("Icon Goes Here");
  });

  function data() {
    return {
      title: "A Long Descriptive Title",
      image: (<span data-test-id="data-test-icon-span">Icon Goes Here</span>),
      titleTestId: "data-test-key-title",
    };
  }
});
