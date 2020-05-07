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
import {Scm} from "models/materials/pluggable_scm";
import {PluginInfo, PluginInfos} from "models/shared/plugin_infos_new/plugin_info";
import {FlashMessageModel, MessageType} from "views/components/flash_message";
import {TestHelper} from "views/pages/spec/test_helper";
import {PluggableScmModalBody} from "../pluggable_scm_modal_body";
import {getPluggableScm, getScmPlugin} from "./test_data";

describe('PluggableScmModalBodySpec', () => {
  const helper        = new TestHelper();
  const spy           = jasmine.createSpy("pluginIdProxy");
  const pluginIdProxy = () => {
    spy();
    return "scm-plugin-id";
  };
  let pluginInfos: PluginInfos;
  let scm: Scm;
  let disabled: boolean;
  let disablePluginId: boolean;

  beforeEach(() => {
    scm             = Scm.fromJSON(getPluggableScm());
    pluginInfos     = new PluginInfos(PluginInfo.fromJSON(getScmPlugin()));
    disabled        = false;
    disablePluginId = false;
  });
  afterEach((done) => helper.unmount(done));

  function mount(message?: FlashMessageModel) {
    helper.mount(() => <PluggableScmModalBody scm={scm} pluginInfos={pluginInfos} disablePluginId={disablePluginId}
                                              disableId={disabled} pluginIdProxy={pluginIdProxy} message={message}/>);
  }

  it('should render input fields for name, plugin and auto update', () => {
    mount();

    expect(helper.byTestId('form-field-input-name')).toBeInDOM();
    expect(helper.byTestId('form-field-input-name')).not.toBeDisabled();
    expect(helper.byTestId("form-field-input-name")).toHaveValue(scm.name());

    expect(helper.byTestId('form-field-input-plugin')).toBeInDOM();
    expect(helper.byTestId('form-field-input-plugin')).not.toBeDisabled();
    expect(helper.byTestId("form-field-input-plugin").children.length).toBe(1);
    expect(helper.byTestId("form-field-input-plugin").children[0].textContent).toBe('SCM Plugin');

    expect(helper.byTestId('auto-update-scm')).toBeInDOM();
    expect(helper.byTestId('auto-update-scm')).not.toBeDisabled();
  });

  it('should render the name as readonly', () => {
    disabled = true;
    mount();

    expect(helper.byTestId('form-field-input-name')).toBeDisabled();
  });

  it('should call the spy method on plugin change', () => {
    const json       = getScmPlugin();
    json.id          = 'new-plugin-id';
    json.about!.name = 'new-plugin-name';
    const pluginInfo = PluginInfo.fromJSON(json);

    pluginInfos.push(pluginInfo);
    mount();

    helper.onchange(helper.byTestId("form-field-input-plugin"), "new-plugin-id");

    expect(spy).toHaveBeenCalled();
  });

  it('should render message when given', () => {
    const message = new FlashMessageModel(MessageType.warning, "some message");
    mount(message);

    expect(helper.byTestId('flash-message-warning')).toBeInDOM();
    expect(helper.textByTestId('flash-message-warning')).toBe('some message');
  });

  it('should render plugin as readonly', () => {
    disablePluginId = true;
    mount();

    expect(helper.byTestId('form-field-input-plugin')).toBeInDOM();
    expect(helper.byTestId('form-field-input-plugin')).toBeDisabled();
  });
});
