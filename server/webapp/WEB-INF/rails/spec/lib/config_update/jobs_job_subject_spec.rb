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

describe ::ConfigUpdate::JobsJobSubject do
  include ::ConfigUpdate::JobsJobSubject

  def params
    @params ||= {}
  end

  before(:each) do
    @jobs = JobConfigs.new([@foo = JobConfig.new("foo"), @bar = JobConfig.new("bar"), @baz = JobConfig.new("baz")].to_java(JobConfig))
  end

  it "should load job from jobs collection" do
    params[:job_name] = "bar"
    expect(subject(@jobs)).to eq(@bar)
  end
end
