export type CampaignStatus = "DRAFT" | "ACTIVE" | "ARCHIVED";
export type JobStatus = "QUEUED" | "PROCESSING" | "FAILED" | "SUCCESS";

export type Campaign = {
  id: string;
  name: string;
  status: CampaignStatus;
  scheduleExpression?: string | null;
  workflowId: string;
  jobPayload: string;
  lastTriggeredAt?: string | null;
};

export type Job = {
  id: string;
  workflowId: string;
  status: JobStatus;
  retryCount: number;
  createdAt: string;
  updatedAt: string;
};

const apiBase = "/api/v1";

async function http<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(`${apiBase}${path}`, {
    headers: {
      "Content-Type": "application/json",
      ...(init?.headers ?? {}),
    },
    ...init,
  });
  if (!res.ok) {
    const text = await res.text();
    throw new Error(`${res.status} ${res.statusText}: ${text}`);
  }
  if (res.status === 204 || res.headers.get("content-length") === "0") {
    return undefined as T;
  }
  const text = await res.text();
  if (!text) {
    return undefined as T;
  }
  return JSON.parse(text) as T;
}

export async function listCampaigns(): Promise<Campaign[]> {
  return http("/campaigns");
}

export async function createCampaign(body: {
  name: string;
  scheduleExpression?: string | null;
  workflowId?: string | null;
  jobPayload?: string | null;
}): Promise<Campaign> {
  return http("/campaigns", { method: "POST", body: JSON.stringify(body) });
}

export async function updateCampaign(
  id: string,
  body: {
    name?: string;
    scheduleExpression?: string | null;
    workflowId?: string;
    jobPayload?: string;
    status?: CampaignStatus;
  },
): Promise<Campaign> {
  return http(`/campaigns/${id}`, { method: "PUT", body: JSON.stringify(body) });
}

export async function deleteCampaign(id: string): Promise<void> {
  return http(`/campaigns/${id}`, { method: "DELETE" });
}

export async function listJobs(): Promise<Job[]> {
  return http("/jobs");
}

export async function createJob(body: {
  workflowId: string;
  payload: string;
  idempotencyKey?: string | null;
}): Promise<Job> {
  return http("/jobs", { method: "POST", body: JSON.stringify(body) });
}

export async function replayJob(id: string): Promise<Job> {
  return http(`/jobs/${id}/replay`, { method: "POST" });
}
