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
import _ from "lodash";

const Modifications = {};

Modifications.Modification = function (modification) {
  this.modifiedTime = modification.modified_time;
  this.revision     = modification.revision;
};

Modifications.Modification.Pipeline = function (modification) {
  Modifications.Modification.call(this, modification);
  this.pipelineLabel   = modification.pipeline_label;
  this.stageDetailsUrl = modification._links.stage_details_url.href;
};

const packageMaterialCommentFor = (comment) => {
  const commentJSON   = JSON.parse(comment);
  const trackbackURL  = commentJSON['TRACKBACK_URL'] ? commentJSON['TRACKBACK_URL'] : "Not Provided";
  const packageOrigin = commentJSON.COMMENT ? commentJSON.COMMENT : "";
  return `${packageOrigin}Trackback: ${trackbackURL}`;
};

Modifications.Modification.SCM = function (modification, materialType) {
  Modifications.Modification.call(this, modification);
  this.username = modification.user_name;
  this.comment  = (materialType === 'Package') ? packageMaterialCommentFor(modification.comment) : modification.comment;
  this.vsmPath  = modification._links.vsm.href;
};

export const MaterialRevision = function (info) {
  const self = this;

  this.materialType         = info.material_type;
  this.materialName         = info.material_name;
  this.changed              = info.changed;
  this.modifications        = getModificationsFor(info);
  this.isDependencyMaterial = () => (self.materialType === 'Pipeline');
};

const isDependencyMaterial = (type) => (type === 'Pipeline');

const getModificationsFor = (info) => {
  const materialType   = info.material_type;
  const kindOfMaterial = isDependencyMaterial(materialType) ? 'Pipeline' : 'SCM';

  return _.map(info.modifications, (modification) => {
    return new Modifications.Modification[kindOfMaterial](modification, materialType);
  });
};
