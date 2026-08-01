<script setup lang="ts">
import {onMounted, ref} from "vue";
import dayjs from "dayjs";
import {ElMessageBox} from "element-plus";
import {convertExecutionToBug, listExecutions} from "@/api/devassist/testcase";
import {message} from "@/utils/message";

const props = withDefaults(
    defineProps<{ formInline: { caseId: number; caseTitle: string } }>(),
    {
      formInline: () => ({caseId: 0, caseTitle: ""})
    }
);

const newFormInline = ref(props.formInline);
const executions = ref<any[]>([]);
const loading = ref(false);
const tableRef = ref();

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

async function handleConvertBug(row: any) {
  await ElMessageBox.confirm(
      `确认为该失败记录创建缺陷？缺陷将使用执行时快照「${row.title || newFormInline.value.caseTitle}」作为标题前缀。`,
      "转缺陷",
      {type: "warning", confirmButtonText: "创建缺陷", cancelButtonText: "取消"}
  );
  const res: any = await convertExecutionToBug(row.id);
  message(`已创建缺陷 #${res.bugId}`, {type: "success"});
  await loadExecutions(); // 刷新后该行变为「缺陷 #id」
}

// 点击整行切换展开（小三角点击是原生展开行为，row-click 不触发，两者互补）
function handleRowClick(row: any) {
  tableRef.value?.toggleRowExpansion(row);
}

onMounted(loadExecutions);
</script>

<template>
  <div class="history-table-wrap">
    <el-table
        ref="tableRef"
        :data="executions"
        v-loading="loading"
        size="small"
        empty-text="暂无执行记录"
        @row-click="handleRowClick"
    >
      <!-- 展开行：完整展示执行时快照的前置条件与测试步骤 -->
      <el-table-column type="expand">
        <template #default="{ row }">
          <div class="exec-expand">
            <el-descriptions :column="1" border size="small" class="mb-2">
              <el-descriptions-item label="前置条件">
                <span class="exec-pre">{{ row.preconditions || "-" }}</span>
              </el-descriptions-item>
              <el-descriptions-item label="测试步骤">
                <span class="exec-pre">{{ row.steps || "-" }}</span>
              </el-descriptions-item>
            </el-descriptions>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="用例名称" prop="title" min-width="160" show-overflow-tooltip>
        <template #default="{ row }">{{ row.title || "-" }}</template>
      </el-table-column>
      <el-table-column label="预期结果" prop="expectedResult" min-width="180" show-overflow-tooltip>
        <template #default="{ row }">{{ row.expectedResult || "-" }}</template>
      </el-table-column>
      <el-table-column label="实际结果" prop="actualResult" min-width="180" show-overflow-tooltip>
        <template #default="{ row }">{{ row.actualResult || "-" }}</template>
      </el-table-column>
      <el-table-column label="执行结果" width="90">
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
      <el-table-column label="执行人" prop="executorName" width="90"/>
      <el-table-column label="执行时间" width="150">
      <template #default="{ row }">
        {{ dayjs(row.executeTime).format("YYYY-MM-DD HH:mm") }}
      </template>
    </el-table-column>
      <el-table-column label="缺陷" width="100" fixed="right" align="center">
        <template #default="{ row }">
          <!-- FAILED 恒显示转缺陷按钮（可重复转缺陷，不因已关联缺陷而隐藏） -->
          <el-button
              v-if="row.result === 'FAILED'"
              type="primary"
              link
              size="small"
              @click.stop="handleConvertBug(row)"
          >
            转缺陷
          </el-button>
          <span v-else>-</span>
        </template>
      </el-table-column>
  </el-table>
  </div>
</template>

<style scoped>
.exec-expand {
  padding: 4px 12px;
}

.exec-pre {
  white-space: pre-wrap;
  word-break: break-word;
}

/* 弹窗无 footer（hideFooter），表格底部留边距避免贴底 */
.history-table-wrap {
  padding-bottom: 16px;
}
</style>
