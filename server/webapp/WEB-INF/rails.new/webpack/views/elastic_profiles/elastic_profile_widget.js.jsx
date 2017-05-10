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

let m = require('mithril');
let f = require('helpers/form_helper');

const Stream          = require('mithril/stream');
const ComponentMixins = require('helpers/mithril_component_mixins');

const ElasticProfileWidget = {
  oninit (vnode) {
    this.args = vnode.attrs;
    ComponentMixins.HasViewModel.call(this);

    const vmStateKey = `show-${  vnode.attrs.profile.id()}`;

    this.vmState(vmStateKey, Stream(false));

    this.toggleHide = function () {
      this.vmState(vmStateKey)(!this.vmState(vmStateKey)());
    };

    this.showState = function () {
      return this.vmState(vmStateKey)() ? 'show' : 'hide';
    };
  },

  view (vnode) {
    const profile = vnode.attrs.profile;
    let image;
    let actionIcons;

    if (vnode.attrs.pluginInfo) {
      image       = (<img src={vnode.attrs.pluginInfo.imageUrl()}/>);
      actionIcons = (
        <div class="plugin-actions">
          <f.link class='edit-profile' onclick={vnode.attrs.onEdit}/>
          <f.link class='delete-profile-confirm' onclick={vnode.attrs.onDelete}/>
        </div>
      );
    } else {
      image       = (<span class="unknown-plugin-icon" title="Plugin not found"/>);
      actionIcons = (
        <div class="plugin-actions">
          <f.link class='edit-profile disabled' title="Plugin not found"/>
          <f.link class='delete-profile-confirm disabled' title="Plugin not found"/>
        </div>
      );
    }
    return (
      <div class="elastic-profile">
        <div class="elastic-profile-header" onclick={vnode.state.toggleHide.bind(vnode.state)}>
            <span class="plugin-icon">
              {image}
            </span>
          <div class="plugin-description">
            <div class="profile-id"><span class="key">Id: </span><span class="value">{profile.id()}</span></div>
            <div class="plugin-id"><span class="key">Plugin ID:</span> <span class="value">{profile.pluginId()}</span>
            </div>
          </div>

        </div>
        <div class={`plugin-config-read-only ${  vnode.state.showState()}`}>
          <dl class="key-value-pair">

            {profile.properties().mapConfigurations((configuration) => {
              return [
                (<dt>{configuration.key()}</dt>),
                (<dd>
                  <pre>{configuration.value()}</pre>
                </dd>)
              ];
            })}
          </dl>
        </div>
        {actionIcons}
      </div>
    );
  }
};

module.exports = ElasticProfileWidget;
