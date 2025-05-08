echo "\033[1;34m[INFO]\033[0m Building the project..."
mvn clean package

if [ $? -ne 0 ]; then
  echo "\033[1;31m[ERROR]\033[0m Build failed! Exiting."
  exit 1
fi

echo "\033[1;34m[INFO]\033[0m Running standalone processor..."
java -jar ./target/embedProc-0.0.1-SNAPSHOT.jar --spring.profiles.active=standalone > standalone.log

# Move processed files back to input directory for next test
mv ./data/processed_files/* ./data/input_files/ 2>/dev/null || true

