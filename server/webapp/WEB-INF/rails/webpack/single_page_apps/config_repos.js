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

const m = require("mithril");
const ReposList = require("views/config_repos/config_repos_list");
const ReposModel = require("models/config_repos/config_repos");
const ReposVM = require("views/config_repos/models/config_repos_vm");

const vm = new ReposVM(new ReposModel()).fetchReposData();

const ConfigReposPage = {
  view() {
    return m(ReposList, {vm});
  }
};

window.addEventListener("DOMContentLoaded", () => {
  const root = document.getElementById("config-repos");
  m.mount(root, ConfigReposPage);
});
