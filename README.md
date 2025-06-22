<h1>
   <img src="./frontend/public/logo/logo-w.svg" alt="Springyield Logo" width="" height="30">
   Springyield
</h1>

![Springyield Employee view](/github/img.png)
![Java](https://img.shields.io/badge/java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![SpringBoot](https://img.shields.io/badge/springboot-72b545?style=for-the-badge&logo=springboot&logoColor=white)
![TailwindCSS](https://img.shields.io/badge/tailwindcss-38B2AC.svg?style=for-the-badge&logo=tailwind-css&logoColor=white)
![Vue.js](https://img.shields.io/badge/vuejs-35495e.svg?style=for-the-badge&logo=vuedotjs&logoColor=%234FC08D)
![Docker](https://img.shields.io/badge/docker-0db7ed.svg?style=for-the-badge&logo=docker&logoColor=white)<br>
💰 Online Banking Application 🌿 Built with Java Spring Boot & Vue.js ✨ Created as part of a school group project, where I
played a major role ℹ️ Clone instead of fork: original repo had exposed credentials outside .gitignore 👤 Major project
member: [@Hellaweird](https://github.com/Hellaweird)

## Project Overview

This application provides a digital banking platform with features for both customers and administrators. The system is
built using:

- **Frontend**: Vue.js 3 with Tailwind CSS
- **Backend**: Spring Boot 3 with Java 21
- **Database**: H2 (in-memory)

## Features

- User authentication and authorization, using JWT
- Customer & Employee dashboard
- Account management
- Transaction history
- Admin user management

## Prerequisites

- Node.js (v18+)
- Java 21
- Maven
- Docker

## How to Run

First make sure Docker is running: **Docker Service** OR **Desktop Application**

```bash
docker-compose up -d
```

If you are having trouble it to compile correctly, you can run the following commands:
```bash
docker-compose build --no-cache
docker-compose up -d
```

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
- spring Boot Starter Security (BCrypt, JWT & WebSecurityConfig)
- Spring Data JPA
- Spring Web
- H2 Database
