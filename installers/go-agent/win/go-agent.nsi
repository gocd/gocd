;
; Copyright 2016 ThoughtWorks, Inc.
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
;

ReserveFile "ServerURL.ini"

!include ..\..\..\..\windows-shared\windows-installer-base.nsi

Page custom CustomGetInput CustomUseInput

Function CustomOnInit
    InitPluginsDir
    File /oname=$PLUGINSDIR\ServerURL.ini ServerURL.ini
    File /oname=$PLUGINSDIR\JavaHome.ini JavaHome.ini
FunctionEnd

Function CustomGetInput
    Push $R0
    InstallOptions::dialog $PLUGINSDIR\ServerURL.ini
    Pop $R0
FunctionEnd

Var ARGS
Var SERVER_URL

Function CustomUseInput
    ReadINIStr $SERVER_URL "$PLUGINSDIR\ServerURL.ini" "Field 2" "State"
    Call CustomInstallBits
FunctionEnd

Function SilentCustomUseInput
    ${GetParameters} $ARGS
    ${GetOptions} $ARGS /SERVERURL= $SERVER_URL
    ${GetOptions} $ARGS /GO_AGENT_JAVA_HOME= $GO_AGENT_JAVA_HOME
    Call CustomInstallBits
FunctionEnd

Function CustomInstallBits
    Call CleanUpUnnecessaryStuffOfOldInstallations
    StrCmp $GO_AGENT_JAVA_HOME "" 0 +2
        StrCpy $GO_AGENT_JAVA_HOME "$INSTDIR\jre"

    StrCmp $SERVER_URL "" 0 +2
        StrCpy $SERVER_URL "https://127.0.0.1:8154/go"

    ; Write the Environment Variables for Wrapper to use
    WriteRegStr HKLM "SYSTEM\CurrentControlSet\Control\Session Manager\Environment" "GO_SERVER_URL" "$SERVER_URL"
    WriteRegStr HKLM "SYSTEM\CurrentControlSet\Control\Session Manager\Environment" "GO_AGENT_DIR" "$INSTDIR"
    WriteRegStr HKLM "SYSTEM\CurrentControlSet\Control\Session Manager\Environment" "GO_AGENT_JAVA_HOME" "$GO_AGENT_JAVA_HOME"

    ; Create shortcut to the .cmd so they can run in interactive mode
    SetOutPath "$INSTDIR"
    CreateShortCut "$SMPROGRAMS\Go Agent\Run Go Agent.lnk" "$INSTDIR\agent.cmd"

    ; Install and start
    ExecWait '"$INSTDIR\cruisewrapper.exe" --install "$INSTDIR\config\wrapper-agent.conf"'
    ClearErrors
    ExecWait 'net start "Go Agent"'
    IfErrors 0 +3
        ${LogText} "Error starting Go Agent Windows Service. Check if service is disabled."
        Goto DONE
    ${LogText} "Successfully started Go Agent"
    DONE:
FunctionEnd

Function CleanUpUnnecessaryStuffOfOldInstallations
    StrCmp $IsUpgrading $UPGRADING upgrade done

    upgrade:
        IfFileExists $INSTDIR\jdk 0 done
        RMDir /r $INSTDIR\jdk

    done:

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
    ExecWait '"$INSTDIR\cruisewrapper.exe" --remove "$INSTDIR\config\wrapper-agent.conf"'

    Sleep 2500

    ; Remove Env Vars
    DeleteRegValue HKLM "SYSTEM\CurrentControlSet\Control\Session Manager\Environment" "GO_AGENT_JAVA_HOME"
    DeleteRegValue HKLM "SYSTEM\CurrentControlSet\Control\Session Manager\Environment" "GO_AGENT_DIR"
    DeleteRegValue HKLM "SYSTEM\CurrentControlSet\Control\Session Manager\Environment" "GO_SERVER_URL"

    ; Remove registry keys
    DeleteRegKey HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\Go $%NAME%"
    DeleteRegKey HKLM "SOFTWARE\ThoughtWorks Studios\Go $%NAME%"

    ; Remove files and uninstaller
    Delete $INSTDIR\uninstall.exe

    RMDir /r "$SMPROGRAMS\Go Agent"
    ; Remove directories used
    RMDir /r "$INSTDIR"
SectionEnd
