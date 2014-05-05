require 'bundler/shared_helpers'

module Spec
  module Builders
    def self.constantize(name)
      name.gsub('-', '').upcase
    end

    def v(version)
      Gem::Version.new(version)
    end

    def pl(platform)
      Gem::Platform.new(platform)
    end

    def build_repo1
      build_repo gem_repo1 do
        build_gem "rack", %w(0.9.1 1.0.0) do |s|
          s.executables = "rackup"
          s.post_install_message = "Rack's post install message"
        end

        build_gem "thin" do |s|
          s.add_dependency "rack"
          s.post_install_message = "Thin's post install message"
        end

        build_gem "rack-obama" do |s|
          s.add_dependency "rack"
          s.post_install_message = "Rack-obama's post install message"
        end

        build_gem "rack_middleware", "1.0" do |s|
          s.add_dependency "rack", "0.9.1"
        end

        build_gem "rails",          "2.3.2" do |s|
          s.executables = "rails"
          s.add_dependency "rake",           "10.0.2"
          s.add_dependency "actionpack",     "2.3.2"
          s.add_dependency "activerecord",   "2.3.2"
          s.add_dependency "actionmailer",   "2.3.2"
          s.add_dependency "activeresource", "2.3.2"
        end
        build_gem "actionpack",     "2.3.2" do |s|
          s.add_dependency "activesupport", "2.3.2"
        end
        build_gem "activerecord",   ["2.3.1", "2.3.2"] do |s|
          s.add_dependency "activesupport", "2.3.2"
        end
        build_gem "actionmailer",   "2.3.2" do |s|
          s.add_dependency "activesupport", "2.3.2"
        end
        build_gem "activeresource", "2.3.2" do |s|
          s.add_dependency "activesupport", "2.3.2"
        end
        build_gem "activesupport",  %w(1.2.3 2.3.2 2.3.5)

        build_gem "activemerchant" do |s|
          s.add_dependency "activesupport", ">= 2.0.0"
        end

        build_gem "rails_fail" do |s|
          s.add_dependency "activesupport", "= 1.2.3"
        end

        build_gem "missing_dep" do |s|
          s.add_dependency "not_here"
        end

        build_gem "rspec", "1.2.7", :no_default => true do |s|
          s.write "lib/spec.rb", "SPEC = '1.2.7'"
        end

        build_gem "rack-test", :no_default => true do |s|
          s.write "lib/rack/test.rb", "RACK_TEST = '1.0'"
        end

        build_gem "platform_specific" do |s|
          s.platform = Gem::Platform.local
          s.write "lib/platform_specific.rb", "PLATFORM_SPECIFIC = '1.0.0 #{Gem::Platform.local}'"
        end

        build_gem "platform_specific" do |s|
          s.platform = "java"
          s.write "lib/platform_specific.rb", "PLATFORM_SPECIFIC = '1.0.0 JAVA'"
        end

        build_gem "platform_specific" do |s|
          s.platform = "ruby"
          s.write "lib/platform_specific.rb", "PLATFORM_SPECIFIC = '1.0.0 RUBY'"
        end

        build_gem "platform_specific" do |s|
          s.platform = "x86-mswin32"
          s.write "lib/platform_specific.rb", "PLATFORM_SPECIFIC = '1.0.0 MSWIN'"
        end

        build_gem "platform_specific" do |s|
          s.platform = "x86-darwin-100"
          s.write "lib/platform_specific.rb", "PLATFORM_SPECIFIC = '1.0.0 x86-darwin-100'"
        end

        build_gem "only_java" do |s|
          s.platform = "java"
        end

        build_gem "nokogiri", "1.4.2"
        build_gem "nokogiri", "1.4.2" do |s|
          s.platform = "java"
          s.write "lib/nokogiri.rb", "NOKOGIRI = '1.4.2 JAVA'"
          s.add_dependency "weakling", ">= 0.0.3"
        end

        build_gem "weakling", "0.0.3"

        build_gem "multiple_versioned_deps" do |s|
          s.add_dependency "weakling", ">= 0.0.1", "< 0.1"
        end

        build_gem "not_released", "1.0.pre"

        build_gem "has_prerelease", "1.0"
        build_gem "has_prerelease", "1.1.pre"

        build_gem "with_development_dependency" do |s|
          s.add_development_dependency "activesupport", "= 2.3.5"
        end

        build_gem "with_license" do |s|
          s.license = "MIT"
        end

        build_gem "with_implicit_rake_dep" do |s|
          s.extensions << "Rakefile"
          s.write "Rakefile", <<-RUBY
            task :default do
              path = File.expand_path("../lib", __FILE__)
              FileUtils.mkdir_p(path)
              File.open("\#{path}/implicit_rake_dep.rb", "w") do |f|
                f.puts "IMPLICIT_RAKE_DEP = 'YES'"
              end
            end
          RUBY
        end

        build_gem "another_implicit_rake_dep" do |s|
          s.extensions << "Rakefile"
          s.write "Rakefile", <<-RUBY
            task :default do
              path = File.expand_path("../lib", __FILE__)
              FileUtils.mkdir_p(path)
              File.open("\#{path}/another_implicit_rake_dep.rb", "w") do |f|
                f.puts "ANOTHER_IMPLICIT_RAKE_DEP = 'YES'"
              end
            end
          RUBY
        end

        build_gem "very_simple_binary" do |s|
          s.add_c_extension
        end

        build_gem "bundler", "0.9" do |s|
          s.executables = "bundle"
          s.write "bin/bundle", "puts 'FAIL'"
        end

        # The bundler 0.8 gem has a rubygems plugin that always loads :(
        build_gem "bundler", "0.8.1" do |s|
          s.write "lib/bundler/omg.rb", ""
          s.write "lib/rubygems_plugin.rb", "require 'bundler/omg' ; puts 'FAIL'"
        end

        build_gem "bundler_dep" do |s|
          s.add_dependency "bundler"
        end

        # The yard gem iterates over Gem.source_index looking for plugins
        build_gem "yard" do |s|
          s.write "lib/yard.rb", <<-Y
            if Gem::Version.new(Gem::VERSION) >= Gem::Version.new("1.8.10")
              specs = Gem::Specification
            else
              specs = Gem.source_index.find_name('')
            end
            specs.each do |gem|
              puts gem.full_name
            end
          Y
        end

        # The rcov gem is platform mswin32, but has no arch
        build_gem "rcov" do |s|
          s.platform = Gem::Platform.new([nil, "mswin32", nil])
          s.write "lib/rcov.rb", "RCOV = '1.0.0'"
        end

        build_gem "net-ssh"
        build_gem "net-sftp", "1.1.1" do |s|
          s.add_dependency "net-ssh", ">= 1.0.0", "< 1.99.0"
        end

        # Test comlicated gem dependencies for install
        build_gem "net_a" do |s|
          s.add_dependency "net_b"
          s.add_dependency "net_build_extensions"
        end

        build_gem "net_b"

        build_gem "net_build_extensions" do |s|
          s.add_dependency "rake"
          s.extensions << "Rakefile"
          s.write "Rakefile", <<-RUBY
            task :default do
              path = File.expand_path("../lib", __FILE__)
              FileUtils.mkdir_p(path)
              File.open("\#{path}/net_build_extensions.rb", "w") do |f|
                f.puts "NET_BUILD_EXTENSIONS = 'YES'"
              end
            end
          RUBY
        end

        build_gem "net_c" do |s|
          s.add_dependency "net_a"
          s.add_dependency "net_d"
        end

        build_gem "net_d"

        build_gem "net_e" do |s|
          s.add_dependency "net_d"
        end

        # Capistrano did this (at least until version 2.5.10)
        # Rubygems 2.2 doesn't allow the specifying of a dependency twice
        # See https://github.com/rubygems/rubygems/commit/03dbac93a3396a80db258d9bc63500333c25bd2f
        build_gem "double_deps", "1.0", :skip_validation => true do |s|
          s.add_dependency "net-ssh", ">= 1.0.0"
          s.add_dependency "net-ssh"
        end

        build_gem "foo"
      end
    end

    def build_repo2(&blk)
      FileUtils.rm_rf gem_repo2
      FileUtils.cp_r gem_repo1, gem_repo2
      update_repo2(&blk) if block_given?
    end

    def build_repo3
      build_repo gem_repo3 do
        build_gem "rack"
      end
      FileUtils.rm_rf Dir[gem_repo3("prerelease*")]
    end

    def update_repo2
      update_repo gem_repo2 do
        build_gem "rack", "1.2" do |s|
          s.executables = "rackup"
        end
        yield if block_given?
      end
    end

    def build_security_repo
      build_repo security_repo do
        build_gem "rack"

        build_gem "signed_gem" do |s|
          cert = 'signing-cert.pem'
          pkey = 'signing-pkey.pem'
          s.write cert, TEST_CERT
          s.write pkey, TEST_PKEY
          s.signing_key = pkey
          s.cert_chain = [cert]
        end
      end

    end

    def build_repo(path, &blk)
      return if File.directory?(path)
      rake_path = Dir["#{Path.base_system_gems}/**/rake*.gem"].first
      if rake_path
        FileUtils.mkdir_p("#{path}/gems")
        FileUtils.cp rake_path, "#{path}/gems/"
      else
        abort "You need to `rm -rf #{tmp}`"
      end
      update_repo(path, &blk)
    end

    def update_repo(path)
      @_build_path = "#{path}/gems"
      yield
      @_build_path = nil
      with_gem_path_as Path.base_system_gems do
        Dir.chdir(path) { gem_command :generate_index }
      end
    end

    def build_index(&block)
      index = Bundler::Index.new
      IndexBuilder.run(index, &block) if block_given?
      index
    end

    def build_spec(name, version, platform = nil, &block)
      Array(version).map do |v|
        Gem::Specification.new do |s|
          s.name     = name
          s.version  = Gem::Version.new(v)
          s.platform = platform
          DepBuilder.run(s, &block) if block_given?
        end
      end
    end

    def build_dep(name, requirements = Gem::Requirement.default, type = :runtime)
      Bundler::Dependency.new(name, :version => requirements)
    end

    def build_lib(name, *args, &blk)
      build_with(LibBuilder, name, args, &blk)
    end

    def build_gem(name, *args, &blk)
      build_with(GemBuilder, name, args, &blk)
    end

    def build_git(name, *args, &block)
      opts = args.last.is_a?(Hash) ? args.last : {}
      builder = opts[:bare] ? GitBareBuilder : GitBuilder
      spec = build_with(builder, name, args, &block)
      GitReader.new(opts[:path] || lib_path(spec.full_name))
    end

    def update_git(name, *args, &block)
      spec = build_with(GitUpdater, name, args, &block)
      GitReader.new lib_path(spec.full_name)
    end

  private

    def build_with(builder, name, args, &blk)
      @_build_path ||= nil
      options  = args.last.is_a?(Hash) ? args.pop : {}
      versions = args.last || "1.0"
      spec     = nil

      options[:path] ||= @_build_path

      Array(versions).each do |version|
        spec = builder.new(self, name, version)
        if !spec.authors or spec.authors.empty?
          spec.authors = ["no one"]
        end
        yield spec if block_given?
        spec._build(options)
      end

      spec
    end

    class IndexBuilder
      include Builders

      def self.run(index, &block)
        new(index).run(&block)
      end

      def initialize(index)
        @index = index
      end

      def run(&block)
        instance_eval(&block)
      end

      def gem(*args, &block)
        build_spec(*args, &block).each do |s|
          @index << s
        end
      end

      def platforms(platforms)
        platforms.split(/\s+/).each do |platform|
          platform.gsub!(/^(mswin32)$/, 'x86-\1')
          yield Gem::Platform.new(platform)
        end
      end

      def versions(versions)
        versions.split(/\s+/).each { |version| yield v(version) }
      end
    end

    class DepBuilder
      include Builders

      def self.run(spec, &block)
        new(spec).run(&block)
      end

      def initialize(spec)
        @spec = spec
      end

      def run(&block)
        instance_eval(&block)
      end

      def runtime(name, requirements)
        @spec.add_runtime_dependency(name, requirements)
      end

      alias dep runtime
    end

    class LibBuilder
      def initialize(context, name, version)
        @context = context
        @name    = name
        @spec = Gem::Specification.new do |s|
          s.name    = name
          s.version = version
          s.summary = "This is just a fake gem for testing"
        end
        @files = {}
      end

      def method_missing(*args, &blk)
        @spec.send(*args, &blk)
      end

      def write(file, source = "")
        @files[file] = source
      end

      def executables=(val)
        Array(val).each do |file|
          write "#{@spec.bindir}/#{file}", "require '#{@name}' ; puts #{@name.upcase}"
        end
        @spec.executables = Array(val)
      end

      def add_c_extension
        require_paths << 'ext'
        extensions << "ext/extconf.rb"
        write "ext/extconf.rb", <<-RUBY
          require "mkmf"

          # exit 1 unless with_config("simple")

          extension_name = "very_simple_binary_c"
          dir_config extension_name
          create_makefile extension_name
        RUBY
        write "ext/very_simple_binary.c", <<-C
          #include "ruby.h"

          void Init_very_simple_binary_c() {
            rb_define_module("VerySimpleBinaryInC");
          }
        C
      end

      def _build(options)
        path = options[:path] || _default_path

        if options[:rubygems_version]
          @spec.rubygems_version = options[:rubygems_version]
          def @spec.mark_version; end
          def @spec.validate; end
        end

        case options[:gemspec]
        when false
          # do nothing
        when :yaml
          @files["#{name}.gemspec"] = @spec.to_yaml
        else
          @files["#{name}.gemspec"] = @spec.to_ruby
        end

        unless options[:no_default]
          @files = _default_files.merge(@files)
        end

        @spec.authors = ["no one"]

        @files.each do |file, source|
          file = Pathname.new(path).join(file)
          FileUtils.mkdir_p(file.dirname)
          File.open(file, 'w') { |f| f.puts source }
        end
        @spec.files = @files.keys
        path
      end

      def _default_files
        @_default_files ||= { "lib/#{name}.rb" => "#{Builders.constantize(name)} = '#{version}'" }
      end

      def _default_path
        @context.tmp('libs', @spec.full_name)
      end
    end

    class GitBuilder < LibBuilder
      def _build(options)
        path = options[:path] || _default_path
        super(options.merge(:path => path))
        Dir.chdir(path) do
          `git init`
          `git add *`
          `git config user.email "lol@wut.com"`
          `git config user.name "lolwut"`
          `git commit -m 'OMG INITIAL COMMIT'`
        end
      end
    end

    class GitBareBuilder < LibBuilder
      def _build(options)
        path = options[:path] || _default_path
        super(options.merge(:path => path))
        Dir.chdir(path) do
          `git init --bare`
        end
      end
    end

    class GitUpdater < LibBuilder
      def silently(str)
        `#{str} 2>#{Bundler::NULL}`
      end

      def _build(options)
        libpath = options[:path] || _default_path

        Dir.chdir(libpath) do
          silently "git checkout master"

          if branch = options[:branch]
            raise "You can't specify `master` as the branch" if branch == "master"

            if `git branch | grep #{branch}`.empty?
              silently("git branch #{branch}")
            end

            silently("git checkout #{branch}")
          elsif tag = options[:tag]
            `git tag #{tag}`
          elsif options[:remote]
            silently("git remote add origin file://#{options[:remote]}")
          elsif options[:push]
            silently("git push origin #{options[:push]}")
          end

          current_ref = `git rev-parse HEAD`.strip
          _default_files.keys.each do |path|
            _default_files[path] << "\n#{Builders.constantize(name)}_PREV_REF = '#{current_ref}'"
          end
          super(options.merge(:path => libpath, :gemspec => false))
          `git add *`
          `git commit -m "BUMP"`
        end
      end
    end

    class GitReader
      attr_reader :path

      def initialize(path)
        @path = path
      end

      def ref_for(ref, len = nil)
        ref = git "rev-parse #{ref}"
        ref = ref[0..len] if len
        ref
      end

    private

      def git(cmd)
        Bundler::SharedHelpers.with_clean_git_env do
          Dir.chdir(@path) { `git #{cmd}`.strip }
        end
      end

    end

    class GemBuilder < LibBuilder

      def _build(opts)
        lib_path = super(opts.merge(:path => @context.tmp(".tmp/#{@spec.full_name}"), :no_default => opts[:no_default]))
        Dir.chdir(lib_path) do
          destination = opts[:path] || _default_path
          FileUtils.mkdir_p(destination)

          if !@spec.authors or @spec.authors.empty?
            @spec.authors = ["that guy"]
          end

          Bundler.rubygems.build(@spec, opts[:skip_validation])
          if opts[:to_system]
            `gem install --ignore-dependencies #{@spec.full_name}.gem`
          else
            FileUtils.mv("#{@spec.full_name}.gem", opts[:path] || _default_path)
          end
        end
      end

      def _default_path
        @context.gem_repo1('gems')
      end
    end

    TEST_CERT=<<CERT
