/*
 * Copyright 2017 ThoughtWorks, Inc.
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

let m             = require('mithril');
let f             = require('helpers/form_helper');
let AngularPlugin = require('views/shared/angular_plugin');

const Stream      = require('mithril/stream');
const NewModal    = require('views/shared/new_modal');
const SCMs        = require('models/pipeline_configs/scms');
const PluginInfos = require('models/pipeline_configs/plugin_infos');


let ScmConfigModalBody = {
  view (vnode) {
    if (!vnode.attrs.pluginInfo()) {
      return (
        <div class="modal-spinner"/>
      );
    }

    let errorMessageElem;

    if (vnode.attrs.errorMessage()) {
      errorMessageElem = (
        <f.row collapse>
          <f.column size="12">
            <f.alert>{vnode.attrs.errorMessage()}</f.alert>
          </f.column>
        </f.row>
      );
    }
    return (
      <div>
        {errorMessageElem}
        <f.row collapse>
          <f.inputWithLabel attrName='name'
                            class='align'
                            size={12}
                            largeSize={12}
                            validate={true}
                            isRequired={true}
                            model={vnode.attrs.scm()}/>
        </f.row>
        <f.row collapse>
          <f.checkBox model={vnode.attrs.scm()}
                      attrName='autoUpdate'
                      class='align'
                      size={6}
                      end={true}/>
        </f.row>
        <f.row collapse>
          <AngularPlugin pluginInfo={vnode.attrs.pluginInfo}
                         configuration={vnode.attrs.scm().configuration}
                         key={vnode.attrs.pluginInfo() ? vnode.attrs.pluginInfo().id() : 'no-plugin'}/>
        </f.row>
      </div>
    );
  }
};

const CreateNewScmMaterialButtonWidget = {
  oninit (vnode) {
    this.showModal = function () {
      const scm = Stream(new SCMs.SCM({
        'plugin_metadata': {
          id:      vnode.attrs.material.pluginInfo().id(),
          version: vnode.attrs.material.pluginInfo().version()
        }
      }));

      const pluginInfo   = Stream();
      const errorMessage = Stream();

      const modal = new NewModal({
        title:   'Create a new plugin SCM material',
        body:    () => (<ScmConfigModalBody scm={scm} pluginInfo={pluginInfo} errorMessage={errorMessage}/>),
        buttons: () => [
          {
            text: 'Save',
            onclick () {
              const callback = function (savedScm) {
                vnode.attrs.material.scm(savedScm);
                modal.destroy();
                SCMs.init();
              };

              const errback = function (json) {
                errorMessage(json.message);
                if (json.data) {
                  scm(new SCMs.SCM(json.data));
                }
              };
              scm().create().then(callback, errback).always(m.redraw);
            }
          }]
      });

      modal.render();
      m.redraw();

      PluginInfos.PluginInfo.get(vnode.attrs.material.pluginInfo().id()).then(pluginInfo).then(m.redraw);
    };
  },

  view (vnode) {
    return (
      <f.row class='scm-selector' collapse>
        <f.column class='no-scm' size={4} largeSize={6} end>
          <f.row collapse>
            <label>
              There are no existing SCM materials for <strong>{vnode.attrs.material.pluginInfo().displayName()}</strong>
            </label>
          </f.row>
          <f.row collapse>
            <f.button class="add-button" onclick={vnode.state.showModal.bind(vnode.state)}>
              Create a new
              {' '}
              <strong>{vnode.attrs.material.pluginInfo().displayName()}</strong>
              {' '}
              material
            </f.button>
          </f.row>
        </f.column>
      </f.row>
    );
  }
};

module.exports = CreateNewScmMaterialButtonWidget;
