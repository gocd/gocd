require 'yaml'

class RubyForge
  class CookieManager
    class << self
      def load(path)
        cm = YAML.load_file(path) rescue CookieManager.new(path)
        cm = CookieManager.new(path) unless cm.is_a?(CookieManager)
        cm.clean_stale_cookies
      end
    end

    attr_accessor :cookies_file
    def initialize(cookies_file = nil)
      @jar = Hash.new { |hash,domain_name|
        hash[domain_name.downcase] = {}
      }
      @cookies_file = cookies_file
    end

    def [](uri)
      # FIXME we need to do more matching on hostname....  This is not
      # bulletproof
      uri = (URI === uri ? uri.host : uri).downcase 
      @jar[uri] ||= {}
    end

    def clear uri
      self[uri].clear
      self.save!
    end

    def empty?
      @jar.empty? || @jar.all? { |k,v| v.empty? }
    end

    def save!
      clean_stale_cookies
      File.open(@cookies_file, 'wb') { |f|
        f.write(YAML.dump(self))
      }
    end

    def add(uri, cookie)
      no_dot_domain = cookie.domain.gsub(/^\./, '')
      return unless uri.host =~ /#{no_dot_domain}$/i
      @jar[no_dot_domain][cookie.name] = cookie
      clean_stale_cookies
    end

    def clean_stale_cookies
      @jar.each do |domain, cookies|
        cookies.each do |name, cookie|
          cookies.delete(name) if cookie.expires < Time.now
        end
      end
      self
    end
  end
end
