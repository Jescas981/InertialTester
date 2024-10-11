package dev.jescas.inertialtester.core.math;

import org.ejml.data.FMatrix3;
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

    // Rotate a vector using the quaternion
    public FMatrix3 rotateVector(FMatrix3 vector) {
        // Convert the vector to a quaternion (w = 0)
        Quaternion vectorQuat = new Quaternion(0.0f, vector.a1, vector.a2, vector.a3);

        // Rotate the vector: q * v * q^(-1)
        Quaternion rotatedQuat = this.mult(vectorQuat).mult(this.inverse());

        // Return the rotated vector, which is contained in the x, y, z components
        return new FMatrix3(rotatedQuat.x(), rotatedQuat.y(), rotatedQuat.z());
    }

    // Get Euler angles from the quaternion
    public FMatrix3 getEulerAngles() {
        float qw = quaternion.a1;
        float qx = quaternion.a2;
        float qy = quaternion.a3;
        float qz = quaternion.a4;

        // Compute roll (x-axis rotation)
        double t0 = +2.0 * (qw * qx + qy * qz);
        double t1 = +1.0 - 2.0 * (qx * qx + qy * qy);
        double roll = Math.atan2(t0, t1);

        // Compute pitch (y-axis rotation)
        double t2 = +2.0 * (qw * qy - qz * qx);
        t2 = Math.min(1.0, Math.max(t2, -1.0)); // Clamp t2 to the range [-1, 1]
        double pitch = Math.asin(t2);

        // Compute yaw (z-axis rotation)
        double t3 = +2.0 * (qw * qz + qx * qy);
        double t4 = +1.0 - 2.0 * (qy * qy + qz * qz);
        double yaw = Math.atan2(t3, t4);

        // Create a new FMatrix3 to hold the Euler angles
        FMatrix3 eulerAngles = new FMatrix3();
        eulerAngles.set(0, 0, (float) Math.toDegrees(roll));   // Roll
        eulerAngles.set(1, 0, (float) Math.toDegrees(pitch));  // Pitch
        eulerAngles.set(2, 0, (float) Math.toDegrees(yaw));    // Yaw

        return eulerAngles; // Return the matrix containing the Euler angles
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
