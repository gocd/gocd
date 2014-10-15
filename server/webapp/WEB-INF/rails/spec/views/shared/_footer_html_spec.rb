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

describe "/shared/_footer.html" do

  it 'should have copyright, license and third-party information in the footer' do
    render :partial => "shared/footer"
    response.body.should have_tag("ul.copyright") do
      with_tag("li", "Copyright &copy; 2014 ThoughtWorks, Inc. Licensed under Apache License, Version 2.0. Go includes third-party software.") do
        with_tag("a[href='http://www.thoughtworks.com/products'][target='_blank']", 'ThoughtWorks, Inc.')
        with_tag("a[href='http://www.apache.org/licenses/LICENSE-2.0'][target='_blank']", 'Apache License, Version 2.0')
        with_tag("a[href='/help/resources/NOTICE/cruise_notice_file.pdf'][target='_blank']", 'third-party software')
      end
    end
  end

  it 'should have miscellaneous footer links with no support link' do
    render :partial => "shared/footer"
    response.body.should have_tag("ul.links") do
      with_tag("li") do
        with_tag("a[href='/cctray.xml']", "(cc) CCTray Feed")
        with_tag("a[href='/about']", "Server Details")
        with_tag("a[href='http://www.go.cd/community']", "Community")
        without_tag("a[href='http://www.thoughtworks.com/products/support']", "Support")
      end
      with_tag("li.last") do
        with_tag("a[href='http://www.go.cd/documentation/user/current']", "Help")
      end
    end
  end

end