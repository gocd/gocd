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
import Stream from "mithril/stream";

export const VM = function () {
  const GLOBAL_KEY   = 'global';
  const PIPELINE_KEY = 'pipeline';

  const selection = Stream(GLOBAL_KEY);

  this.isGlobalTabSelected   = () => selection() === GLOBAL_KEY;
  this.isPipelineTabSelected = () => selection() === PIPELINE_KEY;

  this.setGlobalTabSelection   = () => selection(GLOBAL_KEY);
  this.setPipelineTabSelection = () => selection(PIPELINE_KEY);
};

