package com.sudies.devassist.modules.ai.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sudies.devassist.common.enums.AiRecordStatus;
import com.sudies.devassist.common.enums.AiType;
import com.sudies.devassist.common.enums.RoleCode;
import com.sudies.devassist.common.exception.BizException;
import com.sudies.devassist.common.result.ResultCode;
import com.sudies.devassist.common.utils.SecurityUtils;
import com.sudies.devassist.modules.ai.dto.AiAskDTO;
import com.sudies.devassist.modules.ai.dto.DraftItem;
import com.sudies.devassist.modules.ai.entity.AiRecord;
import com.sudies.devassist.modules.ai.mapper.AiRecordMapper;
import com.sudies.devassist.modules.ai.vo.AiResultVO;
import com.sudies.devassist.modules.bug.entity.Bug;
import com.sudies.devassist.modules.bug.mapper.BugMapper;
import com.sudies.devassist.modules.project.entity.Project;
import com.sudies.devassist.modules.project.entity.ProjectMember;
import com.sudies.devassist.modules.project.mapper.ProjectMapper;
import com.sudies.devassist.modules.project.mapper.ProjectMemberMapper;
import com.sudies.devassist.modules.requirement.entity.Requirement;
import com.sudies.devassist.modules.requirement.mapper.RequirementMapper;
import com.sudies.devassist.modules.sprint.entity.Sprint;
import com.sudies.devassist.modules.sprint.mapper.SprintMapper;
import com.sudies.devassist.modules.task.entity.Task;
import com.sudies.devassist.modules.task.mapper.TaskMapper;
import dev.langchain4j.model.chat.ChatModel;
import jakarta.annotation.Resource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AI 助手：6 个生成型助手（需求分析/任务拆解/用例生成/Bug分析/迭代总结/项目总结）。
 * 加载 prompt 模板 → 占位符注入上下文 → DeepSeek 生成 → 解析结构化 items → 写 ai_record。
 * <p>同步生成（非 SSE，SSE 传输层可后补）；输出为 JSON 数组（容错提取）。
 */
@Service
public class AiService {

    @Resource
    private ChatModel chatModel;

    @Resource
    private AiRecordMapper aiRecordMapper;

    @Resource
    private ProjectMapper projectMapper;

    @Resource
    private RequirementMapper requirementMapper;

    @Resource
    private SprintMapper sprintMapper;

    @Resource
    private BugMapper bugMapper;

    @Resource
    private TaskMapper taskMapper;

    @Resource
    private ProjectMemberMapper projectMemberMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public AiResultVO generate(String assistant, AiAskDTO dto) {
        AiType type = parseType(assistant);
        ensureMember(dto.getProjectId());
        ensurePermission(type);

        String template = loadPrompt(type);
        Map<String, String> vars = buildContext(type, dto);
        String prompt = render(template, vars);

        String output;
        try {
            output = chatModel.chat(prompt);
        } catch (Exception e) {
            throw new BizException(ResultCode.AI_SERVICE_UNAVAILABLE, "AI 生成失败：" + e.getMessage());
        }

        List<DraftItem> items = parseItems(output);

        AiRecord rec = new AiRecord();
        rec.setProjectId(dto.getProjectId());
        rec.setModule(moduleOf(type));
        rec.setAiType(type.name());
        rec.setInputContent(prompt);
        rec.setOutputContent(output);
        rec.setStatus(AiRecordStatus.UNADOPTED.name());
        rec.setCreatorId(SecurityUtils.currentUserId());
        aiRecordMapper.insert(rec);

        AiResultVO vo = new AiResultVO();
        vo.setAiRecordId(rec.getId());
        vo.setItems(items);
        vo.setRawOutput(output);
        return vo;
    }

    /**
     * 列出当前用户在项目的 AI 记录（供采纳回溯）。
     */
    public List<AiRecord> listRecords(Long projectId) {
        ensureMember(projectId);
        return aiRecordMapper.selectList(Wrappers.<AiRecord>lambdaQuery()
                .eq(AiRecord::getProjectId, projectId)
                .orderByDesc(AiRecord::getCreateTime));
    }

    // ============================== 私有 ==============================

    private AiType parseType(String assistant) {
        return switch (assistant) {
            case "requirement-analysis" -> AiType.REQUIREMENT_ANALYSIS;
            case "task-breakdown" -> AiType.TASK_BREAKDOWN;
            case "test-case-generation" -> AiType.TEST_CASE_GENERATION;
            case "bug-analysis" -> AiType.BUG_ANALYSIS;
            case "sprint-summary" -> AiType.SPRINT_SUMMARY;
            case "project-summary" -> AiType.PROJECT_SUMMARY;
            default -> throw new BizException(ResultCode.BAD_REQUEST, "未知助手: " + assistant);
        };
    }

    private String moduleOf(AiType type) {
        return switch (type) {
            case REQUIREMENT_ANALYSIS -> "REQUIREMENT";
            case TASK_BREAKDOWN -> "TASK";
            case TEST_CASE_GENERATION -> "TEST";
            case BUG_ANALYSIS -> "BUG";
            case SPRINT_SUMMARY -> "SPRINT";
            case PROJECT_SUMMARY -> "STATISTICS";
            default -> "DOCUMENT";
        };
    }

