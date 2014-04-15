# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with this
# work for additional information regarding copyright ownership.  The ASF
# licenses this file to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
# License for the specific language governing permissions and limitations under
# the License.

module Buildr

  # Collection of options for controlling Buildr.
  class Options

    # We use this to present environment variable as arrays.
    class EnvArray < Array #:nodoc:

      def initialize(name)
        @name = name.upcase
        replace((ENV[@name] || ENV[@name.downcase] || '').split(/\s*,\s*/).reject(&:empty?))
      end

      (Array.instance_methods - Object.instance_methods - Enumerable.instance_methods - ['each']).sort.each do |method|
        class_eval %{def #{method}(*args, &block) ; result = super ; write_envarray ; result ; end}
      end

    private

      def write_envarray
        ENV[@name.downcase] = nil
        ENV[@name] = map(&:to_s).join(',')
      end

    end


    # Wraps around the proxy environment variables:
    # * :http -- HTTP_PROXY
    # * :https -- HTTPS_PROXY
    # * :exclude -- NO_PROXY
    class Proxies

      # Returns the HTTP_PROXY URL.
      def http
        ENV['HTTP_PROXY'] || ENV['http_proxy']
      end

      # Sets the HTTP_PROXY URL.
      def http=(url)
        ENV['http_proxy'] = nil
        ENV['HTTP_PROXY'] = url
      end

      # Returns the HTTPS_PROXY URL.
      def https
        ENV['HTTPS_PROXY'] || ENV['https_proxy']
      end

      # Sets the HTTPS_PROXY URL.
      def https=(url)
        ENV['https_proxy'] = nil
        ENV['HTTPS_PROXY'] = url
      end

      # Returns list of hosts to exclude from proxying (NO_PROXY).
      def exclude
        @exclude ||= EnvArray.new('NO_PROXY')
      end

      # Sets list of hosts to exclude from proxy (NO_PROXY). Accepts host name, array of names,
      # or nil to clear the list.
      def exclude=(url)
        exclude.clear
        exclude.concat [url].flatten if url
        exclude
      end

    end

    # :call-seq:
    #   proxy => options
    #
    # Returns the proxy options. Currently supported options are:
    # * :http -- HTTP proxy for use when downloading.
    # * :exclude -- Do not use proxy for these hosts/domains.
    #
    # For example:
    #   options.proxy.http = 'http://proxy.acme.com:8080'
    # You can also set it using the environment variable HTTP_PROXY.
    #
    # You can exclude individual hosts from being proxied, or entire domains, for example:
    #   options.proxy.exclude = 'optimus'
    #   options.proxy.exclude = ['optimus', 'prime']
    #   options.proxy.exclude << '*.internal'
    def proxy
      @proxy ||= Proxies.new
    end

  end


  class << self

    # :call-seq:
    #   options => Options
    #
    # Returns the Buildr options. See Options.
    def options
      @options ||= Options.new
    end

  end

  # :call-seq:
  #   options => Options
  #
  # Returns the Buildr options. See Options.
  def options
    Buildr.options
  end

end
