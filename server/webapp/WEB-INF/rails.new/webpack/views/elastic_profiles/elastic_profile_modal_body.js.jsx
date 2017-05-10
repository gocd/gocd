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

let m             = require("mithril");
let f             = require('helpers/form_helper');
let AngularPlugin = require('views/shared/angular_plugin');

const _               = require('lodash');
const $               = require('jquery');
const ElasticProfiles = require('models/elastic_profiles/elastic_profiles');

const autoCompleteCleanup = function (attrName) {
  return function (vnode) {
    const elem       = $(vnode.dom);
    const $inputElem = $($(elem).find(`input[data-prop-name='${  attrName  }']`));
    $inputElem.textcomplete('destroy');
  };
};

const autoCompleteInitialize = function (model, pluginInfos, attrName, selectCb) {
  return function (vnode) {
    const elem       = $(vnode.dom);
    const $inputElem = $($(elem).find(`input[data-prop-name='${  attrName  }']`));
    const values     = pluginInfos().collectPluginInfoProperty('id');

    $inputElem.textcomplete([
      {
        match: /([^\s].*)$/,
        index: 1, // use the second match in the regex to extract the search term
        search (term, callback) {
          term                 = term.toLowerCase();
          const filteredValues = _(values).map((word) => {
            return word.toLowerCase().indexOf(term) >= 0 ? word : null;
          }).compact().value();
          callback(filteredValues);
        },
        replace (word) {
          return word;
        }
      }
    ], {
      zIndex: '10000'
    });

    $inputElem.on('textComplete:select', () => {
      model()[attrName]($inputElem.val());
      if (selectCb) {
        selectCb($inputElem.val());
      }
      m.redraw();
    });
  };
};

const ElasticProfileModalBody = {
  oninit (vnode) {
    const setPluginInfo = function (pluginInfo) {
      vnode.attrs.pluginInfo(pluginInfo);
      vnode.attrs.saveDisabled(!pluginInfo);
    };

    this.selectPluginId = function () {
      const pluginInfo = vnode.attrs.pluginInfos().findById(vnode.attrs.profile().pluginId());

      if (!pluginInfo) {
        return;
      }

      setPluginInfo(pluginInfo);
      const newProfile = new ElasticProfiles.Profile({id: vnode.attrs.profile().id(), pluginId: pluginInfo.id()});
      newProfile.etag(vnode.attrs.profile().etag());
      vnode.attrs.profile(newProfile);
    };
  },

  view (vnode) {
    let profileIdDisabledMessage;

    if (vnode.attrs.errorMessage && vnode.attrs.errorMessage()) {
      return (
        <f.alert>{vnode.attrs.errorMessage()}</f.alert>
      );
    }

    if (!vnode.attrs.profile()) {
      return (
        <div class="modal-spinner"/>
      );
    }

    if (!vnode.attrs.newProfile) {
      profileIdDisabledMessage = (<div>Editing of profile ID is disabled</div>);
    }

    let angularPlugin;

    if (vnode.attrs.pluginInfo()) {
      angularPlugin = (<AngularPlugin pluginInfo={vnode.attrs.pluginInfo().profileSettings}
                                      configuration={vnode.attrs.profile().properties}
                                      key={vnode.attrs.pluginInfo() ? vnode.attrs.pluginInfo().id() : 'no-plugin'}/>);
    }

    return (
      <div>
        <f.row collapse="true">
          <f.row key="static-stuff" class="id-and-plugin-id-wrapper">
            <f.inputWithLabel model={vnode.attrs.profile()}
                              attrName="id"
                              validate="true"
                              isRequired="true"
                              disabled={!vnode.attrs.newProfile}
                              message={profileIdDisabledMessage}
                              label="Id"
                              key="profile-id"/>
            <f.inputWithLabel model={vnode.attrs.profile()}
                              key="plugin-id-autocomplete"
                              validate="true"
                              attrName="pluginId"
                              isRequired="true"
                              label="Plugin Id"
                              onChange={vnode.state.selectPluginId.bind(vnode.state)}
                              oncreate={autoCompleteInitialize(vnode.attrs.profile, vnode.attrs.pluginInfos, 'pluginId', vnode.state.selectPluginId.bind(vnode.state))}
                              onbeforeremove={autoCompleteCleanup('pluginId')}
                              end="true"/>
          </f.row>
        </f.row>

        <div class="row collapse">
          {angularPlugin}
        </div>
      </div>
    );
  }
};

module.exports = ElasticProfileModalBody;
