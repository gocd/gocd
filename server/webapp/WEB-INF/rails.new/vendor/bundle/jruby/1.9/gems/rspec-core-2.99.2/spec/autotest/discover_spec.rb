require "spec_helper"

describe "autotest/discover.rb" do
  before { File.stub(:exist?).and_call_original }

  context "with ./.rspec present" do
    before { File.stub(:exist?).with("./.rspec") { true } }

    context "when RSpec::Autotest is defined" do
      before { stub_const "RSpec::Autotest", Module.new }

      it "does not add 'rspec2' to the list of discoveries" do
        Autotest.should_not_receive(:add_discovery)
        load File.expand_path("../../../lib/autotest/discover.rb", __FILE__)
      end
    end

    context "when RSpec::Autotest is not defined" do
      before { hide_const "RSpec::Autotest" }

      it "adds 'rspec2' to the list of discoveries" do
        Autotest.should_receive(:add_discovery)
        load File.expand_path("../../../lib/autotest/discover.rb", __FILE__)
      end
    end
  end

  context "with ./.rspec absent" do
    before { File.stub(:exist?).with("./.rspec") { false } }

    context "when RSpec::Autotest is defined" do
      before { stub_const "RSpec::Autotest", Module.new }

      it "does not add 'rspec2' to the list of discoveries" do
        Autotest.should_not_receive(:add_discovery)
        load File.expand_path("../../../lib/autotest/discover.rb", __FILE__)
      end
    end

    context "when RSpec::Autotest is not defined" do
      before { hide_const "RSpec::Autotest" }

      it "does not add 'rspec2' to the list of discoveries" do
        Autotest.should_not_receive(:add_discovery)
        load File.expand_path("../../../lib/autotest/discover.rb", __FILE__)
      end
    end
  end
end
