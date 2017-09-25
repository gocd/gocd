module TestSubcommands

  class Subcommand < Thor
    desc "print_opt", "My method"
    def print_opt
      print options["opt"]
    end
  end

  class Parent < Thor
    class_option "opt"

    desc "sub", "My subcommand"
    subcommand "sub", Subcommand
  end

end
