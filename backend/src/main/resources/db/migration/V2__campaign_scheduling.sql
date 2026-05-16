ALTER TABLE campaigns
    ADD COLUMN workflow_id VARCHAR(255),
    ADD COLUMN job_payload TEXT,
    ADD COLUMN last_triggered_at TIMESTAMPTZ;

UPDATE campaigns
SET workflow_id = 'kyc-reminder',
    job_payload = '{"source":"campaign"}'
WHERE workflow_id IS NULL;

ALTER TABLE campaigns
    ALTER COLUMN workflow_id SET NOT NULL,
    ALTER COLUMN job_payload SET NOT NULL;

ALTER TABLE jobs
    ADD COLUMN campaign_id UUID REFERENCES campaigns (id);

CREATE INDEX idx_jobs_campaign_id ON jobs (campaign_id);

CREATE INDEX idx_campaigns_active_scheduled ON campaigns (status)
    WHERE schedule_expression IS NOT NULL;
