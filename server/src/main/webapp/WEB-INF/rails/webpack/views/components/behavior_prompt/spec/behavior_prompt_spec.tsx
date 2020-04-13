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

import m from "mithril";
import {TestHelper} from "views/pages/spec/test_helper";
import {BehaviorPrompt, STORAGE_KEY_PREFIX} from "../behavior_prompt";
import styles from "../behavior_prompt.scss";

describe("BehaviorPrompt", () => {
  const helper = new TestHelper();
  const key = "test";
  const storageKey = STORAGE_KEY_PREFIX.concat(".", key);

  afterEach(() => {
    helper.unmount();
    localStorage.removeItem(storageKey);
  });

  it("should display behavior prompt", () => {
    helper.mount(() => <BehaviorPrompt
      promptText="The box! You opened it, we came!"
      key={key}
      query=""
      position={{
        right: "-2px",
        top: "5px"
      }}
      />);

    expect(helper.byTestId("behavior-prompt")).toBeVisible();
    expect(helper.byTestId("behavior-prompt")).toContainText("The box! You opened it, we came!");
    expect(helper.byTestId("behavior-prompt")).toHaveClass(styles.arrowRight);
  });

  it("should not display the behavior prompt when query has value", () => {
    helper.mount(() => <BehaviorPrompt
      promptText="The box! You opened it, we came!"
      key={key}
      query="true"
      />);

    expect(helper.byTestId("behavior-prompt")).not.toExist();
    expect(localStorage.getItem(storageKey)).toBe("true");
  });

  it("should not display the behavior prompt when localStorage has value", () => {
    localStorage.setItem(storageKey, "true");
    helper.mount(() => <BehaviorPrompt
      promptText="The box! You opened it, we came!"
      key={key}
      query=""
      />);

    expect(helper.byTestId("behavior-prompt")).not.toExist();
    expect(localStorage.getItem(storageKey)).toBe("true");
  });

  describe("Position", () => {
    it("should apply css position offsets based on provided coordinates", () => {
      const position = {
        right: "-2px",
        top: "5px",
        bottom: "0px",
        left: "200px"
      };
      helper.mount(() => <BehaviorPrompt
        promptText="The box! You opened it, we came!"
        key={key}
        query=""
        position={position}
        />);

      const behaviorPrompt = helper.byTestId("behavior-prompt");

      expect(behaviorPrompt).toBeVisible();
      expect(behaviorPrompt.style.left).toBe("200px");
      expect(behaviorPrompt.style.right).toBe("-2px");
      expect(behaviorPrompt.style.top).toBe("5px");
      expect(behaviorPrompt.style.bottom).toBe("0px");
    });
  });
});
