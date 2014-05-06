require "spec_helper"

describe "bundle help" do
  # Rubygems 1.4+ no longer load gem plugins so this test is no longer needed
  rubygems_under_14 = Gem::Requirement.new("< 1.4").satisfied_by?(Gem::Version.new(Gem::VERSION))
  it "complains if older versions of bundler are installed", :if => rubygems_under_14 do
    system_gems "bundler-0.8.1"

    bundle "help", :expect_err => true
    expect(err).to include("older than 0.9")
    expect(err).to include("running `gem cleanup bundler`.")
  end

  it "uses mann when available" do
    fake_man!

    bundle "help gemfile"
    expect(out).to eq(%|["#{root}/lib/bundler/man/gemfile.5"]|)
  end

  it "prefixes bundle commands with bundle- when finding the groff files" do
    fake_man!

    bundle "help install"
    expect(out).to eq(%|["#{root}/lib/bundler/man/bundle-install"]|)
  end

  it "simply outputs the txt file when there is no man on the path" do
    kill_path!

    bundle "help install", :expect_err => true
    expect(out).to match(/BUNDLE-INSTALL/)
  end

  it "still outputs the old help for commands that do not have man pages yet" do
    bundle "help check"
    expect(out).to include("Check searches the local machine")
  end
end
