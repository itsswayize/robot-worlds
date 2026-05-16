# 🤖 RobotWorld (Brownfields Iteration 5)

## 🌎 Brief Overview
RobotWorld is a game-like simulation where users launch robots into an alien world. Robots move and interact within a shared environment filled with obstacles. Each robot can move, turn, shoot, repair itself, and reload—actions controlled by a set of commands that keep everything running smoothly.

## 🚀 Getting Started

This project is a **Java** project using **Maven** as the build tool.

### 📦 Dependencies
* **JSON (org.json):** Used for creating and parsing responses.
* **SQLite JDBC:** For database connectivity.
* **EoDSQL:** For Object-Relational Mapping (Local JAR).
* **Spark Java:** For the Web API server.

---

## 🛠 Prerequisites & Installation

### 1. Install Maven
If you don't have Maven installed:

* **macOS:** `brew install maven`
* **Ubuntu/Linux:** `sudo apt update && sudo apt install maven`
* **Windows:** `scoop install maven` (or download from Apache Maven website).

### 2. Install Local Dependencies (Important!)
This project uses a local library (`eodsql.jar`) that is not in the central Maven repository. You must install it manually before building.

**Run this command from the project root:**

```bash
mvn install:install-file -Dfile=eodsql.jar -DgroupId=net.lemnik -DartifactId=eodsql -Dversion=2.2 -Dpackaging=jar
```

### 3. Build the Project
Clean and compile the project to ensure everything is set up correctly:

```bash
mvn clean compile
```


## 🏗 Running the RobotServer

The Server runs two services:

- **Socket Server (Default Port 5000):** For the Java Client connection.
- **Web API Server (Fixed Port 8080):** For HTTP/JSON interaction.

To run the server:

```bash
mvn exec:java -Dexec.mainClass="za.co.wethinkcode.robots.server.Server"
```

(You may pass a port number argument for the socket server if you wish, e.g., `-Dexec.args="6000"`)

### 🎮 Server Admin Console Commands
Once the server is running, you can type these commands directly into the terminal:

* `robots` : List all connected robots.
* `dump` : Show internal world state.
* `save <name>` : Save the current world configuration to the database.
    - Example: `save my_world`
* `restore <name>` : Load a world configuration from the database (disconnects existing clients).
    - Example: `restore my_world`
* `purge <robot_name>` : Forcefully remove a robot from the world.
* `quit` : Shutdown the server.

---

## 🌐 Using the Web API (HTTP)
The server listens for HTTP requests on port 8080.

### 1. Restore a World
`GET /world/{name}` - Restores a saved world from the database.

Example (curl):

```bash
curl -X GET http://localhost:8080/world/my_world
```

### 2. Command a Robot
`POST /robot/{name}` - Sends a command to a specific robot.

#### Example - Launching a Robot:

```bash
curl -X POST http://localhost:8080/robot/hal -d '{"command": "launch", "arguments": ["hal", "shooter"]}'
```

#### Example - Moving Forward:

```bash
curl -X POST http://localhost:8080/robot/hal -d '{"command": "forward", "arguments": ["hal", "5"]}'
```

---

## 🏗 Running the RobotClient (Socket)

To connect a Java client to the server:

1. Open a new terminal window.
2. Navigate to the project root.
3. Run the client:

```bash
mvn exec:java -Dexec.mainClass="za.co.wethinkcode.robots.client.ClientApp"
```

Follow the prompts:

* **IP Address:** Enter `localhost` (or the server's LAN IP if on a different machine).
* **Port:** Enter `5000` (or whatever port the server started on).

---

## ✅ Running Tests
To run the unit and acceptance tests:

```bash
make test
```
