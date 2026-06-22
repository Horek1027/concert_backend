package com.concer.backend.kafka.DTO;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class OrderAggregate {

    private List<ReserveResult> results = new ArrayList<>();
    private int expectedCount;
    private int currentCount;

    public void add(ReserveResult result) {
        results.add(result);
        expectedCount = result.getTotalSegments();
        currentCount++;
    }

    public boolean isComplete() {
        return currentCount >= expectedCount;
    }

    public boolean allSuccess() {
        return results.stream().allMatch(ReserveResult::isSuccess);
    }
}
