require File.dirname(__FILE__) + '/../../spec_helper.rb'
require File.dirname(__FILE__) + '/../../../lib/spec/rake/spectask.rb'

module Spec
  module Rake

    class MockTask
      class << self
        attr_accessor :last_instance, :last_cmd
      end

      def self.tasks
        @tasks ||= {}
      end
      
      def self.reset_tasks
        @tasks = {}
      end
      
      def self.task(name)
        tasks[name]
      end
      
      def self.register_task(name, block)
        tasks[name] = block
      end

      def initialize(name, &block)
        MockTask.register_task(name, block)
        MockTask.last_instance = block
      end
      
      def self.create_task(name, &block)
        new(name, &block)
      end
    end

    class SpecTask
      def task(name, &block)
        MockTask.create_task(name, &block)
      end

      def system(cmd)
        MockTask.last_cmd = cmd
        true
      end

      def default_ruby_path
        RUBY
      end
    end
    
    describe SpecTask do

      before(:each) do
        MockTask.reset_tasks
      end

      it "should execute rake's ruby path by default" do
        task = SpecTask.new
        MockTask.last_instance.call
        MockTask.last_cmd.should match(/^#{task.default_ruby_path} /)
      end

      it "should execute the command with system if ruby_cmd is specified" do
        task = SpecTask.new {|t| t.ruby_cmd = "path_to_multiruby"}
        task.should_receive(:system).and_return(true)
        MockTask.last_instance.call
      end

      it "should execute the ruby_cmd path if specified" do
        SpecTask.new {|t| t.ruby_cmd = "path_to_multiruby"}
        MockTask.last_instance.call
        MockTask.last_cmd.should match(/^path_to_multiruby /)
      end
      
      it "should produce a deprecation warning if the out option is used" do
        SpecTask.new {|t| t.out = "somewhere_over_the_rainbow"}
        STDERR.should_receive(:puts).with("The Spec::Rake::SpecTask#out attribute is DEPRECATED and will be removed in a future version. Use --format FORMAT:WHERE instead.")
        MockTask.last_instance.call
      end
      
      it "should produce an error if failure_message is set and the command fails" do
        task = SpecTask.new {|t| t.failure_message = "oops"; t.fail_on_error = false}
        STDERR.should_receive(:puts).with("oops")
        task.stub(:system).and_return(false)
        MockTask.last_instance.call
      end
      
      it "should raise if fail_on_error is set and the command fails" do
        task = SpecTask.new
        task.stub(:system).and_return(false)
        lambda {MockTask.last_instance.call}.should raise_error
      end
      
      it "should not raise if fail_on_error is not set and the command fails" do
        task = SpecTask.new {|t| t.fail_on_error = false}
        task.stub(:system).and_return(false)
        lambda {MockTask.last_instance.call}.should_not raise_error
      end
      
      context "with ENV['SPEC'] set" do
        before(:each) do
          @orig_env_spec = ENV['SPEC']
          ENV['SPEC'] = 'foo.rb'
        end
        after(:each) do
          ENV['SPEC'] = @orig_env_spec
        end
        it "should use the provided file list" do
          task = SpecTask.new
          task.spec_file_list.should == ["foo.rb"]
        end
      end
      
      context "with the rcov option" do
        
        it "should create a clobber_rcov task" do
          MockTask.stub!(:create_task)
          MockTask.should_receive(:create_task).with(:clobber_rcov)
          SpecTask.new(:rcov) {|t| t.rcov = true}
        end

        it "should setup the clobber_rcov task to remove the rcov directory" do
          task = SpecTask.new(:rcov) {|t| t.rcov = true; t.rcov_dir = "path_to_rcov_directory"}
          task.should_receive(:rm_r).with("path_to_rcov_directory")
          MockTask.task(:clobber_rcov).call
        end

        it "should make the clobber task depend on clobber_rcov" do
          MockTask.stub!(:create_task)
          MockTask.should_receive(:create_task).with(:clobber => [:clobber_rcov])
          SpecTask.new(:rcov) {|t| t.rcov = true}
        end

        it "should make the rcov task depend on clobber_rcov" do
          MockTask.stub!(:create_task)
          MockTask.should_receive(:create_task).with(:rcov => :clobber_rcov)
          SpecTask.new(:rcov) {|t| t.rcov = true}
        end
        
        it "creates an rcov options list" do
          MockTask.stub!(:create_task)
          task = SpecTask.new(:rcov) {|t| t.rcov = true, t.rcov_opts = ['a','b']}
          task.rcov_option_list.should == "a b"
        end
      end
    end
  end
end