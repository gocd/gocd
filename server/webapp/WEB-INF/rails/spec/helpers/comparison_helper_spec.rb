#
# Copyright 2019 ThoughtWorks, Inc.
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
#

require 'rails_helper'

describe ComparisonHelper do
  include ComparisonHelper
  include ReflectiveUtil

  describe "pipeline_compare_href" do
    it "should construct url for the given pipeline compare parameters" do
      actual = pipeline_compare_href("pipeline-name", 5444, 6666)
      expect(actual).to eq(compare_pipelines_path(:pipeline_name => 'pipeline-name', :from_counter => 5444, :to_counter => 6666))
    end

     it "should construct url by switching 'from' and 'to' counters if 'from' is greater than 'to'" do
      actual = pipeline_compare_href("pipeline-name", 6666, 5444)
      expect(actual).to eq(compare_pipelines_path(:pipeline_name => 'pipeline-name', :from_counter => 5444, :to_counter => 6666))
    end
  end

  describe "any_match?" do
    it "should match in a case insensitive manner" do
      expect(any_match?("foo", "AFOOB")).to eq(true)
    end

    it "should match multiple args" do
      expect(any_match?("foo", "BAR", "FOOBAR")).to eq(true)
    end

    it "should ignore nils" do
      expect(any_match?("foo", "BAR", nil, "FOOBAR")).to eq(true)
    end
  end
end
