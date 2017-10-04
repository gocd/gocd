RSpec.configure do |config|
  def config.infer_spec_type_from_file_location!
    @infer_spec_type_from_file_location = true
  end

  def config.infer_spec_type_from_file_location?
    @infer_spec_type_from_file_location ||= false
  end

  config.before do
    unless config.infer_spec_type_from_file_location?
      RSpec.warn_deprecation(<<-EOS.gsub(/^\s+\|/,''))
       |rspec-rails 3 will no longer automatically infer an example group's spec type
       |from the file location. You can explicitly opt-in to this feature using this
       |snippet:
       |
       |RSpec.configure do |config|
       |  config.infer_spec_type_from_file_location!
       |end
       |
       |If you wish to manually label spec types via metadata you can safely ignore
       |this warning and continue upgrading to RSpec 3 without addressing it.
      EOS
    end
  end
end
