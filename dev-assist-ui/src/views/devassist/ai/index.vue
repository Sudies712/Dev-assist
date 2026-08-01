<script setup lang="ts">
import {computed, onMounted, ref} from "vue";
import {Check, Delete, Promotion, Refresh} from "@element-plus/icons-vue";
import {message} from "@/utils/message";
import {
  type AiDraft,
  type AiRecord,
  confirmDraft,
  createDrafts,
  discardDraft,
  type DraftItem,
  generateAi,
  listAiRecords,
  listDrafts
} from "@/api/devassist/ai";
import {getProjectList} from "@/api/devassist/project";
import {getSprintList} from "@/api/devassist/sprint";
import {getRequirementList} from "@/api/devassist/requirement";
import {getBugList} from "@/api/devassist/bug";

defineOptions({name: "DevAssistAi"});

interface AssistantCfg {
  value: string;
  label: string;
  ctx: "requirement" | "bug" | "sprint" | "none";
  ctxOpt: boolean;
}

const ASSISTANTS: AssistantCfg[] = [
  {value: "task-breakdown", label: "任务拆解", ctx: "requirement", ctxOpt: true},
  {value: "test-case-generation", label: "用例生成", ctx: "requirement", ctxOpt: true},
  {value: "requirement-analysis", label: "需求分析", ctx: "requirement", ctxOpt: true},
  {value: "bug-analysis", label: "Bug 分析", ctx: "bug", ctxOpt: false},
  {value: "sprint-summary", label: "迭代总结", ctx: "sprint", ctxOpt: false},
  {value: "project-summary", label: "项目总结", ctx: "none", ctxOpt: true}
];

const TARGET_MODULE_MAP = {
  REQUIREMENT: "需求",
  TASK: "任务",
  TESTCASE: "用例",
  BUG: "缺陷",
  SPRINT_SUMMARY: "迭代总结",
  PROJECT_SUMMARY: "项目总结"
};
const PRIORITY_MAP = {LOW: "低", MEDIUM: "中", HIGH: "高"};
const PRIORITY_TYPE = {LOW: "info", HIGH: "warning"} as any;
const RECORD_STATUS_MAP = {
  UNADOPTED: "未采纳",
  PARTIAL: "部分采纳",
  FULL: "已采纳"
};
const RECORD_STATUS_TYPE = {UNADOPTED: "info", PARTIAL: "warning", FULL: "success"} as any;
const DRAFT_STATUS_MAP = {
  PENDING_CONFIRM: "待确认",
  ADOPTED: "已采纳",
  DISCARDED: "已丢弃"
};
const DRAFT_STATUS_TYPE = {
  PENDING_CONFIRM: "warning",
  ADOPTED: "success",
  DISCARDED: "info"
} as any;

const projectId = ref<string | number>("");
const projectOptions = ref<any[]>([]);
const assistantType = ref("task-breakdown");
const contextId = ref<string | number>("");
/** 任务草稿归属迭代（confirm 落 task 表时后端必填，前端建草稿时注入） */
const draftSprintId = ref<string | number>("");

const requirements = ref<any[]>([]);
const bugs = ref<any[]>([]);
const sprints = ref<any[]>([]);

const generating = ref(false);
const creatingDrafts = ref(false);
const aiResult = ref<{ aiRecordId: number; items: DraftItem[] } | null>(null);
const selectedIdx = ref<Set<number>>(new Set());

const records = ref<AiRecord[]>([]);
const selectedRecordId = ref<number | null>(null);
const drafts = ref<AiDraft[]>([]);
const loadingDrafts = ref(false);

const currentCfg = computed(
    () => ASSISTANTS.find(a => a.value === assistantType.value) as AssistantCfg
);
const needContext = computed(() => currentCfg.value.ctx !== "none");
const selectedCount = computed(() => selectedIdx.value.size);

function aiTypeLabel(aiType: string) {
  const a = ASSISTANTS.find(x => x.value.replace(/-/g, "_") === aiType.toLowerCase().replace(/-/g, "_"));
  return a?.label || aiType;
}

/** 从 draftContent(JSON) 提取标题用于展示 */
function draftTitle(d: AiDraft): string {
  try {
    const obj = JSON.parse(d.draftContent);
    return obj.title || obj.content?.slice(0, 40) || d.targetModule;
  } catch {
    return d.targetModule;
  }
}

