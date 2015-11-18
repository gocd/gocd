require 'spec_helper'
require 'fileutils'


describe "Phantom JS Runner performance/integration suite", :performance => true do
  before :each do
    temp_dir_before
    Dir::chdir @tmp
  end

  after :each do
    temp_dir_after
  end

  it 'handles deeply nested/large test suites' do
    Jasmine::CommandLineTool.new.process ['init']

    FileUtils.cp(File.join(@root, 'spec', 'fixture', 'large_test_suite_spec.js'), File.join(@tmp, 'spec', 'javascripts'))

    ci_output = `rake --trace jasmine:ci`
    ci_output.should =~ (/40000 specs, 20000 failures/)
  end
end
