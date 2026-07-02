当前岗位基础信息：
- 岗位名称：{title}
- 部门：{department}
- 工作地点：{location}
- 工作模式：{employmentType}
- 职级/年限：{seniority}

当前 JD 草稿：
{currentJd}

最近对话：
{conversationHistory}

HR 本轮需求：
{message}

请根据 HR 本轮需求生成新的 JD 草稿。

处理规则：
1. 如果“当前 JD 草稿”不为空，本轮需求必须视为对当前 JD 的增量修改，保留未被要求修改的内容。
2. 如果 HR 说“修改为”“改成”“补充”“删除”“调整”等，必须基于当前 JD 草稿进行编辑，不要重新生成一份通用 JD。
3. 如果“当前 JD 草稿”为空，才基于岗位基础信息和 HR 需求生成完整 JD。
4. assistantReply 要简洁说明本轮做了什么修改，不要反复要求 HR 提供已经在当前 JD、基础信息或历史对话中出现的信息。
