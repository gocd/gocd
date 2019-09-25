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

class GoCacheStore < ActiveSupport::Cache::Store

  def read_entry(name, options = nil)
    cache.get(*key(name, options))
  end

  def write(name, value, options = nil)
    if value.is_a?(String)
      super(name, value.to_java(:string), options)
    else
      super(name, value, options)
    end
  end

  def write_entry(name, value, options = nil)
    cache.put(*(key(name, options) << value))
  end

  def delete_entry(name, options = nil)
    value = cache.get(*key(name, options))
    cache.remove(*key(name, options))
    value
  end

  def clear
    cache.clear
  end

  private
  def cache
    @cache ||= Spring.bean("goCache")
  end

  def key(name, options)
    (options && options.has_key?(:subkey)) ? [name, options[:subkey]] : [name]
  end
end
