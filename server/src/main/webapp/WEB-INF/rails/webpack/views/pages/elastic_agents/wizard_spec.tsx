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
import m from "mithril";
import Stream from "mithril/stream";
import {
  ClusterProfile,
  ClusterProfileJSON,
  ClusterProfiles,
  ElasticAgentProfile,
  ElasticProfileJSON
} from "models/elastic_profiles/types";
import {Configuration, Configurations} from "models/shared/configuration";
import {PlainTextValue} from "models/shared/config_value";
import {PluginInfo, PluginInfos} from "models/shared/plugin_infos_new/plugin_info";
import {
  pluginInfoWithElasticAgentExtensionV4,
  pluginInfoWithElasticAgentExtensionV5
} from "models/shared/plugin_infos_new/spec/test_data";
import {Wizard} from "views/components/wizard";
import {ElasticAgentsPage} from "views/pages/elastic_agents";
import styles from "views/pages/elastic_agents/index.scss";
import {
  openWizardForAdd,
  openWizardForAddElasticProfile,
  openWizardForEditClusterProfile,
  openWizardForEditElasticProfile
} from "views/pages/elastic_agents/wizard";
import {PageState} from "views/pages/page";
import {TestHelper} from "views/pages/spec/test_helper";

describe("ElasticAgentWizard", () => {
  let wizard: Wizard;
  let pluginInfos: Stream<PluginInfos>;
  let clusterProfile: Stream<ClusterProfile>;
  let elasticProfile: Stream<ElasticAgentProfile>;
  let onSuccessfulSave: any;
  let onError: any;
  const helper: TestHelper = new TestHelper();

  beforeEach(() => {
    jasmine.Ajax.install();
    pluginInfos      = Stream(new PluginInfos(PluginInfo.fromJSON(pluginInfoWithElasticAgentExtensionV5)));
    clusterProfile   = Stream(new ClusterProfile(undefined,
                                                 undefined,
                                                 undefined,
                                                 new Configurations([])));
    elasticProfile   = Stream(new ElasticAgentProfile(undefined,
                                                      undefined,
                                                      undefined,
                                                      undefined,
                                                      new Configurations([])));
    onSuccessfulSave = jasmine.createSpy("onSuccessfulSave");
    onError          = jasmine.createSpy("onError");
    helper.mountPage(() => new StubbedPage());
  });

  afterEach(() => {
    wizard.close();
    m.redraw.sync();
    helper.unmount();
    jasmine.Ajax.uninstall();
  });

  describe("Add", () => {
    it("should display cluster profile properties form", () => {
      wizard = openWizardForAdd(pluginInfos, clusterProfile, elasticProfile, onSuccessfulSave, onError);
      m.redraw.sync();
      expect(wizard).toContainElementWithDataTestId("form-field-input-cluster-profile-name");
      expect(wizard).toContainElementWithDataTestId("form-field-input-plugin-id");
      expect(wizard).toContainElementWithDataTestId("properties-form");
      expect(wizard).toContainInBody("elastic agent plugin settings view");
    });

    it("should display elastic profile properties form", () => {
      wizard = openWizardForAdd(pluginInfos, clusterProfile, elasticProfile, onSuccessfulSave, onError);
      m.redraw.sync();
      wizard.next();
      m.redraw.sync();
      expect(wizard).toContainElementWithDataTestId("form-field-input-elastic-profile-name");
      expect(wizard).toContainElementWithDataTestId("properties-form");
      expect(wizard).toContainInBody("some view for plugin");
    });

    it("should save cluster profile and exit", (done) => {
      const configurations       = new Configurations([new Configuration("GO_SERVER_URL", new PlainTextValue(""))]);
      const clusterProfileObj    = new ClusterProfile("cluster-profile-id", "ecs-elastic-agent", true, configurations);
      const promiseForCreateCall = successResponseForClusterProfile().catch(done.fail);
      clusterProfileObj.create   = jasmine.createSpy("create call").and.returnValue(promiseForCreateCall);
      clusterProfile             = Stream(clusterProfileObj);

      wizard = openWizardForAdd(pluginInfos, clusterProfile, elasticProfile, onSuccessfulSave, onError);
      spyOn(wizard, "close").and.callThrough();
      m.redraw.sync();
      helper.clickByTestId("save-cluster-profile", document.getElementsByClassName("component-modal-container")[0]);
      promiseForCreateCall.finally(() => {
        expect(clusterProfileObj.create).toHaveBeenCalled();
        expect(wizard.close).toHaveBeenCalled();
        done();
      });
    });

    it("should display validation error message on save of cluster profile", (done) => {
      const configurations       = new Configurations([new Configuration("GO_SERVER_URL", new PlainTextValue(""))]);
      const clusterProfileObj    = new ClusterProfile("cluster-profile-id",
                                                      "cd.go.contrib.elastic-agent.docker",
                                                      true,
                                                      configurations);
      const promiseForCreateCall = validationErrorResponseForClusterProfile().catch(done.fail);
      clusterProfileObj.create   = jasmine.createSpy("create call").and.returnValue(promiseForCreateCall);
      clusterProfile             = Stream(clusterProfileObj);

      wizard = openWizardForAdd(pluginInfos, clusterProfile, elasticProfile, onSuccessfulSave, onError);
      spyOn(wizard, "close").and.callThrough();
      m.redraw.sync();
      helper.clickByTestId("save-cluster-profile", modalContext());
      promiseForCreateCall.finally(() => {
        m.redraw.sync();
        expect(clusterProfileObj.create).toHaveBeenCalled();
        expect(wizard.close).not.toHaveBeenCalled();
        expect(helper.text(`.${styles.footerError}`, modalContext()))
          .toBe("Please fix the validation errors above before proceeding.");
        wizard.close();
        done();
      });
    });

    it("should save Elastic Profile and Finish", (done) => {
      const promiseForCreateCall = successResponseForElasticProfile().catch(done.fail);
      elasticProfile().create    = jasmine.createSpy("create call").and.returnValue(promiseForCreateCall);
      wizard                     = openWizardForAdd(pluginInfos,
                                                    clusterProfile,
                                                    elasticProfile,
                                                    onSuccessfulSave,
                                                    onError);
      clusterProfile().id("user-entered-id");
      wizard.next();
      spyOn(wizard, "close").and.callThrough();
      m.redraw.sync();
      helper.clickByTestId("finish", modalContext());
      promiseForCreateCall.finally(() => {
        expect(elasticProfile().create).toHaveBeenCalled();
        expect(wizard.close).toHaveBeenCalled();
        expect(elasticProfile().clusterProfileId()).toBe("user-entered-id");
        expect(onSuccessfulSave)
          .toHaveBeenCalledWith(<span>The Cluster Profile <em>{elasticProfile().clusterProfileId()}</em> and Elastic Agent Profile <em>{elasticProfile()
            .id()}</em> were created successfully!</span>);
        done();
      });
    });

    it("should display validation error message on save of elastic profile", (done) => {
      const promiseForCreateCall = validationErrorResponseForElasticProfile().catch(done.fail);
      const elasticProfileObj    = elasticProfile();
      elasticProfileObj.create   = jasmine.createSpy("create call").and.returnValue(promiseForCreateCall);
      wizard                     = openWizardForAdd(pluginInfos,
                                                    clusterProfile,
                                                    Stream(elasticProfileObj),
                                                    onSuccessfulSave,
                                                    onError);
      wizard.next();
      spyOn(wizard, "close").and.callThrough();
      m.redraw.sync();
      helper.clickByTestId("finish", document.getElementsByClassName("component-modal-container")[0]);
      promiseForCreateCall.finally(() => {
        m.redraw.sync();
        expect(elasticProfileObj.create).toHaveBeenCalled();
        expect(wizard.close).not.toHaveBeenCalled();
        done();
      });
    });

    it("should go back and make changes to cluster profile", (done) => {
      const configurations       = new Configurations([new Configuration("GO_SERVER_URL", new PlainTextValue(""))]);
      const clusterProfileObj    = new ClusterProfile("cluster-profile-id", "ecs-elastic-agent", true, configurations);
      const promiseForCreateCall = successResponseForClusterProfile().catch(done.fail);
      clusterProfileObj.create   = jasmine.createSpy().and.returnValue(promiseForCreateCall);
      clusterProfile             = Stream(clusterProfileObj);

      wizard = openWizardForAdd(pluginInfos, clusterProfile, elasticProfile, onSuccessfulSave, onError);
      expect(helper.byTestId("form-field-input-cluster-profile-name", modalContext())).not.toHaveAttr("readonly");
      m.redraw.sync();

      //Save + Next
      helper.clickByTestId("next", modalContext());
      promiseForCreateCall.finally(() => {
        m.redraw.sync();
        //Go back
        helper.clickByTestId("previous", modalContext());
        m.redraw.sync();

        expect(helper.byTestId("form-field-input-cluster-profile-name", modalContext())).toHaveAttr("readonly");

        done();
      });
    });

    it("should not allow v4 and older plugins to create cluster profile", () => {
      pluginInfos = Stream(new PluginInfos(PluginInfo.fromJSON(pluginInfoWithElasticAgentExtensionV4)));
      wizard      = openWizardForAdd(pluginInfos, clusterProfile, elasticProfile, onSuccessfulSave, onError);
      m.redraw.sync();
      m.redraw.sync(); //second redraw needed for save buttons to be disabled, since the computation happens in a lifecycle method
      expect(wizard).toContainElementWithDataTestId("form-field-input-cluster-profile-name");
      expect(wizard).toContainElementWithDataTestId("form-field-input-plugin-id");
      expect(wizard).not.toContainInBody("elastic agent plugin settings view");
      expect(helper.byTestId("plugin-not-supported", modalContext()))
        .toContainText(`Can not define Cluster profiles for '${pluginInfos()[0].about.name}' plugin as it does not support cluster profiles.`);
      expect(helper.byTestId("save-cluster-profile", modalContext())).toBeDisabled();
      expect(helper.byTestId("next", modalContext())).toBeDisabled();
      expect(helper.byTestId("cancel", modalContext())).not.toBeDisabled();
    });
  });

  describe("Add Elastic Profile to existing Cluster", () => {
    it("should add Elastic Profile to existing Cluster", (done) => {
      const promiseForCreateCall = successResponseForElasticProfile().catch(done.fail);
      elasticProfile().create    = jasmine.createSpy("create call").and.returnValue(promiseForCreateCall);
      clusterProfile             = Stream(new ClusterProfile("cluster-profile-id",
                                                             "plugin-id",
                                                             true,
                                                             new Configurations([])));
      wizard                     = openWizardForAddElasticProfile(pluginInfos,
                                                                  clusterProfile,
                                                                  elasticProfile,
                                                                  onSuccessfulSave,
                                                                  onError);
      wizard.next();
      spyOn(wizard, "close").and.callThrough();
      m.redraw.sync();
      helper.clickByTestId("finish", modalContext());
      promiseForCreateCall.finally(() => {
        expect(elasticProfile().create).toHaveBeenCalled();
        expect(wizard.close).toHaveBeenCalled();
        expect(onSuccessfulSave)
          .toHaveBeenCalledWith(<span>The Elastic Agent Profile <em>{elasticProfile().id()}</em> was created successfully!</span>);
        done();
      });
    });
  });

  describe("Edit Elastic Profile", () => {
    it("should render footer buttons for Elastic Profile", () => {
      wizard = openWizardForEditElasticProfile(pluginInfos, clusterProfile, elasticProfile, onSuccessfulSave, onError);
      wizard.next();
      m.redraw.sync();
      expect(helper.byTestId("finish", modalContext())).toBeInDOM();
      expect(helper.byTestId("cancel", modalContext())).toBeInDOM();
      expect(helper.byTestId("previous", modalContext())).toBeInDOM();
      expect(helper.textByTestId("previous", modalContext())).toBe("Show Cluster Profile");
      expect(helper.textByTestId("finish", modalContext())).toBe("Save");
    });

    it("should render footer buttons for Cluster Profile", () => {
      wizard = openWizardForEditElasticProfile(pluginInfos, clusterProfile, elasticProfile, onSuccessfulSave, onError);
      m.redraw.sync();
      expect(helper.byTestId("next", modalContext())).toBeInDOM();
      expect(helper.byTestId("cancel", modalContext())).toBeInDOM();
      expect(helper.byTestId("save-cluster-profile", modalContext())).not.toBeInDOM();
      expect(helper.textByTestId("next", modalContext())).toBe("Show Elastic Profile");
    });

    it("should not allow changes to Cluster Profile", () => {
      wizard = openWizardForEditElasticProfile(pluginInfos, clusterProfile, elasticProfile, onSuccessfulSave, onError);
      m.redraw.sync();
      expect(helper.byTestId("form-field-input-cluster-profile-name", modalContext())).toHaveAttr("readonly");
      expect(helper.byTestId("form-field-input-plugin-id", modalContext())).toHaveAttr("readonly");
    });

    it("should load data and render form", (done) => {
      clusterProfile = Stream(new ClusterProfile("cluster-profile-id",
                                                 "plugin-id",
                                                 true,
                                                 new Configurations([])));
      elasticProfile = Stream(new ElasticAgentProfile("elastic-profile-id",
                                                      "plugin-id",
                                                      "cluster-profile-id",
                                                      true,
                                                      new Configurations([])));

      const promiseForGetCall = successResponseForElasticProfile().catch(done.fail);
      elasticProfile().get    = jasmine.createSpy("get elastic profile").and.returnValue(promiseForGetCall);
      wizard                  = openWizardForEditElasticProfile(pluginInfos,
                                                                clusterProfile,
                                                                elasticProfile,
                                                                onSuccessfulSave,
                                                                onError);
      wizard.next();
      m.redraw.sync();

      expect(helper.byTestId("spinner", modalContext())).toBeInDOM();
      promiseForGetCall.finally(() => {
        m.redraw.sync();
        expect(helper.byTestId("form-field-input-elastic-profile-name", modalContext())).toBeDisabled();
        done();
      });
    });
  });

  describe("Edit Cluster Profile", () => {
    it("should render footer buttons", () => {
      wizard = openWizardForEditClusterProfile(pluginInfos, clusterProfile, elasticProfile, onSuccessfulSave, onError);
      m.redraw.sync();
      expect(helper.byTestId("next", modalContext())).not.toBeInDOM();
      expect(helper.byTestId("cancel", modalContext())).toBeInDOM();
      expect(helper.byTestId("save-cluster-profile", modalContext())).toHaveText("Save Cluster Profile");
    });

    it("should load data and render form", (done) => {
      clusterProfile = Stream(new ClusterProfile("cluster-profile-id",
                                                 "plugin-id",
                                                 true,
                                                 new Configurations([])));
      elasticProfile = Stream(new ElasticAgentProfile("elastic-profile-id",
                                                      "plugin-id",
                                                      "cluster-profile-id",
                                                      true,
                                                      new Configurations([])));

      const promiseForGetCall = successResponseForClusterProfile().catch(done.fail);
      clusterProfile().get    = jasmine.createSpy("get elastic profile").and.returnValue(promiseForGetCall);
      wizard                  = openWizardForEditClusterProfile(pluginInfos,
                                                                clusterProfile,
                                                                elasticProfile,
                                                                onSuccessfulSave,
                                                                onError);
      m.redraw.sync();

      expect(helper.byTestId("spinner", modalContext())).toBeInDOM();
      promiseForGetCall.finally(() => {
        m.redraw.sync();
        expect(helper.byTestId("form-field-input-cluster-profile-name", modalContext())).toBeDisabled();
        expect(helper.byTestId("form-field-input-plugin-id", modalContext())).toBeDisabled();
        done();
      });
    });
  });

  it("should not allow v4 and older plugins to edit cluster profile", (done) => {
    pluginInfos             = Stream(new PluginInfos(PluginInfo.fromJSON(pluginInfoWithElasticAgentExtensionV4)));
    clusterProfile          = Stream(new ClusterProfile("cluster-profile-id",
                                                        "plugin-id",
                                                        true,
                                                        new Configurations([])));
    elasticProfile          = Stream(new ElasticAgentProfile("elastic-profile-id",
                                                             "plugin-id",
                                                             "cluster-profile-id",
                                                             true,
                                                             new Configurations([])));
    const promiseForGetCall = successResponseForClusterProfile().catch(done.fail);
    clusterProfile().get    = jasmine.createSpy("get elastic profile").and.returnValue(promiseForGetCall);
    wizard                  = openWizardForEditClusterProfile(pluginInfos,
                                                              clusterProfile,
                                                              elasticProfile,
                                                              onSuccessfulSave,
                                                              onError);
    m.redraw.sync();
    promiseForGetCall.finally(() => {
      m.redraw.sync();
      m.redraw.sync(); //second redraw needed for save buttons to be disabled, since the computation happens in a lifecycle method
      expect(wizard).toContainElementWithDataTestId("form-field-input-cluster-profile-name");
      expect(wizard).toContainElementWithDataTestId("form-field-input-plugin-id");
      expect(wizard).not.toContainInBody("elastic agent plugin settings view");
      expect(helper.byTestId("plugin-not-supported", modalContext()))
        .toContainText(`Can not define Cluster profiles for '${pluginInfos()[0].about.name}' plugin as it does not support cluster profiles.`);
      expect(helper.byTestId("save-cluster-profile", modalContext())).toBeDisabled();
      expect(helper.byTestId("cancel", modalContext())).not.toBeDisabled();
      done();
    });
  });

  function successResponseForClusterProfile(): Promise<ApiResult<ObjectWithEtag<ClusterProfile>>> {
    return new Promise<ApiResult<ObjectWithEtag<ClusterProfile>>>((resolve) => {
      const objectWithEtag = {
        object: clusterProfile(),
        etag: "some-etag"
      } as ObjectWithEtag<ClusterProfile>;
      //we don't care about the response body, just map it to the right type of object
      const apiResult      = ApiResult.success("{}", 200, new Map()).map(() => objectWithEtag);
      resolve(apiResult);
    });
  }

  function successResponseForElasticProfile(): Promise<ApiResult<ObjectWithEtag<ElasticAgentProfile>>> {
    return new Promise<ApiResult<ObjectWithEtag<ElasticAgentProfile>>>((resolve) => {
      const objectWithEtag = {
        object: elasticProfile(),
        etag: "some-etag"
      } as ObjectWithEtag<ElasticAgentProfile>;
      //we don't care about the response body, just map it to the right type of object
      const apiResult      = ApiResult.success("{}", 200, new Map()).map(() => objectWithEtag);
      resolve(apiResult);
    });
  }

  function validationErrorResponseForClusterProfile(): Promise<ApiResult<ObjectWithEtag<ClusterProfile>>> {
    return new Promise<ApiResult<ObjectWithEtag<ClusterProfile>>>((resolve) => {
      const responseBody = {
        message: "Validations failed for clusterProfile 'test'. Error(s): [Validation failed.]. Please correct and resubmit.",
        data: {
          id: "test-cluster-profile",
          plugin_id: "cd.go.contrib.elastic-agent.docker",
          properties: [{
            key: "go_server_url",
            value: "http://localhost",
            errors: {
              go_server_url: ["Go Server URL must be a valid HTTPs URL (https://example.com:8154/go)"]
            }
          }],
          errors: {}
        }
      };
      const apiResult    = ApiResult.error(JSON.stringify(responseBody),
                                           "Validations failed for clusterProfile 'test'. Error(s): [Validation failed.]. Please correct and resubmit.",
                                           422,
                                           new Map()).map((body) => {
        const profileJSON = JSON.parse(body) as ClusterProfileJSON;
        return {
          object: ClusterProfile.fromJSON(profileJSON),
          etag: "some-etag"
        } as ObjectWithEtag<ClusterProfile>;
      });
      resolve(apiResult);
    });
  }

  function validationErrorResponseForElasticProfile(): Promise<ApiResult<ObjectWithEtag<ElasticAgentProfile>>> {
    return new Promise<ApiResult<ObjectWithEtag<ElasticAgentProfile>>>((resolve) => {
      const responseBody = {
        message: "Validations failed for agentProfile 'test'. Error(s): [Image must not be blank.]. Please correct and resubmit.",
        data: {
          id: "test",
          cluster_profile_id: "test-cluster",
          properties: [{
            key: "Image",
            errors: {
              Image: ["Image must not be blank."]
            }
          }],
          errors: {}
        }
      };
      const apiResult    = ApiResult.error(JSON.stringify(responseBody),
                                           "Validations failed for agentProfile 'test'. Error(s): [Image must not be blank.]. Please correct and resubmit.",
                                           422,
                                           new Map()).map((body) => {
        const profileJSON = JSON.parse(body) as ElasticProfileJSON;
        return {
          object: ElasticAgentProfile.fromJSON(profileJSON),
          etag: "some-etag"
        } as ObjectWithEtag<ElasticAgentProfile>;
      });
      resolve(apiResult);
    });
  }

  function modalContext() {
    return document.getElementsByClassName("component-modal-container")[0];
  }

  class StubbedPage extends ElasticAgentsPage {
    fetchData(vnode: m.Vnode<any, any>): Promise<any> {
      this.pageState              = PageState.OK;
      vnode.state.pluginInfos     = pluginInfos;
      vnode.state.clusterProfiles = Stream(new ClusterProfiles([]));
      return Promise.resolve();
    }
  }
});
