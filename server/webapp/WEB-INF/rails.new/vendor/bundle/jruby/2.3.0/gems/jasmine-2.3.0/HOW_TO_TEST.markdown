To test changes to the jasmine-gem:

* Export RAILS_VERSION as either "pojs" (Plain Old JavaScript -- IE, no rails bindings), or "rails3" to test environments other than Rails 4.
* Delete `Gemfile.lock`
* Clear out your current gemset
* exec a `bundle install`
* `rake` until specs are green
* Repeat
* Check in
