@rem Gradle startup script for Windows
@setlocal
@set APP_HOME=%~dp0
@set DEFAULT_JVM_OPTS=--enable-native-access=ALL-UNNAMED
@if defined JAVA_HOME (
  @set JAVA_EXE=%JAVA_HOME%\bin\java.exe
) else (
  @set JAVA_EXE=java.exe
)
"%JAVA_EXE%" %DEFAULT_JVM_OPTS% -classpath "%APP_HOME%\gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain %*
@endlocal
