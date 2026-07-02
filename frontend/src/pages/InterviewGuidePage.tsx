import { useEffect, useMemo, useState } from 'react';
import { motion } from 'framer-motion';
import { AlertCircle, ClipboardList, Loader2, Save, Sparkles } from 'lucide-react';
import { recruitmentApi } from '../api/recruitment';
import type { CandidateJobMatch, InterviewGuide, JobPost } from '../types/recruitment';

interface OutlineSection {
  section?: string;
  suggestedMinutes?: number;
  questions?: Array<{
    question?: string;
    purpose?: string;
    evaluationFocus?: string;
    excellentSignal?: string;
    riskSignal?: string;
  }>;
}

interface ScoreDimension {
  dimension?: string;
  weight?: number;
  description?: string;
  scoringCriteria?: string[];
}

function parseJsonArray<T>(json?: string | null): T[] {
  if (!json) return [];
  try {
    const value = JSON.parse(json) as unknown;
    return Array.isArray(value) ? value as T[] : [];
  } catch {
    return [];
  }
}

const ROUND_OPTIONS = ['初筛', '技术一面', '业务二面', 'HR 面', '终面'];

export default function InterviewGuidePage() {
  const [jobs, setJobs] = useState<JobPost[]>([]);
  const [matches, setMatches] = useState<CandidateJobMatch[]>([]);
  const [guides, setGuides] = useState<InterviewGuide[]>([]);
  const [selectedJobId, setSelectedJobId] = useState<number | ''>('');
  const [selectedMatchId, setSelectedMatchId] = useState<number | ''>('');
  const [mode, setMode] = useState<'JOB_ONLY' | 'CANDIDATE_SPECIFIC'>('JOB_ONLY');
  const [interviewRound, setInterviewRound] = useState('技术一面');
  const [durationMinutes, setDurationMinutes] = useState(45);
  const [activeGuide, setActiveGuide] = useState<InterviewGuide | null>(null);
  const [outlineText, setOutlineText] = useState('');
  const [scorecardText, setScorecardText] = useState('');
  const [loading, setLoading] = useState(true);
  const [generating, setGenerating] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');

  const outline = useMemo(() => parseJsonArray<OutlineSection>(activeGuide?.outlineJson), [activeGuide]);
  const scorecard = useMemo(() => parseJsonArray<ScoreDimension>(activeGuide?.scorecardJson), [activeGuide]);

  const loadForJob = async (jobId: number) => {
    const [jobMatches, jobGuides] = await Promise.all([
      recruitmentApi.listMatches(jobId),
      recruitmentApi.listInterviewGuides(jobId),
    ]);
    setMatches(jobMatches);
    setGuides(jobGuides);
    setSelectedMatchId(jobMatches[0]?.id ?? '');
    setActiveGuide(jobGuides[0] ?? null);
    setOutlineText(jobGuides[0]?.outlineJson ?? '');
    setScorecardText(jobGuides[0]?.scorecardJson ?? '');
  };

  useEffect(() => {
    const load = async () => {
      setLoading(true);
      setError('');
      try {
        const jobList = await recruitmentApi.listJobs();
        setJobs(jobList);
        if (jobList[0]) {
          setSelectedJobId(jobList[0].id);
          await loadForJob(jobList[0].id);
        }
      } catch (err) {
        setError(err instanceof Error ? err.message : '加载题纲数据失败');
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
      await loadForJob(jobId);
    } catch (err) {
      setError(err instanceof Error ? err.message : '加载岗位题纲失败');
    }
  };

  const handleGenerate = async () => {
    if (!selectedJobId) return;
    if (mode === 'CANDIDATE_SPECIFIC' && !selectedMatchId) {
      setError('定制题纲需要先选择匹配报告');
      return;
    }
    setGenerating(true);
    setError('');
    try {
      const guide = await recruitmentApi.createInterviewGuide({
        jobPostId: Number(selectedJobId),
        matchId: mode === 'CANDIDATE_SPECIFIC' ? Number(selectedMatchId) : undefined,
        mode,
        interviewRound,
        durationMinutes,
      });
      setActiveGuide(guide);
      setGuides(prev => [guide, ...prev]);
      setOutlineText(guide.outlineJson ?? '');
      setScorecardText(guide.scorecardJson ?? '');
    } catch (err) {
      setError(err instanceof Error ? err.message : '生成面试题纲失败');
    } finally {
      setGenerating(false);
    }
  };

  const handleSave = async () => {
    if (!activeGuide) return;
    setSaving(true);
    setError('');
    try {
      const guide = await recruitmentApi.updateInterviewGuide(activeGuide.id, {
        outlineJson: outlineText,
        scorecardJson: scorecardText,
      });
      setActiveGuide(guide);
      setGuides(prev => prev.map(item => item.id === guide.id ? guide : item));
    } catch (err) {
      setError(err instanceof Error ? err.message : '保存题纲失败');
    } finally {
      setSaving(false);
    }
  };

  const selectGuide = (guide: InterviewGuide) => {
    setActiveGuide(guide);
    setOutlineText(guide.outlineJson ?? '');
    setScorecardText(guide.scorecardJson ?? '');
  };

  return (
    <motion.div className="mx-auto max-w-7xl" initial={{ opacity: 0 }} animate={{ opacity: 1 }}>
      <div className="mb-8 flex items-center justify-between gap-4">
        <div>
          <h1 className="flex items-center gap-3 text-2xl font-bold text-slate-800 dark:text-white">
            <ClipboardList className="h-7 w-7 text-primary-500" />
            面试题纲
          </h1>
          <p className="mt-1 text-slate-500 dark:text-slate-400">基于 JD 或候选人匹配报告生成结构化题纲和评分表</p>
        </div>
        <button
          onClick={handleGenerate}
          disabled={!selectedJobId || generating}
          className="flex items-center gap-2 rounded-lg bg-primary-500 px-4 py-2 text-white transition-colors hover:bg-primary-600 disabled:cursor-not-allowed disabled:opacity-50"
        >
          {generating ? <Loader2 className="h-4 w-4 animate-spin" /> : <Sparkles className="h-4 w-4" />}
          生成题纲
        </button>
      </div>

      {error && (
        <div className="mb-4 flex items-center gap-2 rounded-xl border border-red-100 bg-red-50 px-4 py-3 text-sm text-red-600 dark:border-red-800 dark:bg-red-900/20 dark:text-red-300">
          <AlertCircle className="h-4 w-4" />
          {error}
        </div>
      )}

      <div className="mb-6 rounded-2xl border border-slate-100 bg-white p-5 shadow-sm dark:border-slate-700 dark:bg-slate-800">
        <div className="grid gap-4 lg:grid-cols-4">
          <label>
            <span className="mb-2 block text-sm font-semibold text-slate-700 dark:text-slate-200">岗位 JD</span>
            <select value={selectedJobId} onChange={event => handleJobChange(event.target.value)} className="w-full rounded-xl border border-slate-200 bg-white px-3 py-2.5 text-sm dark:border-slate-700 dark:bg-slate-900 dark:text-slate-100">
              <option value="">请选择岗位</option>
              {jobs.map(job => <option key={job.id} value={job.id}>{job.title}</option>)}
            </select>
          </label>
          <label>
            <span className="mb-2 block text-sm font-semibold text-slate-700 dark:text-slate-200">生成模式</span>
            <select value={mode} onChange={event => setMode(event.target.value as 'JOB_ONLY' | 'CANDIDATE_SPECIFIC')} className="w-full rounded-xl border border-slate-200 bg-white px-3 py-2.5 text-sm dark:border-slate-700 dark:bg-slate-900 dark:text-slate-100">
              <option value="JOB_ONLY">通用题纲</option>
              <option value="CANDIDATE_SPECIFIC">候选人定制</option>
            </select>
          </label>
          <label>
            <span className="mb-2 block text-sm font-semibold text-slate-700 dark:text-slate-200">面试轮次</span>
            <select value={interviewRound} onChange={event => setInterviewRound(event.target.value)} className="w-full rounded-xl border border-slate-200 bg-white px-3 py-2.5 text-sm dark:border-slate-700 dark:bg-slate-900 dark:text-slate-100">
              {ROUND_OPTIONS.map(round => <option key={round} value={round}>{round}</option>)}
            </select>
          </label>
          <label>
            <span className="mb-2 block text-sm font-semibold text-slate-700 dark:text-slate-200">建议时长</span>
            <input type="number" min={15} max={180} value={durationMinutes} onChange={event => setDurationMinutes(Number(event.target.value))} className="w-full rounded-xl border border-slate-200 bg-white px-3 py-2.5 text-sm dark:border-slate-700 dark:bg-slate-900 dark:text-slate-100" />
          </label>
        </div>
        {mode === 'CANDIDATE_SPECIFIC' && (
          <label className="mt-4 block">
            <span className="mb-2 block text-sm font-semibold text-slate-700 dark:text-slate-200">匹配报告</span>
            <select value={selectedMatchId} onChange={event => setSelectedMatchId(Number(event.target.value))} className="w-full rounded-xl border border-slate-200 bg-white px-3 py-2.5 text-sm dark:border-slate-700 dark:bg-slate-900 dark:text-slate-100">
              <option value="">请选择匹配报告</option>
              {matches.map(match => <option key={match.id} value={match.id}>{match.resumeFilename} - {match.matchScore} 分</option>)}
            </select>
          </label>
        )}
      </div>

      {loading ? (
        <div className="flex min-h-[40vh] items-center justify-center">
          <div className="h-10 w-10 animate-spin rounded-full border-3 border-slate-200 border-t-primary-500" />
        </div>
      ) : (
        <div className="grid grid-cols-[280px_minmax(0,1fr)] gap-6">
          <aside className="rounded-2xl border border-slate-100 bg-white p-4 shadow-sm dark:border-slate-700 dark:bg-slate-800">
            <div className="mb-3 text-xs font-semibold uppercase tracking-wide text-slate-400">题纲历史</div>
            <div className="space-y-2">
              {guides.length === 0 ? (
                <div className="rounded-xl bg-slate-50 p-4 text-sm text-slate-500 dark:bg-slate-900/40 dark:text-slate-400">暂无题纲。</div>
              ) : guides.map(guide => (
                <button key={guide.id} onClick={() => selectGuide(guide)} className={`w-full rounded-xl border px-3 py-3 text-left transition-colors ${activeGuide?.id === guide.id ? 'border-primary-200 bg-primary-50 dark:border-primary-800 dark:bg-primary-900/20' : 'border-slate-100 bg-white hover:bg-slate-50 dark:border-slate-700 dark:bg-slate-800 dark:hover:bg-slate-700'}`}>
                  <div className="text-sm font-semibold text-slate-700 dark:text-slate-200">{guide.interviewRound}</div>
                  <div className="mt-1 text-xs text-slate-400">{guide.mode === 'JOB_ONLY' ? '通用题纲' : guide.resumeFilename || '候选人定制'}</div>
                </button>
              ))}
            </div>
          </aside>

          <section className="rounded-2xl border border-slate-100 bg-white shadow-sm dark:border-slate-700 dark:bg-slate-800">
            {!activeGuide ? (
              <div className="p-12 text-center text-sm text-slate-500 dark:text-slate-400">请选择或生成一份面试题纲。</div>
            ) : (
              <div className="grid gap-6 p-6 xl:grid-cols-2">
                <EditableJsonPanel title="题纲 JSON" value={outlineText} onChange={setOutlineText} />
                <EditableJsonPanel title="评分表 JSON" value={scorecardText} onChange={setScorecardText} />
                <div className="xl:col-span-2 flex justify-end">
                  <button onClick={handleSave} disabled={saving} className="flex items-center gap-2 rounded-lg bg-slate-100 px-4 py-2 text-sm font-medium text-slate-700 transition-colors hover:bg-slate-200 disabled:opacity-50 dark:bg-slate-700 dark:text-slate-200 dark:hover:bg-slate-600">
                    {saving ? <Loader2 className="h-4 w-4 animate-spin" /> : <Save className="h-4 w-4" />}
                    保存编辑
                  </button>
                </div>
                <GuidePreview outline={outline} scorecard={scorecard} />
              </div>
            )}
          </section>
        </div>
      )}
    </motion.div>
  );
}

function EditableJsonPanel({ title, value, onChange }: { title: string; value: string; onChange: (value: string) => void }) {
  return (
    <div>
      <h3 className="mb-3 text-sm font-semibold text-slate-700 dark:text-slate-200">{title}</h3>
      <textarea value={value} onChange={event => onChange(event.target.value)} rows={16} className="w-full resize-y rounded-xl border border-slate-200 bg-slate-50 p-4 font-mono text-xs leading-5 text-slate-700 outline-none focus:border-primary-500 dark:border-slate-700 dark:bg-slate-900/60 dark:text-slate-200" />
    </div>
  );
}

function GuidePreview({ outline, scorecard }: { outline: OutlineSection[]; scorecard: ScoreDimension[] }) {
  return (
    <div className="xl:col-span-2 grid gap-6 lg:grid-cols-2">
      <div>
        <h3 className="mb-3 text-sm font-semibold text-slate-700 dark:text-slate-200">题纲预览</h3>
        <div className="space-y-3">
          {outline.map((section, index) => (
            <div key={index} className="rounded-xl bg-slate-50 p-4 dark:bg-slate-900/40">
              <div className="font-semibold text-slate-800 dark:text-white">{section.section || '未命名环节'} · {section.suggestedMinutes || 0} 分钟</div>
              <div className="mt-3 space-y-3">
                {(section.questions || []).map((question, qIndex) => (
                  <div key={qIndex} className="rounded-lg bg-white p-3 text-sm dark:bg-slate-800">
                    <p className="font-medium text-slate-700 dark:text-slate-200">{question.question}</p>
                    <p className="mt-1 text-xs text-slate-500 dark:text-slate-400">{question.purpose}</p>
                  </div>
                ))}
              </div>
            </div>
          ))}
        </div>
      </div>
      <div>
        <h3 className="mb-3 text-sm font-semibold text-slate-700 dark:text-slate-200">评分表预览</h3>
        <div className="space-y-3">
          {scorecard.map((item, index) => (
            <div key={index} className="rounded-xl bg-slate-50 p-4 dark:bg-slate-900/40">
              <div className="flex items-center justify-between gap-3">
                <span className="font-semibold text-slate-800 dark:text-white">{item.dimension || '评分维度'}</span>
                <span className="text-sm font-bold text-primary-500">{item.weight || 0}%</span>
              </div>
              <p className="mt-2 text-sm text-slate-500 dark:text-slate-400">{item.description}</p>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
