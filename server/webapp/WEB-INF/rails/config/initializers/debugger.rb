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

class Object
  def breakpoint
  end
end

if Rails.env.development?
  class Object
    def breakpoint
      puts "Welcome to the JRuby \"debugger\""
      print "> "
      while true
        cmd = STDIN.readline.strip
        break if cmd =~ /^(END|QUIT|STOP|CONT|CONTINUE)$/
        begin
          result = eval cmd
          puts "=> #{result.inspect}"
        rescue => e
          puts "Error: #{e.inspect}\n#{e.backtrace.collect {|line| "  " + line}}"
        end if cmd !~ /^\s*$/
        print "> "
      end
    end
  end
end