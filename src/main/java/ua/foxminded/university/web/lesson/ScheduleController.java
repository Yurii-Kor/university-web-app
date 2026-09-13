package ua.foxminded.university.web.lesson;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import lombok.RequiredArgsConstructor;
import ua.foxminded.university.web.lesson.page.DailySchedulePageModelFactory;
import ua.foxminded.university.web.lesson.page.WeeklySchedulePageModelFactory;

@Controller
@RequestMapping("/schedule")
@PreAuthorize("hasAnyRole('STUDENT','TEACHER')")
@RequiredArgsConstructor
public class ScheduleController {

	private final WeeklySchedulePageModelFactory weeklyPageFactory;
	private final DailySchedulePageModelFactory dailyPageFactory;

	@GetMapping
	public String schedulePage(
			@AuthenticationPrincipal UserDetails principal,
			@RequestParam(name = "date", required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
			LocalDate date,
			Model model) {

		var page = weeklyPageFactory.build(principal, date);
		model.addAttribute("page", page);

		return "lessons/schedule";
	}

	@GetMapping("/day")
	public String dailySchedulePage(
			@AuthenticationPrincipal UserDetails principal,
			@RequestParam(name = "date", required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
			LocalDate date,
			Model model) {

		var page = dailyPageFactory.build(principal, date);
		model.addAttribute("page", page);

		return "lessons/day";
	}
}