<script setup lang="ts">
import {onMounted, ref} from "vue";
import dayjs from "dayjs";
import {listExecutions} from "@/api/devassist/testcase";

const props = withDefaults(
    defineProps<{ formInline: { caseId: number; caseTitle: string } }>(),
    {
      formInline: () => ({caseId: 0, caseTitle: ""})
    }
);

const newFormInline = ref(props.formInline);
const executions = ref<any[]>([]);
const loading = ref(false);

const RESULT_MAP = {
  PASSED: "通过",
  FAILED: "失败",
  BLOCKED: "阻塞",
  SKIPPED: "跳过",
  UNEXECUTED: "未执行"
};
const RESULT_TYPE: any = {
  PASSED: "success",
  FAILED: "danger",
  BLOCKED: "warning",
  SKIPPED: "info",
  UNEXECUTED: "info"
};

async function loadExecutions() {
  loading.value = true;
  try {
    executions.value =
        (await listExecutions(newFormInline.value.caseId)) || [];
  } finally {
    loading.value = false;
  }
}

onMounted(loadExecutions);
</script>

<template>
  <el-table
      :data="executions"
      v-loading="loading"
      size="small"
      empty-text="暂无执行记录"
  >
    <el-table-column label="执行结果" width="100">
      <template #default="{ row }">
        <el-tag
            effect="plain"
            :type="RESULT_TYPE[row.result]"
            size="small"
        >
          {{ RESULT_MAP[row.result] || row.result }}
        </el-tag>
      </template>
    </el-table-column>
    <el-table-column label="实际结果" prop="actualResult" min-width="200" show-overflow-tooltip/>
    <el-table-column label="执行人" prop="executorName" width="100"/>
    <el-table-column label="执行时间" width="160">
      <template #default="{ row }">
        {{ dayjs(row.executeTime).format("YYYY-MM-DD HH:mm") }}
      </template>
    </el-table-column>
  </el-table>
</template>
