require 'action_dispatch/http/request'
require 'active_support/core_ext/uri'
require 'active_support/core_ext/array/extract_options'
require 'rack/utils'
require 'action_controller/metal/exceptions'

module ActionDispatch
  module Routing
    class Redirect # :nodoc:
      attr_reader :status, :block

      def initialize(status, block)
        @status = status
        @block  = block
      end

      def call(env)
        req = Request.new(env)

        # If any of the path parameters has a invalid encoding then
        # raise since it's likely to trigger errors further on.
        req.symbolized_path_parameters.each do |key, value|
          unless value.valid_encoding?
            raise ActionController::BadRequest, "Invalid parameter: #{key} => #{value}"
          end
        end

        uri = URI.parse(path(req.symbolized_path_parameters, req))
        
        unless uri.host
          if relative_path?(uri.path)
            uri.path = "#{req.script_name}/#{uri.path}"
          elsif uri.path.empty?
            uri.path = req.script_name.empty? ? "/" : req.script_name
          end
        end
          
        uri.scheme ||= req.scheme
        uri.host   ||= req.host
        uri.port   ||= req.port unless req.standard_port?

        body = %(<html><body>You are being <a href="#{ERB::Util.h(uri.to_s)}">redirected</a>.</body></html>)

        headers = {
          'Location' => uri.to_s,
          'Content-Type' => 'text/html',
          'Content-Length' => body.length.to_s
        }

        [ status, headers, [body] ]
      end

      def path(params, request)
        block.call params, request
      end

      def inspect
        "redirect(#{status})"
      end

      private
        def relative_path?(path)
          path && !path.empty? && path[0] != '/'
        end
    end

    class PathRedirect < Redirect
      def path(params, request)
        (params.empty? || !block.match(/%\{\w*\}/)) ? block : (block % escape(params))
      end

      def inspect
        "redirect(#{status}, #{block})"
      end

      private
        def escape(params)
          Hash[params.map{ |k,v| [k, Rack::Utils.escape(v)] }]
        end
    end

    class OptionRedirect < Redirect # :nodoc:
      alias :options :block

      def path(params, request)
        url_options = {
          :protocol => request.protocol,
          :host     => request.host,
          :port     => request.optional_port,
          :path     => request.path,
          :params   => request.query_parameters
        }.merge! options

        if !params.empty? && url_options[:path].match(/%\{\w*\}/)
          url_options[:path] = (url_options[:path] % escape_path(params))
        end

        unless options[:host] || options[:domain]
          if relative_path?(url_options[:path])
            url_options[:path] = "/#{url_options[:path]}"
            url_options[:script_name] = request.script_name
          elsif url_options[:path].empty?
            url_options[:path] = request.script_name.empty? ? "/" : ""
            url_options[:script_name] = request.script_name
          end
        end
        
        ActionDispatch::Http::URL.url_for url_options
      end

      def inspect
        "redirect(#{status}, #{options.map{ |k,v| "#{k}: #{v}" }.join(', ')})"
      end

      private
        def escape_path(params)
          Hash[params.map{ |k,v| [k, URI.parser.escape(v)] }]
        end
    end

    module Redirection

      # Redirect any path to another path:
      #
      #   get "/stories" => redirect("/posts")
      #
      # You can also use interpolation in the supplied redirect argument:
      #
      #   get 'docs/:article', to: redirect('/wiki/%{article}')
      #
      # Note that if you return a path without a leading slash then the url is prefixed with the
      # current SCRIPT_NAME environment variable. This is typically '/' but may be different in
      # a mounted engine or where the application is deployed to a subdirectory of a website.
      #
      # Alternatively you can use one of the other syntaxes:
      #
      # The block version of redirect allows for the easy encapsulation of any logic associated with
      # the redirect in question. Either the params and request are supplied as arguments, or just
      # params, depending of how many arguments your block accepts. A string is required as a
      # return value.
      #
      #   get 'jokes/:number', to: redirect { |params, request|
      #     path = (params[:number].to_i.even? ? "wheres-the-beef" : "i-love-lamp")
      #     "http://#{request.host_with_port}/#{path}"
      #   }
      #
      # Note that the +do end+ syntax for the redirect block wouldn't work, as Ruby would pass
      # the block to +get+ instead of +redirect+. Use <tt>{ ... }</tt> instead.
      #
      # The options version of redirect allows you to supply only the parts of the url which need
      # to change, it also supports interpolation of the path similar to the first example.
      #
      #   get 'stores/:name',       to: redirect(subdomain: 'stores', path: '/%{name}')
      #   get 'stores/:name(*all)', to: redirect(subdomain: 'stores', path: '/%{name}%{all}')
      #
      # Finally, an object which responds to call can be supplied to redirect, allowing you to reuse
      # common redirect routes. The call method must accept two arguments, params and request, and return
      # a string.
      #
      #   get 'accounts/:name' => redirect(SubdomainRedirector.new('api'))
      #
      def redirect(*args, &block)
        options = args.extract_options!
        status  = options.delete(:status) || 301
        path    = args.shift

        return OptionRedirect.new(status, options) if options.any?
        return PathRedirect.new(status, path) if String === path

        block = path if path.respond_to? :call
        raise ArgumentError, "redirection argument not supported" unless block
        Redirect.new status, block
      end
    end
  end
end
