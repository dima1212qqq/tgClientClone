
**TgCloneClient** — это десктопное Java-приложение, реализующее пользовательский интерфейс в стиле современных социальных сетей. Проект построен с использованием Java Swing, FlatLaf для оформления интерфейса и MigLayout для гибкой компоновки элементов.

---

## 📦 Технологии и библиотеки

- 🎨 [FlatLaf](https://github.com/JFormDesigner/FlatLaf) — современная библиотека оформления для Java Swing.
- 🖼️ [MigLayout](https://github.com/mikaelgrev/miglayout) — универсальный и удобный layout manager.
- 📝 [Jackson Databind](https://github.com/FasterXML/jackson) — работа с JSON.
- 📝 [org.json](https://github.com/stleary/JSON-java) — альтернативная работа с JSON.
- 🛠️ [Spring Core](https://spring.io/projects/spring-framework) — базовые компоненты Spring.
- 📡 [Spring Messaging](https://spring.io/projects/spring-framework) — работа с сообщениями.
- 🔌 [Spring WebSocket](https://spring.io/projects/spring-framework) — работа с WebSocket.
- 📡 [javax.websocket-api](https://github.com/javaee/websocket-spec) — API для WebSocket.
- 🐟 [Tyrus](https://tyrus-project.github.io/) — клиентская реализация WebSocket.
- 📦 [Maven Shade Plugin](https://maven.apache.org/plugins/maven-shade-plugin/) — сборка fat-jar.

---

## 📜 Структура Maven-проекта

```text
src/
 └── main/
      └── java/
          └── ru/
              └── dovakun/
                  └── Application.java
pom.xml
```

Собрать проект можно с помощью Maven:
mvn clean package
Собранный fat-jar будет доступен в папке target/, например:
target/TgClone-1.0-SNAPSHOT.jar
Как запустить:
java -jar target/TgClone-1.0-SNAPSHOT.jar


После запуска необходимо зарегистрировать хотя бы пару пользователей что бы посмотреть функционал