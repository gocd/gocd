require "spec_helper"

describe "Bundler.setup with multi platform stuff" do
  it "raises a friendly error when gems are missing locally" do
    gemfile <<-G
      source "file://#{gem_repo1}"
      gem "rack"
    G

    lockfile <<-G
      GEM
        remote: file:#{gem_repo1}/
        specs:
          rack (1.0)

      PLATFORMS
        #{local_tag}

      DEPENDENCIES
        rack
    G

    ruby <<-R
      begin
        require 'bundler'
        Bundler.setup
      rescue Bundler::GemNotFound => e
        puts "WIN"
      end
    R

    expect(out).to eq("WIN")
  end

  it "will resolve correctly on the current platform when the lockfile was targetted for a different one" do
    lockfile <<-G
      GEM
        remote: file:#{gem_repo1}/
        specs:
          nokogiri (1.4.2-java)
            weakling (= 0.0.3)
          weakling (0.0.3)

      PLATFORMS
        java

      DEPENDENCIES
        nokogiri
    G

    system_gems "nokogiri-1.4.2"

    simulate_platform "x86-darwin-10"
    gemfile <<-G
      source "file://#{gem_repo1}"
      gem "nokogiri"
    G

    should_be_installed "nokogiri 1.4.2"
  end

  it "will add the resolve for the current platform" do
    lockfile <<-G
      GEM
        remote: file:#{gem_repo1}/
        specs:
          nokogiri (1.4.2-java)
            weakling (= 0.0.3)
          weakling (0.0.3)

      PLATFORMS
        java

      DEPENDENCIES
        nokogiri
    G

    simulate_platform "x86-darwin-100"

    system_gems "nokogiri-1.4.2", "platform_specific-1.0-x86-darwin-100"

    gemfile <<-G
      source "file://#{gem_repo1}"
      gem "nokogiri"
      gem "platform_specific"
    G

    should_be_installed "nokogiri 1.4.2", "platform_specific 1.0 x86-darwin-100"
  end
end
