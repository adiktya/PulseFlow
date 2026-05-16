import { useCallback, useEffect, useMemo, useState } from "react";
import type { Campaign, Job } from "./api";
import * as api from "./api";

function Pill({ children, tone }: { children: string; tone: "good" | "warn" | "bad" | "neutral" }) {
  const cls = useMemo(() => {
    if (tone === "good") return "bg-emerald-500/15 text-emerald-200 ring-1 ring-emerald-500/30";
    if (tone === "warn") return "bg-amber-500/15 text-amber-200 ring-1 ring-amber-500/30";
    if (tone === "bad") return "bg-rose-500/15 text-rose-200 ring-1 ring-rose-500/30";
    return "bg-slate-500/15 text-slate-200 ring-1 ring-slate-500/30";
  }, [tone]);
  return <span className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium ${cls}`}>{children}</span>;
}

function jobTone(status: api.JobStatus): "good" | "warn" | "bad" | "neutral" {
  if (status === "SUCCESS") return "good";
  if (status === "FAILED") return "bad";
  if (status === "PROCESSING") return "warn";
  return "neutral";
}

export default function App() {
  const [campaigns, setCampaigns] = useState<Campaign[]>([]);
  const [jobs, setJobs] = useState<Job[]>([]);
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const [cName, setCName] = useState("");
  const [cSchedule, setCSchedule] = useState("");
  const [cWorkflow, setCWorkflow] = useState("kyc-reminder");
  const [cPayload, setCPayload] = useState('{"source":"campaign"}');
  const [editingCampaignId, setEditingCampaignId] = useState<string | null>(null);

  const [jWorkflow, setJWorkflow] = useState("kyc-reminder");
  const [jPayload, setJPayload] = useState('{"userId":"u1"}');
  const [jIdem, setJIdem] = useState("");

  const refresh = useCallback(async () => {
    setError(null);
    try {
      const [c, j] = await Promise.all([api.listCampaigns(), api.listJobs()]);
      setCampaigns(c);
      setJobs(j);
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    }
  }, []);

  useEffect(() => {
    void refresh();
    const t = window.setInterval(() => void refresh(), 5000);
    return () => window.clearInterval(t);
  }, [refresh]);

  function resetCampaignForm() {
    setEditingCampaignId(null);
    setCName("");
    setCSchedule("");
    setCWorkflow("kyc-reminder");
    setCPayload('{"source":"campaign"}');
  }

  function startEditCampaign(c: Campaign) {
    setEditingCampaignId(c.id);
    setCName(c.name);
    setCSchedule(c.scheduleExpression ?? "");
    setCWorkflow(c.workflowId);
    setCPayload(c.jobPayload);
    setMessage(null);
    setError(null);
  }

  async function onSubmitCampaign(e: React.FormEvent) {
    e.preventDefault();
    setMessage(null);
    setError(null);
    try {
      JSON.parse(cPayload);
      const body = {
        name: cName.trim(),
        scheduleExpression: cSchedule.trim() ? cSchedule.trim() : null,
        workflowId: cWorkflow.trim() || null,
        jobPayload: cPayload,
      };
      if (editingCampaignId) {
        await api.updateCampaign(editingCampaignId, body);
        setMessage("Campaign updated");
      } else {
        await api.createCampaign(body);
        setMessage("Campaign created");
      }
      resetCampaignForm();
      await refresh();
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err));
    }
  }

  async function onDeleteCampaign(c: Campaign) {
    if (!window.confirm(`Archive campaign "${c.name}"? Scheduled runs will stop.`)) {
      return;
    }
    setMessage(null);
    setError(null);
    try {
      await api.deleteCampaign(c.id);
      if (editingCampaignId === c.id) {
        resetCampaignForm();
      }
      setMessage("Campaign archived");
      await refresh();
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err));
    }
  }

  async function onCreateJob(e: React.FormEvent) {
    e.preventDefault();
    setMessage(null);
    setError(null);
    try {
      JSON.parse(jPayload);
      await api.createJob({
        workflowId: jWorkflow.trim(),
        payload: jPayload,
        idempotencyKey: jIdem.trim() ? jIdem.trim() : null,
      });
      setMessage("Job enqueued");
      await refresh();
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err));
    }
  }

  async function onReplay(id: string) {
    setMessage(null);
    setError(null);
    try {
      await api.replayJob(id);
      setMessage("Replay requested");
      await refresh();
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err));
    }
  }

  return (
    <div className="min-h-screen">
      <header className="border-b border-slate-800 bg-slate-950/60 backdrop-blur">
        <div className="mx-auto flex max-w-6xl items-center justify-between px-6 py-4">
          <div>
            <div className="text-sm text-slate-400">PulseFlow</div>
            <div className="text-lg font-semibold tracking-tight">Operations dashboard</div>
          </div>
          <div className="flex items-center gap-3 text-sm">
            <a className="text-slate-300 underline decoration-slate-600 underline-offset-4 hover:text-white" href="/api/v1/campaigns" target="_blank" rel="noreferrer">
              API (same origin)
            </a>
            <button
              type="button"
              className="rounded-md bg-slate-800 px-3 py-1.5 text-slate-100 ring-1 ring-slate-700 hover:bg-slate-800"
              onClick={() => void refresh()}
            >
              Refresh
            </button>
          </div>
        </div>
      </header>

      <main className="mx-auto max-w-6xl space-y-10 px-6 py-10">
        {(message || error) && (
          <div className="rounded-lg border border-slate-800 bg-slate-900/40 p-4">
            {message && <div className="text-emerald-200">{message}</div>}
            {error && <div className="text-rose-200">{error}</div>}
          </div>
        )}

        <section className="grid gap-6 lg:grid-cols-2">
          <div className="rounded-xl border border-slate-800 bg-slate-900/30 p-6">
            <div className="mb-4 flex items-end justify-between gap-4">
              <div>
                <h2 className="text-base font-semibold">Campaigns</h2>
                <p className="mt-1 text-sm text-slate-400">
                  With a cron schedule, a background worker publishes `campaign.triggered` and enqueues jobs automatically.
                </p>
              </div>
            </div>

            <form onSubmit={onSubmitCampaign} className="space-y-3">
              <div>
                <label className="text-xs text-slate-400">Name</label>
                <input
                  className="mt-1 w-full rounded-md border border-slate-800 bg-slate-950 px-3 py-2 text-sm outline-none ring-0 focus:border-slate-600"
                  value={cName}
                  onChange={(e) => setCName(e.target.value)}
                  placeholder="KYC reminders May"
                  required
                />
              </div>
              <div>
                <label className="text-xs text-slate-400">Workflow id</label>
                <input
                  className="mt-1 w-full rounded-md border border-slate-800 bg-slate-950 px-3 py-2 text-sm outline-none focus:border-slate-600"
                  value={cWorkflow}
                  onChange={(e) => setCWorkflow(e.target.value)}
                  placeholder="kyc-reminder"
                  required
                />
              </div>
              <div>
                <label className="text-xs text-slate-400">Job payload (JSON)</label>
                <textarea
                  className="mt-1 min-h-[72px] w-full rounded-md border border-slate-800 bg-slate-950 px-3 py-2 font-mono text-xs outline-none focus:border-slate-600"
                  value={cPayload}
                  onChange={(e) => setCPayload(e.target.value)}
                  required
                />
              </div>
              <div>
                <label className="text-xs text-slate-400">Cron schedule (optional, 6-field)</label>
                <input
                  className="mt-1 w-full rounded-md border border-slate-800 bg-slate-950 px-3 py-2 text-sm outline-none focus:border-slate-600"
                  value={cSchedule}
                  onChange={(e) => setCSchedule(e.target.value)}
                  placeholder="0 * * * * *  (every minute, for testing)"
                />
              </div>
              <div className="flex gap-2">
                <button
                  className="flex-1 rounded-md bg-indigo-600 px-3 py-2 text-sm font-medium text-white hover:bg-indigo-500"
                  type="submit"
                >
                  {editingCampaignId ? "Save changes" : "Create campaign"}
                </button>
                {editingCampaignId && (
                  <button
                    type="button"
                    className="rounded-md bg-slate-800 px-3 py-2 text-sm text-slate-200 ring-1 ring-slate-700 hover:bg-slate-700"
                    onClick={resetCampaignForm}
                  >
                    Cancel
                  </button>
                )}
              </div>
            </form>

            <div className="mt-6 overflow-hidden rounded-lg border border-slate-800">
              <table className="w-full text-left text-sm">
                <thead className="bg-slate-950 text-xs text-slate-400">
                  <tr>
                    <th className="px-3 py-2">Name</th>
                    <th className="px-3 py-2">Schedule</th>
                    <th className="px-3 py-2">Last fired</th>
                    <th className="px-3 py-2">Status</th>
                    <th className="px-3 py-2 text-right">Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {campaigns.map((c) => (
                    <tr key={c.id} className="border-t border-slate-800">
                      <td className="px-3 py-2 font-mono text-xs text-slate-300">{c.name}</td>
                      <td className="px-3 py-2 font-mono text-xs text-slate-400">
                        {c.scheduleExpression ?? "—"}
                      </td>
                      <td className="px-3 py-2 text-xs text-slate-400">
                        {c.lastTriggeredAt ? new Date(c.lastTriggeredAt).toLocaleString() : "—"}
                      </td>
                      <td className="px-3 py-2">
                        <Pill
                          tone={
                            c.status === "ACTIVE" ? "good" : c.status === "ARCHIVED" ? "bad" : "neutral"
                          }
                        >
                          {c.status}
                        </Pill>
                      </td>
                      <td className="px-3 py-2 text-right">
                        {c.status !== "ARCHIVED" ? (
                          <div className="flex justify-end gap-1">
                            <button
                              type="button"
                              className="rounded-md bg-slate-800 px-2 py-1 text-xs text-slate-100 ring-1 ring-slate-700 hover:bg-slate-700"
                              onClick={() => startEditCampaign(c)}
                            >
                              Edit
                            </button>
                            <button
                              type="button"
                              className="rounded-md bg-rose-950 px-2 py-1 text-xs text-rose-200 ring-1 ring-rose-800 hover:bg-rose-900"
                              onClick={() => void onDeleteCampaign(c)}
                            >
                              Delete
                            </button>
                          </div>
                        ) : (
                          <span className="text-xs text-slate-600">—</span>
                        )}
                      </td>
                    </tr>
                  ))}
                  {campaigns.length === 0 && (
                    <tr>
                      <td className="px-3 py-6 text-sm text-slate-500" colSpan={5}>
                        No campaigns yet.
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>

          <div className="rounded-xl border border-slate-800 bg-slate-900/30 p-6">
            <div className="mb-4">
              <h2 className="text-base font-semibold">Jobs</h2>
              <p className="mt-1 text-sm text-slate-400">
                Creates persist rows and publish `notification.send`. Successful jobs also push to{" "}
                <a className="text-indigo-300 underline" href="https://ntfy.sh" target="_blank" rel="noreferrer">
                  ntfy.sh
                </a>{" "}
                (free, no API key).
              </p>
            </div>

            <form onSubmit={onCreateJob} className="space-y-3">
              <div>
                <label className="text-xs text-slate-400">Workflow id</label>
                <input
                  className="mt-1 w-full rounded-md border border-slate-800 bg-slate-950 px-3 py-2 text-sm outline-none focus:border-slate-600"
                  value={jWorkflow}
                  onChange={(e) => setJWorkflow(e.target.value)}
                  required
                />
              </div>
              <div>
                <label className="text-xs text-slate-400">Payload (JSON string)</label>
                <textarea
                  className="mt-1 min-h-[110px] w-full rounded-md border border-slate-800 bg-slate-950 px-3 py-2 font-mono text-xs outline-none focus:border-slate-600"
                  value={jPayload}
                  onChange={(e) => setJPayload(e.target.value)}
                  required
                />
              </div>
              <div>
                <label className="text-xs text-slate-400">Idempotency key (optional)</label>
                <input
                  className="mt-1 w-full rounded-md border border-slate-800 bg-slate-950 px-3 py-2 text-sm outline-none focus:border-slate-600"
                  value={jIdem}
                  onChange={(e) => setJIdem(e.target.value)}
                  placeholder="client-supplied key"
                />
              </div>
              <button className="w-full rounded-md bg-indigo-600 px-3 py-2 text-sm font-medium text-white hover:bg-indigo-500" type="submit">
                Enqueue job
              </button>
            </form>
          </div>
        </section>

        <section className="rounded-xl border border-slate-800 bg-slate-900/30 p-6">
          <div className="mb-4 flex items-end justify-between gap-4">
            <div>
              <h2 className="text-base font-semibold">Recent jobs</h2>
              <p className="mt-1 text-sm text-slate-400">Auto-refreshes every 5 seconds.</p>
            </div>
          </div>

          <div className="overflow-hidden rounded-lg border border-slate-800">
            <table className="w-full text-left text-sm">
              <thead className="bg-slate-950 text-xs text-slate-400">
                <tr>
                  <th className="px-3 py-2">Id</th>
                  <th className="px-3 py-2">Workflow</th>
                  <th className="px-3 py-2">Status</th>
                  <th className="px-3 py-2">Retries</th>
                  <th className="px-3 py-2"></th>
                </tr>
              </thead>
              <tbody>
                {jobs.map((j) => (
                  <tr key={j.id} className="border-t border-slate-800">
                    <td className="px-3 py-2 font-mono text-xs text-slate-300">{j.id}</td>
                    <td className="px-3 py-2">{j.workflowId}</td>
                    <td className="px-3 py-2">
                      <Pill tone={jobTone(j.status)}>{j.status}</Pill>
                    </td>
                    <td className="px-3 py-2 text-slate-300">{j.retryCount}</td>
                    <td className="px-3 py-2 text-right">
                      {j.status === "FAILED" ? (
                        <button
                          type="button"
                          className="rounded-md bg-slate-800 px-2 py-1 text-xs text-slate-100 ring-1 ring-slate-700 hover:bg-slate-800"
                          onClick={() => void onReplay(j.id)}
                        >
                          Replay
                        </button>
                      ) : (
                        <span className="text-xs text-slate-600"> </span>
                      )}
                    </td>
                  </tr>
                ))}
                {jobs.length === 0 && (
                  <tr>
                    <td className="px-3 py-6 text-sm text-slate-500" colSpan={5}>
                      No jobs yet.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>

          <div className="mt-4 space-y-2 text-xs text-slate-500">
            <p>
              Tip: use payload <span className="font-mono text-slate-300">{`{"simulate":"fail-once"}`}</span> to exercise retries, or{" "}
              <span className="font-mono text-slate-300">{`{"simulate":"fail-always"}`}</span> for DLQ.
            </p>
            <p>
              Push notifications: subscribe to topic{" "}
              <span className="font-mono text-slate-300">pulseflow-dev-local</span> in the{" "}
              <a className="text-indigo-300 underline" href="https://ntfy.sh/app" target="_blank" rel="noreferrer">
                ntfy app
              </a>
              . Override with <span className="font-mono text-slate-300">{`{"ntfyTopic":"your-secret-topic"}`}</span> in the payload.
            </p>
          </div>
        </section>
      </main>
    </div>
  );
}


