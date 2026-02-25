@echo off
set JAVA_HOME=C:\Users\adhig\jdk-17.0.13+11
set ANDROID_HOME=C:\Users\adhig\AppData\Local\Android\Sdk
set PATH=%JAVA_HOME%\bin;%ANDROID_HOME%\platform-tools;%PATH%
echo JAVA_HOME: %JAVA_HOME%
echo ANDROID_HOME: %ANDROID_HOME%
gradlew.bat %*
