<script setup lang="ts">
import {computed, nextTick, onMounted, ref, watch} from "vue";
import {useDark, useECharts} from "@pureadmin/utils";
import {getProjectList} from "@/api/devassist/project";
import {getSprintBurndown, getSprintList} from "@/api/devassist/sprint";
import {getMemberLoad, getProjectStatistics} from "@/api/devassist/statistics";

defineOptions({name: "DevAssistDashboard"});

const {isDark} = useDark();
const theme = computed(() => (isDark.value ? "dark" : "light"));

const projectId = ref<string | number>("");
const projectOptions = ref<any[]>([]);
const stats = ref<any>(null);
const memberLoad = ref<any[]>([]);
const sprints = ref<any[]>([]);
const loading = ref(false);

// 各图表 ref
const taskStatusRef = ref();
const reqPriorityRef = ref();
const memberLoadRef = ref();
const aiTypeRef = ref();
const burndownRef = ref();
const {setOptions: setTaskStatus} = useECharts(taskStatusRef, {theme, renderer: "svg"});
const {setOptions: setReqPriority} = useECharts(reqPriorityRef, {theme, renderer: "svg"});
const {setOptions: setMemberLoad} = useECharts(memberLoadRef, {theme, renderer: "svg"});
const {setOptions: setAiType} = useECharts(aiTypeRef, {theme, renderer: "svg"});
const {setOptions: setBurndown} = useECharts(burndownRef, {theme, renderer: "svg"});

// 枚举码 → 中文
const TASK_STATUS: any = {
  TODO: "待处理",
  IN_PROGRESS: "进行中",
  READY_FOR_TEST: "待测试",
  DONE: "已完成",
  CLOSED: "已关闭"
};
const PRIORITY: any = {LOW: "低", MEDIUM: "中", HIGH: "高"};
const AI_TYPE: any = {
  REQUIREMENT_ANALYSIS: "需求分析",
  TASK_BREAKDOWN: "任务拆解",
  TEST_CASE_GENERATION: "用例生成",
  BUG_ANALYSIS: "Bug分析",
  SPRINT_SUMMARY: "迭代总结",
  PROJECT_SUMMARY: "项目总结"
};

const distToPie = (dist: any[], labelMap: any) =>
    (dist || [])
        .filter(d => d.value > 0)
        .map(d => ({name: labelMap[d.name] || d.name, value: d.value}));

const distToBar = (dist: any[], labelMap: any) => ({
  categories: (dist || []).map(d => labelMap[d.name] || d.name),
  values: (dist || []).map(d => d.value)
});

function pct(v: number | undefined) {
  if (v == null) return "-";
  return Math.round(v * 100) + "%";
}

async function loadAll() {
  if (!projectId.value) return;
  loading.value = true;
  try {
    const [st, ml, spr]: any = await Promise.all([
      getProjectStatistics(projectId.value),
      getMemberLoad(projectId.value),
      getSprintList({projectId: projectId.value, pageSize: 100})
    ]);
    stats.value = st;
    memberLoad.value = ml || [];
    sprints.value = spr?.list || [];
    await nextTick();
    renderCharts();
    await renderBurndown();
  } finally {
    loading.value = false;
  }
}

function renderCharts() {
  const s = stats.value;
  if (!s) return;
  setTaskStatus({
    tooltip: {trigger: "item"},
    legend: {bottom: 0},
    series: [
      {
        name: "任务状态",
        type: "pie",
        radius: ["40%", "70%"],
        data: distToPie(s.task?.statusDist, TASK_STATUS),
        label: {formatter: "{b}: {c}"}
      }
    ]
  });
  const prio = distToBar(s.requirement?.priorityDist, PRIORITY);
  setReqPriority({
    tooltip: {trigger: "axis"},
    grid: {left: 40, right: 20, top: 30, bottom: 30},
    xAxis: {type: "category", data: prio.categories},
    yAxis: {type: "value", minInterval: 1},
    series: [{type: "bar", data: prio.values, itemStyle: {borderRadius: [4, 4, 0, 0]}}]
  });
  const ml = memberLoad.value;
  setMemberLoad({
    tooltip: {trigger: "axis"},
    legend: {data: ["任务", "缺陷", "已完成"], top: 0},
    grid: {left: 40, right: 20, top: 40, bottom: 30},
    xAxis: {type: "category", data: ml.map(m => m.realName || m.userId)},
    yAxis: {type: "value", minInterval: 1},
    series: [
      {name: "任务", type: "bar", data: ml.map(m => m.taskCount)},
      {name: "缺陷", type: "bar", data: ml.map(m => m.bugCount)},
      {name: "已完成", type: "bar", data: ml.map(m => m.doneCount)}
    ]
  });
  const ai = distToBar(s.ai?.byType, AI_TYPE);
  setAiType({
    tooltip: {trigger: "axis"},
    grid: {left: 40, right: 20, top: 30, bottom: 40},
    xAxis: {
      type: "category",
      data: ai.categories,
      axisLabel: {interval: 0, rotate: ai.categories.length > 3 ? 20 : 0, fontSize: 10}
    },
    yAxis: {type: "value", minInterval: 1},
    series: [{type: "bar", data: ai.values, itemStyle: {borderRadius: [4, 4, 0, 0]}}]
  });
}

