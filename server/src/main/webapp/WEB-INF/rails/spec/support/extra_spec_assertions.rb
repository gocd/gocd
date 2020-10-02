#
# Copyright 2020 ThoughtWorks, Inc.
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

module ExtraSpecAssertions
  def assert_redirected_with_flash(url, msg, flash_class, params = [])
    assert_redirect(url)
    params.each { |param| expect(response.redirect_url).to match(/#{param}/) }
    flash_guid = $1 if response.redirect_url =~ /[?&]fm=([\w-]+)?(&.+){0,}$/
    flash = controller.flash_message_service.get(flash_guid)
    expect(flash.to_s).to eq(msg)
    expect(flash.flashClass()).to eq(flash_class)
  end

  def assert_redirect(url)
    expect(response.status).to eq(302)
    expect(response.redirect_url).to match(%r{#{url}})
  end
end