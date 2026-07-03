import { useEffect, useMemo, useState } from 'react';
import { motion } from 'framer-motion';
import { AlertCircle, CheckCircle, FileSearch, Loader2, Plus, Search, Sparkles, Target, Trash2, X } from 'lucide-react';
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

function formatDateTime(value?: string) {
  if (!value) return '';
  return new Date(value).toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  });
}

export default function CandidateMatchPage() {
  const [jobs, setJobs] = useState<JobPost[]>([]);
  const [resumes, setResumes] = useState<ResumeListItem[]>([]);
  const [selectedJobId, setSelectedJobId] = useState<number | ''>('');
  const [selectedResumeId, setSelectedResumeId] = useState<number | ''>('');
  const [matches, setMatches] = useState<CandidateJobMatch[]>([]);
  const [activeMatch, setActiveMatch] = useState<CandidateJobMatch | null>(null);
  const [createModalOpen, setCreateModalOpen] = useState(false);
  const [matchQuery, setMatchQuery] = useState('');
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
  const filteredMatches = useMemo(() => {
    const keyword = matchQuery.trim().toLowerCase();
    if (!keyword) return matches;
    return matches.filter(match => {
      const text = [
        match.jobTitle || '',
        match.resumeFilename,
        recommendationText(match.recommendation),
        String(match.matchScore),
        formatDateTime(match.createdAt),
      ].join(' ').toLowerCase();
      return text.includes(keyword);
    });
  }, [matchQuery, matches]);

  useEffect(() => {
    const load = async () => {
      setLoading(true);
      setError('');
      try {
        const [jobList, resumeList, matchList] = await Promise.all([
          recruitmentApi.listJobs(),
          historyApi.getResumes(),
          recruitmentApi.listAllMatches(),
        ]);
        setJobs(jobList);
        setResumes(resumeList);
        setMatches(matchList);
        setActiveMatch(matchList[0] ?? null);
        if (jobList[0]) {
          setSelectedJobId(jobList[0].id);
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
    if (!value) {
      setSelectedJobId('');
      return;
    }
    const jobId = Number(value);
    setSelectedJobId(jobId);
    setError('');
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
      setCreateModalOpen(false);
    } catch (err) {
      setError(err instanceof Error ? err.message : '生成匹配报告失败');
    } finally {
      setMatching(false);
    }
  };

  const handleDeleteMatch = async (match: CandidateJobMatch) => {
    if (!window.confirm(`确定删除「${match.resumeFilename}」的匹配报告吗？`)) return;
    setError('');
    try {
      await recruitmentApi.deleteMatch(match.id);
      const next = matches.filter(item => item.id !== match.id);
      setMatches(next);
      if (activeMatch?.id === match.id) {
        setActiveMatch(next[0] ?? null);
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : '删除匹配报告失败');
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
          onClick={() => setCreateModalOpen(true)}
          className="flex items-center gap-2 rounded-lg bg-primary-500 px-4 py-2 text-white transition-colors hover:bg-primary-600 disabled:cursor-not-allowed disabled:opacity-50"
        >
          <Plus className="h-4 w-4" />
          新增匹配
        </button>
      </div>

      {error && (
        <div className="mb-4 flex items-center gap-2 rounded-xl border border-red-100 bg-red-50 px-4 py-3 text-sm text-red-600 dark:border-red-800 dark:bg-red-900/20 dark:text-red-300">
          <AlertCircle className="h-4 w-4" />
          {error}
        </div>
      )}

      {loading ? (
        <div className="flex min-h-[40vh] items-center justify-center">
          <div className="h-10 w-10 animate-spin rounded-full border-3 border-slate-200 border-t-primary-500" />
        </div>
      ) : (
        <div className="grid grid-cols-[300px_minmax(0,1fr)] gap-6">
          <aside className="rounded-2xl border border-slate-100 bg-white p-4 shadow-sm dark:border-slate-700 dark:bg-slate-800">
            <div className="mb-3">
              <div className="mb-3 flex items-center justify-between gap-2">
                <div>
                  <div className="text-xs font-semibold uppercase tracking-wide text-slate-400">匹配历史</div>
                  <div className="mt-1 truncate text-xs text-slate-400">全部岗位 · 最近生成</div>
                </div>
                <span className="rounded-full bg-slate-100 px-2 py-0.5 text-xs text-slate-500 dark:bg-slate-700 dark:text-slate-300">{matches.length}</span>
              </div>
              <div className="flex items-center gap-2 rounded-xl border border-slate-200 bg-slate-50 px-3 py-2 dark:border-slate-700 dark:bg-slate-900/60">
                <Search className="h-4 w-4 text-slate-400" />
                <input
                  value={matchQuery}
                  onChange={event => setMatchQuery(event.target.value)}
                  placeholder="检索候选人、分数、结论"
                  className="min-w-0 flex-1 bg-transparent text-sm text-slate-700 outline-none placeholder:text-slate-400 dark:text-slate-100"
                />
              </div>
            </div>
            <div className="space-y-2">
              {filteredMatches.length === 0 ? (
                <div className="rounded-xl bg-slate-50 p-4 text-sm text-slate-500 dark:bg-slate-900/40 dark:text-slate-400">
                  暂无匹配报告。
                </div>
              ) : filteredMatches.map(match => (
                <div
                  key={match.id}
                  className={`group flex items-start gap-2 rounded-xl border px-3 py-3 transition-colors ${
                    activeMatch?.id === match.id
                      ? 'border-primary-200 bg-primary-50 dark:border-primary-800 dark:bg-primary-900/20'
                      : 'border-slate-100 bg-white hover:bg-slate-50 dark:border-slate-700 dark:bg-slate-800 dark:hover:bg-slate-700'
                  }`}
                >
                  <button onClick={() => setActiveMatch(match)} className="min-w-0 flex-1 text-left">
                    <div className="flex items-center justify-between gap-2">
                      <span className="truncate text-sm font-semibold text-slate-700 dark:text-slate-200">{match.resumeFilename}</span>
                      <span className="text-lg font-bold text-primary-500">{match.matchScore}</span>
                    </div>
                    <div className="mt-1 truncate text-xs text-slate-400">{match.jobTitle || '未命名岗位'}</div>
                    <div className="mt-1 text-xs text-slate-400">{recommendationText(match.recommendation)} · {formatDateTime(match.createdAt)}</div>
                  </button>
                  <button
                    onClick={() => handleDeleteMatch(match)}
                    className="rounded-lg p-1.5 text-slate-400 opacity-0 transition hover:bg-red-50 hover:text-red-500 group-hover:opacity-100 dark:hover:bg-red-900/20"
                    title="删除匹配报告"
                  >
                    <Trash2 className="h-4 w-4" />
                  </button>
                </div>
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
                    <p className="mt-1 text-sm text-slate-400">{activeMatch.jobTitle || '未命名岗位'}</p>
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

      {createModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/40 p-4">
          <motion.div
            initial={{ opacity: 0, y: 16, scale: 0.98 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            className="w-full max-w-2xl rounded-2xl border border-slate-100 bg-white shadow-2xl dark:border-slate-700 dark:bg-slate-800"
          >
            <div className="flex items-center justify-between border-b border-slate-100 px-5 py-4 dark:border-slate-700">
              <div>
                <h2 className="font-semibold text-slate-800 dark:text-white">新增匹配报告</h2>
                <p className="mt-1 text-xs text-slate-400">选择一个 JD 和一份简历，生成单候选人匹配报告。</p>
              </div>
              <button
                onClick={() => setCreateModalOpen(false)}
                className="rounded-lg p-2 text-slate-400 transition-colors hover:bg-slate-100 hover:text-slate-700 dark:hover:bg-slate-700 dark:hover:text-slate-100"
                title="关闭"
              >
                <X className="h-4 w-4" />
              </button>
            </div>
            <div className="space-y-4 p-5">
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
              <p className="text-xs text-slate-400">当前支持单候选人匹配；批量候选人排序会在后续版本接入。</p>
            </div>
            <div className="flex justify-end gap-2 border-t border-slate-100 px-5 py-4 dark:border-slate-700">
              <button
                onClick={() => setCreateModalOpen(false)}
                className="rounded-lg bg-slate-100 px-4 py-2 text-sm font-medium text-slate-700 transition-colors hover:bg-slate-200 dark:bg-slate-700 dark:text-slate-200 dark:hover:bg-slate-600"
              >
                取消
              </button>
              <button
                onClick={handleCreateMatch}
                disabled={!selectedJobId || !selectedResumeId || matching}
                className="flex items-center gap-2 rounded-lg bg-primary-500 px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-primary-600 disabled:cursor-not-allowed disabled:opacity-50"
              >
                {matching ? <Loader2 className="h-4 w-4 animate-spin" /> : <Sparkles className="h-4 w-4" />}
                生成匹配报告
              </button>
            </div>
          </motion.div>
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
