; ***************************GO-LICENSE-START******************************
; Copyright 2014 ThoughtWorks, Inc.
;
; Licensed under the Apache License, Version 2.0 (the "License");
; you may not use this file except in compliance with the License.
; You may obtain a copy of the License at
;
;    http://www.apache.org/licenses/LICENSE-2.0
;
; Unless required by applicable law or agreed to in writing, software
; distributed under the License is distributed on an "AS IS" BASIS,
; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
; See the License for the specific language governing permissions and
; limitations under the License.
; *************************GO-LICENSE-END**********************************

!include ..\..\..\..\windows-shared\windows-installer-base.nsi

Page custom CustomInstallBits

Function CustomOnInit
    InitPluginsDir
    File /oname=$PLUGINSDIR\JavaHome.ini JavaHome.ini
FunctionEnd

Var ARGS

Function SilentCustomUseInput
    ${GetParameters} $ARGS
    ${GetOptions} $ARGS /GO_SERVER_JAVA_HOME= $GO_SERVER_JAVA_HOME
    Call CustomInstallBits
FunctionEnd

Function CustomInstallBits
    ; Set the default java home
    StrCmp $GO_SERVER_JAVA_HOME "" 0 +2
        StrCpy $GO_SERVER_JAVA_HOME "$INSTDIR\jre"

    ; Write the Environment Variables for Wrapper to use
    WriteRegStr HKLM "SYSTEM\CurrentControlSet\Control\Session Manager\Environment" "CRUISE_SERVER_DIR" "$INSTDIR"
    WriteRegStr HKLM "SYSTEM\CurrentControlSet\Control\Session Manager\Environment" "GO_SERVER_JAVA_HOME" "$GO_SERVER_JAVA_HOME"

    ; Install and start
    ExecWait '"$INSTDIR\cruisewrapper.exe" --install "$INSTDIR\config\wrapper-server.conf"'
    ExecWait 'net start "Go Server"'

    ClearErrors
    FileOpen $0 "$SMPROGRAMS\Go Server\Go Server.url" w
    IfErrors done
        FileWrite $0 "[InternetShortcut]$\r$\n"
        FileWrite $0 "URL=http://localhost:8153/go$\r$\n"
        FileWrite $0 "IDList=$\r$\n"
        FileWrite $0 "IconFile=$INSTDIR\go-server.ico$\r$\n"
        FileWrite $0 "IconIndex=0$\r$\n"
        FileClose $0
        CopyFiles /SILENT "$SMPROGRAMS\Go Server\Go Server.url" $DESKTOP
    done:

    ; Open ports on Windows Firewall
    SimpleFC::AddPort 8153 "Go Server HTTP" 6 0 2 "" 1
    SimpleFC::AddPort 8154 "Go Server HTTPS" 6 0 2 "" 1
FunctionEnd

; Silent Installer Service Creation Section
Section "SilentCreateService"
	IfSilent SilentLabel NonSilentLabel
	SilentLabel:
		Call SilentCustomUseInput
		Goto Done
	NonSilentLabel:
		Goto Done
	Done:
SectionEnd


; Uninstaller
Section "Uninstall"
    ; Stop and remove
    ExecWait '"$INSTDIR\cruisewrapper.exe" --remove "$INSTDIR\config\wrapper-server.conf"'

    ; Remove registry keys
    DeleteRegKey HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\Go $%NAME%"
    DeleteRegKey HKLM "SOFTWARE\ThoughtWorks Studios\Go $%NAME%"

    ; Remove Env Vars
    DeleteRegValue HKLM "SYSTEM\CurrentControlSet\Control\Session Manager\Environment" "GO_SERVER_JAVA_HOME"
    DeleteRegValue HKLM "SYSTEM\CurrentControlSet\Control\Session Manager\Environment" "CRUISE_SERVER_DIR"

    SimpleFC::RemovePort 8153 6
    SimpleFC::RemovePort 8154 6

    Sleep 10000

    ; Remove files and uninstaller
    Delete $INSTDIR\uninstall.exe

    ; Remove directories used
    RMDir /r $INSTDIR\jre
    RMDir /r $INSTDIR\lib
    Delete $INSTDIR\*.*

    RMDir /r "$SMPROGRAMS\Go Server"
    Delete "$DESKTOP\Go Server.url"
SectionEnd

