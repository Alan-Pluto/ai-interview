import { useEffect, useMemo, useState } from 'react';
import { motion } from 'framer-motion';
import {
  AlertCircle,
  Check,
  Clipboard,
  Loader2,
  MessageSquare,
  Plus,
  Save,
  Search,
  Send,
  Sparkles,
  Trash2,
  X,
} from 'lucide-react';
import { historyApi, type ResumeListItem } from '../api/history';
import { recruitmentApi } from '../api/recruitment';
import type {
  CandidateJobMatch,
  CandidateMessageConversationMessage,
  CandidateMessageDraft,
  InterviewGuide,
  JobPost,
} from '../types/recruitment';

const MESSAGE_TYPES = [
  { value: 'INTERVIEW_INVITE', label: '约面邀请' },
  { value: 'RESCHEDULE', label: '改期沟通' },
  { value: 'REMINDER', label: '面试提醒' },
  { value: 'REJECTION', label: '拒信' },
  { value: 'OFFER_COMMUNICATION', label: 'Offer 沟通' },
  { value: 'FOLLOW_UP', label: '跟进沟通' },
];

const CHANNELS = [
  { value: 'EMAIL', label: '邮件' },
  { value: 'SMS', label: '短信' },
  { value: 'WECHAT', label: '微信' },
  { value: 'PHONE_SCRIPT', label: '电话话术' },
];

const STAGES = ['简历筛选后', '约面前', '面试前', '面试后', 'Offer 前', '入职前'];
const TONES = ['专业友好', '简洁直接', '温和真诚', '正式稳妥'];

function parseJsonArray(json?: string | null): string[] {
  if (!json) return [];
  try {
    const value = JSON.parse(json) as unknown;
    return Array.isArray(value) ? value.map(item => String(item)) : [];
  } catch {
    return [];
  }
}

function formatDate(value?: string) {
  if (!value) return '';
  return new Date(value).toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' });
}

