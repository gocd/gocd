#
# Copyright 2021 ThoughtWorks, Inc.
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

module CacheTestHelpers
  def check_fragment_caching(obj1, obj2, cache_key_proc)
    ActionController::Base.cache_store.clear
    ActionController::Base.perform_caching = false

    yield obj1
    obj_1_not_cached_body = response.body
    expect(ActionController::Base.cache_store.writes.length).to eq(0)
    allow_double_render
    expect(ActionController::Base.cache_store.read(*cache_key_proc[obj2])).to be_nil
    ActionController::Base.perform_caching = true

    yield obj2
    expect(ActionController::Base.cache_store.read(*cache_key_proc[obj2])).not_to be_nil
    expect(ActionController::Base.cache_store.writes.length).to eq(1)
    allow_double_render

    yield obj2
    expect(ActionController::Base.cache_store.writes.length).to eq(1)
    allow_double_render

    expect(ActionController::Base.cache_store.read(*cache_key_proc[obj1])).to be_nil
    yield obj1
    expect(ActionController::Base.cache_store.writes.length).to eq(2)
    expect(ActionController::Base.cache_store.read(*cache_key_proc[obj1])).not_to be_nil
    assert_equal obj_1_not_cached_body, response.body
  ensure
    ActionController::Base.perform_caching = false
  end

# erase_results does not exist, in Rails 3 and above.
# https://github.com/markcatley/responds_to_parent/pull/2/files
# http://www.dixis.com/?p=488
  def allow_double_render
    self.instance_variable_set(:@_response_body, nil)
  end
end