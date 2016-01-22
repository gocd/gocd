module ApiV1
  module Config
    module Materials
      class ScmMaterialRepresenter < ApiV1::BaseRepresenter
        alias_method :material_config, :represented

        property :url
        property :folder, as: :destination, skip_parse: SkipParseOnBlank
        property :filter,
                 decorator:  ApiV1::Config::Materials::FilterRepresenter,
                 class:      com.thoughtworks.go.config.materials.Filter,
                 skip_parse: SkipParseOnBlank
        property :name, case_insensitive_string: true, skip_parse: SkipParseOnBlank
        property :auto_update

      end
    end
  end
end
