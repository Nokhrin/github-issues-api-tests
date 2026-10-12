package org.nokhrin.github.model;

import java.util.List;

public record PaginationResult(List<Long> issueNumbers, String headerList) {
}
