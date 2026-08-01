<script setup lang="ts">
import {nextTick, onMounted, reactive, ref} from "vue";
import {Document, Promotion} from "@element-plus/icons-vue";
import {askKnowledge} from "@/api/devassist/kb";
import {getProjectList} from "@/api/devassist/project";

defineOptions({name: "DevAssistKbChat"});

interface Source {
  sourceName: string;
  snippet: string;
}

interface Turn {
  role: "user" | "ai";
  text: string;
  sources?: Source[];
  error?: boolean;
}

const projectId = ref<string | number>("");
const projectOptions = ref<any[]>([]);
const question = ref("");
const conversations = ref<Turn[]>([]);
const loading = ref(false);
const messageAreaRef = ref<HTMLElement>();

const suggestions = [
  "密码是怎么存储的？",
  "项目用了什么技术栈？",
  "权限模型是怎样的？",
  "订单支付支持哪些方式？"
];

async function scrollToBottom() {
  await nextTick();
  if (messageAreaRef.value) {
    messageAreaRef.value.scrollTop = messageAreaRef.value.scrollHeight;
  }
}

async function send(q?: string) {
  const text = (q ?? question.value).trim();
  if (!text) return;
  if (!projectId.value) {
    return;
  }
  question.value = "";
  conversations.value.push({role: "user", text});
  // 占位 AI 消息（loading）
  const aiTurn: Turn = reactive({role: "ai", text: ""});
  conversations.value.push(aiTurn);
  loading.value = true;
  await scrollToBottom();
  try {
    const res: any = await askKnowledge(projectId.value, text);
    aiTurn.text = res?.answer || "（未生成答案）";
    aiTurn.sources = res?.sources || [];
  } catch (e: any) {
    aiTurn.text = e?.message || "问答失败，请稍后重试";
    aiTurn.error = true;
  } finally {
    loading.value = false;
    await scrollToBottom();
  }
}

function onProjectChange() {
  conversations.value = [];
}

onMounted(async () => {
  const p: any = await getProjectList({pageSize: 100});
  projectOptions.value = (p?.list || []).map((x: any) => ({
    id: x.id,
    name: x.name
  }));
  if (projectOptions.value.length) {
    projectId.value = projectOptions.value[0].id;
  }
});
</script>

<template>
  <div class="kb-chat">
    <el-card shadow="never" class="header-card">
      <div class="flex items-center justify-between">
        <div class="flex items-center">
          <el-icon class="mr-2 text-primary">
            <Promotion/>
          </el-icon>
          <span class="text-lg font-bold">知识库问答</span>
          <span class="ml-3 text-xs text-gray-400">
            基于项目文档 RAG 检索 + AI 生成，仅回答已上传文档覆盖的内容
          </span>
        </div>
        <el-select
            v-model="projectId"
            placeholder="选择项目"
            class="w-52"
            @change="onProjectChange"
        >
          <el-option
              v-for="p in projectOptions"
              :key="p.id"
              :label="p.name"
              :value="p.id"
          />
        </el-select>
      </div>
    </el-card>

    <el-card shadow="never" class="chat-card">
      <div ref="messageAreaRef" class="message-area">
        <el-empty
            v-if="!conversations.length"
            description="向知识库提问，获取基于项目文档的解答"
        >
          <div class="flex flex-wrap gap-2 justify-center">
            <el-button
                v-for="s in suggestions"
                :key="s"
                size="small"
                round
                @click="send(s)"
            >
              {{ s }}
            </el-button>
          </div>
        </el-empty>

        <div
            v-for="(turn, i) in conversations"
            :key="i"
            class="turn"
            :class="turn.role === 'user' ? 'turn-user' : 'turn-ai'"
        >
          <div class="bubble" :class="turn.role">
            <span v-if="turn.role === 'user'">{{ turn.text }}</span>
            <template v-else>
              <div v-if="!turn.text && loading" class="typing">
                <span class="dot"/><span class="dot"/><span class="dot"/>
              </div>
              <div v-else :class="{ 'text-red-500': turn.error }">
                {{ turn.text }}
              </div>
              <!-- 引用来源 -->
              <div v-if="turn.sources && turn.sources.length" class="sources">
                <div class="sources-title">
                  <el-icon>
                    <Document/>
                  </el-icon>
                  引用来源（{{ turn.sources.length }}）
                </div>
                <div
                    v-for="(src, j) in turn.sources"
                    :key="j"
                    class="source-card"
                >
                  <div class="source-name">📄 {{ src.sourceName }}</div>
                  <div class="source-snippet">{{ src.snippet }}</div>
                </div>
              </div>
            </template>
          </div>
        </div>
      </div>

      <div class="input-area">
        <el-input
            v-model="question"
            placeholder="输入问题，回车发送（需先选择项目）"
            :disabled="loading"
            @keyup.enter="send()"
        >
          <template #prefix>
            <el-icon>
              <Promotion/>
            </el-icon>
          </template>
        </el-input>
        <el-button
            type="primary"
            :loading="loading"
            :disabled="!question.trim() || !projectId"
            @click="send()"
        >
          发送
        </el-button>
      </div>
    </el-card>
  </div>
