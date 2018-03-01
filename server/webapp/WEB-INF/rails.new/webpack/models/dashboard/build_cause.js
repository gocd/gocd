/*
 * Copyright 2018 ThoughtWorks, Inc.
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

const _                = require('lodash');
const sparkRoutes      = require('helpers/spark_routes');
const AjaxHelper       = require('helpers/ajax_helper');
const MaterialRevision = require('models/dashboard/material_revision');

const BuildCause = function (pipelineName, counter) {
  const self             = this;
  this.materialRevisions = [];

  this.getBuildCause = function () {
    if (self.materialRevisions.length > 0) {
      return;
    }
    AjaxHelper.GET({
      url:        sparkRoutes.buildCausePath(pipelineName, counter),
      apiVersion: 'v1',
    }).then((buildCause) => {
      self.materialRevisions = _.map(buildCause.material_revisions, (revision) => new MaterialRevision(revision));
    });
  };
};

module.exports = BuildCause;
