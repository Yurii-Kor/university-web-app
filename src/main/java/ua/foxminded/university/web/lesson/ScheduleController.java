package ua.foxminded.university.web.lesson;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import lombok.RequiredArgsConstructor;
import ua.foxminded.university.web.lesson.export.ScheduleExportDataFactory;
import ua.foxminded.university.web.lesson.export.pdf.SchedulePdfGenerator;
import ua.foxminded.university.web.lesson.page.DailySchedulePageModelFactory;
import ua.foxminded.university.web.lesson.page.WeeklySchedulePageModelFactory;

@Controller
@RequestMapping("/schedule")
@PreAuthorize("hasAnyRole('STUDENT','TEACHER')")
@RequiredArgsConstructor
public class ScheduleController {

	private final WeeklySchedulePageModelFactory weeklyPageFactory;
	private final DailySchedulePageModelFactory dailyPageFactory;
	
	private final ScheduleExportDataFactory exportDataFactory;
	private final SchedulePdfGenerator pdfGenerator;
	
	private final Clock clock;

	@GetMapping
	public String schedulePage(
			@AuthenticationPrincipal UserDetails principal,
			@RequestParam(name = "date", required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
			LocalDate date,
			Model model) {

		var page =
				weeklyPageFactory.build(
						principal,
						date);

		model.addAttribute(
				"page",
				page);

		return "lessons/schedule";
	}

	@GetMapping("/day")
	public String dailySchedulePage(
			@AuthenticationPrincipal UserDetails principal,
			@RequestParam(name = "date", required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
			LocalDate date,
			Model model) {

		var page =
				dailyPageFactory.build(
						principal,
						date);

		model.addAttribute(
				"page",
				page);

		model.addAttribute(
				"now",
				OffsetDateTime.now(
						clock));

		return "lessons/day";
	}

	@GetMapping(
	        value = "/export",
	        produces = MediaType.APPLICATION_PDF_VALUE)
	public ResponseEntity<byte[]> exportSchedule(
	        @AuthenticationPrincipal UserDetails principal,
	        @RequestParam(name = "date", required = false)
	        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
	        LocalDate date) {

	    var exportData =
	            exportDataFactory.build(
	                    principal,
	                    date);

	    var pdf =
	            pdfGenerator.generate(
	                    exportData);

	    var schedule =
	            exportData.schedule();

	    var fileName =
	            "schedule-%s-%s.pdf"
	                    .formatted(
	                            schedule.weekStart(),
	                            schedule.weekEnd());

	    var contentDisposition =
	            ContentDisposition.attachment()
	                    .filename(fileName)
	                    .build();

	    return ResponseEntity.ok()
	            .contentType(
	                    MediaType.APPLICATION_PDF)
	            .header(
	                    HttpHeaders.CONTENT_DISPOSITION,
	                    contentDisposition.toString())
	            .contentLength(
	                    pdf.length)
	            .body(
	                    pdf);
	}
}