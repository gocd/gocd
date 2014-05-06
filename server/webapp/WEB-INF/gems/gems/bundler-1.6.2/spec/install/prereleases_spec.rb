require 'spec_helper'

describe "bundle install" do

  describe "when prerelease gems are available" do
    it "finds prereleases" do
      install_gemfile <<-G
        source "file://#{gem_repo1}"
        gem "not_released"
      G
      should_be_installed "not_released 1.0.pre"
    end

    it "uses regular releases if available" do
      install_gemfile <<-G
        source "file://#{gem_repo1}"
        gem "has_prerelease"
      G
      should_be_installed "has_prerelease 1.0"
    end

    it "uses prereleases if requested" do
      install_gemfile <<-G
        source "file://#{gem_repo1}"
        gem "has_prerelease", "1.1.pre"
      G
      should_be_installed "has_prerelease 1.1.pre"
    end
  end

  describe "when prerelease gems are not available" do
    it "still works" do
      build_repo3
      install_gemfile <<-G
        source "file://#{gem_repo3}"
        gem "rack"
      G

      should_be_installed "rack 1.0"
    end
  end

end
