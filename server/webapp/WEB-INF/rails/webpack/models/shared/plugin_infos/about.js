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

const Stream = require('mithril/stream');

const About = function (data) {
  this.name                   = Stream(data.name);
  this.version                = Stream(data.version);
  this.targetGoVersion        = Stream(data.targetGoVersion);
  this.description            = Stream(data.description);
  this.targetOperatingSystems = Stream(data.targetOperatingSystems);
  this.vendor                 = Stream(data.vendor);
};

About.fromJSON = (data = {}) => new About({
  name:                   data.name,
  version:                data.version,
  targetGoVersion:        data.target_go_version,
  description:            data.description,
  targetOperatingSystems: data.target_operating_systems,
  vendor:                 About.Vendor.fromJSON(data.vendor)
});

About.Vendor = function ({name, url}) {
  this.name = Stream(name);
  this.url  = Stream(url);
};

About.Vendor.fromJSON = (data = {}) => new About.Vendor({
  name: data.name,
  url:  data.url
});

module.exports = About;
