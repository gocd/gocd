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

const BuildDetailsWidget = {

  oninit (vnode) {
    const args = vnode.attrs;

    this.closeDropDown = function () {
      args.dropdown.reset(false);
    };
  },

  view (vnode) {
    const ctrl = vnode.state;
    const args = vnode.attrs;

    const buildDetails = args.agent.buildDetails();
    return (
      <div class="build-details" onclick={ctrl.closeDropDown}>
        <ul>
          <li><a href={buildDetails.pipelineUrl()}>Pipeline - {buildDetails.pipelineName()}</a></li>
          <li><a href={buildDetails.stageUrl()}>Stage - {buildDetails.stageName()}</a></li>
          <li><a href={buildDetails.jobUrl()}>Job - {buildDetails.jobName()}</a></li>
        </ul>
      </div>
    );
  }
};

module.exports = BuildDetailsWidget;
