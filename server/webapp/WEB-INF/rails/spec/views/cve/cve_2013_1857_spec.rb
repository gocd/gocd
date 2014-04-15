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

describe "cve_2013_1857" do

  before :each do
    @sanitizer = HTML::WhiteListSanitizer.new
  end
  it "test_x03a" do
    assert_sanitized %(<a href="javascript&#x3a;alert('XSS');">), "<a>"
    assert_sanitized %(<a href="javascript&#x003a;alert('XSS');">), "<a>"
    assert_sanitized %(<a href="http&#x3a;//legit">), %(<a href="http://legit">)
    assert_sanitized %(<a href="javascript&#x3A;alert('XSS');">), "<a>"
    assert_sanitized %(<a href="javascript&#x003A;alert('XSS');">), "<a>"
    assert_sanitized %(<a href="http&#x3A;//legit">), %(<a href="http://legit">)
  end

  def assert_sanitized(input, expected)
    if input
      assert_dom_equal expected, @sanitizer.sanitize(input)
    else
      assert_nil @sanitizer.sanitize(input)
    end
  end
end
