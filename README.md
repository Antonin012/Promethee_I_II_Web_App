<center>

# Promethee Web Application

![Java](https://img.shields.io/badge/Java-17-ED8B00?style=for-the-badge&logo=java&logoColor=white)
![Apache Tomcat](https://img.shields.io/badge/Apache%20Tomcat-10.1-F8DC75?style=for-the-badge&logo=apachetomcat&logoColor=black)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-316192?style=for-the-badge&logo=postgresql&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-2CA5E0?style=for-the-badge&logo=docker&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white)
</center>

## Overview

This project is a web-based implementation of the PROMETHEE II (Preference Ranking Organization Method for Enrichment Evaluations) multi-criteria decision analysis method. It allows users to define a set of alternatives and criteria, assign weights and preference functions to those criteria, and calculate the global preference ranking of the alternatives.

The application also includes a PROMETHEE I analysis section, providing pairwise comparisons to identify strict preferences, indifferences, or incomparabilities between specific alternatives.

## Features

*   **Dynamic Matrix**: Add or remove alternatives and criteria dynamically through the web interface.
*   **PROMETHEE II Ranking**: Calculates positive flows (Phi+), negative flows (Phi-), and net flows (Phi Net) to rank alternatives from best to worst.
*   **PROMETHEE I Analysis**: Compare any two alternatives side-by-side to understand the detailed preference relations (A P B, A I B, A R B).
*   **Six Preference Functions**: Supports all standard PROMETHEE preference functions (Usual, U-Shape, V-Shape, Level, V-Shape with Indifference, Gaussian).
*   **Persistence**: 
    *   **Local Storage**: Automatically saves the current state in the browser's local storage to prevent data loss on reload.
    *   **JSON Import/Export**: Download the current matrix as a JSON file and upload it later.
    *   **Database Integration**: Save completed sessions to a PostgreSQL database and load them via the sidebar menu.

## Architecture

*   **Frontend**: HTML, CSS (Vanilla), and JavaScript. Uses AJAX (XMLHttpRequest) to communicate with the backend without reloading the page.
*   **Backend**: Java Servlets (Jakarta EE) running on Apache Tomcat 10.1+.
*   **Database**: PostgreSQL, accessed via standard JDBC (Data Access Object pattern).
*   **Deployment**: Docker and Docker Compose for containerized environment setup.

## Getting Started (Docker)

The easiest way to run the application is using Docker. A `docker-compose.yml` file is provided in the `deploy/` directory.

### Prerequisites

*   Docker installed on your machine.
*   Docker Compose plugin installed.

### Running the Application

1.  Navigate to the `deploy/` directory:
    ```bash
    cd deploy
    ```
2.  Start the containers in detached mode and build the application:
    ```bash
    docker compose up -d --build
    ```
3.  Access the application in your web browser at:
    `http://localhost:8080/`

To stop the application, run:
```bash
docker compose down
```

## Manual Build (Without Docker)

### Prerequisites

*   Java Development Kit (JDK) 17 or higher.
*   Apache Maven 3.6+.
*   Apache Tomcat 10.1+.
*   PostgreSQL Database.

### Instructions

1.  Configure the database connection:
    Set the following environment variables on your system or in your Tomcat configuration before starting:
    *   `POSTGRES_DB`
    *   `POSTGRES_USER`
    *   `POSTGRES_PASSWORD`
2.  Compile the project and build the WAR file using Maven:
    ```bash
    mvn clean package -DskipTests
    ```
3.  Deploy the generated `target/app.war` to your Tomcat `webapps/` directory (you can rename it to `ROOT.war` to serve it at the root context).
4.  Execute the SQL script located in `deploy/init.sql` on your PostgreSQL database to create the necessary tables.

## Usage

1.  **Define Criteria**: In the top row, name your criteria, set their weights (ensure the sum equals 1.0), choose a preference function, set its parameters (p, q, s), and define whether the goal is to maximize ("Higher +") or minimize ("Lower -") the value.
2.  **Define Alternatives**: Add rows for your alternatives and input their respective scores for each criterion.
3.  **Calculate**: The application automatically calculates and updates the "Final Results Matrix" as long as the sum of the weights is exactly 1.0.
4.  **Pairwise Comparison**: Use the dropdown menus in the "Pairwise Comparison" section to deeply analyze the relationship between any two alternatives.
5.  **Save/Load**: Use the right sidebar (accessible via the top-left button) to load previous sessions. Use the action bar below the matrix to save your current work, export to JSON, or reset the board.

## License

This project is developed for educational purposes.
