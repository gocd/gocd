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

require "stringio"
require File.expand_path(File.dirname(__FILE__) + "/../spec_helper.rb")

describe TapFormatter do

  it "should initialize the counter to 0" do
    TapFormatter.new(StringIO.new).total.should eql(0)
  end

  describe "example_passed" do

    it "should increment the counter and use the full_description attribute" do
      example = mock("example")
      example.should_receive(:metadata).and_return({:full_description => "foobar"})

      output = StringIO.new
      f = TapFormatter.new(output)
      f.example_passed(example)

      f.total.should eql(1)
      output.string.should == "ok 1 - foobar\n"
    end

  end

  describe "example_failed" do

    it "should increment the counter and use the full_description attribute" do
      example = mock("example")
      example.stub(:exception) { "exception message" }
      example.should_receive(:metadata).and_return({:full_description => "foobar"})

      output = StringIO.new
      f = TapFormatter.new(output)
      f.example_failed(example)
      
      f.total.should eql(1)
      output.string.should == <<-EOF
not ok 1 - foobar
    ---
     exception message 
     ...
      EOF
    end
  end

  describe "example_pending" do

    it "should do the same as example_failed with SKIP comment" do
      example = mock("example")
      example.stub(:exception) { "exception message" }
      example.should_receive(:metadata).and_return({:full_description => "foobar"})
      
      output = StringIO.new
      f = TapFormatter.new(output)
      f.example_pending(example)
      
      f.total.should eql(1)
      output.string.should == "not ok 1 - # SKIP foobar\n"
    end

  end

  describe "dump_summary" do

    it "should print the number of tests if there were tests" do
      example = mock("example")
      example.stub(:exception) { "exception message" }
      example.should_receive(:metadata).and_return({:full_description => "foobar"})
      example.should_receive(:metadata).and_return({:full_description => "foobar"})
      example.should_receive(:metadata).and_return({:full_description => "foobar"})
      
      output = StringIO.new
      f = TapFormatter.new(output)
      f.example_passed(example)
      f.example_failed(example)
      f.example_pending(example)
      f.dump_summary(0.1, 3, 1, 1)

      output.string.should == <<-EOF
ok 1 - foobar
not ok 2 - foobar
    ---
     exception message 
     ...
not ok 3 - # SKIP foobar
      EOF
    end

    it "should print nothing if there were not tests" do
      f = TapFormatter.new(@output)
      f.dump_summary(0, 0, 0, 0)
    end

  end

end
