<script setup lang="ts">
import {ref} from "vue";
import {useBug} from "./utils/hook";
import {PureTableBar} from "@/components/RePureTableBar";
import {useRenderIcon} from "@/components/ReIcon/src/hooks";
import Delete from "~icons/ep/delete";
import EditPen from "~icons/ep/edit-pen";
import Refresh from "~icons/ep/refresh";
import AddFill from "~icons/ri/add-circle-line";

defineOptions({name: "DevAssistBug"});

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
  isOwner,
  onSearch,
  resetForm,
  openDialog,
  handleChangeStatus,
  handleAssign,
  handleAttachment,
  handleDelete,
  handleSizeChange,
  handleCurrentChange
} = useBug();

// 仅待确认/已拒绝的缺陷可删除（前端门控，后端再校验提交人/负责人）
const canDelete = (status: string) =>
    status === "PENDING_CONFIRM" || status === "REJECTED";
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
      <el-form-item label="状态：" prop="status">
        <el-select v-model="form.status" placeholder="全部" clearable class="w-32!">
          <el-option label="待确认" value="PENDING_CONFIRM"/>
          <el-option label="待修复" value="PENDING_FIX"/>
          <el-option label="修复中" value="FIXING"/>
          <el-option label="待验证" value="PENDING_VERIFY"/>
          <el-option label="已关闭" value="CLOSED"/>
          <el-option label="拒绝修复" value="REJECTED"/>
        </el-select>
      </el-form-item>
      <el-form-item label="严重度：" prop="severity">
        <el-select
            v-model="form.severity"
            placeholder="全部"
            clearable
            class="w-32!"
        >
          <el-option label="轻微" value="MINOR"/>
          <el-option label="普通" value="NORMAL"/>
          <el-option label="严重" value="MAJOR"/>
          <el-option label="致命" value="CRITICAL"/>
        </el-select>
      </el-form-item>
      <el-form-item label="关键字：" prop="keyword">
        <el-input
            v-model="form.keyword"
            placeholder="缺陷标题"
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

    <PureTableBar title="缺陷管理" :columns="columns" @refresh="onSearch">
      <template #buttons>
        <el-button
            type="primary"
            :icon="useRenderIcon(AddFill)"
            @click="openDialog()"
        >
          提交缺陷
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
                v-if="row.status !== 'CLOSED'"
                class="reset-margin"
                link
                type="primary"
                :size="size"
                @click="handleChangeStatus(row)"
            >
              状态
            </el-button>
            <el-button
                class="reset-margin"
                link
                type="primary"
                :size="size"
                @click="handleAttachment(row)"
            >
              附件
            </el-button>
            <el-button
                v-if="isOwner"
                class="reset-margin"
                link
                type="primary"
                :size="size"
                @click="handleAssign(row)"
            >
              分配
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
                v-if="canDelete(row.status)"
                :title="`确认删除缺陷「${row.title}」`"
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
