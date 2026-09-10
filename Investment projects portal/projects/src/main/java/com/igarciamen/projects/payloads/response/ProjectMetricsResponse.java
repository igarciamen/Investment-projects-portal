package com.igarciamen.projects.payloads.response;

// Same role as tasks/payloads/response/MetricsResponse.java in SecGest: a
// flat count-per-status summary for the admin dashboard's headline numbers.
public class ProjectMetricsResponse {

    private long draft;
    private long submitted;
    private long underReview;
    private long approved;
    private long rejected;
    private long total;

    public ProjectMetricsResponse() {}

    public ProjectMetricsResponse(long draft, long submitted, long underReview, long approved, long rejected) {
        this.draft = draft;
        this.submitted = submitted;
        this.underReview = underReview;
        this.approved = approved;
        this.rejected = rejected;
        this.total = draft + submitted + underReview + approved + rejected;
    }

    public long getDraft() { return draft; }
    public long getSubmitted() { return submitted; }
    public long getUnderReview() { return underReview; }
    public long getApproved() { return approved; }
    public long getRejected() { return rejected; }
    public long getTotal() { return total; }
}
