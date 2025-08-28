package poly;

import org.junit.jupiter.api.Test;
import scalar.IntegerScalar;
import scalar.RationalScalar;
import scalar.Scalar;

import static org.junit.jupiter.api.Assertions.*;

public class PolynomialTest {

    @Test
    public void testBuildSimple() {
        Polynomial p = Polynomial.build("1 0  2 3"); // 1 + 2x + 3x^2
        assertEquals("1 + 2x^2 + 3x^3", p.toString());
    }

    @Test
    public void testBuildLeadingZeros() {
        Polynomial p = Polynomial.build("0 1 2 3"); // x + 2x^2 + 3x^3
        assertEquals("x + 2x^2 + 3x^3", p.toString());
    }

    @Test
    public void testBuildHighExponent() {
        Polynomial p = Polynomial.build("0 0 0 0 0 0 7"); // 7x^6
        assertEquals("7x^6", p.toString());
    }

    @Test
    public void testBuildConstant() {
        Polynomial p = Polynomial.build("5"); // 5
        assertEquals("5", p.toString());
    }

    @Test
    public void testBuildNegativeAndRational() {
        Polynomial p = Polynomial.build("1 -2 3"); // 1 - 2x + 3x^2
        assertEquals("1 - 2x + 3x^2", p.toString());

        Polynomial q = Polynomial.build("0 1/2 3 -5/3"); // 1/2x + 3x^2 - 5/3x^3
        assertEquals("1/2x + 3x^2 - 5/3x^3", q.toString());
    }

    @Test
    public void testAddPolynomials() {
        Polynomial p1 = Polynomial.build("1 2 3"); // 1 + 2x + 3x^2
        Polynomial p2 = Polynomial.build("3 0 -1 4"); // 3 - x^2 + 4x^3
        Polynomial sum = p1.add(p2); // 4 + 2x + 2x^2 + 4x^3
        assertEquals("4 + 2x + 2x^2 + 4x^3", sum.toString());
    }

    @Test
    public void testMultiplyPolynomials() {
        Polynomial p = Polynomial.build("1 1"); // 1 + x
        Polynomial q = Polynomial.build("1 -1"); // 1 - x
        Polynomial prod = p.mul(q); // 1 - x^2
        assertEquals("1 - x^2", prod.toString());

        Polynomial r = Polynomial.build("2 0 3"); // 2 + 3x^2
        Polynomial s = Polynomial.build("0 4");   // 4x
        Polynomial prod2 = r.mul(s); // 8x + 12x^3
        assertEquals("8x + 12x^3", prod2.toString());
    }

    @Test
    public void testEvaluatePolynomial() {
        Polynomial p = Polynomial.build("2 0 3"); // 2 + 3x^2
        Scalar resultInt = p.evaluate(new IntegerScalar(2)); // 2 + 3*4 = 14
        assertEquals(new IntegerScalar(14), resultInt);

        Scalar resultRat = p.evaluate(new RationalScalar(1, 2)); // 2 + 3*(1/4) = 2 + 3/4 = 11/4
        assertEquals(new RationalScalar(11, 4), resultRat);
    }

    @Test
    public void testDerivative() {
        Polynomial p = Polynomial.build("5 3 -1 4"); // 5 + 3x - x^2 + 4x^3
        Polynomial deriv = p.derivative(); // 3 - 2x + 12x^2
        assertEquals("3 - 2x + 12x^2", deriv.toString());

        Polynomial constant = Polynomial.build("7"); // constant
        Polynomial dConst = constant.derivative(); // 0
        assertEquals("0", dConst.toString());
    }

    @Test
    public void testEquals() {
        Polynomial p1 = Polynomial.build("1 2 3");
        Polynomial p2 = Polynomial.build("1 2 3");
        Polynomial p3 = Polynomial.build("1 2");
        assertTrue(p1.equals(p2));
        assertFalse(p1.equals(p3));
        assertFalse(p1.equals("not a polynomial"));
    }

    @Test
    public void testToStringFormatting() {
        Polynomial p = Polynomial.build("0 -1 1/2 0 3"); // -x + 1/2x^2 + 3x^4
        assertEquals("-x + 1/2x^2 + 3x^4", p.toString());

        Polynomial zeroPoly = Polynomial.build("0 0"); // zero polynomial
        assertEquals("0", zeroPoly.toString());
    }
}
