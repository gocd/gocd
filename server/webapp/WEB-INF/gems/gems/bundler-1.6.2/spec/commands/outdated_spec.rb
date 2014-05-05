require "spec_helper"

describe "bundle outdated" do
  before :each do
    build_repo2 do
      build_git "foo", :path => lib_path("foo")
      build_git "zebra", :path => lib_path("zebra")
    end

    install_gemfile <<-G
      source "file://#{gem_repo2}"
      gem "zebra", :git => "#{lib_path('zebra')}"
      gem "foo", :git => "#{lib_path('foo')}"
      gem "activesupport", "2.3.5"
      gem "weakling", "~> 0.0.1"
    G
  end

  describe "with no arguments" do
    it "returns a sorted list of outdated gems" do
      update_repo2 do
        build_gem "activesupport", "3.0"
        build_gem "weakling", "0.2"
        update_git "foo", :path => lib_path("foo")
        update_git "zebra", :path => lib_path("zebra")
      end

      bundle "outdated"

      expect(out).to include("activesupport (3.0 > 2.3.5) Gemfile specifies \"= 2.3.5\"")
      expect(out).to include("weakling (0.2 > 0.0.3) Gemfile specifies \"~> 0.0.1\"")
      expect(out).to include("foo (1.0")

      # Gem names are one per-line, between "*" and their parenthesized version.
      gem_list = out.split("\n").map { |g| g[ /\* (.*) \(/, 1] }.compact
      expect(gem_list).to eq(gem_list.sort)
    end

    it "returns non zero exit status if outdated gems present" do
      update_repo2 do
        build_gem "activesupport", "3.0"
        update_git "foo", :path => lib_path("foo")
      end

      bundle "outdated", :exitstatus => true

      expect(exitstatus).to_not be_zero
    end

    it "returns success exit status if no outdated gems present" do
      bundle "outdated", :exitstatus => true

      expect(exitstatus).to be_zero
    end
  end

  describe "with --local option" do
    it "doesn't hit repo2" do
      FileUtils.rm_rf(gem_repo2)

      bundle "outdated --local"
      expect(out).not_to match(/Fetching/)
    end
  end

  describe "with specified gems" do
    it "returns list of outdated gems" do
      update_repo2 do
        build_gem "activesupport", "3.0"
        update_git "foo", :path => lib_path("foo")
      end

      bundle "outdated foo"
      expect(out).not_to include("activesupport (3.0 > 2.3.5)")
      expect(out).to include("foo (1.0")
    end
  end

  describe "pre-release gems" do
    context "without the --pre option" do
      it "ignores pre-release versions" do
        update_repo2 do
          build_gem "activesupport", "3.0.0.beta"
        end

        bundle "outdated"
        expect(out).not_to include("activesupport (3.0.0.beta > 2.3.5)")
      end
    end

    context "with the --pre option" do
      it "includes pre-release versions" do
        update_repo2 do
          build_gem "activesupport", "3.0.0.beta"
        end

        bundle "outdated --pre"
        expect(out).to include("activesupport (3.0.0.beta > 2.3.5) Gemfile specifies \"= 2.3.5\"")
      end
    end

    context "when current gem is a pre-release" do
      it "includes the gem" do
        update_repo2 do
          build_gem "activesupport", "3.0.0.beta.1"
          build_gem "activesupport", "3.0.0.beta.2"
        end

        install_gemfile <<-G
          source "file://#{gem_repo2}"
          gem "activesupport", "3.0.0.beta.1"
        G

        bundle "outdated"
        expect(out).to include("activesupport (3.0.0.beta.2 > 3.0.0.beta.1) Gemfile specifies \"= 3.0.0.beta.1\"")
      end
    end
  end

  describe "with --strict option" do
    it "only reports gems that have a newer version that matches the specified dependency version requirements" do
      update_repo2 do
        build_gem "activesupport", "3.0"
        build_gem "weakling", "0.0.5"
      end

      bundle "outdated --strict"

      expect(out).to_not include("activesupport (3.0 > 2.3.5) Gemfile specifies \"= 2.3.5\"")
      expect(out).to include("weakling (0.0.5 > 0.0.3) Gemfile specifies \"~> 0.0.1\"")
    end

    it "only reports gem dependencies when they can actually be updated" do
      install_gemfile <<-G
        source "file://#{gem_repo2}"
        gem "rack_middleware", "1.0"
      G

      bundle "outdated --strict"

      expect(out).to_not include("rack (1.2 > 0.9.1)")
    end
  end

  describe "with invalid gem name" do
    it "returns could not find gem name" do
      bundle "outdated invalid_gem_name"
      expect(out).to include("Could not find gem 'invalid_gem_name'.")
    end

    it "returns non-zero exit code" do
      bundle "outdated invalid_gem_name", :exitstatus => true
      expect(exitstatus).to_not be_zero
    end
  end
end
