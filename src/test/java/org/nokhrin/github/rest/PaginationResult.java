package org.nokhrin.github.rest;

import java.util.List;

public record PaginationResult(List<Long> issueNumbers, String headerList) {
}
