<script setup lang="ts">
import {ref} from "vue";
import {useUser} from "./utils/hook";
import {PureTableBar} from "@/components/RePureTableBar";
import {useRenderIcon} from "@/components/ReIcon/src/hooks";
import EditPen from "~icons/ep/edit-pen";
import Refresh from "~icons/ep/refresh";
import Key from "~icons/ep/key";
import UserFilled from "~icons/ep/user-filled";
import AddFill from "~icons/ri/add-circle-line";

defineOptions({name: "SystemUser"});

const formRef = ref();
const tableRef = ref();

const {
  form,
  loading,
  columns,
  dataList,
  pagination,
  roleOptions,
  onSearch,
  resetForm,
  openDialog,
  handleResetPassword,
  handleAssignRoles,
  handleSizeChange,
  handleCurrentChange
} = useUser();
</script>

<template>
  <div>
    <el-form
        ref="formRef"
        :inline="true"
        :model="form"
        class="search-form bg-bg_color w-full pl-8 pt-3 overflow-auto"
    >
      <el-form-item label="用户名：" prop="username">
        <el-input
            v-model="form.username"
            placeholder="用户名"
            clearable
            class="w-40!"
            @keyup.enter="onSearch"
        />
      </el-form-item>
      <el-form-item label="状态：" prop="status">
        <el-select v-model="form.status" placeholder="全部" clearable class="w-32!">
          <el-option label="启用" value="ENABLED"/>
          <el-option label="禁用" value="DISABLED"/>
        </el-select>
      </el-form-item>
      <el-form-item label="角色：" prop="roleId">
        <el-select
            v-model="form.roleId"
            placeholder="全部"
            clearable
            class="w-36!"
        >
          <el-option
              v-for="r in roleOptions"
              :key="r.id"
              :label="r.roleName"
              :value="r.id"
          />
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

    <PureTableBar title="用户管理" :columns="columns" @refresh="onSearch">
      <template #buttons>
        <el-button
            type="primary"
            :icon="useRenderIcon(AddFill)"
            @click="openDialog()"
        >
          新增用户
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
                :icon="useRenderIcon(UserFilled)"
                @click="handleAssignRoles(row)"
            >
              角色
            </el-button>
            <el-popconfirm
                :title="`确认重置「${row.username}」的密码？`"
                @confirm="handleResetPassword(row)"
            >
              <template #reference>
                <el-button
                    class="reset-margin"
                    link
                    type="primary"
                    :size="size"
                    :icon="useRenderIcon(Key)"
                >
                  重置密码
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
