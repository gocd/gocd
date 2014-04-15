require "spec_helper"
require "rubygems/security"

# unfortunately, testing signed gems with a provided CA is extremely difficult
# as 'gem cert' is currently the only way to add CAs to the system.

describe "policies with unsigned gems" do
  before do
    build_security_repo
    gemfile <<-G
      source "file://#{security_repo}"
      gem "rack"
      gem "signed_gem"
    G
  end

  it "works after you try to deploy without a lock" do
    bundle "install --deployment"
    bundle :install, :exitstatus => true
    expect(exitstatus).to eq(0)
    should_be_installed "rack 1.0", "signed_gem 1.0"
  end

  it "fails when given invalid security policy" do
    bundle "install --trust-policy=InvalidPolicyName"
    expect(out).to include("Rubygems doesn't know about trust policy")
  end

  it "fails with High Security setting due to presence of unsigned gem" do
    bundle "install --trust-policy=HighSecurity"
    expect(out).to include("security policy didn't allow")
  end

  # This spec will fail on Rubygems 2 rc1 due to a bug in policy.rb. the bug is fixed in rc3.
  it "fails with Medium Security setting due to presence of unsigned gem", :unless => ENV['RGV'] == "v2.0.0.rc.1" do
    bundle "install --trust-policy=MediumSecurity"
    expect(out).to include("security policy didn't allow")
  end

  it "succeeds with no policy" do
    bundle "install", :exitstatus => true
    expect(exitstatus).to eq(0)
  end

end

describe "policies with signed gems, no CA" do
  before do
    build_security_repo
    gemfile <<-G
      source "file://#{security_repo}"
      gem "signed_gem"
    G
  end

  it "fails with High Security setting, gem is self-signed" do
    bundle "install --trust-policy=HighSecurity"
    expect(out).to include("security policy didn't allow")
  end

  it "fails with Medium Security setting, gem is self-signed" do
    bundle "install --trust-policy=MediumSecurity"
    expect(out).to include("security policy didn't allow")
  end

  it "succeeds with Low Security setting, low security accepts self signed gem" do
    bundle "install --trust-policy=LowSecurity", :exitstatus => true
    expect(exitstatus).to eq(0)
    should_be_installed "signed_gem 1.0"
  end

  it "succeeds with no policy" do
    bundle "install", :exitstatus => true
    expect(exitstatus).to eq(0)
    should_be_installed "signed_gem 1.0"
  end

end
