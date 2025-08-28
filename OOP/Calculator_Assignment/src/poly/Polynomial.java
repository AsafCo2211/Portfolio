package poly;

import scalar.IntegerScalar;
import scalar.RationalScalar;
import scalar.Scalar;

import java.util.ArrayList;
import java.util.List;

public class Polynomial {
    private List<Monomial> monomials;

    public Polynomial() {
        monomials = new ArrayList<>();
    }

    // Helper method to add a monomial into the polynomial.
    // Combines like terms and keeps the list sorted by exponent.
    void addMonomial(Monomial m) {
        if (m == null || m.getCoefficient().sign() == 0) {
            return;
        }
        int exp = m.getExponent();
        for (int i = 0; i < monomials.size(); i++) {
            Monomial curr = monomials.get(i);
            if (curr.getExponent() == exp) {
                Monomial sum = curr.add(m);
                if (sum == null || sum.getCoefficient().sign() == 0) {
                    monomials.remove(i);
                } else {
                    monomials.set(i, sum);
                }
                return;
            } else if (curr.getExponent() > exp) {
                monomials.add(i, m);
                return;
            }
        }
        monomials.add(m);
    }

    // Builds a polynomial from a string of coefficients (separated by spaces).
    // The first token corresponds to x^0, the second to x^1, and so on.
    public static Polynomial build(String input) {
        Polynomial poly = new Polynomial();
        String[] tokens = input.trim().split("\\s+");
        for (int i = 0; i < tokens.length; i++) {
            Scalar coeff = parseScalar(tokens[i]);
            Monomial m = new Monomial(i, coeff);
            if (coeff.sign() != 0) {
                poly.addMonomial(m);
            }
        }
        if (poly.monomials.isEmpty()) {
            poly.monomials.add(new Monomial(0, new IntegerScalar(0)));
        }
        return poly;
    }

    // Helper method to convert a token into a scalar.Scalar.
    // If the token contains a '/', it is assumed to be a scalar.RationalScalar,
    // otherwise, an scalar.IntegerScalar.
    private static Scalar parseScalar(String token) {
        if (token.contains("/")) {
            String[] parts = token.split("/");
            int numerator = Integer.parseInt(parts[0]);
            int denominator = Integer.parseInt(parts[1]);
            return new RationalScalar(numerator, denominator);
        } else {
            return new IntegerScalar(Integer.parseInt(token));
        }
    }

    // Returns a new polynomial representing the sum of this polynomial and p.
    public Polynomial add(Polynomial p) {
        Polynomial result = new Polynomial();
        for (Monomial m : this.monomials) {
            result.addMonomial(m);
        }
        for (Monomial m : p.monomials) {
            result.addMonomial(m);
        }
        return result;
    }

    // Returns a new polynomial representing the product of this polynomial and p.
    public Polynomial mul(Polynomial p) {
        Polynomial result = new Polynomial();
        for (Monomial m1 : this.monomials) {
            for (Monomial m2 : p.monomials) {
                result.addMonomial(m1.mul(m2));
            }
        }
        return result;
    }

    // Evaluates the polynomial using the given scalar value.
    public Scalar evaluate(Scalar s) {
        Scalar result = new IntegerScalar(0);
        for (Monomial m : this.monomials) {
            result = result.add(m.evaluate(s));
        }
        return result;
    }

    // Returns a new polynomial that is the derivative of this polynomial.
    public Polynomial derivative() {
        Polynomial result = new Polynomial();
        for (Monomial m : this.monomials) {
            Monomial deriv = m.derivative();
            if (deriv != null && deriv.getCoefficient().sign() != 0) {
                result.addMonomial(deriv);
            }
        }
        if (result.monomials.isEmpty()) {
            result.monomials.add(new Monomial(0, new IntegerScalar(0)));
        }
        return result;
    }

    // Compares this polynomial with another object for equality.
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Polynomial)) {
            return false;
        }
        Polynomial p = (Polynomial) o;
        return this.monomials.equals(p.monomials);
    }

    // Returns a string representation of the polynomial.
    // Terms are shown in increasing order by exponent and are properly signed.
    @Override
    public String toString() {
        if (monomials.isEmpty()) return "0";
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Monomial m : monomials) {
            String term = m.toString();
            if (first) {
                sb.append(term);
                first = false;
            } else {
                if (m.getCoefficient().sign() < 0) {
                    if (term.startsWith("-")) {
                        term = term.substring(1);
                    }
                    sb.append(" - ").append(term);
                } else {
                    sb.append(" + ").append(term);
                }
            }
        }
        return sb.toString();
    }
}
