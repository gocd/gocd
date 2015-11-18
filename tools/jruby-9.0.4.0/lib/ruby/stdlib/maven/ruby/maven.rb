#
# Copyright (C) 2013 Christian Meier
#
# Permission is hereby granted, free of charge, to any person obtaining a copy of
# this software and associated documentation files (the "Software"), to deal in
# the Software without restriction, including without limitation the rights to
# use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
# the Software, and to permit persons to whom the Software is furnished to do so,
# subject to the following conditions:
#
# The above copyright notice and this permission notice shall be included in all
# copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
# FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
# COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
# IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
# CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
#
require 'ruby_maven'
require 'fileutils'

module Maven
  module Ruby
    class Maven

      attr_accessor :embedded

      private

      def options_array
        options.collect do |k,v|
          if k =~ /^-D/
            v = "=#{v}" unless v.nil?
            "#{k}#{v}"
          else
            if v.nil?
              "#{k}"
            else
              ["#{k}", "#{v}"]
            end
          end
        end.flatten
      end

      public

      def initialize( project = nil, temp_pom = nil )
        super()
        
        if project
          warn 'deprecated: End Of Life - just tell maven where your (ruby) pom is'
          begin
            require 'maven/tools/model'
            require 'maven/tools/visitor'
          rescue LoadError => e
            warn 'maven-tools gem is not a direct dependency anymore'
            raise e
          end
          f = File.expand_path( temp_pom || '.pom.xml' )
          v = ::Maven::Tools::Visitor.new( File.open( f, 'w' ) )
          # parse project and write out to temp_pom file
          v.accept_project( project )
          # tell maven to use the generated file
          options[ '-f' ] = f
          @embedded = true
        end
      end

      def options
        @options ||= {}
      end

      def <<( v )
        options[ v ] = nil
      end

      def verbose= v
        @verbose = v
      end

      def property(key, value = nil)
        options["-D#{key}"] = value
      end
      alias :[]= :property

      def verbose
        if @verbose.nil?
          @verbose = options.delete('-Dverbose').to_s == 'true'
        else
          @verbose
        end
      end

      def exec(*args)
        if verbose
          puts "mvn #{args.join(' ')}"
        end   
        
        result = RubyMaven.exec( *(args + options_array) )
        if @embedded and not result
          # TODO remove this when the embedded case is gone
          raise "error in executing maven #{result}"
        else
          result
        end
      end

      def method_missing( method, *args )
        method = method.to_s.gsub( /_/, '-' ).to_sym
        exec( *([ method ] + args) )
      end
    end
  end
end