async function loadContextOptions() {
  const pid = projectId.value;
  if (!pid) return;
  const [req, bug, spr]: any = await Promise.all([
    getRequirementList({projectId: pid, pageSize: 100}),
    getBugList({projectId: pid, pageSize: 100}),
    getSprintList({projectId: pid, pageSize: 100})
  ]);
  requirements.value = (req?.list || []).map((x: any) => ({id: x.id, name: x.title}));
  bugs.value = (bug?.list || []).map((x: any) => ({id: x.id, name: x.title}));
  sprints.value = (spr?.list || []).map((x: any) => ({id: x.id, name: x.name}));
  // 任务草稿默认归属第一个迭代
  draftSprintId.value = sprints.value.length ? sprints.value[0].id : "";
}

async function loadRecords() {
  if (!projectId.value) return;
  records.value = (await listAiRecords(projectId.value)) || [];
}

function onProjectChange() {
  aiResult.value = null;
  contextId.value = "";
  records.value = [];
  drafts.value = [];
  selectedRecordId.value = null;
  loadContextOptions();
  loadRecords();
}

function onAssistantChange() {
  aiResult.value = null;
  contextId.value = "";
}

async function generate() {
  if (!projectId.value) return;
  if (needContext.value && !currentCfg.value.ctxOpt && !contextId.value) {
    message(`请选择${ctxLabel()}`, {type: "warning"});
    return;
  }
  generating.value = true;
  aiResult.value = null;
  try {
    const res: any = await generateAi(
        assistantType.value,
        projectId.value,
        contextId.value
    );
    aiResult.value = {aiRecordId: res.aiRecordId, items: res.items || []};
    selectedIdx.value = new Set(res.items?.map((_: any, i: number) => i) || []);
    if (!res.items?.length) {
      message("AI 未生成结构化建议，请查看原始输出或重试", {type: "info"});
    }
    await loadRecords();
    selectedRecordId.value = res.aiRecordId;
    await loadDraftsFor(res.aiRecordId);
  } finally {
    generating.value = false;
  }
}

function ctxLabel() {
  return currentCfg.value.ctx === "bug"
      ? "缺陷"
      : currentCfg.value.ctx === "sprint"
          ? "迭代"
          : "需求";
}

function toggleItem(i: number) {
  const s = new Set(selectedIdx.value);
  s.has(i) ? s.delete(i) : s.add(i);
  selectedIdx.value = s;
}

async function handleCreateDrafts() {
  if (!aiResult.value) return;
  const items = aiResult.value.items
      .filter((_, i) => selectedIdx.value.has(i))
      // TASK 草稿落表需 sprintId（后端 confirm 必填），前端注入归属迭代
      .map(it => ({
        ...it,
        sprintId:
            it.targetModule === "TASK" ? draftSprintId.value || undefined : it.sprintId
      }));
  if (!items.length) {
    message("请至少勾选一条建议", {type: "warning"});
    return;
  }
  if (items.some((it: DraftItem) => it.targetModule === "TASK" && !it.sprintId)) {
    message("任务草稿需选择归属迭代", {type: "warning"});
    return;
  }
  creatingDrafts.value = true;
  try {
    const ids: any = await createDrafts(
        aiResult.value.aiRecordId,
        projectId.value,
        items
    );
    message(`已生成 ${ids?.length || items.length} 条草稿，待确认`, {
      type: "success"
    });
    selectedRecordId.value = aiResult.value.aiRecordId;
    await loadDraftsFor(aiResult.value.aiRecordId);
    await loadRecords();
  } finally {
    creatingDrafts.value = false;
  }
}

async function loadDraftsFor(recordId: number) {
  loadingDrafts.value = true;
  try {
    drafts.value = (await listDrafts(recordId)) || [];
  } finally {
    loadingDrafts.value = false;
  }
}

async function selectRecord(rec: AiRecord) {
  selectedRecordId.value = rec.id;
  await loadDraftsFor(rec.id);
}

async function handleConfirm(d: AiDraft) {
  const bizId = await confirmDraft(d.id);
  message(`已确认，业务记录 #${bizId} 已创建`, {type: "success"});
  await loadDraftsFor(d.aiRecordId);
  await loadRecords();
}

async function handleDiscard(d: AiDraft) {
  await discardDraft(d.id);
  message("已丢弃", {type: "success"});
  await loadDraftsFor(d.aiRecordId);
  await loadRecords();
}