-----BEGIN CERTIFICATE-----
MIIDMjCCAhqgAwIBAgIBATANBgkqhkiG9w0BAQUFADAnMQwwCgYDVQQDDAN5b3Ux
FzAVBgoJkiaJk/IsZAEZFgdleGFtcGxlMB4XDTE0MDIwNTE0MTEwNloXDTE1MDIw
NTE0MTEwNlowJzEMMAoGA1UEAwwDeW91MRcwFQYKCZImiZPyLGQBGRYHZXhhbXBs
ZTCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBANlvFdpN43c4DMS9Jo06
m0a7k3bQ3HWQ1yrYhZMi77F1F73NpBknYHIzDktQpGn6hs/4QFJT4m4zNEBF47UL
jHU5nTK5rjkS3niGYUjvh3ZEzVeo9zHUlD/UwflDo4ALl3TSo2KY/KdPS/UTdLXL
ajkQvaVJtEDgBPE3DPhlj5whp+Ik3mDHej7qpV6F502leAwYaFyOtlEG/ZGNG+nZ
L0clH0j77HpP42AylHDi+vakEM3xcjo9BeWQ6Vkboic93c9RTt6CWBWxMQP7Nol1
MOebz9XOSQclxpxWteXNfPRtMdAhmRl76SMI8ywzThNPpa4EH/yz34ftebVOgKyM
nd0CAwEAAaNpMGcwCQYDVR0TBAIwADALBgNVHQ8EBAMCBLAwHQYDVR0OBBYEFA7D
n9qo0np23qi3aOYuAAPn/5IdMBYGA1UdEQQPMA2BC3lvdUBleGFtcGxlMBYGA1Ud
EgQPMA2BC3lvdUBleGFtcGxlMA0GCSqGSIb3DQEBBQUAA4IBAQB5N+71KiNX3k0Y
FWizUHUYOqo+fEk3aBoFbOFcx+M5TXPiA91pH8mgyyD9tVzqiYgCGk7g/UrOv5Ec
NHjYqsFecNPayj8u2Po07pp2Nbc9aqt9uxoRSENZ72Dzrk3YDStgji7Sk/LZW5Kn
4JFTeRof+B7/ZijLZWBEnMfSHSIPaQu0Ig6c19OEhiiuhGOn4WUhGFYCadciKgnj
PtaUcxKuvXMwRPG3NZqeGZivTlrhAmmf7iPV/E334qzMytvAV8zycZFj2J5hvyLk
432zLezmbFGc0uLIUjXDu9bM5WYGy1BOTuzuApyuCs9S1greqz17f8f01J87DX3X
wJsshs4b
-----END CERTIFICATE-----
CERT

    TEST_PKEY=<<PKEY
