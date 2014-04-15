require "spec_helper"
require "rspec/core/rake_task"
require 'tempfile'

module RSpec::Core
  describe RakeTask do
    let(:task) { RakeTask.new }

    def ruby
      FileUtils::RUBY
    end

    def with_rcov
      task.rcov = true
      yield
    end

    def spec_command
      task.__send__(:spec_command)
    end

    context "default" do
      it "renders rspec" do
        spec_command.should =~ /^#{ruby} -S rspec/
      end
    end

    context "with rcov" do
      it "renders rcov" do
          with_rcov do
            spec_command.should =~ /^#{ruby} -S rcov/
          end
        end
    end

    context "with ruby options" do
      it "renders them before -S" do
          task.ruby_opts = "-w"
          spec_command.should =~ /^#{ruby} -w -S rspec/
      end
    end

    context "with rcov_opts" do
      context "with rcov=false (default)" do
        it "does not add the rcov options to the command" do
          task.rcov_opts = '--exclude "mocks"'
          spec_command.should_not =~ /--exclude "mocks"/
        end
      end

      context "with rcov=true" do
        it "renders them after rcov" do
          task.rcov = true
          task.rcov_opts = '--exclude "mocks"'
          spec_command.should =~ /rcov.*--exclude "mocks"/
        end

        it "ensures that -Ispec:lib is in the resulting command" do
          task.rcov = true
          task.rcov_opts = '--exclude "mocks"'
          spec_command.should =~ /rcov.*-Ispec:lib/
        end
      end
    end

    context "with rspec_opts" do
      context "with rcov=true" do
        it "adds the rspec_opts after the rcov_opts and files" do
          task.stub(:files_to_run) { "this.rb that.rb" }
          task.rcov = true
          task.rspec_opts = "-Ifoo"
          spec_command.should =~ /this.rb that.rb -- -Ifoo/
        end
      end
      context "with rcov=false (default)" do
        it "adds the rspec_opts" do
          task.rspec_opts = "-Ifoo"
          spec_command.should =~ /rspec.*-Ifoo/
        end
      end
    end

    context "with SPEC=path/to/file" do
      before do
        @orig_spec = ENV["SPEC"]
        ENV["SPEC"] = "path/to/file"
      end

      after do
        ENV["SPEC"] = @orig_spec
      end

      it "sets files to run" do
        task.__send__(:files_to_run).should eq(["path/to/file"])
      end
    end

    context "with paths with quotes" do
      it "escapes the quotes" do
        task = RakeTask.new do |t|
          t.pattern = File.join(Dir.tmpdir, "*spec.rb")
        end
        ["first_spec.rb", "second_\"spec.rb", "third_\'spec.rb"].each do |file_name|
          FileUtils.touch(File.join(Dir.tmpdir, file_name))
        end
        task.__send__(:files_to_run).sort.should eq([
          File.join(Dir.tmpdir, "first_spec.rb"),
          File.join(Dir.tmpdir, "second_\\\"spec.rb"),
          File.join(Dir.tmpdir, "third_\\\'spec.rb")
        ])
      end
    end

    context "with paths including symlinked directories" do
      it "finds the files" do
        project_dir = File.join(Dir.tmpdir, "project")
        FileUtils.rm_rf project_dir

        foos_dir = File.join(project_dir, "spec/foos")
        FileUtils.mkdir_p foos_dir
        FileUtils.touch(File.join(foos_dir, "foo_spec.rb"))

        bars_dir = File.join(Dir.tmpdir, "shared/spec/bars")
        FileUtils.mkdir_p bars_dir
        FileUtils.touch(File.join(bars_dir, "bar_spec.rb"))

        FileUtils.ln_s bars_dir, File.join(project_dir, "spec/bars")

        FileUtils.cd(project_dir) do
          RakeTask.new.__send__(:files_to_run).sort.should eq([
            "./spec/bars/bar_spec.rb",
            "./spec/foos/foo_spec.rb"
          ])
        end
      end
    end
  end
end
