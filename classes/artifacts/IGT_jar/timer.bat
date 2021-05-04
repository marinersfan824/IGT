@echo off
echo To exit do Ctrl+C

:: This is the relevant command for non-windows users
java --module-path javafx-sdk-11.0.2\lib --add-modules=javafx.controls,javafx.fxml -jar IGT.jar

pause