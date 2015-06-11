require "spec_helper"
require 'rspec/core/drb_command_line'

describe "::DRbCommandLine", :type => :drb, :unless => RUBY_PLATFORM == 'java' do
  let(:config) { RSpec::Core::Configuration.new }
  let(:out)    { StringIO.new }
  let(:err)    { StringIO.new }

  include_context "spec files"

  def command_line(*args)
    RSpec::Core::DRbCommandLine.new(config_options(*args))
  end

  def config_options(*args)
    options = RSpec::Core::ConfigurationOptions.new(args)
    options.parse_options
    options
  end

  context "without server running" do
    it "raises an error" do
      expect { command_line.run(err, out) }.to raise_error(DRb::DRbConnError)
    end
  end

  describe "--drb-port" do
    def with_RSPEC_DRB_set_to(val)
      with_env_vars('RSPEC_DRB' => val) { yield }
    end

    context "without RSPEC_DRB environment variable set" do
      it "defaults to 8989" do
        with_RSPEC_DRB_set_to(nil) do
          expect(command_line.drb_port).to eq(8989)
        end
      end

      it "sets the DRb port" do
        with_RSPEC_DRB_set_to(nil) do
          expect(command_line("--drb-port", "1234").drb_port).to eq(1234)
          expect(command_line("--drb-port", "5678").drb_port).to eq(5678)
        end
      end
    end

    context "with RSPEC_DRB environment variable set" do
      context "without config variable set" do
        it "uses RSPEC_DRB value" do
          with_RSPEC_DRB_set_to('9000') do
            expect(command_line.drb_port).to eq("9000")
          end
        end
      end

      context "and config variable set" do
        it "uses configured value" do
          with_RSPEC_DRB_set_to('9000') do
            expect(command_line(*%w[--drb-port 5678]).drb_port).to eq(5678)
          end
        end
      end
    end
  end

  context "with server running" do
    class SimpleDRbSpecServer
      def self.run(argv, err, out)
        options = RSpec::Core::ConfigurationOptions.new(argv)
        options.parse_options
        RSpec::Core::CommandLine.new(options, RSpec::Core::Configuration.new).run(err, out)
      end
    end

    before(:all) do
      @drb_port = '8990'
      @drb_example_file_counter = 0
      DRb::start_service("druby://127.0.0.1:#{@drb_port}", SimpleDRbSpecServer)
    end

    after(:all) do
      DRb::stop_service
    end

    it "returns 0 if spec passes" do
      result = command_line("--drb-port", @drb_port, passing_spec_filename).run(err, out)
      expect(result).to be(0)
    end

    it "returns 1 if spec fails" do
      result = command_line("--drb-port", @drb_port, failing_spec_filename).run(err, out)
      expect(result).to be(1)
    end

    it "outputs colorized text when running with --colour option" do
      pending "figure out a way to tell the output to say it's tty"
      command_line(failing_spec_filename, "--color", "--drb-port", @drb_port).run(err, out)
      out.rewind
      expect(out.read).to match(/\e\[31m/m)
    end
  end
end
