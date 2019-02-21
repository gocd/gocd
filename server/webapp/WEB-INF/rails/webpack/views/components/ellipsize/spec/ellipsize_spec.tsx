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

import * as m from "mithril";
import * as simulateEvent from "simulate-event";
import {Ellipsize} from "views/components/ellipsize/index";
import * as styles from "../index.scss";

describe("EllipsizeComponent", () => {
  let $root: any, root: any;
  beforeEach(() => {
    // @ts-ignore
    [$root, root] = window.createDomElementForTest();
  });

  afterEach(unmount);

  describe("Fixed", () => {
    it("should ellipsize when text string is bigger then specified size", () => {
      mount("This is small", 5, true);

      expect(findByClass(styles.wrapper)).toBeInDOM();
      expect(findByClass(styles.wrapper)).toHaveClass(styles.fixEllipsized);
      expect(findByClass(styles.ellipsisActionButton)).not.toBeInDOM();
    });
  });

  describe("Size", () => {
    it("should show entire text if is smaller then specified size", () => {
      mount("This is small", 15, false);

      expect(findByClass(styles.wrapper)).toBeInDOM();
      expect(findByClass(styles.wrapper)).not.toHaveClass(styles.fixEllipsized);
      expect(findByClass(styles.wrapper)).toHaveText("This is small");
      expect(findByClass(styles.ellipsisActionButton)).not.toBeInDOM();
    });

    it("should show text of only specified size", () => {
      mount("This is small", 5, false);

      expect(findByClass(styles.wrapper)).toBeInDOM();
      expect(findByClass(styles.wrapper)).not.toHaveClass(styles.fixEllipsized);
      expect(findByDataTestId("ellipsized-content")).toHaveText("This ...");
      expect(findByClass(styles.ellipsisActionButton)).toBeInDOM();
    });
  });

  describe("Actions", () => {
    it("should expand and show full text on click of more", () => {
      mount("This is small", 5, false);

      expect(findByClass(styles.wrapper)).toBeInDOM();
      expect(findByClass(styles.wrapper)).not.toHaveClass(styles.fixEllipsized);
      expect(findByDataTestId("ellipsized-content")).toHaveText("This ...");
      expect(findByClass(styles.ellipsisActionButton)).toHaveText("more");

      simulateEvent.simulate(findByClass(styles.ellipsisActionButton).get(0), "click");
      m.redraw();

      expect(findByDataTestId("ellipsized-content")).toHaveText("This is small");
      expect(findByClass(styles.ellipsisActionButton)).toBeInDOM();
      expect(findByClass(styles.ellipsisActionButton)).toHaveText("less");
    });

    it("should ellipsize the text on click of less", () => {
      mount("This is small", 5, false);

      expect(findByClass(styles.wrapper)).toBeInDOM();
      expect(findByClass(styles.wrapper)).not.toHaveClass(styles.fixEllipsized);
      expect(findByDataTestId("ellipsized-content")).toHaveText("This ...");
      expect(findByClass(styles.ellipsisActionButton)).toHaveText("more");

      simulateEvent.simulate(findByClass(styles.ellipsisActionButton).get(0), "click");
      m.redraw();

      expect(findByDataTestId("ellipsized-content")).toHaveText("This is small");
      expect(findByClass(styles.ellipsisActionButton)).toBeInDOM();
      expect(findByClass(styles.ellipsisActionButton)).toHaveText("less");

      simulateEvent.simulate(findByClass(styles.ellipsisActionButton).get(0), "click");
      m.redraw();

      expect(findByDataTestId("ellipsized-content")).toHaveText("This ...");
    });
  });

  function mount(text: string, size = 20, fixed = false) {
    m.mount(root, {
      view() {
        return (
          <Ellipsize text={text} size={size} fixed={fixed}/>);
      }
    });

    m.redraw();
  }

  function unmount() {
    m.mount(root, null);
    m.redraw();
  }

  function findByClass(className: string) {
    return $root.find(`.${className}`);
  }

  function findByDataTestId(id: string) {
    return $root.find(`[data-test-id='${id}']`);
  }
});
