/*
 * Copyright 2020 ThoughtWorks, Inc.
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

import {docsUrl} from "gen/gocd_version";
import m from "mithril";
import Stream from "mithril/stream";
import {TextField} from "views/components/forms/input_fields";
import {TestHelper} from "views/pages/spec/test_helper";

describe("Help Text on input fields", () => {

  const formValue = Stream("initial value");
  const helper    = new TestHelper();

  afterEach(() => helper.unmount());

  it("should render help text without a documentation link", () => {
    helper.mount(() => <TextField required={true}
                                  helpText="Enter your username here"
                                  label="Username"
                                  placeholder="username"
                                  property={formValue}/>);
    const id              = `${helper.q("input").getAttribute("id")}-help-text`;
    const helpTextElement = helper.q(`#${id}`);
    expect(helpTextElement).toContainText("Enter your username here");
    expect(helpTextElement).not.toContainText("Learn More");
  });

  it("should render help text with a documentation link", () => {
    helper.mount(() => <TextField required={true}
                                  helpText="Enter your username here"
                                  docLink="foo/bar#baz"
                                  label="Username"
                                  placeholder="username"
                                  property={formValue}/>);
    const id              = `${helper.q("input").getAttribute("id")}-help-text`;
    const helpTextElement = helper.q(`#${id}`);
    expect(helpTextElement).toContainText("Enter your username here");
    expect(helpTextElement).toContainText("Learn More");

    const expectedUrl = docsUrl("foo/bar#baz");
    const link        = helper.q("a");

    expect(link).toHaveAttr("href", expectedUrl);
    expect(link).toHaveAttr("target", "_blank");
  });

});
