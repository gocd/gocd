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

module ApiHeaderSetupForRouting
  def setup_header
    expect(Rack::MockRequest).to receive(:env_for).and_wrap_original do |original_method, *args, &block|
      original_method.call(*args, &block).tap { |hash| hash['HTTP_ACCEPT'] = described_class::DEFAULT_ACCEPTS_HEADER }
    end
  end

  def stub_confirm_header
    expect(Rack::MockRequest).to receive(:env_for).and_wrap_original do |original_method, *args, &block|
      original_method.call(*args, &block).tap { |hash| hash['HTTP_CONFIRM'] = "true" }
    end
  end
end
