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

class HashMapKey
  SEPERATOR = '<|>'
  ESCAPED_SEPERATOR = '<<|>>'

  attr_reader :key, :is_string, :key_as_str

  def initialize key
    @key = key
    @key_as_str = @key.to_s
    @is_string = (@key_as_str == @key)
  end

  def <=> other
    if (!(@is_string ^ other.is_string))
      return @key_as_str <=> other.key_as_str
    end
    return @is_string ? -1 : 1
  end

  def == other
    @key == other.key
  end

  def self.replace_special_chars str
    str.gsub('-', '--').gsub(SEPERATOR, ESCAPED_SEPERATOR)
  end

  def self.hypen_safe_key_for(hash_or_object)
    hash = to_hash(hash_or_object)
    buffer = java.lang.StringBuilder.new
    hash.keys.map{|k| HashMapKey.new(k) }.sort.each do |key|
      buffer.append(replace_special_chars(key.key_as_str)).append(SEPERATOR).append(replace_special_chars(hash[key.key].to_s))
    end
    buffer.to_s
  end

  def self.to_hash(object)
    object.respond_to?(:has_key?) ? object : {:object_type => object.class, :object_id => object.id} 
  end
end

