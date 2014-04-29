require 'spec_helper'

describe "parallel", :realworld => true do
  it "installs", :ruby => "1.8" do
    gemfile <<-G
      source "https://rubygems.org"
      gem 'activesupport', '~> 3.2.13'
      gem 'faker', '~> 1.1.2'
    G

    bundle :install, :jobs => 4, :env => {"DEBUG" => "1"}
    expect(out).to match(/[1-3]: /)

    bundle "show activesupport"
    expect(out).to match(/activesupport/)

    bundle "show faker"
    expect(out).to match(/faker/)

    bundle "config jobs"
    expect(out).to match(/: "4"/)
  end

  it "installs even with circular dependency", :ruby => "1.9" do
    gemfile <<-G
      source 'https://rubygems.org'
      gem 'activesupport', '~> 3.2.13'
      gem 'mongoid_auto_increment', "0.1.1"
    G

    bundle :install, :jobs => 4, :env => {"DEBUG" => "1"}
    expect(out).to match(/[1-3]: /)

    bundle "show activesupport"
    expect(out).to match(/activesupport/)

    bundle "show mongoid_auto_increment"
    expect(out).to match(%r{gems/mongoid_auto_increment})

    bundle "config jobs"
    expect(out).to match(/: "4"/)
  end

  it "updates" do
    install_gemfile <<-G
      source "https://rubygems.org"
      gem 'activesupport', '3.2.12'
      gem 'faker', '~> 1.1.2'
    G

    gemfile <<-G
      source "https://rubygems.org"
      gem 'activesupport', '~> 3.2.12'
      gem 'faker', '~> 1.1.2'
    G

    bundle :update, :jobs => 4, :env => {"DEBUG" => "1"}
     expect(out).to match(/[1-3]: /)

    bundle "show activesupport"
    expect(out).to match(/activesupport-3\.2\.1[3-9]/)

    bundle "show faker"
    expect(out).to match(/faker/)

    bundle "config jobs"
    expect(out).to match(/: "4"/)
  end
end
