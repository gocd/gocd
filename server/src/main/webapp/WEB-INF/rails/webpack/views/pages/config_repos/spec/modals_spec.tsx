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

import {ApiResult, ObjectWithEtag} from "helpers/api_request_builder";
import {asSelector} from "helpers/css_proxies";
import m from "mithril";
import Stream from "mithril/stream";
import {ConfigRepo} from "models/config_repos/types";
import {GitMaterialAttributes, Material} from "models/materials/types";
import {Rule} from "models/rules/rules";
import {PluginInfos} from "models/shared/plugin_infos_new/plugin_info";
import {ConfigRepoModal} from "views/pages/config_repos/modals";
import {configRepoPluginInfo, createConfigRepoParsedWithError} from "views/pages/config_repos/spec/test_data";
import {TestHelper} from "views/pages/spec/test_helper";
import styles from "../index.scss";

const sel = asSelector(styles);

class TestConfigRepoModal extends ConfigRepoModal {
  readonly repo: ConfigRepo;

  constructor(onSuccessfulSave: (msg: m.Children) => any,
              onError: (msg: m.Children) => any,
              pluginInfos: Stream<PluginInfos>,
              resourceAutocompleteHelper: Map<string, string[]> = new Map(),
              isNew: boolean = true) {
    super(onSuccessfulSave, onError, pluginInfos, resourceAutocompleteHelper);
    this.isNew = isNew;

    const configRepo = new ConfigRepo(undefined,
                                      this.pluginInfos()[0].id,
                                      new Material("git", new GitMaterialAttributes()));
    const parsedRepo = createConfigRepoParsedWithError();
    parsedRepo.rules().push(Stream(new Rule("allow", "refer", "pipeline", "common*")));
    this.repo = this.initProperties(this.isNew ? configRepo : parsedRepo);
  }

  setErrorMessageForTest(errorMsg: string): void {
    this.error = errorMsg;
  }

  simulateValidationFailedResponse(errorBody: any) {
    const resp = ApiResult.error(JSON.stringify(errorBody), "validation failed", 422, new Map()).
      map((x) => null as unknown as ObjectWithEtag<ConfigRepo>);

    this.handleError(resp, (resp as any).errorResponse);
  }

  performSave(): Promise<any> {
    return Promise.resolve();
  }

  title(): string {
    return "Modal Title for Config Repo";
  }

  reinit() {
    this.initProperties(this.repo);
  }

  protected getRepo(): ConfigRepo {
    return this.repo;
  }
}

