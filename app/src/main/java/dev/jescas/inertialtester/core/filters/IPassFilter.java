package dev.jescas.inertialtester.core.filters;

public interface IPassFilter {
    void Update(double raw);
    double GetOutput();
}
