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

module FixtureTestHelpers

# Modify HTML to remove time dependent stuff so we can compare HTML files more reliably
  def extract_test(xml)
    xml.gsub!(/[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}/, "UUID")
    xml.gsub!(/\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(([-+]\d{2}:\d{2})|Z)/, "REPLACED_DATE")
    xml.gsub!(/\w{3} \w{3} \d{2} \d{2}:\d{2}:\d{2} \w{3} \d{4}/, "REPLACED_DATE_TIME")
    xml.gsub!(/\w{3} \w{3} \d{2} \d{2}:\d{2}:\d{2} \w{3}\+\d{2}:\d{2} \d{4}/, "REPLACED_DATE_TIME")
    xml.gsub!(/\d{13}/, "REPLACED_DATE_TIME_MILLIS")
    xml.gsub!(/[\d\w\s]*?\sminute[s]*\sago\s*/, "REPLACED_RELATIVE_TIME")
    xml.gsub!(/[\d\w\s]*?\shour[s]*\sago\s*/, "REPLACED_RELATIVE_TIME")
    xml.gsub!(/[\d\w\s]*?\sday[s]*\sago\s*/, "REPLACED_RELATIVE_TIME")
    xml.gsub!(/\s+/m, " ")
    xml.gsub!(/Windows 2003/, "OPERATING SYSTEM")
    xml.gsub!(/Linux/, "OPERATING SYSTEM")
    xml.gsub!(/SunOS/, "OPERATING SYSTEM")
    xml.gsub!(/Mac OS X/, "OPERATING SYSTEM")
    xml.gsub!(/<script.*?<\/script>/m, "")

    resp_doc = REXML::Document.new(xml)
    generated_content = resp_doc.root.elements["//div[@class='under_test']"]

    formatter = REXML::Formatters::Pretty.new
    formatter.compact = true
    out = ""
    formatter.write(generated_content, out)
    out
  end
end