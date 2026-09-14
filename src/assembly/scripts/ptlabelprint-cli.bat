@echo off
set "DIR=%~dp0"
java -cp "%DIR%ptlabelprint-cli.jar;%DIR%lib\*" cz.bliksoft.ptlabelprint.Cli %*
