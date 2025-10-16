package ua.foxminded.university.service.dto;

import java.util.List;

public record DeleteResult(List<Long> deletedIds, List<Long> notFoundIds) {
}
