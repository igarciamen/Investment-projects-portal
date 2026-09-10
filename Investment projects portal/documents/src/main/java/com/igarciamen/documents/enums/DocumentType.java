package com.igarciamen.documents.enums;

// Allowed attachment types for a project, per the roadmap: business plan,
// annual accounts, technical report. Kept as a closed enum (not free text)
// so the frontend can render a fixed dropdown and the backend can validate it.
public enum DocumentType {
    BUSINESS_PLAN,
    ANNUAL_ACCOUNTS,
    TECHNICAL_REPORT
}
