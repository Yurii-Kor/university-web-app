package ua.foxminded.university;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration(proxyBeanMethods = false)
public class TestClockConfig {

    public static final OffsetDateTime TEST_NOW =
            OffsetDateTime.of(
                    2030,
                    1,
                    10,
                    9,
                    0,
                    0,
                    0,
                    ZoneOffset.UTC);

    @Bean
    @Primary
    Clock testClock() {
        return Clock.fixed(
                TEST_NOW.toInstant(),
                TEST_NOW.getOffset());
    }
}