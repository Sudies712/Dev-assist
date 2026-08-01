<script setup lang="ts">
import {ref, watch} from "vue";
import {message} from "@/utils/message";
import {
  addProjectMember,
  changeMemberRole,
  getMemberCandidates,
  getProject,
  getProjectMembers,
  removeProjectMember
} from "@/api/devassist/project";
import {getMemberLoad, getProjectStatistics} from "@/api/devassist/statistics";

const props = defineProps<{ visible: boolean; projectId: number | null }>();
const emit = defineEmits<{ "update:visible": [boolean] }>();

const project = ref<any>(null);
const members = ref<any[]>([]);
const statistics = ref<any>(null);
const candidates = ref<any[]>([]);
const loading = ref(false);

const addVisible = ref(false);
const addForm = ref({userId: "" as number | string, projectRole: "DEVELOPER"});

const ROLE_MAP: any = {
  OWNER: "项目负责人",
  DEVELOPER: "开发人员",
  TESTER: "测试人员"
};
const ROLE_OPTS = [
  {label: "项目负责人", value: "OWNER"},
  {label: "开发人员", value: "DEVELOPER"},
  {label: "测试人员", value: "TESTER"}
];
const STATUS_MAP: any = {
  NOT_STARTED: "未开始",
  IN_PROGRESS: "进行中",
  PAUSED: "暂停中",
  COMPLETED: "已完成",
  ARCHIVED: "已归档"
};
const STATUS_TYPE: any = {
  NOT_STARTED: "info",
  IN_PROGRESS: "success",
  PAUSED: "warning",
  COMPLETED: "primary",
  ARCHIVED: ""
};

async function loadData() {
  if (!props.projectId) return;
  loading.value = true;
  try {
    const [p, mem, load, stat]: any = await Promise.all([
      getProject(props.projectId),
      getProjectMembers(props.projectId),
      getMemberLoad(props.projectId),
      getProjectStatistics(props.projectId)
    ]);
    project.value = p;
    statistics.value = stat;
    const loadMap = new Map((load || []).map((x: any) => [x.userId, x]));
    members.value = (mem || []).map((m: any) => ({...m, ...loadMap.get(m.userId)}));
  } finally {
    loading.value = false;
  }
}

async function openAdd() {
  const c: any = await getMemberCandidates(props.projectId as number);
  candidates.value = c || [];
  if (!candidates.value.length) {
    message("暂无可添加的候选成员", {type: "info"});
    return;
  }
  addForm.value = {userId: "", projectRole: "DEVELOPER"};
  addVisible.value = true;
}

async function submitAdd() {
  if (!addForm.value.userId) {
    message("请选择成员", {type: "warning"});
    return;
  }
  await addProjectMember(props.projectId as number, {
    userId: addForm.value.userId,
    projectRole: addForm.value.projectRole
  });
  message("添加成功", {type: "success"});
  addVisible.value = false;
  loadData();
}

async function handleRoleChange(m: any, role: string) {
  if (role === m.projectRole) return;
  await changeMemberRole(props.projectId as number, m.userId, role);
  message("角色已更新", {type: "success"});
  m.projectRole = role;
}

async function handleRemove(m: any) {
  await removeProjectMember(props.projectId as number, m.userId);
  message("已移除", {type: "success"});
  loadData();
}

watch(
    () => [props.visible, props.projectId],
    ([vis]) => {
      if (vis) loadData();
    }
);
</script>

<template>
  <el-drawer
      :model-value="visible"
      size="42%"
      :title="project ? `项目详情「${project.name}」` : '项目详情'"
      @update:model-value="emit('update:visible', $event)"
  >
    <div v-loading="loading">
      <!-- 基本信息 -->
      <el-descriptions v-if="project" :column="2" border size="small" class="mb-4">
        <el-descriptions-item label="状态">
          <el-tag effect="plain" :type="STATUS_TYPE[project.status] || ''">
            {{ STATUS_MAP[project.status] || project.status }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="技术栈">
          {{ project.techStack || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="开始时间">
          {{ project.startDate || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="结束时间">
          {{ project.endDate || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="创建人">
          {{ project.creatorName || project.creatorId }}
        </el-descriptions-item>
        <el-descriptions-item label="描述" :span="2">
          {{ project.description || "-" }}
        </el-descriptions-item>
      </el-descriptions>

      <!-- 统计摘要（需求/任务/Bug/测试） -->
      <div class="section-title">统计摘要</div>
      <el-row :gutter="10" class="mb-4">
        <el-col :span="6">
          <el-card shadow="never" class="stat">
            <div class="stat-num">{{ statistics?.requirement?.total ?? "-" }}</div>
            <div class="stat-lbl">需求</div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="never" class="stat">
            <div class="stat-num">{{ statistics?.task?.total ?? "-" }}</div>
            <div class="stat-lbl">任务</div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="never" class="stat">
            <div class="stat-num">{{ statistics?.bug?.total ?? "-" }}</div>
            <div class="stat-lbl">缺陷</div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="never" class="stat">
            <div class="stat-num">{{ statistics?.test?.total ?? "-" }}</div>
            <div class="stat-lbl">测试用例</div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 成员管理 -->
      <div class="flex items-center justify-between section-title">
        <span>项目成员（{{ members.length }}）</span>
        <el-button size="small" type="primary" @click="openAdd">添加成员</el-button>
      </div>
      <el-table :data="members" size="small" empty-text="暂无成员">
        <el-table-column label="姓名" prop="realName" width="110"/>
        <el-table-column label="项目角色" width="150">
          <template #default="{ row }">
            <el-select
                :model-value="row.projectRole"
                size="small"
                @change="(v: string) => handleRoleChange(row, v)"
            >
              <el-option
                  v-for="o in ROLE_OPTS"
                  :key="o.value"
                  :label="o.label"
                  :value="o.value"
              />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="任务负载">
          <template #default="{ row }">
            <span>任务 {{ row.taskCount ?? 0 }}</span>
            <el-divider direction="vertical"/>
            <span>缺陷 {{ row.bugCount ?? 0 }}</span>
            <el-divider direction="vertical"/>
            <span>已完成 {{ row.doneCount ?? 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="90">
          <template #default="{ row }">
            <el-popconfirm
                :title="`确认移除「${row.realName}」？（须无未完成任务）`"
                @confirm="handleRemove(row)"
            >
              <template #reference>
                <el-button link type="primary" size="small">移除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 添加成员对话框 -->
    <el-dialog v-model="addVisible" title="添加成员" width="38%" append-to-body>
      <el-form label-width="90px">
        <el-form-item label="成员">
          <el-select v-model="addForm.userId" placeholder="选择系统用户" filterable>
            <el-option
                v-for="c in candidates"
                :key="c.id"
                :label="`${c.realName}（${c.username}）`"
                :value="c.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="项目角色">
          <el-select v-model="addForm.projectRole">
            <el-option
                v-for="o in ROLE_OPTS"
                :key="o.value"
                :label="o.label"
                :value="o.value"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addVisible = false">取消</el-button>
        <el-button type="primary" @click="submitAdd">确定</el-button>
      </template>
    </el-dialog>
  </el-drawer>
</template>

<style lang="scss" scoped>
.section-title {
  font-size: 14px;
  font-weight: 600;
  margin: 12px 0 8px;
}

.stat {
  :deep(.el-card__body) {
    padding: 10px;
    text-align: center;
  }

  .stat-num {
    font-size: 22px;
    font-weight: 700;
    color: var(--el-color-primary);
  }

  .stat-lbl {
    font-size: 12px;
    color: var(--el-text-color-secondary);
  }
}
</style>
