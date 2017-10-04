require "spec_helper"

describe RSpec::Core::Deprecation do
  describe "#deprecate" do
    it "passes the hash to the reporter" do
      expect(RSpec.configuration.reporter).to receive(:deprecation).with(hash_including :deprecated => "deprecated_method", :replacement => "replacement")
      RSpec.deprecate("deprecated_method", :replacement => "replacement")
    end

    it "adds the call site" do
      expect_deprecation_with_call_site(__FILE__, __LINE__ + 1)
      RSpec.deprecate("deprecated_method")
    end

    it "doesn't override a passed call site" do
      expect_deprecation_with_call_site("some_file.rb", 17)
      RSpec.deprecate("deprecated_method", :call_site => "/some_file.rb:17")
    end
  end

  describe "#warn_deprecation" do
    it "puts message in a hash" do
      expect(RSpec.configuration.reporter).to receive(:deprecation).with(hash_including :message => "this is the message")
      RSpec.warn_deprecation("this is the message")
    end
  end
end
