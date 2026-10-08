package skoreiko.chnu.edu;

/**
 * @author Rout
 * @version 1.0.0
 * @project Default (Template) Project
 * @class ${NAME}
 * @since 08.10.2026 - 17.01
 */

/**
 * Immutable triangle defined by the lengths of its three sides.
 *
 * @param a first side
 * @param b second side
 * @param c third side
 */
public record Triangle(double a, double b, double c) {

    /**
     * Validates the sides when the object is created.
     *
     * @throws IllegalArgumentException if a triangle with such sides does not exist
     */
    public Triangle {
        if (!isValid(a, b, c)) {
            throw new IllegalArgumentException(
                    "Triangle does not exist with sides: " + a + ", " + b + ", " + c);
        }
    }

    /**
     * Checks whether a triangle with the given sides exists.
     * The sides must be finite and positive and satisfy the triangle inequality.
     */
    public static boolean isValid(double a, double b, double c) {
        return isPositiveFinite(a) && isPositiveFinite(b) && isPositiveFinite(c)
                && a + b > c
                && a + c > b
                && b + c > a;
    }

    private static boolean isPositiveFinite(double side) {
        return side > 0 && Double.isFinite(side);
    }

    /** Returns the perimeter of the triangle. */
    public double perimeter() {
        return a + b + c;
    }

    /** Returns the area of the triangle using Heron's formula. */
    public double area() {
        double p = perimeter() / 2;
        return Math.sqrt(p * (p - a) * (p - b) * (p - c));
    }

    /** Returns true if all three sides are equal. */
    public boolean isEquilateral() {
        return a == b && b == c;
    }
}