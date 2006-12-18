# Weka NSIS installation script
#
# Note: content between "Start: .../End: ..." comments could get replaced, so
#       DO NOT modify these sections.
#
# Author : FracPete (fracpete at waikato dot at dot nz)
# Version: $Revision: 1.6 $

Name Weka

# Start: Weka
!define WEKA_WEKA "Weka"
!define WEKA_VERSION "3.4.7"   # must be of form 'X.Y.Z'
!define WEKA_VERSION_HYPHEN "3-4-7"   # must be of form 'X-Y-Z'
!define WEKA_FILES "D:\development\projects\weka.previous_releases\weka-3-4-7"
!define WEKA_TEMPLATES "D:\development\projects\weka.release\nsis\templates"
!define WEKA_LINK_PREFIX "Weka 3.4"
!define WEKA_DIR "Weka-3-4"
!define WEKA_URL "http://www.cs.waikato.ac.nz/~ml/weka/"
!define WEKA_MLGROUP "Machine Learning Group, University of Waikato, Hamilton, NZ"
!define WEKA_HEADERIMAGE "D:\development\projects\weka.release\nsis\images\weka_new.bmp"
!define WEKA_JRE "D:\installs\windows\programming\java\jdk.14\j2re-1_4_2_11-windows-i586-p.exe"
!define WEKA_JRE_TEMP "jre_setup.exe"
!define WEKA_JRE_INSTALL "RunJREInstaller.bat"
!define WEKA_JRE_INSTALL_DONE "RunJREInstaller.done"
!define WEKA_JRE_SUFFIX ""
# End: Weka

# Defines
!define REGKEY "SOFTWARE\$(^Name)"
!define VERSION "${WEKA_VERSION}"
!define COMPANY "${WEKA_MLGROUP}"
!define URL "${WEKA_URL}"

# MUI defines
!define MUI_ICON "${NSISDIR}\Contrib\Graphics\Icons\modern-install.ico"
!define MUI_FINISHPAGE_NOAUTOCLOSE
!define MUI_STARTMENUPAGE_REGISTRY_ROOT HKLM
!define MUI_STARTMENUPAGE_NODISABLE
!define MUI_STARTMENUPAGE_REGISTRY_KEY ${REGKEY}
!define MUI_STARTMENUPAGE_REGISTRY_VALUENAME StartMenuGroup
!define MUI_STARTMENUPAGE_DEFAULT_FOLDER ${WEKA_WEKA}
!define MUI_UNICON "${NSISDIR}\Contrib\Graphics\Icons\modern-uninstall.ico"
!define MUI_UNFINISHPAGE_NOAUTOCLOSE
!define MUI_HEADERIMAGE
!define MUI_HEADERIMAGE_BITMAP "${WEKA_HEADERIMAGE}"
!define MUI_FINISHPAGE_RUN
!define MUI_FINISHPAGE_RUN_TEXT "Start ${WEKA_WEKA}"
!define MUI_FINISHPAGE_RUN_FUNCTION "LaunchProgram"

# Included files
!include Sections.nsh
!include MUI.nsh

# Variables
Var StartMenuGroup

# Installer pages
!insertmacro MUI_PAGE_WELCOME
!insertmacro MUI_PAGE_LICENSE "${WEKA_FILES}\COPYING"
!insertmacro MUI_PAGE_COMPONENTS
!insertmacro MUI_PAGE_DIRECTORY
!insertmacro MUI_PAGE_STARTMENU Application $StartMenuGroup
!insertmacro MUI_PAGE_INSTFILES
!insertmacro MUI_PAGE_FINISH
!insertmacro MUI_UNPAGE_CONFIRM
!insertmacro MUI_UNPAGE_INSTFILES

# Installer languages
!insertmacro MUI_LANGUAGE English

# Installer attributes
OutFile "weka-${WEKA_VERSION_HYPHEN}${WEKA_JRE_SUFFIX}.exe"
InstallDir $PROGRAMFILES\${WEKA_DIR}
CRCCheck on
XPStyle on
ShowInstDetails show
VIProductVersion ${WEKA_VERSION}.0
VIAddVersionKey ProductName "${WEKA_WEKA}"
VIAddVersionKey ProductVersion "${VERSION}"
VIAddVersionKey CompanyName "${COMPANY}"
VIAddVersionKey CompanyWebsite "${URL}"
VIAddVersionKey FileVersion ""
VIAddVersionKey FileDescription ""
VIAddVersionKey LegalCopyright ""
InstallDirRegKey HKLM "${REGKEY}" Path
ShowUninstDetails show

# Installer options
InstType "Full"
InstType "Minimal"

