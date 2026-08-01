<script setup lang="ts">
import {onMounted, ref} from "vue";
import {PureTableBar} from "@/components/RePureTableBar";
import {getRoleList, getRolePermissionIds} from "@/api/system";

defineOptions({name: "SystemRole"});

const loading = ref(true);
const dataList = ref<any[]>([]);

const columns: TableColumnList = [
  {label: "ID", prop: "id", width: 70},
  {label: "角色名称", prop: "roleName", width: 140},
  {label: "角色码", prop: "roleCode", width: 130},
  {label: "说明", prop: "description", minWidth: 200},
  {label: "状态", width: 100, slot: "status"},
  {label: "权限数", prop: "permCount", width: 100},
  {label: "类型", width: 100, slot: "buildIn"}
];

async function loadData() {
  loading.value = true;
  try {
    const roles: any = await getRoleList();
    const list = roles || [];
    const counts = await Promise.all(
        list.map((r: any) =>
            getRolePermissionIds(r.id)
                .then((ids: any) => (Array.isArray(ids) ? ids.length : 0))
                .catch(() => 0)
        )
    );
    dataList.value = list.map((r: any, i: number) => ({
      ...r,
      permCount: counts[i]
    }));
  } finally {
    loading.value = false;
  }
}

onMounted(loadData);
</script>

<template>
  <div>
    <PureTableBar title="角色管理" :columns="columns" @refresh="loadData">
      <template #buttons>
      <span class="text-xs text-gray-400">
        系统预置 4 个角色，权限通过 RBAC 维护（权限码见权限表）
      </span>
      </template>
      <template v-slot="{ size, dynamicColumns }">
        <pure-table
            row-key="id"
            adaptive
            :adaptiveConfig="{ offsetBottom: 108 }"
            align-whole="center"
            table-layout="auto"
            :loading="loading"
            :size="size"
            :data="dataList"
            :columns="dynamicColumns"
            :header-cell-style="{
          background: 'var(--el-fill-color-light)',
          color: 'var(--el-text-color-primary)'
        }"
        >
          <template #status="{ row }">
            <el-tag :type="row.status === 'ENABLED' ? 'success' : 'info'" effect="plain">
              {{ row.status === "ENABLED" ? "启用" : "停用" }}
            </el-tag>
          </template>
          <template #buildIn="{ row }">
            <el-tag :type="row.buildIn ? 'warning' : 'info'" effect="plain">
              {{ row.buildIn ? "预置" : "自定义" }}
            </el-tag>
          </template>
        </pure-table>
      </template>
    </PureTableBar>
  </div>
</template>
