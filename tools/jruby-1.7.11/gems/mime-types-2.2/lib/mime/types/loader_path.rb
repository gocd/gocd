# -*- ruby encoding: utf-8 -*-

class MIME::Types::Loader
  # The path that will be used for loading the MIME::Types data. The default
  # location is __FILE__/../../../../data, which is where the data lives
  # in the gem installation of the mime-types library.
  #
  # The MIME::Types::Loader will load all YAML files contained in this path.
  # By convention, there is one file for each media type (e.g.,
  # application.yml, audio.yml, etc.).
  #
  # System repackagers note: this is the constant that you would change if
  # you repackage mime-types for your system. It is recommended that the
  # path be something like /usr/share/ruby/mime-types/.
  PATH = File.expand_path('../../../../data', __FILE__)
end
