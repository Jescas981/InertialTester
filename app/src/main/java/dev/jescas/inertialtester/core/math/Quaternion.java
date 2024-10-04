package dev.jescas.inertialtester.core.math;

import org.ejml.data.FMatrix4;

public class Quaternion {
    private FMatrix4 quaternion;

    // Constructor
    public Quaternion(float w, float x, float y, float z) {
        this.quaternion = new FMatrix4(w, x, y, z);
    }

    // Identity quaternion
    public static Quaternion identity() {
        return new Quaternion(1.0f, 0.0f, 0.0f, 0.0f);
    }

    // Conjugate of the quaternion
    public Quaternion conjugate() {
        return new Quaternion(quaternion.a1, -quaternion.a2, -quaternion.a3, -quaternion.a4);
    }

    // Normalize the quaternion
    public Quaternion normalize() {
        float norm = (float) Math.sqrt(normSquared());

        if (norm == 0) {
            throw new IllegalStateException("Cannot normalize a zero-length quaternion.");
        }

        return new Quaternion(
                quaternion.a1 / norm,
                quaternion.a2 / norm,
                quaternion.a3 / norm,
                quaternion.a4 / norm
        );
    }

    // Inverse of the quaternion
    public Quaternion inverse() {
        Quaternion conjugate = this.conjugate();
        float normSquared = this.normSquared();
        return new Quaternion(
                conjugate.quaternion.a1 / normSquared,
                conjugate.quaternion.a2 / normSquared,
                conjugate.quaternion.a3 / normSquared,
                conjugate.quaternion.a4 / normSquared
        );
    }

    // Compute the norm squared of the quaternion
    private float normSquared() {
        return (quaternion.a1 * quaternion.a1 +
                quaternion.a2 * quaternion.a2 +
                quaternion.a3 * quaternion.a3 +
                quaternion.a4 * quaternion.a4);
    }

    // Multiply two quaternions
    public Quaternion mult(Quaternion other) {
        float w = quaternion.a1 * other.quaternion.a1 -
                quaternion.a2 * other.quaternion.a2 -
                quaternion.a3 * other.quaternion.a3 -
                quaternion.a4 * other.quaternion.a4;

        float x = quaternion.a1 * other.quaternion.a2 +
                quaternion.a2 * other.quaternion.a1 +
                quaternion.a3 * other.quaternion.a4 -
                quaternion.a4 * other.quaternion.a3;

        float y = quaternion.a1 * other.quaternion.a3 -
                quaternion.a2 * other.quaternion.a4 +
                quaternion.a3 * other.quaternion.a1 +
                quaternion.a4 * other.quaternion.a2;

        float z = quaternion.a1 * other.quaternion.a4 +
                quaternion.a2 * other.quaternion.a3 -
                quaternion.a3 * other.quaternion.a2 +
                quaternion.a4 * other.quaternion.a1;

        return new Quaternion(w, x, y, z);
    }

    // Getters for the quaternion components
    public float w() {
        return quaternion.a1;
    }

    public float x() {
        return quaternion.a2;
    }

    public float y() {
        return quaternion.a3;
    }

    public float z() {
        return quaternion.a4;
    }
}
