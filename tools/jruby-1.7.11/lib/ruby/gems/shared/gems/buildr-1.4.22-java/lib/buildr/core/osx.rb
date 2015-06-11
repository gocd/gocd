# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with this
# work for additional information regarding copyright ownership.  The ASF
# licenses this file to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
# License for the specific language governing permissions and limitations under
# the License.

# Let's see if we can use Growl.  Must be running from console in verbose mode.
if $stdout.isatty && verbose
  def growl_notify(type, title, message)
    begin
      # Loading Ruby Cocoa can slow the build down (hooks on Object class), so we're
      # saving the best for last and only requiring it at the very end.
      require 'osx/cocoa'
      icon = OSX::NSApplication.sharedApplication.applicationIconImage
      icon = OSX::NSImage.alloc.initWithContentsOfFile(File.join(File.dirname(__FILE__), '../resources/buildr.icns'))

      # Register with Growl, that way you can turn notifications on/off from system preferences.
      OSX::NSDistributedNotificationCenter.defaultCenter.
        postNotificationName_object_userInfo_deliverImmediately(:GrowlApplicationRegistrationNotification, nil,
          { :ApplicationName=>'Buildr', :AllNotifications=>['Completed', 'Failed'],
            :ApplicationIcon=>icon.TIFFRepresentation }, true)

      OSX::NSDistributedNotificationCenter.defaultCenter.
        postNotificationName_object_userInfo_deliverImmediately(:GrowlNotification, nil,
          { :ApplicationName=>'Buildr', :NotificationName=>type,
            :NotificationTitle=>title, :NotificationDescription=>message }, true)
    rescue Exception
      # We get here in two cases: system doesn't have Growl installed so one of the OSX
      # calls raises an exception; system doesn't have osx/cocoa, e.g. MacPorts Ruby 1.9,
      # so we also need to rescue LoadError.
    end
  end

  Buildr.application.on_completion { |title, message| growl_notify('Completed', title, message) if verbose }
  Buildr.application.on_failure { |title, message, ex| growl_notify('Failed', title, message) if verbose }
end
