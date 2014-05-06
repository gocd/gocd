require File.dirname(__FILE__) + '/../rspec_default_values'

class IntegrationSpecGenerator < ModelGenerator
  def manifest
    record do |m|
      m.class_collisions class_path, class_name
      m.template 'integration_spec.rb',  File.join('spec/integration', class_path, "#{class_name.tableize}_spec.rb")
    end
  end
end
