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

require 'spec_helper'

describe "/api/dependency_modifications/index.xml.erb" do
  before do
    @modifications = [
      Modification.new(java.util.Date.new(111, 5, 28, 19, 0, 0), "acceptance/2016/twist/1", "acceptance-2016", nil),
      Modification.new(java.util.Date.new(111, 6, 30, 5, 0, 0), "acceptance/3000/twist/2", "acceptance-3000", nil),
      Modification.new(java.util.Date.new(111, 7, 4, 15, 0, 0), "acceptance/3050/twist/3", "acceptance-3050", nil)
    ]
    assign(:modifications, @modifications)

    params[:pipeline_name] = "acceptance"
    params[:stage_name] = "twist"
  end

  it "should render modification list" do
    render :template => '/api/dependency_modifications/index.xml.erb'

    timezone = Time.parse("2011-06-28T19:00:00").strftime('%:z')

    modifications_tag = Nokogiri::XML(response.body).xpath("modifications")
    expect(modifications_tag).to_not be_nil_or_empty
    modifications_tag.tap do |entry|
      expect(entry.xpath("title").text).to eq("acceptance/twist")

      entry_tag_1 = entry.xpath("entry")[0]
      entry_tag_1.tap do |node|
        expect(node.xpath("revision").text).to eq("acceptance/2016/twist/1")
        expect(node.xpath("modifiedTime").text).to eq(@modifications.first.getModifiedTime.iso8601)
        expect(node.xpath("pipelineLabel").text).to eq("acceptance-2016")
      end

      entry_tag_2 = entry.xpath("entry")[1]
      entry_tag_2.tap do |node|
        expect(node.xpath("revision").text).to eq("acceptance/3000/twist/2")
        expect(node.xpath("modifiedTime").text).to eq(@modifications.second.getModifiedTime.iso8601)
        expect(node.xpath("pipelineLabel").text).to eq("acceptance-3000")
      end

      entry_tag_3 = entry.xpath("entry")[2]
      entry_tag_3.tap do |node|
        expect(node.xpath("revision").text).to eq("acceptance/3050/twist/3")
        expect(node.xpath("modifiedTime").text).to eq(@modifications.third.getModifiedTime.iso8601)
        expect(node.xpath("pipelineLabel").text).to eq("acceptance-3050")
      end
    end
  end
end
