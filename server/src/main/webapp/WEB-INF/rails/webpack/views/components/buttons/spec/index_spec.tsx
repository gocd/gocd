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
import {Primary} from "views/components/buttons/index";
import {TestHelper} from "views/pages/spec/test_helper";
import * as buttonStyles from "../index.scss";

describe("Button", () => {

  const helper = new TestHelper();

  afterEach(helper.unmount.bind(helper));

  describe("Ajax Operation", () => {

    it("should show a spinner icon and disable button when ajax request is in progress", (done) => {
      const promise = new Promise<void>((resolve) => {
        resolve();
      }).then(() => {
        const ajaxButton = helper.byTestId("ajax-button");
        expect(ajaxButton).toHaveClass(buttonStyles.iconSpinner);
        expect(ajaxButton).toBeDisabled();
        done();
      });
      helper.mount(() => <Primary dataTestId="ajax-button" ajaxOperation={() => promise}>Save</Primary>);
      expect(helper.byTestId("ajax-button")).not.toHaveClass(buttonStyles.iconSpinner);
      helper.clickByTestId("ajax-button");
    });

    it("should ignore clicks when ajax operation is in progress", (done) => {
      const spy                           = jasmine.createSpy();
      const promises: Array<Promise<any>> = [];
      const simulateRemoteCall            = () => {
        const promise = new Promise<void>((resolve) => {
          setTimeout(() => {
            spy();
            resolve();
          });
        });
        promises.push(promise);
        return promise;
      };

      helper.mount(() => <Primary dataTestId="ajax-button"
                                  ajaxOperation={simulateRemoteCall}>Save</Primary>);
      helper.clickByTestId("ajax-button");
      helper.clickByTestId("ajax-button");
      Promise.all(promises).finally(() => {
        expect(spy).toHaveBeenCalledTimes(1);
        done();
      });
    });

    it("should honor disabled attribute when passed externally", () => {
      const promise = Promise.resolve();
      helper.mount(() => <Primary disabled={true} dataTestId="ajax-button" ajaxOperation={() => promise}>Save</Primary>);
      expect(helper.byTestId("ajax-button")).toBeDisabled();
    });
  });
});
