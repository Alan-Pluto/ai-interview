import { useEffect, useMemo, useState } from 'react';
import { motion } from 'framer-motion';
import { AlertCircle, CheckCircle, FileSearch, Loader2, Sparkles, Target } from 'lucide-react';
import { historyApi, type ResumeListItem } from '../api/history';
import { recruitmentApi } from '../api/recruitment';
import type { CandidateJobMatch, JobPost } from '../types/recruitment';

function parseJsonList(json?: string | null): string[] {
  if (!json) return [];
  try {
    const value = JSON.parse(json) as unknown;
    return Array.isArray(value) ? value.map(String).filter(Boolean) : [];
  } catch {
    return [];
  }
}

interface RequirementCheck {
  requirement?: string;
  satisfied?: boolean;
  evidence?: string;
  confidence?: string;
}

function parseRequirementChecks(json?: string | null): RequirementCheck[] {
  if (!json) return [];
  try {
    const value = JSON.parse(json) as unknown;
    if (!Array.isArray(value)) return [];
    return value.filter(item => item && typeof item === 'object') as RequirementCheck[];
  } catch {
    return [];
  }
}

function recommendationText(value: string) {
  if (value === 'RECOMMENDED') return '推荐进入下一轮';
  if (value === 'NOT_RECOMMENDED') return '暂不推荐';
  return '可考虑';
}

function recommendationClass(value: string) {
  if (value === 'RECOMMENDED') return 'bg-emerald-50 text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-300';
  if (value === 'NOT_RECOMMENDED') return 'bg-red-50 text-red-700 dark:bg-red-900/30 dark:text-red-300';
  return 'bg-amber-50 text-amber-700 dark:bg-amber-900/30 dark:text-amber-300';
}