# Installer sections
Section -Main SectionMain
    SetOverwrite on
    # Files
    SetOutPath $INSTDIR
    File /r ${WEKA_FILES}\*
    # files from template directory have to be listed separately
    File ${WEKA_TEMPLATES}\RunWeka.bat
    File ${WEKA_TEMPLATES}\RunWeka.ini
    File ${WEKA_TEMPLATES}\RunWeka.class
    File ${WEKA_TEMPLATES}\weka.ico
    File ${WEKA_TEMPLATES}\weka.gif
    File ${WEKA_TEMPLATES}\documentation.html
    File ${WEKA_TEMPLATES}\documentation.css
    # Start: JRE
    File ${WEKA_JRE}
    # End: JRE
    # Links in App directory (to get the working directory of the links correct!)
    SetOutPath $INSTDIR
    CreateShortcut "$INSTDIR\${WEKA_LINK_PREFIX}.lnk" "$INSTDIR\RunWeka.bat" "default" $INSTDIR\Weka.ico
    CreateShortcut "$INSTDIR\${WEKA_LINK_PREFIX} (with console).lnk" "$INSTDIR\RunWeka.bat" "default" $INSTDIR\Weka.ico
    SetOutPath $SMPROGRAMS\$StartMenuGroup
    WriteRegStr HKLM "${REGKEY}\Components" Main 1
SectionEnd

# Start menu
Section "Start Menu" SectionMenu
    SectionIn 1
    SetOutPath $SMPROGRAMS\$StartMenuGroup
    CreateShortcut "$SMPROGRAMS\$StartMenuGroup\Documentation.lnk" $INSTDIR\documentation.html
    CreateShortcut "$SMPROGRAMS\$StartMenuGroup\${WEKA_LINK_PREFIX} (with console).lnk" "$INSTDIR\${WEKA_LINK_PREFIX} (with console).lnk" "" $INSTDIR\Weka.ico
    CreateShortcut "$SMPROGRAMS\$StartMenuGroup\${WEKA_LINK_PREFIX}.lnk" "$INSTDIR\${WEKA_LINK_PREFIX}.lnk" "" $INSTDIR\Weka.ico
    # delete temporary shortcuts
    Delete /REBOOTOK "$INSTDIR\${WEKA_LINK_PREFIX}.lnk"
    Delete /REBOOTOK "$INSTDIR\${WEKA_LINK_PREFIX} (with console).lnk"
SectionEnd

# associate .arff with WEKA
Section "Associate Files" SectionAssociations
    SectionIn 1
    WriteRegStr HKCR ".arff" "" "ARFFDataFile"
    WriteRegStr HKCR "ARFFDataFile" "" "ARFF Data File"
    WriteRegStr HKCR "ARFFDataFile\DefaultIcon" "" "$INSTDIR\weka.ico"
    WriteRegStr HKCR "ARFFDataFile\shell\open\command" "" '"javaw.exe" "-classpath" "$INSTDIR" "RunWeka" "-i" "$INSTDIR\RunWeka.ini" "-w" "$INSTDIR\weka.jar" "-c" "explorer" "%1"'
SectionEnd

# Start: JRE
# install JRE
Section "Install JRE" SectionJRE
    SectionIn 1
    
    # extract JRE to "jre_setup.exe"
    File /oname=$INSTDIR\${WEKA_JRE_TEMP} "${WEKA_JRE}"
    SetOutPath $INSTDIR
    
    # write batch file
    FileOpen $9 "${WEKA_JRE_INSTALL}" w
    FileWrite $9 "${WEKA_JRE_TEMP}$\r$\n"
    FileWrite $9 "del ${WEKA_JRE_TEMP}$\r$\n"
    FileClose $9

    # execute batch file    
    ExecWait "${WEKA_JRE_INSTALL}"
    
    # delete batch file
    Rename "${WEKA_JRE_INSTALL}" "${WEKA_JRE_INSTALL_DONE}"
SectionEnd
# End: JRE

Section -post SectionPost
    WriteRegStr HKLM "${REGKEY}" Path $INSTDIR
    WriteUninstaller $INSTDIR\uninstall.exe
    !insertmacro MUI_STARTMENU_WRITE_BEGIN Application
    SetOutPath $SMPROGRAMS\$StartMenuGroup
    CreateShortcut "$SMPROGRAMS\$StartMenuGroup\Uninstall $(^Name).lnk" $INSTDIR\uninstall.exe
    !insertmacro MUI_STARTMENU_WRITE_END
    WriteRegStr HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\$(^Name)" DisplayName "$(^Name)"
    WriteRegStr HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\$(^Name)" DisplayVersion "${VERSION}"
    WriteRegStr HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\$(^Name)" Publisher "${COMPANY}"
    WriteRegStr HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\$(^Name)" URLInfoAbout "${URL}"
    WriteRegStr HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\$(^Name)" DisplayIcon $INSTDIR\uninstall.exe
    WriteRegStr HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\$(^Name)" UninstallString $INSTDIR\uninstall.exe
    WriteRegDWORD HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\$(^Name)" NoModify 1
    WriteRegDWORD HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\$(^Name)" NoRepair 1
