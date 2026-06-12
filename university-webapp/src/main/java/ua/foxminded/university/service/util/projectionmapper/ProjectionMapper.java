package ua.foxminded.university.service.util.projectionmapper;

public interface ProjectionMapper<P, R> {
    public R toView(P projection);
}
