export interface JobPost {
  id: number;
  title: string;
  department?: string | null;
  location?: string | null;
  employmentType?: string | null;
  seniority?: string | null;
  currentVersionId?: number | null;
  status: string;
  createdAt: string;
  updatedAt: string;
}

export interface JobPostVersion {
  id: number;
  jobPostId: number;
  versionNo: number;
  content: string;
  structuredContentJson?: string | null;
  changeSummary?: string | null;
  createdByAi: boolean;
  createdAt: string;
}

export interface ComplianceRiskItem {
  id: number;
  jobPostVersionId: number;
  riskLevel: 'HIGH' | 'MEDIUM' | 'LOW' | string;
  riskType: string;
  matchedText?: string | null;
  description: string;
  suggestion?: string | null;
  basis?: string | null;
  sortOrder: number;
}

export interface JobProfile {
  id: number;
  jobPostId: number;
  jobPostVersionId: number;
  hardRequirementsJson?: string | null;
  competenciesJson?: string | null;
  bonusItemsJson?: string | null;
  eliminationItemsJson?: string | null;
  verificationPointsJson?: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface JobConversation {
  id: number;
  jobPostId: number;
  title: string;
  createdAt: string;
  updatedAt: string;
}

export interface JobConversationMessage {
  id: number;
  conversationId: number;
  role: 'user' | 'assistant' | string;
  content: string;
  relatedVersionId?: number | null;
  messageOrder: number;
  createdAt: string;
}

export interface JobPostDetail {
  jobPost: JobPost;
  currentVersion?: JobPostVersion | null;
  complianceRisks: ComplianceRiskItem[];
  jobProfile?: JobProfile | null;
}

export interface CreateJobPostRequest {
  title: string;
  department?: string;
  location?: string;
  employmentType?: string;
  seniority?: string;
  initialRequirement?: string;
}

export interface JobChatRequest {
  conversationId?: number;
  message: string;
  provider?: string;
}

export interface SaveJobVersionRequest {
  content: string;
  structuredContentJson?: string;
  changeSummary?: string;
}

export interface JobChatResponse {
  jobPost: JobPost;
  conversation: JobConversation;
  assistantMessage: JobConversationMessage;
  version: JobPostVersion;
  complianceRisks: ComplianceRiskItem[];
  jobProfile?: JobProfile | null;
}

export type JobChatStreamEvent =
  | { type: 'status'; message: string }
  | { type: 'assistant_delta'; content: string }
  | { type: 'complete'; response: JobChatResponse }
  | { type: 'error'; message: string };

export interface CandidateJobMatch {
  id: number;
  jobPostId: number;
  jobPostVersionId: number;
  resumeId: number;
  resumeFilename: string;
  batchId?: string | null;
  matchScore: number;
  recommendation: 'RECOMMENDED' | 'CONSIDER' | 'NOT_RECOMMENDED' | string;
  summary?: string | null;
  strengthsJson?: string | null;
  risksJson?: string | null;
  requirementChecksJson?: string | null;
  verificationQuestionsJson?: string | null;
  status: string;
  createdAt: string;
}

export interface CreateCandidateMatchRequest {
  resumeId: number;
  provider?: string;
}

export interface InterviewGuide {
  id: number;
  jobPostId: number;
  resumeId?: number | null;
  resumeFilename?: string | null;
  matchId?: number | null;
  interviewRound: string;
  durationMinutes: number;
  mode: 'JOB_ONLY' | 'CANDIDATE_SPECIFIC' | string;
  outlineJson?: string | null;
  scorecardJson?: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateInterviewGuideRequest {
  jobPostId: number;
  matchId?: number;
  mode: 'JOB_ONLY' | 'CANDIDATE_SPECIFIC';
  interviewRound: string;
  durationMinutes?: number;
  provider?: string;
}

export interface UpdateInterviewGuideRequest {
  outlineJson: string;
  scorecardJson: string;
}

export interface CandidateMessageDraft {
  id: number;
  jobPostId: number;
  resumeId?: number | null;
  resumeFilename?: string | null;
  matchId?: number | null;
  interviewGuideId?: number | null;
  messageType: string;
  channel: string;
  stage: string;
  tone: string;
  subject?: string | null;
  content: string;
  keyPointsJson?: string | null;
  cautionNotesJson?: string | null;
  status: string;
  createdAt: string;
  updatedAt: string;
}

export interface CandidateMessageConversation {
  id: number;
  draftId: number;
  title: string;
  createdAt: string;
  updatedAt: string;
}

export interface CandidateMessageConversationMessage {
  id: number;
  conversationId: number;
  role: 'user' | 'assistant' | string;
  content: string;
  messageOrder: number;
  createdAt: string;
}

export interface CreateCandidateMessageDraftRequest {
  jobPostId: number;
  resumeId?: number;
  matchId?: number;
  interviewGuideId?: number;
  messageType: string;
  channel: string;
  stage: string;
  tone: string;
  extraInstruction?: string;
  provider?: string;
}

export interface OptimizeCandidateMessageDraftRequest {
  instruction: string;
  provider?: string;
}

export interface UpdateCandidateMessageDraftRequest {
  subject?: string;
  content: string;
}

export interface CandidateMessageDraftResponse {
  draft: CandidateMessageDraft;
  conversation: CandidateMessageConversation;
  assistantMessage: CandidateMessageConversationMessage;
}
