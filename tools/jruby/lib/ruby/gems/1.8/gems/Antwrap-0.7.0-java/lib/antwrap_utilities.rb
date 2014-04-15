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
  module AntwrapClassLoader
    
    require 'find'
    
    def match(*paths)
      
      matched = Array.new 
      Find.find(*paths){ |path| matched << path if yield path }
      return matched
      
    end
    
    def load_ant_libs(ant_home)
      
      jars = match(ant_home + File::SEPARATOR + 'lib') {|p| ext = p[-4...p.size]; ext && ext.downcase == '.jar'} 
      
      if(RUBY_PLATFORM == 'java')
        jars.each {|jar| require jar }
      else
        Rjb::load(jars.join(File::PATH_SEPARATOR), [])
      end
      
    end
    
    module_function :match, :load_ant_libs
    
  end
end