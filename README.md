# 🏆 Online Chess Game

A modern Java-based online chess game with real-time multiplayer capabilities, built using JavaFX.

![Chess Game Screenshot](screenshot.png)

## 🚀 Features

- 🎮 Real-time multiplayer gameplay
- ⏱️ Timer system for both players
- 🎨 Modern and intuitive UI
- ♟️ Standard chess rules implementation
- 📱 Responsive design
- 🔄 Draw and resign options
- 🎯 Move validation and check detection
- 💬 Game status notifications

## 📋 Prerequisites

- Java 14 or higher
- JavaFX 17 or higher
- Maven (for building)

## 🛠️ Installation

1. Clone the repository:
```bash
git clone https://github.com/yourusername/chess.git
cd chess
```

2. Build the project:
```bash
mvn clean install
```

3. Run the game:
```bash
mvn javafx:run
```

## 🎮 How to Play

### Starting a Game

1. Launch the application
2. Choose to start as either:
   - Server (White pieces)
   - Client (Black pieces)
3. Enter your name
4. If joining as client, enter the server's IP address

### Game Controls

- Click on a piece to select it
- Click on a highlighted square to move
- Use the control panel to:
  - Offer a draw
  - Resign the game

### Game Rules

- Standard chess rules apply
- Each player has 10 minutes initially
- The game ends when:
  - Checkmate is achieved
  - A player resigns
  - A draw is agreed upon
  - A player's time runs out

## 🎨 UI Features

- Hover effects on pieces
- Visual move validation
- Clear turn indicators
- Timer display for both players
- Game status messages
- Confirmation dialogs for important actions

## 🛠️ Development

### Project Structure

```
chess/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   ├── Main.java
│   │   │   ├── NetworkConnection.java
│   │   │   ├── Server.java
│   │   │   └── Client.java
│   │   └── resources/
├── pom.xml
└── README.md
```

### Building from Source

1. Ensure you have Java 14+ and Maven installed
2. Clone the repository
3. Run `mvn clean install`
4. The executable JAR will be in the `target` directory

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- JavaFX for the UI framework
- Unicode chess symbols
- All contributors and testers

## 📞 Support

For support, email divye.prakash07@gmail.com or open an issue in the repository.

---

<div align="center">
  <sub>Built with ❤️ by Team SSEHC[-1]</sub>
</div> 
