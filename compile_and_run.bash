rm -r build/
mkdir build/
javac src/Main.java src/Index.java -d build/
java -cp build/:resources/postgresql-42.2.14.jar Main
