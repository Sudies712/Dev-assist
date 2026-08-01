<script setup lang="ts">
import dayjs from "dayjs";

defineOptions({name: "TestCaseDetail"});

const props = defineProps<{ visible: boolean; caseData: any }>();
const emit = defineEmits<{ "update:visible": [boolean] }>();

const PRIORITY_MAP: any = {LOW: "低", MEDIUM: "中", HIGH: "高"};
const RESULT_MAP: any = {
  PASSED: "通过",
  FAILED: "失败",
  BLOCKED: "阻塞",
  SKIPPED: "跳过"
};
const RESULT_TYPE: any = {
  PASSED: "success",
  FAILED: "danger",
  BLOCKED: "warning",
  SKIPPED: "info"
};
</script>

<template>
  <el-drawer
      :model-value="visible"
      title="用例详情"
      size="42%"
      destroy-on-close
      @update:model-value="emit('update:visible', $event)"
  >
    <el-descriptions v-if="caseData" :column="2" border size="small" class="mb-4">
      <el-descriptions-item label="用例标题" :span="2">
        {{ caseData.title }}
      </el-descriptions-item>
      <el-descriptions-item label="优先级">
        <el-tag effect="plain">{{ PRIORITY_MAP[caseData.priority] || caseData.priority }}</el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="执行状态">
        <el-tag effect="plain" :type="RESULT_TYPE[caseData.lastResult] || 'info'">
          {{ RESULT_MAP[caseData.lastResult] || "未执行" }}
        </el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="所属迭代">
        {{ caseData.sprintName || "-" }}
      </el-descriptions-item>
      <el-descriptions-item label="创建人">
        {{ caseData.creatorName || "-" }}
      </el-descriptions-item>
      <el-descriptions-item label="最近执行时间" :span="2">
        {{
          caseData.lastExecuteTime
              ? dayjs(caseData.lastExecuteTime).format("YYYY-MM-DD HH:mm") : "-"
        }}
      </el-descriptions-item>
      <el-descriptions-item label="前置条件" :span="2">
        {{ caseData.preconditions || "-" }}
      </el-descriptions-item>
      <el-descriptions-item label="测试步骤" :span="2">
        {{ caseData.steps || "-" }}
      </el-descriptions-item>
      <el-descriptions-item label="预期结果" :span="2">
        {{ caseData.expectedResult || "-" }}
      </el-descriptions-item>
    </el-descriptions>
  </el-drawer>
</template>
