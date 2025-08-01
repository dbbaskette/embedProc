# Project Instructions: [PROJECT NAME]

> **Instructions for the User**:
>This app reads text from HDFS directories and sends to an embedding model, then writes the results to a PostgreSQL vector store.   
It includes the ability to write 2 keys (refnum1 and refnum2) to the metadata column of the row being added to the vector store. They are written as JSON: `{"refnum1":100001,"refnum2":200001}`. Those numbers are extracted from the filename it reads (e.g., `100001-200001.txt`). 

>**Architecture Note**: The original implementation had confusing naming with "ReferenceNumberEmbeddingService" etc., but this has been **refactored to use a unified EmbeddingService** that handles both plain embeddings and embeddings with metadata as optional parameters. This simplified architecture eliminates code duplication and follows better design principles.
---

## 1. Project Overview & Goal

*  

## 2. Tech Stack

*   **Language(s) & Version(s)**:  Java and Spring versions currently in POM
*   **Framework(s)**:  Spring versions currently in POM
*   **Database(s)**:Postgresql
*   **Key Libraries**: Spring AI 1.0.0
*   **Build/Package Manager**: Maven
*   **Deployed in prod via Spring Cloud Dataflow**
*   **Deployed in Prod on Tanzu Cloud Foundry**

---


## 3. Architecture & Design

*   **High-Level Architecture**: Part of a streaming solution for building vector stores
*   **Key Design Patterns**: Spring RAG pattern (handles the Retrieval setup by building the vectorstore)
*   **Embedding Service**: Unified service that handles both plain embeddings and embeddings with metadata
    *   `storeEmbedding(text)` - for plain embeddings
    *   `storeEmbeddingWithMetadata(text, refnum1, refnum2)` - for embeddings with metadata
*   **Processors**: 
    *   `ScdfStreamProcessor` - Cloud deployment (Spring Cloud Dataflow)
    *   `StandaloneDirectoryProcessor` - Local directory processing
*   **Directory Structure**:
    *   `src/main/java/com/baskettecase/embedProc/`: Main application source
    *   `src/main/resources/`: Configuration files
    *   `REFERENCE_NUMBERS_IMPLEMENTATION.md`: Metadata implementation guide

## 4. Coding Standards & Conventions
(e.g., "
*   **Code Style**: Google Java Style Guide but with Spring 
*   **Naming Conventions**:  "Use `camelCase` for variables", "Services should be suffixed with `Service`"
*   **Error Handling**: (e.g., "Use custom exception classes", "Return standardized JSON error responses")

## 5. Important "Do's and Don'ts"

*   **DON'T**:  "Do not commit secrets or API keys directly into the repository.
*   **DO**:  "Log important events and errors.