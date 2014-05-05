# Copyright (c) 2011, Diego Souza
# All rights reserved.
#
# Redistribution and use in source and binary forms, with or without
# modification, are permitted provided that the following conditions are met:
#
#   * Redistributions of source code must retain the above copyright notice,
#     this list of conditions and the following disclaimer.
#   * Redistributions in binary form must reproduce the above copyright notice,
#     this list of conditions and the following disclaimer in the documentation
#     and/or other materials provided with the distribution.
#   * Neither the name of the <ORGANIZATION> nor the names of its contributors
#     may be used to endorse or promote products derived from this software
#     without specific prior written permission.
#
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
# ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
# WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
# DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
# FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
# DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
# SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
# CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
# OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
# OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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

require "time"
require "spec/runner/formatter/base_text_formatter"

class JUnitFormatter < Spec::Runner::Formatter::BaseTextFormatter

  attr_reader :test_results, :output

  def initialize(_, output)
    super(_, output)
    @output = File.open(output, 'w')
    @test_results = { :failures => [], :successes => [] }
  end

  def example_passed(example)
    super(example)
    @test_results[:successes].push(example)
  end

  def example_pending(example)
    self.example_failed(example)
  end

  def example_failed(example, *ignore)
    super(example, *ignore)
    @test_results[:failures].push(example)
  end

  def read_failure(t)
    message = "no one's grand daddy knows why i failed!"
    message += "\n"
    message += t.location
    return(message)
  end

  def dump_summary(duration, example_count, failure_count, pending_count)
    super(duration, example_count, failure_count, pending_count)
    output.puts("<?xml version=\"1.0\" encoding=\"utf-8\" ?>")
    output.puts("<testsuite errors=\"0\" failures=\"#{failure_count+pending_count}\" tests=\"#{example_count}\" time=\"#{duration}\" timestamp=\"#{Time.now.iso8601}\" name=\"rspec\">")
    output.puts("  <properties />")
    @test_results[:successes].each do |t|
      runtime     = 5
      description = _xml_escape(t.description)
      file_path   = _xml_escape(t.location)
      output.puts("  <testcase classname=\"#{file_path}\" name=\"#{description}\" time=\"#{runtime}\" />")
    end
    @test_results[:failures].each do |t|
      description = _xml_escape(t.description)
      file_path   = _xml_escape(t.location)
      runtime     = 10
      output.puts("  <testcase classname=\"#{file_path}\" name=\"#{description}\" time=\"#{runtime}\">")
      output.puts("    <failure message=\"failure\" type=\"failure\">")
      output.puts("<![CDATA[ #{read_failure(t)} ]]>")
      output.puts("    </failure>")
      output.puts("  </testcase>")
    end
    output.puts("</testsuite>")
  ensure
    output.close()
  end

  def _xml_escape(x)
    x.gsub("&", "&amp;").
      gsub("\"", "&quot;").
      gsub(">", "&gt;").
      gsub("<", "&lt;")
  end
end
