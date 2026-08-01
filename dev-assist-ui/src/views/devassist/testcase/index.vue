<script setup lang="ts">
import {ref} from "vue";
import {useTestCase} from "./utils/hook";
import TestCaseDetail from "./detail.vue";
import {PureTableBar} from "@/components/RePureTableBar";
import {useRenderIcon} from "@/components/ReIcon/src/hooks";
import Delete from "~icons/ep/delete";
import EditPen from "~icons/ep/edit-pen";
import Refresh from "~icons/ep/refresh";
import AddFill from "~icons/ri/add-circle-line";

defineOptions({name: "DevAssistTestCase"});

const formRef = ref();
const tableRef = ref();

const {
  form,
  loading,
  columns,
  dataList,
  pagination,
  projectOptions,
  sprintOptions,
  onSearch,
  resetForm,
  openDialog,
  handleExecute,
  handleHistory,
  handleDelete,
  handleSizeChange,
  handleCurrentChange,
  detailVisible,
  detailCase,
  openDetail,
  RESULT_TYPE
} = useTestCase();
</script>

<template>
  <div>
    <el-form
        ref="formRef"
        :inline="true"
        :model="form"
        class="search-form bg-bg_color w-full pl-8 pt-3 overflow-auto"
    >
      <el-form-item label="项目：" prop="projectId">
        <el-select
            v-model="form.projectId"
            placeholder="全部"
            clearable
            class="w-40!"
        >
          <el-option
              v-for="p in projectOptions"
              :key="p.id"
              :label="p.name"
              :value="p.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="迭代：" prop="sprintId">
        <el-select
            v-model="form.sprintId"
            placeholder="全部"
            clearable
            class="w-36!"
        >
          <el-option
              v-for="s in sprintOptions"
              :key="s.id"
              :label="s.name"
              :value="s.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="优先级：" prop="priority">
        <el-select
            v-model="form.priority"
            placeholder="全部"
            clearable
            class="w-32!"
        >
          <el-option label="低" value="LOW"/>
          <el-option label="中" value="MEDIUM"/>
          <el-option label="高" value="HIGH"/>
        </el-select>
      </el-form-item>
      <el-form-item label="关键字：" prop="keyword">
        <el-input
            v-model="form.keyword"
            placeholder="用例标题"
            clearable
            class="w-44!"
        />
      </el-form-item>
      <el-form-item>
        <el-button
            type="primary"
            :icon="useRenderIcon('ri/search-line')"
            :loading="loading"
            @click="onSearch"
        >
          搜索
        </el-button>
        <el-button :icon="useRenderIcon(Refresh)" @click="resetForm(formRef)">
          重置
        </el-button>
      </el-form-item>
    </el-form>

    <PureTableBar title="测试用例" :columns="columns" @refresh="onSearch">
      <template #buttons>
        <el-button
            type="primary"
            :icon="useRenderIcon(AddFill)"
            @click="openDialog()"
        >
          新增用例
        </el-button>
      </template>
      <template v-slot="{ size, dynamicColumns }">
        <pure-table
            ref="tableRef"
            row-key="id"
            adaptive
            :adaptiveConfig="{ offsetBottom: 108 }"
            align-whole="center"
            table-layout="auto"
            :loading="loading"
            :size="size"
            :data="dataList"
            :columns="dynamicColumns"
            :pagination="{ ...pagination, size }"
            :header-cell-style="{
            background: 'var(--el-fill-color-light)',
            color: 'var(--el-text-color-primary)'
          }"
            @page-size-change="handleSizeChange"
            @page-current-change="handleCurrentChange"
        >
          <template #operation="{ row }">
            <el-button
                class="reset-margin"
                link
                type="primary"
                :size="size"
                @click="openDetail(row)"
            >
              详情
            </el-button>
            <el-button
                class="reset-margin"
                link
                :type="RESULT_TYPE[row.lastResult] || 'primary'"
                :size="size"
                @click="handleExecute(row)"
            >
              执行
            </el-button>
            <el-button
                class="reset-margin"
                link
                type="primary"
                :size="size"
                @click="handleHistory(row)"
            >
              历史
            </el-button>
            <el-button
                class="reset-margin"
                link
                type="primary"
                :size="size"
                :icon="useRenderIcon(EditPen)"
                @click="openDialog('修改', row)"
            >
              编辑
            </el-button>
            <el-popconfirm
                :title="`确认删除用例「${row.title}」`"
                @confirm="handleDelete(row)"
            >
              <template #reference>
                <el-button
                    class="reset-margin"
                    link
                    type="primary"
                    :size="size"
                    :icon="useRenderIcon(Delete)"
                >
                  删除
                </el-button>
              </template>
            </el-popconfirm>
          </template>
        </pure-table>
      </template>
    </PureTableBar>

    <TestCaseDetail
        v-model:visible="detailVisible"
        :case-data="detailCase"
    />
  </div>
</template>

<style lang="scss" scoped>
.search-form {
  :deep(.el-form-item) {
    margin-bottom: 12px;
  }
}
</style>
