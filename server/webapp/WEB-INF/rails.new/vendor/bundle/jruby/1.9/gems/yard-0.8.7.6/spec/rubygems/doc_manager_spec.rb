require File.dirname(__FILE__) + '/../spec_helper'
require File.join(YARD::ROOT, 'rubygems_plugin')
require 'fileutils'

describe Gem::DocManager do
  before do
    # Ensure filesystem integrity
    FileUtils.stub(:mkdir_p)
    FileUtils.stub(:rm_rf)
    Dir.stub(:chdir)

    YARD::CLI::Yardoc.stub(:run)

    @spec_file = File.join(YARD::ROOT, '..', 'yard.gemspec')
    @spec = Gem::SourceIndex.load_specification(@spec_file)
    @spec.has_yardoc = false # no yardoc docs for now
    @yardopts = File.join(@spec.full_gem_path, '.yardopts')
    @doc = Gem::DocManager.new(@spec)
    @doc.stub(:install_ri_yard_orig)
    @doc.stub(:install_rdoc_yard_orig)
  end

  def runs; YARD::CLI::Yardoc.should_receive(:run) end

  describe '.load_yardoc' do
    it "should properly load YARD" do
      Gem::DocManager.should_receive(:require) do |path|
        File.expand_path(path).should == YARD::ROOT + '/yard'
      end
      Gem::DocManager.load_yardoc
    end
  end

  describe '#install_ri_yard' do
    def install
      msg = "Building YARD (yri) index for #{@spec.full_name}..."
      @doc.should_receive(:say).with(msg)
      @doc.install_ri_yard
    end

    it "should pass --quiet to all documentation" do
      runs.with('-c', '-n', '--quiet', 'lib')
      install
    end

    it "should pass extra_rdoc_files to documentation" do
      @spec.extra_rdoc_files = %w(README LICENSE)
      runs.with('-c', '-n', '--quiet', 'lib', '-', 'README', 'LICENSE')
      install
    end

    it "should add --backtrace if Gem.configuration.backtrace" do
      Gem.configuration.backtrace = true
      runs.with('-c', '-n', '--quiet', '--backtrace', 'lib')
      install
      Gem.configuration.backtrace = false
    end

    it "should add require_paths if there is no .yardopts" do
      File.should_receive(:file?).with(@yardopts).and_return(true)
      runs.with('-c', '-n', '--quiet')
      install
    end

    it "should add extra_rdoc_files if there is no .yardopts" do
      @spec.extra_rdoc_files = %w(README LICENSE)
      File.should_receive(:file?).with(@yardopts).and_return(true)
      runs.with('-c', '-n', '--quiet')
      install
    end

    it "should switch to directory before running command" do
      old = Dir.pwd
      Dir.should_receive(:chdir).with(@spec.full_gem_path)
      Dir.should_receive(:chdir).with(old)
      install
    end

    it "should ensure that directory is switched back at end of command in failure" do
      old = Dir.pwd
      Dir.should_receive(:chdir).with(@spec.full_gem_path)
      Dir.should_receive(:chdir).with(old)
      @doc.ui.errs.should_receive(:puts).with(/ERROR:\s*While generating documentation/)
      @doc.ui.errs.should_receive(:puts).with(/MESSAGE:\s*foo/)
      @doc.ui.errs.should_receive(:puts).with(/YARDOC args:\s*-c -n --quiet lib/)
      @doc.ui.errs.should_receive(:puts).with("(continuing with the rest of the installation)")
      YARD::CLI::Yardoc.should_receive(:run).and_raise(RuntimeError.new("foo"))
      install
    end

    it "should handle permission errors" do
      YARD::CLI::Yardoc.should_receive(:run).and_raise(Errno::EACCES.new("- dir"))
      lambda { install }.should raise_error(Gem::FilePermissionError)
    end
  end

  describe '#install_rdoc_yard' do
    def install
      msg = "Installing YARD documentation for #{@spec.full_name}..."
      @doc.should_receive(:say).with(msg)
      @doc.install_rdoc_yard
    end

    it "should add -o outdir when generating docs" do
      File.should_receive(:file?).with(@yardopts).and_return(true)
      @spec.has_yardoc = true
      doc_dir = File.join(@doc.instance_variable_get("@doc_dir"), 'rdoc')
      runs.with('-o', doc_dir, '--quiet')
      install
    end
  end
end if Gem::VERSION < '2.0.0'
