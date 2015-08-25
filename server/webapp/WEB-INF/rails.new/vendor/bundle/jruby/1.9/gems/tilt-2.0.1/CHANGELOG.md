## master

## 2.0.1 (2014-03-21)

* Fix Tilt::Mapping bug in Ruby 2.1.0 (9589652c569760298f2647f7a0f9ed4f85129f20)
* Fix `tilt --list` (#223, Achrome)
* Fix circular require (#221, amarshall)

## 2.0.0 (2013-11-30)

* Support Pathname in Template#new (#219, kabturek)
* Add Mapping#templates_for (judofyr)
* Support old-style #register (judofyr)
* Add Handlebars as external template engine (#204, judofyr, jimothyGator)
* Add org-ruby as external template engine (#207, judofyr, minad)
* Documentation typo (#208, elgalu)

## 2.0.0.beta1 (2013-07-16)

* Documentation typo (#202, chip)
* Use YARD for documentation (#189, judofyr)
* Add Slim as an external template engine (judofyr)
* Add Tilt.templates_for (#121, judofyr)
* Add Tilt.current_template (#151, judofyr)
* Avoid loading all files in tilt.rb (#160, #187, judofyr)
* Implement lazily required templates classes (#178, #187, judofyr)
* Move #allows_script and default_mime_type to metadata (#187, judofyr)
* Introduce Tilt::Mapping (#187, judofyr)
* Make template compilation thread-safe (#191, judofyr)

## 1.4.1 (2013-05-08)

* Support Arrays in pre/postambles (#193, jbwiv)

## 1.4.0 (2013-05-01)

* Better encoding support

## 1.3.7 (2013-04-09)

* Erubis: Check for the correct constant (#183, mattwildig)
* Don't fail when BasicObject is defined in 1.8 (#182, technobrat, judofyr)

## 1.3.6 (2013-03-17)

* Accept Hash that implements #path as options (#180, lawso017)
* Changed extension for CsvTemplate from '.csv' to '.rcsv' (#177, alexgb)

## 1.3.5 (2013-03-06)

* Fixed extension for PlainTemplate (judofyr)
* Improved local variables regexp (#174, razorinc)
* Added CHANGELOG.md

## 1.3.4 (2013-02-28)

* Support RDoc 4.0 (#168, judofyr)
* Add mention of Org-Mode support (#165, aslakknutsen)
* Add AsciiDoctorTemplate (#163, #164, aslakknutsen)
* Add PlainTextTemplate (nathanaeljones)
* Restrict locals to valid variable names (#158, thinkerbot)
* ERB: Improve trim mode support (#156, ssimeonov)
* Add CSVTemplate (#153, alexgb)
* Remove special case for 1.9.1 (#147, guilleiguaran)
* Add allows\_script? method to Template (#143, bhollis)
* Default to using Redcarpet2 (#139, DAddYE)
* Allow File/Tempfile as filenames (#134, jamesotron)
* Add EtanniTemplate (#131, manveru)
* Support RDoc 3.10 (#112, timfel)
* Always compile templates; remove old source evaluator (rtomayko)
* Less: Options are now being passed to the parser (#106, cowboyd)

