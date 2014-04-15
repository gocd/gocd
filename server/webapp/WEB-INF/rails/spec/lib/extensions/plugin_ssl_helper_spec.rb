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

require File.expand_path(File.dirname(__FILE__) + '/../../spec_helper')

describe GoSslHelper do
  def mandatory_ssl
    true
  end
  include GoSslHelper, RailsLocalizer

  before do
    Thread.current[:base_url] = nil
    Thread.current[:ssl_base_url] = nil
  end

  it "should not error render when base_ssl_url is configured" do
    Thread.current[:ssl_base_url] = "boozer.box"
    mandatory_ssl.should be_true
  end

  it "should render and error when no url configured" do
    should_receive(:render).with('shared/ssl_not_configured_error', :status => 404, :layout => true)
    mandatory_ssl.should be_false
  end
end