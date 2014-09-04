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

module FailuresHelper
  include ParamEncoder

  def esc(string, sequence)
    string.gsub(sequence, sequence * 2)
  end

  def fbh_failure_detail_popup_id_for_failure(job_identifier, test_suite_name, test_case_name)
    "for_fbh_failure_details_#{esc(job_identifier.buildLocator(), '_')}_#{esc(test_suite_name, '_')}_#{esc(test_case_name, '_')}".to_json()
  end

  def failure_details_link(job_id, suite_name, test_name)
    id = fbh_failure_detail_popup_id_for_failure(job_id, suite_name, test_name)
    link =<<-LINK
<a href='#{failure_details_path(job_id, suite_name, test_name)}' id=#{id} class="fbh_failure_detail_button" title='#{(l.string("VIEW_FAILURE_DETAILS")).html_safe}'>[Trace]</a>
LINK
    link.html_safe
  end

  def failure_details_path job_id, suite_name, test_name
    failure_details_internal_path(:pipeline_name => job_id.getPipelineName(), :pipeline_counter => job_id.getPipelineCounter(), :stage_name => job_id.getStageName(),
                                  :stage_counter => job_id.getStageCounter(), :job_name => job_id.getBuildName(),
                                  :suite_name => CGI.escape(enc(suite_name)), :test_name => CGI.escape(enc(test_name)))
  end
end