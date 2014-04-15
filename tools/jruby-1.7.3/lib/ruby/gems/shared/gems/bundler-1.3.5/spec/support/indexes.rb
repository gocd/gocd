module Spec
  module Indexes
    def dep(name, reqs = nil)
      @deps ||= []
      @deps << Bundler::Dependency.new(name, :version => reqs)
    end

    def platform(*args)
      @platforms ||= []
      @platforms.concat args.map { |p| Gem::Platform.new(p) }
    end

    alias platforms platform

    def resolve
      @platforms ||= ['ruby']
      deps = []
      @deps.each do |d|
        @platforms.each do |p|
          deps << Bundler::DepProxy.new(d, p)
        end
      end
      Bundler::Resolver.resolve(deps, @index)
    end

    def should_resolve_as(specs)
      got = resolve
      got = got.map { |s| s.full_name }.sort
      expect(got).to eq(specs.sort)
    end

    def should_conflict_on(names)
      begin
        got = resolve
        flunk "The resolve succeeded with: #{got.map { |s| s.full_name }.sort.inspect}"
      rescue Bundler::VersionConflict => e
        expect(Array(names).sort).to eq(e.conflicts.sort)
      end
    end

    def gem(*args, &blk)
      build_spec(*args, &blk).first
    end

    def an_awesome_index
      build_index do
        gem "rack", %w(0.8 0.9 0.9.1 0.9.2 1.0 1.1)
        gem "rack-mount", %w(0.4 0.5 0.5.1 0.5.2 0.6)

        # --- Rails
        versions "1.2.3 2.2.3 2.3.5 3.0.0.beta 3.0.0.beta1" do |version|
          gem "activesupport", version
          gem "actionpack", version do
            dep "activesupport", version
            if version >= v('3.0.0.beta')
              dep "rack", '~> 1.1'
              dep "rack-mount", ">= 0.5"
            elsif version > v('2.3')   then dep "rack", '~> 1.0.0'
            elsif version > v('2.0.0') then dep "rack", '~> 0.9.0'
            end
          end
          gem "activerecord", version do
            dep "activesupport", version
            dep "arel", ">= 0.2" if version >= v('3.0.0.beta')
          end
          gem "actionmailer", version do
            dep "activesupport", version
            dep "actionmailer",  version
          end
          if version < v('3.0.0.beta')
            gem "railties", version do
              dep "activerecord",  version
              dep "actionpack",    version
              dep "actionmailer",  version
              dep "activesupport", version
            end
          else
            gem "railties", version
            gem "rails", version do
              dep "activerecord",  version
              dep "actionpack",    version
              dep "actionmailer",  version
              dep "activesupport", version
              dep "railties",      version
            end
          end
        end

        versions '1.0 1.2 1.2.1 1.2.2 1.3 1.3.0.1 1.3.5 1.4.0 1.4.2 1.4.2.1' do |version|
          platforms "ruby java mswin32 mingw32" do |platform|
            next if version == v('1.4.2.1') && platform != pl('x86-mswin32')
            next if version == v('1.4.2') && platform == pl('x86-mswin32')
            gem "nokogiri", version, platform do
              dep "weakling", ">= 0.0.3" if platform =~ pl('java')
            end
          end
        end

        versions '0.0.1 0.0.2 0.0.3' do |version|
          gem "weakling", version
        end

        # --- Rails related
        versions '1.2.3 2.2.3 2.3.5' do |version|
          gem "activemerchant", version do
            dep "activesupport", ">= #{version}"
          end
        end
      end
    end

    # Builder 3.1.4 will activate first, but if all
    # goes well, it should resolve to 3.0.4
    def a_conflict_index
      build_index do
        gem "builder", %w(3.0.4 3.1.4)
        gem("grape", '0.2.6') do
          dep "builder", ">= 0"
        end

        versions '3.2.8 3.2.9 3.2.10 3.2.11' do |version|
          gem("activemodel", version) do
            dep "builder", "~> 3.0.0"
          end
        end

        gem("my_app", '1.0.0') do
          dep "activemodel", ">= 0"
          dep "grape", ">= 0"
        end
      end
    end
  end
end
