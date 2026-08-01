<script setup lang="ts">
import {ref} from "vue";
import {useDocument} from "./utils/hook";
import {PureTableBar} from "@/components/RePureTableBar";
import {useRenderIcon} from "@/components/ReIcon/src/hooks";
import Delete from "~icons/ep/delete";
import EditPen from "~icons/ep/edit-pen";
import Refresh from "~icons/ep/refresh";
import UploadLine from "~icons/ri/upload-line";

defineOptions({name: "DevAssistDocument"});

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
  openUpload,
  openEdit,
  openChunks,
  handleReparse,
  handleDownload,
  handleDelete,
  handleSizeChange,
  handleCurrentChange
} = useDocument();
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
      <el-form-item label="类型：" prop="type">
        <el-select v-model="form.type" placeholder="全部" clearable class="w-32!">
          <el-option label="需求" value="REQUIREMENT"/>
          <el-option label="设计" value="DESIGN"/>
          <el-option label="接口" value="API"/>
          <el-option label="测试" value="TEST"/>
          <el-option label="会议" value="MEETING"/>
          <el-option label="规范" value="STANDARD"/>
          <el-option label="迭代总结" value="SPRINT_SUMMARY"/>
          <el-option label="项目总结" value="PROJECT_SUMMARY"/>
          <el-option label="其他" value="OTHER"/>
        </el-select>
      </el-form-item>
      <el-form-item label="解析状态：" prop="parseStatus">
        <el-select
            v-model="form.parseStatus"
            placeholder="全部"
            clearable
            class="w-32!"
        >
          <el-option label="未解析" value="UNPARSED"/>
          <el-option label="解析中" value="PARSING"/>
          <el-option label="已解析" value="PARSED"/>
          <el-option label="解析失败" value="FAILED"/>
        </el-select>
      </el-form-item>
      <el-form-item label="关键字：" prop="keyword">
        <el-input
            v-model="form.keyword"
            placeholder="文档名"
            clearable
            class="w-40!"
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

    <PureTableBar title="项目文档" :columns="columns" @refresh="onSearch">
      <template #buttons>
        <el-button
            type="primary"
            :icon="useRenderIcon(UploadLine)"
            @click="openUpload()"
        >
          上传文档
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
                @click="openChunks(row)"
            >
              切片
            </el-button>
            <el-button
                class="reset-margin"
                link
                type="primary"
                :size="size"
                @click="handleDownload(row)"
            >
              下载
            </el-button>
            <el-button
                v-if="row.parseStatus !== 'PARSING'"
                class="reset-margin"
                link
                type="primary"
                :size="size"
                @click="handleReparse(row)"
            >
              重新解析
            </el-button>
            <el-button
                class="reset-margin"
                link
                type="primary"
                :size="size"
                :icon="useRenderIcon(EditPen)"
                @click="openEdit(row)"
            >
              编辑
            </el-button>
            <el-popconfirm
                :title="`确认删除文档「${row.name}」？将清理向量与文件`"
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
