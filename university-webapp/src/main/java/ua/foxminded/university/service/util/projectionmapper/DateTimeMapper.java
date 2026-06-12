package ua.foxminded.university.service.util.projectionmapper;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import org.springframework.stereotype.Component;

@Component
public class DateTimeMapper {

    public OffsetDateTime toUtcOffsetDateTime(Instant instant) {
        return Optional.ofNullable(instant)
                .map(value -> value.atOffset(ZoneOffset.UTC))
                .orElse(null);
    }
}