-----BEGIN RSA PRIVATE KEY-----
MIIEowIBAAKCAQEA2W8V2k3jdzgMxL0mjTqbRruTdtDcdZDXKtiFkyLvsXUXvc2k
GSdgcjMOS1CkafqGz/hAUlPibjM0QEXjtQuMdTmdMrmuORLeeIZhSO+HdkTNV6j3
MdSUP9TB+UOjgAuXdNKjYpj8p09L9RN0tctqORC9pUm0QOAE8TcM+GWPnCGn4iTe
YMd6PuqlXoXnTaV4DBhoXI62UQb9kY0b6dkvRyUfSPvsek/jYDKUcOL69qQQzfFy
Oj0F5ZDpWRuiJz3dz1FO3oJYFbExA/s2iXUw55vP1c5JByXGnFa15c189G0x0CGZ
GXvpIwjzLDNOE0+lrgQf/LPfh+15tU6ArIyd3QIDAQABAoIBACbDqz20TS1gDMa2
gj0DidNedbflHKjJHdNBru7Ad8NHgOgR1YO2hXdWquG6itVqGMbTF4SV9/R1pIcg
7qvEV1I+50u31tvOBWOvcYCzU48+TO2n7gowQA3xPHPYHzog1uu48fAOHl0lwgD7
av9OOK3b0jO5pC08wyTOD73pPWU0NrkTh2+N364leIi1pNuI1z4V+nEuIIm7XpVd
5V4sXidMTiEMJwE6baEDfTjHKaoRndXrrPo3ryIXmcX7Ag1SwAQwF5fBCRToCgIx
dszEZB1bJD5gA6r+eGnJLB/F60nK607az5o3EdguoB2LKa6q6krpaRCmZU5svvoF
J7xgBPECgYEA8RIzHAQ3zbaibKdnllBLIgsqGdSzebTLKheFuigRotEV3Or/z5Lg
k/nVnThWVkTOSRqXTNpJAME6a4KTdcVSxYP+SdZVO1esazHrGb7xPVb7MWSE1cqp
WEk3Yy8OUOPoPQMc4dyGzd30Mi8IBB6gnFIYOTrpUo0XtkBv8rGGhfsCgYEA5uYn
6QgL4NqNT84IXylmMb5ia3iBt6lhxI/A28CDtQvfScl4eYK0IjBwdfG6E1vJgyzg
nJzv3xEVo9bz+Kq7CcThWpK5JQaPnsV0Q74Wjk0ShHet15txOdJuKImnh5F6lylC
GTLR9gnptytfMH/uuw4ws0Q2kcg4l5NHKOWOnAcCgYEAvAwIVkhsB0n59Wu4gCZu
FUZENxYWUk/XUyQ6KnZrG2ih90xQ8+iMyqFOIm/52R2fFKNrdoWoALC6E3ct8+ZS
pMRLrelFXx8K3it4SwMJR2H8XBEfFW4bH0UtsW7Zafv+AunUs9LETP5gKG1LgXsq
qgXX43yy2LQ61O365YPZfdUCgYBVbTvA3MhARbvYldrFEnUL3GtfZbNgdxuD9Mee
xig0eJMBIrgfBLuOlqtVB70XYnM4xAbKCso4loKSHnofO1N99siFkRlM2JOUY2tz
kMWZmmxKdFjuF0WZ5f/5oYxI/QsFGC+rUQEbbWl56mMKd5qkvEhKWudxoklF0yiV
ufC8SwKBgDWb8iWqWN5a/kfvKoxFcDM74UHk/SeKMGAL+ujKLf58F+CbweM5pX9C
EUsxeoUEraVWTiyFVNqD81rCdceus9TdBj0ZIK1vUttaRZyrMAwF0uQSfjtxsOpd
l69BkyvzjgDPkmOHVGiSZDLi3YDvypbUpo6LOy4v5rVg5U2F/A0v
-----END RSA PRIVATE KEY-----
PKEY
  end
end
