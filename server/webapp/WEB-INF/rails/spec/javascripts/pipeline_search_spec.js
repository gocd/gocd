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

describe("pipeline_search", function () {

  it("test_should_trigger_search_when_initialized", function () {
    var id = "#pipeline-search";
    var fakePipelineSearch = jasmine.createSpyObj('pipelineSearch', ['forceSearch']);
    spyOn(jQuery.fn, "listsearch").and.callFake(function () {
      return fakePipelineSearch;
    });
    PipelineSearch.initialize(id);
    expect(fakePipelineSearch.forceSearch).toHaveBeenCalled();
  });

});