SectionEnd

# Macro for selecting uninstaller sections
!macro SELECT_UNSECTION SECTION_NAME UNSECTION_ID
    Push $R0
    ReadRegStr $R0 HKLM "${REGKEY}\Components" "${SECTION_NAME}"
    StrCmp $R0 1 0 next${UNSECTION_ID}
    !insertmacro SelectSection "${UNSECTION_ID}"
    GoTo done${UNSECTION_ID}
next${UNSECTION_ID}:
    !insertmacro UnselectSection "${UNSECTION_ID}"
done${UNSECTION_ID}:
    Pop $R0
!macroend

# Uninstaller sections
Section /o un.Main UNSEC0000
    Delete /REBOOTOK "$SMPROGRAMS\$StartMenuGroup\${WEKA_LINK_PREFIX}.lnk"
    Delete /REBOOTOK "$SMPROGRAMS\$StartMenuGroup\${WEKA_LINK_PREFIX} (with console).lnk"
    Delete /REBOOTOK "$SMPROGRAMS\$StartMenuGroup\Documentation.lnk"
    Delete /REBOOTOK "$INSTDIR\RunWeka.class"
    Delete /REBOOTOK "$INSTDIR\RunWeka.ini"
    Delete /REBOOTOK "$INSTDIR\RunWeka.bat"
    Delete /REBOOTOK "$INSTDIR\weka.ico"
    Delete /REBOOTOK "$INSTDIR\weka.gif"
    Delete /REBOOTOK "$INSTDIR\documentation.html"
    Delete /REBOOTOK "$INSTDIR\documentation.css"
    # Start: JRE
    Delete /REBOOTOK "$INSTDIR\${WEKA_JRE_INSTALL_DONE}"
    # End: JRE
    RmDir /r /REBOOTOK $INSTDIR
    DeleteRegValue HKLM "${REGKEY}\Components" Main
    DeleteRegKey HKCR ".arff"
    DeleteRegKey HKCR "ARFFDataFile"
SectionEnd

Section un.post UNSEC0001
    DeleteRegKey HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\$(^Name)"
    Delete /REBOOTOK "$SMPROGRAMS\$StartMenuGroup\Uninstall $(^Name).lnk"
    Delete /REBOOTOK $INSTDIR\uninstall.exe
    DeleteRegValue HKLM "${REGKEY}" StartMenuGroup
    DeleteRegValue HKLM "${REGKEY}" Path
    DeleteRegKey /IfEmpty HKLM "${REGKEY}\Components"
    DeleteRegKey /IfEmpty HKLM "${REGKEY}"
    RmDir /REBOOTOK $SMPROGRAMS\$StartMenuGroup
    RmDir /REBOOTOK $INSTDIR
SectionEnd

# Section overview
!insertmacro MUI_FUNCTION_DESCRIPTION_BEGIN
  !insertmacro MUI_DESCRIPTION_TEXT ${SectionMenu} "Adds a group to the Start Menu with links for Weka and the Documentation."
  !insertmacro MUI_DESCRIPTION_TEXT ${SectionAssociations} "Associates the .arff files with the Weka Explorer."
  # Start: JRE
  !insertmacro MUI_DESCRIPTION_TEXT ${SectionJRE} "Installs the Java Runtime Environment (JRE). The setup file will be placed in the Weka program folder regardless of the selection here."
  # End: JRE
!insertmacro MUI_FUNCTION_DESCRIPTION_END

# Installer functions
Function .onInit
    InitPluginsDir
FunctionEnd

# Uninstaller functions
Function un.onInit
    ReadRegStr $INSTDIR HKLM "${REGKEY}" Path
    ReadRegStr $StartMenuGroup HKLM "${REGKEY}" StartMenuGroup
    !insertmacro SELECT_UNSECTION Main ${UNSEC0000}
FunctionEnd

# launches program
Function LaunchProgram
  ExecShell "" "$SMPROGRAMS\$StartMenuGroup\${WEKA_LINK_PREFIX}.lnk" "" SW_SHOWNORMAL
FunctionEnd
