require 'spec_helper'

describe "real world edgecases", :realworld => true do
  # there is no rbx-relative-require gem that will install on 1.9
  it "ignores extra gems with bad platforms", :ruby => "1.8" do
    install_gemfile <<-G
      source :rubygems
      gem "linecache", "0.46"
    G
    expect(err).to eq("")
  end

  # https://github.com/bundler/bundler/issues/1202
  it "bundle cache works with rubygems 1.3.7 and pre gems", :ruby => "1.8" do
    install_gemfile <<-G
      source :rubygems
      gem "rack",          "1.3.0.beta2"
      gem "will_paginate", "3.0.pre2"
    G
    bundle :cache
    expect(out).not_to include("Removing outdated .gem files from vendor/cache")
  end

  # https://github.com/bundler/bundler/issues/1486
  # this is a hash collision that only manifests on 1.8.7
  it "finds the correct child versions", :ruby => "1.8" do
    install_gemfile <<-G
      source :rubygems

      gem 'i18n', '~> 0.4'
      gem 'activesupport', '~> 3.0'
      gem 'activerecord', '~> 3.0'
      gem 'builder', '~> 2.1.2'
    G
    expect(out).to include("activemodel 3.0.5")
  end

  # https://github.com/bundler/bundler/issues/1500
  it "does not fail install because of gem plugins" do
    realworld_system_gems("open_gem --version 1.4.2", "rake --version 0.9.2")
    gemfile <<-G
      source :rubygems

      gem 'rack', '1.0.1'
    G

    bundle "install --path vendor/bundle", :expect_err => true
    expect(err).not_to include("Could not find rake")
    expect(err).to be_empty
  end

  it "checks out git repos when the lockfile is corrupted" do
    gemfile <<-G
      source :rubygems

      gem 'activerecord',  :github => 'carlhuda/rails-bundler-test', :branch => 'master'
      gem 'activesupport', :github => 'carlhuda/rails-bundler-test', :branch => 'master'
      gem 'actionpack',    :github => 'carlhuda/rails-bundler-test', :branch => 'master'
    G

    lockfile <<-L
      GIT
        remote: git://github.com/carlhuda/rails-bundler-test.git
        revision: 369e28a87419565f1940815219ea9200474589d4
        branch: master
        specs:
          actionpack (3.2.2)
            activemodel (= 3.2.2)
            activesupport (= 3.2.2)
            builder (~> 3.0.0)
            erubis (~> 2.7.0)
            journey (~> 1.0.1)
            rack (~> 1.4.0)
            rack-cache (~> 1.2)
            rack-test (~> 0.6.1)
            sprockets (~> 2.1.2)
          activemodel (3.2.2)
            activesupport (= 3.2.2)
            builder (~> 3.0.0)
          activerecord (3.2.2)
            activemodel (= 3.2.2)
            activesupport (= 3.2.2)
            arel (~> 3.0.2)
            tzinfo (~> 0.3.29)
          activesupport (3.2.2)
            i18n (~> 0.6)
            multi_json (~> 1.0)

      GIT
        remote: git://github.com/carlhuda/rails-bundler-test.git
        revision: 369e28a87419565f1940815219ea9200474589d4
        branch: master
        specs:
          actionpack (3.2.2)
            activemodel (= 3.2.2)
            activesupport (= 3.2.2)
            builder (~> 3.0.0)
            erubis (~> 2.7.0)
            journey (~> 1.0.1)
            rack (~> 1.4.0)
            rack-cache (~> 1.2)
            rack-test (~> 0.6.1)
            sprockets (~> 2.1.2)
          activemodel (3.2.2)
            activesupport (= 3.2.2)
            builder (~> 3.0.0)
          activerecord (3.2.2)
            activemodel (= 3.2.2)
            activesupport (= 3.2.2)
            arel (~> 3.0.2)
            tzinfo (~> 0.3.29)
          activesupport (3.2.2)
            i18n (~> 0.6)
            multi_json (~> 1.0)

      GIT
        remote: git://github.com/carlhuda/rails-bundler-test.git
        revision: 369e28a87419565f1940815219ea9200474589d4
        branch: master
        specs:
          actionpack (3.2.2)
            activemodel (= 3.2.2)
            activesupport (= 3.2.2)
            builder (~> 3.0.0)
            erubis (~> 2.7.0)
            journey (~> 1.0.1)
            rack (~> 1.4.0)
            rack-cache (~> 1.2)
            rack-test (~> 0.6.1)
            sprockets (~> 2.1.2)
          activemodel (3.2.2)
            activesupport (= 3.2.2)
            builder (~> 3.0.0)
          activerecord (3.2.2)
            activemodel (= 3.2.2)
            activesupport (= 3.2.2)
            arel (~> 3.0.2)
            tzinfo (~> 0.3.29)
          activesupport (3.2.2)
            i18n (~> 0.6)
            multi_json (~> 1.0)

      GEM
        remote: https://rubygems.org/
        specs:
          arel (3.0.2)
          builder (3.0.0)
          erubis (2.7.0)
          hike (1.2.1)
          i18n (0.6.0)
          journey (1.0.3)
          multi_json (1.1.0)
          rack (1.4.1)
          rack-cache (1.2)
            rack (>= 0.4)
          rack-test (0.6.1)
            rack (>= 1.0)
          sprockets (2.1.2)
            hike (~> 1.2)
            rack (~> 1.0)
            tilt (~> 1.1, != 1.3.0)
          tilt (1.3.3)
          tzinfo (0.3.32)

      PLATFORMS
        ruby

      DEPENDENCIES
        actionpack!
        activerecord!
        activesupport!
    L

    bundle :install, :exitstatus => true
    expect(exitstatus).to eq(0)
  end
end
