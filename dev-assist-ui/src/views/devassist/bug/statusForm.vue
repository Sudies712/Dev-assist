<script setup lang="ts">
import {computed, ref} from "vue";

const props = withDefaults(
    defineProps<{
      formInline: {
        current: string;
        targetStatus: string;
        rejectReason: string;
        fixDescription: string;
        failReason: string;
      };
    }>(),
    {
      formInline: () => ({
        current: "",
        targetStatus: "",
        rejectReason: "",
        fixDescription: "",
        failReason: ""
      })
    }
);

const ruleFormRef = ref();
const newFormInline = ref(props.formInline);

// 状态机下一态（前端提示，合法性以后端 next() 为准）
const NEXT: any = {
  PENDING_CONFIRM: ["PENDING_FIX", "REJECTED"],
  PENDING_FIX: ["FIXING"],
  FIXING: ["PENDING_VERIFY"],
  PENDING_VERIFY: ["CLOSED", "PENDING_FIX"],
  CLOSED: ["PENDING_CONFIRM"],
  REJECTED: ["PENDING_CONFIRM"]
};

const STATUS_MAP: any = {
  PENDING_CONFIRM: "待确认",
  PENDING_FIX: "待修复",
  FIXING: "修复中",
  PENDING_VERIFY: "待验证",
  CLOSED: "已关闭",
  REJECTED: "拒绝修复"
};

const options = computed(() =>
    (NEXT[newFormInline.value.current] || []).map((s: string) => ({
      label: STATUS_MAP[s],
      value: s
    }))
);

// 三处条件必填（与后端 BugService.changeStatus 一致）
// 1) 拒绝修复：target=REJECTED 须填拒绝原因
const needReject = computed(
    () => newFormInline.value.targetStatus === "REJECTED"
);
// 2) 修复完成：FIXING→PENDING_VERIFY 须填修复说明
const needFix = computed(
    () =>
        newFormInline.value.current === "FIXING" &&
        newFormInline.value.targetStatus === "PENDING_VERIFY"
);
// 3) 验证失败：PENDING_VERIFY→PENDING_FIX 须填失败原因
const needFail = computed(
    () =>
        newFormInline.value.current === "PENDING_VERIFY" &&
        newFormInline.value.targetStatus === "PENDING_FIX"
);

const rules = computed(() => {
  const r: any = {
    targetStatus: [
      {required: true, message: "请选择目标状态", trigger: "change"}
    ]
  };
  if (needReject.value) {
    r.rejectReason = [
      {required: true, message: "请填写拒绝原因", trigger: "blur"}
    ];
  }
  if (needFix.value) {
    r.fixDescription = [
      {required: true, message: "请填写修复说明", trigger: "blur"}
    ];
  }
  if (needFail.value) {
    r.failReason = [
      {required: true, message: "请填写验证失败原因", trigger: "blur"}
    ];
  }
  return r;
});

function getRef() {
  return ruleFormRef.value;
}

defineExpose({getRef});
</script>

<template>
  <el-form
      ref="ruleFormRef"
      :model="newFormInline"
      :rules="rules"
      label-width="100px"
  >
    <el-form-item label="目标状态" prop="targetStatus">
      <el-select v-model="newFormInline.targetStatus" class="w-full">
        <el-option
            v-for="o in options"
            :key="o.value"
            :label="o.label"
            :value="o.value"
        />
      </el-select>
    </el-form-item>
    <el-form-item v-if="needReject" label="拒绝原因" prop="rejectReason">
      <el-input
          v-model="newFormInline.rejectReason"
          type="textarea"
          :rows="3"
          placeholder="拒绝修复须填写原因"
      />
    </el-form-item>
    <el-form-item
        v-else-if="needFix"
        label="修复说明"
        prop="fixDescription"
    >
      <el-input
          v-model="newFormInline.fixDescription"
          type="textarea"
          :rows="3"
          placeholder="修复完成须填写修复说明"
      />
    </el-form-item>
    <el-form-item
        v-else-if="needFail"
        label="验证失败原因"
        prop="failReason"
    >
      <el-input
          v-model="newFormInline.failReason"
          type="textarea"
          :rows="3"
          placeholder="验证失败须填写原因"
      />
    </el-form-item>
    <div
        v-if="!newFormInline.targetStatus"
        class="text-xs text-gray-400 pl-[100px]"
    >
      提示：先选择目标状态，相应场景须填写原因（拒绝/修复完成/验证失败）
    </div>
  </el-form>
</template>
