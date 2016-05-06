##########################################################################
# Copyright 2016 ThoughtWorks, Inc.
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

describe ApiV1::Config::StageWithMinimalAttributesRepresenter do
  describe :serialize do
    it 'should render basic stage with hal representation' do
      stage = StageConfigMother.custom('build', 'junit', 'jasmine')
      presenter   = ApiV1::Config::StageWithMinimalAttributesRepresenter.new(stage)

      actual_json = presenter.to_hash(url_builder: UrlBuilder.new)

      expect(actual_json).to eq({name: 'build', jobs: ['junit', 'jasmine']})
    end
  end
end