import { useEffect, useMemo, useRef, useState } from 'react';
import { motion } from 'framer-motion';
import {
  AlertTriangle,
  Briefcase,
  Check,
  CheckCircle,
  Clock,
  Copy,
  FileText,
  Loader2,
  Plus,
  RotateCcw,
  Send,
  ShieldCheck,
  Trash2,
  X,
} from 'lucide-react';
import { recruitmentApi } from '../api/recruitment';
import type {
  ComplianceRiskItem,
  JobConversationMessage,
  JobPost,
  JobPostDetail,
  JobPostVersion,
  JobProfile,
} from '../types/recruitment';

function parseJsonList(json?: string | null): string[] {
  if (!json) return [];
  try {
    const value = JSON.parse(json) as unknown;
    return Array.isArray(value) ? value.map(String).filter(Boolean) : [];
  } catch {
    return [];
  }
}

function getRiskClass(level: string) {
  if (level === 'HIGH') return 'bg-red-50 text-red-700 border-red-100 dark:bg-red-900/20 dark:text-red-300 dark:border-red-800';
  if (level === 'MEDIUM') return 'bg-amber-50 text-amber-700 border-amber-100 dark:bg-amber-900/20 dark:text-amber-300 dark:border-amber-800';
  return 'bg-emerald-50 text-emerald-700 border-emerald-100 dark:bg-emerald-900/20 dark:text-emerald-300 dark:border-emerald-800';
}

function formatDateTime(value?: string) {
  if (!value) return '-';
  return new Date(value).toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  });
}

function ProfileList({ title, items }: { title: string; items: string[] }) {
  if (items.length === 0) return null;
  return (
    <div>
      <p className="mb-2 text-xs font-semibold uppercase tracking-wide text-slate-400">{title}</p>
      <div className="space-y-2">
        {items.map((item, index) => (
          <div key={`${title}-${index}`} className="rounded-lg bg-slate-50 px-3 py-2 text-sm text-slate-600 dark:bg-slate-900/40 dark:text-slate-300">
            {item}
          </div>
        ))}
      </div>
    </div>
  );
}

