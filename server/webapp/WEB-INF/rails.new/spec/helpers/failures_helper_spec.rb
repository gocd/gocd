##########################GO-LICENSE-START################################
# Copyright 2014 ThoughtWorks, Inc.
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
##########################GO-LICENSE-END##################################

require 'rails_helper'

describe FailuresHelper do
  include FailuresHelper
  include RailsLocalizer

  describe "fbh_details_link" do
    it "should ensure uniqueness" do
      job_id = JobIdentifier.new("pipeline-foo", 12, "label-1020", "stage-bar", "34", "build-dev")
      id = fbh_failure_detail_popup_id_for_failure(job_id, "my-suite", "his-test")
      expect(id).to match(/pipeline-foo/)
      expect(id).to match(/12/)
      expect(id).to match(/stage-bar/)
      expect(id).to match(/34/)
      expect(id).to match(/build-dev/)
      expect(id).to match(/my-suite/)
      expect(id).to match(/his-test/)
    end

    it "should enforce uniqueness of suite_name test_name combination" do
      job_id = JobIdentifier.new("pipeline_foo", 12, "label-1020", "stage_bar", "34", "build_dev")
      expect(fbh_failure_detail_popup_id_for_failure(job_id, "my", "suite_his_test")).not_to eq(fbh_failure_detail_popup_id_for_failure(job_id, "my_suite", "his_test"))
    end

    it "should escape single quotes" do
      job_id = JobIdentifier.new("p", 12, "l", "s", "34", "j")
      id = fbh_failure_detail_popup_id_for_failure(job_id, 'm"y', 'su\"ite_"his_test')
      expect(id).to match(/m\\"y/)
      expect(id).to match(/su\\\\\\"ite/)
    end

    it "should render anchor and js for failed_test details" do
      job_id = JobIdentifier.new("pipeline1", 1234, "pip-label-1", "defaultStage", "5678", "defaultJob")
      expect(failure_details_link(job_id, "cruise.testing.JUnit", "a")).to eq(%Q{<a href='/failures/pipeline1/1234/defaultStage/5678/defaultJob/Y3J1aXNlLnRlc3RpbmcuSlVuaXQ%253D%250A/YQ%253D%253D%250A' id="for_fbh_failure_details_pipeline1/1234/defaultStage/5678/defaultJob_cruise.testing.JUnit_a" class="fbh_failure_detail_button" title='View failure details'>[Trace]</a>\n})
    end
  end
end
