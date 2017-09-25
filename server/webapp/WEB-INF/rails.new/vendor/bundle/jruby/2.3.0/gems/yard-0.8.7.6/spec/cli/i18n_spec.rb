require File.dirname(__FILE__) + '/../spec_helper'

describe YARD::CLI::I18n do
  before do
    @i18n = YARD::CLI::I18n.new
    @i18n.use_document_file = false
    @i18n.use_yardopts_file = false
    output_path = File.expand_path(@i18n.options.serializer.basepath)
    File.stub!(:open!).with(output_path, "wb")
    YARD.stub!(:parse)
  end

  describe 'Defaults' do
    before do
      @i18n = YARD::CLI::I18n.new
      @i18n.stub!(:yardopts).and_return([])
      @i18n.stub!(:support_rdoc_document_file!).and_return([])
      @i18n.parse_arguments
    end

    it "should read .yardopts by default" do
      @i18n.use_yardopts_file.should == true
    end

    it "should use {lib,app}/**/*.rb and ext/**/*.c as default file glob" do
      @i18n.files.should == ['{lib,app}/**/*.rb', 'ext/**/*.c']
    end

    it "should only show public visibility by default" do
      @i18n.visibilities.should == [:public]
    end
  end

  describe 'General options' do
    def self.should_accept(*args, &block)
      @counter ||= 0
      @counter += 1
      counter = @counter
      args.each do |arg|
        define_method("test_options_#{@counter}", &block)
        it("should accept #{arg}") { send("test_options_#{counter}", arg) }
      end
    end

    should_accept('--yardopts') do |arg|
      @i18n = YARD::CLI::I18n.new
      @i18n.use_document_file = false
      @i18n.should_receive(:yardopts).at_least(1).times.and_return([])
      @i18n.parse_arguments(arg)
      @i18n.use_yardopts_file.should == true
      @i18n.parse_arguments('--no-yardopts', arg)
      @i18n.use_yardopts_file.should == true
    end

    should_accept('--yardopts with filename') do |arg|
      @i18n = YARD::CLI::I18n.new
      File.should_receive(:read_binary).with('.yardopts_i18n').and_return('')
      @i18n.use_document_file = false
      @i18n.parse_arguments('--yardopts', '.yardopts_i18n')
      @i18n.use_yardopts_file.should == true
      @i18n.options_file.should == '.yardopts_i18n'
    end

    should_accept('--no-yardopts') do |arg|
      @i18n = YARD::CLI::I18n.new
      @i18n.use_document_file = false
      @i18n.should_not_receive(:yardopts)
      @i18n.parse_arguments(arg)
      @i18n.use_yardopts_file.should == false
      @i18n.parse_arguments('--yardopts', arg)
      @i18n.use_yardopts_file.should == false
    end

    should_accept('--exclude') do |arg|
      YARD.should_receive(:parse).with(['a'], ['nota', 'b'])
      @i18n.run(arg, 'nota', arg, 'b', 'a')
    end
  end

  describe '.yardopts handling' do
    before do
      @i18n.use_yardopts_file = true
    end

    it "should search for and use yardopts file specified by #options_file" do
      File.should_receive(:read_binary).with("test").and_return("-o \n\nMYPATH\nFILE1 FILE2")
      @i18n.use_document_file = false
      @i18n.options_file = "test"
      File.should_receive(:open!).with(File.expand_path("MYPATH"), "wb")
      @i18n.run
      @i18n.files.should == ["FILE1", "FILE2"]
    end
  end

  describe '#run' do
    it "should parse_arguments if run() is called" do
      @i18n.should_receive(:parse_arguments)
      @i18n.run
    end

    it "should parse_arguments if run(arg1, arg2, ...) is called" do
      @i18n.should_receive(:parse_arguments)
      @i18n.run('--private', '-p', 'foo')
    end

    it "should not parse_arguments if run(nil) is called" do
      @i18n.should_not_receive(:parse_arguments)
      @i18n.run(nil)
    end
  end
end
