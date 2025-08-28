# 🧮 Calculator Assignment – Java CLI Application

A console-based calculator implementing arithmetic operations  
and demonstrating Object-Oriented Programming (OOP) principles.  
Developed as part of the Object-Oriented Software Design coursework at  
Ben-Gurion University.

---

## 📌 Project Status
✅ Completed – full implementation of calculator logic, error handling, and unit tests.

---

## 🎯 Features
- CLI-based calculator
- Supports basic arithmetic: addition, subtraction, multiplication, division
- Extended operations (exponents, square roots, etc.)
- Input validation & error handling (division by zero, invalid input)
- Modular design for adding new operations
- Includes automated unit tests

---

## 🧰 Tech Stack
- Java  
- Object-Oriented Programming (Inheritance, Polymorphism, Encapsulation)  
- Design Patterns (where applicable: Factory / Strategy for operations)  
- JUnit (unit testing)

---

## 🗂️ Project Structure
```
Calculator_Assignment/
├── .gitignore
├── README.md
└── src/
├── calculator/
│ ├── Calculator.java
│ ├── Operation.java
│ ├── Addition.java
│ ├── Subtraction.java
│ ├── Multiplication.java
│ ├── Division.java
│ └── (other operations)
├── cli/
│ ├── CalculatorApp.java
│ └── InputHandler.java
├── utils/
│ └── Validator.java
└── tests/
└── (unit tests)
```

---

## ▶️ How to Run
```
javac -d out src/**/*.java
java -cp out cli.CalculatorApp
```

---

## 📚 Learning Outcomes
- Applied OOP principles (inheritance, polymorphism, encapsulation)  
- Implemented modular design for extensible arithmetic operations  
- Practiced CLI-based input/output and error handling  
- Strengthened Java development and testing skills  

---

## 🔗 Links
- Part of my [Portfolio Repository](https://github.com/AsafCo2211/Portfolio)
