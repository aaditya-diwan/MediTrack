# MediTrack: A Real-Time Healthcare Data Exchange Hub

## Overview
MediTrack is a robust, event-driven microservices platform designed to resolve healthcare data fragmentation. By leveraging **Apache Kafka** as a central nervous system, MediTrack establishes a real-time data exchange hub, ensuring seamless interoperability across the healthcare ecosystem, including hospitals, laboratories, insurance providers, and pharmacies.

---

## Local Development Environment Setup

This guide outlines the steps to configure and run the MediTrack **Laboratory Service** within a local development environment.

### Prerequisites

* **Java 17+**: Ensure the Java Runtime Environment (JRE) or Java Development Kit (JDK) is installed and configured in your system's `PATH`.
* **Apache Kafka**: Download the binary distribution of Apache Kafka and extract it to a short path, such as `C:\kafka`, to avoid command line length issues on Windows.

### Installation and Deployment

1.  **Kafka Installation**: Unzip the Kafka binaries to `C:\kafka`. This location minimizes the classpath length, preventing "input line too long" errors.

2.  **ZooKeeper Activation**: Open a command prompt and navigate to the `C:\kafka` directory. Launch the ZooKeeper server, which manages Kafka's cluster state, by executing the following command: (This should be done in the kafka folder)

    ```bash
    .\bin\windows\zookeeper-server-start.bat .\config\zookeeper.properties
    ```

3.  **Kafka Broker Initialization**: In a **separate** command prompt, also from the `C:\kafka` directory, start the Kafka broker using its server configuration file:

    ```bash
    .\bin\windows\kafka-server-start.bat .\config\server.properties
    ```

4.  **Application Deployment**: Navigate to the `Laboratory Service` root directory. Use the Maven Wrapper to build and deploy the Spring Boot application on a local Tomcat server:

    ```bash
    ./mvnw spring-boot:run
    ```

### Verification and Testing

1.  **API Endpoint Testing**: Utilize a tool like **Postman** to interact with the deployed application's REST endpoints and trigger events. This step validates the service's operational status.

2.  **Event Stream Validation**: To confirm that events are being successfully produced to the Kafka topic, use the console consumer. Open a **fourth** terminal and subscribe to the `lab.test.ordered.v1` topic to view all messages from the beginning of the log.

    ```bash
    .\bin\windows\kafka-console-consumer.bat --bootstrap-server localhost:9092 --topic lab.test.ordered.v1 --from-beginning
    ```

    This command provides real-time visibility into the event stream, confirming the correct functionality of the producer within the Laboratory Service.