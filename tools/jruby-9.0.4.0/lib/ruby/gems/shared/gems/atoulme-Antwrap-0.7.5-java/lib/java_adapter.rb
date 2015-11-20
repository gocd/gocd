# Copyright 2008 Caleb Powell 
# Licensed under the Apache License, Version 2.0 (the "License"); 
# you may not use this file except in compliance with the License. 
# You may obtain a copy of the License at 
#
#   http://www.apache.org/licenses/LICENSE-2.0 
# 
# Unless required by applicable law or agreed to in writing, software 
# distributed under the License is distributed on an "AS IS" BASIS, 
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
# See the License for the specific language governing permissions and limitations 
# under the License.

module Antwrap
  module JavaAdapter
    
    def is_jruby_interpreter?
      return RUBY_PLATFORM =~ /java/
    end

    def import_class(name)
      if is_jruby_interpreter?
        return import_using_jruby(name)
      else
        return Rjb::import(name)
      end
    end

    def load(files=[], args=[])
      if is_jruby_interpreter?
        files.each {|jar| require jar }
      else
        Rjb::load(files.join(File::PATH_SEPARATOR), [])
      end
    end
    
    module_function :import_class, :load, :is_jruby_interpreter?

    if is_jruby_interpreter?
      require 'java'
    else
      require 'rubygems'
      require 'rjb'
    end
    
    private    
    def JavaAdapter.import_using_jruby(name)
      java_import(name) 
      return remove_const(name.scan(/[_a-zA-Z0-9$]+/).last)
    end
  end
end
