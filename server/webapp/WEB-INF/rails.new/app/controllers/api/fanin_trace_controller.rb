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

class Api::FaninTraceController < Api::ApiController
  def fanin_trace
    cruise_config = go_config_service.getCurrentConfig()
    pipeline_name = params[:name]
    reporting_fanin_graph = ReportingFanInGraph.new(cruise_config, pipeline_name, pipeline_sql_map_dao)
    @str = reporting_fanin_graph.computeRevisions(pipeline_service.getPipelineTimeline())
    render text: @str
  end

  def fanin_debug
    pipeline_name = params[:name]
    index = params[:index].to_i
    data = pipeline_service.getRevisionsBasedOnDependenciesForDebug(CaseInsensitiveString.new(pipeline_name), index)
    render json: data
  end

  def fanin
    cruise_config = go_config_service.getCurrentConfig()
    pipeline_name = params[:name]
    output = ""
    revisions = pipeline_service.getRevisionsBasedOnDependenciesForReporting(cruise_config, CaseInsensitiveString.new(pipeline_name))
    render text: "No Fan-In" and return unless revisions
    revisions.each do |rev|
      newline = "\n   "
      output = output + "Material: " + rev.getMaterial().to_s + "\n"
      output = output + "***" + newline
      rev.getModifications().each do |mod|
        output = output + "Revision: " + mod.getRevision() + newline
        output = output + "Modified-Time: " + mod.getModifiedTime().to_s + newline
        output = output + "Fingerprint: " + mod.getMaterialInstance().getFingerprint() + newline
        output = output + "Flyweight-Name: " + mod.getMaterialInstance().getFlyweightName() + "\n"
        output = output + "***\n"
      end
      output = output + "---\n\n"
    end
    output = output
    render text: output
  end
end