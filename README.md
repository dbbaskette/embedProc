# 📝 textProc

![Java](https://img.shields.io/badge/Java-21-blue?logo=java)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.4.5-brightgreen?logo=springboot)
![Apache Tika](https://img.shields.io/badge/Apache_Tika-2.9.2-yellow?logo=apache)
![Maven](https://img.shields.io/badge/Maven-Build-red?logo=apachemaven)

---

## 🚀 Project Overview

**textProc** is a modern Spring Boot application that leverages Apache Tika and Spring AI for robust, profile-driven document processing. It supports standalone, Kubernetes, and Cloud Foundry modes, making it suitable for a variety of deployment scenarios.

---

## ✨ Features

- ⚡ Fast and flexible directory-based text extraction
- 🤖 Spring AI integration (configurable for future LLM/AI enhancements)
- 📄 Apache Tika-powered document parsing
- 🔄 Profile-based configuration (standalone, scdf)
- 📂 Automatic directory management and error handling
- 📝 Easy to extend and customize

---

## 📁 Directory Structure

```text
textProc/
├── src/main/java/com/baskettecase/textProc/
│   ├── TextProcApplication.java         # Main Spring Boot entry point
│   ├── config/ProcessorProperties.java  # Configuration properties (profile-driven)
│   ├── processor/StandaloneDirectoryProcessor.java # Standalone mode processor
│   └── service/ExtractionService.java   # Tika-based extraction logic
├── src/main/resources/
│   ├── application.properties
│   ├── application-standalone.properties
│   └── application-scdf.properties
├── data/                               # Input, output, error, and processed files (gitignored)
├── pom.xml                             # Maven build file
└── README.md
```

---

## 🛠️ Getting Started

### Docker Build and Multi-Arch Images

You can build, push, and pull multi-architecture Docker images using the included `dockerbuild.sh` script:

```sh
./dockerbuild.sh <dockerhub-username> <image-name> [tag]
```

- Builds the Spring Boot JAR
- Builds and pushes a multi-arch (amd64/arm64) Docker image
- Pulls the image after push to verify success

### Troubleshooting

If you see errors like `NoSuchMethodError` for `TarArchiveInputStream.getNextEntry()` from Tika, ensure you have `commons-compress` version 1.21+ (this project uses 1.26.1). See `pom.xml` for details.

### SCDF (Spring Cloud Data Flow) Integration

This project supports SCDF profile for event-driven processing via RabbitMQ. To use:
- Set `app.processor.mode=scdf` in `application-scdf.properties`
- Configure input/output channels if needed
- Ensure MinIO/S3 environment variables are set for object storage

See code and comments in `ScdfStreamProcessor.java` for details.


### Prerequisites
- Java 21+
- Maven 3.8+

### Build and Run
```sh
mvn clean install
mvn spring-boot:run
```

### Directory Setup
The application will automatically create required directories (input, output, error, processed) on startup.

---

## ⚙️ Configuration

Configuration is profile-driven. Example for standalone mode:

```properties
# src/main/resources/application-standalone.properties
app.processor.mode=standalone
app.processor.standalone.input-directory=./data/input_files
app.processor.standalone.output-directory=./data/output_text
app.processor.standalone.error-directory=./data/error_files
app.processor.standalone.processed-directory=./data/processed_files
```

Example for Spring Cloud Data Flow (SCDF) mode:

```properties
# src/main/resources/application-scdf.properties
app.processor.mode=scdf
app.processor.scdf.input-channel=textProcInput
app.processor.scdf.output-channel=textProcOutput
```

Switch profiles with:
```sh
-Dspring.profiles.active=standalone   # or scdf
```

---

## 📦 Usage
- Drop files into the input directory (e.g., `./data/input_files`)
- Processed text will appear in the output directory
- Errors and processed originals are moved to their respective directories

---

## 🤝 Contributing
Pull requests are welcome! For major changes, please open an issue first to discuss what you would like to change.

---

## 📝 License
[MIT](LICENSE)

---

> Made with ❤️ using Spring Boot & Apache Tika
