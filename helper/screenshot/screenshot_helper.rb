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

require 'pathname'

class ScreenshotHelper

    def replace(startWith, endWith, str)
        new_str = ""
        begin  
            head = startWith.match(str)
            tail = endWith.match(str)
            headresult = head.pre_match
            rest = headresult + "../../../screenshots/"+ tail.post_match
            
            
            head = startWith.match(rest)
            headresult = head.pre_match
            rest=head.post_match
            tail = endWith.match(rest)
            last = tail.post_match
            result = headresult + "../../../screenshots/"+last
            new_str << result
        rescue
            new_str <<str
        end
        
        return new_str
    end
    def getTwistReportPath
        currentpath = Pathname.new(File.dirname(__FILE__)).realpath
        filePath = "target/twistreports/html/junit-noframes.html"
        cpath_re = /helper/
        result= cpath_re.match(currentpath)
        @twistreportpath = result.pre_match + filePath
        puts "The name of twist report is "+@twistreportpath
    end 
    def fixpath
        getTwistReportPath
        if(File.exist?(@twistreportpath))
            file=File.open(@twistreportpath,'r')
            startWith = /file:\/\/\//
            puts ENV['OS']
            begin
              if(ENV['OS'].include?("Windows") )
                  startWith = /file:\/\//
              end
            rescue
              puts "Can not detect the type of OS"
            end
            endWith = /screenshots\//
            new_str = ""
            puts "Reading the report and replace the path..."
            while (lines=file.gets)
              new_str <<replace(startWith, endWith, lines)
            end
            begin
                puts "Rewriting the report..."
                aFile = File.open(@twistreportpath, "w")
                aFile.puts new_str
                aFile.close
            rescue
                puts "Can not rewrite" + @twistreportpath
            end
        else
           puts "NO found " + @twistreportpath
        end 
    end    
end
