package scalar;

public class IntegerScalar implements Scalar {

    private int number;

    public IntegerScalar(int num) {
        this.number = num;
    }

    // Double-dispatch: delegate addition to the other scalar.
    @Override
    public Scalar add(Scalar s) {
        return s.addInteger(this);
    }

    // When adding two IntegerScalars, simply add their numbers.
    @Override
    public Scalar addInteger(IntegerScalar s) {
        return new IntegerScalar(s.number + this.number);
    }

    // When adding a scalar.RationalScalar, convert the integer to a rational number.
    @Override
    public Scalar addRational(RationalScalar s) {
        int numerator = (s.getDenominator() * this.number) + s.getNumerator();
        return (new RationalScalar(numerator, s.getDenominator())).reduce();
    }

    // For multiplication, use double dispatch as well.
    @Override
    public Scalar mul(Scalar s) {
        return s.mulInteger(this);
    }

    // Multiplication with another scalar.IntegerScalar.
    // Note: multiplication is commutative.
    public Scalar mulInteger(IntegerScalar s) {
        return new IntegerScalar(s.number * this.number);
    }

    @Override
    public Scalar mulRational(RationalScalar s) {
        int newNumerator = this.number * s.getNumerator();
        return (new RationalScalar(newNumerator, s.getDenominator())).reduce();
    }

    // Returns the negation of this integer.
    @Override
    public Scalar neg() {
        return new IntegerScalar(-this.number);
    }

    // Computes the power (non-negative exponent) of this integer.
    @Override
    public Scalar power(int exponent) {
        if (exponent < 0)
            throw new IllegalArgumentException("Exponent must be non-negative");

        // 0^0 is conventionally handled as 1 in many contexts
        if (exponent == 0)
            return new IntegerScalar(1);

        return new IntegerScalar((int) Math.pow(this.number, exponent));
    }

    // Returns 1 if the number is positive, -1 if negative, 0 if zero.
    @Override
    public int sign() {
        if (number > 0)
            return 1;
        else if (number < 0)
            return -1;
        else
            return 0;
    }

    // Returns a string representation of the integer.
    @Override
    public String toString() {
        return Integer.toString(this.number);
    }

    // Determines equality based on the numeric value.
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Scalar)) return false;

        if (o instanceof IntegerScalar) {
            return this.number == ((IntegerScalar) o).number;
        }

        if (o instanceof RationalScalar) {
            RationalScalar r = (RationalScalar) o;
            // a == (c/d)  ↔  a * d == c
            return this.number * r.getDenominator() == r.getNumerator();
        }
        return false;
    }

    public int getNumber() {
        return this.number;
    }
}
