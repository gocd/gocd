##########################################################################
# Copyright 2015 ThoughtWorks, Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
##########################################################################

require 'spec_helper'

describe ApiV1::JobSummaryRepresenter do

  it 'renders an job with hal representation' do
    job_instance=com.thoughtworks.go.helper.JobInstanceMother.jobInstance('job1', '')
    presenter   = ApiV1::JobSummaryRepresenter.new(job_instance)
    actual_json = presenter.to_hash(url_builder: UrlBuilder.new)

    expect(actual_json).to have_links(:doc)

    expect(actual_json).to have_link(:doc).with_url('https://api.go.cd/#jobs')

    actual_json.delete(:_links)
    expect(actual_json).to eq(job_hash(job_instance))
  end

  def job_hash(job_instance)
    {
      name:         job_instance.getName(),
      result:       job_instance.getResult(),
      scheduled_at: job_instance.getScheduledDate,
      state:        job_instance.getState()
    }
  end
end
