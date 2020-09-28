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

import {docsUrl} from "gen/gocd_version";
import m from "mithril";
import Stream from "mithril/stream";
import {Scm, Scms} from "models/materials/pluggable_scm";
import {PluginInfo, PluginInfos} from "models/shared/plugin_infos_new/plugin_info";
import {ScrollManager} from "views/components/anchor/anchor";
import {stubAllMethods, TestHelper} from "views/pages/spec/test_helper";
import {PluggableScmsWidget} from "../pluggable_scms_widget";
import {getPluggableScm, getScmPlugin} from "./test_data";

describe('PluggableScmsWidgetSpec', () => {
  const helper = new TestHelper();
  let scms: Scms;
  let pluginInfos: PluginInfos;

  beforeEach(() => {
    scms        = Scms.fromJSON([]);
    pluginInfos = new PluginInfos(PluginInfo.fromJSON(getScmPlugin()));
  });
  afterEach((done) => helper.unmount(done));

  function mount(sm: ScrollManager = stubAllMethods(["shouldScroll", "getTarget", "setTarget", "scrollToEl", "hasTarget"])) {
    const scrollOptions = {
      sm,
      shouldOpenEditView: false
    };
    helper.mount(() => <PluggableScmsWidget scms={Stream(scms)} pluginInfos={Stream(pluginInfos)}
                                            onEdit={jasmine.createSpy("onEdit")}
                                            onClone={jasmine.createSpy("onClone")}
                                            onDelete={jasmine.createSpy("onDelete")}
                                            showUsages={jasmine.createSpy("showUsages")}
                                            onError={jasmine.createSpy("onError")}
                                            scrollOptions={scrollOptions}/>);
  }

  it('should render info div if repos is empty', () => {
    mount();

    expect(helper.byTestId('scms-widget')).not.toBeInDOM();
    const helpInfo = helper.byTestId('pluggable-scm-info');
    expect(helpInfo).toBeInDOM();
    expect(helper.qa('li', helpInfo)[0].textContent).toBe('Click on "Create Pluggable Scm" to add new SCM.');
    expect(helper.qa('li', helpInfo)[1].textContent).toBe('An SCM can be set up and used as a material in the pipelines. You can read more from here.');

    expect(helper.q('a', helpInfo)).toHaveAttr('href', docsUrl("extension_points/scm_extension.html"));
  });

  it('should render scms if present', () => {
    scms.push(Scm.fromJSON(getPluggableScm()));
    mount();

    expect(helper.byTestId('pluggable-scm-info')).not.toBeInDOM();
    expect(helper.byTestId('scms-widget')).toBeInDOM();
  });

  it('should render error info if the element specified in the anchor does not exist', () => {
    let scrollManager: ScrollManager;
    scrollManager = {
      hasTarget:    jasmine.createSpy().and.callFake(() => true),
      getTarget:    jasmine.createSpy().and.callFake(() => "test"),
      shouldScroll: jasmine.createSpy(),
      setTarget:    jasmine.createSpy(),
      scrollToEl:   jasmine.createSpy()
    };
    mount(scrollManager);

    expect(helper.byTestId("anchor-scm-not-present")).toBeInDOM();
    expect(helper.textByTestId("anchor-scm-not-present")).toBe("'test' SCM has not been set up.");
  });
});
