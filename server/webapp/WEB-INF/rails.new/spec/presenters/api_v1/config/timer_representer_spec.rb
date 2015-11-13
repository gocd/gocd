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

describe ApiV1::Config::TimerRepresenter do

  it "should represent a timer" do
    timer = com.thoughtworks.go.config.TimerConfig.new("0 0 7 ? * MON", false)
    timer.validateTree(nil)

    presenter   = ApiV1::Config::TimerRepresenter.new(timer)
    actual_json = presenter.to_hash(url_builder: UrlBuilder.new)
    expect(actual_json).to eq(timer_hash)
  end

  it "should represent validation errors" do
    timer = com.thoughtworks.go.config.TimerConfig.new("SOME JUNK TIMER SPEC", false)
    timer.validateTree(nil)
    presenter   = ApiV1::Config::TimerRepresenter.new(timer)
    actual_json = presenter.to_hash(url_builder: UrlBuilder.new)
    expect(actual_json).to eq(timer_hash_with_error)
  end

  it "should deserialize" do
    presenter           = ApiV1::Config::TimerRepresenter.new(com.thoughtworks.go.config.TimerConfig.new)
    deserialized_object = presenter.from_hash(timer_hash)
    expected            = com.thoughtworks.go.config.TimerConfig.new("0 0 7 ? * MON", false)
    expect(deserialized_object).to eq(expected)
  end

  def timer_hash
    {
      spec:            "0 0 7 ? * MON",
      only_on_changes: false
    }
  end

  def timer_hash_with_error
    {
        spec: "SOME JUNK TIMER SPEC",
        only_on_changes: false,
        errors: {
          spec: ["Invalid cron syntax: Illegal characters for this position: 'SOM'"]
        }
    }
  end
end
