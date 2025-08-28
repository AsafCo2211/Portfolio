package scalar;

public interface Scalar {

    // accepts a scalar argument and returns the sum of the current scalar and the argument.
    Scalar add(Scalar s);

    // accepts a scalar argument and returns the current scalar’s and the argument’s multiplication.
    Scalar mul(Scalar s);

    Scalar addInteger(IntegerScalar s);

    Scalar addRational(RationalScalar s);

    Scalar mulInteger(IntegerScalar integerScalar);

    Scalar mulRational(RationalScalar rationalScalar);

    // returns the negation of the current scalar.
    Scalar neg();

    // accepts a non-negative integer argument and returns the power of the scalar by the exponent argument.
    Scalar power(int exponent);

    // returns 1 for positive scalar, -1 for negative, and 0 for 0.
    int sign();


}
