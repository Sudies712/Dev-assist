<script setup lang="ts">
import {ref} from "vue";
import {useRequirement} from "./utils/hook";
import {PureTableBar} from "@/components/RePureTableBar";
import {useRenderIcon} from "@/components/ReIcon/src/hooks";
import Delete from "~icons/ep/delete";
import Refresh from "~icons/ep/refresh";
import AddFill from "~icons/ri/add-circle-line";

defineOptions({name: "DevAssistRequirement"});

const formRef = ref();
const tableRef = ref();

const {
  form,
  loading,
  columns,
  dataList,
  pagination,
  projectOptions,
  onSearch,
  resetForm,
  openDialog,
  handleReview,
  handleSchedule,
  handleDelete,
  handleSizeChange,
  handleCurrentChange
} = useRequirement();
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
            placeholder="全部项目"
            clearable
            class="w-45!"
        >
          <el-option
              v-for="p in projectOptions"
              :key="p.id"
              :label="p.name"
              :value="p.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="状态：" prop="status">
        <el-select v-model="form.status" placeholder="全部" clearable class="w-36!">
          <el-option label="待评审" value="PENDING_REVIEW"/>
          <el-option label="已确认" value="CONFIRMED"/>
          <el-option label="已排期" value="SCHEDULED"/>
          <el-option label="开发中" value="DEVELOPING"/>
          <el-option label="测试中" value="TESTING"/>
          <el-option label="已完成" value="DONE"/>
          <el-option label="已关闭" value="CLOSED"/>
        </el-select>
      </el-form-item>
      <el-form-item label="优先级：" prop="priority">
        <el-select v-model="form.priority" placeholder="全部" clearable class="w-32!">
          <el-option label="低" value="LOW"/>
          <el-option label="中" value="MEDIUM"/>
          <el-option label="高" value="HIGH"/>
          <el-option label="紧急" value="URGENT"/>
        </el-select>
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

    <PureTableBar title="需求管理" :columns="columns" @refresh="onSearch">
      <template #buttons>
        <el-button
            type="primary"
            :icon="useRenderIcon(AddFill)"
            @click="openDialog()"
        >
          新增需求
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
                v-if="row.status === 'PENDING_REVIEW'"
                class="reset-margin"
                link
                type="primary"
                :size="size"
                @click="handleReview(row)"
            >
              评审
            </el-button>
            <el-button
                v-if="row.status === 'CONFIRMED'"
                class="reset-margin"
                link
                type="primary"
                :size="size"
                @click="handleSchedule(row)"
            >
              排期
            </el-button>
            <el-popconfirm
                :title="`确认删除需求「${row.title}」`"
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
  </div>
</template>

<style lang="scss" scoped>
.search-form {
  :deep(.el-form-item) {
    margin-bottom: 12px;
  }
}
</style>
