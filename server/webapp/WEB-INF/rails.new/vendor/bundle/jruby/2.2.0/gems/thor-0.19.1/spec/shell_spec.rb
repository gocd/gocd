require "helper"

describe Thor::Shell do
  def shell
    @shell ||= Thor::Base.shell.new
  end

  describe "#initialize" do
    it "sets shell value" do
      base = MyCounter.new [1, 2], {}, :shell => shell
      expect(base.shell).to eq(shell)
    end

    it "sets the base value on the shell if an accessor is available" do
      base = MyCounter.new [1, 2], {}, :shell => shell
      expect(shell.base).to eq(base)
    end
  end

  describe "#shell" do
    it "returns the shell in use" do
      expect(MyCounter.new([1, 2]).shell).to be_kind_of(Thor::Base.shell)
    end

    it "uses $THOR_SHELL" do
      class Thor::Shell::TestShell < Thor::Shell::Basic; end

      expect(Thor::Base.shell).to eq(shell.class)
      ENV["THOR_SHELL"] = "TestShell"
      Thor::Base.shell = nil
      expect(Thor::Base.shell).to eq(Thor::Shell::TestShell)
      ENV["THOR_SHELL"] = ""
      Thor::Base.shell = shell.class
      expect(Thor::Base.shell).to eq(shell.class)
    end
  end

  describe "with_padding" do
    it "uses padding for inside block outputs" do
      base = MyCounter.new([1, 2])
      base.with_padding do
        expect(capture(:stdout) { base.say_status :padding, "cool" }.strip).to eq("padding    cool")
      end
    end
  end

end
