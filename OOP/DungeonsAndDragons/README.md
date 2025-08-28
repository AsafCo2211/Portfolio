# 🐉 Dungeons & Dragons – OOP CLI Game (Java)

A text-based role-playing game demonstrating Object-Oriented Programming (OOP) principles  
and design patterns. Developed as part of the Object-Oriented Software Design course at  
Ben-Gurion University.

---

## 📌 Project Status
✅ Fully playable – implemented core mechanics, multiple characters, combat system, and design patterns.

---

## 🎯 Features
- CLI-based role-playing game
- Multiple player classes: Warrior, Mage, Rogue, Hunter
- Enemies: Monsters, Traps, Bosses
- Combat system with attack & defense mechanics
- XP, leveling, and special abilities
- File-based map input
- Implemented **Visitor** & **Observer** design patterns

---

## 🧰 Tech Stack
- Java  
- Object-Oriented Programming (Inheritance, Polymorphism, Encapsulation)  
- Design Patterns: Visitor, Observer  
- JUnit (for testing)

---

## 🗂️ Project Structure
```
DungeonsAndDragons-OOP/
├── .gitignore
├── README.md
└── src/
    ├── cli/
    │   ├── cli.java
    │   └── InputHandler.java
    ├── game/
    │   ├── Board.java
    │   ├── Game.java
    │   └── GameManager.java
    ├── messages/
    │   └── Message.java
    ├── tests/
    │   └── (unit tests)
    ├── tiles/
    │   ├── Empty.java
    │   ├── Position.java
    │   ├── Tile.java
    │   └── Wall.java
    └── units/
        ├── Boss.java
        ├── Enemy.java
        ├── HeroicUnit.java
        ├── Hunter.java
        ├── Mage.java
        ├── Monster.java
        ├── Player.java
        ├── Rogue.java
        ├── Trap.java
        ├── Unit.java
        └── Warrior.java
```

## ▶️ How to Run
```
javac -d out src/**/*.java
java -cp out game.GameManager
```

## 📚 Learning Outcomes
- Applied OOP principles (inheritance, polymorphism, encapsulation)
- Implemented Visitor and Observer design patterns
- Designed a modular multi-class system in Java
- Strengthened testing and CLI application development skills

## 🔗 Links
- Part of my [Portfolio Repository](https://github.com/AsafCo2211/Portfolio)
