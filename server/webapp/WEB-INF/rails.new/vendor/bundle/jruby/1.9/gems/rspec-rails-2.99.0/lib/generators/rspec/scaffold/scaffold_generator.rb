require 'generators/rspec'
require 'rails/generators/resource_helpers'

module Rspec
  module Generators
    class ScaffoldGenerator < Base
      include ::Rails::Generators::ResourceHelpers
      source_paths << File.expand_path("../../helper/templates", __FILE__)
      argument :attributes, :type => :array, :default => [], :banner => "field:type field:type"

      class_option :orm, :desc => "ORM used to generate the controller"
      class_option :template_engine, :desc => "Template engine to generate view files"
      class_option :singleton, :type => :boolean, :desc => "Supply to create a singleton controller"

      class_option :controller_specs, :type => :boolean, :default => true,  :desc => "Generate controller specs"
      class_option :view_specs,       :type => :boolean, :default => true,  :desc => "Generate view specs"
      class_option :webrat,           :type => :boolean, :default => false, :desc => "Use webrat methods/matchers"
      class_option :webrat_matchers,  :type => :boolean, :default => false, :desc => "Use webrat methods/matchers (deprecated - use --webrat)"
      class_option :helper_specs,     :type => :boolean, :default => true,  :desc => "Generate helper specs"
      class_option :routing_specs,    :type => :boolean, :default => true,  :desc => "Generate routing specs"

      def generate_controller_spec
        return unless options[:controller_specs]

        RSpec.deprecate("the --webrat-matchers option", :replacement => nil) if options[:webrat_matchers]
        RSpec.deprecate("the --webrat option", :replacement => nil) if options[:webrat]

        template 'controller_spec.rb',
                 File.join('spec/controllers', controller_class_path, "#{controller_file_name}_controller_spec.rb")
      end

      def generate_view_specs
        return unless options[:view_specs] && options[:template_engine]

        copy_view :edit
        copy_view :index unless options[:singleton]
        copy_view :new
        copy_view :show
      end

      def generate_routing_spec
        return unless options[:routing_specs]

        template 'routing_spec.rb',
          File.join('spec/routing', controller_class_path, "#{controller_file_name}_routing_spec.rb")
      end

      hook_for :integration_tool, :as => :integration

      protected

        def webrat?
          options[:webrat] || options[:webrat_matchers]
        end

        def copy_view(view)
          template "#{view}_spec.rb",
                   File.join("spec/views", controller_file_path, "#{view}.html.#{options[:template_engine]}_spec.rb")
        end

        def example_valid_attributes
          # Only take the first attribute so this hash does not become unweildy and large in the
          # generated controller spec. It is the responsibility of the user to keep the the valid
          # attributes method up-to-date as they add validations.
          @example_valid_attributes ||=
            if attributes.any?
              { attributes.first.name => attributes.first.default.to_s }
            else
              { }
            end
        end

        def example_invalid_attributes
          @example_invalid_attributes ||=
            if attributes.any?
              { attributes.first.name => "invalid value" }
            else
              { }
            end
        end

        def example_params_for_update
          @example_params_for_update ||=
            if example_valid_attributes.any?
              example_valid_attributes
            else
              { "these" => "params" }
            end
        end

        def formatted_hash(hash)
          formatted = hash.inspect
          formatted.gsub!("{", "{ ")
          formatted.gsub!("}", " }")
          formatted.gsub!("=>", " => ")
          formatted
        end

        # support for namespaced-resources
        def ns_file_name
          ns_parts.empty? ? file_name : "#{ns_parts[0].underscore}_#{ns_parts[1].singularize.underscore}"
        end

        # support for namespaced-resources
        def ns_table_name
          ns_parts.empty? ? table_name : "#{ns_parts[0].underscore}/#{ns_parts[1].tableize}"
        end

        def ns_parts
          @ns_parts ||= begin
                          matches = ARGV[0].to_s.match(/\A(\w+)(?:\/|::)(\w+)/)
                          matches ? [matches[1], matches[2]] : []
                        end
        end

        # Returns the name of the mock. For example, if the file name is user,
        # it returns mock_user.
        #
        # If a hash is given, it uses the hash key as the ORM method and the
        # value as response. So, for ActiveRecord and file name "User":
        #
        #   mock_file_name(:save => true)
        #   #=> mock_user(:save => true)
        #
        # If another ORM is being used and another method instead of save is
        # called, it will be the one used.
        #
        def mock_file_name(hash=nil)
          if hash
            method, and_return = hash.to_a.first
            method = orm_instance.send(method).split('.').last.gsub(/\(.*?\)/, '')
            "mock_#{ns_file_name}(:#{method} => #{and_return})"
          else
            "mock_#{ns_file_name}"
          end
        end

        # Receives the ORM chain and convert to expects. For ActiveRecord:
        #
        #   should! orm_class.find(User, "37")
        #   #=> User.should_receive(:find).with(37)
        #
        # For Datamapper:
        #
        #   should! orm_class.find(User, "37")
        #   #=> User.should_receive(:get).with(37)
        #
        def should_receive(chain)
          stub_or_should_chain(:should_receive, chain)
        end

        # Receives the ORM chain and convert to stub. For ActiveRecord:
        #
        #   stub orm_class.find(User, "37")
        #   #=> User.stub(:find).with(37)
        #
        # For Datamapper:
        #
        #   stub orm_class.find(User, "37")
        #   #=> User.stub(:get).with(37)
        #
        def stub(chain)
          stub_or_should_chain(:stub, chain)
        end

        def stub_or_should_chain(mode, chain)
          receiver, method = chain.split(".")
          method.gsub!(/\((.*?)\)/, '')

          response = "#{receiver}.#{mode}(:#{method})"
          response << ".with(#{$1})" unless $1.blank?
          response
        end

        def value_for(attribute)
          case attribute.type
          when :string
            "#{attribute.name.titleize}".inspect
          when :integer
            @attribute_id_map ||= {}
            @attribute_id_map[attribute] ||= @attribute_id_map.keys.size.next.to_s
          else
            attribute.default.inspect
          end
        end

        def banner
          self.class.banner
        end

    end
  end
end
