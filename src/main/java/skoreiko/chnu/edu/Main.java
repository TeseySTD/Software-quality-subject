package skoreiko.chnu.edu;


/**
 * @author Rout
 * @version 1.0.0
 * @project Default (Template) Project
 * @class ${NAME}
 * @since 08.10.2026 - 17.01
 */
public class Main {

    public static void main(String[] args) {
        Triangle triangle = new Triangle(3, 4, 5);
        System.out.println(triangle);
        System.out.println("Perimeter: " + triangle.perimeter());
        System.out.println("Area: " + triangle.area());
        System.out.println("Equilateral: " + triangle.isEquilateral());

        Triangle equilateral = new Triangle(2, 2, 2);
        System.out.println(equilateral + " equilateral: " + equilateral.isEquilateral());

        try {
            new Triangle(1, 1, 10);
        } catch (IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}
