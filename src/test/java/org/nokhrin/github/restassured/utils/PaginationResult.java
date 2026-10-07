package org.nokhrin.github.restassured.utils;

import java.util.List;

public record PaginationResult(List<Long> issueNumbers, String headerList) {
}
