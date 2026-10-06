package ua.foxminded.university.web.lesson.export.pdf;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;

import org.openpdf.text.DocumentException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.xhtmlrenderer.pdf.ITextRenderer;

import lombok.RequiredArgsConstructor;
import ua.foxminded.university.web.lesson.export.ScheduleExportData;

@Component
@RequiredArgsConstructor
public class SchedulePdfGenerator {

	private static final String TEMPLATE =
			"lessons/export/schedule-pdf";

	private static final String STATIC_RESOURCE_PATH =
			"static/";

	private static final DateTimeFormatter WEEK_DATE_FORMAT =
			DateTimeFormatter.ofPattern(
					"MMMM d, uuuu",
					Locale.ENGLISH);

	private static final DateTimeFormatter DAY_FORMAT =
			DateTimeFormatter.ofPattern(
					"EEEE, MMMM d, uuuu",
					Locale.ENGLISH);

	private static final DateTimeFormatter TIME_FORMAT =
			DateTimeFormatter.ofPattern(
					"HH:mm",
					Locale.ENGLISH);

	private final SpringTemplateEngine templateEngine;

	public byte[] generate(
			ScheduleExportData data) {

		Objects.requireNonNull(
				data,
				"data must not be null");

		var context =
				createContext(
						data);

		var html =
				templateEngine.process(
						TEMPLATE,
						context);

		var output =
				new ByteArrayOutputStream();

		try {
			var renderer =
					new ITextRenderer();

			var baseUrl =
					new ClassPathResource(
							STATIC_RESOURCE_PATH)
									.getURL()
									.toExternalForm();

			renderer.setDocumentFromString(
					html,
					baseUrl);

			renderer.layout();

			renderer.createPDF(
					output);

			return output.toByteArray();

		} catch (DocumentException | IOException e) {
			throw new IllegalStateException(
					"Failed to generate schedule PDF.",
					e);
		}
	}

	private Context createContext(
			ScheduleExportData data) {

		var context =
				new Context(
						Locale.ENGLISH);

		context.setVariable(
				"data",
				data);

		context.setVariable(
				"weekDateFormat",
				WEEK_DATE_FORMAT);

		context.setVariable(
				"dayFormat",
				DAY_FORMAT);

		context.setVariable(
				"timeFormat",
				TIME_FORMAT);

		context.setVariable(
				"hasLessons",
				data.schedule()
						.days()
						.stream()
						.anyMatch(day ->
								!day.lessons()
										.isEmpty()));

		return context;
	}
}