</template>

<style lang="scss" scoped>
.kb-chat {
  display: flex;
  flex-direction: column;
  gap: 12px;
  height: calc(100vh - 140px);

  .header-card {
    :deep(.el-card__body) {
      padding: 12px 20px;
    }
  }

  .chat-card {
    flex: 1;
    display: flex;
    flex-direction: column;
    min-height: 0;

    :deep(.el-card__body) {
      flex: 1;
      display: flex;
      flex-direction: column;
      min-height: 0;
      padding: 0;
    }
  }

  .message-area {
    flex: 1;
    overflow-y: auto;
    padding: 20px;

    .turn {
      display: flex;
      margin-bottom: 16px;

      &.turn-user {
        justify-content: flex-end;
      }

      &.turn-ai {
        justify-content: flex-start;
      }
    }

    .bubble {
      max-width: 75%;
      padding: 10px 14px;
      border-radius: 10px;
      line-height: 1.7;
      word-break: break-word;

      &.user {
        background: var(--el-color-primary);
        color: #fff;
      }

      &.ai {
        background: var(--el-fill-color-light);
        color: var(--el-text-color-primary);
      }
    }

    .typing {
      display: inline-flex;
      gap: 4px;
      align-items: center;

      .dot {
        width: 6px;
        height: 6px;
        border-radius: 50%;
        background: var(--el-text-color-secondary);
        animation: blink 1.4s infinite both;

        &:nth-child(2) {
          animation-delay: 0.2s;
        }

        &:nth-child(3) {
          animation-delay: 0.4s;
        }
      }
    }

    .sources {
      margin-top: 10px;
      border-top: 1px dashed var(--el-border-color);
      padding-top: 8px;

      .sources-title {
        display: flex;
        align-items: center;
        gap: 4px;
        font-size: 12px;
        color: var(--el-text-color-secondary);
        margin-bottom: 6px;
      }

      .source-card {
        background: var(--el-fill-color-blank);
        border: 1px solid var(--el-border-color-lighter);
        border-radius: 6px;
        padding: 6px 10px;
        margin-bottom: 6px;

        .source-name {
          font-size: 13px;
          font-weight: 600;
          margin-bottom: 2px;
        }

        .source-snippet {
          font-size: 12px;
          color: var(--el-text-color-regular);
          max-height: 80px;
          overflow-y: auto;
          white-space: pre-wrap;
        }
      }
    }
  }

  .input-area {
    display: flex;
    gap: 8px;
    padding: 12px 16px;
    border-top: 1px solid var(--el-border-color-lighter);
  }
}

@keyframes blink {
  0%,
  80%,
  100% {
    opacity: 0.2;
  }
  40% {
    opacity: 1;
  }
}
</style>
