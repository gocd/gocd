require 'spec_helper'

describe "Jasmine command line tool" do
  context "when rake has not been required yet" do
    before :each do
      temp_dir_before
      Dir::chdir @tmp
    end

    after :each do
      temp_dir_after
    end

    it "should append to an existing Rakefile" do
      FileUtils.cp("#{@old_dir}/spec/fixture/Rakefile", @tmp)
      output = capture_stdout { Jasmine::CommandLineTool.new.process ["init"] }
      output.should =~ /Jasmine has been installed with example specs./
    end
  end
end
