import dayjs from "dayjs";
import {h, onMounted, reactive, ref, toRaw} from "vue";
import {message} from "@/utils/message";
import {addDialog} from "@/components/ReDialog";
import type {PaginationProps} from "@pureadmin/table";
import editForm from "../form.vue";
import executeForm from "../executeForm.vue";
import historyForm from "../historyForm.vue";
import {getProjectList} from "@/api/devassist/project";
import {getSprintList} from "@/api/devassist/sprint";
import {
    createTestCase,
    deleteTestCase,
    executeTestCase,
    getTestCaseList,
    updateTestCase
} from "@/api/devassist/testcase";

const PRIORITY_MAP = {LOW: "低", MEDIUM: "中", HIGH: "高"};
const PRIORITY_TYPE: any = {LOW: "info", HIGH: "warning"};

/** 执行结果映射（列表 tag 与执行按钮共用配色） */
const RESULT_MAP: any = {
    PASSED: "通过",
    FAILED: "失败",
    BLOCKED: "阻塞",
    SKIPPED: "跳过"
};
const RESULT_TYPE: any = {
    PASSED: "success",
    FAILED: "danger",
    BLOCKED: "warning",
    SKIPPED: "info"
};

export function useTestCase() {
    const form = reactive({
        projectId: "",
        sprintId: "",
        priority: "",
        keyword: "",
        page: 1,
        pageSize: 10
    });
    const formRef = ref();
    const editFormRef = ref();
    const executeFormRef = ref();
    const historyFormRef = ref();
    const dataList = ref([]);
    const loading = ref(true);
    const projectOptions = ref([]);
    const sprintOptions = ref([]);
    const detailVisible = ref(false);
    const detailCase = ref<any>(null);

    function openDetail(row: any) {
        detailCase.value = row;
        detailVisible.value = true;
    }

    const pagination = reactive<PaginationProps>({
        total: 0,
        pageSize: 10,
        currentPage: 1,
        background: true
    });

    const columns: TableColumnList = [
        {label: "ID", prop: "id", width: 70},
        {label: "用例标题", prop: "title", minWidth: 200},
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
            label: "执行状态",
            prop: "lastResult",
            width: 100,
            cellRenderer: ({row}) => (
                <el-tag effect="plain" type={RESULT_TYPE[row.lastResult] || "info"}>
                    {RESULT_MAP[row.lastResult] || "未执行"}
                </el-tag>
            )
        },
        {label: "创建人", prop: "creatorName", width: 100},
        {label: "所属迭代", prop: "sprintName", width: 120},
        {
            label: "创建时间",
            prop: "createTime",
            minWidth: 150,
            formatter: ({createTime}) => dayjs(createTime).format("YYYY-MM-DD HH:mm")
        },
        {label: "操作", fixed: "right", width: 280, slot: "operation"}
    ];

    async function onSearch() {
        loading.value = true;
        try {
            const data: any = await getTestCaseList(toRaw(form));
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
        onSearch();
    };

    function openDialog(title = "新增", row?: any) {
        addDialog({
            title: `${title}测试用例`,
            props: {
                formInline: {
                    id: row?.id,
                    projectId: row?.projectId ?? "",
                    sprintId: row?.sprintId ?? "",
                    title: row?.title ?? "",
                    priority: row?.priority ?? "MEDIUM",
                    preconditions: row?.preconditions ?? "",
                    steps: row?.steps ?? "",
                    expectedResult: row?.expectedResult ?? "",
                    projectOptions: projectOptions.value,
                    sprintOptions: sprintOptions.value
                }
            },
            width: "48%",
            draggable: true,
            closeOnClickModal: false,
            contentRenderer: () => h(editForm, {ref: editFormRef, formInline: null}),
            beforeSure: (done, {options}) => {
                const cur = options.props.formInline;
                editFormRef.value.getRef().validate(async (valid: boolean) => {
                    if (!valid) return;
                    if (title === "新增") {
                        await createTestCase(cur);
                        message("新增成功", {type: "success"});
                    } else {
                        await updateTestCase(cur.id, cur);
                        message("修改成功", {type: "success"});
                    }
                    done();
                    onSearch();
                });
            }
        });
    }

    function handleExecute(row) {
        addDialog({
            title: `执行用例「${row.title}」`,
            props: {
                formInline: {result: "", actualResult: "", submitBug: false}
            },
            width: "46%",
            draggable: true,
            closeOnClickModal: false,
            contentRenderer: () =>
                h(executeForm, {ref: executeFormRef, formInline: null}),
            beforeSure: (done, {options}) => {
                const cur = options.props.formInline;
                executeFormRef.value.getRef().validate(async (valid: boolean) => {
                    if (!valid) return;
                    const res: any = await executeTestCase(row.id, {
                        result: cur.result,
                        actualResult: cur.actualResult,
                        submitBug: cur.result === "FAILED" ? cur.submitBug : false
                    });
                    if (res?.bugId) {
                        message(`执行已记录，已联动创建缺陷 #${res.bugId}`, {
                            type: "success"
                        });
                    } else {
                        message("执行已记录", {type: "success"});
                    }
                    done();
                    onSearch();
                });
            }
        });
    }

    function handleHistory(row) {
        addDialog({
            title: `执行历史「${row.title}」`,
            props: {
                formInline: {caseId: row.id, caseTitle: row.title}
            },
            width: "60%",
            draggable: true,
            closeOnClickModal: false,
            contentRenderer: () =>
                h(historyForm, {ref: historyFormRef, formInline: null}),
            beforeSure: done => {
                done();
            }
        });
    }

    async function handleDelete(row) {
        await deleteTestCase(row.id);
        message("删除成功", {type: "success"});
        onSearch();
    }

    function handleSizeChange(val: number) {
        form.pageSize = val;
        onSearch();
    }

    function handleCurrentChange(val: number) {
        form.page = val;
        onSearch();
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
        onSearch();
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
        handleExecute,
        handleHistory,
        handleDelete,
        handleSizeChange,
        handleCurrentChange,
        detailVisible,
        detailCase,
        openDetail,
        RESULT_TYPE
    };
}
