require 'webrick/cookie'
require 'net/http'
require 'net/https'
require 'rubyforge/cookie_manager'

# clean up warnings caused by web servers that send down 2 digit years
class Time
  class << self
    alias :old_utc :utc

    def utc(*args)
      century = Time.now.year / 100 * 100
      args[0] += century if args[0] < 100
      old_utc(*args)
    end
  end
end unless Time.respond_to? :old_utc

# clean up "using default DH parameters" warning for https
class Net::HTTP
  alias :old_use_ssl= :use_ssl=
  def use_ssl= flag
    self.old_use_ssl = flag
    @ssl_context.tmp_dh_callback = proc {} if @ssl_context
  end
end unless Net::HTTP.public_instance_methods.include? "old_use_ssl="

class RubyForge
  class Client
    attr_accessor :debug_dev, :ssl_verify_mode, :agent_class

    def initialize(proxy = nil)
      @debug_dev       = nil
      @ssl_verify_mode = OpenSSL::SSL::VERIFY_NONE
      @cookie_manager  = CookieManager.new
      @agent_class     = Net::HTTP
    end

    def cookie_store
      @cookie_manager
    end

    def cookie_store=(path)
      @cookie_manager = CookieManager.load(path)
    end

    def post_content(uri, form = {}, headers = {})
      uri = URI.parse(uri) unless uri.is_a?(URI)
      request = agent_class::Post.new(uri.request_uri)
      execute(request, uri, form, headers)
    end

    def get_content(uri, query = {}, headers = {})
      uri = URI.parse(uri) unless uri.is_a?(URI)
      request = agent_class::Get.new(uri.request_uri)
      execute(request, uri, query, headers)
    end

    def execute(request, uri, parameters = {}, headers = {})
      {
        'content-type' => 'application/x-www-form-urlencoded'
      }.merge(headers).each { |k,v| request[k] = v }

      @cookie_manager[uri].each { |k,v|
        request['Cookie'] = v.to_s
      } if @cookie_manager[uri]

      http = agent_class.new( uri.host, uri.port )

      if uri.scheme == 'https'
        http.use_ssl      = true
        http.verify_mode  = OpenSSL::SSL::VERIFY_NONE
      end

      request_data = case request['Content-Type']
                     when /boundary=(.*)$/
                       boundary_data_for($1, parameters)
                     else
                       query_string_for(parameters)
                     end
      request['Content-Length'] = request_data.length.to_s

      response = http.request(request, request_data)
      (response.get_fields('Set-Cookie') || []).each do |raw_cookie|
        WEBrick::Cookie.parse_set_cookies(raw_cookie).each { |baked_cookie|
          baked_cookie.domain ||= url.host
          baked_cookie.path   ||= url.path
          @cookie_manager.add(uri, baked_cookie)
        }
      end

      return response.body if response.class <= Net::HTTPSuccess

      if response.class <= Net::HTTPRedirection
        location = response['Location']
        unless location =~ /^http/
          location = "#{uri.scheme}://#{uri.host}#{location}"
        end
        uri = URI.parse(location)

        execute(agent_class::Get.new(uri.request_uri), uri)
      end
    end

    def boundary_data_for(boundary, parameters)
      parameters.sort_by {|k,v| k.to_s }.map { |k,v|
        parameter = "--#{boundary}\r\nContent-Disposition: form-data; name=\"" +
            WEBrick::HTTPUtils.escape_form(k.to_s) + "\""

        if v.respond_to? :path
          parameter += "; filename=\"#{File.basename(v.path)}\"\r\n"
          parameter += "Content-Transfer-Encoding: binary\r\n"
          parameter += "Content-Type: text/plain"
        end
        parameter += "\r\n\r\n"

        if v.respond_to? :path
          parameter += v.read
        else
          parameter += v.to_s
        end

        parameter
      }.join("\r\n") + "\r\n--#{boundary}--\r\n"
    end

    def query_string_for(parameters)
      parameters.sort_by {|k,v| k.to_s }.map { |k,v|
        k && [  WEBrick::HTTPUtils.escape_form(k.to_s),
                WEBrick::HTTPUtils.escape_form(v.to_s) ].join('=')
      }.compact.join('&')
    end

    def save_cookie_store
      @cookie_manager.save!
    end
  end
end
