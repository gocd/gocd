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

class GoCacheStore < ActiveSupport::Cache::Store

  VIEW_PREFIX = 'view_'

  def read_entry(name, options = nil)
    entry = cache.get(*key(name, options))
    return nil unless entry

    entry = entry.dup
    if name.start_with?(VIEW_PREFIX) && entry.value.is_a?(java.lang.String)
      entry.value = entry.value.to_s
    end
    entry
  end

  def write(name, value, options = nil)
    if name.start_with?(VIEW_PREFIX) && value.is_a?(String)
      return super(name, value.to_java(:string), options)
    end
    super(name, value, options)
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