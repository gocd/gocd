require "spec_helper"

describe "bundle console" do
  before :each do
    install_gemfile <<-G
      source "file://#{gem_repo1}"
      gem "rack"
      gem "activesupport", :group => :test
      gem "rack_middleware", :group => :development
    G
  end

  it "starts IRB with the default group loaded" do
    bundle "console" do |input|
      input.puts("puts RACK")
      input.puts("exit")
    end
    expect(out).to include("0.9.1")
  end

  it "starts another REPL if configured as such" do
    bundle "config console pry"

    bundle "console" do |input|
      input.puts("__callee__")
      input.puts("exit")
    end
    expect(out).to include("pry")
  end

  it "falls back to IRB if the other REPL isn't available" do
    bundle "config console pry"
    # make sure pry isn't there

    bundle "console" do |input|
      input.puts("__callee__")
      input.puts("exit")
    end
    expect(out).to include("irb")
  end


  it "doesn't load any other groups" do
    bundle "console" do |input|
      input.puts("puts ACTIVESUPPORT")
      input.puts("exit")
    end
    expect(out).to include("NameError")
  end

  describe "when given a group" do
    it "loads the given group" do
      bundle "console test" do |input|
        input.puts("puts ACTIVESUPPORT")
        input.puts("exit")
      end
      expect(out).to include("2.3.5")
    end

    it "loads the default group" do
      bundle "console test" do |input|
        input.puts("puts RACK")
        input.puts("exit")
      end
      expect(out).to include("0.9.1")
    end

    it "doesn't load other groups" do
      bundle "console test" do |input|
        input.puts("puts RACK_MIDDLEWARE")
        input.puts("exit")
      end
      expect(out).to include("NameError")
    end
  end
end
