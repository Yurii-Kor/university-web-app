package ua.foxminded.university.service.lesson;

import static ua.foxminded.university.service.lesson.LessonServiceTestConstants.*;

import java.time.Clock;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
class LessonServiceTestConfig {

	@Bean
	@Primary
	Clock lessonServiceTestClock() {
		return Clock.fixed(
				TEST_NOW.toInstant(),
				TEST_NOW.getOffset());
	}
}