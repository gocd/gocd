# -*- encoding : utf-8 -*-
# 
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

require "rspec/core/formatters/base_formatter"

class TapFormatter < RSpec::Core::Formatters::BaseFormatter

  attr_reader :total
    $VERBOSE = nil
  OK     = 'ok'
  NOT_OK = 'not ok'
  TODO   = '# TODO '
  SKIP   = '# SKIP '

  def initialize(output)
    super(output)      
    @total = 0
  end

  def start(example_count)
      super(example_count)
      output.puts("TAP version 13")
      output.puts("1.." + example_count.to_s)

  end

  def example_passed(example)
    super(example)
    tap_example_output(OK, example)
  end

  def example_pending(example)
    super(example)
    tap_example_output(NOT_OK, example, SKIP)
  end

  def example_failed(example)
    super(example)
    tap_example_output(NOT_OK, example)
    output.puts("    ---")
    my_exception = example.exception.to_s
    my_exception.gsub! /"/, ''
    output.puts("     #{my_exception} ")
    output.puts("     ...")
  end

  def dump_summary(duration, example_count, failure_count, pending_count)
    super(duration, example_count, failure_count, pending_count)
  end

  private
  def tap_example_output(ok, example, modifier='')
    @total += 1
    output.puts("#{ok} #{@total} - #{modifier}#{example.metadata[:full_description]}")
  end

end
