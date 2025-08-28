package poly;

import scalar.IntegerScalar;
import scalar.Scalar;

public class Monomial {
    private int exponent;
    private Scalar coefficient;

    public Monomial(int expo, Scalar coefficient){
        this.exponent = expo;
        this.coefficient = coefficient;
    }

    // Add two monomials if exponents match
    public Monomial add(Monomial m){
        if (this.exponent != m.getExponent())
            return null;

        return new Monomial(this.exponent, this.coefficient.add(m.getCoefficient()));
    }

    // Multiply two monomials
    public Monomial mul(Monomial m){
        return new Monomial(this.exponent + m.getExponent(), this.coefficient.mul(m.getCoefficient()));
    }

    // Evaluate monomial with given scalar
    public Scalar evaluate(Scalar s){
        return coefficient.mul(s.power(exponent));
    }

    // Derivative of monomial
    public Monomial derivative(){
        if (exponent == 0)
            return new Monomial(0, new IntegerScalar(0));

        return new Monomial(exponent - 1, coefficient.mul(new IntegerScalar(exponent)));
    }

    // Sign of coefficient
    int sign(){
        return coefficient.sign();
    }

    // Equality check
    @Override
    public boolean equals(Object o){
        if (!(o instanceof Monomial)) return false;

        Monomial m = (Monomial) o;
        return (this.exponent == m.getExponent() || (this.coefficient.equals(new IntegerScalar(0)) && m.coefficient.equals(new IntegerScalar(0)))) && this.coefficient.equals(m.getCoefficient());
    }

    // String representation
    @Override
    public String toString(){
        if (coefficient.sign() == 0) return "0";

        String coeffStr = coefficient.toString();
        if (exponent == 0) return coeffStr;
        if (coefficient.equals(new IntegerScalar(1))) coeffStr = "";
        if (coefficient.equals(new IntegerScalar(-1))) coeffStr = "-";

        if (exponent == 1) return coeffStr + "x";
        return coeffStr + "x^" + exponent;
    }

    public Scalar getCoefficient() {
        return this.coefficient;
    }

    public int getExponent() {
        return this.exponent;
    }
}
