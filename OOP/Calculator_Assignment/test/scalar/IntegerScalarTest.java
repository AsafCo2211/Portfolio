package scalar;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class IntegerScalarTest {

    @Test
    void testAdd_TwoIntegers() {
        IntegerScalar a = new IntegerScalar(5);
        IntegerScalar b = new IntegerScalar(7);
        Scalar result = a.add(b);

        assertEquals("12", result.toString());
        assertTrue(result instanceof IntegerScalar);
        System.out.println("✅ Test testAdd_TwoIntegers PASSED!");
    }

    @Test
    void testAdd_RationalAndInteger() {
        IntegerScalar a = new IntegerScalar(2);
        // We use 5/10 instead of 1/2 to also test the 'reduce()' logic inside RationalScalar
        RationalScalar b = new RationalScalar(5, 10); // 1/2

        Scalar result = a.addRational(b);
        assertEquals("5/2", result.toString());
        System.out.println("✅ testAdd_RationalAndInteger PASSED!");
    }

    @Test
    void testMul_TwoIntegers() {
        IntegerScalar a = new IntegerScalar(3);
        IntegerScalar b = new IntegerScalar(4);

        Scalar result = a.mul(b);
        assertEquals("12", result.toString());
        assertTrue(result instanceof IntegerScalar);

        System.out.println("✅ testMul_TwoIntegers PASSED!");
    }

    @Test
    void testMulRational_RationalAndInteger() {
        IntegerScalar a = new IntegerScalar(3);
        RationalScalar b = new RationalScalar(4, 10);

        Scalar result = a.mulRational(b); // 3 * 2/5 = 6/5

        assertEquals("6/5", result.toString());
        assertTrue(result instanceof RationalScalar);

        System.out.println("✅ testMulRational_RationalAndInteger PASSED!");
    }

    @Test
    void testNegInteger() {
        IntegerScalar a = new IntegerScalar(8);

        Scalar result = a.neg(); // -8

        assertEquals("-8", result.toString());
        assertTrue(result instanceof IntegerScalar);

        System.out.println("✅ testNegInteger PASSED!");
    }

    @Test
    void testPowerInteger() {
        IntegerScalar a = new IntegerScalar(2);

        Scalar result = a.power(5); // 2^5 = 32

        assertEquals("32", result.toString());
        assertTrue(result instanceof IntegerScalar);

        System.out.println("✅ testPowerInteger PASSED!");
    }

    @Test
    void testPower_zeroExponent() {
        IntegerScalar a = new IntegerScalar(9); // 9^0 = 1

        Scalar result = a.power(0);

        assertEquals("1", result.toString());
        assertTrue(result instanceof IntegerScalar);

        System.out.println("✅ testPower_zeroExponent PASSED!");
    }

    @Test
    void testSign_positiveNumber() {
        IntegerScalar a = new IntegerScalar(10);
        assertEquals(1, a.sign());
        System.out.println("✅ testSign_positiveNumber PASSED!");
    }

    @Test
    void testSign_negativeNumber() {
        IntegerScalar a = new IntegerScalar(-3);
        assertEquals(-1, a.sign());
        System.out.println("✅ testSign_negativeNumber PASSED!");
    }

    @Test
    void testSign_zero() {
        IntegerScalar a = new IntegerScalar(0);
        assertEquals(0, a.sign());
        System.out.println("✅ testSign_zero PASSED!");
    }

    @Test
    void testToString() {
        IntegerScalar a = new IntegerScalar(42);
        assertEquals("42", a.toString());
        System.out.println("✅ testToString PASSED!");
    }

    @Test
    void testEquals() {
        IntegerScalar a = new IntegerScalar(7);
        IntegerScalar b = new IntegerScalar(7);

        assertTrue(a.equals(b));
        System.out.println("✅ testEquals PASSED!");
    }










}
