@echo off

set SEP=;
set BASE_DIR=%~dp0

set CLASSPATH_B=.%SEP%%BASE_DIR%/../lib/*
set JAVA_OPTS_B=-Xverify:none -Djava.library.path=%BASE_DIR%/../lib

java -cp "%CLASSPATH_B%" %JAVA_OPTS_B% blutwurst.core %*
