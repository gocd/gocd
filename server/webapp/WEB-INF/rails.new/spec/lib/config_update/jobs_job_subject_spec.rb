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

describe ::ConfigUpdate::JobsJobSubject do
  include ::ConfigUpdate::JobsJobSubject

  before(:each) do
    allow(self).to receive(:params).and_return(@params = {})
    @jobs = JobConfigs.new([@foo = JobConfig.new("foo"), @bar = JobConfig.new("bar"), @baz = JobConfig.new("baz")].to_java(JobConfig))
  end

  it "should load job from jobs collection" do
    params[:job_name] = "bar"
    subject(@jobs).should == @bar
  end
end