export default function JobPostAssistantPage() {
  const [jobs, setJobs] = useState<JobPost[]>([]);
  const [selectedJobId, setSelectedJobId] = useState<number | null>(null);
  const [detail, setDetail] = useState<JobPostDetail | null>(null);
  const [versions, setVersions] = useState<JobPostVersion[]>([]);
  const [messages, setMessages] = useState<JobConversationMessage[]>([]);
  const [conversationId, setConversationId] = useState<number | undefined>();
  const [createModalOpen, setCreateModalOpen] = useState(false);
  const [newJobTitle, setNewJobTitle] = useState('');
  const [input, setInput] = useState('');
  const inputRef = useRef<HTMLTextAreaElement | null>(null);
  const [jdDraftText, setJdDraftText] = useState('');
  const [loading, setLoading] = useState(true);
  const [sending, setSending] = useState(false);
  const [savingJd, setSavingJd] = useState(false);
  const [streamStatus, setStreamStatus] = useState('');
  const [copied, setCopied] = useState(false);
  const [error, setError] = useState('');
  const [activePanel, setActivePanel] = useState<'jd' | 'risks' | 'profile' | 'versions'>('jd');

  const currentVersion = detail?.currentVersion;
  const risks = detail?.complianceRisks ?? [];
  const profile = detail?.jobProfile ?? null;

  const highRiskCount = risks.filter(item => item.riskLevel === 'HIGH').length;
  const mediumRiskCount = risks.filter(item => item.riskLevel === 'MEDIUM').length;

  const profileGroups = useMemo(() => {
    const p: JobProfile | null | undefined = profile;
    return {
      hardRequirements: parseJsonList(p?.hardRequirementsJson),
      competencies: parseJsonList(p?.competenciesJson),
      bonusItems: parseJsonList(p?.bonusItemsJson),
      verificationPoints: parseJsonList(p?.verificationPointsJson),
    };
  }, [profile]);

  useEffect(() => {
    setJdDraftText(currentVersion?.content ?? '');
  }, [currentVersion?.id, currentVersion?.content]);

  useEffect(() => {
    const textarea = inputRef.current;
    if (!textarea) return;
    textarea.style.height = 'auto';
    textarea.style.height = `${Math.min(textarea.scrollHeight, 112)}px`;
  }, [input]);

  const loadJobs = async () => {
    setLoading(true);
    setError('');
    try {
      const data = await recruitmentApi.listJobs();
      setJobs(data);
      const firstId = selectedJobId ?? data[0]?.id ?? null;
      setSelectedJobId(firstId);
      if (firstId) {
        await loadJobDetail(firstId);
      } else {
        setSelectedJobId(null);
        setDetail(null);
        setVersions([]);
        setMessages([]);
        setConversationId(undefined);
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : '加载岗位失败');
    } finally {
      setLoading(false);
    }
  };

  const loadJobDetail = async (jobId: number, preferredConversationId?: number) => {
    const [jobDetail, jobVersions, jobConversations] = await Promise.all([
      recruitmentApi.getJob(jobId),
      recruitmentApi.listVersions(jobId),
      recruitmentApi.listConversations(jobId),
    ]);
    setDetail(jobDetail);
    setVersions(jobVersions);
    const activeConversationId = preferredConversationId ?? jobConversations[0]?.id;
    setConversationId(activeConversationId);
    if (activeConversationId) {
      setMessages(await recruitmentApi.listMessages(activeConversationId));
    } else {
      setMessages([]);
    }
  };

  useEffect(() => {
    loadJobs();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const handleSelectJob = async (jobId: number) => {
    setSelectedJobId(jobId);
    setError('');
    try {
      await loadJobDetail(jobId);
    } catch (err) {
      setError(err instanceof Error ? err.message : '加载岗位详情失败');
    }
  };

  const handleCreateJob = async () => {
    if (!newJobTitle.trim()) {
      setError('请输入岗位名称');
      return;
    }
    setError('');
    try {
      const job = await recruitmentApi.createJob({ title: newJobTitle.trim() });
      setJobs(prev => [job, ...prev]);
      setSelectedJobId(job.id);
      setCreateModalOpen(false);
      setNewJobTitle('');
      await loadJobDetail(job.id);
    } catch (err) {
      setError(err instanceof Error ? err.message : '创建岗位失败');
    }
  };

  const handleArchiveJob = async (jobId: number) => {
    const job = jobs.find(item => item.id === jobId);
    if (!window.confirm(`确定删除「${job?.title || '该岗位'}」吗？删除后不会出现在岗位列表中。`)) {
      return;
    }
    setError('');
    try {
      await recruitmentApi.archiveJob(jobId);
      const nextJobs = jobs.filter(item => item.id !== jobId);
      setJobs(nextJobs);
      if (selectedJobId === jobId) {
        const nextJobId = nextJobs[0]?.id ?? null;
        setSelectedJobId(nextJobId);
        if (nextJobId) {
          await loadJobDetail(nextJobId);
        } else {
          setDetail(null);
          setVersions([]);
          setMessages([]);
          setConversationId(undefined);
        }
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : '删除岗位失败');
    }
  };

  const handleSend = async () => {
    if (!selectedJobId || !input.trim()) return;
    const userInput = input.trim();
    const optimisticUserMessage: JobConversationMessage = {
      id: -Date.now(),
      conversationId: conversationId ?? 0,
      role: 'user',
      content: userInput,
      relatedVersionId: null,
      messageOrder: messages.length + 1,
      createdAt: new Date().toISOString(),
    };
    const optimisticAssistantMessage: JobConversationMessage = {
      id: optimisticUserMessage.id - 1,
      conversationId: conversationId ?? 0,
      role: 'assistant',
      content: '正在生成 JD 草稿...',
      relatedVersionId: null,
      messageOrder: messages.length + 2,
      createdAt: new Date().toISOString(),
    };
    setSending(true);
    setError('');
    setStreamStatus('正在发送...');
    setInput('');
    setMessages(prev => [...prev, optimisticUserMessage, optimisticAssistantMessage]);

    await recruitmentApi.chatStream(
      selectedJobId,
      {
        conversationId,
        message: userInput,
      },
      event => {
        if (event.type === 'status') {
          setStreamStatus(event.message);
          setMessages(prev => prev.map(message =>
            message.id === optimisticAssistantMessage.id
              ? { ...message, content: event.message }
              : message
          ));
          return;
        }
        if (event.type === 'assistant_delta') {
          setStreamStatus('正在输出回复...');
          setMessages(prev => prev.map(message =>
            message.id === optimisticAssistantMessage.id
              ? {
                  ...message,
                  content: message.content.startsWith('正在') ? event.content : `${message.content}${event.content}`,
                }
              : message
          ));
          return;
        }
        if (event.type === 'complete') {
          const response = event.response;
          setConversationId(response.conversation.id);
          setDetail({
            jobPost: response.jobPost,
            currentVersion: response.version,
            complianceRisks: response.complianceRisks,
            jobProfile: response.jobProfile ?? null,
          });
          setVersions(prev => [response.version, ...prev.filter(version => version.id !== response.version.id)]);
          setJobs(prev => prev.map(job => job.id === response.jobPost.id ? response.jobPost : job));
          setMessages(prev => prev.map(message =>
            message.id === optimisticAssistantMessage.id ? response.assistantMessage : message
          ).map(message =>
            message.id === optimisticUserMessage.id
              ? { ...message, conversationId: response.conversation.id }
              : message
          ));
          setStreamStatus('');
          void loadJobDetail(response.jobPost.id, response.conversation.id);
        }
      },
      () => {
        setSending(false);
        setStreamStatus('');
      },
      async err => {
        setSending(false);
        setStreamStatus('');
        setError(err.message || '生成 JD 失败');
        await loadJobDetail(selectedJobId);
      }
    );
  };

  const handleRestoreVersion = async (versionId: number) => {
    if (!selectedJobId) return;
    setError('');
    try {
      const restored = await recruitmentApi.restoreVersion(selectedJobId, versionId);
      setDetail(restored);
      setVersions(await recruitmentApi.listVersions(selectedJobId));
      setActivePanel('jd');
    } catch (err) {
      setError(err instanceof Error ? err.message : '恢复版本失败');
    }
  };

  const handleCopyJd = async () => {
    if (!jdDraftText.trim()) return;
    await navigator.clipboard.writeText(jdDraftText);
    setCopied(true);
    window.setTimeout(() => setCopied(false), 1600);
  };

  const handleSaveJdDraft = async () => {
    if (!selectedJobId || !jdDraftText.trim()) return;
    setSavingJd(true);
    setError('');
    try {
      const version = await recruitmentApi.saveVersion(selectedJobId, {
        content: jdDraftText.trim(),
        structuredContentJson: currentVersion?.structuredContentJson ?? undefined,
        changeSummary: 'HR 手动编辑保存',
      });
      const [risks, profile] = await Promise.all([
        recruitmentApi.reviewCompliance(version.id),
        recruitmentApi.extractProfile(version.id),
      ]);
      setDetail(prev => prev ? {
        ...prev,
        currentVersion: version,
        complianceRisks: risks,
        jobProfile: profile,
        jobPost: { ...prev.jobPost, currentVersionId: version.id },
      } : prev);
      setVersions(prev => [version, ...prev.filter(item => item.id !== version.id)]);
      setActivePanel('jd');
    } catch (err) {
      setError(err instanceof Error ? err.message : '保存 JD 草稿失败');
    } finally {
      setSavingJd(false);
    }
  };

  return (
    <motion.div className="mx-auto flex h-[calc(100vh-5rem)] max-w-7xl flex-col overflow-hidden" initial={{ opacity: 0 }} animate={{ opacity: 1 }}>
      <div className="mb-8 flex shrink-0 items-center justify-between gap-4">
        <div>
          <h1 className="flex items-center gap-3 text-2xl font-bold text-slate-800 dark:text-white">
            <Briefcase className="h-7 w-7 text-primary-500" />
            JD 助手
          </h1>
          <p className="mt-1 text-slate-500 dark:text-slate-400">通过多轮对话生成 JD，并自动完成合规初筛和岗位画像抽取</p>
        </div>
        <button
          onClick={() => setCreateModalOpen(true)}
          className="flex items-center gap-2 rounded-lg bg-primary-500 px-4 py-2 text-white transition-colors hover:bg-primary-600"
        >
          <Plus className="h-4 w-4" />
          新建 JD
        </button>
      </div>

      {error && (
        <div className="mb-4 flex shrink-0 items-center gap-2 rounded-xl border border-red-100 bg-red-50 px-4 py-3 text-sm text-red-600 dark:border-red-800 dark:bg-red-900/20 dark:text-red-300">
          <AlertTriangle className="h-4 w-4" />
          {error}
        </div>
      )}

      {loading ? (
        <div className="flex min-h-0 flex-1 items-center justify-center">
          <div className="h-10 w-10 animate-spin rounded-full border-3 border-slate-200 border-t-primary-500" />
        </div>
      ) : (
        <div className="grid min-h-0 flex-1 grid-cols-[260px_minmax(0,1fr)_420px] gap-5">
          <aside className="flex min-h-0 flex-col rounded-2xl border border-slate-100 bg-white p-4 shadow-sm dark:border-slate-700 dark:bg-slate-800">
            <div className="mb-3 shrink-0 text-xs font-semibold uppercase tracking-wide text-slate-400">岗位列表</div>
            <div className="min-h-0 flex-1 space-y-2 overflow-y-auto pr-1">
              {jobs.length === 0 ? (
                <div className="rounded-xl bg-slate-50 p-4 text-sm text-slate-500 dark:bg-slate-900/40 dark:text-slate-400">
                  还没有岗位，点击右上角新建一个 JD。
                </div>
              ) : jobs.map(job => (
                <div
                  key={job.id}
                  className={`group flex items-start gap-2 rounded-xl border px-3 py-3 transition-colors ${
                    selectedJobId === job.id
                      ? 'border-primary-200 bg-primary-50 text-primary-700 dark:border-primary-800 dark:bg-primary-900/20 dark:text-primary-300'
                      : 'border-slate-100 bg-white text-slate-600 hover:bg-slate-50 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-300 dark:hover:bg-slate-700'
                  }`}
                >
                  <button onClick={() => handleSelectJob(job.id)} className="min-w-0 flex-1 text-left">
                    <div className="truncate text-sm font-semibold">{job.title}</div>
                    <div className="mt-1 text-xs text-slate-400">{formatDateTime(job.updatedAt)}</div>
                  </button>
                  <button
                    onClick={() => handleArchiveJob(job.id)}
                    className="rounded-lg p-1.5 text-slate-400 opacity-0 transition hover:bg-red-50 hover:text-red-500 group-hover:opacity-100 dark:hover:bg-red-900/20"
                    title="删除岗位"
                  >
                    <Trash2 className="h-4 w-4" />
                  </button>
                </div>
              ))}
            </div>
          </aside>

          <section className="flex min-h-0 min-w-0 flex-col overflow-hidden rounded-2xl border border-slate-100 bg-white shadow-sm dark:border-slate-700 dark:bg-slate-800">
            <div className="shrink-0 border-b border-slate-100 p-4 dark:border-slate-700">
              <div className="flex items-center justify-between gap-3">
                <div className="flex items-center gap-2 text-sm font-semibold text-slate-700 dark:text-slate-200">
                  <FileText className="h-4 w-4 text-primary-500" />
                  多轮对话优化
                </div>
                <div className="text-xs text-slate-400">
                  {conversationId ? `当前会话 #${conversationId}` : '新会话'}
                </div>
              </div>
            </div>
            <div className="min-h-0 flex-1 space-y-4 overflow-y-auto p-5">
              {messages.length === 0 ? (
                <div className="rounded-2xl bg-slate-50 p-6 text-sm text-slate-500 dark:bg-slate-900/40 dark:text-slate-400">
                  输入岗位需求，例如“帮我写一个 Java 后端开发工程师 JD，3-5 年经验，负责 AI 面试平台后端”。
                </div>
              ) : messages.map(message => (
                <div
                  key={message.id}
                  className={`max-w-[85%] rounded-2xl px-4 py-3 text-sm leading-6 ${
                    message.role === 'user'
                      ? 'ml-auto bg-primary-500 text-white'
                      : 'bg-slate-100 text-slate-700 dark:bg-slate-900 dark:text-slate-200'
                  }`}
                >
                  {message.content}
                  {sending && message.id < 0 && message.role === 'assistant' && (
                    <Loader2 className="ml-2 inline h-3 w-3 animate-spin opacity-70" />
                  )}
                </div>
              ))}
            </div>
            <div className="shrink-0 border-t border-slate-100 p-4 dark:border-slate-700">
              {streamStatus && (
                <div className="mb-2 text-xs text-slate-400">{streamStatus}</div>
              )}
              <div className="flex items-end gap-2 rounded-2xl border border-slate-200 bg-white px-3 py-2 transition focus-within:border-primary-500 focus-within:ring-2 focus-within:ring-primary-100 dark:border-slate-700 dark:bg-slate-900 dark:focus-within:ring-primary-900/30">
                <textarea
                  ref={inputRef}
                  value={input}
                  onChange={event => setInput(event.target.value)}
                  onKeyDown={event => {
                    if (event.key === 'Enter' && !event.shiftKey && !event.nativeEvent.isComposing) {
                      event.preventDefault();
                      handleSend();
                    }
                  }}
                  placeholder="输入你的 JD 生成或优化要求..."
                  rows={1}
                  className="max-h-28 min-h-10 flex-1 resize-none overflow-y-auto border-0 bg-transparent py-2 text-sm leading-6 text-slate-700 outline-none placeholder:text-slate-400 dark:text-slate-100"
                />
                <button
                  onClick={handleSend}
                  disabled={!selectedJobId || !input.trim() || sending}
                  className="mb-0.5 flex h-9 w-9 shrink-0 items-center justify-center rounded-xl bg-primary-500 text-white transition-colors hover:bg-primary-600 disabled:cursor-not-allowed disabled:opacity-50"
                  title="发送"
                >
                  {sending ? <Loader2 className="h-4 w-4 animate-spin" /> : <Send className="h-4 w-4" />}
                </button>
              </div>
            </div>
          </section>

          <aside className="flex min-h-0 min-w-0 flex-col overflow-hidden rounded-2xl border border-slate-100 bg-white shadow-sm dark:border-slate-700 dark:bg-slate-800">
            <div className="shrink-0 border-b border-slate-100 p-4 dark:border-slate-700">
              <div className="mb-3 flex items-center justify-between">
                <div>
                  <h2 className="font-semibold text-slate-800 dark:text-white">当前 JD 草稿</h2>
                  <p className="text-xs text-slate-400">AI 辅助审查，需人工确认</p>
                </div>
                <div className="flex items-center gap-2">
                  <div className={`rounded-full border px-2.5 py-1 text-xs font-semibold ${getRiskClass(highRiskCount > 0 ? 'HIGH' : mediumRiskCount > 0 ? 'MEDIUM' : 'LOW')}`}>
                    {highRiskCount > 0 ? `${highRiskCount} 高风险` : mediumRiskCount > 0 ? `${mediumRiskCount} 中风险` : '低风险'}
                  </div>
                </div>
              </div>
              <div className="grid grid-cols-4 gap-1 rounded-lg bg-slate-100 p-1 dark:bg-slate-900/70">
                {([
                  ['jd', 'JD'],
                  ['risks', '风险'],
                  ['profile', '画像'],
                  ['versions', '版本'],
                ] as const).map(([key, label]) => (
                  <button
                    key={key}
                    onClick={() => setActivePanel(key)}
                    className={`rounded-md px-2 py-1.5 text-xs font-medium transition-colors ${
                      activePanel === key
                        ? 'bg-white text-primary-600 shadow-sm dark:bg-slate-800 dark:text-primary-300'
                        : 'text-slate-500 hover:text-slate-800 dark:text-slate-400 dark:hover:text-white'
                    }`}
                  >
                    {label}
                  </button>
                ))}
              </div>
            </div>

            <div className="min-h-0 flex-1 p-4">
              {activePanel === 'jd' && (
                <div className="flex h-full min-h-0 flex-col gap-3">
                  <textarea
                    value={jdDraftText}
                    onChange={event => setJdDraftText(event.target.value)}
                    placeholder="还没有 JD 草稿。可以在左侧对话生成，也可以直接在这里编辑。"
                    className="min-h-0 flex-1 resize-none rounded-xl border border-slate-200 bg-slate-50 p-4 text-sm leading-6 text-slate-700 outline-none transition-colors focus:border-primary-500 focus:bg-white dark:border-slate-700 dark:bg-slate-900/50 dark:text-slate-200 dark:focus:bg-slate-900"
                  />
                  <div className="flex justify-end gap-2">
                    <button
                      onClick={handleCopyJd}
                      disabled={!jdDraftText.trim()}
                      className="flex items-center gap-2 rounded-lg bg-slate-100 px-3 py-2 text-sm font-medium text-slate-700 transition-colors hover:bg-slate-200 disabled:cursor-not-allowed disabled:opacity-50 dark:bg-slate-700 dark:text-slate-200 dark:hover:bg-slate-600"
                    >
                      {copied ? <Check className="h-4 w-4 text-emerald-500" /> : <Copy className="h-4 w-4" />}
                      {copied ? '已复制' : '复制'}
                    </button>
                    <button
                      onClick={handleSaveJdDraft}
                      disabled={!selectedJobId || !jdDraftText.trim() || savingJd}
                      className="flex items-center gap-2 rounded-lg bg-primary-500 px-3 py-2 text-sm font-medium text-white transition-colors hover:bg-primary-600 disabled:cursor-not-allowed disabled:opacity-50"
                    >
                      {savingJd ? <Loader2 className="h-4 w-4 animate-spin" /> : <CheckCircle className="h-4 w-4" />}
                      保存
                    </button>
                  </div>
                </div>
              )}

              {activePanel === 'risks' && (
                <div className="h-full space-y-3 overflow-y-auto pr-1">
                  {risks.length === 0 ? (
                    <div className="flex items-center gap-2 rounded-xl bg-emerald-50 p-4 text-sm text-emerald-700 dark:bg-emerald-900/20 dark:text-emerald-300">
                      <ShieldCheck className="h-4 w-4" />
                      暂无明显合规风险。
                    </div>
                  ) : risks.map((risk: ComplianceRiskItem) => (
                    <div key={risk.id} className={`rounded-xl border p-4 ${getRiskClass(risk.riskLevel)}`}>
                      <div className="mb-2 flex items-center justify-between gap-2">
                        <span className="text-sm font-semibold">{risk.riskType}</span>
                        <span className="text-xs">{risk.riskLevel}</span>
                      </div>
                      {risk.matchedText && <p className="mb-2 text-xs opacity-80">命中：{risk.matchedText}</p>}
                      <p className="text-sm leading-6">{risk.description}</p>
                      {risk.suggestion && <p className="mt-2 text-sm leading-6">建议：{risk.suggestion}</p>}
                    </div>
                  ))}
                </div>
              )}

              {activePanel === 'profile' && (
                <div className="h-full space-y-5 overflow-y-auto pr-1">
                  <ProfileList title="硬性条件" items={profileGroups.hardRequirements} />
                  <ProfileList title="核心能力" items={profileGroups.competencies} />
                  <ProfileList title="加分项" items={profileGroups.bonusItems} />
                  <ProfileList title="面试验证点" items={profileGroups.verificationPoints} />
                  {!profile && (
                    <div className="rounded-xl bg-slate-50 p-4 text-sm text-slate-500 dark:bg-slate-900/40 dark:text-slate-400">
                      生成 JD 后会自动抽取岗位画像。
                    </div>
                  )}
                </div>
              )}

              {activePanel === 'versions' && (
                <div className="h-full space-y-3 overflow-y-auto pr-1">
                  {versions.length === 0 ? (
                    <div className="rounded-xl bg-slate-50 p-4 text-sm text-slate-500 dark:bg-slate-900/40 dark:text-slate-400">暂无版本。</div>
                  ) : versions.map(version => (
                    <div key={version.id} className="rounded-xl border border-slate-100 p-4 dark:border-slate-700">
                      <div className="mb-2 flex items-center justify-between">
                        <div className="flex items-center gap-2 text-sm font-semibold text-slate-700 dark:text-slate-200">
                          {version.id === detail?.jobPost.currentVersionId ? <CheckCircle className="h-4 w-4 text-emerald-500" /> : <Clock className="h-4 w-4 text-slate-400" />}
                          v{version.versionNo}
                        </div>
                        <button
                          onClick={() => handleRestoreVersion(version.id)}
                          className="flex items-center gap-1 rounded-lg bg-slate-100 px-2 py-1 text-xs text-slate-600 hover:bg-slate-200 dark:bg-slate-700 dark:text-slate-200 dark:hover:bg-slate-600"
                        >
                          <RotateCcw className="h-3 w-3" />
                          恢复
                        </button>
                      </div>
                      <p className="text-xs text-slate-400">{formatDateTime(version.createdAt)}</p>
                      {version.changeSummary && <p className="mt-2 text-sm text-slate-500 dark:text-slate-400">{version.changeSummary}</p>}
                    </div>
                  ))}
                </div>
              )}
            </div>
          </aside>
        </div>
      )}

      {createModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/40 p-4">
          <motion.div
            initial={{ opacity: 0, scale: 0.98 }}
            animate={{ opacity: 1, scale: 1 }}
            className="w-full max-w-md rounded-2xl border border-slate-100 bg-white p-5 shadow-xl dark:border-slate-700 dark:bg-slate-800"
          >
            <div className="mb-5 flex items-center justify-between">
              <div>
                <h2 className="text-lg font-semibold text-slate-800 dark:text-white">新建 JD</h2>
                <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">请输入岗位名称，创建后即可开始对话生成 JD。</p>
              </div>
              <button
                onClick={() => setCreateModalOpen(false)}
                className="rounded-lg p-2 text-slate-400 hover:bg-slate-100 hover:text-slate-600 dark:hover:bg-slate-700 dark:hover:text-slate-200"
              >
                <X className="h-5 w-5" />
              </button>
            </div>
            <label className="block">
              <span className="mb-2 block text-sm font-semibold text-slate-700 dark:text-slate-200">岗位名称</span>
              <input
                autoFocus
                value={newJobTitle}
                onChange={event => setNewJobTitle(event.target.value)}
                onKeyDown={event => {
                  if (event.key === 'Enter') {
                    handleCreateJob();
                  }
                }}
                placeholder="例如：Java 后端开发工程师"
                className="w-full rounded-xl border border-slate-200 bg-white px-4 py-3 text-sm text-slate-700 outline-none focus:border-primary-500 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-100"
              />
            </label>
            <div className="mt-6 flex justify-end gap-3">
              <button
                onClick={() => setCreateModalOpen(false)}
                className="rounded-lg bg-slate-100 px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-200 dark:bg-slate-700 dark:text-slate-200 dark:hover:bg-slate-600"
              >
                取消
              </button>
              <button
                onClick={handleCreateJob}
                className="rounded-lg bg-primary-500 px-4 py-2 text-sm font-medium text-white hover:bg-primary-600"
              >
                创建
              </button>
            </div>
          </motion.div>
        </div>
      )}
    </motion.div>
  );
}
