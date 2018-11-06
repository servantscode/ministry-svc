package org.servantscode.ministry.rest;

import org.servantscode.ministry.Ministry;

import java.util.List;

public class MinistryQueryResponse {
    private int start;
    private int count;
    private int totalResults;
    private List<Ministry> results;

    public MinistryQueryResponse() {}

    public MinistryQueryResponse(int start, int count, int totalResults, List<Ministry> results) {
        this.start = start;
        this.count = count;
        this.totalResults = totalResults;
        this.results = results;
    }

    // ----- Accesssors -----
    public int getStart() { return start; }
    public void setStart(int start) { this.start = start; }

    public int getCount() { return count; }
    public void setCount(int count) { this.count = count; }

    public int getTotalResults() { return totalResults; }
    public void setTotalResults(int totalResults) { this.totalResults = totalResults; }

    public List<Ministry> getResults() { return results; }
    public void setResults(List<Ministry> results) { this.results = results; }
}
