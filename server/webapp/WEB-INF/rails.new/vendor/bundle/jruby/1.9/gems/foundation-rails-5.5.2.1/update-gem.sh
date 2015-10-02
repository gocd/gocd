#! /bin/sh

# clean old version
rm -rf vendor
mkdir -p vendor/assets/javascripts/vendor vendor/assets/stylesheets

# update assets
bower install
cp bower_components/modernizr/modernizr.js vendor/assets/javascripts/vendor/.
cp -R bower_components/foundation/js/foundation/ vendor/assets/javascripts/foundation/
cp -R bower_components/foundation/scss/* vendor/assets/stylesheets/

# create vendor/assets/javascripts/foundation.js (rails inclusions //=require foundation, ...)
cd vendor/assets/javascripts
for f in foundation/*.js; do echo "//= require $f" | sed 's/.js//' >> foundation.js ; done

# echo "Now update version.rb"

