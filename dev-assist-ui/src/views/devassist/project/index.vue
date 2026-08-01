<script setup lang="ts">
import {ref} from "vue";
import {useProject} from "./utils/hook";
import ProjectDetail from "./detail.vue";
import {PureTableBar} from "@/components/RePureTableBar";
import {useRenderIcon} from "@/components/ReIcon/src/hooks";
import EditPen from "~icons/ep/edit-pen";
import Refresh from "~icons/ep/refresh";
import AddFill from "~icons/ri/add-circle-line";

defineOptions({name: "DevAssistProject"});

const formRef = ref();
const tableRef = ref();

const {
  form,
  loading,
  columns,
  dataList,
  pagination,
  onSearch,
  resetForm,
  openDialog,
  handleAction,
  handleSizeChange,
  handleCurrentChange,
  detailVisible,
  detailProjectId,
  openDetail
} = useProject();
</script>

<template>
  <div>
    <el-form
        ref="formRef"
        :inline="true"
        :model="form"
        class="search-form bg-bg_color w-full pl-8 pt-3 overflow-auto"
    >
      <el-form-item label="项目名称：" prop="name">
        <el-input
            v-model="form.name"
            placeholder="请输入项目名称"
            clearable
            class="w-45!"
        />
      </el-form-item>
      <el-form-item label="状态：" prop="status">
        <el-select
            v-model="form.status"
            placeholder="请选择"
            clearable
            class="w-45!"
        >
          <el-option label="未开始" value="NOT_STARTED"/>
          <el-option label="进行中" value="IN_PROGRESS"/>
          <el-option label="暂停中" value="PAUSED"/>
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

    <PureTableBar title="项目管理" :columns="columns" @refresh="onSearch">
      <template #buttons>
        <el-button
            type="primary"
            :icon="useRenderIcon(AddFill)"
            @click="openDialog()"
        >
          新增项目
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
                type="primary"
                :size="size"
                :icon="useRenderIcon(EditPen)"
                :disabled="row.status === 'ARCHIVED'"
                @click="openDialog('修改', row)"
            >
              修改
            </el-button>
            <!-- 上下文状态动作：按当前状态展示对应操作 -->
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
            <template v-else-if="row.status === 'IN_PROGRESS'">
              <el-button
                  class="reset-margin"
                  link
                  type="warning"
                  :size="size"
                  @click="handleAction(row, 'PAUSED')"
              >
                暂停
              </el-button>
              <el-popconfirm
                  title="确认结束项目？将记录结束日期"
                  @confirm="handleAction(row, 'COMPLETED')"
              >
                <template #reference>
                  <el-button class="reset-margin" link type="primary" :size="size">
                    结束
                  </el-button>
                </template>
              </el-popconfirm>
            </template>
            <el-button
                v-else-if="row.status === 'PAUSED'"
                class="reset-margin"
                link
                type="success"
                :size="size"
                @click="handleAction(row, 'IN_PROGRESS')"
            >
              继续
            </el-button>
            <el-popconfirm
                v-else-if="row.status === 'COMPLETED'"
                title="确认归档？归档后数据只读"
                @confirm="handleAction(row, 'ARCHIVED')"
            >
              <template #reference>
                <el-button class="reset-margin" link type="info" :size="size">
                  归档
                </el-button>
              </template>
            </el-popconfirm>
          </template>
        </pure-table>
      </template>
    </PureTableBar>

    <ProjectDetail v-model:visible="detailVisible" :project-id="detailProjectId"/>
  </div>
</template>

<style lang="scss" scoped>
.search-form {
  :deep(.el-form-item) {
    margin-bottom: 12px;
  }
}
</style>
