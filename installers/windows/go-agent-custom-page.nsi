Var GO_SERVER_URL

Page custom ConfigureGetServerUrlPage GetServerUrlCallback

Function "ConfigureGetServerUrlPage"
  ; Read the legacy "GO_SERVER_URL" from the system
  ReadEnvStr $GO_SERVER_URL GO_SERVER_URL

  ${IfNot} $GO_SERVER_URL == ""
    ${LogText} "Detected (legacy) GO_SERVER_URL environment variable, will use that variable, and not prompt the Server URL"
    Abort
    Return
  ${ElseIf} ${FileExists} "$INSTDIR\config\wrapper-properties.conf"
    ${LogText} "$INSTDIR\config\wrapper-properties.conf already exists, not prompting user for Server URL"
    Abort
    Return
  ${EndIf}

  ${LogText} "Prompting user for Server URL"
  Push $R0
  InstallOptions::dialog $PLUGINSDIR\ServerURL.ini
  Pop $R0
FunctionEnd

Function "GetServerUrlCallback"
  ${LogText} "Reading Server URL"
  ReadINIStr $GO_SERVER_URL "$PLUGINSDIR\ServerURL.ini" "Field 2" "State"
  ${LogText} "User entered GO_SERVER_URL=$GO_SERVER_URL"
FunctionEnd
