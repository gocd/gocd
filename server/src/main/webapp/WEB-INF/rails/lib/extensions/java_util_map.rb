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

java.util.Map.class_eval do

  def to_hash
    hash = {}
    entrySet().each do |entry|
      hash[entry.getKey()] = entry.getValue()
    end
    hash
  end
end


# map_mixin to patch LinkedHashMap and HashMap. it must be done directly on the classes,
# using a module mixin does not work, and injecting in the Map interface does not work either
# but injecting in the class works.
# see https://github.com/jruby/jruby/issues/1249

# this is a temporary fix to solve a bug in JRuby where classes implementing the Map interface, like LinkedHashMap
# have a bug in the has_key? method that is implemented in the Enumerable module that is somehow mixed in the Map interface.
# this bug makes has_key? (and all its aliases) return false for a key that has a nil value.
# Only LinkedHashMap is patched here because patching the Map interface is not working.
# TODO find proper fix, and submit upstream
# releavant JRuby files:
# https://github.com/jruby/jruby/blob/master/core/src/main/ruby/jruby/java/java_ext/java.util.rb
# https://github.com/jruby/jruby/blob/master/core/src/main/java/org/jruby/java/proxies/MapJavaProxy.java

Java::JavaUtil::HashMap.class_eval {alias merge ruby_merge}
Java::JavaUtil::LinkedHashMap.class_eval {alias merge ruby_merge}
