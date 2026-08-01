import dayjs from "dayjs";
import {h, onMounted, reactive, ref, toRaw} from "vue";
import {message} from "@/utils/message";
import {addDialog} from "@/components/ReDialog";
import type {PaginationProps} from "@pureadmin/table";
import editForm from "../form.vue";
import reviewForm from "../reviewForm.vue";
import scheduleForm from "../scheduleForm.vue";
import {getProjectList} from "@/api/devassist/project";
import {getSprintList} from "@/api/devassist/sprint";
import {
    createRequirement,
    deleteRequirement,
    getRequirementList,
    reviewRequirement,
    scheduleRequirement
} from "@/api/devassist/requirement";

const STATUS_MAP = {
    PENDING_REVIEW: "待评审",
    CONFIRMED: "已确认",
    SCHEDULED: "已排期",
    DEVELOPING: "开发中",
    TESTING: "测试中",
    DONE: "已完成",
    CLOSED: "已关闭"
};
const STATUS_TYPE: any = {
    PENDING_REVIEW: "warning",
    CONFIRMED: "success",
    SCHEDULED: "primary",
    DEVELOPING: "",
    TESTING: "",
    DONE: "success",
    CLOSED: "info"
};
const PRIORITY_MAP = {LOW: "低", MEDIUM: "中", HIGH: "高", URGENT: "紧急"};
const PRIORITY_TYPE: any = {LOW: "info", MEDIUM: "", HIGH: "warning", URGENT: "danger"};

export function useRequirement() {
    const form = reactive({
        projectId: "",
        status: "",
        priority: "",
        page: 1,
        pageSize: 10
    });
    const formRef = ref();
    const editFormRef = ref();
    const reviewFormRef = ref();
    const scheduleFormRef = ref();
    const dataList = ref([]);
    const loading = ref(true);
    const projectOptions = ref([]);
    const pagination = reactive<PaginationProps>({
        total: 0,
        pageSize: 10,
        currentPage: 1,
        background: true
    });

    const columns: TableColumnList = [
        {label: "ID", prop: "id", width: 70},
        {label: "需求标题", prop: "title", minWidth: 200},
        {
            label: "优先级",
            prop: "priority",
            width: 90,
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
        {label: "故事点", prop: "estimatedEffort", width: 80},
        {
            label: "创建时间",
            prop: "createTime",
            minWidth: 150,
            formatter: ({createTime}) => dayjs(createTime).format("YYYY-MM-DD HH:mm")
        },
        {label: "操作", fixed: "right", width: 220, slot: "operation"}
    ];

    async function onSearch() {
        loading.value = true;
        try {
            const data: any = await getRequirementList(toRaw(form));
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

    function openDialog() {
        addDialog({
            title: "新增需求",
            props: {
                formInline: {
                    projectId: form.projectId || "",
                    title: "",
                    type: "FUNCTIONAL",
                    priority: "MEDIUM",
                    estimatedEffort: 0,
                    description: "",
                    projectOptions: projectOptions.value
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
                    await createRequirement({
                        projectId: cur.projectId,
                        title: cur.title,
                        type: cur.type,
                        priority: cur.priority,
                        estimatedEffort: cur.estimatedEffort ?? null,
                        description: cur.description
                    });
                    message("新增成功", {type: "success"});
                    done();
                    void onSearch();
                });
            }
        });
    }

    function handleReview(row) {
        addDialog({
            title: `评审「${row.title}」`,
            props: {formInline: {result: "PASS", opinion: ""}},
            width: "40%",
            draggable: true,
            closeOnClickModal: false,
            contentRenderer: () => h(reviewForm, {ref: reviewFormRef, formInline: null}),
            beforeSure: (done, {options}) => {
                const cur = options.props.formInline;
                reviewFormRef.value.getRef().validate(async (valid: boolean) => {
                    if (!valid) return;
                    const target = cur.result === "PASS" ? "CONFIRMED" : "CLOSED";
                    await reviewRequirement(row.id, {
                        targetStatus: target,
                        reviewResult: cur.result,
                        reviewOpinion: cur.opinion
                    });
                    message("评审完成", {type: "success"});
                    done();
                    void onSearch();
                });
            }
        });
    }

    async function handleSchedule(row) {
        const sprints: any = await getSprintList({projectId: row.projectId, pageSize: 100});
        const sprintOptions = (sprints?.list || []).map((s: any) => ({
            id: s.id,
            name: s.name
        }));
        addDialog({
            title: `排期「${row.title}」`,
            props: {
                formInline: {sprintId: sprintOptions[0]?.id || "", sprintOptions}
            },
            width: "40%",
            draggable: true,
            closeOnClickModal: false,
            contentRenderer: () => h(scheduleForm, {ref: scheduleFormRef, formInline: null}),
            beforeSure: (done, {options}) => {
                const cur = options.props.formInline;
                scheduleFormRef.value.getRef().validate(async (valid: boolean) => {
                    if (!valid) return;
                    await scheduleRequirement(row.id, cur.sprintId);
                    message("排期成功", {type: "success"});
                    done();
                    void onSearch();
                });
            }
        });
    }

    async function handleDelete(row) {
        await deleteRequirement(row.id);
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
        const p: any = await getProjectList({pageSize: 100});
        projectOptions.value = (p?.list || []).map((x: any) => ({id: x.id, name: x.name}));
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
        onSearch,
        resetForm,
        openDialog,
        handleReview,
        handleSchedule,
        handleDelete,
        handleSizeChange,
        handleCurrentChange
    };
}
