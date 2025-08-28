package scalar;

public class RationalScalar implements Scalar {

    private int numerator;
    private int denominator;

    public RationalScalar(int numer, int denom) {
        if(denom < 0){
            numer = -numer;
            denom = -denom;
        }

        this.numerator = numer;
        this.denominator = denom;
    }

    @Override
    public Scalar add(Scalar s) {
        return s.addRational(this);
    }

    // When adding an scalar.IntegerScalar to this scalar.RationalScalar.
    // Computes: (this.numerator / this.denominator) + (integer) = (this.numerator + integer*this.denom) / this.denom.
    @Override
    public Scalar addInteger(IntegerScalar s) {
        int newNumerator = this.numerator + s.getNumber() * this.denominator;
        return new RationalScalar(newNumerator, this.denominator).reduce();
    }

    // When adding two RationalScalars.
    // Computes: a/b + c/d = (a*d + c*b) / (b*d).
    @Override
    public Scalar addRational(RationalScalar s) {
        int newNumerator = this.numerator * s.getDenominator() + s.getNumerator() * this.denominator;
        int newDenom = this.denominator * s.getDenominator();
        return new RationalScalar(newNumerator, newDenom).reduce();
    }

    // Delegates multiplication to the other scalar's mulRational() method.
    @Override
    public Scalar mul(Scalar s) {
        return s.mulRational(this);
    }

    // Multiplication with an scalar.IntegerScalar.
    // Computes: (this.numerator/this.denom) * (integer) = (integer*this.numerator)/this.denom.
    public Scalar mulInteger(IntegerScalar s) {
        int newNumerator = s.getNumber() * this.numerator;
        int newDenom = this.denominator;
        return new RationalScalar(newNumerator, newDenom).reduce();
    }

    // Multiplication with another scalar.RationalScalar.
    // Computes: (a/b)*(c/d) = (a*c)/(b*d).
    public Scalar mulRational(RationalScalar s) {
        int newNumerator = this.numerator * s.getNumerator();
        int newDenom = this.denominator * s.getDenominator();
        return new RationalScalar(newNumerator, newDenom).reduce();
    }

    // Returns the negation of this rational: -a/b.
    @Override
    public Scalar neg() {
        return new RationalScalar(-this.numerator, this.denominator);
    }

    // Returns the power of this rational raised to a non-negative exponent.
    // For negative exponent, this implementation inverts the rational number.
    @Override
    public Scalar power(int exponent) {
        if (exponent < 0) {
            if (this.numerator == 0)
                throw new ArithmeticException("Cannot raise zero to a negative power");

            return (new RationalScalar(this.denominator, this.numerator)).power(-exponent);
        }
        if (exponent == 0)
            return new IntegerScalar(1);

        int newNumerator = (int) Math.pow(this.numerator, exponent);
        int newDenominator = (int) Math.pow(this.denominator, exponent);
        return new RationalScalar(newNumerator, newDenominator).reduce();
    }

    // Returns the sign of the rational number.
    // The result is 1 for positive, -1 for negative, and 0 if the numerator is zero.
    @Override
    public int sign() {
        if (this.numerator == 0)
            return 0;
        int s = 1;
        if (this.numerator < 0)
            s *= -1;
        if (this.denominator < 0)
            s *= -1;
        return s;
    }

    // Returns a new scalar.RationalScalar representing this rational number in its lowest terms.
    public RationalScalar reduce() {
        int gcd = gcd(Math.abs(numerator), Math.abs(denominator));
        int rNumerator = numerator / gcd;
        int rDenom = denominator / gcd;

        return new RationalScalar(rNumerator, rDenom);
    }

    // Helper method to compute the greatest common divisor.
    private int gcd(int a, int b) {
        if (b == 0)
            return a;
        return gcd(b, a % b);
    }

    // Returns a string that represents the rational number.
    // If the rational can be represented as an integer (denom equals 1), it returns just that integer.
    // Otherwise, it returns a fraction in the format "numerator/denominator".
    @Override
    public String toString() {
        RationalScalar reduced = this.reduce();
        if (reduced.denominator == 1)
            return Integer.toString(reduced.numerator);
        else
            return reduced.numerator + "/" + reduced.denominator;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Scalar)) return false;

        if (o instanceof IntegerScalar) {
            int n = ((IntegerScalar) o).getNumber();
            //  a/b == n   ↔   a == n * b
            return this.numerator == n * this.denominator;
        }

        RationalScalar other = (RationalScalar) o;
        return this.numerator * other.denominator == other.numerator * this.denominator;
    }

    public int getNumerator() {
        return this.numerator;
    }

    public int getDenominator() {
        return this.denominator;
    }
}