export default function CandidateMatchPage() {
  const [jobs, setJobs] = useState<JobPost[]>([]);
  const [resumes, setResumes] = useState<ResumeListItem[]>([]);
  const [selectedJobId, setSelectedJobId] = useState<number | ''>('');
  const [selectedResumeId, setSelectedResumeId] = useState<number | ''>('');
  const [matches, setMatches] = useState<CandidateJobMatch[]>([]);
  const [activeMatch, setActiveMatch] = useState<CandidateJobMatch | null>(null);
  const [loading, setLoading] = useState(true);
  const [matching, setMatching] = useState(false);
  const [error, setError] = useState('');

  const strengths = useMemo(() => parseJsonList(activeMatch?.strengthsJson), [activeMatch]);
  const risks = useMemo(() => parseJsonList(activeMatch?.risksJson), [activeMatch]);
  const verificationQuestions = useMemo(
    () => parseJsonList(activeMatch?.verificationQuestionsJson),
    [activeMatch]
  );
  const requirementChecks = useMemo(
    () => parseRequirementChecks(activeMatch?.requirementChecksJson),
    [activeMatch]
  );

  useEffect(() => {
    const load = async () => {
      setLoading(true);
      setError('');
      try {
        const [jobList, resumeList] = await Promise.all([
          recruitmentApi.listJobs(),
          historyApi.getResumes(),
        ]);
        setJobs(jobList);
        setResumes(resumeList);
        if (jobList[0]) {
          setSelectedJobId(jobList[0].id);
          const jobMatches = await recruitmentApi.listMatches(jobList[0].id);
          setMatches(jobMatches);
          setActiveMatch(jobMatches[0] ?? null);
        }
        if (resumeList[0]) {
          setSelectedResumeId(resumeList[0].id);
        }
      } catch (err) {
        setError(err instanceof Error ? err.message : '加载匹配数据失败');
      } finally {
        setLoading(false);
      }
    };
    load();
  }, []);

  const handleJobChange = async (value: string) => {
    const jobId = Number(value);
    setSelectedJobId(jobId);
    setError('');
    try {
      const jobMatches = await recruitmentApi.listMatches(jobId);
      setMatches(jobMatches);
      setActiveMatch(jobMatches[0] ?? null);
    } catch (err) {
      setError(err instanceof Error ? err.message : '加载岗位匹配历史失败');
    }
  };

  const handleCreateMatch = async () => {
    if (!selectedJobId || !selectedResumeId) return;
    setMatching(true);
    setError('');
    try {
      const match = await recruitmentApi.createMatch(Number(selectedJobId), {
        resumeId: Number(selectedResumeId),
      });
      setActiveMatch(match);
      setMatches(prev => [match, ...prev]);
    } catch (err) {
      setError(err instanceof Error ? err.message : '生成匹配报告失败');
    } finally {
      setMatching(false);
    }
  };

  return (
    <motion.div className="mx-auto max-w-7xl" initial={{ opacity: 0 }} animate={{ opacity: 1 }}>
      <div className="mb-8 flex items-center justify-between gap-4">
        <div>
          <h1 className="flex items-center gap-3 text-2xl font-bold text-slate-800 dark:text-white">
            <FileSearch className="h-7 w-7 text-primary-500" />
            简历匹配
          </h1>
          <p className="mt-1 text-slate-500 dark:text-slate-400">选择一个 JD 和一份简历，生成单候选人岗位匹配报告</p>
        </div>
        <button
          onClick={handleCreateMatch}
          disabled={!selectedJobId || !selectedResumeId || matching}
          className="flex items-center gap-2 rounded-lg bg-primary-500 px-4 py-2 text-white transition-colors hover:bg-primary-600 disabled:cursor-not-allowed disabled:opacity-50"
        >
          {matching ? <Loader2 className="h-4 w-4 animate-spin" /> : <Sparkles className="h-4 w-4" />}
          生成匹配报告
        </button>
      </div>

      {error && (
        <div className="mb-4 flex items-center gap-2 rounded-xl border border-red-100 bg-red-50 px-4 py-3 text-sm text-red-600 dark:border-red-800 dark:bg-red-900/20 dark:text-red-300">
          <AlertCircle className="h-4 w-4" />
          {error}
        </div>
      )}

      <div className="mb-6 rounded-2xl border border-slate-100 bg-white p-5 shadow-sm dark:border-slate-700 dark:bg-slate-800">
        <div className="grid gap-4 md:grid-cols-2">
          <label className="block">
            <span className="mb-2 block text-sm font-semibold text-slate-700 dark:text-slate-200">岗位 JD</span>
            <select
              value={selectedJobId}
              onChange={event => handleJobChange(event.target.value)}
              className="w-full rounded-xl border border-slate-200 bg-white px-3 py-2.5 text-sm text-slate-700 outline-none focus:border-primary-500 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-100"
            >
              <option value="">请选择岗位</option>
              {jobs.map(job => (
                <option key={job.id} value={job.id}>{job.title}</option>
              ))}
            </select>
          </label>

          <label className="block">
            <span className="mb-2 block text-sm font-semibold text-slate-700 dark:text-slate-200">候选人简历</span>
            <select
              value={selectedResumeId}
              onChange={event => setSelectedResumeId(Number(event.target.value))}
              className="w-full rounded-xl border border-slate-200 bg-white px-3 py-2.5 text-sm text-slate-700 outline-none focus:border-primary-500 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-100"
            >
              <option value="">请选择简历</option>
              {resumes.map(resume => (
                <option key={resume.id} value={resume.id}>{resume.filename}</option>
              ))}
            </select>
          </label>
        </div>
        <p className="mt-3 text-xs text-slate-400">当前支持单候选人匹配；批量候选人排序会在后续版本接入。</p>
      </div>

      {loading ? (
        <div className="flex min-h-[40vh] items-center justify-center">
          <div className="h-10 w-10 animate-spin rounded-full border-3 border-slate-200 border-t-primary-500" />
        </div>
      ) : (
        <div className="grid grid-cols-[300px_minmax(0,1fr)] gap-6">
          <aside className="rounded-2xl border border-slate-100 bg-white p-4 shadow-sm dark:border-slate-700 dark:bg-slate-800">
            <div className="mb-3 text-xs font-semibold uppercase tracking-wide text-slate-400">匹配历史</div>
            <div className="space-y-2">
              {matches.length === 0 ? (
                <div className="rounded-xl bg-slate-50 p-4 text-sm text-slate-500 dark:bg-slate-900/40 dark:text-slate-400">
                  暂无匹配报告。
                </div>
              ) : matches.map(match => (
                <button
                  key={match.id}
                  onClick={() => setActiveMatch(match)}
                  className={`w-full rounded-xl border px-3 py-3 text-left transition-colors ${
                    activeMatch?.id === match.id
                      ? 'border-primary-200 bg-primary-50 dark:border-primary-800 dark:bg-primary-900/20'
                      : 'border-slate-100 bg-white hover:bg-slate-50 dark:border-slate-700 dark:bg-slate-800 dark:hover:bg-slate-700'
                  }`}
                >
                  <div className="flex items-center justify-between gap-2">
                    <span className="truncate text-sm font-semibold text-slate-700 dark:text-slate-200">{match.resumeFilename}</span>
                    <span className="text-lg font-bold text-primary-500">{match.matchScore}</span>
                  </div>
                  <div className="mt-2 text-xs text-slate-400">{recommendationText(match.recommendation)}</div>
                </button>
              ))}
            </div>
          </aside>

          <section className="min-w-0 rounded-2xl border border-slate-100 bg-white shadow-sm dark:border-slate-700 dark:bg-slate-800">
            {!activeMatch ? (
              <div className="p-12 text-center">
                <div className="mx-auto mb-5 flex h-14 w-14 items-center justify-center rounded-2xl bg-primary-50 text-primary-500 dark:bg-primary-900/30">
                  <Target className="h-7 w-7" />
                </div>
                <h2 className="text-lg font-semibold text-slate-800 dark:text-white">选择 JD 和简历开始匹配</h2>
                <p className="mt-2 text-sm text-slate-500 dark:text-slate-400">匹配报告会包含分数、推荐结论、亮点、风险和待验证问题。</p>
              </div>
            ) : (
              <div className="p-6">
                <div className="mb-6 flex flex-wrap items-center justify-between gap-4">
                  <div>
                    <p className="text-sm text-slate-400">候选人</p>
                    <h2 className="text-xl font-bold text-slate-800 dark:text-white">{activeMatch.resumeFilename}</h2>
                  </div>
                  <div className="flex items-center gap-3">
                    <div className={`rounded-full px-3 py-1.5 text-sm font-semibold ${recommendationClass(activeMatch.recommendation)}`}>
                      {recommendationText(activeMatch.recommendation)}
                    </div>
                    <div className="text-4xl font-bold text-primary-500">{activeMatch.matchScore}</div>
                  </div>
                </div>

                <div className="mb-6 rounded-xl bg-slate-50 p-4 text-sm leading-6 text-slate-600 dark:bg-slate-900/40 dark:text-slate-300">
                  {activeMatch.summary || '暂无匹配摘要'}
                </div>

                <div className="grid gap-5 lg:grid-cols-2">
                  <ReportBlock title="候选人亮点" items={strengths} empty="暂无亮点" positive />
                  <ReportBlock title="候选人风险" items={risks} empty="暂无明显风险" />
                </div>

                <div className="mt-6">
                  <h3 className="mb-3 text-sm font-semibold text-slate-700 dark:text-slate-200">必备条件核对</h3>
                  <div className="space-y-3">
                    {requirementChecks.length === 0 ? (
                      <div className="rounded-xl bg-slate-50 p-4 text-sm text-slate-500 dark:bg-slate-900/40 dark:text-slate-400">暂无核对项</div>
                    ) : requirementChecks.map((item, index) => (
                      <div key={index} className="rounded-xl border border-slate-100 p-4 dark:border-slate-700">
                        <div className="mb-2 flex items-center gap-2">
                          <CheckCircle className={`h-4 w-4 ${item.satisfied ? 'text-emerald-500' : 'text-amber-500'}`} />
                          <span className="font-medium text-slate-700 dark:text-slate-200">{item.requirement || '未命名条件'}</span>
                          {item.confidence && <span className="rounded-full bg-slate-100 px-2 py-0.5 text-xs text-slate-500 dark:bg-slate-700 dark:text-slate-300">{item.confidence}</span>}
                        </div>
                        <p className="text-sm leading-6 text-slate-500 dark:text-slate-400">{item.evidence || '暂无证据'}</p>
                      </div>
                    ))}
                  </div>
                </div>

                <div className="mt-6">
                  <ReportBlock title="面试待验证问题" items={verificationQuestions} empty="暂无待验证问题" />
                </div>
              </div>
            )}
          </section>
        </div>
      )}
    </motion.div>
  );
}

function ReportBlock({
  title,
  items,
  empty,
  positive = false,
}: {
  title: string;
  items: string[];
  empty: string;
  positive?: boolean;
}) {
  return (
    <div>
      <h3 className="mb-3 text-sm font-semibold text-slate-700 dark:text-slate-200">{title}</h3>
      <div className="space-y-2">
        {items.length === 0 ? (
          <div className="rounded-xl bg-slate-50 p-4 text-sm text-slate-500 dark:bg-slate-900/40 dark:text-slate-400">{empty}</div>
        ) : items.map((item, index) => (
          <div
            key={`${title}-${index}`}
            className={`rounded-xl px-4 py-3 text-sm leading-6 ${
              positive
                ? 'bg-emerald-50 text-emerald-700 dark:bg-emerald-900/20 dark:text-emerald-300'
                : 'bg-slate-50 text-slate-600 dark:bg-slate-900/40 dark:text-slate-300'
            }`}
          >
            {item}
          </div>
        ))}
      </div>
    </div>
  );
}
