package think.rpgitems.utils.cast;

public class RoundedConeInfo {
    private double phi;
    private double theta;

    private double r;
    private double rPhi;
    private double rTheta;

    private double initalRotation;

    public RoundedConeInfo(
            double theta, double phi, double r, double rPhi, double rTheta, double initalRotation) {
        this.theta = theta;
        this.phi = phi;
        this.r = r;
        this.rPhi = rPhi;
        this.rTheta = rTheta;
        this.initalRotation = initalRotation;
    }

    public double getPhi() {
        return phi;
    }

    public double getTheta() {
        return theta;
    }

    public double getR() {
        return r;
    }

    public double getRPhi() {
        return rPhi;
    }

    public double getRTheta() {
        return rTheta;
    }

    public double getInitalRotation() {
        return initalRotation;
    }

    public void setPhi(double phi) {
        this.phi = phi;
    }

    public void setTheta(double theta) {
        this.theta = theta;
    }

    public void setR(double r) {
        this.r = r;
    }

    public void setRPhi(double rPhi) {
        this.rPhi = rPhi;
    }

    public void setrTheta(double rTheta) {
        this.rTheta = rTheta;
    }

    public void setInitalRotation(double initalRotation) {
        this.initalRotation = initalRotation;
    }
}
