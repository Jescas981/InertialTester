package dev.jescas.inertialtester.core.algorithms;


import org.ejml.data.FMatrix3;
import org.ejml.data.FMatrix4;
import org.ejml.data.ZMatrixD1;
import org.ejml.dense.fixed.NormOps_FDF3;

import dev.jescas.inertialtester.core.math.Quaternion;

public class FactoredQuaternionAlgorithm {
    private Quaternion quaternion = new Quaternion(1,0,0,0);

    // TODO: Input accelerometer, magnetometer data
    public void Feed(FMatrix3 acc, FMatrix3 mag) {
        // Normalize vectors
        NormOps_FDF3.normalizeF(acc);
        NormOps_FDF3.normalizeF(mag);
        // Elevation quaternion
        float sin_th = -acc.a1;
        float cos_th = (float) Math.sqrt((1.0f - Math.pow(sin_th, 2.0f)));

        // Compute quaternion of elevation
        float cos_th2 = (float) Math.sqrt((1.0f + cos_th)/2.0f);
        float sin_th2 = Sign(sin_th) * (float) Math.sqrt((1.0f - cos_th)/2.0f);

        // Elevation quaternion (rotation about x-axis)
        Quaternion qe = new Quaternion(cos_th2, 0, sin_th2, 0);
        // Compute quaternion of roll
        Quaternion qr = Quaternion.identity();

        if(cos_th != 0) {
            float sin_ph = acc.a1/cos_th;
            float cos_ph = acc.a3/cos_th;
            float cos2h_ph = (float) Math.sqrt((1.0f + cos_ph)/2.0f);
            float sin2h_ph = Sign(sin_ph) * (float) Math.sqrt((1.0f - cos_ph)/2.0f);
            qr =  new Quaternion(cos2h_ph, sin2h_ph, 0, 0);
        }

        // Azimuth quaternion (rotation about z-axis)
        Quaternion magQ = new Quaternion(0,mag.a1,mag.a2,mag.a3);
        Quaternion M = qe.mult(qr).mult(magQ).mult(qr.conjugate()).mult(qe.conjugate());
        double Mx = M.x()/ Math.sqrt(M.x()*M.x() + M.y()*M.y());
        double My = M.y()/ Math.sqrt(M.x()*M.x() + M.y()*M.y());
        float sin_ah = (float) Mx;
        float cos_ah = (float) -My;
        float cos2h_ah = (float) Math.sqrt((1.0f + cos_ah)/2.0f);
        float sin2h_ah = Sign(sin_ah) * (float) Math.sqrt((1.0f - cos_ah)/2.0f);
        Quaternion qa =  new Quaternion(cos2h_ah, 0, 0, sin2h_ah);
        quaternion = qa.mult(qe).mult(qr);
    }

    // TODO: Get Euler Angles
    public FMatrix3 GetEulerAngles(){
        float w = quaternion.w();
        float x = quaternion.x();
        float y = quaternion.y();
        float z = quaternion.z();

        // Compute Euler angles (yaw, pitch, roll)
        float roll = (float) Math.atan2(2 * (w*x+y*z), 1 - 2 * (Math.pow(x,2) + Math.pow(y,2)));
        float v = 2 * (w * y - x * z);
        float pitch = (float) (- Math.PI/2 + 2* Math.atan2(Math.sqrt(1 + v), Math.sqrt(1 - v)));
        float yaw = (float) Math.atan2(2 * (w*z+x*y), 1 - 2 * (Math.pow(y,2) + Math.pow(z,2)));
        // Convert to degrees
        float rollDegrees = (float) (roll * (180 / Math.PI));
        float pitchDegrees = (float) (pitch * (180 / Math.PI));
        float yawDegrees = (float) (yaw * (180 / Math.PI));

        // Return as FMatrix3
        return new FMatrix3(rollDegrees, pitchDegrees, yawDegrees);
    }

    // TODO: Get quaternions
    public Quaternion GetQuaternion() {
        return quaternion;
    }

    // UTILITY FUNCTION
    private float Sign(float value) {
        if (value > 0) {
            return 1.0f;
        } else if (value < 0) {
            return -1.0f;
        } else {
            return 0.0f;
        }
    }
}
