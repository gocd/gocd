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

describe ApiV1::Config::TrackingTool::TrackingToolRepresenter do
  describe :generic_tool do
    it 'renders generic tracking tool with hal representation' do
      generic_tracking_tool = TrackingTool.new('link', 'regex')
      presenter             = ApiV1::Config::TrackingTool::TrackingToolRepresenter.new(generic_tracking_tool)
      actual_json           = presenter.to_hash(url_builder: UrlBuilder.new)
      expect(actual_json).to eq(generic_tracking_tool_hash)
    end

    it 'should deserialize' do
      presenter           = ApiV1::Config::TrackingTool::TrackingToolRepresenter.new(TrackingTool.new)
      deserialized_object = presenter.from_hash(generic_tracking_tool_hash)
      expected            = TrackingTool.new('link', 'regex')
      expect(deserialized_object).to eq(expected)
    end

    it 'should render validation errors' do
      tracking_tool= TrackingTool.new
      tracking_tool.validateTree(nil)

      presenter   = ApiV1::Config::TrackingTool::TrackingToolRepresenter.new(tracking_tool)
      actual_json = presenter.to_hash(url_builder: UrlBuilder.new)
      expect(actual_json).to eq(generic_tracking_tool_with_errors_hash)
    end

    def generic_tracking_tool_hash
      {
        type:       'generic',
        attributes: {
          url_pattern: 'link',
          regex:       'regex'
        }
      }
    end

    def generic_tracking_tool_with_errors_hash
      {
        type:       'generic',
        attributes: {
          url_pattern: '',
          regex:       ''
        },
        errors:     {
          url_pattern:  [
                   'Link should be populated',
                   "Link must be a URL containing '${ID}'. Go will replace the string '${ID}' with the first matched group from the regex at run-time."
                 ],
          regex: ['Regex should be populated']
        }
      }
    end
  end

  describe :mingle do
    it 'renders mingle tracking tool with hal representation' do

      mingle_tracking_tool= MingleConfig.new('http://mingle.example.com', 'my_project', "status > 'In Dev'")
      presenter           = ApiV1::Config::TrackingTool::TrackingToolRepresenter.new(mingle_tracking_tool)
      actual_json         = presenter.to_hash(url_builder: UrlBuilder.new)
      expect(actual_json).to eq(mingle_tracking_tool_hash)
    end

    it 'should deserialize' do
      presenter           = ApiV1::Config::TrackingTool::TrackingToolRepresenter.new(MingleConfig.new)
      deserialized_object = presenter.from_hash(mingle_tracking_tool_hash)
      expected            = MingleConfig.new('http://mingle.example.com', 'my_project', "status > 'In Dev'")
      expect(deserialized_object).to eq(expected)
    end

    it 'should render validation errors' do
      mingle_tracking_tool= MingleConfig.new('http://mingle.example.com', 'wrong project identifier', "status > 'In Dev'")
      mingle_tracking_tool.validateTree(nil)
      mingle_tracking_tool_with_errors_hash={
        type:       'mingle',
        attributes: {
          base_url:                'http://mingle.example.com',
          project_identifier:      'wrong project identifier',
          mql_grouping_conditions: "status > 'In Dev'"
        },
        errors:     {
          base_url: ['Should be a URL starting with https://'], project_identifier: ["Should be a valid mingle identifier."]
        }
      }
      presenter                            = ApiV1::Config::TrackingTool::TrackingToolRepresenter.new(mingle_tracking_tool)
      actual_json                          = presenter.to_hash(url_builder: UrlBuilder.new)
      expect(actual_json).to eq(mingle_tracking_tool_with_errors_hash)
    end

    def mingle_tracking_tool_hash
      {
        type:       'mingle',
        attributes: {
          base_url:                'http://mingle.example.com',
          project_identifier:      'my_project',
          mql_grouping_conditions: "status > 'In Dev'"
        }
      }

    end
  end
end