describe("ConfigRepoModal", () => {
  const helper           = new TestHelper();
  const pluginInfos      = Stream(new PluginInfos(configRepoPluginInfo()));
  const onSuccessfulSave = jasmine.createSpy("onSuccessfulSave");
  const onError          = jasmine.createSpy("onError");

  afterEach((done) => helper.unmount(done));

  it("should render modal title and fields", () => {
    const modal = new TestConfigRepoModal(onSuccessfulSave, onError, pluginInfos);
    helper.mount(() => m(modal));

    expect(modal.title()).toEqual("Modal Title for Config Repo");
    expect(helper.byTestId("form-field-label-plugin-id")).toBeInDOM();
    expect(helper.byTestId("form-field-label-material-type")).toBeInDOM();
    expect(helper.byTestId("form-field-label-config-repository-name")).toBeInDOM();

    expect(helper.byTestId("form-field-input-plugin-id")).toBeInDOM();
    expect(helper.byTestId("form-field-input-plugin-id").firstElementChild!.textContent).toContain("JSON Configuration Plugin");
    expect(helper.byTestId("form-field-input-material-type")).toBeInDOM();
    expect(helper.byTestId("form-field-input-material-type").firstElementChild!.textContent).toContain("Git");
  });

  it("should display error message", () => {
    const modal = new TestConfigRepoModal(onSuccessfulSave, onError, pluginInfos);
    modal.setErrorMessageForTest("some error message");
    helper.mount(() => m(modal));

    expect(helper.byTestId("flash-message-alert")).toBeInDOM();
    expect(helper.textByTestId("flash-message-alert")).toContain("some error message");
  });

  it("renders inline errors from the server", () => {
    const modal = new TestConfigRepoModal(onSuccessfulSave, onError, pluginInfos);
    helper.mount(() => m(modal));

    modal.simulateValidationFailedResponse({
      message: "Dude, I'm dead.",
      data: {
        errors: {
          id: ["I've been through the desert on a repo with no name ~ ~"]
        },
        id: "",
        plugin_id: "json.config.plugin",
        material : {
          type: "git",
          attributes: {
            name: null,
            auto_update: true,
            url: "",
            branch: "master",
            errors: {
              url: ["You fool! How could you forget the URL?"]
            }
          }
        },
        configuration: [ ],
        rules: [ ]
      }
    });

    helper.redraw();

    expect(helper.text(`[data-test-id="form-field-input-config-repository-name"] + span`)).toBe("I've been through the desert on a repo with no name ~ ~.");
    expect(helper.text(`[data-test-id="form-field-input-url"] + span`)).toBe("You fool! How could you forget the URL?.");
  });

  it("renders the material auto update mismatch error", () => {
    const modal = new TestConfigRepoModal(onSuccessfulSave, onError, pluginInfos);
    helper.mount(() => m(modal));

    modal.simulateValidationFailedResponse({
      message: "Dude, I'm dead.",
      data: {
        id: "",
        plugin_id: "json.config.plugin",
        material : {
          type: "git",
          attributes: {
            name: null,
            auto_update: true,
            url: "",
            branch: "master",
            errors: {
              auto_update: ["Now you've done it"]
            }
          }
        },
        configuration: [ ],
        rules: [ ]
      }
    });

    helper.redraw();

    expect(helper.byTestId("flash-message-alert")).toBeInDOM();
    expect(helper.textByTestId("flash-message-alert")).toContain("Now you've done it");
  });

  it("renders configuration property errors", () => {
    const modal = new TestConfigRepoModal(onSuccessfulSave, onError, pluginInfos);

    // cheating; easier than pretending to click and type, must use reinit() after setting
    modal.repo.userProps([
      { key: "userdef.doppelganger", value: "data" },
      { key: "userdef.doppelganger", value: "lore" },
    ]);

    modal.reinit();

    spyOn(modal, "performSave");

    helper.mount(() => m(modal));

    modal.save();

    helper.redraw();

    expect(modal.performSave).not.toHaveBeenCalled();
    expect(helper.byTestId("flash-message-alert")).toBeInDOM();
    expect(helper.textByTestId("flash-message-alert")).toContain("One or more properties is invalid");
    expect(helper.textAll(`${sel.configProperties} input[type="text"] + span`)).toEqual(["Names must be unique.", "Names must be unique."]);
  });

  describe("CreateModal", () => {
    it("should not disable id while creating secret config", () => {
      const modal = new TestConfigRepoModal(onSuccessfulSave, onError, pluginInfos);
      helper.mount(() => m(modal));

      expect(helper.byTestId("form-field-input-config-repository-name")).toBeInDOM();
      expect(helper.byTestId("form-field-input-config-repository-name")).not.toBeDisabled();
    });

    it('should not add any default rule', () => {
      const modal = new TestConfigRepoModal(onSuccessfulSave, onError, pluginInfos);
      helper.mount(() => m(modal));

      expect(helper.byTestId('rules-widget')).toBeInDOM();
      expect(helper.byTestId('rules-table')).not.toBeInDOM();
      expect(helper.byTestId('add-rule-button')).toBeInDOM();
    });
  });

  describe("EditModal", () => {
    it("should disable id while editing secret config", () => {
      const modal = new TestConfigRepoModal(onSuccessfulSave, onError, pluginInfos, new Map(), false);
      helper.mount(() => m(modal));

      expect(helper.byTestId("form-field-input-config-repository-name")).toBeDisabled();
    });

    it('should render any rules configures', () => {
      const modal = new TestConfigRepoModal(onSuccessfulSave, onError, pluginInfos, new Map(), false);
      helper.mount(() => m(modal));

      expect(helper.byTestId('rules-table')).toBeInDOM();
      expect(helper.qa('tr', helper.byTestId('rules-table')).length).toBe(2);
    });
  });

});
