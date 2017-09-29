require "spec_helper"
require "stringio"
require 'tmpdir'

module RSpec::Core
  describe Runner do

    let(:out)    { StringIO.new         }
    let(:err)    { StringIO.new         }
    let(:config) { RSpec::configuration }
    let(:world)  { RSpec::world         }

    before { config.stub :run_hook }

    it "configures streams before command line options" do
      config.stub(:reporter => double.as_null_object)
      config.stub :load_spec_files

      # this is necessary to ensure that color works correctly on windows
      config.should_receive(:error_stream=).ordered
      config.should_receive(:output_stream=).ordered
      config.should_receive(:force).at_least(:once).ordered

      runner = build_runner
      runner.run err, out
    end

    it "assigns submitted ConfigurationOptions to @options" do
      config_options = ConfigurationOptions.new(%w[--color])
      runner   = Runner.new(config_options)
      expect(runner.instance_eval { @options }).to be(config_options)
    end

    describe "#run" do
      context "running files" do
        include_context "spec files"

        it "returns 0 if spec passes" do
          runner = build_runner passing_spec_filename
          expect(runner.run(err, out)).to eq 0
        end

        it "returns 1 if spec fails" do
          runner = build_runner failing_spec_filename
          expect(runner.run(err, out)).to eq 1
        end

        it "returns 2 if spec fails and --failure-exit-code is 2" do
          runner = build_runner failing_spec_filename, "--failure-exit-code", "2"
          expect(runner.run(err, out)).to eq 2
        end
      end

      context "running hooks" do
        before { config.stub :load_spec_files }

        it "runs before suite hooks" do
          config.should_receive(:run_hook).with(:before, :suite)
          runner = build_runner
          runner.run err, out
        end

        it "runs after suite hooks" do
          config.should_receive(:run_hook).with(:after, :suite)
          runner = build_runner
          runner.run err, out
        end

        it "runs after suite hooks even after an error" do
          config.should_receive(:run_hook).with(:before, :suite).and_raise "this error"
          config.should_receive(:run_hook).with(:after , :suite)
          expect do
            runner = build_runner
            runner.run err, out
          end.to raise_error
        end
      end
    end

    describe "#run with custom output" do
      before { config.stub :files_to_run => [] }

      let(:output_file) { File.new("#{Dir.tmpdir}/runner_spec_output.txt", 'w') }

      it "doesn't override output_stream" do
        config.output_stream = output_file
        runner = build_runner
        runner.run err, out
        expect(runner.instance_eval { @configuration.output_stream }).to eq output_file
      end
    end

    def build_runner *args
      Runner.new build_config_options(*args)
    end

    def build_config_options *args
      options = ConfigurationOptions.new args
      options.parse_options
      options
    end
  end

  describe CommandLine do
    it 'is a subclass of `Runner` so that it inherits the same behavior' do
      expect(CommandLine.superclass).to be(Runner)
    end

    it 'prints a deprecation when instantiated' do
      expect_deprecation_with_call_site(__FILE__, __LINE__ + 1, /RSpec::Core::CommandLine/)
      CommandLine.new(ConfigurationOptions.new([]))
    end

    context "when given an array as the first arg" do
      it 'parses it into configuration options' do
        cl = CommandLine.new(%w[ --require foo ])
        options = cl.instance_eval { @options }
        expect(options.options).to include(:requires => ['foo'])
      end

      it 'issues a deprecation about the array arg' do
        expect_deprecation_with_call_site(__FILE__, __LINE__ + 1, /array/)
        CommandLine.new(%w[ --require foo ])
      end
    end
  end
end
