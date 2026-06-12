package ua.foxminded.university.service.util;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class DuplicateGuard {
	
	private static int ONE_ITEM = 1;

	private final Logger log = LoggerFactory.getLogger(getClass());

	public <T> void assertNoDuplicates(Collection<T> items, String what) {
		Set<T> dup = findDuplicates(items);
		
		if (!dup.isEmpty()) {
			log.warn("duplicate {} in request: {}", what, dup);
			throw new IllegalArgumentException("Duplicate " + what + " in request: " + dup);
		}
	}

	public <T> Set<T> findDuplicates(Collection<T> items) {
		return Optional.ofNullable(items)
				.orElseGet(Collections::emptyList)
				.stream()
				.filter(Objects::nonNull)
				.collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
				.entrySet()
				.stream()
				.filter(e -> e.getValue() > ONE_ITEM)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
	}
}
