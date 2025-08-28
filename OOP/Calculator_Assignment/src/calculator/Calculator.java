package calculator;

import poly.Polynomial;
import scalar.IntegerScalar;
import scalar.RationalScalar;
import scalar.Scalar;

import java.util.Scanner;

public class Calculator {
    private Scanner _sc;
    private boolean contProg;

    public static void main(String[] args) {
        Calculator calc = new Calculator();
        calc.run();
    }

    public void run() {
        this.contProg = true;
        this._sc = new Scanner(System.in);
        System.out.println("~~~Welcome to the poly.poly.Polynomial calculator.Calculator!~~~\n");
        this.mainMenu();
    }

    private void mainMenu() {
        while (this.contProg) {
            System.out.println("Please select an operation:");
            System.out.println("1. Addition");
            System.out.println("2. Multiplication");
            System.out.println("3. Evaluation");
            System.out.println("4. Derivative");
            System.out.println("5. Exit");
            try {
                int tUserInput = this._sc.nextInt();
                switch (tUserInput) {
                    case 1: {
                        this.userAddition();
                        break;
                    }
                    case 2: {
                        this.userMultiplication();
                        break;
                    }
                    case 3: {
                        this.userEvaluation();
                        break;
                    }
                    case 4: {
                        this.userDerivative();
                        break;
                    }
                    case 5: {
                        this.exit();
                        break;
                    }
                    default: {
                        this.tryAgain();
                    }
                }
                System.out.println();
            }
            catch (Exception e) {
                this.tryAgain();
            }
        }
    }

    private void tryAgain() {
        System.out.println("Invalid number, please try again\n");
        this.mainMenu();
    }

    private void exit() {
        this.contProg = false;
        System.out.println("Thank you for using our calculator! :)");
    }

    private void userDerivative() {
        System.out.println("You have selected: Derivative");
        System.out.println("Please insert polynomial and scalar in separate lines:");
        this._sc.nextLine();
        Polynomial p = Polynomial.build(this._sc.nextLine());
        Scalar s = this.stringToScalar(this._sc.next());
        String st1 = "The derivative polynomial is: ";
        String st2 = "The result of the derivation is: ";
        Polynomial derP = p.derivative();
        System.out.println(st1 + derP.toString());
        System.out.println(st2 + derP.evaluate(s).toString());
    }

    private void userEvaluation() {
        System.out.println("You have selected: Evaluation");
        System.out.println("Please insert polynomial and scalar in separate lines:");
        this._sc.nextLine();
        Polynomial p = Polynomial.build(this._sc.nextLine());
        Scalar s = this.stringToScalar(this._sc.next());
        String st = "The result of the evaluation is: ";
        System.out.println(st + p.evaluate(s).toString());
    }

    private void userMultiplication() {
        System.out.println("You have selected: Multiplication");
        System.out.println("Please insert 2 polynomials in separate lines:");
        this._sc.nextLine();
        Polynomial p1 = Polynomial.build(this._sc.nextLine());
        Polynomial p2 = Polynomial.build(this._sc.nextLine());
        System.out.println("The result of the multiplication is: " + p1.mul(p2).toString());
    }

    private void userAddition() {
        System.out.println("You have selected: Addition");
        System.out.println("Please insert 2 polynomials in separate lines:");
        this._sc.nextLine();
        Polynomial p1 = Polynomial.build(this._sc.nextLine());
        Polynomial p2 = Polynomial.build(this._sc.nextLine());
        System.out.println("The result of the addition is: " + p1.add(p2).toString());
    }

    private Scalar stringToScalar(String s) {
        if (s.contains("/")) {
            // It's a rational number
            String[] parts = s.split("/");
            int numerator = Integer.parseInt(parts[0].trim());
            int denominator = Integer.parseInt(parts[1].trim());
            return new RationalScalar(numerator, denominator);
        } else {
            // It's an integer
            int value = Integer.parseInt(s.trim());
            return new IntegerScalar(value);
        }
    }

}
