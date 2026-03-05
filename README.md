# Salles-Matériels Service – English School Management Platform

## Overview

This module is part of a larger School Management Platform developed to help manage the resources of an English language school.
The **Salles-Matériels service** is responsible for managing classrooms and the equipment available in each room. It allows administrators to create, update, and organize rooms and materials efficiently.

The goal of this service is to ensure that classrooms are properly equipped for courses and that school resources are managed effectively.

## Features

* CRUD operations for **Salles (Rooms)**
* CRUD operations for **Matériels (Equipment)**
* Assign equipment to specific rooms
* Manage equipment quantity
* Database integration with MySQL
* RESTful APIs for communication with other microservices

## Tech Stack

### Backend

* Spring Boot
* Java
* Spring Data JPA
* MySQL
* Maven

## Architecture

This service follows a **Microservices Architecture** as part of a larger system composed of multiple services.

The main components include:

* **Controller Layer** – Handles HTTP requests and API endpoints
* **Service Layer** – Contains business logic
* **Repository Layer** – Manages database operations
* **Entity Layer** – Represents database models such as `Salle` and `Materiel`

The service communicates with other modules of the platform such as:

* Class management
* Session scheduling
* School administration

## Contributors

* Med Malek Chourabi
* Project Team – 4SAE5

## Academic Context

Developed at **Esprit School of Engineering – Tunisia**
Software Engineering Program – **4th Year (4SAE5)**
Academic Project – **PIDEV**
Academic Year **2025–2026**

## Getting Started

### Prerequisites

* Java 17+
* Maven
* MySQL
* IDE (IntelliJ IDEA recommended)

### Installation

1. Clone the repository
2. Configure the database in `application.properties`
3. Run the Spring Boot application

Example:

```
mvn spring-boot:run
```

The service will start on the configured port and expose REST APIs for managing salles and matériels.

## Acknowledgments

This project was developed as part of the **PIDEV academic program** at Esprit School of Engineering.
Special thanks to our professors and mentors for their guidance and support.
