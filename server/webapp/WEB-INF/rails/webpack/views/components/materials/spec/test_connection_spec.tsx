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

import SparkRoutes from "helpers/spark_routes";
import * as m from "mithril";
import {GitMaterialAttributes, Material} from "models/materials/types";
import * as simulateEvent from "simulate-event";
import {TestHelper} from "views/pages/spec/test_helper";
import {TestConnection} from "../test_connection";
import * as styles from "../test_connection.scss";

describe("Materials: TestConnection", () => {
  const helper = new TestHelper();
  const TEST_CONNECTION_URL = SparkRoutes.materialConnectionCheck();
  const invalidMaterial = new Material("git", new GitMaterialAttributes());
  const validMaterial = new Material("git", new GitMaterialAttributes("SomeRepo", false, "https://github.com/gocd/gocd", "master"));

  afterEach(helper.unmount.bind(helper));

  it("Renders success message when successful connection", (done) => {
    jasmine.Ajax.withMock(() => {
      const response = {message: "Connection OK."};

      jasmine.Ajax.stubRequest(TEST_CONNECTION_URL, payload(validMaterial), "POST")
        .andReturn({
          responseText: JSON.stringify(response),
          status: 200,
          responseHeaders: {
            "Content-Type": "application/vnd.go.cd.v1+json",
            "ETag": "ETag"
          }
        });

      helper.mount(() => <TestConnection material={validMaterial} complete={() => {
        setTimeout(() => { // make this async so as to allow mithril to update the dom
          expect(find(helper, "test-connection-icon")).toHaveClass(styles.testConnectionSuccess);
          expect(find(helper, "flash-message-success").querySelector("p")).toContainText("Connection OK");
          done();
        }, 0);
      }}/>);

      expect(find(helper, 'test-connection-button')).toBeVisible();
      simulateEvent.simulate(find(helper, "test-connection-button"), "click");
      expect(jasmine.Ajax.requests.count()).toEqual(1);
    });
  });

  it("Renders error message when connection is not successful", (done) => {
    jasmine.Ajax.withMock(() => {
      const response = {message: "Error while parsing material URL"};

      jasmine.Ajax.stubRequest(TEST_CONNECTION_URL, payload(invalidMaterial), "POST")
        .andReturn({
          responseText: JSON.stringify(response),
          status: 422,
          responseHeaders: {
            "Content-Type": "application/vnd.go.cd.v1+json",
            "ETag": "ETag"
          }
        });

      helper.mount(() => <TestConnection material={invalidMaterial} complete={() => {
        setTimeout(() => { // make this async so as to allow mithril to update the dom
          expect(find(helper, "test-connection-icon")).toHaveClass(styles.testConnectionFailure);
          expect(find(helper, "flash-message-alert").querySelector("pre")).toContainText("Error while parsing material URL");
          done();
        }, 0);
      }}/>);

      expect(find(helper, 'test-connection-button')).toBeVisible();
      simulateEvent.simulate(find(helper, "test-connection-button") as Element, "click");
      expect(jasmine.Ajax.requests.count()).toEqual(1);
    });
  });

  function find(helper: TestHelper, id: string): Element {
    return helper.findByDataTestId(id)[0];
  }

  function payload(material: Material): string {
    return JSON.stringify(material.toApiPayload());
  }
});
