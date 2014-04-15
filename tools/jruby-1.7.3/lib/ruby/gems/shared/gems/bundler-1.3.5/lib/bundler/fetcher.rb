require 'bundler/vendored_persistent'

module Bundler

  # Handles all the fetching with the rubygems server
  class Fetcher
    # How many redirects to allew in one request
    REDIRECT_LIMIT = 5
    # how long to wait for each gemcutter API call
    API_TIMEOUT = 10

    # This error is raised if the API returns a 413 (only printed in verbose)
    class FallbackError < HTTPError; end
    # This is the error raised if OpenSSL fails the cert verification
    class CertificateFailureError < HTTPError
      def initialize(remote_uri)
        super "Could not verify the SSL certificate for #{remote_uri}.\nThere" \
          " is a chance you are experiencing a man-in-the-middle attack, but" \
          " most likely your system doesn't have the CA certificates needed" \
          " for verification. For information about OpenSSL certificates, see" \
          " bit.ly/ruby-ssl. To connect without using SSL, edit your Gemfile" \
          " sources and change 'https' to 'http'."
      end
    end
    # This is the error raised when a source is HTTPS and OpenSSL didn't load
    class SSLError < HTTPError
      def initialize(msg = nil)
        super msg || "Could not load OpenSSL.\n" \
            "You must recompile Ruby with OpenSSL support or change the sources in your " \
            "Gemfile from 'https' to 'http'. Instructions for compiling with OpenSSL " \
            "using RVM are available at rvm.io/packages/openssl."
      end
    end

    class << self
      attr_accessor :disable_endpoint

      @@spec_fetch_map ||= {}

      def fetch(spec)
        spec, uri = @@spec_fetch_map[spec.full_name]
        if spec
          path = download_gem_from_uri(spec, uri)
          s = Bundler.rubygems.spec_from_gem(path, Bundler.settings["trust-policy"])
          spec.__swap__(s)
        end
      end

      def download_gem_from_uri(spec, uri)
        spec.fetch_platform

        download_path = Bundler.requires_sudo? ? Bundler.tmp : Bundler.rubygems.gem_dir
        gem_path = "#{Bundler.rubygems.gem_dir}/cache/#{spec.full_name}.gem"

        FileUtils.mkdir_p("#{download_path}/cache")
        Bundler.rubygems.download_gem(spec, uri, download_path)

        if Bundler.requires_sudo?
          Bundler.mkdir_p "#{Bundler.rubygems.gem_dir}/cache"
          Bundler.sudo "mv #{Bundler.tmp}/cache/#{spec.full_name}.gem #{gem_path}"
        end

        gem_path
      end
    end

    def initialize(remote_uri)
      @remote_uri = remote_uri
      @public_uri = remote_uri.dup
      @public_uri.user, @public_uri.password = nil, nil # don't print these
      if defined?(Net::HTTP::Persistent)
        @connection = Net::HTTP::Persistent.new 'bundler', :ENV
        @connection.verify_mode = (Bundler.settings[:ssl_verify_mode] ||
          OpenSSL::SSL::VERIFY_PEER)
        @connection.cert_store = bundler_cert_store
      else
        raise SSLError if @remote_uri.scheme == "https"
        @connection = Net::HTTP.new(@remote_uri.host, @remote_uri.port)
      end
      @connection.read_timeout = API_TIMEOUT

      Socket.do_not_reverse_lookup = true
    end

    # fetch a gem specification
    def fetch_spec(spec)
      spec = spec - [nil, 'ruby', '']
      spec_file_name = "#{spec.join '-'}.gemspec.rz"

      uri = URI.parse("#{@remote_uri}#{Gem::MARSHAL_SPEC_DIR}#{spec_file_name}")

      spec_rz = (uri.scheme == "file") ? Gem.read_binary(uri.path) : fetch(uri)
      Bundler.load_marshal Gem.inflate(spec_rz)
    rescue MarshalError => e
      raise HTTPError, "Gemspec #{spec} contained invalid data.\n" \
        "Your network or your gem server is probably having issues right now."
    end

    # return the specs in the bundler format as an index
    def specs(gem_names, source)
      index = Index.new
      use_full_source_index = !gem_names || @remote_uri.scheme == "file" || Bundler::Fetcher.disable_endpoint

      if gem_names && use_api
        Bundler.ui.info "Fetching gem metadata from #{@public_uri}", Bundler.ui.debug?
        specs = fetch_remote_specs(gem_names)
        # new line now that the dots are over
        Bundler.ui.info "" if specs && !Bundler.ui.debug?
      end

      if specs.nil?
        # API errors mean we should treat this as a non-API source
        @use_api = false

        Bundler.ui.info "Fetching source index from #{@public_uri}"
        specs = fetch_all_remote_specs
      end

      specs[@remote_uri].each do |name, version, platform, dependencies|
        next if name == 'bundler'
        spec = nil
        if dependencies
          spec = EndpointSpecification.new(name, version, platform, dependencies)
        else
          spec = RemoteSpecification.new(name, version, platform, self)
        end
        spec.source = source
        @@spec_fetch_map[spec.full_name] = [spec, @remote_uri]
        index << spec
      end

      index
    rescue CertificateFailureError => e
      Bundler.ui.info "" if gem_names && use_api # newline after dots
      raise e
    end

    # fetch index
    def fetch_remote_specs(gem_names, full_dependency_list = [], last_spec_list = [])
      query_list = gem_names - full_dependency_list

      # only display the message on the first run
      if Bundler.ui.debug?
        Bundler.ui.debug "Query List: #{query_list.inspect}"
      else
        Bundler.ui.info ".", false
      end

      return {@remote_uri => last_spec_list} if query_list.empty?

      spec_list, deps_list = fetch_dependency_remote_specs(query_list)
      returned_gems = spec_list.map {|spec| spec.first }.uniq

      fetch_remote_specs(deps_list, full_dependency_list + returned_gems, spec_list + last_spec_list)
    # fall back to the legacy index in the following cases
    # 1. Gemcutter Endpoint doesn't return a 200
    # 2. Marshal blob doesn't load properly
    # 3. One of the YAML gemspecs has the Syck::DefaultKey problem
    rescue HTTPError, MarshalError, GemspecError => e
      @use_api = false

      # new line now that the dots are over
      Bundler.ui.info "" unless Bundler.ui.debug?

      Bundler.ui.debug "Error during API request. #{e.class}: #{e.message}"
      Bundler.ui.debug e.backtrace.join("  ")

      return nil
    end

    def use_api
      return @use_api if defined?(@use_api)

      if @remote_uri.scheme == "file" || Bundler::Fetcher.disable_endpoint
        @use_api = false
      elsif fetch(dependency_api_uri)
        @use_api = true
      end
    rescue HTTPError
      @use_api = false
    end

    def inspect
      "#<#{self.class}:0x#{object_id} uri=#{@public_uri.to_s}>"
    end

  private

    HTTP_ERRORS = [
      Timeout::Error, EOFError, SocketError,
      Errno::EINVAL, Errno::ECONNRESET, Errno::ETIMEDOUT, Errno::EAGAIN,
      Net::HTTPBadResponse, Net::HTTPHeaderSyntaxError, Net::ProtocolError
    ]
    HTTP_ERRORS << Net::HTTP::Persistent::Error if defined?(Net::HTTP::Persistent)

    def fetch(uri, counter = 0)
      raise HTTPError, "Too many redirects" if counter >= REDIRECT_LIMIT

      begin
        Bundler.ui.debug "Fetching from: #{uri}"
        if defined?(Net::HTTP::Persistent)
          response = @connection.request(uri)
        else
          req = Net::HTTP::Get.new uri.request_uri
          req.basic_auth(uri.user, uri.password) if uri.user && uri.password
          response = @connection.request(req)
        end
      rescue OpenSSL::SSL::SSLError
        raise CertificateFailureError.new(@public_uri)
      rescue *HTTP_ERRORS
        raise HTTPError, "Network error while fetching #{uri}"
      end

      case response
      when Net::HTTPRedirection
        Bundler.ui.debug("HTTP Redirection")
        new_uri = URI.parse(response["location"])
        if new_uri.host == uri.host
          new_uri.user = uri.user
          new_uri.password = uri.password
        end
        fetch(new_uri, counter + 1)
      when Net::HTTPSuccess
        Bundler.ui.debug("HTTP Success")
        response.body
      when Net::HTTPRequestEntityTooLarge
        raise FallbackError, response.body
      else
        raise HTTPError, "#{response.class}: #{response.body}"
      end
    end

    def dependency_api_uri(gem_names = [])
      url = "#{@remote_uri}api/v1/dependencies"
      url << "?gems=#{URI.encode(gem_names.join(","))}" if gem_names.any?
      URI.parse(url)
    end

    # fetch from Gemcutter Dependency Endpoint API
    def fetch_dependency_remote_specs(gem_names)
      Bundler.ui.debug "Query Gemcutter Dependency Endpoint API: #{gem_names.join(',')}"
      marshalled_deps = fetch dependency_api_uri(gem_names)
      gem_list = Bundler.load_marshal(marshalled_deps)
      deps_list = []

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

    # fetch from modern index: specs.4.8.gz
    def fetch_all_remote_specs
      Bundler.rubygems.sources = ["#{@remote_uri}"]
      Bundler.rubygems.fetch_all_remote_specs
    rescue Gem::RemoteFetcher::FetchError, OpenSSL::SSL::SSLError => e
      if e.message.match("certificate verify failed")
        raise CertificateFailureError.new(@public_uri)
      else
        Bundler.ui.trace e
        raise HTTPError, "Could not fetch specs from #{@public_uri}"
      end
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

    def bundler_cert_store
      store = OpenSSL::X509::Store.new
      if Bundler.settings[:ssl_ca_cert]
        if File.directory? Bundler.settings[:ssl_ca_cert]
          store.add_path Bundler.settings[:ssl_ca_cert]
        else
          store.add_file Bundler.settings[:ssl_ca_cert]
        end
      else
        store.set_default_paths
        certs = File.expand_path("../ssl_certs/*.pem", __FILE__)
        Dir.glob(certs).each { |c| store.add_file c }
      end
      store
    end

  end
end
