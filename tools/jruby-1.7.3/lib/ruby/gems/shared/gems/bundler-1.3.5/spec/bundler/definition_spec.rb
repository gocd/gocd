require 'spec_helper'
require 'bundler/definition'

describe Bundler::Definition do
  before do
    Bundler.stub(:settings){ Bundler::Settings.new(".") }
  end

  describe "#lock" do
    context "when it's not possible to write to the file" do
      subject{ Bundler::Definition.new(nil, [], [], []) }

      before do
        File.should_receive(:open).with("Gemfile.lock", "wb").
          and_raise(Errno::EACCES)
      end

      it "raises an InstallError with explanation" do
        expect{ subject.lock("Gemfile.lock") }.
          to raise_error(Bundler::InstallError)
      end
    end
  end

end
