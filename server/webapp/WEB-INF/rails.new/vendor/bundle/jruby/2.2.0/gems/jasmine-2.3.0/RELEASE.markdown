# Releasing Jasmine

## Jasmine core

See release docs in jasmine-core

## Jasmine Gem

1. Update the release notes in `release_notes` - use the Anchorman gem to generate the markdown file and edit accordingly
1. update version in version.rb
  * for release candidates, add ".rc" + number to the end of the appropriate version part
1. commit and push the version update to github
1. Update the gemspec to depend on the latest version of jasmine-core
1. `rake release` - tags the repo with the version, builds the `jasmine` gem, pushes the gem to Rubygems.org. In order to release you will have to ensure you have rubygems creds locally.

