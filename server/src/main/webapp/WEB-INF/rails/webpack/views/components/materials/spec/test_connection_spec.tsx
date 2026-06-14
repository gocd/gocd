/*
 * Copyright Thoughtworks, Inc.
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

import {SparkRoutes} from "helpers/spark_routes";
import m from "mithril";
import {GitMaterialAttributes, Material} from "models/materials/types";
import {TestHelper} from "views/pages/spec/test_helper";
import {ConfigRepo} from "../../../../models/config_repos/types";
import {TestConnection} from "../test_connection";
import styles from "../test_connection.scss";

describe("Materials: TestConnection", () => {
  const helper              = new TestHelper();
  const testPipelineGroup   = "test-group";
  const testPipelineName    = "test-pipeline";
  const baseUrl             = SparkRoutes.materialConnectionCheck("");
  const baseUrlRegex        = new RegExp(baseUrl.replace("?", "\\?"));
  const invalidMaterial     = new Material("git", new GitMaterialAttributes());
  const validMaterial       = new Material("git", new GitMaterialAttributes("SomeRepo", false, "https://github.com/gocd/gocd", "master"));

  afterEach(helper.unmount.bind(helper));

  it("Renders success message when successful connection", (done) => {
    jasmine.Ajax.withMock(() => {
      const response = {message: "Connection OK."};

      jasmine.Ajax.stubRequest(baseUrlRegex, payload(validMaterial), "POST")
             .andReturn({
                          responseText:    JSON.stringify(response),
                          status:          200,
                          responseHeaders: {
                            "Content-Type": "application/vnd.go.cd.v1+json",
                            "ETag":         "ETag"
                          }
                        });

      helper.mount(() => <TestConnection material={validMaterial} group={testPipelineGroup} complete={() => {
        expect(helper.byTestId("test-connection-button").matches("[disabled]")).toBe(true); // disabled while connection is in progress

        setTimeout(() => { // make this async so as to allow mithril to update the dom
          m.redraw.sync();
          expect(helper.byTestId("test-connection-button").matches("[disabled]")).toBe(false); // enabled on complete
          expect(helper.byTestId("test-connection-icon")).toHaveClass(styles.testConnectionSuccess);
          expect(helper.byTestId("flash-message-success").querySelector("p")).toContainText("Connection OK");
          done();
        }, 0);
      }}/>);

      expect(helper.byTestId('test-connection-button')).toBeVisible();
      helper.clickByTestId("test-connection-button");
      expect(jasmine.Ajax.requests.count()).toEqual(1);

      const request = jasmine.Ajax.requests.mostRecent();
      expect(request.url).toEqual(`${baseUrl}=${testPipelineGroup}`);
    });
  });

  it("Renders error message when connection is not successful", (done) => {
    jasmine.Ajax.withMock(() => {
      const response = {message: "Error while parsing material URL"};

      jasmine.Ajax.stubRequest(baseUrlRegex, payload(invalidMaterial), "POST")
             .andReturn({
                          responseText:    JSON.stringify(response),
                          status:          422,
                          responseHeaders: {
                            "Content-Type": "application/vnd.go.cd.v1+json",
                            "ETag":         "ETag"
                          }
                        });

      helper.mount(() => <TestConnection material={invalidMaterial} group={testPipelineGroup} complete={() => {
        expect(helper.byTestId("test-connection-button").matches("[disabled]")).toBe(true); // disabled while connection is in progress

        setTimeout(() => { // make this async so as to allow mithril to update the dom
          m.redraw.sync();
          expect(helper.byTestId("test-connection-button").matches("[disabled]")).toBe(false); // enabled on complete
          expect(helper.byTestId("test-connection-icon")).toHaveClass(styles.testConnectionFailure);
          expect(helper.byTestId("flash-message-alert").querySelector("pre")).toContainText("Error while parsing material URL");
          done();
        }, 10);
      }}/>);

      expect(helper.byTestId('test-connection-button')).toBeVisible();
      helper.clickByTestId("test-connection-button");
      expect(jasmine.Ajax.requests.count()).toEqual(1);
    });
  });

  it('should include pipeline name in the query params when defined', (done) => {
    jasmine.Ajax.withMock(() => {
      jasmine.Ajax.stubRequest(baseUrlRegex)
             .andReturn({
                          responseText:    JSON.stringify({message: "Connection OK."}),
                          status:          200,
                          responseHeaders: {
                            "Content-Type": "application/vnd.go.cd.v1+json",
                            "ETag":         "ETag"
                          }
                        });

      helper.mount(() => <TestConnection material={validMaterial} group={testPipelineGroup} pipeline={testPipelineName} complete={() => {
        setTimeout(() => { // make this async so as to allow mithril to update the dom
          done();
        }, 0);
      }}/>);

      expect(helper.byTestId('test-connection-button')).toBeVisible();
      helper.clickByTestId("test-connection-button");
      expect(jasmine.Ajax.requests.count()).toEqual(1);

      const request = jasmine.Ajax.requests.mostRecent();
      expect(request.url).toEqual(`${baseUrl}=${testPipelineGroup}&pipeline_name=${testPipelineName}`);
      const data = request.data();
      expect(Object.keys(data)).toEqual(['attributes', 'type']);
    });
  });

  describe("Config Repo Check Connection", () => {
    const testConfigRepoId = 'repo1';
    const testConfigRepoConnectionUrl = SparkRoutes.configRepoConnectionCheck(testConfigRepoId);
    const material = new Material('git', new GitMaterialAttributes("git", true, "some-git-repo"));
    const configRepo = new ConfigRepo(testConfigRepoId, "json", material);

    it("Renders success message when successful connection", (done) => {
      jasmine.Ajax.withMock(() => {
        const response = {message: "Connection OK."};

        jasmine.Ajax.stubRequest(testConfigRepoConnectionUrl, payload(validMaterial), "POST")
          .andReturn({
            responseText:    JSON.stringify(response),
            status:          200,
            responseHeaders: {
              "Content-Type": "application/vnd.go.cd.v4+json",
              "ETag":         "ETag"
            }
          });

        helper.mount(() => <TestConnection material={validMaterial} configRepo={configRepo} complete={() => {
          expect(helper.byTestId("test-connection-button").matches("[disabled]")).toBe(true); // disabled while connection is in progress

          setTimeout(() => { // make this async so as to allow mithril to update the dom
            m.redraw.sync();
            expect(helper.byTestId("test-connection-button").matches("[disabled]")).toBe(false); // enabled on complete
            expect(helper.byTestId("test-connection-icon")).toHaveClass(styles.testConnectionSuccess);
            expect(helper.byTestId("flash-message-success").querySelector("p")).toContainText("Connection OK");
            done();
          }, 0);
        }}/>);

        expect(helper.byTestId('test-connection-button')).toBeVisible();
        helper.clickByTestId("test-connection-button");
        expect(jasmine.Ajax.requests.count()).toEqual(1);
      });
    });

    it("Renders error message when connection is not successful", (done) => {
      jasmine.Ajax.withMock(() => {
        const response = {message: "Error while parsing material URL"};

        jasmine.Ajax.stubRequest(testConfigRepoConnectionUrl, payload(invalidMaterial), "POST")
          .andReturn({
            responseText:    JSON.stringify(response),
            status:          422,
            responseHeaders: {
              "Content-Type": "application/vnd.go.cd.v1+json",
              "ETag":         "ETag"
            }
          });

        helper.mount(() => <TestConnection material={invalidMaterial} configRepo={configRepo} complete={() => {
          expect(helper.byTestId("test-connection-button").matches("[disabled]")).toBe(true); // disabled while connection is in progress

          setTimeout(() => { // make this async so as to allow mithril to update the dom
            m.redraw.sync();
            expect(helper.byTestId("test-connection-button").matches("[disabled]")).toBe(false); // enabled on complete
            expect(helper.byTestId("test-connection-icon")).toHaveClass(styles.testConnectionFailure);
            expect(helper.byTestId("flash-message-alert").querySelector("pre")).toContainText("Error while parsing material URL");
            done();
          }, 10);
        }}/>);

        expect(helper.byTestId('test-connection-button')).toBeVisible();
        helper.clickByTestId("test-connection-button");
        expect(jasmine.Ajax.requests.count()).toEqual(1);
      });
    });
  });

  function payload(material: Material): string {
    return JSON.stringify(material.toApiPayload());
  }
});