onMounted(async () => {
  const p: any = await getProjectList({pageSize: 100});
  projectOptions.value = (p?.list || []).map((x: any) => ({id: x.id, name: x.name}));
  if (projectOptions.value.length) {
    projectId.value = projectOptions.value[0].id;
    await onProjectChange();
  }
});
</script>

<template>
  <div class="ai-page">
    <!-- 顶部：生成条件 -->
    <el-card shadow="never" class="gen-card">
      <div class="gen-bar">
        <div class="flex items-center gap-3 flex-wrap">
          <el-icon class="text-primary text-xl">
            <Promotion/>
          </el-icon>
          <span class="text-lg font-bold">AI 助手</span>
          <el-select
              v-model="projectId"
              placeholder="项目"
              class="w-44"
              @change="onProjectChange"
          >
            <el-option
                v-for="p in projectOptions"
                :key="p.id"
                :label="p.name"
                :value="p.id"
            />
          </el-select>
          <el-select
              v-model="assistantType"
              class="w-36"
              @change="onAssistantChange"
          >
            <el-option
                v-for="a in ASSISTANTS"
                :key="a.value"
                :label="a.label"
                :value="a.value"
            />
          </el-select>
          <el-select
              v-if="needContext && currentCfg.ctx === 'requirement'"
              v-model="contextId"
              :placeholder="currentCfg.ctxOpt ? '选择需求（可选）' : '选择需求'"
              clearable
              class="w-56"
          >
            <el-option
                v-for="r in requirements"
                :key="r.id"
                :label="r.name"
                :value="r.id"
            />
          </el-select>
          <el-select
              v-if="needContext && currentCfg.ctx === 'bug'"
              v-model="contextId"
              placeholder="选择缺陷"
              clearable
              class="w-56"
          >
            <el-option
                v-for="b in bugs"
                :key="b.id"
                :label="b.name"
                :value="b.id"
            />
          </el-select>
          <el-select
              v-if="needContext && currentCfg.ctx === 'sprint'"
              v-model="contextId"
              placeholder="选择迭代"
              clearable
              class="w-56"
          >
            <el-option
                v-for="s in sprints"
                :key="s.id"
                :label="s.name"
                :value="s.id"
            />
          </el-select>
          <!-- task-breakdown：任务草稿归属迭代（confirm 落 task 表必填） -->
          <el-select
              v-if="assistantType === 'task-breakdown'"
              v-model="draftSprintId"
              placeholder="草稿归属迭代"
              class="w-48"
          >
            <el-option
                v-for="s in sprints"
                :key="s.id"
                :label="s.name"
                :value="s.id"
            />
          </el-select>
        </div>
        <el-button
            type="primary"
            :icon="Promotion"
            :loading="generating"
            :disabled="!projectId"
            @click="generate"
        >
          {{ generating ? "生成中…" : "生成" }}
        </el-button>
      </div>
    </el-card>

    <el-row :gutter="12" class="mt-3">
      <!-- 左：生成结果 / 建议勾选 -->
      <el-col :xs="24" :md="14">
        <el-card shadow="never" class="panel">
          <template #header>
            <div class="flex items-center justify-between">
              <span class="font-bold">AI 建议</span>
              <el-button
                  v-if="aiResult"
                  type="primary"
                  size="small"
                  :icon="Check"
                  :loading="creatingDrafts"
                  @click="handleCreateDrafts"
              >
                生成草稿（{{ selectedCount }}）
              </el-button>
            </div>
          </template>
          <div v-loading="generating">
            <el-empty
                v-if="!aiResult && !generating"
                description="选择项目+助手后点「生成」，AI 将产出结构化建议"
            />
            <div v-if="generating" class="py-8 text-center text-gray-400">
              AI 正在生成（DeepSeek，约 5-15s）…
            </div>
            <div v-if="aiResult" class="items-list">
              <div
                  v-for="(item, i) in aiResult.items"
                  :key="i"
                  class="item"
                  :class="{ active: selectedIdx.has(i) }"
                  @click="toggleItem(i)"
              >
                <el-checkbox :model-value="selectedIdx.has(i)" class="!mr-2"/>
                <div class="flex-1">
                  <div class="flex items-center gap-2 mb-1 flex-wrap">
                    <el-tag size="small" effect="plain">
                      {{ TARGET_MODULE_MAP[item.targetModule] || item.targetModule }}
                    </el-tag>
                    <el-tag
                        v-if="item.targetType"
                        size="small"
                        type="info"
                        effect="plain"
                    >
                      {{ item.targetType }}
                    </el-tag>
                    <el-tag
                        v-if="item.priority"
                        size="small"
                        :type="PRIORITY_TYPE[item.priority]"
                        effect="plain"
                    >
                      {{ PRIORITY_MAP[item.priority] || item.priority }}
                    </el-tag>
                    <span class="font-semibold">{{ item.title }}</span>
                  </div>
                  <div v-if="item.content" class="text-sm text-gray-500 leading-6">
                    {{ item.content }}
                  </div>
                </div>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 右：记录 + 草稿 -->
      <el-col :xs="24" :md="10">
        <el-card shadow="never" class="panel">
          <template #header>
            <div class="flex items-center justify-between">
              <span class="font-bold">生成记录 / 草稿</span>
              <el-button :icon="Refresh" link @click="loadRecords">
                刷新
              </el-button>
            </div>
          </template>
          <div v-if="!records.length" class="py-6">
            <el-empty description="暂无 AI 记录" :image-size="60"/>
          </div>
          <div v-else class="records">
            <div
                v-for="rec in records"
                :key="rec.id"
                class="record"
                :class="{ active: rec.id === selectedRecordId }"
                @click="selectRecord(rec)"
            >
              <div class="flex items-center justify-between">
                <span class="font-medium">{{ aiTypeLabel(rec.aiType) }}</span>
                <el-tag size="small" :type="RECORD_STATUS_TYPE[rec.status]" effect="plain">
                  {{ RECORD_STATUS_MAP[rec.status] || rec.status }}
                </el-tag>
              </div>
              <div class="text-xs text-gray-400 mt-1">
                {{ rec.createTime?.replace("T", " ").slice(0, 16) }}
              </div>
            </div>
          </div>

          <div v-if="selectedRecordId" class="drafts">
            <div class="drafts-title">草稿（{{ drafts.length }}）</div>
            <div v-loading="loadingDrafts">
              <div v-if="!drafts.length" class="text-center text-gray-400 text-sm py-3">
                暂无草稿（左侧勾选建议 → 生成草稿）
              </div>
              <div v-for="d in drafts" :key="d.id" class="draft">
                <div class="flex items-center gap-2 mb-1 flex-wrap">
                  <el-tag size="small" effect="plain">
                    {{ TARGET_MODULE_MAP[d.targetModule] || d.targetModule }}
                  </el-tag>
                  <el-tag size="small" :type="DRAFT_STATUS_TYPE[d.status]" effect="plain">
                    {{ DRAFT_STATUS_MAP[d.status] || d.status }}
                  </el-tag>
                </div>
                <div class="text-sm font-medium mb-2">{{ draftTitle(d) }}</div>
                <div v-if="d.status === 'PENDING_CONFIRM'" class="flex gap-2">
                  <el-button size="small" type="primary" :icon="Check" @click="handleConfirm(d)">
                    确认落表
                  </el-button>
                  <el-button size="small" :icon="Delete" @click="handleDiscard(d)">
                    丢弃
                  </el-button>
                </div>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style lang="scss" scoped>
