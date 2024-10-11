package dev.jescas.inertialtester.core.algorithms;

import org.ejml.data.DMatrix3;
import org.ejml.data.DMatrix4;
import org.ejml.data.DMatrix6;
import org.ejml.data.FMatrix3;
import org.ejml.data.FMatrix6;
import org.ejml.dense.fixed.CommonOps_DDF3;
import org.ejml.dense.fixed.CommonOps_DDF4;
import org.ejml.dense.fixed.NormOps_FDF3;
import org.ejml.dense.row.CommonOps_DDRM;

import dev.jescas.inertialtester.core.math.Quaternion;

public class MadgwickFilter {
    // Initialize quaternion
    private double qw = 1;
    private double qx = 0;
    private double qy = 0;
    private double qz = 0;
    // Gain filter
    private double beta = 4e-1;

    public void Feed(FMatrix3 acc_orig,  FMatrix3 mag_orig, FMatrix3 gyro_orig, double dt) {
        FMatrix3 acc = acc_orig.copy();
        FMatrix3 mag = mag_orig.copy();
        NormOps_FDF3.normalizeF(acc);
        NormOps_FDF3.normalizeF(mag);

        float ax = acc.a1;
        float ay = acc.a2;
        float az = acc.a3;

        float gx = gyro_orig.a1;
        float gy = gyro_orig.a2;
        float gz = gyro_orig.a3;

        float mx = mag.a1;
        float my = mag.a2;
        float mz = mag.a3;

        double bx = Math.sqrt(mx*mx+my*my);
        double bz = mz;

        // Derivate of q
        double dqw = 0.5 * -(qw * gx + qx * gy + qy * gz);
        double dqx = 0.5 * (qw * gx + qy * gz - qz * gy);
        double dqy = 0.5 * (qw * gy - qx * gz + qz * gx);
        double dqz = 0.5 * (qw * gz + qx * gy - qy * gx);

        // Gradient calculations
        double gradw = 2 * ax * qy - 2 * ay * qx +
                4 * bx * bx * qw * (qy * qy + qz * qz) -
                8 * bx * bz * qw * qx * qz +
                2 * bx * (my * qz - mz * qy) +
                4 * bz * bz * qw * (qx * qx + qy * qy) -
                2 * bz * my * qx + 4 * qw * (qx * qx + qy * qy);

        double gradx = -2 * bz * qz * (2 * bx * (qy * qy + qz * qz - 0.5) +
                2 * bz * (qw * qy - qx * qz) + mx) +
                2 * qw * (-ay + 2 * qw * qx + 2 * qy * qz) +
                4 * qx * (az + 2 * (qx * qx + qy * qy) - 1.0) -
                2 * qz * (ax + 2 * qw * qy - 2 * qx * qz) -
                2 * (bx * qy + bz * qw) * (2 * bx * (qw * qz - qx * qy) -
                        2 * bz * (qw * qx + qy * qz) + my) -
                2 * (bx * qz - 2 * bz * qx) * (-2 * bx * (qw * qy + qx * qz) +
                        2 * bz * (qx * qx + qy * qy - 0.5) + mz);

        double grady = 2 * qw * (ax + 2 * qw * qy - 2 * qx * qz) +
                4 * qy * (az + 2 * (qx * qx + qy * qy) - 1.0) +
                2 * qz * (-ay + 2 * qw * qx + 2 * qy * qz) -
                2 * (bx * qw - 2 * bz * qy) * (-2 * bx * (qw * qy + qx * qz) +
                        2 * bz * (qx * qx + qy * qy - 0.5) + mz) -
                2 * (bx * qx + bz * qz) * (2 * bx * (qw * qz - qx * qy) -
                        2 * bz * (qw * qx + qy * qz) + my) +
                2 * (2 * bx * qy + bz * qw) * (2 * bx * (qy * qy + qz * qz - 0.5) +
                        2 * bz * (qw * qy - qx * qz) + mx);

        double gradz = -2 * bx * qx * (-2 * bx * (qw * qy + qx * qz) +
                2 * bz * (qx * qx + qy * qy - 0.5) + mz) -
                2 * qx * (ax + 2 * qw * qy - 2 * qx * qz) +
                2 * qy * (-ay + 2 * qw * qx + 2 * qy * qz) +
                2 * (bx * qw - bz * qy) * (2 * bx * (qw * qz - qx * qy) -
                        2 * bz * (qw * qx + qy * qz) + my) +
                2 * (2 * bx * qz - bz * qx) * (2 * bx * (qy * qy + qz * qz - 0.5) +
                        2 * bz * (qw * qy - qx * qz) + mx);

        qw += (dqw - beta * gradw) * dt; // Apply gradient update if needed
        qx += (dqx - beta * gradx) * dt; // Apply gradient update if needed
        qy += (dqy - beta * grady) * dt; // Apply gradient update if needed
        qz += (dqz - beta * gradz) * dt; // Apply gradient update if needed


        // Normalize quaternion
        double norm = Math.sqrt(qw * qw + qx * qx + qy * qy + qz * qz);
        qw /= norm;
        qx /= norm;
        qy /= norm;
        qz /= norm;
    }

    public FMatrix3 GetEulerAngles() {
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

        FMatrix3 eulerAngles = new FMatrix3();

        float rollDegrees = (float) (roll * (180 / Math.PI));
        float pitchDegrees = (float) (pitch * (180 / Math.PI));
        float yawDegrees = (float) (yaw * (180 / Math.PI));

        // Assigning the angles to the matrix
        eulerAngles.set(0, 0, rollDegrees);   // Roll
        eulerAngles.set(1, 0, pitchDegrees);  // Pitch
        eulerAngles.set(2, 0, yawDegrees);    // Yaw

        return eulerAngles; // Return the matrix containing the Euler angles
    }

    public Quaternion GetQuaternion() {
        return new Quaternion((float) qw,(float)qx,(float)qy,(float)qz);
    }

}
