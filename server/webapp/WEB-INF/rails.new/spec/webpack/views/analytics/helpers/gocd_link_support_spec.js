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

describe("GoCD Link Support", () => {
  const GoCDLinkSupport = require("views/analytics/helpers/gocd_link_support");

  beforeEach(() => {
    spyOn(window, 'open');
  });

  it('should link to job details page', () => {
    const linkTo = 'job_details_page';
    const params = {
      'pipeline_name':    'up42',
      'pipeline_counter': 1,
      'stage_name':       'up42_stage',
      'stage_counter':    1,
      'job_name':         'up42_job'
    };

    GoCDLinkSupport[linkTo](params);

    expect(window.open).toHaveBeenCalled();
    const jobDetailsPagePath = window.open.calls.mostRecent().args[0];
    expect(jobDetailsPagePath).toBe(`/go/tab/build/detail/${params.pipeline_name}/${params.pipeline_counter}/${params.stage_name}/${params.stage_counter}/${params.job_name}`);
  });
});
