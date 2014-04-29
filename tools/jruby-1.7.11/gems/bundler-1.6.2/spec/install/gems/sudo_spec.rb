require "spec_helper"

describe "when using sudo", :sudo => true do
  describe "and BUNDLE_PATH is writable" do
    context "but BUNDLE_PATH/build_info is not writable" do
      before do
        subdir = system_gem_path('cache')
        subdir.mkpath
        sudo "chmod u-w #{subdir}"
      end

      it "installs" do
        install_gemfile <<-G
          source "file://#{gem_repo1}"
          gem "rack"
        G

        expect(out).to_not match(/an error occurred/i)
        expect(system_gem_path("cache/rack-1.0.0.gem")).to exist
        should_be_installed "rack 1.0"
      end
    end
  end

  describe "and GEM_HOME is owned by root" do
    before :each do
      chown_system_gems_to_root
    end

    it "installs" do
      install_gemfile <<-G
        source "file://#{gem_repo1}"
        gem "rack", '1.0'
        gem "thin"
      G

      expect(system_gem_path("gems/rack-1.0.0")).to exist
      expect(system_gem_path("gems/rack-1.0.0").stat.uid).to eq(0)
      should_be_installed "rack 1.0"
    end

    it "installs rake and a gem dependent on rake in the same session" do
      gemfile <<-G
          source "file://#{gem_repo1}"
          gem "rake"
          gem "another_implicit_rake_dep"
      G
      bundle "install"
      expect(system_gem_path("gems/another_implicit_rake_dep-1.0")).to exist
    end


    it "installs when BUNDLE_PATH is owned by root" do
      bundle_path = tmp("owned_by_root")
      FileUtils.mkdir_p bundle_path
      sudo "chown -R root #{bundle_path}"

      ENV['BUNDLE_PATH'] = bundle_path.to_s
      install_gemfile <<-G
        source "file://#{gem_repo1}"
        gem "rack", '1.0'
      G

      expect(bundle_path.join("gems/rack-1.0.0")).to exist
      expect(bundle_path.join("gems/rack-1.0.0").stat.uid).to eq(0)
      should_be_installed "rack 1.0"
    end

    it "installs when BUNDLE_PATH does not exist" do
      root_path = tmp("owned_by_root")
      FileUtils.mkdir_p root_path
      sudo "chown -R root #{root_path}"
      bundle_path = root_path.join("does_not_exist")

      ENV['BUNDLE_PATH'] = bundle_path.to_s
      install_gemfile <<-G
        source "file://#{gem_repo1}"
        gem "rack", '1.0'
      G

      expect(bundle_path.join("gems/rack-1.0.0")).to exist
      expect(bundle_path.join("gems/rack-1.0.0").stat.uid).to eq(0)
      should_be_installed "rack 1.0"
    end

    it "installs extensions/ compiled by Rubygems 2.2", :rubygems => "2.2" do
      install_gemfile <<-G
        source "file://#{gem_repo1}"
        gem "very_simple_binary"
      G

      expect(system_gem_path("gems/very_simple_binary-1.0")).to exist
      binary_glob = system_gem_path("extensions/*/*/very_simple_binary-1.0")
      expect(Dir.glob(binary_glob).first).to be
    end
  end

  describe "and BUNDLE_PATH is not writable" do
    it "installs" do
      sudo "chmod ugo-w #{default_bundle_path}"
      install_gemfile <<-G
        source "file://#{gem_repo1}"
        gem "rack", '1.0'
      G

      expect(default_bundle_path("gems/rack-1.0.0")).to exist
      should_be_installed "rack 1.0"
    end
  end

  describe "and GEM_HOME is not writable" do
    it "installs" do
      gem_home = tmp('sudo_gem_home')
      sudo "mkdir -p #{gem_home}"
      sudo "chmod ugo-w #{gem_home}"

      gemfile <<-G
        source "file://#{gem_repo1}"
        gem "rack", '1.0'
      G

      bundle :install, :env => {'GEM_HOME' => gem_home.to_s, 'GEM_PATH' => nil}
      expect(gem_home.join('bin/rackup')).to exist
      should_be_installed "rack 1.0", :env => {'GEM_HOME' => gem_home.to_s, 'GEM_PATH' => nil}
    end
  end

  describe "and root runs install" do
    it "warns against that" do
      gemfile %|source "file://#{gem_repo1}"|
      bundle :install, :sudo => true
      expect(out).to include("Don't run Bundler as root.")
    end
  end

end
