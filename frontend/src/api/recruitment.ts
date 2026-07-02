import { request } from './request';
import { streamSse } from './stream';
import type {
  ComplianceRiskItem,
  CreateJobPostRequest,
  JobChatRequest,
  JobChatResponse,
  JobChatStreamEvent,
  JobConversation,
  JobConversationMessage,
  JobPost,
  JobPostDetail,
  JobPostVersion,
  JobProfile,
  SaveJobVersionRequest,
  CandidateJobMatch,
  CreateCandidateMatchRequest,
  InterviewGuide,
  CreateInterviewGuideRequest,
  UpdateInterviewGuideRequest,
  CandidateMessageDraft,
  CandidateMessageConversationMessage,
  CandidateMessageDraftResponse,
  CreateCandidateMessageDraftRequest,
  OptimizeCandidateMessageDraftRequest,
  UpdateCandidateMessageDraftRequest,
} from '../types/recruitment';

export const recruitmentApi = {
  listJobs(): Promise<JobPost[]> {
    return request.get('/api/recruitment/jobs');
  },

  createJob(data: CreateJobPostRequest): Promise<JobPost> {
    return request.post('/api/recruitment/jobs', data);
  },

  getJob(jobId: number): Promise<JobPostDetail> {
    return request.get(`/api/recruitment/jobs/${jobId}`);
  },

  archiveJob(jobId: number): Promise<void> {
    return request.post(`/api/recruitment/jobs/${jobId}/archive`);
  },

  chat(jobId: number, data: JobChatRequest): Promise<JobChatResponse> {
    return request.post(`/api/recruitment/jobs/${jobId}/chat`, data);
  },

  chatStream(
    jobId: number,
    data: JobChatRequest,
    onEvent: (event: JobChatStreamEvent) => void,
    onComplete: () => void,
    onError: (error: Error) => void
  ): Promise<void> {
    return streamSse({
      url: `/api/recruitment/jobs/${jobId}/chat/stream`,
      init: {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(data),
      },
      onMessage: chunk => {
        const parsed = JSON.parse(chunk) as unknown;
        if (parsed && typeof parsed === 'object' && 'jobPost' in parsed) {
          onEvent({ type: 'complete', response: parsed as JobChatResponse });
          return;
        }
        const record = parsed as Record<string, unknown>;
        if (typeof record.content === 'string') {
          onEvent({ type: 'assistant_delta', content: record.content });
          return;
        }
        const message = typeof record.message === 'string' ? record.message : '';
        if (message) {
          onEvent({ type: 'status', message });
        }
      },
      onComplete,
      onError,
      parseMode: 'event',
      trimDataPrefixSpace: false,
      dataJoiner: '',
    });
  },

  listVersions(jobId: number): Promise<JobPostVersion[]> {
    return request.get(`/api/recruitment/jobs/${jobId}/versions`);
  },

  saveVersion(jobId: number, data: SaveJobVersionRequest): Promise<JobPostVersion> {
    return request.post(`/api/recruitment/jobs/${jobId}/versions`, data);
  },

  restoreVersion(jobId: number, versionId: number): Promise<JobPostDetail> {
    return request.post(`/api/recruitment/jobs/${jobId}/versions/${versionId}/restore`);
  },

  listConversations(jobId: number): Promise<JobConversation[]> {
    return request.get(`/api/recruitment/jobs/${jobId}/conversations`);
  },

  listMessages(conversationId: number): Promise<JobConversationMessage[]> {
    return request.get(`/api/recruitment/job-conversations/${conversationId}/messages`);
  },

  reviewCompliance(versionId: number): Promise<ComplianceRiskItem[]> {
    return request.post(`/api/recruitment/job-versions/${versionId}/compliance-review`);
  },

  extractProfile(versionId: number): Promise<JobProfile> {
    return request.post(`/api/recruitment/job-versions/${versionId}/profile`);
  },

  createMatch(jobId: number, data: CreateCandidateMatchRequest): Promise<CandidateJobMatch> {
    return request.post(`/api/recruitment/jobs/${jobId}/matches`, data);
  },

  listMatches(jobId: number): Promise<CandidateJobMatch[]> {
    return request.get(`/api/recruitment/jobs/${jobId}/matches`);
  },

  getMatch(matchId: number): Promise<CandidateJobMatch> {
    return request.get(`/api/recruitment/matches/${matchId}`);
  },

  createInterviewGuide(data: CreateInterviewGuideRequest): Promise<InterviewGuide> {
    return request.post('/api/recruitment/interview-guides', data);
  },

  getInterviewGuide(guideId: number): Promise<InterviewGuide> {
    return request.get(`/api/recruitment/interview-guides/${guideId}`);
  },

  updateInterviewGuide(guideId: number, data: UpdateInterviewGuideRequest): Promise<InterviewGuide> {
    return request.put(`/api/recruitment/interview-guides/${guideId}`, data);
  },

  listInterviewGuides(jobId: number): Promise<InterviewGuide[]> {
    return request.get(`/api/recruitment/jobs/${jobId}/interview-guides`);
  },

  createCandidateMessage(data: CreateCandidateMessageDraftRequest): Promise<CandidateMessageDraftResponse> {
    return request.post('/api/recruitment/candidate-messages', data);
  },

  optimizeCandidateMessage(
    draftId: number,
    data: OptimizeCandidateMessageDraftRequest
  ): Promise<CandidateMessageDraftResponse> {
    return request.post(`/api/recruitment/candidate-messages/${draftId}/optimize`, data);
  },

  listCandidateMessages(jobId: number): Promise<CandidateMessageDraft[]> {
    return request.get(`/api/recruitment/jobs/${jobId}/candidate-messages`);
  },

  getCandidateMessage(draftId: number): Promise<CandidateMessageDraft> {
    return request.get(`/api/recruitment/candidate-messages/${draftId}`);
  },

  listCandidateMessageConversation(draftId: number): Promise<CandidateMessageConversationMessage[]> {
    return request.get(`/api/recruitment/candidate-messages/${draftId}/messages`);
  },

  updateCandidateMessage(
    draftId: number,
    data: UpdateCandidateMessageDraftRequest
  ): Promise<CandidateMessageDraft> {
    return request.put(`/api/recruitment/candidate-messages/${draftId}`, data);
  },
};