export default function CandidateMessagePage() {
  const [jobs, setJobs] = useState<JobPost[]>([]);
  const [resumes, setResumes] = useState<ResumeListItem[]>([]);
  const [matches, setMatches] = useState<CandidateJobMatch[]>([]);
  const [guides, setGuides] = useState<InterviewGuide[]>([]);
  const [drafts, setDrafts] = useState<CandidateMessageDraft[]>([]);
  const [messages, setMessages] = useState<CandidateMessageConversationMessage[]>([]);
  const [selectedJobId, setSelectedJobId] = useState<number | ''>('');
  const [selectedResumeId, setSelectedResumeId] = useState<number | ''>('');
  const [selectedMatchId, setSelectedMatchId] = useState<number | ''>('');
  const [selectedGuideId, setSelectedGuideId] = useState<number | ''>('');
  const [messageType, setMessageType] = useState('INTERVIEW_INVITE');
  const [channel, setChannel] = useState('EMAIL');
  const [stage, setStage] = useState(STAGES[1]);
  const [tone, setTone] = useState(TONES[0]);
  const [extraInstruction, setExtraInstruction] = useState('');
  const [activeDraft, setActiveDraft] = useState<CandidateMessageDraft | null>(null);
  const [subject, setSubject] = useState('');
  const [content, setContent] = useState('');
  const [optimizeInstruction, setOptimizeInstruction] = useState('');
  const [createModalOpen, setCreateModalOpen] = useState(false);
  const [draftQuery, setDraftQuery] = useState('');
  const [loading, setLoading] = useState(true);
  const [generating, setGenerating] = useState(false);
  const [saving, setSaving] = useState(false);
  const [optimizing, setOptimizing] = useState(false);
  const [copied, setCopied] = useState(false);
  const [error, setError] = useState('');

  const keyPoints = useMemo(() => parseJsonArray(activeDraft?.keyPointsJson), [activeDraft]);
  const cautionNotes = useMemo(() => parseJsonArray(activeDraft?.cautionNotesJson), [activeDraft]);
  const filteredDrafts = useMemo(() => {
    const keyword = draftQuery.trim().toLowerCase();
    if (!keyword) return drafts;
    return drafts.filter(draft => {
      const typeLabel = MESSAGE_TYPES.find(item => item.value === draft.messageType)?.label || draft.messageType;
      const channelLabel = CHANNELS.find(item => item.value === draft.channel)?.label || draft.channel;
      const text = [
        draft.jobTitle || '',
        typeLabel,
        channelLabel,
        draft.stage,
        draft.tone,
        draft.resumeFilename || '',
        draft.subject || '',
        formatDate(draft.updatedAt),
      ].join(' ').toLowerCase();
      return text.includes(keyword);
    });
  }, [draftQuery, drafts]);

  const loadJobDependencies = async (jobId: number) => {
    const [jobMatches, jobGuides] = await Promise.all([
      recruitmentApi.listMatches(jobId),
      recruitmentApi.listInterviewGuides(jobId),
    ]);
    setMatches(jobMatches);
    setGuides(jobGuides);
    setSelectedMatchId(jobMatches[0]?.id ?? '');
    setSelectedGuideId(jobGuides[0]?.id ?? '');
    setSelectedResumeId(jobMatches[0]?.resumeId ?? '');
  };

  useEffect(() => {
    const load = async () => {
      setLoading(true);
      setError('');
      try {
        const [jobList, resumeList, draftList] = await Promise.all([
          recruitmentApi.listJobs(),
          historyApi.getResumes(),
          recruitmentApi.listAllCandidateMessages(),
        ]);
        setJobs(jobList);
        setResumes(resumeList);
        setDrafts(draftList);
        await selectDraft(draftList[0] ?? null);
        if (jobList[0]) {
          setSelectedJobId(jobList[0].id);
          await loadJobDependencies(jobList[0].id);
        }
      } catch (err) {
        setError(err instanceof Error ? err.message : '加载沟通文案数据失败');
      } finally {
        setLoading(false);
      }
    };
    load();
  }, []);

  const selectDraft = async (draft: CandidateMessageDraft | null) => {
    setActiveDraft(draft);
    setSubject(draft?.subject ?? '');
    setContent(draft?.content ?? '');
    setMessages([]);
    if (!draft) return;
    try {
      const history = await recruitmentApi.listCandidateMessageConversation(draft.id);
      setMessages(history);
    } catch (err) {
      setError(err instanceof Error ? err.message : '加载文案对话失败');
    }
  };

  const handleJobChange = async (value: string) => {
    if (!value) {
      setSelectedJobId('');
      setMatches([]);
      setGuides([]);
      setSelectedMatchId('');
      setSelectedGuideId('');
      return;
    }
    const jobId = Number(value);
    setSelectedJobId(jobId);
    setError('');
    try {
      await loadJobDependencies(jobId);
    } catch (err) {
      setError(err instanceof Error ? err.message : '加载岗位关联信息失败');
    }
  };

  const handleMatchChange = (value: string) => {
    const matchId = value ? Number(value) : '';
    setSelectedMatchId(matchId);
    const match = matches.find(item => item.id === matchId);
    if (match) {
      setSelectedResumeId(match.resumeId);
    }
  };

  const handleGenerate = async () => {
    if (!selectedJobId) return;
    setGenerating(true);
    setError('');
    try {
      const response = await recruitmentApi.createCandidateMessage({
        jobPostId: Number(selectedJobId),
        resumeId: selectedResumeId ? Number(selectedResumeId) : undefined,
        matchId: selectedMatchId ? Number(selectedMatchId) : undefined,
        interviewGuideId: selectedGuideId ? Number(selectedGuideId) : undefined,
        messageType,
        channel,
        stage,
        tone,
        extraInstruction,
      });
      setDrafts(prev => [response.draft, ...prev]);
      setMessages([response.assistantMessage]);
      setActiveDraft(response.draft);
      setSubject(response.draft.subject ?? '');
      setContent(response.draft.content);
      setCreateModalOpen(false);
    } catch (err) {
      setError(err instanceof Error ? err.message : '生成沟通文案失败');
    } finally {
      setGenerating(false);
    }
  };

  const handleOptimize = async () => {
    if (!activeDraft || !optimizeInstruction.trim()) return;
    setOptimizing(true);
    setError('');
    try {
      await recruitmentApi.updateCandidateMessage(activeDraft.id, { subject, content });
      const response = await recruitmentApi.optimizeCandidateMessage(activeDraft.id, {
        instruction: optimizeInstruction,
      });
      setActiveDraft(response.draft);
      setSubject(response.draft.subject ?? '');
      setContent(response.draft.content);
      setDrafts(prev => prev.map(item => item.id === response.draft.id ? response.draft : item));
      setMessages(prev => [
        ...prev,
        {
          id: Date.now(),
          conversationId: response.assistantMessage.conversationId,
          role: 'user',
          content: optimizeInstruction,
          messageOrder: response.assistantMessage.messageOrder - 1,
          createdAt: new Date().toISOString(),
        },
        response.assistantMessage,
      ]);
      setOptimizeInstruction('');
    } catch (err) {
      setError(err instanceof Error ? err.message : '优化沟通文案失败');
    } finally {
      setOptimizing(false);
    }
  };

  const handleSave = async () => {
    if (!activeDraft) return;
    setSaving(true);
    setError('');
    try {
      const draft = await recruitmentApi.updateCandidateMessage(activeDraft.id, { subject, content });
      setActiveDraft(draft);
      setDrafts(prev => prev.map(item => item.id === draft.id ? draft : item));
    } catch (err) {
      setError(err instanceof Error ? err.message : '保存沟通文案失败');
    } finally {
      setSaving(false);
    }
  };

  const handleDeleteDraft = async (draft: CandidateMessageDraft) => {
    const label = draft.subject || MESSAGE_TYPES.find(item => item.value === draft.messageType)?.label || '该文案';
    if (!window.confirm(`确定删除「${label}」吗？`)) return;
    setError('');
    try {
      await recruitmentApi.deleteCandidateMessage(draft.id);
      const next = drafts.filter(item => item.id !== draft.id);
      setDrafts(next);
      if (activeDraft?.id === draft.id) {
        await selectDraft(next[0] ?? null);
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : '删除沟通文案失败');
    }
  };

  const handleCopy = async () => {
    if (!content) return;
    const text = subject ? `${subject}\n\n${content}` : content;
    await navigator.clipboard.writeText(text);
    setCopied(true);
    window.setTimeout(() => setCopied(false), 1600);
  };

  return (
    <motion.div className="mx-auto max-w-7xl" initial={{ opacity: 0 }} animate={{ opacity: 1 }}>
      <div className="mb-8 flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 className="flex items-center gap-3 text-2xl font-bold text-slate-800 dark:text-white">
            <MessageSquare className="h-7 w-7 text-primary-500" />
            沟通文案
          </h1>
          <p className="mt-1 text-slate-500 dark:text-slate-400">结合候选人简历、匹配结果和面试情况生成沟通文案</p>
        </div>
        <button
          onClick={() => setCreateModalOpen(true)}
          className="flex items-center gap-2 rounded-lg bg-primary-500 px-4 py-2 text-white transition-colors hover:bg-primary-600 disabled:cursor-not-allowed disabled:opacity-50"
        >
          <Plus className="h-4 w-4" />
          新增文案
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
        <div className="grid gap-6 xl:grid-cols-[280px_minmax(0,1fr)_340px]">
          <aside className="rounded-2xl border border-slate-100 bg-white p-4 shadow-sm dark:border-slate-700 dark:bg-slate-800">
            <div className="mb-3">
              <div className="mb-3 flex items-center justify-between gap-2">
                <div>
                  <div className="text-xs font-semibold uppercase tracking-wide text-slate-400">文案草稿</div>
                  <div className="mt-1 truncate text-xs text-slate-400">全部岗位 · 最近更新</div>
                </div>
                <span className="rounded-full bg-slate-100 px-2 py-0.5 text-xs text-slate-500 dark:bg-slate-700 dark:text-slate-300">{drafts.length}</span>
              </div>
              <div className="flex items-center gap-2 rounded-xl border border-slate-200 bg-slate-50 px-3 py-2 dark:border-slate-700 dark:bg-slate-900/60">
                <Search className="h-4 w-4 text-slate-400" />
                <input
                  value={draftQuery}
                  onChange={event => setDraftQuery(event.target.value)}
                  placeholder="检索候选人、类型、主题"
                  className="min-w-0 flex-1 bg-transparent text-sm text-slate-700 outline-none placeholder:text-slate-400 dark:text-slate-100"
                />
              </div>
            </div>
            <div className="space-y-2">
              {filteredDrafts.length === 0 ? (
                <div className="rounded-xl bg-slate-50 p-4 text-sm text-slate-500 dark:bg-slate-900/40 dark:text-slate-400">暂无文案。</div>
              ) : filteredDrafts.map(draft => (
                <div
                  key={draft.id}
                  className={`group flex items-start gap-2 rounded-xl border px-3 py-3 transition-colors ${activeDraft?.id === draft.id ? 'border-primary-200 bg-primary-50 dark:border-primary-800 dark:bg-primary-900/20' : 'border-slate-100 bg-white hover:bg-slate-50 dark:border-slate-700 dark:bg-slate-800 dark:hover:bg-slate-700'}`}
                >
                  <button onClick={() => selectDraft(draft)} className="min-w-0 flex-1 text-left">
                    <div className="text-sm font-semibold text-slate-700 dark:text-slate-200">
                      {MESSAGE_TYPES.find(item => item.value === draft.messageType)?.label || draft.messageType} · {CHANNELS.find(item => item.value === draft.channel)?.label || draft.channel}
                    </div>
                    <div className="mt-1 truncate text-xs text-slate-400">{draft.jobTitle || '未命名岗位'}</div>
                    <div className="mt-1 truncate text-xs text-slate-400">{draft.resumeFilename || '未指定候选人'} · {draft.stage} · {formatDate(draft.updatedAt)}</div>
                    {draft.subject && <div className="mt-1 truncate text-xs text-slate-400">{draft.subject}</div>}
                  </button>
                  <button
                    onClick={() => handleDeleteDraft(draft)}
                    className="rounded-lg p-1.5 text-slate-400 opacity-0 transition hover:bg-red-50 hover:text-red-500 group-hover:opacity-100 dark:hover:bg-red-900/20"
                    title="删除文案"
                  >
                    <Trash2 className="h-4 w-4" />
                  </button>
                </div>
              ))}
            </div>
          </aside>

          <section className="rounded-2xl border border-slate-100 bg-white shadow-sm dark:border-slate-700 dark:bg-slate-800">
            {!activeDraft ? (
              <div className="p-12 text-center text-sm text-slate-500 dark:text-slate-400">请选择或生成一份沟通文案。</div>
            ) : (
              <div className="p-6">
                <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
                  <div>
                    <div className="text-sm font-semibold text-slate-700 dark:text-slate-200">{activeDraft.stage} · {activeDraft.tone}</div>
                    <div className="mt-1 text-xs text-slate-400">当前仅支持复制和保存，发送能力后续接入。</div>
                  </div>
                  <div className="flex gap-2">
                    <button onClick={handleCopy} className="flex items-center gap-2 rounded-lg bg-slate-100 px-3 py-2 text-sm font-medium text-slate-700 transition-colors hover:bg-slate-200 dark:bg-slate-700 dark:text-slate-200 dark:hover:bg-slate-600">
                      {copied ? <Check className="h-4 w-4 text-emerald-500" /> : <Clipboard className="h-4 w-4" />}
                      {copied ? '已复制' : '复制'}
                    </button>
                    <button onClick={handleSave} disabled={saving} className="flex items-center gap-2 rounded-lg bg-slate-100 px-3 py-2 text-sm font-medium text-slate-700 transition-colors hover:bg-slate-200 disabled:opacity-50 dark:bg-slate-700 dark:text-slate-200 dark:hover:bg-slate-600">
                      {saving ? <Loader2 className="h-4 w-4 animate-spin" /> : <Save className="h-4 w-4" />}
                      保存
                    </button>
                    <button onClick={() => handleDeleteDraft(activeDraft)} className="flex items-center gap-2 rounded-lg bg-slate-100 px-3 py-2 text-sm font-medium text-slate-700 transition-colors hover:bg-red-50 hover:text-red-600 dark:bg-slate-700 dark:text-slate-200 dark:hover:bg-red-900/20 dark:hover:text-red-300">
                      <Trash2 className="h-4 w-4" />
                      删除
                    </button>
                  </div>
                </div>

                <label className="mb-4 block">
                  <span className="mb-2 block text-sm font-semibold text-slate-700 dark:text-slate-200">主题</span>
                  <input value={subject} onChange={event => setSubject(event.target.value)} className="w-full rounded-xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm outline-none focus:border-primary-500 dark:border-slate-700 dark:bg-slate-900/60 dark:text-slate-100" />
                </label>
                <label className="block">
                  <span className="mb-2 block text-sm font-semibold text-slate-700 dark:text-slate-200">正文</span>
                  <textarea value={content} onChange={event => setContent(event.target.value)} rows={18} className="w-full resize-y rounded-xl border border-slate-200 bg-slate-50 p-4 text-sm leading-6 text-slate-700 outline-none focus:border-primary-500 dark:border-slate-700 dark:bg-slate-900/60 dark:text-slate-200" />
                </label>

                <div className="mt-5 grid gap-4 lg:grid-cols-2">
                  <InfoList title="生成要点" items={keyPoints} />
                  <InfoList title="发送前注意" items={cautionNotes} />
                </div>
              </div>
            )}
          </section>

          <aside className="rounded-2xl border border-slate-100 bg-white p-4 shadow-sm dark:border-slate-700 dark:bg-slate-800">
            <div className="mb-3 text-xs font-semibold uppercase tracking-wide text-slate-400">多轮优化</div>
            <div className="mb-4 max-h-[420px] space-y-3 overflow-y-auto pr-1">
              {messages.length === 0 ? (
                <div className="rounded-xl bg-slate-50 p-4 text-sm text-slate-500 dark:bg-slate-900/40 dark:text-slate-400">生成文案后，可以继续要求 AI 调整语气、长度或重点。</div>
              ) : messages.map(message => (
                <div key={`${message.id}-${message.messageOrder}`} className={`rounded-xl p-3 text-sm leading-6 ${message.role === 'user' ? 'bg-primary-50 text-primary-900 dark:bg-primary-900/20 dark:text-primary-100' : 'bg-slate-50 text-slate-600 dark:bg-slate-900/50 dark:text-slate-300'}`}>
                  <div className="mb-1 text-xs font-semibold opacity-70">{message.role === 'user' ? 'HR' : 'AI'}</div>
                  {message.content}
                </div>
              ))}
            </div>
            <textarea
              value={optimizeInstruction}
              onChange={event => setOptimizeInstruction(event.target.value)}
              rows={5}
              placeholder="例如：更温和一点，减少模板感，并加一句对项目经验的认可"
              className="w-full resize-none rounded-xl border border-slate-200 bg-slate-50 p-3 text-sm outline-none focus:border-primary-500 dark:border-slate-700 dark:bg-slate-900/60 dark:text-slate-100"
            />
            <button
              onClick={handleOptimize}
              disabled={!activeDraft || !optimizeInstruction.trim() || optimizing}
              className="mt-3 flex w-full items-center justify-center gap-2 rounded-lg bg-primary-500 px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-primary-600 disabled:cursor-not-allowed disabled:opacity-50"
            >
              {optimizing ? <Loader2 className="h-4 w-4 animate-spin" /> : <Send className="h-4 w-4" />}
              发送优化要求
            </button>
          </aside>
        </div>
      )}

      {createModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/40 p-4">
          <motion.div
            initial={{ opacity: 0, y: 16, scale: 0.98 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            className="w-full max-w-4xl rounded-2xl border border-slate-100 bg-white shadow-2xl dark:border-slate-700 dark:bg-slate-800"
          >
            <div className="flex items-center justify-between border-b border-slate-100 px-5 py-4 dark:border-slate-700">
              <div>
                <h2 className="font-semibold text-slate-800 dark:text-white">新增沟通文案</h2>
                <p className="mt-1 text-xs text-slate-400">选择候选人上下文和沟通场景，生成可编辑文案。</p>
              </div>
              <button
                onClick={() => setCreateModalOpen(false)}
                className="rounded-lg p-2 text-slate-400 transition-colors hover:bg-slate-100 hover:text-slate-700 dark:hover:bg-slate-700 dark:hover:text-slate-100"
                title="关闭"
              >
                <X className="h-4 w-4" />
              </button>
            </div>
            <div className="max-h-[70vh] overflow-y-auto p-5">
              <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
                <SelectField label="岗位 JD" value={selectedJobId} onChange={handleJobChange}>
                  <option value="">请选择岗位</option>
                  {jobs.map(job => <option key={job.id} value={job.id}>{job.title}</option>)}
                </SelectField>
                <SelectField label="匹配报告" value={selectedMatchId} onChange={handleMatchChange}>
                  <option value="">不使用匹配报告</option>
                  {matches.map(match => <option key={match.id} value={match.id}>{match.resumeFilename} - {match.matchScore} 分</option>)}
                </SelectField>
                <SelectField label="候选人简历" value={selectedResumeId} onChange={value => setSelectedResumeId(value ? Number(value) : '')}>
                  <option value="">不指定候选人</option>
                  {resumes.map(resume => <option key={resume.id} value={resume.id}>{resume.filename}</option>)}
                </SelectField>
                <SelectField label="面试题纲" value={selectedGuideId} onChange={value => setSelectedGuideId(value ? Number(value) : '')}>
                  <option value="">不使用题纲</option>
                  {guides.map(guide => <option key={guide.id} value={guide.id}>{guide.interviewRound} · {guide.mode === 'JOB_ONLY' ? '通用' : guide.resumeFilename || '定制'}</option>)}
                </SelectField>
                <SelectField label="文案类型" value={messageType} onChange={setMessageType}>
                  {MESSAGE_TYPES.map(item => <option key={item.value} value={item.value}>{item.label}</option>)}
                </SelectField>
                <SelectField label="沟通渠道" value={channel} onChange={setChannel}>
                  {CHANNELS.map(item => <option key={item.value} value={item.value}>{item.label}</option>)}
                </SelectField>
                <SelectField label="候选人阶段" value={stage} onChange={setStage}>
                  {STAGES.map(item => <option key={item} value={item}>{item}</option>)}
                </SelectField>
                <SelectField label="语气" value={tone} onChange={setTone}>
                  {TONES.map(item => <option key={item} value={item}>{item}</option>)}
                </SelectField>
              </div>
              <label className="mt-4 block">
                <span className="mb-2 block text-sm font-semibold text-slate-700 dark:text-slate-200">额外要求</span>
                <input
                  value={extraInstruction}
                  onChange={event => setExtraInstruction(event.target.value)}
                  placeholder="例如：控制在 150 字内，强调远程面试链接稍后发送"
                  className="w-full rounded-xl border border-slate-200 bg-white px-3 py-2.5 text-sm outline-none focus:border-primary-500 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-100"
                />
              </label>
            </div>
            <div className="flex justify-end gap-2 border-t border-slate-100 px-5 py-4 dark:border-slate-700">
              <button
                onClick={() => setCreateModalOpen(false)}
                className="rounded-lg bg-slate-100 px-4 py-2 text-sm font-medium text-slate-700 transition-colors hover:bg-slate-200 dark:bg-slate-700 dark:text-slate-200 dark:hover:bg-slate-600"
              >
                取消
              </button>
              <button
                onClick={handleGenerate}
                disabled={!selectedJobId || generating}
                className="flex items-center gap-2 rounded-lg bg-primary-500 px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-primary-600 disabled:cursor-not-allowed disabled:opacity-50"
              >
                {generating ? <Loader2 className="h-4 w-4 animate-spin" /> : <Sparkles className="h-4 w-4" />}
                生成文案
              </button>
            </div>
          </motion.div>
        </div>
      )}
    </motion.div>
  );
}

function SelectField({
  label,
  value,
  onChange,
  children,
}: {
  label: string;
  value: string | number;
  onChange: (value: string) => void;
  children: React.ReactNode;
}) {
  return (
    <label>
      <span className="mb-2 block text-sm font-semibold text-slate-700 dark:text-slate-200">{label}</span>
      <select value={value} onChange={event => onChange(event.target.value)} className="w-full rounded-xl border border-slate-200 bg-white px-3 py-2.5 text-sm dark:border-slate-700 dark:bg-slate-900 dark:text-slate-100">
        {children}
      </select>
    </label>
  );
}

function InfoList({ title, items }: { title: string; items: string[] }) {
  return (
    <div className="rounded-xl bg-slate-50 p-4 dark:bg-slate-900/40">
      <div className="mb-2 text-sm font-semibold text-slate-700 dark:text-slate-200">{title}</div>
      {items.length === 0 ? (
        <div className="text-sm text-slate-400">暂无</div>
      ) : (
        <ul className="space-y-2 text-sm text-slate-500 dark:text-slate-400">
          {items.map((item, index) => <li key={index}>{item}</li>)}
        </ul>
      )}
    </div>
  );
}
