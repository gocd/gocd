# Active Record Deprecated Finders

This gem will be used to extract and deprecate old-style finder option
hashes in Active Record:

``` ruby
Post.find(:all, conditions: { published_on: 2.weeks.ago }, limit: 5)
```

It will be a dependency of Rails 4.0 to provide the deprecated
functionality.

It will be removed as a dependency in Rails 4.1, but users can manually
include it in their Gemfile and it will continue to be maintained until
Rails 5.
