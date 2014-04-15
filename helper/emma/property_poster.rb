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

require "rexml/document"
require 'net/http'
include REXML  # so that we don't have to prefix everything with REXML

class PropertyPoster

    def postcoverage
        @serverurl= ENV['CRUISE_SERVER_URL']
        @pipeline= ENV['CRUISE_PIPELINE_NAME']
        @stage= ENV['CRUISE_STAGE_NAME']
        @job= ENV['CRUISE_JOB_NAME']
        @label= ENV['CRUISE_PIPELINE_LABEL']
        emmafile = "../../target/emma/coverage.xml";
        @emmafilepath = Pathname.new(File.dirname(__FILE__)).realpath + emmafile
        puts @emmafilepath
        file = File.new( @emmafilepath )
        doc = REXML::Document.new file
        coverages = doc.root.elements.to_a("data/all/coverage")
        coverages.each do |coverage| 
            url = URI.parse("http://10.18.7.51:8153/go/properties/"+@pipeline+"/"+@label+"/"+@stage+"/"+@job+"/coverage_"+coverage.attributes["type"].to_s.split( ",")[0])
            req = Net::HTTP::Post.new(url.path)
            req.basic_auth 'jez', 'badger'
            req.set_form_data({'value'=>coverage.attributes["value"].split("%")[0]}, ';')
            puts url
            res = Net::HTTP.new(url.host, url.port).start {|http| http.request(req) }
            case res
            when Net::HTTPSuccess, Net::HTTPRedirection
                puts 'done!'
            else
                res.error!
            end
        end
     end
end