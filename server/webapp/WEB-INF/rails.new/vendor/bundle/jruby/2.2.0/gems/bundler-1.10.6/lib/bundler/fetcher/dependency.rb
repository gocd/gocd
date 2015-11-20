require 'bundler/fetcher/base'

module Bundler
  class Fetcher
    class Dependency < Base
      def api_available?
        downloader.fetch(dependency_api_uri)
      rescue NetworkDownError => e
        raise HTTPError, e.message
      rescue AuthenticationRequiredError
        # We got a 401 from the server. Just fail.
        raise
      rescue HTTPError
      end

      def api_fetcher?
        true
      end

      def specs(gem_names, full_dependency_list = [], last_spec_list = [])
        query_list = gem_names - full_dependency_list

        # only display the message on the first run
        if Bundler.ui.debug?
          Bundler.ui.debug "Query List: #{query_list.inspect}"
        else
          Bundler.ui.info ".", false
        end

        return {remote_uri => last_spec_list} if query_list.empty?

        remote_specs = Bundler::Retry.new("dependency api", AUTH_ERRORS).attempts do
          dependency_specs(query_list)
        end

        spec_list, deps_list = remote_specs
        returned_gems = spec_list.map(&:first).uniq
        specs(deps_list, full_dependency_list + returned_gems, spec_list + last_spec_list)
      rescue HTTPError, MarshalError, GemspecError
        Bundler.ui.info "" unless Bundler.ui.debug? # new line now that the dots are over
        Bundler.ui.debug "could not fetch from the dependency API, trying the full index"
        return nil
      end

      def dependency_specs(gem_names)
        Bundler.ui.debug "Query Gemcutter Dependency Endpoint API: #{gem_names.join(',')}"
        gem_list = []
        deps_list = []

        gem_names.each_slice(Source::Rubygems::API_REQUEST_SIZE) do |names|
          marshalled_deps = downloader.fetch dependency_api_uri(names)
          gem_list += Bundler.load_marshal(marshalled_deps)
        end

        spec_list = gem_list.map do |s|
          dependencies = s[:dependencies].map do |name, requirement|
            dep = well_formed_dependency(name, requirement.split(", "))
            deps_list << dep.name
            dep
          end

          [s[:name], Gem::Version.new(s[:number]), s[:platform], dependencies]
        end

        [spec_list, deps_list.uniq]
      end

      def dependency_api_uri(gem_names = [])
        uri = fetch_uri + "api/v1/dependencies"
        uri.query = "gems=#{URI.encode(gem_names.join(","))}" if gem_names.any?
        uri
      end

      def well_formed_dependency(name, *requirements)
        Gem::Dependency.new(name, *requirements)
      rescue ArgumentError => e
        illformed = 'Ill-formed requirement ["#<YAML::Syck::DefaultKey'
        raise e unless e.message.include?(illformed)
        puts # we shouldn't print the error message on the "fetching info" status line
        raise GemspecError,
          "Unfortunately, the gem #{s[:name]} (#{s[:number]}) has an invalid " \
          "gemspec. \nPlease ask the gem author to yank the bad version to fix " \
          "this issue. For more information, see http://bit.ly/syck-defaultkey."
      end

    end
  end
end
