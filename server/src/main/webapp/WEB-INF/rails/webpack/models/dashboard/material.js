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
import _ from "lodash";
import {SparkRoutes} from "helpers/spark_routes";
import m from "mithril";
import Stream from "mithril/stream";
import {AjaxHelper} from "helpers/ajax_helper";

export class Material {
  selectRevision     = (revision) => {
    this.updateSearchText(revision);
    this.selection(revision);
  };
  updateSearchText   = (newText) => {
    this.selection('');
    this.searchText(newText);
    this.debouncedSearch();
  };
  isRevisionSelected = () => {
    return !_.isEmpty(this.selection());
  };
  performSearch      = () => {
    this.searchInProgress(true);
    return AjaxHelper.GET({
      url:        SparkRoutes.pipelineMaterialSearchPath(this.pipelineName, this.fingerprint, this.searchText()),
      apiVersion: 'v1',
    }).then((result) => {
      this.searchResults(result);
      if ((result.length === 1) && (this.searchText() === result[0].revision)) {
        this.selection(this.searchText());
      }
    }).always(() => {
      this.searchInProgress(false);
      m.redraw();
    });
  };

  debouncedSearch = _.debounce(() => {
    this.performSearch();
  }, 200);

  validate = () => {
    if (this.selection() !== this.searchText()) {
      const msg = `Invalid revision ${this.searchText()} specified for material ${this.name}`;
      this.error(msg);
      return false;
    }
    return true;
  };

  constructor({type, name, fingerprint, folder, revision, pipelineName}) {
    this.type             = type;
    this.name             = name;
    this.fingerprint      = fingerprint;
    this.destination      = folder;
    this.revision         = revision;
    this.pipelineName     = pipelineName;
    this.selection        = Stream('');
    this.searchText       = Stream('');
    this.searchInProgress = Stream(false);
    this.searchResults    = Stream([]);
    this.error            = Stream();
  }
}

