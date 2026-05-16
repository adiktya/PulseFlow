CREATE TABLE campaigns (
    id UUID PRIMARY KEY,
    name VARCHAR(512) NOT NULL,
    schedule_expression VARCHAR(255),
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE jobs (
    id UUID PRIMARY KEY,
    workflow_id VARCHAR(255) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(32) NOT NULL,
    retry_count INTEGER NOT NULL DEFAULT 0,
    idempotency_key VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_jobs_workflow_id ON jobs (workflow_id);
CREATE INDEX idx_jobs_status ON jobs (status);
