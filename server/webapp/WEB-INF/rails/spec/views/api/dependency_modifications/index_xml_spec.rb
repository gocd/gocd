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

require File.expand_path(File.dirname(__FILE__) + '/../../../spec_helper')

describe "/api/dependency_modifications/index.xml.erb" do
  before do
    assigns[:modifications] = [
            Modification.new(java.util.Date.new(111, 5, 28, 19, 0, 0), "acceptance/2016/twist/1", "acceptance-2016", nil),
            Modification.new(java.util.Date.new(111, 6, 30, 5, 0, 0), "acceptance/3000/twist/2", "acceptance-3000", nil),
            Modification.new(java.util.Date.new(111, 7, 4, 15, 0, 0), "acceptance/3050/twist/3", "acceptance-3050", nil)
    ]

    params[:pipeline_name] = "acceptance"
    params[:stage_name] = "twist"
  end

  it "should render modification list" do
    render '/api/dependency_modifications/index.xml'

    response.body.should have_tag("modifications") do
      have_tag("title", "acceptance/twist")
      have_tag("entry") do
        with_tag("revision", "acceptance/3016/twist/1")
        with_tag("modifiedTime", "2011-07-01T04:00:00+05:30")
        with_tag("pipelineLabel", "acceptance-3016")
      end
      have_tag("entry") do
        with_tag("revision", "acceptance/3020/twist/1")
        with_tag("modifiedTime", "2011-07-01T08:10:00+05:30")
        with_tag("pipelineLabel", "acceptance-3020")
      end
      have_tag("entry") do
        with_tag("revision", "acceptance/3100/twist/1")
        with_tag("modifiedTime", "2011-07-01T04:10:00+05:30")
        with_tag("pipelineLabel", "acceptance-3100")
      end
    end
  end
end