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

import {ApiResult} from "helpers/api_request_builder";
import m from "mithril";
import Stream from "mithril/stream";
import {ValidatableMixin} from "models/mixins/new_validatable_mixin";
import {PluginInfo, PluginInfos} from "models/shared/plugin_infos_new/plugin_info";
import {MessageType} from "views/components/flash_message";
import {EntityModalWithCheckConnection} from "views/components/modal/entity_modal";
import {TestHelper} from "views/pages/spec/test_helper";

describe("Entity Modal", () => {
  describe("Entity Modal With Check Connection", () => {
    const helper = new TestHelper();

    it("should contain verify connection button", () => {
      const verifyConnection = jasmine.createSpy().and.returnValue(Promise.resolve({}));
      const modal            = new DummyModalWithVerifyConnection(verifyConnection);
      const buttons          = modal.buttons();

      helper.mount(() => buttons);

      expect(helper.qa("button")).toHaveLength(2);
      expect(helper.byTestId("button-check-connection")).toBeInDOM();
      expect(helper.byTestId("button-save")).toBeInDOM();

      helper.unmount();
    });

    it("should show message on successful verify connection", (done) => {
      let result = ApiResult.success(JSON.stringify({message: "Connection OK"}), 200, new Map());
      result     = result.map((body) => JSON.parse(body));

      const verifyConnection = jasmine.createSpy().and.returnValue(Promise.resolve(result));
      const modal            = new DummyModalWithVerifyConnection(verifyConnection);

      expect(modal.getFlashMessage().hasMessage()).toBeFalse();

      modal.performCheckConnection().then(() => {
        expect(modal.getFlashMessage().hasMessage()).toBeTrue();
        expect(modal.getFlashMessage().message).toEqual("Connection OK");
        expect(modal.getFlashMessage().type).toEqual(MessageType.success);
        done();
      });
    });

    it("should show message on failure verify connection", (done) => {
      let result = ApiResult.error(JSON.stringify({message: "Verify Connection Failed"}), "failed", 400, new Map());
      result     = result.map((body) => JSON.parse(body));

      const verifyConnection = jasmine.createSpy().and.returnValue(Promise.resolve(result));
      const modal            = new DummyModalWithVerifyConnection(verifyConnection);

      expect(modal.getFlashMessage().hasMessage()).toBeFalse();

      modal.performCheckConnection().then(() => {
        expect(modal.getFlashMessage().hasMessage()).toBeTrue();
        expect(modal.getFlashMessage().message).toEqual("Verify Connection Failed");
        expect(modal.getFlashMessage().type).toEqual(MessageType.alert);
        done();
      });
    });

    class DummyModel extends ValidatableMixin {
    }

    class DummyModalWithVerifyConnection extends EntityModalWithCheckConnection<DummyModel> {
      private verifyConnectionPromise: () => Promise<any>;

      constructor(verifyConnectionPromise: () => Promise<any>) {
        super(new DummyModel(), new PluginInfos(), jasmine.createSpy());
        this.verifyConnectionPromise = verifyConnectionPromise;
      }

      title(): string {
        return "Dummy Test Modal";
      }

      protected verifyConnectionOperationPromise(): Promise<any> {
        return this.verifyConnectionPromise();
      }

      protected modalBody(): m.Children {
        return <div>This is modal body</div>;
      }

      protected onPluginChange(entity: Stream<DummyModel>, pluginInfo: PluginInfo): void {
        //do nothing
      }

      protected operationPromise(): Promise<any> {
        return Promise.resolve();
      }

      protected parseJsonToEntity(json: object): DummyModel {
        return new DummyModel();
      }

      protected performFetch(entity: DummyModel): Promise<any> {
        return Promise.resolve();
      }

      protected successMessage(): m.Children {
        return undefined;
      }
    }
  });
});
