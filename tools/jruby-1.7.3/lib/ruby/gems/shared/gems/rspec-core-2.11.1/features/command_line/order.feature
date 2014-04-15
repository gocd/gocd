Feature: --order (new in rspec-core-2.8)

  Use the `--order` option to tell RSpec how to order the files, groups, and
  examples. Options are `default` and `rand`:

  Default is:

    * files are ordered based on the underlying file system's order (typically
      case-sensitive alpha on *nix OS's and case-insenstive alpha in Windows)
    * groups/examples are loaded in the order in which they are declared

  Use `rand` to randomize the order of files, groups within files, and
  examples within groups.*

  * Nested groups are always run from top-level to bottom-level in order to avoid
    executing `before(:all)` and `after(:all)` hooks more than once, but the order
    of groups at each level is randomized.

  You can also specify a seed

  <h3>Examples</h3>

      --order default
      --order rand
      --order rand:123
      --seed 123 # same as --order rand:123

  The `default` option is only necessary when you have `--order rand` stored in a
  config file (e.g. `.rspec`) and you want to override it from the command line.
