<script setup lang="ts">
import {ref} from "vue";
import {UploadFilled} from "@element-plus/icons-vue";
import type {UploadFile} from "element-plus";
import ReCol from "@/components/ReCol";

const props = withDefaults(
    defineProps<{ formInline: any }>(),
    {
      formInline: () => ({
        projectId: "",
        type: "OTHER",
        description: "",
        file: null as File | null,
        projectOptions: []
      })
    }
);

const ruleFormRef = ref();
const newFormInline = ref(props.formInline);

const rules = {
  projectId: [{required: true, message: "请选择项目", trigger: "change"}]
};

function handleFileChange(uploadFile: UploadFile) {
  newFormInline.value.file = uploadFile.raw || null;
}

function handleFileRemove() {
  newFormInline.value.file = null;
}

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
      label-width="90px"
  >
    <el-row :gutter="30">
      <re-col :value="12" :xs="24" :sm="24">
        <el-form-item label="所属项目" prop="projectId">
          <el-select
              v-model="newFormInline.projectId"
              placeholder="请选择项目"
              class="w-full"
          >
            <el-option
                v-for="p in newFormInline.projectOptions"
                :key="p.id"
                :label="p.name"
                :value="p.id"
            />
          </el-select>
        </el-form-item>
      </re-col>
      <re-col :value="12" :xs="24" :sm="24">
        <el-form-item label="文档类型">
          <el-select v-model="newFormInline.type" class="w-full">
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
      </re-col>
      <re-col :value="24">
        <el-form-item label="描述">
          <el-input
              v-model="newFormInline.description"
              placeholder="文档说明（可选）"
          />
        </el-form-item>
      </re-col>
      <re-col :value="24">
        <el-form-item label="文档文件" required>
          <el-upload
              drag
              :limit="1"
              :auto-upload="false"
              :on-change="handleFileChange"
              :on-remove="handleFileRemove"
              class="w-full"
          >
            <el-icon class="el-icon--upload">
              <UploadFilled/>
            </el-icon>
            <div class="el-upload__text">
              将文件拖到此处，或<em>点击选择</em>
            </div>
            <template #tip>
              <div class="el-upload__tip">
                支持 md/txt/doc/docx/pdf 等格式；上传后异步解析切片入知识库
              </div>
            </template>
          </el-upload>
        </el-form-item>
      </re-col>
    </el-row>
  </el-form>
</template>
