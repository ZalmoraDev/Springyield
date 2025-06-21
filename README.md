# Banking Application (Springyield)
A full-stack banking application with a Vue.js frontend and Spring Boot backend.

⚠️DISCLAIMER: This project is a work in progress and is not yet complete. Current instructions on how to run the application are not accurate!<br>
## Project Overview

This application provides a digital banking platform with features for both customers and administrators. The system is built using:

- **Frontend**: Vue.js 3 with Tailwind CSS
- **Backend**: Spring Boot 3 with Java 21
- **Database**: H2 (in-memory)

## Features

- models.com.stefvisser.springyield.User authentication and authorization, using JWT
- Customer & Employee dashboard
- models.com.stefvisser.springyield.Account management
- models.com.stefvisser.springyield.Transaction history
- Admin user management

## Project Structure

```
codegen/
├── backend/              # Spring Boot application
│   ├── src/              # Java source code
│   ├── pom.xml           # Maven dependencies
│   └── ...
├── frontend/             # Vue.js application
│   ├── src/              # Vue source code
│   │   ├── assets/       # CSS and other assets
│   │   ├── components/   # Vue components
│   │   ├── router/       # Vue Router configuration
│   │   ├── utils/        # Utility functions
│   │   └── views/        # Vue pages
│   ├── public/           # Static assets
│   ├── package.json      # NPM dependencies
│   └── ...
└── README.md             # This file
```

## Getting Started

### Prerequisites

- Node.js (v18+)
- Java 21
- Maven

### Running the Frontend

1. Installing frontend npm dependencies:
   ```bash
   cd frontend
   npm install
   ```

2. Running Vite for frontend development (terminal 1):
   ```bash
   cd frontend
   npm run dev
   ```

3. Running Tailwind for frontend (terminal 2, open a new terminal):
   ```bash
   cd frontend
   npx @tailwindcss/cli -i ./src/assets/input.css -o ./src/assets/output.css --watch
   ```

### Running the Backend

1. Open the project in your IDE (IntelliJ IDEA recommended)
2. Run the Spring Boot application by pressing `SHIFT + F10` or using the run button in the top-right corner

## Technologies Used

### Frontend
- Vue.js 3
- Vue Router
- Tailwind CSS
- Axios
- Chart.js
- JWT Authentication

### Backend
- Spring Boot 3
- spring Boot Starter Security (BCrypt, JWT & config.com.stefvisser.springyield.WebSecurityConfig)
- Spring Data JPA
- Spring Web
- H2 Database
