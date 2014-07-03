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

require File.join(File.dirname(__FILE__), "/../../../spec_helper")

describe "admin/garage/index.html" do
  before :each do
    @garage_data = mock('garage data')
    template.should_receive(:garage_gc_path).and_return('garage_gc_path')
  end

  it 'should show config.git details' do
    size = "42MB"
    @garage_data.should_receive(:getConfigRepositorySize).and_return(size)
    assign(:garage_data, @garage_data)

    render "admin/garage/index.html"
    response.body.should have_tag("div#config_version_content") do
      with_tag("h3", "Config Version Control GC")
      with_tag("p", "Current size of the config.git directory is: #{size}")
      with_tag("form[action='garage_gc_path'][method='post']") do
        with_tag("input[type='submit'][value=?]", "Perform GC")
      end
    end
  end

  it 'should show config.git flash messages' do
    size = "42MB"
    @garage_data.should_receive(:getConfigRepositorySize).and_return(size)
    assign(:garage_data, @garage_data)
    flash = {:notice => {:gc => "notice"}, :error => {:gc => "error"}}
    template.should_receive(:flash).any_number_of_times.and_return(flash)

    render "admin/garage/index.html"
    response.body.should have_tag("div#config_version_content") do
      with_tag("div.flash") do
        with_tag("p.notice code", "notice")
        with_tag("p.error code", "error")
      end
    end
  end
end