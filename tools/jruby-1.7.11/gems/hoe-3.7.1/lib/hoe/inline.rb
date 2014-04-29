require 'rbconfig'

##
# Hoe allows bundling of pre-compiled extensions in the +package+ task.
#
# To create a package for your current platform:
#
#   rake package INLINE=1
#
# This will force Hoe analize your +Inline+ already compiled
# extensions and include them in your gem.
#
# If somehow you need to force a specific platform:
#
#   rake package INLINE=1 FORCE_PLATFORM=mswin32
#
# This will set the +Gem::Specification+ platform to the one indicated in
# +FORCE_PLATFORM+ (instead of default Gem::Platform::CURRENT)

module Hoe::Inline
  def initialize_inline # :nodoc:
    clean_globs << File.expand_path("~/.ruby_inline")
  end

  ##
  # Activate the inline dependencies.

  def activate_inline_deps
    dependency "RubyInline", "~> 3.9"
  end

  ##
  # Define tasks for plugin.

  def define_inline_tasks
    task :test => :clean

    if ENV['INLINE'] then
      s.platform = ENV['FORCE_PLATFORM'] || Gem::Platform::CURRENT

      # Try collecting Inline extensions for +name+
      if defined?(Inline) then
        directory 'lib/inline'

        dlext = RbConfig::CONFIG['DLEXT']

        Inline.registered_inline_classes.each do |cls|
          name = cls.name.gsub(/::/, '')
          # name of the extension is CamelCase
          alternate_name = if name =~ /[A-Z]/ then
                             name.gsub(/([A-Z])/, '_\1').downcase.sub(/^_/, '')
                           elsif name =~ /_/ then
                             name.capitalize.gsub(/_([a-z])/) { $1.upcase }
                           end
          extensions = Dir.chdir(Inline::directory) {
            Dir["Inline_{#{name},#{alternate_name}}_*.#{dlext}"]
          }

          extensions.each do |ext|
            # add the inlined extension to the spec files
            s.files += ["lib/inline/#{ext}"]

            # include the file in the tasks
            file "lib/inline/#{ext}" => ["lib/inline"] do
              cp File.join(Inline::directory, ext), "lib/inline"
            end
          end
        end
      end
    end
  end
end
