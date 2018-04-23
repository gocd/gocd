require "rails/generators"

module Rails
  module Command
    class GenerateCommand < Base # :nodoc:
      no_commands do
        def help
          require_application_and_environment!
          load_generators

          Rails::Generators.help self.class.command_name
        end
      end

      def perform(*)
        generator = args.shift
        return help unless generator

        require_application_and_environment!
        load_generators

        ARGV.shift

        Rails::Generators.invoke generator, args, behavior: :invoke, destination_root: Rails::Command.root
      end
    end
  end
end
