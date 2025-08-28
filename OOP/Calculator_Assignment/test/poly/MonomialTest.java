package poly;

import org.junit.jupiter.api.Test;
import scalar.IntegerScalar;
import scalar.RationalScalar;
import scalar.Scalar;

import static org.junit.jupiter.api.Assertions.*;

public class MonomialTest {

    @Test
    public void testAddSameExponent() {
        Monomial m1 = new Monomial(2, new IntegerScalar(3));
        Monomial m2 = new Monomial(2, new IntegerScalar(5));
        Monomial sum = m1.add(m2);
        assertNotNull(sum);
        assertEquals(2, sum.getExponent());
        assertEquals(new IntegerScalar(8), sum.getCoefficient());
    }

    @Test
    public void testAddDifferentExponent() {
        Monomial m1 = new Monomial(2, new IntegerScalar(3));
        Monomial m2 = new Monomial(3, new IntegerScalar(5));
        assertNull(m1.add(m2), "Adding monomials with different exponents should return null");
    }

    @Test
    public void testMul() {
        Monomial m1 = new Monomial(2, new IntegerScalar(3));
        Monomial m2 = new Monomial(3, new IntegerScalar(4));
        Monomial product = m1.mul(m2);
        assertEquals(5, product.getExponent());
        assertEquals(new IntegerScalar(12), product.getCoefficient());
    }

    @Test
    public void testEvaluateWithIntegerScalar() {
        Monomial m = new Monomial(3, new IntegerScalar(2)); // 2x^3
        Scalar result = m.evaluate(new IntegerScalar(3)); // 2 * 3^3 = 54
        assertEquals(new IntegerScalar(54), result);
    }

    @Test
    public void testEvaluateWithRationalScalar() {
        Monomial m = new Monomial(2, new IntegerScalar(2)); // 2x^2
        Scalar input = new RationalScalar(2, 3);         // 2/3
        // 2 * (2/3)^2 = 2 * 4/9 = 8/9
        Scalar expected = new RationalScalar(8, 9);
        Scalar result = m.evaluate(input);
        assertEquals(expected, result);
    }

    @Test
    public void testDerivativeNonZeroExponent() {
        Monomial m = new Monomial(4, new IntegerScalar(3)); // 3x^4
        Monomial deriv = m.derivative();                    // 12x^3
        assertEquals(3, deriv.getExponent());
        assertEquals(new IntegerScalar(12), deriv.getCoefficient());
    }

    @Test
    public void testDerivativeZeroExponent() {
        Monomial m = new Monomial(0, new IntegerScalar(5)); // constant 5
        Monomial deriv = m.derivative();                    // 0
        assertEquals(0, deriv.getExponent());
        assertEquals(new IntegerScalar(0), deriv.getCoefficient());
    }

    @Test
    public void testSignPositiveNegativeZero() {
        Monomial pos = new Monomial(1, new IntegerScalar(5));
        Monomial neg = new Monomial(1, new IntegerScalar(-3));
        Monomial zero = new Monomial(2, new IntegerScalar(0));

        assertEquals(1, pos.sign());
        assertEquals(-1, neg.sign());
        assertEquals(0, zero.sign());
    }

    @Test
    public void testEqual() {
        Monomial m1 = new Monomial(3, new IntegerScalar(7));
        Monomial m2 = new Monomial(3, new IntegerScalar(7));
        Monomial m3 = new Monomial(3, new IntegerScalar(8));
        Monomial otherType = null;

        assertTrue(m1.equals(m2));
        assertFalse(m1.equals(m3));
        assertFalse(m1.equals("not a monomial"));
    }

    @Test
    public void testToStringVariousCases() {
        // Zero coefficient
        Monomial zero = new Monomial(5, new IntegerScalar(0));
        assertEquals("0", zero.toString());

        // Exponent 0
        Monomial constant = new Monomial(0, new IntegerScalar(4));
        assertEquals("4", constant.toString());

        // Coefficient 1, exponent >1
        Monomial xCubed = new Monomial(3, new IntegerScalar(1));
        assertEquals("x^3", xCubed.toString());

        // Coefficient -1, exponent >1
        Monomial negX7 = new Monomial(7, new IntegerScalar(-1));
        assertEquals("-x^7", negX7.toString());

        // Exponent 1
        Monomial xTimes5 = new Monomial(1, new IntegerScalar(4));
        assertEquals("4x", xTimes5.toString());

        // General case
        Monomial general = new Monomial(2, new IntegerScalar(-5));
        assertEquals("-5x^2", general.toString());
    }
}