async function renderBurndown() {
  const active =
      sprints.value.find((s: any) => s.status === "IN_PROGRESS") || sprints.value[0];
  if (!active) {
    setBurndownOpt([]);
    return;
  }
  try {
    const data: any = await getSprintBurndown(active.id);
    setBurndownOpt(Array.isArray(data) ? data : []);
  } catch {
    setBurndownOpt([]);
  }
}

function setBurndownOpt(list: any[]) {
  setBurndown({
    tooltip: {trigger: "axis"},
    legend: {data: ["实际剩余", "理想线"], top: 0},
    grid: {left: 40, right: 20, top: 40, bottom: 30},
    xAxis: {type: "category", data: list.map(d => d.date), axisLabel: {fontSize: 10}},
    yAxis: {type: "value", name: "剩余任务", minInterval: 1},
    series: [
      {name: "实际剩余", type: "line", data: list.map(d => d.remaining), smooth: true, areaStyle: {opacity: 0.1}},
      {name: "理想线", type: "line", data: list.map(d => d.ideal), lineStyle: {type: "dashed"}, symbol: "none"}
    ]
  });
}

watch(projectId, () => loadAll());

onMounted(async () => {
  const p: any = await getProjectList({pageSize: 100});
  projectOptions.value = (p?.list || []).map((x: any) => ({id: x.id, name: x.name}));
  if (projectOptions.value.length) {
    projectId.value = projectOptions.value[0].id;
  }
});
</script>

<template>
  <div v-loading="loading" class="dashboard">
    <el-card shadow="never" class="proj-card">
      <div class="flex items-center justify-between flex-wrap gap-3">
        <div class="flex items-center">
          <span class="text-lg font-bold mr-2">Scrum 控制台</span>
          <el-tag type="info" effect="plain" size="small">项目概况 + 各模块统计</el-tag>
        </div>
        <el-select v-model="projectId" placeholder="选择项目" class="w-56" @change="loadAll">
          <el-option v-for="p in projectOptions" :key="p.id" :label="p.name" :value="p.id"/>
        </el-select>
      </div>
    </el-card>

    <el-row :gutter="12" class="mt-3">
      <el-col :xs="12" :sm="8" :md="4">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-label">项目</div>
          <div class="stat-value">{{ stats?.project?.total ?? "-" }}</div>
          <div class="stat-sub">进行中 {{ stats?.project?.inProgress ?? 0 }}</div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="8" :md="4">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-label">需求</div>
          <div class="stat-value">{{ stats?.requirement?.total ?? "-" }}</div>
          <div class="stat-sub">完成率 {{ pct(stats?.requirement?.doneRate) }}</div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="8" :md="4">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-label">任务</div>
          <div class="stat-value">{{ stats?.task?.total ?? "-" }}</div>
          <div class="stat-sub">完成率 {{ pct(stats?.task?.doneRate) }}</div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="8" :md="4">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-label">缺陷</div>
          <div class="stat-value">{{ stats?.bug?.total ?? "-" }}</div>
          <div class="stat-sub">
            严重 {{ stats?.bug?.severityDist?.find((d: any) => d.name === "CRITICAL")?.value ?? 0 }}
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="8" :md="4">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-label">测试用例</div>
          <div class="stat-value">{{ stats?.test?.total ?? "-" }}</div>
          <div class="stat-sub">通过率 {{ pct(stats?.test?.passRate) }}</div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="8" :md="4">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-label">AI 调用</div>
          <div class="stat-value">{{ stats?.ai?.totalCalls ?? "-" }}</div>
          <div class="stat-sub">采纳率 {{ pct(stats?.ai?.adoptRate) }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="12" class="mt-3">
      <el-col :xs="24" :md="12">
        <el-card shadow="never">
          <template #header>任务状态分布</template>
          <div ref="taskStatusRef" style="height: 280px"/>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="12">
        <el-card shadow="never">
          <template #header>需求优先级分布</template>
          <div ref="reqPriorityRef" style="height: 280px"/>
        </el-card>
      </el-col>
    </el-row>
    <el-row :gutter="12" class="mt-3">
      <el-col :xs="24" :md="12">
        <el-card shadow="never">
          <template #header>成员负载</template>
          <div ref="memberLoadRef" style="height: 280px"/>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="12">
        <el-card shadow="never">
          <template #header>AI 调用分布</template>
          <div ref="aiTypeRef" style="height: 280px"/>
        </el-card>
      </el-col>
    </el-row>
    <el-row :gutter="12" class="mt-3">
      <el-col :span="24">
        <el-card shadow="never">
          <template #header>迭代燃尽图（当前迭代）</template>
          <div ref="burndownRef" style="height: 320px"/>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style lang="scss" scoped>
.dashboard {
  .proj-card :deep(.el-card__body) {
    padding: 12px 20px;
  }

  .stat-card {
    :deep(.el-card__body) {
      padding: 16px;
    }

    .stat-label {
      font-size: 13px;
      color: var(--el-text-color-secondary);
    }

    .stat-value {
      font-size: 28px;
      font-weight: 700;
      color: var(--el-color-primary);
      margin: 4px 0;
    }

    .stat-sub {
      font-size: 12px;
      color: var(--el-text-color-regular);
    }
  }
}
</style>
