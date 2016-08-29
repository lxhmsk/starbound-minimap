mkdir bin
dir /s /b /a-d src > files.txt
javac -cp gson-2.2.4.jar;jna-4.2.2.jar;jna-platform-4.2.2.jar -d bin @files.txt
rm files.txt
echo done
