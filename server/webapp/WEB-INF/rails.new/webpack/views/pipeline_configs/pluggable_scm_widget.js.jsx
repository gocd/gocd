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

let m                                = require('mithril');
let f                                = require('helpers/form_helper');
let CreateNewScmMaterialButtonWidget = require('views/pipeline_configs/create_new_scm_material_button_widget');
let SCMConfigEditWidget              = require('views/pipeline_configs/scm_config_edit_widget');
let MaterialFilterWidget             = require('views/pipeline_configs/material_filter_widget');

const Stream      = require('mithril/stream');
const _           = require('lodash');
const tt          = require('helpers/pipeline_configs/tooltips');
const Modal       = require('views/shared/modal');
const PluginInfos = require('models/pipeline_configs/plugin_infos');

let EditPluggableSCMWidget = {
  oninit (vnode) {
    const args = vnode.attrs;

    this.material           = args.material;
    const saveButtonClasses = Stream(['save-pipeline']);
    const modalErrors       = Stream([]);

    const updateMaterial = function () {
      saveButtonClasses.push('in-progress', 'disabled');

      args.material.scm().clone().update().then((scm) => {
        saveButtonClasses(['save-pipeline', 'success']);
        args.material.scm().reInitialize(JSON.parse(JSON.stringify(scm)));
        args.parentView.close();
      }, function (data) {
        modalErrors().push(data.message);

        if (data.data) {
          if (data.data.configuration) {
            modalErrors(_.concat(modalErrors(), _.flattenDeep(_.map(data.data.configuration, (conf) => {
              return _.values(conf.errors);
            }))));
          }
        }

        this.saveState('alert');

        if (data.data) {
          self.scmForEdit.reInitialize(data.data);
        }
      });
    };

    this.foo = new Modal({
      title:   `Edit - ${args.material.scm().name()}`,
      body:    () => (<SCMConfigEditWidget material={args.material}/>),
      buttons: () => [
        {
          text:    'Save',
          class:   saveButtonClasses(),
          onclick: updateMaterial
        }
      ]
    });

    this.modal = new Modal({
      subView: {
        component: SCMConfigEditWidget,
        args:      {material: args.material}
      },
      title:   `Edit - ${args.material.scm().name()}`
    });

    this.isPluginMissing = function () {
      return _.isNil(PluginInfos.findById(this.material.scm().pluginMetadata().id()));
    };

    this.modalView = function () {
      if (!this.isPluginMissing()) {
        return this.modal.view();
      }
    };
  },

  view (vnode) {
    const ctrl = vnode.state;

    ctrl.onunload = function () {
      ctrl.modal.destroy();
    };

    const scm  = ctrl.material.scm();
    const data = {Name: scm.name(), AutoUpdate: scm.autoUpdate()};

    scm.configuration().mapConfigurations((conf) => {
      data[_.capitalize(conf.key())] = conf.isSecureValue() ? "***********" : conf.value();
    });

    const editButton = ctrl.isPluginMissing() ? (
        <f.alert><b>{scm.pluginMetadata().id()}</b> plugin seems to be removed.</f.alert>)
      : (<f.editButton class={null} onclick={ctrl.modal.open.bind(ctrl.modal)}/>);

    return (
      <div>
        <div class='pluggable-scm'>
          {editButton}
          <f.row>
            <ul>
              {_.map(data, (v, k) => {
                return (
                  <li>
                    <label class={_.toLower(k)}>{k}</label><span>{v}</span>
                  </li>
                );
              })}
            </ul>
          </f.row>
        </div>
        <f.row>
          <f.inputWithLabel attrName='destination'
                            label="Destination"
                            model={ctrl.material}
                            validate={true}
                            tooltip={{
                              content:   tt.material.destination,
                              direction: 'bottom',
                              size:      'small'
                            }}/>
        </f.row>
        <MaterialFilterWidget material={ctrl.material}/>
        {/*{ctrl.modalView()}*/}
      </div>
    );
  }
};

const PluggableSCMWidget = {
  view (vnode) {
    if (vnode.attrs.material.scm()) {
      return (<EditPluggableSCMWidget material={vnode.attrs.material}/>);
    } else {
      return (<CreateNewScmMaterialButtonWidget material={vnode.attrs.material}/>);
    }
  }
};

module.exports = PluggableSCMWidget;
