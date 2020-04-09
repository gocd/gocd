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
import {TestHelper} from "views/pages/spec/test_helper";
import {PluggableScmWidget} from "../pluggable_scm_widget";
import {getPluggableScm} from "./test_data";

describe('PluggableScmWidgetSpec', () => {
  const helper    = new TestHelper();
  const editSpy   = jasmine.createSpy("onEdit");
  const cloneSpy  = jasmine.createSpy("onClone");
  const deleteSpy = jasmine.createSpy("onDelete");
  const usagesSpy = jasmine.createSpy("showUsages");
  let scm: Scm;
  let disableActions: boolean;

  beforeEach(() => {
    scm            = Scm.fromJSON(getPluggableScm());
    disableActions = false;
  });
  afterEach((done) => helper.unmount(done));

  function mount() {
    helper.mount(() => <PluggableScmWidget scm={scm} disableActions={disableActions}
                                           onEdit={editSpy} onClone={cloneSpy} onDelete={deleteSpy}
                                           showUsages={usagesSpy}/>);
  }

  it('should render scm details and action buttons', () => {
    mount();

    expect(helper.byTestId('pluggable-scm-panel')).toBeInDOM();

    const headerPanel = helper.byTestId('collapse-header');
    expect(helper.textByTestId('key-value-key-name', headerPanel)).toBe('Name');
    expect(helper.textByTestId('key-value-value-name', headerPanel)).toBe(scm.name());
    expect(helper.textByTestId('key-value-key-plugin-id', headerPanel)).toBe('Plugin Id');
    expect(helper.textByTestId('key-value-value-plugin-id', headerPanel)).toBe(scm.pluginMetadata().id());

    const scmDetails = helper.byTestId('pluggable-scm-details');
    expect(helper.textByTestId('key-value-key-id', scmDetails)).toBe('Id');
    expect(helper.textByTestId('key-value-value-id', scmDetails)).toBe(scm.id());
    expect(helper.textByTestId('key-value-key-name', scmDetails)).toBe('Name');
    expect(helper.textByTestId('key-value-value-name', scmDetails)).toBe(scm.name());
    expect(helper.textByTestId('key-value-key-plugin-id', scmDetails)).toBe('Plugin Id');
    expect(helper.textByTestId('key-value-value-plugin-id', scmDetails)).toBe(scm.pluginMetadata().id());
    expect(helper.textByTestId('key-value-key-url', scmDetails)).toBe('url');
    expect(helper.textByTestId('key-value-value-url', scmDetails)).toBe('https://github.com/sample/example.git');

    expect(helper.byTestId('pluggable-scm-edit')).toBeInDOM();
    expect(helper.byTestId('pluggable-scm-edit')).not.toBeDisabled();
    expect(helper.byTestId('pluggable-scm-clone')).toBeInDOM();
    expect(helper.byTestId('pluggable-scm-clone')).not.toBeDisabled();
    expect(helper.byTestId('pluggable-scm-delete')).toBeInDOM();
    expect(helper.byTestId('pluggable-scm-delete')).not.toBeDisabled();
    expect(helper.byTestId('pluggable-scm-usages')).toBeInDOM();
    expect(helper.byTestId('pluggable-scm-usages')).not.toBeDisabled();
    expect(helper.byTestId('Info Circle-icon')).not.toBeInDOM();
  });

  it('should give a call to the callbacks on relevant button clicks', () => {
    mount();

    helper.clickByTestId('pluggable-scm-edit');
    expect(editSpy).toHaveBeenCalled();

    helper.clickByTestId('pluggable-scm-clone');
    expect(cloneSpy).toHaveBeenCalled();

    helper.clickByTestId('pluggable-scm-delete');
    expect(deleteSpy).toHaveBeenCalled();

    helper.clickByTestId('pluggable-scm-usages');
    expect(usagesSpy).toHaveBeenCalled();
  });

  it('should disable edit and clone button and render a warning button', () => {
    disableActions = true;
    mount();

    ['pluggable-scm-edit', 'pluggable-scm-clone'].forEach((key) => {
      expect(helper.byTestId(key)).toBeDisabled();
      expect(helper.byTestId(key)).toHaveAttr('title', "Plugin 'github.pr' not found!");
    });
    expect(helper.byTestId('pluggable-scm-delete')).not.toBeDisabled();
    expect(helper.byTestId('pluggable-scm-usages')).not.toBeDisabled();
    const warningIcon = helper.byTestId('Info Circle-icon');
    expect(warningIcon).toBeInDOM();
    expect(warningIcon).toHaveAttr('title', "Plugin 'github.pr' was not found!");
  });
});
