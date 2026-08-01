import dayjs from "dayjs";
import {h, onMounted, reactive, ref, toRaw} from "vue";
import {ElMessageBox} from "element-plus";
import {message} from "@/utils/message";
import {addDialog} from "@/components/ReDialog";
import type {PaginationProps} from "@pureadmin/table";
import editForm from "../form.vue";
import {getProjectList} from "@/api/devassist/project";
import {getSprintList} from "@/api/devassist/sprint";
import {changeTaskStatus, createTask, deleteTask, getTaskList, updateTask} from "@/api/devassist/task";

const STATUS_MAP = {
    TODO: "待处理",
    IN_PROGRESS: "进行中",
    READY_FOR_TEST: "待测试",
    DONE: "已完成",
    CLOSED: "已关闭"
};
const STATUS_TYPE: any = {
    TODO: "info",
    IN_PROGRESS: "success",
    READY_FOR_TEST: "warning",
    DONE: "primary",
    CLOSED: ""
};
const PRIORITY_MAP = {LOW: "低", MEDIUM: "中", HIGH: "高"};
const PRIORITY_TYPE: any = {LOW: "info", MEDIUM: "", HIGH: "warning"};

export function useTask() {
    const form = reactive({
        projectId: "",
        sprintId: "",
        status: "",
        assigneeId: "",
        page: 1,
        pageSize: 10
    });
    const formRef = ref();
    const editFormRef = ref();
    const dataList = ref([]);
    const loading = ref(true);
    const projectOptions = ref([]);
    const sprintOptions = ref([]);
    const pagination = reactive<PaginationProps>({
        total: 0,
        pageSize: 10,
        currentPage: 1,
        background: true
    });

    const columns: TableColumnList = [
        {label: "ID", prop: "id", width: 70},
        {label: "任务标题", prop: "title", minWidth: 180},
        {
            label: "优先级",
            prop: "priority",
            width: 80,
            cellRenderer: ({row}) => (
                <el-tag effect="plain" type={PRIORITY_TYPE[row.priority]}>
                    {PRIORITY_MAP[row.priority] || row.priority}
                </el-tag>
            )
        },
        {
            label: "状态",
            prop: "status",
            width: 100,
            cellRenderer: ({row}) => (
                <el-tag effect="plain" type={STATUS_TYPE[row.status] || ""}>
                    {STATUS_MAP[row.status] || row.status}
                </el-tag>
            )
        },
        {label: "负责人", prop: "assigneeName", width: 100},
        {
            label: "工时(估/实)",
            width: 100,
            formatter: ({estimatedHours, actualHours}) =>
                `${estimatedHours ?? "-"}/${actualHours ?? "-"}`
        },
        {
            label: "创建时间",
            prop: "createTime",
            minWidth: 150,
            formatter: ({createTime}) => dayjs(createTime).format("YYYY-MM-DD HH:mm")
        },
        {label: "操作", fixed: "right", width: 260, slot: "operation"}
    ];

    async function onSearch() {
        loading.value = true;
        try {
            const data: any = await getTaskList(toRaw(form));
            dataList.value = data?.list || [];
            pagination.total = data?.total || 0;
            pagination.pageSize = data?.pageSize || 10;
            pagination.currentPage = data?.currentPage || 1;
        } finally {
            loading.value = false;
        }
    }

    const resetForm = el => {
        if (!el) return;
        el.resetFields();
        void onSearch();
    };

    function openDialog(title = "新增", row?: any) {
        addDialog({
            title: `${title}任务`,
            props: {
                formInline: {
                    id: row?.id,
                    projectId: row?.projectId ?? "",
                    sprintId: row?.sprintId ?? "",
                    title: row?.title ?? "",
                    priority: row?.priority ?? "MEDIUM",
                    description: row?.description ?? "",
                    estimatedHours: row?.estimatedHours ?? "",
                    projectOptions: projectOptions.value,
                    sprintOptions: sprintOptions.value
                }
            },
            width: "46%",
            draggable: true,
            closeOnClickModal: false,
            contentRenderer: () => h(editForm, {ref: editFormRef, formInline: null}),
            beforeSure: (done, {options}) => {
                const cur = options.props.formInline;
                editFormRef.value.getRef().validate(async (valid: boolean) => {
                    if (!valid) return;
                    if (title === "新增") {
                        await createTask(cur);
                        message("新增成功", {type: "success"});
                    } else {
                        await updateTask(cur.id, cur);
                        message("修改成功", {type: "success"});
                    }
                    done();
                    void onSearch();
                });
            }
        });
    }

    /**
     * 上下文状态动作：直接流转（开始/提测）走此函数；
     * 需确认（取消/关闭/完成）与需填原因（退回）由调用方先行交互后再传入。
     */
    async function handleAction(row: any, targetStatus: string, reason?: string) {
        await changeTaskStatus(row.id, {targetStatus, reason});
        message("状态变更成功", {type: "success"});
        void onSearch();
    }

    /** 退回（须填原因）：IN_PROGRESS→TODO 后端强校验；READY_FOR_TEST→IN_PROGRESS 原因写入评论 */
    function handleRollback(row: any, target: string, promptTitle: string) {
        ElMessageBox.prompt(`请输入「${row.title}」的退回原因`, promptTitle, {
            inputPlaceholder: "退回原因（必填）",
            inputValidator: value => (value && value.trim() ? true : "退回必须填写原因"),
            confirmButtonText: "确定退回",
            cancelButtonText: "取消",
            type: "warning"
        })
            .then(({value}) => handleAction(row, target, value.trim()))
            .catch(() => {
            });
    }

    async function handleDelete(row) {
        await deleteTask(row.id);
        message("删除成功", {type: "success"});
        void onSearch();
    }

    function handleSizeChange(val: number) {
        form.pageSize = val;
        void onSearch();
    }

    function handleCurrentChange(val: number) {
        form.page = val;
        void onSearch();
    }

    onMounted(async () => {
        const [p, s]: any = await Promise.all([
            getProjectList({pageSize: 100}),
            getSprintList({pageSize: 100})
        ]);
        projectOptions.value = (p?.list || []).map((x: any) => ({
            id: x.id,
            name: x.name
        }));
        sprintOptions.value = (s?.list || []).map((x: any) => ({
            id: x.id,
            name: x.name
        }));
        void onSearch();
    });

    return {
        form,
        formRef,
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
    };
}
