package scalar;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class RationalScalarTest {

    @Test
    void testAddRational() {
        RationalScalar a = new RationalScalar(1, 3);
        RationalScalar b = new RationalScalar(1, 6);
        Scalar result = a.addRational(b); // (1/3 + 1/6) = 1/2
        assertEquals("1/2", result.toString());
        System.out.println("✅ Test testAddRational PASSED!");
    }

    @Test
    void testAddInteger() {
        RationalScalar r = new RationalScalar(3, 4);
        IntegerScalar i = new IntegerScalar(2);
        Scalar result = r.addInteger(i); // 3/4 + 2 = 11/4
        assertEquals("11/4", result.toString());
        System.out.println("✅ Test testAddInteger PASSED!");
    }

    @Test
    void testMulRational() {
        RationalScalar a = new RationalScalar(2, 3);
        RationalScalar b = new RationalScalar(3, 4);
        Scalar result = a.mulRational(b); // (2*3)/(3*4) = 6/12 = 1/2
        assertEquals("1/2", result.toString());
        System.out.println("✅ Test testMulRational PASSED!");
    }

    @Test
    void testMulInteger() {
        RationalScalar r = new RationalScalar(5, 7);
        IntegerScalar i = new IntegerScalar(3);
        Scalar result = r.mulInteger(i); // (5*3)/7 = 15/7
        assertEquals("15/7", result.toString());
        System.out.println("✅ Test testMulInteger PASSED!");
    }

    @Test
    void testNeg() {
        RationalScalar r = new RationalScalar(5, 7);
        Scalar neg = r.neg(); // -5/7
        assertEquals("-5/7", neg.toString());
        System.out.println("✅ Test testNeg PASSED!");
    }

    @Test
    void testPower_PositiveExponent() {
        RationalScalar r = new RationalScalar(6, 8);
        Scalar result = r.power(2); // (6/8)^2 = 36/64
        assertEquals("9/16", result.toString());
        System.out.println("✅ Test testPower_PositiveExponent PASSED!");
    }

    @Test
    void testPower_ZeroExponent() {
        RationalScalar r = new RationalScalar(5, 7);
        Scalar result = r.power(0); // any number ^ 0 = 1
        assertEquals("1", result.toString());
        System.out.println("✅ Test testPower_ZeroExponent PASSED!");
    }

    @Test
    void testPower_NegativeExponent() {
        RationalScalar r = new RationalScalar(2, 3);
        Scalar result = r.power(-1); // (2/3)^(-1) = 3/2
        assertEquals("3/2", result.toString());
        System.out.println("✅ Test testPower_NegativeExponent PASSED!");
    }

    @Test
    void testSign_Positive() {
        RationalScalar r = new RationalScalar(3, 5);
        assertEquals(1, r.sign());
        System.out.println("✅ Test testSign_Positive PASSED!");
    }

    @Test
    void testSign_Negative() {
        RationalScalar r = new RationalScalar(-4, 7);
        assertEquals(-1, r.sign());
        System.out.println("✅ Test testSign_Negative PASSED!");
    }

    @Test
    void testSign_Zero() {
        RationalScalar r = new RationalScalar(0, 1);
        assertEquals(0, r.sign());
        System.out.println("✅ Test testSign_Zero PASSED!");
    }

    @Test
    void testReduce() {
        RationalScalar r = new RationalScalar(96, 108); // Should reduce to 8/9
        assertEquals("8/9", r.reduce().toString());
        System.out.println("✅ Test testReduce PASSED!");
    }

    @Test
    void testToString_Integer() {
        RationalScalar r = new RationalScalar(10, 5); // Should return "2"
        assertEquals("2", r.toString());
        System.out.println("✅ Test testToString_Integer PASSED!");
    }

    @Test
    void testToString_NegativeDenominator() {
        RationalScalar r = new RationalScalar(3, -4); // Should return "-3/4"
        assertEquals("-3/4", r.toString());
        System.out.println("✅ Test testToString_NegativeDenominator PASSED!");
    }

    @Test
    void testToString_NegativeNumeratorAndDenominator() {
        RationalScalar r = new RationalScalar(-3, -4); // Should return "3/4"
        assertEquals("3/4", r.toString());
        System.out.println("✅ Test testToString_NegativeNumeratorAndDenominator PASSED!");
    }

    @Test
    void testEquals_ReducedForm() {
        RationalScalar a = new RationalScalar(2, 4); // reduces to 1/2
        RationalScalar b = new RationalScalar(1, 2);
        assertTrue(a.equals(b));
    }

    @Test
    void testEquals_Different() {
        RationalScalar a = new RationalScalar(1, 3);
        RationalScalar b = new RationalScalar(2, 3);
        assertFalse(a.equals(b));
        System.out.println("✅ Test testEquals_Different PASSED!");
    }
}