    private Map<String, String> buildContext(AiType type, AiAskDTO dto) {
        Map<String, String> vars = new HashMap<>();
        Project p = projectMapper.selectById(dto.getProjectId());
        if (p != null) {
            vars.put("project_name", n(p.getName()));
            vars.put("project_description", n(p.getDescription()));
            vars.put("tech_stack", n(p.getTechStack()));
        }
        Long ctx = dto.getContextId();
        switch (type) {
            case REQUIREMENT_ANALYSIS, TASK_BREAKDOWN, TEST_CASE_GENERATION -> {
                if (ctx != null) {
                    Requirement r = requirementMapper.selectById(ctx);
                    if (r != null) {
                        vars.put("requirement_title", n(r.getTitle()));
                        vars.put("requirement_description", n(r.getDescription()));
                        vars.put("requirement_priority", n(r.getPriority()));
                    }
                }
            }
            case BUG_ANALYSIS -> {
                if (ctx != null) {
                    Bug b = bugMapper.selectById(ctx);
                    if (b != null) {
                        vars.put("bug_title", n(b.getTitle()));
                        vars.put("bug_description", n(b.getDescription()));
                        vars.put("bug_steps", n(b.getStepsToReproduce()));
                    }
                }
            }
            case SPRINT_SUMMARY -> {
                if (ctx != null) {
                    Sprint s = sprintMapper.selectById(ctx);
                    if (s != null) {
                        vars.put("sprint_name", n(s.getName()));
                        vars.put("sprint_goal", n(s.getGoal()));
                        vars.put("existing_tasks", titlesOfTasks(
                                taskMapper.selectList(Wrappers.<Task>lambdaQuery().eq(Task::getSprintId, ctx))));
                    }
                }
            }
            case PROJECT_SUMMARY -> vars.put("existing_tasks", titlesOfTasks(
                    taskMapper.selectList(Wrappers.<Task>lambdaQuery().eq(Task::getProjectId, dto.getProjectId()))));
            default -> {
            }
        }
        return vars;
    }

    private String titlesOfTasks(List<Task> tasks) {
        if (tasks.isEmpty()) {
            return "无";
        }
        return tasks.stream().map(Task::getTitle).limit(50).collect(Collectors.joining("、"));
    }

    private String loadPrompt(AiType type) {
        String file = switch (type) {
            case REQUIREMENT_ANALYSIS -> "prompt/requirement-analysis.md";
            case TASK_BREAKDOWN -> "prompt/task-breakdown.md";
            case TEST_CASE_GENERATION -> "prompt/test-case-generation.md";
            case BUG_ANALYSIS -> "prompt/bug-analysis.md";
            case SPRINT_SUMMARY -> "prompt/sprint-summary.md";
            case PROJECT_SUMMARY -> "prompt/project-summary.md";
            default -> "prompt/knowledge-qa.md";
        };
        try (var in = new ClassPathResource(file).getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new BizException(ResultCode.ERROR, "加载 prompt 失败: " + file);
        }
    }

    private String render(String template, Map<String, String> vars) {
        if (vars != null) {
            for (Map.Entry<String, String> e : vars.entrySet()) {
                template = template.replace("{{" + e.getKey() + "}}", e.getValue() == null ? "" : e.getValue());
            }
        }
        // 清理未替换的占位符
        return template.replaceAll("\\{\\{[^}]+\\}\\}", "");
    }

    /**
     * 从 LLM 输出容错提取 JSON 数组（AI 可能带 markdown 包裹或解释）。
     */
    private List<DraftItem> parseItems(String output) {
        if (!StringUtils.hasText(output)) {
            return List.of();
        }
        int start = output.indexOf('[');
        int end = output.lastIndexOf(']');
        if (start < 0 || end < 0 || end <= start) {
            return List.of();
        }
        try {
            return objectMapper.readValue(output.substring(start, end + 1), new TypeReference<>() {
            });
        } catch (Exception e) {
            return List.of();
        }
    }

    private void ensurePermission(AiType type) {
        String code = "ai:" + type.name().toLowerCase();
        if (!SecurityUtils.hasPermission(code)) {
            throw new BizException(ResultCode.FORBIDDEN, "无该 AI 助手使用权限");
        }
    }

    private void ensureMember(Long projectId) {
        if (SecurityUtils.hasRole(RoleCode.ADMIN.name())) {
            return;
        }
        Long uid = SecurityUtils.currentUserId();
        Long cnt = projectMemberMapper.selectCount(Wrappers.<ProjectMember>lambdaQuery()
                .eq(ProjectMember::getProjectId, projectId)
                .eq(ProjectMember::getUserId, uid));
        if (cnt == null || cnt == 0L) {
            throw new BizException(ResultCode.FORBIDDEN, "不属于该项目，无权操作");
        }
    }

    private String n(String s) {
        return s == null ? "" : s;
    }
}
