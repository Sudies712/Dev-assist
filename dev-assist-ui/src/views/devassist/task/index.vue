<script setup lang="ts">
import {ref} from "vue";
import {useTask} from "./utils/hook";
import {PureTableBar} from "@/components/RePureTableBar";
import {useRenderIcon} from "@/components/ReIcon/src/hooks";
import Delete from "~icons/ep/delete";
import EditPen from "~icons/ep/edit-pen";
import Refresh from "~icons/ep/refresh";
import AddFill from "~icons/ri/add-circle-line";

defineOptions({name: "DevAssistTask"});

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
  handleAction,
  handleRollback,
  handleDelete,
  handleSizeChange,
  handleCurrentChange
} = useTask();
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
          <el-option label="待处理" value="TODO"/>
          <el-option label="进行中" value="IN_PROGRESS"/>
          <el-option label="待测试" value="READY_FOR_TEST"/>
          <el-option label="已完成" value="DONE"/>
          <el-option label="已关闭" value="CLOSED"/>
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

    <PureTableBar title="任务管理" :columns="columns" @refresh="onSearch">
      <template #buttons>
        <el-button
            type="primary"
            :icon="useRenderIcon(AddFill)"
            @click="openDialog()"
        >
          新增任务
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
            <!-- 待处理：开始 / 取消 -->
            <el-button
                v-if="row.status === 'TODO'"
                class="reset-margin"
                link
                type="success"
                :size="size"
                @click="handleAction(row, 'IN_PROGRESS')"
            >
              开始
            </el-button>
            <el-popconfirm
                v-if="row.status === 'TODO'"
                title="确认取消该任务？"
                @confirm="handleAction(row, 'CLOSED')"
            >
              <template #reference>
                <el-button class="reset-margin" link type="info" :size="size">
                  取消
                </el-button>
              </template>
            </el-popconfirm>
            <!-- 进行中：提测 / 退回(填原因) / 取消 -->
            <template v-else-if="row.status === 'IN_PROGRESS'">
              <el-button
                  class="reset-margin"
                  link
                  type="primary"
                  :size="size"
                  @click="handleAction(row, 'READY_FOR_TEST')"
              >
                提测
              </el-button>
              <el-button
                  class="reset-margin"
                  link
                  type="warning"
                  :size="size"
                  @click="handleRollback(row, 'TODO', '退回待处理')"
              >
                退回
              </el-button>
              <el-popconfirm
                  title="确认取消该任务？"
                  @confirm="handleAction(row, 'CLOSED')"
              >
                <template #reference>
                  <el-button class="reset-margin" link type="info" :size="size">
                    取消
                  </el-button>
                </template>
              </el-popconfirm>
            </template>
            <!-- 待测试：完成 / 退回(填原因) -->
            <template v-else-if="row.status === 'READY_FOR_TEST'">
              <el-popconfirm
                  title="确认任务完成？将记录完成时间"
                  @confirm="handleAction(row, 'DONE')"
              >
                <template #reference>
                  <el-button class="reset-margin" link type="success" :size="size">
                    完成
                  </el-button>
                </template>
              </el-popconfirm>
              <el-button
                  class="reset-margin"
                  link
                  type="warning"
                  :size="size"
                  @click="handleRollback(row, 'IN_PROGRESS', '退回开发')"
              >
                退回
              </el-button>
            </template>
            <!-- 已完成：关闭 -->
            <el-popconfirm
                v-else-if="row.status === 'DONE'"
                title="确认关闭任务？关闭后为终态"
                @confirm="handleAction(row, 'CLOSED')"
            >
              <template #reference>
                <el-button class="reset-margin" link type="info" :size="size">
                  关闭
                </el-button>
              </template>
            </el-popconfirm>
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
                :title="`确认删除任务「${row.title}」`"
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
