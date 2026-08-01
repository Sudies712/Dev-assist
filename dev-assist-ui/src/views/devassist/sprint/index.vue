<script setup lang="ts">
import {ref} from "vue";
import {useSprint} from "./utils/hook";
import {PureTableBar} from "@/components/RePureTableBar";
import {useRenderIcon} from "@/components/ReIcon/src/hooks";
import Delete from "~icons/ep/delete";
import EditPen from "~icons/ep/edit-pen";
import Refresh from "~icons/ep/refresh";
import AddFill from "~icons/ri/add-circle-line";

defineOptions({name: "DevAssistSprint"});

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
  handleAction,
  handleBurndown,
  handleDelete,
  handleSizeChange,
  handleCurrentChange
} = useSprint();
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
          <el-option label="未开始" value="NOT_STARTED"/>
          <el-option label="进行中" value="IN_PROGRESS"/>
          <el-option label="已完成" value="COMPLETED"/>
          <el-option label="已归档" value="ARCHIVED"/>
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

    <PureTableBar title="迭代管理" :columns="columns" @refresh="onSearch">
      <template #buttons>
        <el-button
            type="primary"
            :icon="useRenderIcon(AddFill)"
            @click="openDialog()"
        >
          新增迭代
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
                :icon="useRenderIcon(EditPen)"
                @click="openDialog('修改', row)"
            >
              编辑
            </el-button>
            <el-button
                class="reset-margin"
                link
                type="primary"
                :size="size"
                @click="handleBurndown(row)"
            >
              燃尽图
            </el-button>
            <!-- 上下文状态动作：未开始→开始、进行中→完成、已完成→归档 -->
            <el-button
                v-if="row.status === 'NOT_STARTED'"
                class="reset-margin"
                link
                type="success"
                :size="size"
                @click="handleAction(row, 'IN_PROGRESS')"
            >
              开始
            </el-button>
            <el-popconfirm
                v-else-if="row.status === 'IN_PROGRESS'"
                title="确认完成迭代？完成后不可回退"
                @confirm="handleAction(row, 'COMPLETED')"
            >
              <template #reference>
                <el-button class="reset-margin" link type="primary" :size="size">
                  完成
                </el-button>
              </template>
            </el-popconfirm>
            <el-popconfirm
                v-else-if="row.status === 'COMPLETED'"
                title="确认归档迭代？归档后只读"
                @confirm="handleAction(row, 'ARCHIVED')"
            >
              <template #reference>
                <el-button class="reset-margin" link type="info" :size="size">
                  归档
                </el-button>
              </template>
            </el-popconfirm>
            <el-popconfirm
                :title="`确认删除迭代「${row.name}」`"
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
