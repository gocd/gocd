module ActionController #:nodoc:
  class TestResponse #:nodoc:
    attr_writer :controller_path

    def capture(name)
      template.instance_variable_get "@content_for_#{name.to_s}"
    end
    
    if ::Rails::VERSION::STRING < "2.3"
      def [](name)
        Kernel.warn <<-WARNING
DEPRECATION NOTICE: [](name) as an alias for capture(name) (TestResponse
extension in rspec-rails) is deprecated and will not be defined by rspec-rails
when working with rails >= 2.3.0. It will also be removed entirely from
a future version of rspec-rails.
WARNING
        capture(name)
      end
    end
  end
end
