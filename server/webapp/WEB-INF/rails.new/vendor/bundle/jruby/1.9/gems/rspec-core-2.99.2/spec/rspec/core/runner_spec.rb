require 'spec_helper'
require 'rspec/core/drb_command_line'

module RSpec::Core
  describe Runner do
    describe 'at_exit' do
      it 'sets an at_exit hook if none is already set' do
        RSpec::Core::Runner.stub(:installed_at_exit?).and_return(false)
        RSpec::Core::Runner.stub(:running_in_drb?).and_return(false)
        RSpec::Core::Runner.stub(:at_exit_hook_disabled?).and_return(false)
        RSpec::Core::Runner.stub(:run).and_return(-1)
        RSpec::Core::Runner.should_receive(:at_exit)
        RSpec::Core::Runner.autorun
      end

      it 'does not set the at_exit hook if it is already set' do
        RSpec::Core::Runner.stub(:installed_at_exit?).and_return(true)
        RSpec::Core::Runner.stub(:running_in_drb?).and_return(false)
        RSpec::Core::Runner.stub(:at_exit_hook_disabled?).and_return(false)
        RSpec::Core::Runner.should_receive(:at_exit).never
        RSpec::Core::Runner.autorun
      end
    end

    describe "#run" do
      let(:err) { StringIO.new }
      let(:out) { StringIO.new }

      after { RSpec.resets_required = 0 }
      before { RSpec.resets_required = 0 }

      it "tells RSpec to reset" do
        RSpec.configuration.stub(:files_to_run => [])
        RSpec.should_receive(:internal_reset)
        RSpec::Core::Runner.run([], err, out)
      end

      it "does not issue a deprecation warning at the end of the first run" do
        RSpec.configuration.stub(:files_to_run => [])
        RSpec.should_receive(:internal_reset)
        allow(RSpec.configuration).to receive(:deprecation_stream).and_return(err)
        RSpec::Core::Runner.run([], err, out)
        expect(err.string).to eq("")
      end

      it "issues a deprecation if warn is invoked twice and reset is not called manually" do
        RSpec.configuration.stub(:files_to_run => [])
        RSpec::Core::Runner.run([], err, out)
        RSpec.configuration.stub(:files_to_run => [])
        allow(RSpec.configuration).to receive(:deprecation_stream).and_return(err)
        RSpec::Core::Runner.run([], err, out)
        expect(err.string).to match(/no longer implicitly/)
      end

      context "when the user manually invokes reset" do
        after { RSpec.instance_variable_set(:@reset_called_by_user, false) }

        it "does not issue a deprecation warning" do
          RSpec.configuration.stub(:files_to_run => [])
          RSpec::Core::Runner.run([], err, out)
          RSpec.configuration.stub(:files_to_run => [])
          RSpec.reset
          RSpec.configuration.stub(:files_to_run => [])
          allow(RSpec.configuration).to receive(:deprecation_stream).and_return(err)
          RSpec::Core::Runner.run([], err, out)
          expect(err.string).to eq("")
        end
      end

      context "with --drb or -X" do

        before(:each) do
          @options = RSpec::Core::ConfigurationOptions.new(%w[--drb --drb-port 8181 --color])
          RSpec::Core::ConfigurationOptions.stub(:new) { @options }
        end

        def run_specs
          RSpec::Core::Runner.run(%w[ --drb ], err, out)
        end

        context 'and a DRb server is running' do
          it "builds a DRbCommandLine and runs the specs" do
            drb_proxy = double(RSpec::Core::DRbCommandLine, :run => true)
            drb_proxy.should_receive(:run).with(err, out)

            RSpec::Core::DRbCommandLine.should_receive(:new).and_return(drb_proxy)

            run_specs
          end
        end

        context 'and a DRb server is not running' do
          before(:each) do
            RSpec::Core::DRbCommandLine.should_receive(:new).and_raise(DRb::DRbConnError)
          end

          it "outputs a message" do
            RSpec.configuration.stub(:files_to_run) { [] }
            err.should_receive(:puts).with(
              "No DRb server is running. Running in local process instead ..."
            )
            run_specs
          end

          it "builds a Runner and runs the specs" do
            process_proxy = double(RSpec::Core::Runner, :run => 0)
            process_proxy.should_receive(:run).with(err, out)

            RSpec::Core::Runner.should_receive(:new).and_return(process_proxy)

            run_specs
          end
        end
      end
    end
  end
end