.ai-page {
  .gen-card {
    :deep(.el-card__body) {
      padding: 12px 20px;
    }
  }

  .gen-bar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    flex-wrap: wrap;
  }

  .panel {
    min-height: 400px;
  }

  .items-list {
    .item {
      display: flex;
      align-items: flex-start;
      padding: 10px 12px;
      border: 1px solid var(--el-border-color-lighter);
      border-radius: 8px;
      margin-bottom: 8px;
      cursor: pointer;
      transition: all 0.2s;

      &:hover {
        border-color: var(--el-color-primary-light-5);
      }

      &.active {
        background: var(--el-color-primary-light-9);
        border-color: var(--el-color-primary-light-5);
      }
    }
  }

  .records {
    max-height: 200px;
    overflow-y: auto;

    .record {
      padding: 8px 10px;
      border-radius: 6px;
      cursor: pointer;
      margin-bottom: 4px;

      &:hover {
        background: var(--el-fill-color-light);
      }

      &.active {
        background: var(--el-color-primary-light-9);
      }
    }
  }

  .drafts {
    margin-top: 12px;
    border-top: 1px dashed var(--el-border-color);
    padding-top: 10px;

    .drafts-title {
      font-size: 13px;
      color: var(--el-text-color-secondary);
      margin-bottom: 8px;
    }

    .draft {
      padding: 8px 10px;
      border: 1px solid var(--el-border-color-lighter);
      border-radius: 6px;
      margin-bottom: 8px;
    }
  }
}
</style>
