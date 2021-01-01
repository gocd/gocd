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

import {SparkRoutes} from "helpers/spark_routes";
import m from "mithril";
import {GitMaterialAttributes, Material} from "models/materials/types";
import {TestHelper} from "views/pages/spec/test_helper";
import {MaterialCheck} from "../material_check";
import styles from "../material_check.scss";

describe("ConfigRepos: MaterialCheck", () => {
  const helper = new TestHelper();
  const PLUGIN_ID = "yaml.config.plugin";
  const CONFIG_FILES_URL = SparkRoutes.pacListConfigFiles(PLUGIN_ID);
  const invalidMaterial = new Material("git", new GitMaterialAttributes());
  const validMaterial = new Material("git", new GitMaterialAttributes("SomeRepo", false, "https://github.com/gocd/gocd", "master"));

  afterEach(helper.unmount.bind(helper));

  it("Renders available config files when successful connection", (done) => {
    jasmine.Ajax.withMock(() => {
      const response = {
        plugins: [ {
          plugin_id: PLUGIN_ID,
          files: [ "thepipes.gocd.yaml" ],
          errors: ""
        }]
      };

      jasmine.Ajax.stubRequest(CONFIG_FILES_URL, payload(validMaterial), "POST")
        .andReturn({
          responseText: JSON.stringify(response),
          status: 200,
          responseHeaders: {
            "Content-Type": "application/vnd.go.cd.v1+json"
          }
        });

      helper.mount(() => <MaterialCheck material={validMaterial} pluginId={PLUGIN_ID} complete={() => {
        expect(helper.byTestId("material-check-button").matches("[disabled]")).toBe(true); // disabled while connection is in progress

        setTimeout(() => { // make this async so as to allow mithril to update the dom
          m.redraw.sync();
          expect(helper.byTestId("material-check-button").matches("[disabled]")).toBe(false); // enabled on complete
          expect(helper.byTestId("material-check-icon")).toHaveClass(styles.materialCheckSuccess);
          expect(helper.allByTestId("material-check-plugin-file").length).toBe(1);
          done();
        }, 0);
      }}/>);

      expect(helper.byTestId('material-check-button')).toBeVisible();
      helper.clickByTestId("material-check-button");
      expect(jasmine.Ajax.requests.count()).toEqual(1);
    });
  });

  it("Renders errors plugins might have returned when fetching config files", (done) => {
    jasmine.Ajax.withMock(() => {
      const response = {
        plugins: [ {
          plugin_id: PLUGIN_ID,
          files: [],
          errors: "wubba lubba dub dub!!!"
        } ]
      };

      jasmine.Ajax.stubRequest(CONFIG_FILES_URL, payload(validMaterial), "POST")
        .andReturn({
          responseText: JSON.stringify(response),
          status: 200,
          responseHeaders: {
            "Content-Type": "application/vnd.go.cd.v1+json"
          }
        });

      helper.mount(() => <MaterialCheck material={validMaterial} pluginId={PLUGIN_ID} complete={() => {
        expect(helper.byTestId("material-check-button").matches("[disabled]")).toBe(true); // disabled while connection is in progress

        setTimeout(() => { // make this async so as to allow mithril to update the dom
          m.redraw.sync();
          expect(helper.byTestId("material-check-button").matches("[disabled]")).toBe(false); // enabled on complete
          expect(helper.byTestId("material-check-icon")).toHaveClass(styles.materialCheckSuccess);
          expect(helper.allByTestId("material-check-plugin-file").length).toBe(0);
          expect(helper.byTestId("flash-message-alert")).toContainText("wubba lubba dub dub!!!");
          done();
        }, 0);
      }}/>);

      expect(helper.byTestId('material-check-button')).toBeVisible();
      helper.clickByTestId("material-check-button");
      expect(jasmine.Ajax.requests.count()).toEqual(1);
    });
  });

  it("Renders message when no config files found", (done) => {
    jasmine.Ajax.withMock(() => {
      const response = {
        plugins: [ {
          plugin_id: PLUGIN_ID,
          files: [],
          errors: ""
        } ]
      };

      jasmine.Ajax.stubRequest(CONFIG_FILES_URL, payload(validMaterial), "POST")
        .andReturn({
          responseText: JSON.stringify(response),
          status: 200,
          responseHeaders: {
            "Content-Type": "application/vnd.go.cd.v1+json"
          }
        });

      helper.mount(() => <MaterialCheck material={validMaterial} pluginId={PLUGIN_ID} complete={() => {
        expect(helper.byTestId("material-check-button").matches("[disabled]")).toBe(true); // disabled while connection is in progress

        setTimeout(() => { // make this async so as to allow mithril to update the dom
          m.redraw.sync();
          expect(helper.byTestId("material-check-button").matches("[disabled]")).toBe(false); // enabled on complete
          expect(helper.byTestId("material-check-icon")).toHaveClass(styles.materialCheckSuccess);
          expect(helper.byTestId("flash-message-alert")).toContainText("No config files found");
          done();
        }, 0);
      }}/>);

      expect(helper.byTestId('material-check-button')).toBeVisible();
      helper.clickByTestId("material-check-button");
      expect(jasmine.Ajax.requests.count()).toEqual(1);
    });
  });

  it("Renders error message when listing config files is not successful", (done) => {
    jasmine.Ajax.withMock(() => {
      const response = {message: "eek barba durkle, there was an error"};
      jasmine.Ajax.stubRequest(CONFIG_FILES_URL, payload(invalidMaterial), "POST")
        .andReturn({
          responseText: JSON.stringify(response),
          status: 500,
          responseHeaders: {
            "Content-Type": "application/vnd.go.cd.v1+json"
          }
        });

      helper.mount(() => <MaterialCheck material={invalidMaterial} pluginId={PLUGIN_ID} complete={() => {
        expect(helper.byTestId("material-check-button").matches("[disabled]")).toBe(true); // disabled while connection is in progress

        setTimeout(() => { // make this async so as to allow mithril to update the dom
          m.redraw.sync();
          expect(helper.byTestId("material-check-button").matches("[disabled]")).toBe(false); // enabled on complete
          expect(helper.byTestId("material-check-icon")).toHaveClass(styles.materialCheckFailure);
          expect(helper.byTestId("flash-message-alert")).toContainText("eek barba durkle, there was an error");
          done();
        }, 0);
      }}/>);

      expect(helper.byTestId('material-check-button')).toBeVisible();
      helper.clickByTestId("material-check-button");
      expect(jasmine.Ajax.requests.count()).toEqual(1);
    });
  });

  function payload(material: Material): string {
    return JSON.stringify(material.toApiPayload());
  }
});
